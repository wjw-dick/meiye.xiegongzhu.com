<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ auth.isPlatform ? '平台经营看板' : '门店经营看板' }}</h2>
        <div class="page-subtitle">{{ auth.isPlatform ? '跨租户 / 跨品类实时统计，支持下钻租户只读查看' : '本租户预约、收银、会员、技师与预警概览' }}</div>
      </div>
      <el-button :icon="Refresh" @click="load">刷新数据</el-button>
    </div>

    <template v-if="auth.isPlatform && !auth.drillTenant">
      <div class="stat-grid">
        <div class="stat-card" v-for="s in platformStats" :key="s.label">
          <div class="label">{{ s.label }}</div>
          <div class="value">{{ s.value }}</div>
          <div class="hint">{{ s.hint }}</div>
        </div>
      </div>
      <el-row :gutter="16" class="mt-16">
        <el-col :span="16">
          <div class="page-card">
            <div class="card-title">平台营收趋势（近 7 天）</div>
            <Chart :option="trendOption" height="300px" />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="page-card">
            <div class="card-title">品类启用分布</div>
            <Chart :option="bizOption" height="300px" />
          </div>
        </el-col>
      </el-row>
      <div class="page-card mt-16">
        <div class="card-title">租户经营排行</div>
        <el-table :data="tenantRank" stripe>
          <el-table-column type="index" label="#" width="60" />
          <el-table-column prop="code" label="租户编码" width="120" />
          <el-table-column prop="name" label="租户名称" min-width="180" />
          <el-table-column prop="customers" label="客户数" width="110" />
          <el-table-column prop="orders" label="订单数" width="110" />
          <el-table-column label="营收" width="140">
            <template #default="{ row }">¥{{ money(row.revenue) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <template v-else>
      <div class="stat-grid">
        <div class="stat-card" v-for="s in tenantStats" :key="s.label">
          <div class="label">{{ s.label }}</div>
          <div class="value">{{ s.value }}</div>
          <div class="hint">{{ s.hint }}</div>
        </div>
      </div>
      <el-row :gutter="16" class="mt-16">
        <el-col :span="16">
          <div class="page-card">
            <div class="card-title">近期营业额趋势</div>
            <Chart :option="tenantTrendOption" height="300px" />
          </div>
        </el-col>
        <el-col :span="8">
          <div class="page-card">
            <div class="card-title">预约状态分布</div>
            <Chart :option="apptOption" height="300px" />
          </div>
        </el-col>
      </el-row>
      <el-row :gutter="16" class="mt-16">
        <el-col :span="12">
          <div class="page-card">
            <div class="card-title">待办提醒</div>
            <div class="todo-list">
              <div class="todo-item">
                <span>今日预约</span><b>{{ data.todayAppointments || 0 }}</b>
              </div>
              <div class="todo-item">
                <span>待回访任务</span><b>{{ data.pendingFollowUps || 0 }}</b>
              </div>
              <div class="todo-item">
                <span>库存不足</span><b>{{ data.lowStock || 0 }}</b>
              </div>
              <div class="todo-item">
                <span>预约履约率</span><b>{{ data.fulfillRate || 0 }}%</b>
              </div>
            </div>
            <el-button class="mt-16" @click="$router.push('/warnings')">查看预警清单</el-button>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="page-card">
            <div class="card-title">快捷入口</div>
            <div class="quick-grid">
              <div class="quick" @click="$router.push('/appointments')"><el-icon><Calendar /></el-icon>预约看板</div>
              <div class="quick" @click="$router.push('/orders')"><el-icon><ShoppingCart /></el-icon>收银订单</div>
              <div class="quick" @click="$router.push('/member-cards')"><el-icon><CreditCard /></el-icon>会员卡</div>
              <div class="quick" @click="$router.push('/settlements')"><el-icon><Coin /></el-icon>结算分账</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'
import { useAuthStore } from '@/stores/auth'
import Chart from '@/components/Chart.vue'

const auth = useAuthStore()
const data = ref<any>({})
const platform = ref<any>({})
const tenantRank = ref<any[]>([])
const orderReport = ref<any>({})
const apptReport = ref<any>({})

const platformStats = computed(() => [
  { label: '租户总数', value: platform.value.tenants ?? 0, hint: `正常 ${platform.value.normalTenants ?? 0} 家` },
  { label: '客户总数', value: platform.value.customers ?? 0, hint: '跨租户汇总' },
  { label: '订单总数', value: platform.value.orders ?? 0, hint: '全平台订单' },
  { label: '平台营收', value: `¥${money(platform.value.revenue)}`, hint: '已支付订单' },
  { label: '储值余额', value: `¥${money(platform.value.storedBalance)}`, hint: '有效会员卡' },
  { label: '平台抽成', value: `¥${money(platform.value.platformShare)}`, hint: '结算分账累计' }
])

const tenantStats = computed(() => [
  { label: '今日营业额', value: `¥${money(data.value.todayRevenue)}`, hint: '已支付/核销' },
  { label: '今日预约', value: data.value.todayAppointments ?? 0, hint: `累计 ${data.value.appointments ?? 0} 单` },
  { label: '客户总数', value: data.value.customers ?? 0, hint: '本租户会员' },
  { label: '储值余额', value: `¥${money(data.value.storedBalance)}`, hint: '有效会员卡' },
  { label: '累计营收', value: `¥${money(data.value.revenue)}`, hint: '已支付订单' },
  { label: '客单价', value: `¥${money(data.value.avgOrder)}`, hint: '平均订单金额' }
])

const trendOption = computed(() => {
  const rows = platform.value.trend || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 24, right: 24, top: 28, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r: any) => String(r.d).slice(5)) },
    yAxis: [{ type: 'value', name: '营业额' }, { type: 'value', name: '订单数' }],
    series: [
      { name: '营业额', type: 'line', smooth: true, areaStyle: { opacity: 0.12 }, data: rows.map((r: any) => r.amount), itemStyle: { color: '#6b4eff' } },
      { name: '订单数', type: 'bar', yAxisIndex: 1, barWidth: 12, data: rows.map((r: any) => r.orders), itemStyle: { color: '#c9b8ff' } }
    ]
  }
})

const bizOption = computed(() => {
  const rows = platform.value.bizDistribution || []
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      data: rows.map((r: any) => ({ name: r.biz_type || r.bizType, value: r.tenants })),
      label: { formatter: '{b}\n{c}' }
    }]
  }
})

const tenantTrendOption = computed(() => {
  const rows = orderReport.value.byDay || []
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 24, right: 24, top: 28, bottom: 24, containLabel: true },
    xAxis: { type: 'category', data: rows.map((r: any) => String(r.d).slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '营业额', type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, data: rows.map((r: any) => r.amount), itemStyle: { color: '#6b4eff' } },
      { name: '订单数', type: 'bar', barWidth: 14, data: rows.map((r: any) => r.orders), itemStyle: { color: '#e3d9ff' } }
    ]
  }
})

const apptOption = computed(() => {
  const rows = apptReport.value.byStatus || []
  const map: Record<string, string> = {
    PENDING: '待确认', CONFIRMED: '已确认', ARRIVED: '已到店', SERVING: '服务中',
    DONE: '已完成', CANCELLED: '已取消', NO_SHOW: '未到店'
  }
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '66%'], center: ['50%', '44%'],
      data: rows.map((r: any) => ({ name: map[r.status] || r.status, value: r.total }))
    }]
  }
})

function money(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

async function load() {
  if (auth.isPlatform && !auth.drillTenant) {
    const res: any = await request.get('/platform/dashboard')
    platform.value = res.data || {}
    tenantRank.value = res.data?.tenantRank || []
  } else {
    const [d, o, a] = await Promise.all([
      request.get('/tenant/dashboard'),
      request.get('/tenant/reports/orders'),
      request.get('/tenant/reports/appointments')
    ])
    data.value = (d as any).data || {}
    orderReport.value = (o as any).data || {}
    apptReport.value = (a as any).data || {}
  }
}

onMounted(load)
</script>

<style scoped>
.card-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.todo-list { display: flex; flex-direction: column; gap: 10px; }
.todo-item { display: flex; justify-content: space-between; padding: 12px 14px; border-radius: 10px; background: #f7f6ff; font-size: 14px; }
.todo-item b { color: #6b4eff; }
.quick-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.quick {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 8px;
  height: 92px; border-radius: 12px; background: #f7f7fb; cursor: pointer; font-size: 13px; color: #4a4f63;
  transition: all 0.2s;
}
.quick:hover { background: #efeaff; color: #6b4eff; transform: translateY(-2px); }
.quick .el-icon { font-size: 22px; }
</style>
