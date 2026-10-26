<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt, statusText, money } from '../api'

const tabs = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待确认' },
  { key: 'CONFIRMED', label: '已确认' },
  { key: 'DONE', label: '已完成' },
  { key: 'CANCELLED', label: '已取消' }
]
const active = ref('')
const list = ref<any[]>([])
const loading = ref(true)
const toast = ref('')

async function load() {
  loading.value = true
  try {
    const p: any = await api('/api/customer/appointments/page', { query: { page: 1, size: 50, status: active.value } })
    list.value = p.records || []
  } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function cancel(a: any) {
  if (!confirm('确认取消该预约？')) return
  try {
    await api(`/api/customer/appointments/${a.id}/cancel?reason=客户取消`, { method: 'POST' })
    toast.value = '已取消预约'
    load()
  } catch (e: any) { toast.value = e.message }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="tabs-line">
      <button v-for="t in tabs" :key="t.key" :class="{ active: active === t.key }" @click="active = t.key; load()">{{ t.label }}</button>
    </div>
    <div class="section">
      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!list.length" class="empty">暂无预约记录</div>
      <div v-else class="list">
        <div v-for="a in list" :key="a.id" class="card">
          <div class="row between">
            <b>{{ dt(a.startTime) }}</b>
            <span class="pill" :class="{ green: a.status === 'DONE', red: a.status === 'CANCELLED', gray: a.status === 'NO_SHOW' }">{{ statusText[a.status] || a.status }}</span>
          </div>
          <div class="small muted" style="margin-top:6px">预约号 {{ a.appointmentNo }}</div>
          <div class="row between small" style="margin-top:8px">
            <span>门店 #{{ a.storeId }} · 技师 #{{ a.technicianId || '-' }}</span>
            <span class="price">{{ money(a.amount) }}</span>
          </div>
          <div class="row" style="margin-top:10px;gap:8px" v-if="['PENDING','CONFIRMED'].includes(a.status)">
            <button class="btn ghost sm" @click="cancel(a)">取消预约</button>
            <RouterLink class="btn gray sm" to="/items">再约一次</RouterLink>
          </div>
          <div class="small muted" style="margin-top:8px" v-if="a.remark">备注：{{ a.remark }}</div>
        </div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
