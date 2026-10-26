<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt } from '../api'

const list = ref<any[]>([])
const techs = ref<any[]>([])
const loading = ref(true)
const showForm = ref(false)
const score = ref(5)
const content = ref('')
const techId = ref<number | null>(null)
const toast = ref('')
const busy = ref(false)

async function load() {
  loading.value = true
  try {
    const [r, t] = await Promise.all([api('/api/customer/reviews'), api('/api/customer/technicians')])
    list.value = r
    techs.value = t
    if (techs.value.length) techId.value = techs.value[0].id
  } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function submit() {
  if (!content.value.trim()) { toast.value = '请填写评价内容'; return }
  busy.value = true
  try {
    await api('/api/customer/reviews', { method: 'POST', body: { technicianId: techId.value, score: score.value, content: content.value } })
    toast.value = '评价已提交'
    showForm.value = false
    content.value = ''
    load()
  } catch (e: any) { toast.value = e.message } finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <div class="section">
    <button class="btn" @click="showForm = !showForm">{{ showForm ? '收起' : '写评价' }}</button>
    <div class="card" style="margin-top:12px" v-if="showForm">
      <div class="field">
        <label>选择技师</label>
        <select v-model="techId">
          <option v-for="t in techs" :key="t.id" :value="t.id">{{ t.name }} · {{ t.position }}</option>
        </select>
      </div>
      <div class="field">
        <label>评分</label>
        <div class="stars">
          <span v-for="i in 5" :key="i" :class="{ on: i <= score }" @click="score = i">★</span>
        </div>
      </div>
      <div class="field"><label>评价内容</label><textarea v-model="content" placeholder="分享你的服务体验"></textarea></div>
      <button class="btn" :disabled="busy" @click="submit">{{ busy ? '提交中…' : '提交评价' }}</button>
    </div>

    <div class="sec-head" style="margin-top:18px"><h3>我的评价</h3></div>
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无评价</div>
    <div v-else class="list">
      <div v-for="r in list" :key="r.id" class="card">
        <div class="row between">
          <b>{{ '★'.repeat(r.score) }}</b>
          <span class="pill">{{ r.status === 'PUBLISHED' ? '已发布' : r.status }}</span>
        </div>
        <p style="margin:8px 0 4px">{{ r.content }}</p>
        <div class="small muted">{{ dt(r.createdAt) }}</div>
        <div class="small" style="margin-top:6px;color:var(--brand)" v-if="r.reply">商家回复：{{ r.reply }}</div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>

<style scoped>
.stars span { font-size: 28px; color: #ddd; margin-right: 4px; cursor: pointer; }
.stars span.on { color: #f2b64c; }
</style>
