<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt, money } from '../api'

const list = ref<any[]>([])
const orders = ref<any[]>([])
const loading = ref(true)
const showForm = ref(false)
const type = ref('REFUND')
const reason = ref('')
const orderId = ref<number | null>(null)
const toast = ref('')
const busy = ref(false)

const typeText: Record<string, string> = { REFUND: '退款', RERUN: '返工', COMPLAINT: '投诉' }
const statusText: Record<string, string> = { PENDING: '待处理', PROCESSING: '处理中', DONE: '已完成', CLOSED: '已关闭' }

async function load() {
  loading.value = true
  try {
    const [a, o] = await Promise.all([api('/api/customer/after-sales'), api('/api/customer/orders/page', { query: { page: 1, size: 30 } })])
    list.value = a
    orders.value = (o.records || []).map((r: any) => r.order || r)
    if (orders.value.length) orderId.value = orders.value[0].id
  } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function submit() {
  if (!reason.value.trim()) { toast.value = '请填写申请原因'; return }
  busy.value = true
  try {
    await api('/api/customer/after-sales', { method: 'POST', body: { type: type.value, reason: reason.value, orderId: orderId.value } })
    toast.value = '售后申请已提交'
    showForm.value = false
    reason.value = ''
    load()
  } catch (e: any) { toast.value = e.message } finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <div class="section">
    <button class="btn" @click="showForm = !showForm">{{ showForm ? '收起' : '申请售后' }}</button>
    <div class="card" style="margin-top:12px" v-if="showForm">
      <div class="field">
        <label>售后类型</label>
        <select v-model="type">
          <option value="REFUND">退款</option>
          <option value="RERUN">返工</option>
          <option value="COMPLAINT">投诉建议</option>
        </select>
      </div>
      <div class="field">
        <label>关联订单</label>
        <select v-model="orderId">
          <option v-for="o in orders" :key="o.id" :value="o.id">{{ o.orderNo }} · {{ money(o.paidAmount || o.totalAmount) }}</option>
        </select>
      </div>
      <div class="field"><label>申请原因</label><textarea v-model="reason" placeholder="请描述遇到的问题"></textarea></div>
      <button class="btn" :disabled="busy" @click="submit">{{ busy ? '提交中…' : '提交申请' }}</button>
    </div>

    <div class="sec-head" style="margin-top:18px"><h3>售后记录</h3></div>
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无售后记录</div>
    <div v-else class="list">
      <div v-for="a in list" :key="a.id" class="card">
        <div class="row between">
          <b>{{ typeText[a.type] || a.type }}</b>
          <span class="pill" :class="{ gray: a.status === 'DONE', green: a.status === 'DONE' }">{{ statusText[a.status] || a.status }}</span>
        </div>
        <p style="margin:8px 0 4px;font-size:13px">{{ a.reason }}</p>
        <div class="small muted">{{ dt(a.createdAt) }}</div>
        <div class="small" style="margin-top:6px;color:var(--brand)" v-if="a.result">处理结果：{{ a.result }}</div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
