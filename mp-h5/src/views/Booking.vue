<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, money, dt, bizName } from '../api'

const route = useRoute()
const router = useRouter()
const items = ref<any[]>([])
const stores = ref<any[]>([])
const techs = ref<any[]>([])
const slots = ref<any[]>([])
const itemId = ref<number | null>(route.query.itemId ? Number(route.query.itemId) : null)
const storeId = ref<number | null>(null)
const techId = ref<number | null>(null)
const date = ref(new Date(Date.now() + 86400000).toISOString().slice(0, 10))
const slot = ref<any>(null)
const remark = ref('')
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const success = ref<any>(null)
const today = new Date().toISOString().slice(0, 10)

const item = computed(() => items.value.find((i: any) => i.id === itemId.value) || null)

onMounted(async () => {
  try {
    const [i, s] = await Promise.all([api('/api/customer/items'), api('/api/customer/stores')])
    items.value = i
    stores.value = s
    if (stores.value.length) storeId.value = stores.value[0].id
    if (!itemId.value && items.value.length) itemId.value = items.value[0].id
  } catch (e: any) { error.value = e.message }
})

watch([storeId, itemId], async () => {
  techId.value = null
  slot.value = null
  slots.value = []
  if (!storeId.value) return
  try { techs.value = await api('/api/customer/technicians', { query: { storeId: storeId.value, bizType: item.value?.bizType } }) } catch { techs.value = [] }
  loadSlots()
})

watch(techId, loadSlots)

async function loadSlots() {
  if (!storeId.value) return
  loading.value = true
  slot.value = null
  try {
    slots.value = await api('/api/customer/slots', { query: { storeId: storeId.value, itemId: itemId.value, date: date.value, techId: techId.value } })
  } catch { slots.value = [] } finally { loading.value = false }
}

async function submit() {
  error.value = ''
  if (!slot.value) { error.value = '请选择预约时段'; return }
  submitting.value = true
  try {
    success.value = await api('/api/customer/appointments', {
      method: 'POST',
      body: {
        storeId: storeId.value,
        itemId: itemId.value,
        technicianId: slot.value.technicianId || techId.value,
        bizType: item.value?.bizType,
        startTime: slot.value.time,
        remark: remark.value
      }
    })
  } catch (e: any) { error.value = e.message } finally { submitting.value = false }
}
</script>

<template>
  <div>
    <div v-if="success" class="section">
      <div class="card" style="text-align:center;padding:34px 20px">
        <div style="font-size:44px">✓</div>
        <h3>预约成功</h3>
        <p class="muted small">预约号 {{ success.appointmentNo }}</p>
        <p class="muted small">到店时间 {{ dt(success.startTime) }}</p>
        <button class="btn" style="margin-top:14px" @click="router.replace('/appointments')">查看我的预约</button>
        <button class="btn ghost" style="margin-top:10px" @click="success = null">继续预约</button>
      </div>
    </div>
    <template v-else>
      <div class="section">
        <div class="sec-head"><h3>选择项目</h3></div>
        <div class="field">
          <select v-model="itemId">
            <option v-for="it in items" :key="it.id" :value="it.id">{{ it.name }} · {{ money(it.price) }} · {{ it.duration }}分钟</option>
          </select>
        </div>
        <div class="row between small muted" v-if="item">
          <span>{{ bizName[item.bizType] || item.bizType }}</span>
          <span>{{ item.description }}</span>
        </div>
      </div>

      <div class="section">
        <div class="sec-head"><h3>选择门店</h3></div>
        <div class="field">
          <select v-model="storeId">
            <option v-for="s in stores" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="field">
          <label>选择技师（可选）</label>
          <select v-model="techId">
            <option :value="null">不指定，由门店安排</option>
            <option v-for="t in techs" :key="t.id" :value="t.id">{{ t.name }} · {{ t.position }} · ★{{ t.rating }}</option>
          </select>
        </div>
      </div>

      <div class="section">
        <div class="sec-head"><h3>选择日期</h3></div>
        <div class="field"><input v-model="date" type="date" :min="today" @change="loadSlots" /></div>
        <div v-if="loading" class="loading">查询可约时段…</div>
        <div v-else-if="!slots.length" class="empty">当日暂无可约时段，请更换日期或门店</div>
        <div v-else class="chips" style="flex-wrap:wrap;gap:8px">
          <button v-for="(s, i) in slots" :key="i" class="chip" :class="{ active: slot === s }" @click="slot = s">
            {{ dt(s.time).slice(11) }}<span v-if="s.technicianName" class="small"> · {{ s.technicianName }}</span>
          </button>
        </div>
      </div>

      <div class="section">
        <div class="field"><label>备注（可选）</label><textarea v-model="remark" placeholder="过敏史、偏好或其他需求"></textarea></div>
        <p class="error" v-if="error">{{ error }}</p>
        <button class="btn" :disabled="submitting || !slot" @click="submit">{{ submitting ? '提交中…' : slot ? '确认预约' : '请选择时段' }}</button>
        <p class="small muted" style="text-align:center;margin-top:10px">到店前 2 小时可免费取消</p>
      </div>
    </template>
  </div>
</template>
