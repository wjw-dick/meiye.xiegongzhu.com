<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt, timeOnly, statusText } from '../api'

const tabs = [
  { key: '', label: '全部' },
  { key: 'CONFIRMED', label: '待到店' },
  { key: 'ARRIVED', label: '已到店' },
  { key: 'SERVING', label: '服务中' },
  { key: 'DONE', label: '已完成' }
]
const active = ref('')
const date = ref('')
const list = ref<any[]>([])
const loading = ref(true)
const toast = ref('')

async function load() {
  loading.value = true
  try {
    const p: any = await api('/api/worker/appointments/page', { query: { page: 1, size: 50, status: active.value, date: date.value } })
    list.value = p.records || []
  } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function act(a: any, action: string, label: string) {
  if (!confirm(`确认${label}？`)) return
  try {
    await api(`/api/worker/appointments/${a.id}/${action}`, { method: 'POST' })
    toast.value = `${label}成功`
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
      <div class="field"><input v-model="date" type="date" @change="load" /></div>
      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!list.length" class="empty">暂无预约</div>
      <div v-else class="list">
        <div v-for="a in list" :key="a.id" class="card">
          <div class="row between">
            <b>{{ timeOnly(a.startTime) }} - {{ timeOnly(a.endTime) }}</b>
            <span class="pill" :class="{ green: a.status === 'DONE', orange: a.status === 'SERVING' }">{{ statusText[a.status] || a.status }}</span>
          </div>
          <div class="small muted" style="margin-top:5px">{{ (a.startTime || '').slice(0, 10) }} · 预约号 {{ a.appointmentNo }}</div>
          <div class="small" style="margin-top:6px">客户 #{{ a.customerId }} · 门店 #{{ a.storeId }}</div>
          <div class="small muted" v-if="a.remark" style="margin-top:4px">备注：{{ a.remark }}</div>
          <div class="row" style="margin-top:10px;gap:8px">
            <button v-if="['PENDING','CONFIRMED'].includes(a.status)" class="btn sm" @click="act(a, 'arrive', '确认到店')">确认到店</button>
            <button v-if="a.status === 'ARRIVED'" class="btn sm" @click="act(a, 'start', '开始服务')">开始服务</button>
            <button v-if="a.status === 'SERVING'" class="btn sm" @click="act(a, 'done', '完成服务')">完成服务</button>
            <RouterLink class="btn gray sm" :to="`/logs?appointmentId=${a.id}`">填日志</RouterLink>
          </div>
        </div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
