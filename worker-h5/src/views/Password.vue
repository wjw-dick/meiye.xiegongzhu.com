<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api'

const router = useRouter()
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const busy = ref(false)
const error = ref('')
const ok = ref('')

async function submit() {
  error.value = ''
  ok.value = ''
  if (!oldPassword.value || !newPassword.value) { error.value = '请填写完整'; return }
  if (newPassword.value.length < 6) { error.value = '新密码至少 6 位'; return }
  if (newPassword.value !== confirmPassword.value) { error.value = '两次输入的新密码不一致'; return }
  busy.value = true
  try {
    await api('/api/auth/password', { method: 'POST', body: { oldPassword: oldPassword.value, newPassword: newPassword.value } })
    ok.value = '密码修改成功'
    setTimeout(() => router.back(), 800)
  } catch (e: any) { error.value = e.message } finally { busy.value = false }
}
</script>

<template>
  <div class="section">
    <div class="card">
      <div class="field"><label>原密码</label><input v-model="oldPassword" type="password" /></div>
      <div class="field"><label>新密码</label><input v-model="newPassword" type="password" /></div>
      <div class="field"><label>确认新密码</label><input v-model="confirmPassword" type="password" /></div>
      <p class="error" v-if="error">{{ error }}</p>
      <p class="small" style="color:#178a52" v-if="ok">{{ ok }}</p>
      <button class="btn" :disabled="busy" @click="submit">{{ busy ? '提交中…' : '确认修改' }}</button>
    </div>
  </div>
</template>
