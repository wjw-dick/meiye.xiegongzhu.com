<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, money, timeOnly, statusText } from '../api'

const dash = ref<any>({})
const profile = ref<any>({})
const loading = ref(true)
const error = ref('')
const tech = computed(() => profile.value.technician || {})
const income = computed(() => dash.value.income || {})

const shortcuts = [
  { label: '我的排班', icon: '◷', to: '/schedule' },
  { label: '我的预约', icon: '☰', to: '/appointments' },
  { label: '订单核销', icon: '▤', to: '/orders' },
  { label: '业绩提成', icon: '↗', to: '/performance' },
  { label: '提现', icon: '¥', to: '/withdrawals' },
  { label: '服务日志', icon: '✎', to: '/logs' },
  { label: '客户评价', icon: '★', to: '/reviews' },
  { label: '资质证书', icon: '✓', to: '/certs' }
]

onMounted(async () => {
  try {
    const [d, p] = await Promise.all([api('/api/worker/dashboard'), api('/api/worker/profile')])
    dash.value = d
    profile.value = p
  } catch (e: any) { error.value = e.message } finally { loading.value = false }
})
</script>

<template>
  <div>
    <section class="hero">
      <div class="hero-row">
        <div class="avatar">
          <img v-if="tech.avatar" :src="tech.avatar" style="width:100%;height:100%;object-fit:cover" alt="" />
          <span v-else>{{ (tech.name || '技').slice(0, 1) }}</span>
        </div>
        <div style="flex:1">
          <h2>{{ tech.name || '技师' }}</h2>
          <div class="sub">{{ tech.position }} · {{ tech.techNo }}</div>
        </div>
        <span class="pill" style="background:rgba(255,255,255,.2);color:#fff">★ {{ tech.rating || '-' }}</span>
      </div>
      <div class="hero-stats">
        <div class="hero-stat"><b>{{ dash.todayAppointments || 0 }}</b><span>今日预约</span></div>
        <div class="hero-stat"><b>{{ dash.servingCount || 0 }}</b><span>服务中</span></div>
        <div class="hero-stat"><b>{{ money(dash.monthCommission) }}</b><span>本月提成</span></div>
      </div>
    </section>

    <div class="section">
      <div class="card" style="margin-top:-36px;position:relative;z-index:2">
        <div class="row between" style="margin-bottom:10px">
          <b>快捷操作</b>
          <span class="small muted">可提现 {{ money(income.available) }}</span>
        </div>
        <div class="grid4">
          <RouterLink v-for="s in shortcuts" :key="s.label" :to="s.to" class="quick">
            <span class="quick-icon">{{ s.icon }}</span><span>{{ s.label }}</span>
          </RouterLink>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中…</div>
    <div v-else-if="error" class="empty">{{ error }}</div>
    <template v-else>
      <div class="section">
        <div class="sec-head"><h3>今日排班</h3><RouterLink to="/schedule">全部</RouterLink></div>
        <div v-if="!(profile.todaySchedules || []).length" class="card muted small">今日暂无排班</div>
        <div v-else class="list">
          <div v-for="s in profile.todaySchedules" :key="s.id" class="card">
            <div class="row between">
              <b>{{ s.startTime }} - {{ s.endTime }}</b>
              <span class="pill" :class="{ gray: s.status !== 'WORK' }">{{ s.status === 'WORK' ? '出勤' : '休息' }}</span>
            </div>
            <div class="small muted" style="margin-top:4px">门店 #{{ s.storeId }} · {{ s.bizType || '全部品类' }}</div>
          </div>
        </div>
      </div>

      <div class="section">
        <div class="sec-head"><h3>收入概览</h3><RouterLink to="/performance">明细</RouterLink></div>
        <div class="card">
          <div class="kv"><span>累计提成</span><b>{{ money(income.totalCommission) }}</b></div>
          <div class="kv"><span>提现中/已提现</span><span>{{ money(income.withdrawing) }}</span></div>
          <div class="kv"><span>可提现</span><span class="price">{{ money(income.available) }}</span></div>
          <div class="kv"><span>服务次数</span><span>{{ tech.serviceCount || 0 }}</span></div>
        </div>
      </div>

      <div class="section">
        <div class="sec-head"><h3>待办提醒</h3></div>
        <div class="card small muted">
          <p v-if="dash.servingCount > 0">有 {{ dash.servingCount }} 个服务正在进行，请及时填写服务日志。</p>
          <p v-else>暂无进行中的服务，保持好状态迎接下一位客人。</p>
          <RouterLink to="/appointments" class="btn ghost sm" style="margin-top:6px">查看今日预约</RouterLink>
        </div>
      </div>
    </template>
  </div>
</template>
