<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">会员积分</h2>
        <div class="page-subtitle">积分发放、扣减、兑换记录与会员积分余额</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openGrant">发放 / 扣减</el-button>
        <el-button type="warning" :icon="Present" @click="openExchange">积分兑换</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.customerId" filterable remote clearable :remote-method="searchCustomers"
                 placeholder="搜索会员" style="width: 240px" @change="search">
        <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（积分 ${c.points || 0}）`" :value="c.id" />
      </el-select>
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 140px" @change="search">
        <el-option label="获得" value="EARN" /><el-option label="使用" value="USE" /><el-option label="过期" value="EXPIRE" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="customerId" label="会员ID" width="110" />
      <el-table-column label="积分变动" width="130">
        <template #default="{ row }">
          <span :class="Number(row.points) >= 0 ? 'plus' : 'minus'">{{ Number(row.points) > 0 ? '+' : '' }}{{ row.points }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }"><el-tag :type="row.type === 'EARN' ? 'success' : 'warning'" size="small">{{ row.type === 'EARN' ? '获得' : row.type === 'USE' ? '使用' : '过期' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="orderId" label="关联订单" width="110" />
      <el-table-column prop="remark" label="备注" min-width="200" />
      <el-table-column label="时间" width="170"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 条积分记录</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="grantVisible" title="积分发放 / 扣减" width="520px">
      <el-form label-width="100px">
        <el-form-item label="会员" required>
          <el-select v-model="grantForm.customerId" filterable remote :remote-method="searchCustomers" placeholder="搜索会员" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（当前 ${c.points || 0} 积分）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="积分数量" required>
          <el-input-number v-model="grantForm.points" :min="-100000" :max="100000" style="width: 100%" />
          <div class="tip">正数为发放，负数为扣减</div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="grantForm.remark" placeholder="如：活动奖励 / 人工调整" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="grantVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doGrant">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="exchangeVisible" title="积分兑换" width="520px">
      <el-form label-width="100px">
        <el-form-item label="会员" required>
          <el-select v-model="exchangeForm.customerId" filterable remote :remote-method="searchCustomers" placeholder="搜索会员" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（可用 ${c.points || 0} 积分）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="兑换积分" required><el-input-number v-model="exchangeForm.points" :min="1" style="width: 100%" /></el-form-item>
        <el-form-item label="兑换项目"><el-input v-model="exchangeForm.item" placeholder="如：补水面膜 / 抵扣券" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="exchangeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doExchange">确认兑换</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh, Present } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ customerId: null, type: '' })
const customers = ref<any[]>([])
const grantVisible = ref(false)
const exchangeVisible = ref(false)
const grantForm = reactive<any>({ customerId: null, points: 0, remark: '' })
const exchangeForm = reactive<any>({ customerId: null, points: 1, item: '' })

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/tenant/points/page', { params: { page: page.value, size: size.value, ...query } })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { customerId: null, type: '' }); search() }
async function searchCustomers(kw: string) {
  const res: any = await request.get('/tenant/customers/page', { params: { page: 1, size: 20, kw } })
  customers.value = res.data.records || []
}
function openGrant() {
  Object.assign(grantForm, { customerId: null, points: 0, remark: '' })
  searchCustomers('')
  grantVisible.value = true
}
function openExchange() {
  Object.assign(exchangeForm, { customerId: null, points: 1, item: '' })
  searchCustomers('')
  exchangeVisible.value = true
}
async function doGrant() {
  if (!grantForm.customerId || !grantForm.points) { ElMessage.warning('请选择会员并填写积分'); return }
  saving.value = true
  try {
    await request.post('/tenant/points/grant', grantForm)
    ElMessage.success('操作成功')
    grantVisible.value = false
    load()
  } finally { saving.value = false }
}
async function doExchange() {
  if (!exchangeForm.customerId || !exchangeForm.points) { ElMessage.warning('请选择会员并填写兑换积分'); return }
  saving.value = true
  try {
    await request.post('/tenant/points/exchange', null, { params: exchangeForm })
    ElMessage.success('兑换成功')
    exchangeVisible.value = false
    load()
  } finally { saving.value = false }
}

onMounted(() => { searchCustomers(''); load() })
</script>

<style scoped>
.plus { color: #22a06b; font-weight: 600; }
.minus { color: #e6542c; font-weight: 600; }
.tip { font-size: 11px; color: #9aa0b2; margin-top: 4px; }
</style>
