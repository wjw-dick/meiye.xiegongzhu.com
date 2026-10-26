<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, money } from '../api'

const list = ref<any[]>([])
const loading = ref(true)
const tab = ref('UNUSED')

async function load() {
  loading.value = true
  try { list.value = await api('/api/customer/coupons', { query: { status: tab.value } }) } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="tabs-line">
      <button :class="{ active: tab === 'UNUSED' }" @click="tab = 'UNUSED'; load()">可使用</button>
      <button :class="{ active: tab === 'USED' }" @click="tab = 'USED'; load()">已使用</button>
      <button :class="{ active: tab === 'EXPIRED' }" @click="tab = 'EXPIRED'; load()">已过期</button>
    </div>
    <div class="section">
      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!list.length" class="empty">暂无优惠券</div>
      <div v-else class="list">
        <div v-for="c in list" :key="c.id" class="coupon">
          <div class="coupon-left">
            <b>{{ c.type === 'CASH' ? money(c.amount) : (c.discount ? c.discount / 10 + '折' : '优惠') }}</b>
            <span>{{ c.minAmount ? '满' + c.minAmount + '可用' : '无门槛' }}</span>
          </div>
          <div class="coupon-right">
            <b>{{ c.name }}</b>
            <div class="small muted">有效期至 {{ (c.expireAt || '').slice(0, 10) }}</div>
            <span class="pill" :class="{ gray: c.status !== 'UNUSED' }">{{ c.status === 'UNUSED' ? '可使用' : c.status === 'USED' ? '已使用' : '已过期' }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.coupon { display: flex; background: #fff; border-radius: 14px; overflow: hidden; }
.coupon-left { width: 108px; background: linear-gradient(135deg, #b4235a, #d9588b); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 16px 8px; text-align: center; }
.coupon-left b { font-size: 21px; }
.coupon-left span { font-size: 10px; opacity: .85; }
.coupon-right { flex: 1; padding: 14px; }
</style>
