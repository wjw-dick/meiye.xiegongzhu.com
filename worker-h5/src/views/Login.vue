<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, auth, setAuth } from '../api'

const route = useRoute()
const router = useRouter()
const tenantCode = ref((route.query.tenant as string) || auth.tenantCode || '5MY')
const phone = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

onMounted(() => { if (auth.token) router.replace('/home') })

async function login() {
  error.value = ''
  if (!tenantCode.value || !phone.value || !password.value) { error.value = '请填写门店编码、手机号和密码'; return }
  loading.value = true
  try {
    const data = await api<any>('/api/auth/login', {
      method: 'POST',
      body: { username: phone.value.trim(), password: password.value, tenantCode: tenantCode.value.trim().toUpperCase(), userType: 'TECHNICIAN' }
    })
    setAuth(data.token, tenantCode.value.trim().toUpperCase(), data.user)
    router.replace((route.query.redirect as string) || '/home')
  } catch (e: any) { error.value = e.message } finally { loading.value = false }
}

function demo() { tenantCode.value = '5MY'; phone.value = '13900000001'; password.value = '123456' }
</script>

<template>
  <div class="login-wrap">
    <div class="login-logo">技</div>
    <h1 class="login-title">技师工作台</h1>
    <p class="login-sub">排班 · 接单 · 核销 · 业绩</p>
    <div class="login-card">
      <div class="field"><label>门店编码</label><input v-model="tenantCode" autocapitalize="characters" placeholder="如 5MY" /></div>
      <div class="field"><label>手机号</label><input v-model="phone" type="tel" maxlength="11" placeholder="请输入手机号" /></div>
      <div class="field"><label>密码</label><input v-model="password" type="password" placeholder="请输入密码" @keyup.enter="login" /></div>
      <p class="error" v-if="error">{{ error }}</p>
      <button class="btn" :disabled="loading" @click="login">{{ loading ? '登录中…' : '登录' }}</button>
      <p class="login-tip" @click="demo">演示账号：13900000001 / 123456（点击填入）</p>
    </div>
  </div>
</template>
