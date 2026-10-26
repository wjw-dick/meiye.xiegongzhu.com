<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">平台菜单</h2>
        <div class="page-subtitle">平台管理端、租户端、小程序、H5、公众号菜单可视化配置总览</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="loadList">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openNew">新建配置</el-button>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="8">
        <el-table :data="configs" v-loading="loading" stripe border highlight-current-row @current-change="selectRow">
          <el-table-column prop="tenantId" label="租户" width="80">
            <template #default="{ row }">{{ row.tenantId === 0 ? '平台' : row.tenantId }}</template>
          </el-table-column>
          <el-table-column prop="target" label="端" width="110" />
          <el-table-column label="更新时间" min-width="150"><template #default="{ row }">{{ fmt(row.updatedAt, 'MM-DD HH:mm') }}</template></el-table-column>
        </el-table>
      </el-col>
      <el-col :span="16">
        <div class="section-title flex-between">
          <span>{{ current ? `${current.target} · 租户 ${current.tenantId === 0 ? '平台' : current.tenantId}` : '请选择左侧菜单配置' }}</span>
          <el-button type="primary" size="small" :disabled="!current" :loading="saving" @click="save">保存</el-button>
        </div>
        <el-input v-model="content" type="textarea" :rows="18" class="json-editor" spellcheck="false" :disabled="!current" />
        <div class="flex gap-8 mt-16">
          <el-button :disabled="!current" @click="formatJson">格式化</el-button>
          <el-button :disabled="!current" @click="validate">校验</el-button>
          <el-button :disabled="!current" @click="loadContent">还原</el-button>
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="newVisible" title="新建菜单配置" width="460px">
      <el-form label-width="100px">
        <el-form-item label="目标端">
          <el-select v-model="newForm.target" style="width: 100%">
            <el-option label="管理端 ADMIN" value="ADMIN" /><el-option label="小程序 MINIAPP" value="MINIAPP" />
            <el-option label="H5 H5" value="H5" /><el-option label="公众号 WECHAT" value="WECHAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="租户ID"><el-input-number v-model="newForm.tenantId" :min="0" style="width: 100%" /><div class="tip">0 表示平台级</div></el-form-item>
        <el-form-item label="初始 JSON"><el-input v-model="newForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newVisible = false">取消</el-button>
        <el-button type="primary" @click="createNew">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt } from '@/utils/format'

const configs = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const current = ref<any>(null)
const content = ref('[]')
const newVisible = ref(false)
const newForm = reactive<any>({ target: 'ADMIN', tenantId: 0, content: '[]' })

async function loadList() {
  loading.value = true
  try {
    const res: any = await request.get('/menus/list')
    configs.value = res.data || []
    if (configs.value.length && !current.value) selectRow(configs.value[0])
  } finally { loading.value = false }
}
function selectRow(row: any) {
  if (!row) return
  current.value = row
  loadContent()
}
async function loadContent() {
  if (!current.value) return
  const res: any = await request.get('/menus', { params: { target: current.value.target, tenantId: current.value.tenantId } })
  content.value = res.data?.content || '[]'
  try { content.value = JSON.stringify(JSON.parse(content.value), null, 2) } catch (e) { /* keep */ }
}
function formatJson() {
  try { content.value = JSON.stringify(JSON.parse(content.value || '[]'), null, 2); ElMessage.success('已格式化') } catch (e: any) { ElMessage.error('JSON 错误：' + e.message) }
}
function validate() {
  try {
    const arr = JSON.parse(content.value || '[]')
    if (!Array.isArray(arr)) throw new Error('根节点必须是数组')
    ElMessage.success(`校验通过，共 ${arr.length} 项`)
  } catch (e: any) { ElMessage.error('校验失败：' + e.message) }
}
async function save() {
  try {
    const arr = JSON.parse(content.value || '[]')
    if (!Array.isArray(arr)) throw new Error('根节点必须是数组')
  } catch (e: any) { ElMessage.error('JSON 错误：' + e.message); return }
  saving.value = true
  try {
    await request.put('/menus', { target: current.value.target, tenantId: current.value.tenantId, content: content.value })
    ElMessage.success('菜单已保存')
    loadList()
  } finally { saving.value = false }
}
function openNew() {
  Object.assign(newForm, { target: 'ADMIN', tenantId: 0, content: '[]' })
  newVisible.value = true
}
async function createNew() {
  await request.put('/menus', newForm)
  ElMessage.success('已创建')
  newVisible.value = false
  loadList()
}
onMounted(loadList)
</script>

<style scoped>
.section-title { font-weight: 600; margin-bottom: 10px; padding-left: 10px; border-left: 3px solid var(--my-primary); }
.json-editor :deep(textarea) { font-family: Menlo, Consolas, monospace; font-size: 12px; line-height: 1.6; }
.tip { font-size: 11px; color: #9aa0b2; }
</style>
