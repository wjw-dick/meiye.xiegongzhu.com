<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { api, money, dt, statusText } from '../api'

const list = ref<any[]>([])
const amount = ref('')
const remark = ref('')
const toast = ref('')
const busy = ref(false)
const loading = ref(true)

async function load() {
  loading.value = true
  try { list.value = await api('/api/worker/withdrawals') } catch (e: any) { toast.value = e.message } finally { loading.value = false }
}

async function submit() {
  const v = Number(amount.value)
  if (!v || v <= 0) { toast.value = '请输入提现金额'; return }
  busy.value = true
  try {
    await api('/api/worker/withdrawals', { method: 'POST', body: { amount: v, remark: remark.value } })
    toast.value = '提现申请已提交，等待审核'
    amount.value = ''
    remark.value = ''
    load()
  } catch (e: any) { toast.value = e.message } finally { busy.value = false }
}
onMounted(load)
</script>

<template>
  <div class="section">
    <div class="card">
      <h3 style="font-size:15px">申请提现</h3>
      <div class="field"><label>提现金额（元）</label><input v-model="amount" inputmode="decimal" placeholder="请输入金额" /></div>
      <div class="field"><label>备注</label><input v-model="remark" placeholder="可选" /></div>
      <button class="btn" :disabled="busy" @click="submit">{{ busy ? '提交中…' : '提交申请' }}</button>
      <p class="small muted" style="margin-top:8px">提现将在财务审核后打款，具体到账时间以门店通知为准。</p>
    </div>

    <div class="sec-head" style="margin-top:18px"><h3>提现记录</h3></div>
    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="!list.length" class="empty">暂无提现记录</div>
    <div v-else class="list">
      <div v-for="w in list" :key="w.id" class="card">
        <div class="row between">
          <b>{{ money(w.amount) }}</b>
          <span class="pill" :class="{ green: w.status === 'PAID', red: w.status === 'REJECTED', orange: w.status === 'APPLIED' }">{{ statusText[w.status] || w.status }}</span>
        </div>
        <div class="row between small muted" style="margin-top:6px">
          <span>手续费 {{ money(w.fee) }} · 实得 {{ money(w.actualAmount) }}</span>
          <span>{{ dt(w.createdAt) }}</span>
        </div>
        <div class="small muted" v-if="w.remark" style="margin-top:4px">备注：{{ w.remark }}</div>
      </div>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
