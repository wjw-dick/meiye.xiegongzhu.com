<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt, statusText, money } from '../api'

const tabs = [
  { key: '', label: '全部' },
  { key: 'PENDING', label: '待支付' },
  { key: 'PAID', label: '已支付' },
  { key: 'VERIFIED', label: '已核销' },
  { key: 'CANCELLED', label: '已取消' }
]
const active = ref('')
const list = ref<any[]>([])
const loading = ref(true)
const toast = ref('')
const paying = ref<number | null>(null)

async function load() {
  loading.value = true
  try {
    const p: any = await api('/api/customer/orders/page', { query: { page: 1, size: 50, status: active.value } })
    list.value = (p.records || []).map((r: any) => r.order || r)
  } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function pay(o: any) {
  if (!confirm(`确认支付 ${money(o.paidAmount ?? o.totalAmount)}？`)) return
  paying.value = o.id
  try {
    await api(`/api/customer/orders/${o.id}/pay`, { method: 'POST', body: { payType: 'WECHAT' } })
    toast.value = '支付成功'
    load()
  } catch (e: any) { toast.value = e.message } finally { paying.value = null }
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
      <div v-else-if="!list.length" class="empty">暂无订单</div>
      <div v-else class="list">
        <div v-for="o in list" :key="o.id" class="card">
          <div class="row between">
            <b class="small">{{ o.orderNo }}</b>
            <span class="pill" :class="{ green: ['PAID','VERIFIED','DONE'].includes(o.status), gray: o.status === 'CANCELLED' }">{{ statusText[o.status] || o.status }}</span>
          </div>
          <div class="row between" style="margin-top:8px">
            <span class="muted small">{{ dt(o.createdAt) }}</span>
            <span class="price">{{ money(o.paidAmount || o.totalAmount) }}</span>
          </div>
          <div class="row between" style="margin-top:10px">
            <span class="small muted">支付方式：{{ o.payType || '未支付' }}</span>
            <button v-if="o.status === 'PENDING'" class="btn sm" :disabled="paying === o.id" @click="pay(o)">
              {{ paying === o.id ? '支付中…' : '立即支付' }}
            </button>
          </div>
        </div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
