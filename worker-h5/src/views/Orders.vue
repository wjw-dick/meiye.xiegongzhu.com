<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, money, dt, statusText } from '../api'

const tabs = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待支付' },
  { key: 'PAID', label: '待核销' },
  { key: 'VERIFIED', label: '已核销' }
]
const active = ref('')
const list = ref<any[]>([])
const loading = ref(true)
const toast = ref('')

async function load() {
  loading.value = true
  try {
    const p: any = await api('/api/worker/orders/page', { query: { page: 1, size: 50, status: active.value } })
    list.value = p.records || []
  } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function verify(o: any) {
  if (!confirm(`确认核销订单 ${o.orderNo}？`)) return
  try {
    await api(`/api/worker/orders/${o.id}/verify`, { method: 'POST' })
    toast.value = '核销成功'
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
    <div class="section" style="padding-top:0">
      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!list.length" class="empty">暂无订单</div>
      <div v-else class="list">
        <div v-for="o in list" :key="o.id" class="card">
          <div class="row between">
            <b class="small">{{ o.orderNo }}</b>
            <span class="pill" :class="{ green: ['VERIFIED','PAID','DONE'].includes(o.status) }">{{ statusText[o.status] || o.status }}</span>
          </div>
          <div class="row between" style="margin-top:8px">
            <span class="muted small">{{ dt(o.createdAt) }}</span>
            <span class="price">{{ money(o.paidAmount || o.totalAmount) }}</span>
          </div>
          <button v-if="o.status === 'PAID'" class="btn sm" style="margin-top:10px" @click="verify(o)">扫码核销</button>
        </div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
