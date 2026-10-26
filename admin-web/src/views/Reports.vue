<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">经营报表</h2>
        <div class="page-subtitle">预约、收银、会员、技师、营销、结算多维统计，支持 Excel / PDF 导出</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Download" @click="exportExcel">导出 Excel</el-button>
        <el-button :icon="Printer" @click="printPdf">打印报表</el-button>
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期"
                      end-placeholder="结束日期" style="width: 250px" @change="loadAll" />
      <el-select v-model="storeId" placeholder="全部门店" clearable style="width: 170px" @change="loadAll">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-select v-model="bizType" placeholder="全部品类" clearable style="width: 150px" @change="loadAll">
        <el-option v-for="b in bizTypes" :key="b.code" :label="b.name" :value="b.code" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="loadAll">查询</el-button>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="经营概览" name="overview">
        <div class="stat-grid">
          <div class="stat-card"><div class="label">累计营收</div><div class="value">¥{{ money(overview.revenue) }}</div><div class="hint">今日 ¥{{ money(overview.todayRevenue) }}</div></div>
          <div class="stat-card"><div class="label">订单数 / 客单价</div><div class="value">{{ overview.orders || 0 }}</div><div class="hint">客单价 ¥{{ money(overview.avgOrder) }}</div></div>
          <div class="stat-card"><div class="label">预约到店率</div><div class="value">{{ overview.fulfillRate || 0 }}%</div><div class="hint">今日预约 {{ overview.todayAppointments || 0 }}</div></div>
          <div class="stat-card"><div class="label">会员 / 储值余额</div><div class="value">{{ overview.customers || 0 }}</div><div class="hint">储值余额 ¥{{ money(overview.storedBalance) }}</div></div>
          <div class="stat-card"><div class="label">技师 / 门店</div><div class="value">{{ overview.technicians || 0 }} / {{ overview.stores || 0 }}</div><div class="hint">服务能力概览</div></div>
          <div class="stat-card"><div class="label">待办</div><div class="value">{{ overview.pendingFollowUps || 0 }}</div><div class="hint">低库存 {{ overview.lowStock || 0 }} 项</div></div>
        </div>
        <el-row :gutter="16" class="mt-16">
          <el-col :span="16">
            <div class="chart-card">
              <div class="card-title">近 30 天营业额趋势</div>
              <Chart :option="trendOption" height="320px" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="chart-card">
              <div class="card-title">支付方式占比</div>
              <Chart :option="payOption" height="320px" />
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="预约分析" name="appointments">
        <el-row :gutter="14" class="mb-16">
          <el-col :span="6"><div class="stat-card"><div class="label">预约总数</div><div class="value">{{ appt.summary?.total || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">到店/服务</div><div class="value">{{ appt.summary?.arrived || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">未到店</div><div class="value">{{ appt.summary?.noShow || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">取消</div><div class="value">{{ appt.summary?.cancelled || 0 }}</div></div></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="14"><div class="chart-card"><div class="card-title">每日预约</div><Chart :option="apptDayOption" height="300px" /></div></el-col>
          <el-col :span="10"><div class="chart-card"><div class="card-title">状态分布</div><Chart :option="apptStatusOption" height="300px" /></div></el-col>
        </el-row>
        <el-table :data="appt.byStore" stripe border class="mt-16">
          <el-table-column prop="storeName" label="门店" min-width="180" />
          <el-table-column prop="total" label="预约数" width="120" />
          <el-table-column prop="arrived" label="到店数" width="120" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="收银分析" name="orders">
        <el-row :gutter="14" class="mb-16">
          <el-col :span="6"><div class="stat-card"><div class="label">订单数</div><div class="value">{{ orders.summary?.orders || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">营业额</div><div class="value">¥{{ money(orders.summary?.revenue) }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">客单价</div><div class="value">¥{{ money(orders.summary?.avgOrder) }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">优惠合计</div><div class="value">¥{{ money(orders.summary?.discount) }}</div></div></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="14"><div class="chart-card"><div class="card-title">每日营业额</div><Chart :option="orderDayOption" height="300px" /></div></el-col>
          <el-col :span="10"><div class="chart-card"><div class="card-title">品类收入</div><Chart :option="bizOption" height="300px" /></div></el-col>
        </el-row>
        <el-row :gutter="16" class="mt-16">
          <el-col :span="12">
            <div class="chart-card"><div class="card-title">支付方式</div>
              <el-table :data="orders.byPayType" size="small" border>
                <el-table-column prop="payType" label="方式" width="120" />
                <el-table-column prop="orders" label="笔数" width="100" />
                <el-table-column label="金额"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
              </el-table>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="chart-card"><div class="card-title">门店收入</div>
              <el-table :data="orders.byStore" size="small" border>
                <el-table-column prop="storeName" label="门店" min-width="140" />
                <el-table-column prop="orders" label="订单" width="90" />
                <el-table-column label="金额"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
              </el-table>
            </div>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="会员分析" name="members">
        <el-row :gutter="14" class="mb-16">
          <el-col :span="6"><div class="stat-card"><div class="label">会员总数</div><div class="value">{{ members.summary?.customers || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">30 天活跃</div><div class="value">{{ members.summary?.activeCustomers || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">储值余额</div><div class="value">¥{{ money(members.summary?.storedBalance) }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">会员卡数</div><div class="value">{{ members.summary?.cards || 0 }}</div><div class="hint">30 天内到期 {{ members.summary?.expiringCards || 0 }}</div></div></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><div class="chart-card"><div class="card-title">会员等级分布</div><Chart :option="levelOption" height="300px" /></div></el-col>
          <el-col :span="12"><div class="chart-card"><div class="card-title">卡种销售</div>
            <el-table :data="members.cardSales" size="small" border>
              <el-table-column prop="cardType" label="卡种" min-width="150" />
              <el-table-column prop="sold" label="销量" width="100" />
              <el-table-column label="金额"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
            </el-table>
          </div></el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="技师分析" name="technicians">
        <el-table :data="techs.list" stripe border>
          <el-table-column prop="techNo" label="编号" width="140" />
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="position" label="岗位" width="130" />
          <el-table-column prop="serviceCount" label="服务次数" width="110" />
          <el-table-column label="累计业绩" width="130"><template #default="{ row }">¥{{ money(row.totalRevenue) }}</template></el-table-column>
          <el-table-column label="已结算提成" width="130"><template #default="{ row }">¥{{ money(row.commission) }}</template></el-table-column>
          <el-table-column prop="rating" label="评分" width="90" />
          <el-table-column label="轮牌状态" width="110">
            <template #default="{ row }"><el-tag :type="row.rotationStatus === 'ON' ? 'success' : 'info'" size="small">{{ row.rotationStatus === 'ON' ? '接单中' : '休息' }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="营销分析" name="marketing">
        <el-row :gutter="16">
          <el-col :span="8">
            <div class="chart-card"><div class="card-title">优惠券状态</div>
              <el-table :data="marketing.coupons" size="small" border>
                <el-table-column prop="status" label="状态" /><el-table-column prop="total" label="数量" />
              </el-table>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="chart-card"><div class="card-title">分销佣金</div>
              <el-table :data="marketing.distributions" size="small" border>
                <el-table-column prop="status" label="状态" />
                <el-table-column prop="total" label="笔数" width="80" />
                <el-table-column label="金额"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
              </el-table>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="chart-card"><div class="card-title">评价概览</div>
              <div class="review-stat">
                <div class="big">{{ Number(marketing.reviews?.avgScore || 0).toFixed(1) }}</div>
                <div class="text-muted">平均评分 / 共 {{ marketing.reviews?.total || 0 }} 条</div>
                <el-rate :model-value="Number(marketing.reviews?.avgScore || 0)" disabled />
              </div>
            </div>
          </el-col>
        </el-row>
        <div class="chart-card mt-16"><div class="card-title">活动类型分布</div>
          <el-table :data="marketing.activities" size="small" border>
            <el-table-column prop="type" label="活动类型" /><el-table-column prop="total" label="数量" />
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="结算分析" name="settlements">
        <el-row :gutter="14" class="mb-16">
          <el-col :span="6"><div class="stat-card"><div class="label">结算单</div><div class="value">{{ settle.summary?.total || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">结算收入</div><div class="value">¥{{ money(settle.summary?.revenue) }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">技师提成</div><div class="value">¥{{ money(settle.summary?.techCommission) }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card"><div class="label">平台抽成</div><div class="value">¥{{ money(settle.summary?.platformShare) }}</div></div></el-col>
        </el-row>
        <el-table :data="settle.list" size="small" stripe border>
          <el-table-column prop="settlementNo" label="结算单号" width="180" />
          <el-table-column label="周期" width="200"><template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template></el-table-column>
          <el-table-column label="收入" width="120"><template #default="{ row }">¥{{ money(row.revenue) }}</template></el-table-column>
          <el-table-column label="技师提成" width="120"><template #default="{ row }">¥{{ money(row.techCommission) }}</template></el-table-column>
          <el-table-column label="状态" width="100"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Printer, Refresh, Search } from '@element-plus/icons-vue'
import request from '@/api/request'
import Chart from '@/components/Chart.vue'
import { money, download, statusText } from '@/utils/format'

const tab = ref('overview')
const range = ref<[string, string] | null>(null)
const storeId = ref<number | null>(null)
const bizType = ref('')
const stores = ref<any[]>([])
const bizTypes = ref<any[]>([])
const overview = ref<any>({})
const trend = ref<any[]>([])
const appt = ref<any>({})
const orders = ref<any>({})
const members = ref<any>({})
const techs = ref<any>({})
const marketing = ref<any>({})
const settle = ref<any>({})

function params() {
  const p: any = {}
  if (range.value) { p.start = range.value[0]; p.end = range.value[1] }
  if (storeId.value) p.storeId = storeId.value
  if (bizType.value) p.bizType = bizType.value
  return p
}
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 50, right: 20, top: 30, bottom: 40 },
  xAxis: { type: 'category', data: trend.value.map((r) => r.d) },
  yAxis: { type: 'value' },
  series: [{ name: '营业额', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: trend.value.map((r) => r.amount) }]
}))
const payOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie', radius: ['42%', '68%'],
    data: (orders.value.byPayType || []).map((r: any) => ({ name: r.payType || '其他', value: r.amount }))
  }]
}))
const apptDayOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 45, right: 20, top: 30, bottom: 40 },
  xAxis: { type: 'category', data: (appt.value.byDay || []).map((r: any) => r.d) },
  yAxis: { type: 'value' },
  series: [{ name: '预约', type: 'bar', barMaxWidth: 26, itemStyle: { color: '#6b4eff' }, data: (appt.value.byDay || []).map((r: any) => r.total) }]
}))
const apptStatusOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie', radius: ['40%', '66%'],
    data: (appt.value.byStatus || []).map((r: any) => ({ name: statusText(r.status), value: r.total }))
  }]
}))
const orderDayOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 55, right: 20, top: 30, bottom: 40 },
  xAxis: { type: 'category', data: (orders.value.byDay || []).map((r: any) => r.d) },
  yAxis: { type: 'value' },
  series: [{ name: '营业额', type: 'bar', barMaxWidth: 26, itemStyle: { color: '#8f7bff' }, data: (orders.value.byDay || []).map((r: any) => r.amount) }]
}))
const bizOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie', radius: ['40%', '66%'],
    data: (orders.value.byBizType || []).map((r: any) => ({ name: r.bizType || '未分类', value: r.amount }))
  }]
}))
const levelOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{
    type: 'pie', radius: ['40%', '66%'],
    data: (members.value.levelDistribution || []).map((r: any) => ({ name: r.level, value: r.total }))
  }]
}))

async function loadBase() {
  const [st, bt] = await Promise.all([request.get('/tenant/stores/list'), request.get('/tenant/biz-types')]) as any[]
  stores.value = st.data || []
  bizTypes.value = bt.data || []
}
async function loadAll() {
  try {
    const p = params()
    const [o, ord, ap, mem, te, mk, se] = await Promise.all([
      request.get('/tenant/reports/overview'),
      request.get('/tenant/reports/orders', { params: p }),
      request.get('/tenant/reports/appointments', { params: p }),
      request.get('/tenant/reports/members'),
      request.get('/tenant/reports/technicians'),
      request.get('/tenant/reports/marketing'),
      request.get('/tenant/reports/settlements')
    ]) as any[]
    overview.value = o.data || {}
    orders.value = ord.data || {}
    appt.value = ap.data || {}
    members.value = mem.data || {}
    techs.value = te.data || {}
    marketing.value = mk.data || {}
    settle.value = se.data || {}
    const tr: any = await request.get('/tenant/reports/orders', { params: p })
    trend.value = (tr.data?.byDay || [])
  } catch (e) { /* handled by interceptor */ }
}
function exportExcel() {
  const type = tab.value === 'orders' ? 'orders' : tab.value === 'members' ? 'customers' : tab.value === 'technicians' ? 'technicians' : 'settlements'
  download('/api/tenant/reports/export', { type, start: range.value?.[0], end: range.value?.[1] })
  ElMessage.success('开始导出')
}
function printPdf() { download('/api/tenant/pdf/report') }
onMounted(async () => { await loadBase(); loadAll() })
</script>

<style scoped>
.chart-card { background: #fff; border: 1px solid #f0f1f7; border-radius: 12px; padding: 16px; }
.card-title { font-weight: 600; margin-bottom: 12px; }
.review-stat { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 200px; gap: 8px; }
.review-stat .big { font-size: 48px; font-weight: 700; color: #6b4eff; }
</style>
