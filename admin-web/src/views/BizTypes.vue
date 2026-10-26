<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">品类开关</h2>
        <div class="page-subtitle">租户按需启停业务品类，未启用品类在业务链路中被强制拦截</div>
      </div>
      <div class="flex gap-8">
        <el-tag type="success" effect="plain">已启用 {{ enabledCount }}</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="医疗类目（如医美）启用前请确保已取得《医疗机构执业许可证》等资质；平台端可随时停用插件，停用后所有租户相关入口自动关闭。" />

    <div class="biz-grid" v-loading="loading">
      <div v-for="b in list" :key="b.code" class="biz-card" :class="{ enabled: b.enabled }">
        <div class="biz-head">
          <div class="biz-icon">{{ (b.name || '品').slice(0, 1) }}</div>
          <div class="biz-main">
            <div class="biz-name">
              {{ b.name }}
              <el-tag v-if="b.medical" type="danger" size="small" effect="plain">医疗类目</el-tag>
            </div>
            <div class="biz-code">{{ b.code }}</div>
          </div>
          <el-switch :model-value="!!b.enabled" :loading="toggling === b.code" @change="(v: any) => toggle(b, v)" />
        </div>
        <div class="biz-meta">
          <span>默认提成 {{ b.defaultCommission ?? '-' }}%</span>
          <el-tag v-if="b.enabled && b.inventoryEnabled" size="small" type="success" effect="plain">已开库存</el-tag>
          <el-tag v-if="b.enabled && b.commissionOverride" size="small" type="warning" effect="plain">提成覆盖 {{ b.commissionOverride }}%</el-tag>
        </div>
        <div class="biz-modules" v-if="b.subModules">
          <el-tag v-for="m in String(b.subModules).split(',').slice(0, 5)" :key="m" size="small" effect="plain">{{ m }}</el-tag>
        </div>
        <div class="biz-foot">
          <span class="text-muted">{{ b.enabled ? '启用时间 ' + fmt(b.enabledAt, 'YYYY-MM-DD') : '尚未启用' }}</span>
          <el-button v-if="b.enabled" link type="primary" @click="openConfig(b)">品类配置</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="configVisible" :title="`品类配置 · ${current?.name}`" width="520px">
      <el-form label-width="130px">
        <el-form-item label="启用库存管理"><el-switch v-model="configForm.inventoryEnabled" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="提成比例覆盖(%)">
          <el-input-number v-model="configForm.commissionOverride" :min="0" :max="100" :precision="2" style="width: 100%" />
          <div class="tip">留空或 0 表示沿用平台默认 / 提成规则</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt } from '@/utils/format'

const list = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const toggling = ref('')
const enabledCount = computed(() => list.value.filter((b) => b.enabled).length)
const configVisible = ref(false)
const current = ref<any>(null)
const configForm = reactive<any>({ inventoryEnabled: 0, commissionOverride: null })

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/tenant/biz-types')
    list.value = res.data || []
  } finally { loading.value = false }
}
async function toggle(b: any, value: any) {
  const enabled = value ? 1 : 0
  if (!enabled) {
    await ElMessageBox.confirm(`停用「${b.name}」后，该品类下的项目、预约、收银入口将全部关闭，确认停用？`, '停用品类', { type: 'warning' })
  }
  toggling.value = b.code
  try {
    const res: any = await request.post(`/tenant/biz-types/${b.code}/toggle`, null, { params: { enabled } })
    ElMessage.success(res.data?.warning || (enabled ? '品类已启用' : '品类已停用'))
    load()
  } finally { toggling.value = '' }
}
function openConfig(b: any) {
  current.value = b
  configForm.inventoryEnabled = b.inventoryEnabled ? 1 : 0
  configForm.commissionOverride = b.commissionOverride || null
  configVisible.value = true
}
async function saveConfig() {
  saving.value = true
  try {
    await request.put(`/tenant/biz-types/${current.value.code}`, configForm)
    ElMessage.success('品类配置已保存')
    configVisible.value = false
    load()
  } finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.biz-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px; }
.biz-card { border: 1px solid #eceef5; border-radius: 12px; padding: 16px; background: #fafbfe; transition: all .2s; }
.biz-card.enabled { background: #fff; border-color: #d9d0ff; box-shadow: 0 6px 18px rgba(107, 78, 255, .08); }
.biz-head { display: flex; align-items: center; gap: 12px; }
.biz-icon { width: 42px; height: 42px; border-radius: 10px; background: #eeeaff; color: #6b4eff; display: flex; align-items: center; justify-content: center; font-weight: 700; font-size: 18px; }
.biz-main { flex: 1; min-width: 0; }
.biz-name { font-weight: 600; display: flex; align-items: center; gap: 6px; }
.biz-code { font-size: 12px; color: #9aa0b2; margin-top: 2px; }
.biz-meta { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #6c7285; margin-top: 12px; }
.biz-modules { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
.biz-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; font-size: 12px; }
.tip { font-size: 11px; color: #9aa0b2; }
</style>
