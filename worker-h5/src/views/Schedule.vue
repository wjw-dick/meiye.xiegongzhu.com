<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api } from '../api'

const list = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  try { list.value = await api('/api/worker/schedules') } finally { loading.value = false }
})
</script>

<template>
  <div class="section">
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无排班</div>
    <div v-else class="timeline">
      <div v-for="s in list" :key="s.id" class="tl-item">
        <div class="card">
          <div class="row between">
            <b>{{ s.workDate }}</b>
            <span class="pill" :class="{ gray: s.status !== 'WORK' }">{{ s.status === 'WORK' ? '出勤' : '休息' }}</span>
          </div>
          <div class="small" style="margin-top:6px">{{ s.startTime }} - {{ s.endTime }}</div>
          <div class="small muted">门店 #{{ s.storeId }} · {{ s.bizType || '全部品类' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
