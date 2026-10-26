<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt } from '../api'

const list = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try { list.value = await api('/api/customer/messages') } finally { loading.value = false }
})
</script>

<template>
  <div class="section">
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无消息</div>
    <div v-else class="list">
      <div v-for="m in list" :key="m.id" class="card">
        <div class="row between">
          <b>{{ m.title }}</b>
          <span class="pill gray">{{ m.channel || '站内' }}</span>
        </div>
        <div style="margin-top:6px;font-size:13px;color:#51485a">{{ m.content }}</div>
        <div class="small muted" style="margin-top:8px">{{ dt(m.sentAt || m.createdAt) }}</div>
      </div>
    </div>
  </div>
</template>
