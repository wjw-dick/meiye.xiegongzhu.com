<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, money } from '../api'

const data = ref<any>({})
const loading = ref(true)
const income = computed(() => data.value.income || {})
const reviews = computed(() => data.value.reviews || {})

onMounted(async () => {
  try { data.value = await api('/api/worker/performance') } finally { loading.value = false }
})
</script>

<template>
  <div class="section">
    <div v-if="loading" class="loading">加载中…</div>
    <template v-else>
      <div class="card" style="text-align:center;padding:22px">
        <div class="small muted">本月提成</div>
        <div style="font-size:34px;font-weight:800;color:var(--brand)">{{ money(data.month) }}</div>
        <div class="small muted">本月服务 {{ data.monthServices || 0 }} 单 · 今日 {{ data.todayServices || 0 }} 单</div>
      </div>
      <div class="card" style="margin-top:12px">
        <div class="kv"><span>今日提成</span><b>{{ money(data.today) }}</b></div>
        <div class="kv"><span>本周提成</span><b>{{ money(data.week) }}</b></div>
        <div class="kv"><span>累计提成</span><span class="price">{{ money(data.total) }}</span></div>
        <div class="kv"><span>累计提现</span><span>{{ money(income.withdrawing) }}</span></div>
        <div class="kv"><span>可提现余额</span><span class="price">{{ money(income.available) }}</span></div>
        <div class="kv"><span>客户评分</span><span>★ {{ Number(reviews.avgScore || 0).toFixed(2) }}（{{ reviews.total || 0 }} 条）</span></div>
      </div>

      <div class="sec-head" style="margin-top:18px"><h3>结算明细</h3><RouterLink to="/withdrawals">提现</RouterLink></div>
      <div v-if="!(income.records || []).length" class="card muted small">暂无结算明细</div>
      <div v-else class="list">
        <div v-for="r in income.records" :key="r.id" class="card">
          <div class="row between">
            <b class="small">{{ r.settlementNo }}</b>
            <span class="pill">{{ r.status }}</span>
          </div>
          <div class="row between small" style="margin-top:6px">
            <span class="muted">{{ r.startDate }} ~ {{ r.endDate }}</span>
            <span>提成 <b class="price">{{ money(r.commissionAmount) }}</b></span>
          </div>
        </div>
      </div>
      <RouterLink to="/withdrawals" class="btn" style="margin-top:16px">申请提现</RouterLink>
    </template>
  </div>
</template>
