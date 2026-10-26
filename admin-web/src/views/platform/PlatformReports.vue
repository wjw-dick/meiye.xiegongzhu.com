<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">平台报表</h2>
        <div class="page-subtitle">跨租户 / 跨品类经营对比、平台抽成与月度趋势，只读统计</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Download" @click="exportExcel">导出 Excel</el-button>
      </div>
    </div>

    <div class="stat-grid mb-16">
      <div class="stat-card"><div class="label">租户总数</div><div class="value">{{ summary.tenants || 0 }}</div><div class="hint">正常运营 {{ summary.normalTenants || 0 }}</div></div>
      <div class="stat-card"><div class="label">平台客户</div><div class="value">{{ summary.customers || 0 }}</div></div>
      <div class="stat-card"><div class="label">平台订单</div><div class="value">{{ summary.orders || 0 }}</div></div>
      <div class="stat-card"><div class="label">平台营收</div><div class="value">¥{{ money(summary.revenue) }}</div></div>
      <div class="stat-card"><div class="label">储值余额</div><div class="value">¥{{ money(summary.storedBalance) }}</div></div>
      <div class="stat-card"><div class="label">平台抽成</div><div class="value">¥{{ money(summary.platformShare) }}</div></div>
    </div>

    <el-row :gutter="16">
      <el-col :span="14">
        <div class="chart-card">
          <div class="card-title">租户营收对比</div>
          <Chart :option="tenantOption" height="320px" />
        </div>
      </el-col>
      <el-col :span="10">
        <div class="chart-card">
          <div class="card-title">月度营收趋势</div>
          <Chart :option="monthOption" height="320px" />
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <el-col :span="10">
        <div class="chart-card">
          <div class="card-title">品类收入分布</div>
          <Chart :option="bizOption" height="300px" />
        </div>
      </el-col>
      <el-col :span="14">
        <div class="chart-card">
          <div class="card-title">租户经营明细</div>
          <el-table :data="compare" size="small" border max-height="300">
            <el-table-column prop="code" label="编码" width="90" />
            <el-table-column prop="name" label="租户" min-width="150" />
            <el-table-column prop="customers" label="客户" width="80" />
            <el-table-column prop="orders" label="订单" width="80" />
            <el-table-column label="营收" width="110"><template #default="{ row }">¥{{ money(row.revenue) }}</template></el-table-column>
            <el-table-column label="平台抽成" width="110"><template #default="{ row }">¥{{ money(row.platformShare) }}</template></el-table-column>
            <el-table-column prop="technicians" label="技师" width="70" />
          </el-table>
        </div>
      </el-col>
    </el-row>

    <div class="chart-card mt-16">
      <div class="card-title">品类启用租户数</div>
      <el-table :data="overview.byBizType" size="small" border>
        <el-table-column prop="bizType" label="品类" width="160" />
        <el-table-column prop="tenants" label="启用租户数" width="140" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Refresh, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import Chart from '@/components/Chart.vue'
import { money, download } from '@/utils/format'

const overview = ref<any>({})
const compare = ref<any[]>([])
const summary = computed(() => overview.value.summary || {})
const tenantOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 80, right: 30, top: 30, bottom: 30 },
  xAxis: { type: 'value' },
  yAxis: { type: 'category', data: (overview.value.byTenant || []).map((r: any) => r.name) },
  series: [{ type: 'bar', barMaxWidth: 18, itemStyle: { color: '#6b4eff' }, data: (overview.value.byTenant || []).map((r: any) => r.revenue) }]
}))
const monthOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 55, right: 20, top: 30, bottom: 40 },
  xAxis: { type: 'category', data: (overview.value.byMonth || []).slice().reverse().map((r: any) => r.month) },
  yAxis: { type: 'value' },
  series: [{ type: 'line', smooth: true, areaStyle: { opacity: .15 }, data: (overview.value.byMonth || []).slice().reverse().map((r: any) => r.revenue) }]
}))
const bizOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [{ type: 'pie', radius: ['40%', '66%'], data: (overview.value.bizTypeRevenue || []).map((r: any) => ({ name: r.bizType || '未分类', value: r.revenue })) }]
}))

async function load() {
  const [o, c] = await Promise.all([request.get('/platform/reports/overview'), request.get('/platform/reports/compare')]) as any[]
  overview.value = o.data || {}
  compare.value = c.data || []
}
function exportExcel() { download('/api/platform/reports/export') }
onMounted(load)
</script>

<style scoped>
.chart-card { background: #fff; border: 1px solid #f0f1f7; border-radius: 12px; padding: 16px; }
.card-title { font-weight: 600; margin-bottom: 12px; }
</style>
