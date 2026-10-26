<template>
  <div class="login-page">
    <div class="login-left">
      <div class="brand">
        <div class="logo">美</div>
        <span>美业大健康数字化平台</span>
      </div>
      <h1>业务品类插件化<br />SaaS 全链路经营系统</h1>
      <p>多租户隔离 · 两级 RBAC · 预约排班 · 收银核销 · 会员储值 · 技师结算 · 营销库存 · 多端小程序 / 公众号 / App</p>
      <div class="feature-grid">
        <div class="feature"><el-icon><Lock /></el-icon><span>行级租户隔离</span></div>
        <div class="feature"><el-icon><Cpu /></el-icon><span>品类插件开关</span></div>
        <div class="feature"><el-icon><Calendar /></el-icon><span>智能预约排班</span></div>
        <div class="feature"><el-icon><Coin /></el-icon><span>多方分账结算</span></div>
      </div>
      <div class="deco deco-1"></div>
      <div class="deco deco-2"></div>
    </div>
    <div class="login-right">
      <div class="login-card">
        <h2>欢迎登录</h2>
        <p class="sub">请选择端类型并使用账号登录</p>
        <el-tabs v-model="tab" stretch>
          <el-tab-pane label="租户后台" name="tenant" />
          <el-tab-pane label="平台管理" name="platform" />
        </el-tabs>
        <el-form :model="form" label-position="top" @keyup.enter="submit">
          <el-form-item v-if="tab === 'tenant'" label="租户编码">
            <el-input v-model="form.tenantCode" placeholder="如 5MY / 2HL" size="large">
              <template #prefix><el-icon><OfficeBuilding /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="请输入账号" size="large">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" size="large">
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
            登录
          </el-button>
        </el-form>
        <div class="demo-tip">
          <div>演示账号</div>
          <div>平台：<code>admin / admin123</code></div>
          <div>租户：<code>5MY · admin / admin123</code></div>
        </div>
      </div>
      <div class="copyright">© 2026 美业大健康数字化平台 · 业务品类插件化 SaaS</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const tab = ref<'tenant' | 'platform'>('tenant')
const loading = ref(false)
const form = reactive({ tenantCode: '5MY', username: 'admin', password: 'admin123' })

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (tab.value === 'tenant' && !form.tenantCode) {
    ElMessage.warning('请输入租户编码')
    return
  }
  loading.value = true
  try {
    const payload: any = { username: form.username, password: form.password }
    if (tab.value === 'tenant') payload.tenantCode = form.tenantCode.trim().toUpperCase()
    await auth.login(payload)
    ElMessage.success('登录成功')
    if (tab.value === 'platform') {
      auth.setDrillTenant('', '')
      router.push('/platform/dashboard')
    } else {
      router.push('/dashboard')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; min-height: 100%; background: #fff; }
.login-left {
  flex: 1.15; background: linear-gradient(135deg, #191c2e 0%, #2d2350 55%, #4a2f7a 100%);
  color: #fff; padding: 56px 64px; position: relative; overflow: hidden; display: flex; flex-direction: column; justify-content: center;
}
.brand { display: flex; align-items: center; gap: 12px; font-size: 17px; font-weight: 600; position: relative; z-index: 2; }
.logo { width: 42px; height: 42px; border-radius: 12px; background: linear-gradient(135deg, #7b5cff, #c9a5ff); display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: 700; }
.login-left h1 { font-size: 40px; line-height: 1.35; margin: 56px 0 18px; position: relative; z-index: 2; letter-spacing: 1px; }
.login-left p { color: #b9b4d8; line-height: 1.9; max-width: 540px; position: relative; z-index: 2; }
.feature-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 14px; margin-top: 44px; max-width: 520px; position: relative; z-index: 2; }
.feature { display: flex; align-items: center; gap: 10px; background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.09); border-radius: 12px; padding: 14px 16px; font-size: 13px; color: #d8d3f0; }
.deco { position: absolute; border-radius: 50%; filter: blur(1px); opacity: 0.5; }
.deco-1 { width: 420px; height: 420px; background: radial-gradient(circle, rgba(130,90,255,0.45), transparent 65%); top: -120px; right: -140px; }
.deco-2 { width: 360px; height: 360px; background: radial-gradient(circle, rgba(255,140,200,0.28), transparent 65%); bottom: -130px; left: -80px; }
.login-right { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px; background: #fbfbfe; }
.login-card { width: 400px; background: #fff; border-radius: 20px; padding: 36px 36px 28px; box-shadow: 0 18px 60px rgba(43, 38, 80, 0.1); }
.login-card h2 { margin: 0; font-size: 26px; }
.login-card .sub { color: #8a90a2; font-size: 13px; margin: 8px 0 18px; }
.demo-tip { margin-top: 22px; background: #f6f4ff; border: 1px dashed #d8d0ff; border-radius: 10px; padding: 12px 14px; font-size: 12px; color: #6c658c; line-height: 1.9; }
.demo-tip code { color: #6b4eff; background: #efeaff; padding: 1px 6px; border-radius: 4px; }
.copyright { margin-top: 28px; font-size: 12px; color: #a6abbd; }
@media (max-width: 980px) {
  .login-left { display: none; }
}
</style>
