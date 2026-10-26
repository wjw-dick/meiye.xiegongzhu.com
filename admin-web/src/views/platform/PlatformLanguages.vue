<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">多语言</h2>
        <div class="page-subtitle">管理端、门户、小程序、App 多语言开关与默认语言设置</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增语言</el-button>
      </div>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="code" label="语言代码" width="150" />
      <el-table-column prop="name" label="语言名称" min-width="180" />
      <el-table-column label="默认" width="100">
        <template #default="{ row }"><el-tag v-if="row.isDefault === 1" type="warning" size="small">默认</el-tag><span v-else class="text-muted">-</span></template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.isDefault !== 1" link type="warning" @click="setDefault(row)">设为默认</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑语言' : '新增语言'" width="460px">
      <el-form label-width="100px">
        <el-form-item label="语言代码" required><el-input v-model="form.code" placeholder="zh-CN / en-US / ja-JP" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="语言名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="默认语言"><el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/platform/languages/list')
    rows.value = res.data || []
  } finally { loading.value = false }
}
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { enabled: 1, isDefault: 0, sort: 0 })
  editVisible.value = true
}
async function save() {
  if (!form.code || !form.name) { ElMessage.warning('请填写语言代码和名称'); return }
  saving.value = true
  try {
    if (form.isDefault === 1) {
      for (const r of rows.value.filter((x) => x.isDefault === 1 && x.id !== form.id)) {
        await request.put('/platform/languages', { ...r, isDefault: 0 })
      }
    }
    if (form.id) await request.put('/platform/languages', form)
    else await request.post('/platform/languages', form)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally { saving.value = false }
}
async function setDefault(row: any) {
  for (const r of rows.value.filter((x) => x.isDefault === 1 && x.id !== row.id)) {
    await request.put('/platform/languages', { ...r, isDefault: 0 })
  }
  await request.put('/platform/languages', { ...row, isDefault: 1 })
  ElMessage.success('默认语言已切换')
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该语言？', '删除语言', { type: 'warning' })
  await request.delete(`/platform/languages/${row.id}`)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>
