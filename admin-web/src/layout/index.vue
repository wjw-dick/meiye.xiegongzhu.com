<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="brand">
        <div class="logo">美</div>
        <div v-show="!collapsed" class="brand-text">
          <div class="brand-name">美业大健康</div>
          <div class="brand-sub">数字化 SaaS 平台</div>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <template v-for="group in groups" :key="group.name">
          <div v-show="!collapsed" class="menu-group">{{ group.name }}</div>
          <el-menu
            :default-active="route.path"
            :collapse="collapsed"
            :collapse-transition="false"
            background-color="transparent"
            text-color="#aab0c5"
            active-text-color="#ffffff"
            router
          >
            <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
              <el-icon><component :is="item.icon" /></el-icon>
              <template #title>{{ item.title }}</template>
            </el-menu-item>
          </el-menu>
        </template>
      </el-scrollbar>
      <div class="sidebar-footer" @click="collapsed = !collapsed">
        <el-icon><DArrowLeft v-if="!collapsed" /><DArrowRight v-else /></el-icon>
        <span v-show="!collapsed">收起菜单</span>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ currentGroup }}</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-tag v-if="auth.isPlatform" type="warning" effect="plain" size="small">平台端</el-tag>
          <el-select
            v-if="auth.isPlatform"
            v-model="drill"
            filterable
            clearable
            placeholder="选择下钻租户（只读）"
            size="small"
            style="width: 220px"
            @change="onDrill"
          >
            <el-option v-for="t in tenants" :key="t.id" :label="`${t.name}（${t.code}）`" :value="String(t.id)" />
          </el-select>
          <el-tag v-else type="success" effect="plain" size="small">{{ auth.user?.tenantCode }}</el-tag>
          <el-dropdown @command="onCommand">
            <span class="user-chip">
              <el-avatar :size="28" style="background: var(--my-primary)">{{ (auth.displayName || 'U').slice(0, 1) }}</el-avatar>
              <span class="user-name">{{ auth.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <main class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" :key="route.fullPath + drill" />
        </router-view>
      </main>
    </div>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form :model="pwd" label-width="90px">
        <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
        <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="changePassword">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const drill = ref(auth.drillTenant)
const tenants = ref<any[]>([])
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

interface Group { name: string; items: { path: string; title: string; icon: string }[] }

const groups = computed<Group[]>(() => {
  const all = router.getRoutes().filter((r) => r.meta?.title && r.meta?.group)
  const map = new Map<string, Group>()
  for (const r of all) {
    const isPlatform = !!r.meta.platform
    if (isPlatform && !auth.isPlatform) continue
    if (!isPlatform && auth.isPlatform && !auth.drillTenant) continue
    if (!isPlatform && r.path === '/dashboard' && auth.isPlatform) continue
    const group = String(r.meta.group)
    if (!map.has(group)) map.set(group, { name: group, items: [] })
    map.get(group)!.items.push({ path: r.path, title: String(r.meta.title), icon: String(r.meta.icon || 'Menu') })
  }
  const order = ['工作台', '业务中心', '会员营销', '财务结算', '供应链', '基础设置', '系统管理', '数据报表', '平台管理']
  return Array.from(map.values()).sort((a, b) => order.indexOf(a.name) - order.indexOf(b.name))
})

const currentGroup = computed(() => String(route.meta.group || ''))

async function loadTenants() {
  if (!auth.isPlatform) return
  try {
    const res: any = await request.get('/platform/tenants/page', { params: { page: 1, size: 200 } })
    tenants.value = res.data.records || []
  } catch (e) {
    tenants.value = []
  }
}

function onDrill(val: string) {
  const t = tenants.value.find((x) => String(x.id) === val)
  auth.setDrillTenant(val || '', t?.name || '')
  if (val && route.path.startsWith('/platform/')) {
    router.push('/dashboard')
  }
}

function onCommand(cmd: string) {
  if (cmd === 'password') {
    pwdVisible.value = true
    return
  }
  if (cmd === 'logout') {
    ElMessageBox.confirm('确认退出登录？', '提示', { type: 'warning' }).then(async () => {
      await auth.logout()
      router.push('/login')
    })
  }
}

async function changePassword() {
  if (pwd.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    ElMessage.warning('两次密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await request.post('/auth/password', { oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码修改成功，请重新登录')
    pwdVisible.value = false
    await auth.logout()
    router.push('/login')
  } finally {
    pwdLoading.value = false
  }
}

onMounted(loadTenants)
</script>

<style scoped>
.layout { display: flex; height: 100%; }
.sidebar {
  width: 232px; background: var(--my-sidebar); color: #fff; display: flex; flex-direction: column;
  transition: width 0.2s ease; flex-shrink: 0;
}
.sidebar.collapsed { width: 68px; }
.brand { display: flex; align-items: center; gap: 10px; padding: 18px 16px; border-bottom: 1px solid rgba(255,255,255,0.06); }
.logo {
  width: 36px; height: 36px; border-radius: 10px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #7b5cff, #b18cff); font-weight: 700; font-size: 18px; flex-shrink: 0;
}
.brand-name { font-size: 15px; font-weight: 600; }
.brand-sub { font-size: 11px; color: #7d849e; margin-top: 2px; }
.menu-scroll { flex: 1; }
.menu-group { font-size: 11px; color: #6b7290; padding: 14px 20px 6px; letter-spacing: 1px; }
:deep(.el-menu) { border-right: none; }
:deep(.el-menu-item) { height: 42px; margin: 2px 10px; border-radius: 8px; }
:deep(.el-menu-item.is-active) { background: linear-gradient(90deg, #6b4eff, #8f7bff); }
:deep(.el-menu-item:hover) { background: var(--my-sidebar-active); }
.sidebar-footer {
  display: flex; align-items: center; gap: 8px; padding: 14px 20px; font-size: 12px; color: #7d849e;
  border-top: 1px solid rgba(255,255,255,0.06); cursor: pointer;
}
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar {
  height: 60px; background: #fff; display: flex; align-items: center; justify-content: space-between;
  padding: 0 22px; box-shadow: 0 1px 0 #eef0f5; flex-shrink: 0;
}
.topbar-right { display: flex; align-items: center; gap: 12px; }
.user-chip { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.user-name { font-size: 13px; color: #333; }
.content { flex: 1; overflow: auto; padding: 18px 22px 30px; }
</style>
