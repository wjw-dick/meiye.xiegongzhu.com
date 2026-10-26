<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, statusText } from '../api'

const list = ref<any[]>([])
const loading = ref(true)
onMounted(async () => { try { list.value = await api('/api/worker/certs') } finally { loading.value = false } })
</script>

<template>
  <div class="section">
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无证书，请联系店长录入</div>
    <div v-else class="list">
      <div v-for="c in list" :key="c.id" class="card">
        <div class="row between">
          <b>{{ c.certType }}</b>
          <span class="pill" :class="{ red: c.status === 'EXPIRED', orange: c.status === 'WARN' }">{{ statusText[c.status] || c.status }}</span>
        </div>
        <div class="small muted" style="margin-top:5px">证书编号 {{ c.certNo }}</div>
        <div class="small muted">发证机构 {{ c.issuer }}</div>
        <div class="small" :style="{ color: c.status === 'EXPIRED' ? '#c22f4a' : '#4c5a58' }">有效期至 {{ c.expireAt }}</div>
      </div>
    </div>
  </div>
</template>
