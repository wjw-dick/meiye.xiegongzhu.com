<template>
  <div>
    <div class="stat-grid mb-16">
      <div class="stat-card">
        <div class="label">佣金记录</div>
        <div class="value">{{ summary.records || 0 }}</div>
        <div class="hint">两级分销：一级 10% / 二级 5%</div>
      </div>
      <div class="stat-card">
        <div class="label">待结算佣金</div>
        <div class="value">¥{{ money(summary.pendingAmount) }}</div>
        <div class="hint">需财务确认结算</div>
      </div>
      <div class="stat-card">
        <div class="label">已结算佣金</div>
        <div class="value">¥{{ money(summary.settledAmount) }}</div>
        <div class="hint">已计入推广收益</div>
      </div>
      <div class="stat-card">
        <div class="label">快捷操作</div>
        <div class="value" style="font-size: 16px; margin-top: 10px">
          <el-button size="small" type="primary" @click="promptSettle">按订单生成佣金</el-button>
        </div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">分销佣金</h2>
          <div class="page-subtitle">推荐关系、订单分佣、结算状态与佣金明细</div>
        </div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="结算状态" clearable style="width: 140px" @change="search">
          <el-option label="待结算" value="PENDING" /><el-option label="已结算" value="SETTLED" />
        </el-select>
        <el-select v-model="query.level" placeholder="分销层级" clearable style="width: 130px" @change="search">
          <el-option label="一级" :value="1" /><el-option label="二级" :value="2" />
        </el-select>
        <el-input v-model="query.orderId" placeholder="订单ID" clearable style="width: 140px" @keyup.enter="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="customerId" label="下单会员ID" width="120" />
        <el-table-column prop="parentCustomerId" label="获得佣金会员ID" width="140" />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column label="层级" width="90">
          <template #default="{ row }"><el-tag :type="row.level === 1 ? 'success' : 'info'" size="small">{{ row.level === 1 ? '一级' : '二级' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="订单金额" width="120"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="比例" width="90"><template #default="{ row }">{{ row.rate }}%</template></el-table-column>
        <el-table-column label="佣金" width="120"><template #default="{ row }"><b class="money">¥{{ money(row.commission) }}</b></template></el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'" size="small">{{ row.status === 'PENDING' ? '待结算' : '已结算' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="时间" width="170"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="settle(row)">结算</el-button>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex-between mt-16">
        <span class="text-muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                       layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const summary = ref<any>({})
const query = reactive<any>({ status: '', level: '', orderId: '' })

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v !== '' && v !== null) params[k] = v })
    const [list, sum] = await Promise.all([
      request.get('/tenant/distributions/page', { params }),
      request.get('/tenant/distributions/summary')
    ]) as any[]
    rows.value = list.data.records || []
    total.value = list.data.total || 0
    summary.value = sum.data || {}
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { status: '', level: '', orderId: '' }); search() }
async function settle(row: any) {
  await ElMessageBox.confirm(`确认结算佣金 ¥${money(row.commission)}？`, '佣金结算', { type: 'warning' })
  await request.post(`/tenant/distributions/${row.id}/settle`)
  ElMessage.success('已结算')
  load()
}
async function promptSettle() {
  const { value } = await ElMessageBox.prompt('请输入订单 ID，系统将按推荐关系生成佣金', '按订单生成佣金', {
    inputPattern: /^\d+$/, inputErrorMessage: '请输入数字订单 ID'
  })
  const res: any = await request.post(`/tenant/distributions/settle/${value}`)
  ElMessage.success(res.data?.message || `生成 ${res.data?.created || 0} 条佣金记录`)
  load()
}
onMounted(load)
</script>

<style scoped>
.money { color: #e6542c; }
</style>
