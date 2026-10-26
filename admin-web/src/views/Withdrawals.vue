<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">技师提现</h2>
        <div class="page-subtitle">提现申请、审核、打款与技师收入/可提现余额</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openApply">申请提现</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="mb-16">
      <el-col :span="18">
        <div class="filter-bar" style="margin-bottom: 0">
          <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="search">
            <el-option v-for="s in ['APPLIED', 'APPROVED', 'PAID', 'REJECTED']" :key="s" :label="statusText(s)" :value="s" />
          </el-select>
          <el-select v-model="query.techId" placeholder="技师" clearable filterable style="width: 180px" @change="onTechChange">
            <el-option v-for="t in techs" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="search">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="income-card">
          <div class="label">可提现余额</div>
          <div class="value">¥{{ money(income.available) }}</div>
          <div class="hint">累计提成 ¥{{ money(income.totalCommission) }}</div>
        </div>
      </el-col>
    </el-row>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="技师" width="140">
        <template #default="{ row }">{{ techName(row.techId) }}</template>
      </el-table-column>
      <el-table-column label="申请金额" width="120"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
      <el-table-column label="手续费" width="110"><template #default="{ row }">¥{{ money(row.fee) }}</template></el-table-column>
      <el-table-column label="实际到账" width="120"><template #default="{ row }"><b class="money">¥{{ money(row.actualAmount) }}</b></template></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="申请时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
      <el-table-column label="打款时间" width="160"><template #default="{ row }">{{ fmt(row.paidAt) }}</template></el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'APPLIED'" link type="success" @click="approve(row)">审核</el-button>
          <el-button v-if="row.status === 'APPLIED'" link type="danger" @click="reject(row)">驳回</el-button>
          <el-button v-if="row.status === 'APPROVED'" link type="primary" @click="pay(row)">打款</el-button>
          <span v-if="['PAID', 'REJECTED'].includes(row.status)" class="text-muted">-</span>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 条提现记录</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="applyVisible" title="申请提现" width="520px">
      <el-form label-width="100px">
        <el-form-item label="技师" required>
          <el-select v-model="form.techId" filterable style="width: 100%" @change="onTechChange">
            <el-option v-for="t in techs" :key="t.id" :label="`${t.name}（${t.techNo || ''}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="可提现">¥{{ money(income.available) }}</el-form-item>
        <el-form-item label="提现金额" required>
          <el-input-number v-model="form.amount" :min="0.01" :max="Number(income.available || 0)" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" placeholder="收款账户 / 说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt, statusText, statusTag } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ status: '', techId: null })
const techs = ref<any[]>([])
const income = ref<any>({})
const applyVisible = ref(false)
const form = reactive<any>({ techId: null, amount: 0, remark: '' })

function techName(id: any) { return techs.value.find((t) => t.id === id)?.name || `技师#${id}` }
async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/tenant/withdrawals/page', { params: { page: page.value, size: size.value, ...query } })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { status: '', techId: null }); income.value = {}; search() }
async function loadTechs() {
  const res: any = await request.get('/tenant/technicians/list')
  techs.value = res.data || []
}
async function onTechChange(id: any) {
  if (!id) { income.value = {}; return }
  try {
    const res: any = await request.get('/tenant/settlements/tech-income', { params: { techId: id } })
    income.value = res.data || {}
  } catch (e) { income.value = {} }
}
function openApply() {
  Object.assign(form, { techId: query.techId, amount: 0, remark: '' })
  if (form.techId) onTechChange(form.techId)
  else income.value = {}
  applyVisible.value = true
}
async function doApply() {
  if (!form.techId || !form.amount) { ElMessage.warning('请选择技师并填写金额'); return }
  saving.value = true
  try {
    await request.post('/tenant/withdrawals/apply', form)
    ElMessage.success('提现申请已提交')
    applyVisible.value = false
    load()
  } finally { saving.value = false }
}
async function approve(row: any) {
  await request.post(`/tenant/withdrawals/${row.id}/approve`)
  ElMessage.success('已审核通过')
  load()
}
async function reject(row: any) {
  const { value } = await ElMessageBox.prompt('请输入驳回原因', '驳回提现', { inputPlaceholder: '账户信息有误' })
  await request.post(`/tenant/withdrawals/${row.id}/reject`, null, { params: { remark: value } })
  ElMessage.success('已驳回')
  load()
}
async function pay(row: any) {
  await ElMessageBox.confirm(`确认打款 ¥${money(row.actualAmount)}？`, '提现打款', { type: 'warning' })
  await request.post(`/tenant/withdrawals/${row.id}/pay`)
  ElMessage.success('已打款')
  load()
}
onMounted(async () => { await loadTechs(); load() })
</script>

<style scoped>
.income-card { background: linear-gradient(120deg, #6b4eff, #9b86ff); border-radius: 12px; padding: 14px 18px; color: #fff; }
.income-card .label { font-size: 12px; opacity: 0.85; }
.income-card .value { font-size: 24px; font-weight: 700; margin: 2px 0; }
.income-card .hint { font-size: 11px; opacity: 0.75; }
.money { color: #e6542c; }
</style>
