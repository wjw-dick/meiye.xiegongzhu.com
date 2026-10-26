<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, auth, clearAuth, money } from '../api'

const router = useRouter()
const data = ref<any>({})
const editing = ref(false)
const form = ref<any>({ name: '', gender: '女', birthday: '', remark: '' })
const toast = ref('')
const customer = computed(() => data.value.customer || {})

const menus = [
  { label: '我的预约', icon: '◷', to: '/appointments' },
  { label: '我的订单', icon: '☰', to: '/orders' },
  { label: '会员卡包', icon: '▣', to: '/cards' },
  { label: '优惠券', icon: '◈', to: '/coupons' },
  { label: '积分明细', icon: '✦', to: '/points' },
  { label: '消息通知', icon: '✉', to: '/messages' },
  { label: '服务评价', icon: '★', to: '/reviews' },
  { label: '售后服务', icon: '↺', to: '/after-sales' }
]

async function load() {
  try {
    data.value = await api('/api/customer/profile')
    form.value = {
      name: customer.value.name || '',
      gender: customer.value.gender || '女',
      birthday: customer.value.birthday || '',
      remark: customer.value.remark || ''
    }
  } catch (e: any) { toast.value = e.message }
}

async function save() {
  try {
    await api('/api/customer/profile', { method: 'PUT', body: form.value })
    toast.value = '资料已更新'
    editing.value = false
    load()
  } catch (e: any) { toast.value = e.message }
}

function logout() {
  if (!confirm('确认退出登录？')) return
  clearAuth()
  router.replace('/login')
}

onMounted(load)
</script>

<template>
  <div>
    <section class="hero">
      <div class="hero-row">
        <div class="avatar">{{ (customer.name || '会').slice(0, 1) }}</div>
        <div style="flex:1">
          <h2>{{ customer.name || '会员' }}</h2>
          <div class="sub">{{ customer.level || '普通会员' }} · {{ customer.customerNo }}</div>
        </div>
        <button class="btn sm ghost" style="border-color:rgba(255,255,255,.5);color:#fff;background:transparent" @click="editing = !editing">编辑</button>
      </div>
      <div class="hero-stats">
        <div class="hero-stat"><b>{{ customer.points || 0 }}</b><span>积分</span></div>
        <div class="hero-stat"><b>{{ (data.cards || []).length }}</b><span>会员卡</span></div>
        <div class="hero-stat"><b>{{ customer.growth || 0 }}</b><span>成长值</span></div>
      </div>
    </section>

    <div class="section" v-if="editing">
      <div class="card">
        <div class="field"><label>姓名</label><input v-model="form.name" /></div>
        <div class="field">
          <label>性别</label>
          <select v-model="form.gender"><option>女</option><option>男</option><option>其他</option></select>
        </div>
        <div class="field"><label>生日</label><input v-model="form.birthday" type="date" /></div>
        <div class="field"><label>备注</label><textarea v-model="form.remark" placeholder="偏好、禁忌等"></textarea></div>
        <button class="btn" @click="save">保存资料</button>
      </div>
    </div>

    <div class="section" v-if="(data.cards || []).length">
      <div class="sec-head"><h3>我的会员卡</h3><RouterLink to="/cards">全部</RouterLink></div>
      <div class="list">
        <div v-for="c in (data.cards || []).slice(0, 2)" :key="c.id" class="card">
          <div class="row between"><b>{{ c.name }}</b><span class="pill">{{ money(c.balance) }}</span></div>
          <div class="small muted" style="margin-top:4px">卡号 {{ c.cardNo }} · 剩余 {{ c.times || 0 }} 次</div>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="menu-list">
        <RouterLink v-for="m in menus" :key="m.label" :to="m.to" class="menu-item">
          <span><span class="menu-icon">{{ m.icon }}</span>{{ m.label }}</span>
          <span class="arrow">›</span>
        </RouterLink>
      </div>
    </div>

    <div class="section">
      <div class="menu-list">
        <RouterLink to="/password" class="menu-item"><span><span class="menu-icon">⚿</span>修改密码</span><span class="arrow">›</span></RouterLink>
        <div class="menu-item" @click="logout"><span><span class="menu-icon">⏻</span>退出登录</span><span class="arrow">›</span></div>
      </div>
      <p class="small muted" style="text-align:center;margin-top:14px">门店编码 {{ auth.tenantCode }} · v1.0.0</p>
    </div>
    <div class="toast" v-if="toast" @click="toast = ''">{{ toast }}</div>
  </div>
</template>
