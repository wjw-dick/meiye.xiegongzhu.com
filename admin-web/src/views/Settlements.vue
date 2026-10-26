<template>
  <div>
    <div class="stat-grid mb-16">
      <div class="stat-card">
        <div class="label">结算单数量</div>
        <div class="value">{{ report.summary?.total || 0 }}</div>
        <div class="hint">按周 / 按月自动汇总</div>
      </div>
      <div class="stat-card">
        <div class="label">订单收入</div>
        <div class="value">¥{{ money(report.summary?.revenue) }}</div>
        <div class="hint">结算周期内已完成订单</div>
      </div>
      <div class="stat-card">
        <div class="label">技师提成</div>
        <div class="value">¥{{ money(report.summary?.techCommission) }}</div>
        <div class="hint">按项目 / 阶梯规则计算</div>
      </div>
      <div class="stat-card">
        <div class="label">平台抽成</div>
        <div class="value">¥{{ money(report.summary?.platformShare) }}</div>
        <div class="hint">门店分成 ¥{{ money(report.summary?.storeShare) }}</div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">结算分账</h2>
          <div class="page-subtitle">按周/按月生成结算单，确认、打款，支持 PDF 结算单打印</div>
        </div>
        <div class="flex gap-8">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Money" @click="openGenerate">生成结算单</el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="search">
          <el-option label="草稿" value="DRAFT" /><el-option label="已确认" value="CONFIRMED" /><el-option label="已打款" value="PAID" />
        </el-select>
        <el-select v-model="query.storeId" placeholder="门店" clearable style="width: 180px" @change="search">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="settlementNo" label="结算单号" width="190" />
        <el-table-column label="周期" width="110"><template #default="{ row }">{{ row.periodType === 'WEEK' ? '按周' : '按月' }}</template></el-table-column>
        <el-table-column label="结算区间" width="200"><template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template></el-table-column>
        <el-table-column label="订单收入" width="120"><template #default="{ row }">¥{{ money(row.revenue) }}</template></el-table-column>
        <el-table-column label="技师提成" width="120"><template #default="{ row }">¥{{ money(row.techCommission) }}</template></el-table-column>
        <el-table-column label="平台抽成" width="120"><template #default="{ row }">¥{{ money(row.platformShare) }}</template></el-table-column>
        <el-table-column label="门店分成" width="120"><template #default="{ row }">¥{{ money(row.storeShare) }}</template></el-table-column>
        <el-table-column label="股东分红" width="120"><template #default="{ row }">¥{{ money(row.dividend) }}</template></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="success" @click="confirm(row)">确认</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" link type="warning" @click="pay(row)">打款</el-button>
            <el-button link @click="printPdf(row)">PDF</el-button>
            <el-button v-if="row.status === 'DRAFT'" link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex-between mt-16">
        <span class="text-muted">共 {{ total }} 张结算单</span>
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                       layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="generateVisible" title="生成结算单" width="560px">
      <el-form label-width="100px">
        <el-form-item label="周期类型">
          <el-radio-group v-model="genForm.periodType">
            <el-radio-button label="WEEK">按周</el-radio-button>
            <el-radio-button label="MONTH">按月</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="结算区间" required>
          <el-date-picker v-model="genRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="门店">
          <el-select v-model="genForm.storeId" clearable placeholder="全部门店" style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="同一周期重复生成会覆盖统计口径，请在生成前确认订单均已支付/核销。" />
      </el-form>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doGenerate">生成</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="结算单明细" size="760px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="结算单号">{{ detail.settlement?.settlementNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusText(detail.settlement?.status) }}</el-descriptions-item>
          <el-descriptions-item label="周期">{{ detail.settlement?.startDate }} ~ {{ detail.settlement?.endDate }}</el-descriptions-item>
          <el-descriptions-item label="订单收入">¥{{ money(detail.settlement?.revenue) }}</el-descriptions-item>
          <el-descriptions-item label="技师提成">¥{{ money(detail.settlement?.techCommission) }}</el-descriptions-item>
          <el-descriptions-item label="平台抽成">¥{{ money(detail.settlement?.platformShare) }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="detail.items" size="small" border class="mt-16">
          <el-table-column prop="technicianId" label="技师ID" width="100" />
          <el-table-column prop="orderId" label="订单ID" width="100" />
          <el-table-column label="收入" width="120"><template #default="{ row }">¥{{ money(row.revenue) }}</template></el-table-column>
          <el-table-column label="比例" width="100"><template #default="{ row }">{{ row.rate }}%</template></el-table-column>
          <el-table-column label="提成" width="120"><template #default="{ row }">¥{{ money(row.commissionAmount) }}</template></el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Money } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, statusText, statusTag, download } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ status: '', storeId: null })
const stores = ref<any[]>([])
const report = ref<any>({})
const generateVisible = ref(false)
const genForm = reactive<any>({ periodType: 'MONTH', storeId: null })
const genRange = ref<[string, string] | null>(null)
const detailVisible = ref(false)
const detail = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const [list, rep] = await Promise.all([
      request.get('/tenant/settlements/page', { params: { page: page.value, size: size.value, ...query } }),
      request.get('/tenant/reports/settlements')
    ]) as any[]
    rows.value = list.data.records || []
    total.value = list.data.total || 0
    report.value = rep.data || {}
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { status: '', storeId: null }); search() }
async function loadStores() {
  const res: any = await request.get('/tenant/stores/list')
  stores.value = res.data || []
}
function openGenerate() {
  genRange.value = null
  generateVisible.value = true
}
async function doGenerate() {
  if (!genRange.value) { ElMessage.warning('请选择结算区间'); return }
  saving.value = true
  try {
    const res: any = await request.post('/tenant/settlements/generate', null, {
      params: { periodType: genForm.periodType, start: genRange.value[0], end: genRange.value[1], storeId: genForm.storeId || undefined }
    })
    ElMessage.success(res.data?.message || '结算单已生成')
    generateVisible.value = false
    load()
  } finally { saving.value = false }
}
async function openDetail(row: any) {
  const res: any = await request.get(`/tenant/settlements/${row.id}`)
  detail.value = res.data
  detailVisible.value = true
}
async function confirm(row: any) {
  await ElMessageBox.confirm('确认该结算单？确认后将进入打款流程。', '确认结算单', { type: 'warning' })
  await request.post(`/tenant/settlements/${row.id}/confirm`)
  ElMessage.success('已确认')
  load()
}
async function pay(row: any) {
  await ElMessageBox.confirm('确认已向技师/门店完成打款？', '结算打款', { type: 'warning' })
  await request.post(`/tenant/settlements/${row.id}/pay`)
  ElMessage.success('已打款')
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该草稿结算单？', '删除', { type: 'warning' })
  await request.delete(`/tenant/settlements/${row.id}`)
  ElMessage.success('已删除')
  load()
}
function printPdf(row: any) { download(`/api/tenant/pdf/settlement/${row.id}`) }
onMounted(async () => { await loadStores(); load() })
</script>
