<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api, dt } from '../api'

const route = useRoute()
const list = ref<any[]>([])
const loading = ref(true)
const showForm = ref(false)
const appointmentId = ref<number | null>(route.query.appointmentId ? Number(route.query.appointmentId) : null)
const content = ref('')
const duration = ref('')
const toast = ref('')
const busy = ref(false)

async function load() {
  loading.value = true
  try { list.value = await api('/api/worker/service-logs', { query: { appointmentId: route.query.appointmentId } }) } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function submit() {
  if (!content.value.trim()) { toast.value = '请填写服务内容'; return }
  busy.value = true
  try {
    await api('/api/worker/service-logs', { method: 'POST', body: { appointmentId: appointmentId.value, content: content.value, duration: duration.value ? Number(duration.value) : null } })
    toast.value = '日志已保存'
    content.value = ''
    duration.value = ''
    showForm.value = false
    load()
  } catch (e: any) { toast.value = e.message } finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <div class="section">
    <button class="btn" @click="showForm = !showForm">{{ showForm ? '收起' : '写服务日志' }}</button>
    <div class="card" style="margin-top:12px" v-if="showForm">
      <div class="field"><label>关联预约 ID（可选）</label><input v-model="appointmentId" inputmode="numeric" placeholder="如 12" /></div>
      <div class="field"><label>服务内容</label><textarea v-model="content" placeholder="记录服务过程、使用产品与客户反馈"></textarea></div>
      <div class="field"><label>服务时长（分钟）</label><input v-model="duration" inputmode="numeric" placeholder="如 60" /></div>
      <button class="btn" :disabled="busy" @click="submit">{{ busy ? '保存中…' : '保存日志' }}</button>
    </div>

    <div class="sec-head" style="margin-top:18px"><h3>日志记录</h3></div>
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无服务日志</div>
    <div v-else class="list">
      <div v-for="l in list" :key="l.id" class="card">
        <div class="row between">
          <b class="small">服务日志 #{{ l.id }}</b>
          <span class="small muted">{{ dt(l.createdAt) }}</span>
        </div>
        <p style="margin:8px 0 0;font-size:13px">{{ l.content }}</p>
        <div class="small muted" style="margin-top:6px" v-if="l.duration">时长 {{ l.duration }} 分钟</div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
