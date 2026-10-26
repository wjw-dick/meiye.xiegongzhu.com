<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, dt } from '../api'

const data = ref<any>({ balance: 0, growth: 0, records: [] })
const loading = ref(true)

onMounted(async () => {
  try { data.value = await api('/api/customer/points') } finally { loading.value = false }
})
</script>

<template>
  <div>
    <div class="points-hero">
      <div class="small" style="opacity:.8">可用积分</div>
      <div class="points-num">{{ data.balance || 0 }}</div>
      <div class="small" style="opacity:.8">成长值 {{ data.growth || 0 }} · 100 积分可抵 1 元</div>
    </div>
    <div class="section">
      <div class="sec-head"><h3>积分明细</h3><RouterLink to="/items">去消费赚积分</RouterLink></div>
      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!(data.records || []).length" class="empty">暂无积分记录</div>
      <div v-else class="card">
        <div v-for="r in data.records" :key="r.id" class="kv">
          <div>
            <div>{{ r.remark || r.type }}</div>
            <div class="small muted">{{ dt(r.createdAt) }}</div>
          </div>
          <b :style="{ color: Number(r.points) >= 0 ? '#178a52' : '#c22f4a' }">{{ Number(r.points) > 0 ? '+' : '' }}{{ r.points }}</b>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.points-hero { background: linear-gradient(140deg,#6e1839,#b4235a); color: #fff; padding: 30px 18px; text-align: center; }
.points-num { font-size: 44px; font-weight: 800; line-height: 1.2; }
</style>
