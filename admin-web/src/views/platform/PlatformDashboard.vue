<template>
  <div>
    <div class="page-header">
      <div>
        <h2 class="page-title">平台经营看板</h2>
        <div class="page-subtitle">跨租户营收、租户增长、品类分布与平台抽成概览（只读统计）</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新数据</el-button>
        <el-button :icon="Download" @click="exportExcel">导出租户报表</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="label">{{ s.label }}</div>
        <div class="value">{{ s.value }}</div>
        <div class="hint">{{ s.hint }}</div>
      </div>
    </div>

    <el-row :gutter="16" class="mt-16">
      <el-col :span="16">
        <div class="page-card">
          <div class="card-title">平台近 7 天营收趋势</div>
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

    <el-row :gutter="16" class="mt-16">
      <el-col :span="14">
        <div class="page-card">
          <div class="card-title flex-between">租户经营排行 <el-button link type="primary" @click="$router.push('/platform/tenants')">租户管理</el-button></div>
          <el-table :data="data.tenantRank" stripe size="small">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="code" label="编码" width="100" />
            <el-table-column prop="name" label="租户" min-width="160" />
            <el-table-column prop="customers" label="客户" width="80" />
            <el-table-column prop="orders" label="订单" width="80" />
            <el-table-column label="营收" width="120"><template #default="{ row }">¥{{ money(row.revenue) }}</template></el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :span="10">
        <div class="page-card">
          <div class="card-title">月度营收</div>
          <Chart :option="monthOption" height="280px" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import Chart from '@/components/Chart.vue'
import { money, download } from '@/utils/format'

const data = ref<any>({})
const report = ref<any>({})
const expiring = ref<any[]>([])
const stats = computed(() => [
  { label: '租户总数', value: data.value.tenants || 0, hint: `正常运营 ${data.value.normalTenants || 0}` },
  { label: '30天内到期', value: expiring.value.length, hint: expiring.value.length ? expiring.value.slice(0, 2).map((t: any) => t.code).join('、') + ' 待续期' : '暂无到期风险' },
  { label: '平台客户', value: data.value.customers || 0, hint: '跨租户去重统计' },
  { label: '平台技师', value: data.value.technicians || 0, hint: `门店 ${data.value.stores || 0} 家` },
  { label: '预约履约率', value: `${data.value.fulfillRate ?? 0}%`, hint: `预约 ${data.value.appointments || 0} 单` },
  { label: '平台订单', value: data.value.orders || 0, hint: '全部租户累计' },
  { label: '平台营收', value: `¥${money(data.value.revenue)}`, hint: '已支付/已核销订单' },
  { label: '储值余额', value: `¥${money(data.value.storedBalance)}`, hint: '全平台会员卡余额' },
  { label: '平台抽成', value: `¥${money(data.value.settlement || report.value.summary?.platformShare)}`, hint: '已生成结算单' }
])
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 55, right: 20, top: 30, bottom: 40 },
  xAxis: { type: 'category', data: (data.value.trend || []).map((r: any) => r.d) },
  yAxis: { type: 'value' },
  series: [{ name: '营收', type: 'line', smooth: true, areaStyle: { opacity: .18 }, data: (data.value.trend || []).map((r: any) => r.amount) }]
}))
const bizOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['40%', '66%'], data: (data.value.bizDistribution || []).map((r: any) => ({ name: r.biz_type, value: r.tenants })) }]
}))
const monthOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 55, right: 20, top: 30, bottom: 40 },
  xAxis: { type: 'category', data: (report.value.byMonth || []).slice().reverse().map((r: any) => r.month) },
  yAxis: { type: 'value' },
  series: [{ name: '营收', type: 'bar', barMaxWidth: 30, itemStyle: { color: '#6b4eff' }, data: (report.value.byMonth || []).slice().reverse().map((r: any) => r.revenue) }]
}))

async function load() {
  const [d, r, e] = await Promise.all([
    request.get('/platform/dashboard'),
    request.get('/platform/reports/overview'),
    request.get('/platform/tenants/expiring', { params: { days: 30 } })
  ]) as any[]
  data.value = d.data || {}
  report.value = r.data || {}
  expiring.value = e.data || []
}
function exportExcel() { download('/api/platform/reports/export') }
onMounted(load)
</script>

<style scoped>
.card-title { font-size: 15px; font-weight: 600; margin-bottom: 12px; }
</style>
