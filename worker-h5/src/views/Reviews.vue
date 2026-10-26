<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt } from '../api'

const list = ref<any[]>([])
const loading = ref(true)
onMounted(async () => { try { list.value = await api('/api/worker/reviews') } finally { loading.value = false } })
</script>

<template>
  <div class="section">
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无客户评价</div>
    <div v-else class="list">
      <div v-for="r in list" :key="r.id" class="card">
        <div class="row between">
          <b style="color:#e0a13c">{{ '★'.repeat(r.score) }}</b>
          <span class="small muted">{{ dt(r.createdAt) }}</span>
        </div>
        <p style="margin:8px 0 0;font-size:13px">{{ r.content }}</p>
        <div class="small" style="margin-top:6px;color:var(--brand)" v-if="r.reply">门店回复：{{ r.reply }}</div>
      </div>
    </div>
  </div>
</template>
