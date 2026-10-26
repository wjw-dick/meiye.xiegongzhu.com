<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, auth, clearAuth, money } from '../api'

const router = useRouter()
const data = ref<any>({})
const editing = ref(false)
const form = ref({ bio: '', avatar: '', skills: '' })
const toast = ref('')
const tech = computed(() => data.technician || {})
const income = ref<any>({})

const menus = [
  { label: '我的排班', icon: '◷', to: '/schedule' },
  { label: '业绩提成', icon: '↗', to: '/performance' },
  { label: '提现管理', icon: '¥', to: '/withdrawals' },
  { label: '服务日志', icon: '✎', to: '/logs' },
  { label: '客户评价', icon: '★', to: '/reviews' },
  { label: '资质证书', icon: '✓', to: '/certs' },
  { label: '消息通知', icon: '✉', to: '/messages' }
]

async function load() {
  try {
    data.value = await api('/api/worker/profile')
    form.value = { bio: tech.value.bio || '', avatar: tech.value.avatar || '', skills: tech.value.skills || '' }
    try { income.value = await api('/api/worker/performance') } catch { /* ignore */ }
  } catch (e: any) { toast.value = e.message }
}

async function save() {
  try {
    await api('/api/worker/profile', { method: 'PUT', body: form.value })
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
        <div class="avatar">
          <img v-if="tech.avatar" :src="tech.avatar" style="width:100%;height:100%;object-fit:cover" alt="" />
          <span v-else>{{ (tech.name || '技').slice(0, 1) }}</span>
        </div>
        <div style="flex:1">
          <h2>{{ tech.name }}</h2>
          <div class="sub">{{ tech.position }} · {{ tech.techNo }}</div>
        </div>
        <button class="btn sm" style="background:rgba(255,255,255,.2)" @click="editing = !editing">编辑</button>
      </div>
      <div class="hero-stats">
        <div class="hero-stat"><b>{{ tech.serviceCount || 0 }}</b><span>服务次数</span></div>
        <div class="hero-stat"><b>★ {{ tech.rating || '-' }}</b><span>评分</span></div>
        <div class="hero-stat"><b>{{ money(income.income?.available) }}</b><span>可提现</span></div>
      </div>
    </section>

    <div class="section" v-if="editing">
      <div class="card">
        <div class="field"><label>头像地址</label><input v-model="form.avatar" placeholder="https://..." /></div>
        <div class="field"><label>擅长品类</label><input v-model="form.skills" placeholder="BEAUTY,SKIN" /></div>
        <div class="field"><label>个人简介</label><textarea v-model="form.bio" placeholder="介绍你的专业经验"></textarea></div>
        <button class="btn" @click="save">保存资料</button>
      </div>
    </div>

    <div class="section">
      <div class="card">
        <div class="kv"><span>所属门店</span><span>#{{ tech.storeId }}</span></div>
        <div class="kv"><span>入职时间</span><span>{{ (tech.entryAt || '').slice(0, 10) }}</span></div>
        <div class="kv"><span>证书状态</span><span>{{ tech.certStatus }}</span></div>
        <div class="kv"><span>个人简介</span><span style="max-width:60%;text-align:right">{{ tech.bio || '-' }}</span></div>
      </div>
    </div>

    <div class="section">
      <div class="menu-list">
        <RouterLink v-for="m in menus" :key="m.label" :to="m.to" class="menu-item">
          <span><span class="menu-icon">{{ m.icon }}</span>{{ m.label }}</span><span class="arrow">›</span>
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
