<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">售后返工</h2>
        <div class="page-subtitle">返工、退款、投诉受理与处理结果、补偿金额登记</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openApply">新建售后单</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="search">
        <el-option v-for="s in ['PENDING', 'PROCESSING', 'DONE', 'CLOSED']" :key="s" :label="statusText(s)" :value="s" />
      </el-select>
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 140px" @change="search">
        <el-option label="返工" value="RERUN" /><el-option label="退款" value="REFUND" /><el-option label="投诉" value="COMPLAINT" />
      </el-select>
      <el-input v-model="query.customerId" placeholder="会员ID" clearable style="width: 140px" @keyup.enter="search" />
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="customerId" label="会员ID" width="100" />
      <el-table-column prop="orderId" label="订单ID" width="100" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }"><el-tag :type="row.type === 'COMPLAINT' ? 'danger' : row.type === 'REFUND' ? 'warning' : 'info'" size="small">{{ typeText(row.type) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
      <el-table-column prop="result" label="处理结果" min-width="200" show-overflow-tooltip />
      <el-table-column label="补偿" width="110"><template #default="{ row }">¥{{ money(row.compensation) }}</template></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="申请时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="primary" @click="openHandle(row)">处理</el-button>
          <span v-else class="text-muted">已处理</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 条售后单</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="applyVisible" title="新建售后单" width="580px">
      <el-form label-width="100px">
        <el-form-item label="会员" required>
          <el-select v-model="form.customerId" filterable remote :remote-method="searchCustomers" placeholder="搜索会员" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${maskPhone(c.phone)}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联订单" required>
          <el-select v-model="form.orderId" filterable remote :remote-method="searchOrders" placeholder="搜索订单号" style="width: 100%">
            <el-option v-for="o in orders" :key="o.id" :label="`${o.orderNo}（${o.customerName || ''} ¥${money(o.paidAmount)}）`" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="售后类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="返工" value="RERUN" /><el-option label="退款" value="REFUND" /><el-option label="投诉" value="COMPLAINT" />
          </el-select>
        </el-form-item>
        <el-form-item label="原因"><el-input v-model="form.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doApply">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="handleVisible" title="处理售后单" width="560px">
      <el-form label-width="100px">
        <el-form-item label="售后原因">{{ current?.reason }}</el-form-item>
        <el-form-item label="处理结果">
          <el-select v-model="handleForm.status" style="width: 100%">
            <el-option label="处理中" value="PROCESSING" /><el-option label="已完成" value="DONE" /><el-option label="关闭" value="CLOSED" />
          </el-select>
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="handleForm.result" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="补偿金额"><el-input-number v-model="handleForm.compensation" :min="0" :precision="2" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doHandle">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt, maskPhone, statusText, statusTag } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ status: '', type: '', customerId: '' })
const customers = ref<any[]>([])
const orders = ref<any[]>([])
const applyVisible = ref(false)
const handleVisible = ref(false)
const current = ref<any>(null)
const form = reactive<any>({ customerId: null, orderId: null, type: 'RERUN', reason: '' })
const handleForm = reactive<any>({ status: 'DONE', result: '', compensation: 0 })

function typeText(v: any) { return ({ RERUN: '返工', REFUND: '退款', COMPLAINT: '投诉' } as any)[v] || v }
async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v) params[k] = v })
    const res: any = await request.get('/tenant/after-sales/page', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { status: '', type: '', customerId: '' }); search() }
async function searchCustomers(kw: string) {
  const res: any = await request.get('/tenant/customers/page', { params: { page: 1, size: 20, kw } })
  customers.value = res.data.records || []
}
async function searchOrders(kw: string) {
  const res: any = await request.get('/tenant/orders/page', { params: { page: 1, size: 20, kw } })
  orders.value = res.data.records || []
}
function openApply() {
  Object.assign(form, { customerId: null, orderId: null, type: 'RERUN', reason: '' })
  searchCustomers(''); searchOrders('')
  applyVisible.value = true
}
async function doApply() {
  if (!form.customerId || !form.orderId) { ElMessage.warning('请选择会员和订单'); return }
  saving.value = true
  try {
    await request.post('/tenant/after-sales/apply', form)
    ElMessage.success('售后单已提交')
    applyVisible.value = false
    load()
  } finally { saving.value = false }
}
function openHandle(row: any) {
  current.value = row
  Object.assign(handleForm, { status: 'DONE', result: '', compensation: 0 })
  handleVisible.value = true
}
async function doHandle() {
  saving.value = true
  try {
    await request.post(`/tenant/after-sales/${current.value.id}/handle`, null, { params: handleForm })
    ElMessage.success('处理完成')
    handleVisible.value = false
    load()
  } finally { saving.value = false }
}
onMounted(load)
</script>
