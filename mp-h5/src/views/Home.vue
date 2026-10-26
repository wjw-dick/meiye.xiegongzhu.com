<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, auth, money, dt, statusText, bizName } from '../api'

const loading = ref(true)
const error = ref('')
const data = ref<any>({})
const customer = computed(() => data.value.customer || {})
const cards = computed(() => data.value.cards || [])
const appointments = computed(() => data.value.appointments || [])

const shortcuts = [
  { label: '在线预约', icon: '✚', to: '/booking' },
  { label: '我的预约', icon: '◷', to: '/appointments' },
  { label: '会员卡包', icon: '▣', to: '/cards' },
  { label: '优惠券', icon: '◈', to: '/coupons' },
  { label: '积分商城', icon: '✦', to: '/points' },
  { label: '消息通知', icon: '✉', to: '/messages' },
  { label: '服务评价', icon: '★', to: '/reviews' },
  { label: '售后退款', icon: '↺', to: '/after-sales' }
]

onMounted(async () => {
  try { data.value = await api('/api/customer/home') }
  catch (e: any) { error.value = e.message }
  finally { loading.value = false }
})
</script>

<template>
  <div>
    <section class="hero">
      <div class="hero-row">
        <div class="avatar">{{ (customer.name || '会').slice(0, 1) }}</div>
        <div>
          <h2>Hi，{{ customer.name || '会员' }}</h2>
          <div class="sub">{{ customer.level || '普通会员' }} · 成长值 {{ customer.growth || 0 }}</div>
        </div>
      </div>
      <div class="hero-stats">
        <div class="hero-stat"><b>{{ customer.points || 0 }}</b><span>积分</span></div>
        <div class="hero-stat"><b>{{ cards.length }}</b><span>会员卡</span></div>
        <div class="hero-stat"><b>{{ (data.coupons || []).length }}</b><span>优惠券</span></div>
      </div>
    </section>

    <div class="section">
      <div class="card" style="margin-top:-38px;position:relative;z-index:2">
        <div class="row between" style="margin-bottom:10px">
          <b>快捷服务</b>
          <RouterLink to="/items" class="small muted">全部项目 ›</RouterLink>
        </div>
        <div class="grid4">
          <RouterLink v-for="s in shortcuts" :key="s.label" :to="s.to" class="quick">
            <span class="quick-icon">{{ s.icon }}</span>
            <span>{{ s.label }}</span>
          </RouterLink>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="error" class="empty">{{ error }}</div>
    <template v-else>
      <div class="section" v-if="appointments.length">
        <div class="sec-head"><h3>即将到来</h3><RouterLink to="/appointments">全部</RouterLink></div>
        <div class="list">
          <RouterLink v-for="a in appointments.slice(0, 2)" :key="a.id" to="/appointments" class="card">
            <div class="row between">
              <b>{{ dt(a.startTime) }}</b>
              <span class="pill">{{ statusText[a.status] || a.status }}</span>
            </div>
            <div class="small muted" style="margin-top:4px">预约号 {{ a.appointmentNo }}</div>
          </RouterLink>
        </div>
      </div>

      <div class="section">
        <div class="sec-head"><h3>臻选项目</h3><RouterLink to="/items">更多</RouterLink></div>
        <div class="list">
          <RouterLink v-for="it in (data.items || []).slice(0, 4)" :key="it.id" :to="`/booking?itemId=${it.id}`" class="item-card">
            <img v-if="it.image" :src="it.image" class="item-img" :alt="it.name" />
            <div v-else class="item-img" style="display:grid;place-items:center;color:#c99">美</div>
            <div style="flex:1;min-width:0">
              <div class="row between">
                <h4>{{ it.name }}</h4>
                <span class="pill">{{ bizName[it.bizType] || it.bizType }}</span>
              </div>
              <div class="desc">{{ it.description || '专业服务' }}</div>
              <div class="row between" style="margin-top:6px">
                <span class="price">{{ money(it.price) }}</span>
                <span class="small muted">{{ it.duration }} 分钟</span>
              </div>
            </div>
          </RouterLink>
        </div>
      </div>

      <div class="section">
        <div class="sec-head"><h3>会员卡包</h3><RouterLink to="/cards">全部</RouterLink></div>
        <div v-if="!cards.length" class="card muted small">暂无会员卡，到店可咨询办理。</div>
        <div v-else class="list">
          <RouterLink v-for="c in cards.slice(0, 2)" :key="c.id" to="/cards" class="card">
            <div class="row between">
              <b>{{ c.name }}</b>
              <span class="pill">{{ c.type === 'TIMES' ? '次卡' : '储值卡' }}</span>
            </div>
            <div class="row between small muted" style="margin-top:6px">
              <span>卡号 {{ c.cardNo }}</span>
              <span>余额 {{ money(c.balance) }} / {{ c.times || 0 }} 次</span>
            </div>
          </RouterLink>
        </div>
      </div>

      <div class="section" v-if="(data.notices || []).length">
        <div class="sec-head"><h3>门店公告</h3></div>
        <div class="list">
          <div v-for="n in data.notices" :key="n.id" class="card">
            <b style="font-size:13.5px">{{ n.title }}</b>
            <div class="small muted" style="margin-top:3px">{{ n.summary }}</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.grid4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px 6px; }
.quick { text-align: center; font-size: 11.5px; color: #51485a; display: flex; flex-direction: column; align-items: center; gap: 5px; }
.quick-icon { width: 38px; height: 38px; border-radius: 12px; background: var(--brand-soft); color: var(--brand); display: grid; place-items: center; font-size: 18px; }
</style>
