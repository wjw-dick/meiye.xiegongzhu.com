<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, money, dt } from '../api'

const cards = ref<any[]>([])
const loading = ref(true)
const active = ref<any>(null)
const flows = ref<any[]>([])
const rechargeAmount = ref('')
const toast = ref('')
const busy = ref(false)

async function load() {
  loading.value = true
  try { cards.value = await api('/api/customer/cards') } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function openFlows(c: any) {
  active.value = c
  try { flows.value = await api(`/api/customer/cards/${c.id}/flows`) } catch { flows.value = [] }
}

async function recharge() {
  const amount = Number(rechargeAmount.value)
  if (!active.value || !amount || amount <= 0) { toast.value = '请输入充值金额'; return }
  busy.value = true
  try {
    await api(`/api/customer/cards/${active.value.id}/recharge`, { method: 'POST', query: { amount } })
    toast.value = '充值成功'
    rechargeAmount.value = ''
    await load()
    await openFlows(cards.value.find((c: any) => c.id === active.value.id))
  } catch (e: any) { toast.value = e.message } finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <div class="section">
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!cards.length" class="empty">暂无会员卡</div>
    <div v-else class="list">
      <div v-for="c in cards" :key="c.id" class="card card-visual">
        <div class="row between">
          <b>{{ c.name }}</b>
          <span class="pill">{{ c.type === 'TIMES' ? '次卡' : '储值卡' }}</span>
        </div>
        <div class="card-no">{{ c.cardNo }}</div>
        <div class="row between" style="margin-top:14px">
          <div><div class="small" style="opacity:.75">余额</div><b>{{ money(c.balance) }}</b></div>
          <div style="text-align:right"><div class="small" style="opacity:.75">剩余次数</div><b>{{ c.times || 0 }}</b></div>
        </div>
        <div class="row between small" style="margin-top:10px;opacity:.8">
          <span>折扣 {{ c.discount || 100 }}%</span>
          <span>有效期至 {{ (c.expireAt || '').slice(0, 10) }}</span>
        </div>
        <button class="btn ghost sm" style="margin-top:12px" @click="openFlows(c)">查看明细 / 充值</button>
      </div>
    </div>

    <div v-if="active" class="card" style="margin-top:14px">
      <div class="row between"><b>充值 {{ active.name }}</b><button class="btn gray sm" @click="active = null">关闭</button></div>
      <div class="field" style="margin-top:12px">
        <label>充值金额（元）</label>
        <input v-model="rechargeAmount" inputmode="decimal" placeholder="如 1000" />
      </div>
      <button class="btn" :disabled="busy" @click="recharge">{{ busy ? '处理中…' : '确认充值' }}</button>
      <div style="margin-top:18px">
        <b style="font-size:13.5px">消费明细</b>
        <div v-if="!flows.length" class="small muted" style="padding:12px 0">暂无明细</div>
        <div v-for="f in flows" :key="f.id" class="kv">
          <span>{{ f.type }} · {{ dt(f.createdAt) }}</span>
          <span :style="{ color: Number(f.amount) >= 0 ? '#178a52' : '#c22f4a' }">{{ Number(f.amount) >= 0 ? '+' : '' }}{{ money(f.amount) }}</span>
        </div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>

<style scoped>
.card-visual { background: linear-gradient(135deg, #7c2149, #c22f6b); color: #fff; }
.card-visual .pill { background: rgba(255,255,255,.2); color: #fff; }
.card-no { letter-spacing: 2px; font-size: 12px; opacity: .8; margin-top: 6px; }
</style>
