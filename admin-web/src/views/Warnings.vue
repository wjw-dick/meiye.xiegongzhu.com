<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">到期预警</h2>
        <div class="page-subtitle">证书、会员卡、超时预约、待回访、低库存、欠款六大预警统一清单</div>
      </div>
      <div class="flex gap-8">
        <el-select v-model="days" style="width: 160px" @change="load">
          <el-option label="证书 30 天内" :value="30" /><el-option label="证书 60 天内" :value="60" /><el-option label="证书 90 天内" :value="90" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="stat-grid mb-16">
      <div class="stat-card clickable" @click="tab = 'certificates'">
        <div class="label">证书到期</div><div class="value warning">{{ summary.certificates || 0 }}</div>
      </div>
      <div class="stat-card clickable" @click="tab = 'cards'">
        <div class="label">会员卡到期</div><div class="value warning">{{ summary.cards || 0 }}</div>
      </div>
      <div class="stat-card clickable" @click="tab = 'appointments'">
        <div class="label">超时预约</div><div class="value danger">{{ summary.appointments || 0 }}</div>
      </div>
      <div class="stat-card clickable" @click="tab = 'followUps'">
        <div class="label">待回访</div><div class="value">{{ summary.followUps || 0 }}</div>
      </div>
      <div class="stat-card clickable" @click="tab = 'lowStock'">
        <div class="label">低库存</div><div class="value danger">{{ summary.lowStock || 0 }}</div>
      </div>
      <div class="stat-card clickable" @click="tab = 'debts'">
        <div class="label">未结清欠款</div><div class="value">{{ summary.debts || 0 }}</div>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="证书到期" name="certificates">
        <el-table :data="data.certificates" stripe border>
          <el-table-column prop="technicianName" label="技师" width="130" />
          <el-table-column prop="certType" label="证书类型" width="150" />
          <el-table-column prop="certNo" label="证书编号" min-width="180" />
          <el-table-column label="到期时间" width="140"><template #default="{ row }">{{ dateOnly(row.expireAt) }}</template></el-table-column>
          <el-table-column label="剩余天数" width="120">
            <template #default="{ row }"><el-tag :type="Number(row.remainDays) <= 7 ? 'danger' : 'warning'" size="small">{{ row.remainDays }} 天</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="会员卡到期" name="cards">
        <el-table :data="data.cards" stripe border>
          <el-table-column prop="cardNo" label="卡号" width="180" />
          <el-table-column prop="name" label="卡名称" min-width="150" />
          <el-table-column prop="customerName" label="会员" width="120" />
          <el-table-column label="余额" width="110"><template #default="{ row }">¥{{ money(row.balance) }}</template></el-table-column>
          <el-table-column prop="times" label="剩余次数" width="100" />
          <el-table-column label="到期时间" width="140"><template #default="{ row }">{{ dateOnly(row.expireAt) }}</template></el-table-column>
          <el-table-column label="剩余天数" width="110">
            <template #default="{ row }"><el-tag :type="Number(row.remainDays) <= 3 ? 'danger' : 'warning'" size="small">{{ row.remainDays }} 天</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="超时预约" name="appointments">
        <el-table :data="data.appointments" stripe border>
          <el-table-column prop="appointmentNo" label="预约号" width="180" />
          <el-table-column prop="customerName" label="客户" width="120" />
          <el-table-column label="预约时间" width="170"><template #default="{ row }">{{ fmt(row.startTime) }}</template></el-table-column>
          <el-table-column label="状态" width="110"><template #default="{ row }">{{ statusText(row.status) }}</template></el-table-column>
          <el-table-column label="超时" width="120">
            <template #default="{ row }"><el-tag type="danger" size="small">{{ row.overdueMinutes }} 分钟</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }"><el-button link type="primary" @click="$router.push('/appointments')">去处理</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="待回访" name="followUps">
        <el-table :data="data.followUps" stripe border>
          <el-table-column prop="customerName" label="客户" width="130" />
          <el-table-column prop="type" label="类型" width="120" />
          <el-table-column label="计划时间" width="170"><template #default="{ row }">{{ fmt(row.dueAt) }}</template></el-table-column>
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column label="操作" width="120">
            <template #default><el-button link type="primary" @click="$router.push('/follow-ups')">去回访</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="低库存" name="lowStock">
        <el-table :data="data.lowStock" stripe border>
          <el-table-column prop="name" label="库存品" min-width="160" />
          <el-table-column prop="sku" label="SKU" width="140" />
          <el-table-column label="当前库存" width="120"><template #default="{ row }"><span class="danger">{{ row.stock }}</span></template></el-table-column>
          <el-table-column prop="warnStock" label="预警线" width="110" />
          <el-table-column prop="unit" label="单位" width="90" />
          <el-table-column label="操作" width="120">
            <template #default><el-button link type="primary" @click="$router.push('/inventory')">去补货</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="未结清欠款" name="debts">
        <el-table :data="data.debts" stripe border>
          <el-table-column prop="customerName" label="客户" width="130" />
          <el-table-column label="欠款金额" width="130"><template #default="{ row }"><span class="danger">¥{{ money(row.amount) }}</span></template></el-table-column>
          <el-table-column prop="status" label="状态" width="110" />
          <el-table-column label="创建时间" width="170"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
          <el-table-column label="操作" width="120">
            <template #default><el-button link type="primary" @click="$router.push('/debts')">去收款</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt, dateOnly, statusText } from '@/utils/format'

const days = ref(30)
const tab = ref('certificates')
const data = ref<any>({})
const summary = ref<any>({})

async function load() {
  const res: any = await request.get('/tenant/warnings', { params: { days: days.value } })
  data.value = res.data || {}
  summary.value = res.data?.summary || {}
}
onMounted(load)
</script>

<style scoped>
.stat-card.clickable { cursor: pointer; transition: all .2s; }
.stat-card.clickable:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(107, 78, 255, .12); }
.warning { color: #e6a23c; }
.danger { color: #e6542c; font-weight: 600; }
</style>
