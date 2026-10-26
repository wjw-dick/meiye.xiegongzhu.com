<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">品类插件库</h2>
        <div class="page-subtitle">平台统一维护业务品类插件、子模块、字段模板与默认提成，租户按需启停</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增品类</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="停用品类插件后，所有租户对应入口立即失效；医疗类目需在租户开通时提示资质要求。" />

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="sort" label="排序" width="70" />
      <el-table-column prop="code" label="品类编码" width="130" />
      <el-table-column prop="name" label="品类名称" width="150" />
      <el-table-column label="医疗类目" width="100">
        <template #default="{ row }"><el-tag :type="row.medical === 1 ? 'danger' : 'info'" size="small" effect="plain">{{ row.medical === 1 ? '是' : '否' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="subModules" label="子模块" min-width="200" show-overflow-tooltip />
      <el-table-column label="默认启用" width="110">
        <template #default="{ row }">
          <el-switch :model-value="row.defaultEnabled === 1" @change="(v: any) => setDefault(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="(v: any) => toggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 个品类插件</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑品类插件' : '新增品类插件'" width="720px" destroy-on-close>
      <el-form label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="品类编码" required><el-input v-model="form.code" placeholder="如 BEAUTY" :disabled="!!form.id" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="品类名称" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="医疗类目"><el-switch v-model="form.medical" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="默认启用"><el-switch v-model="form.defaultEnabled" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="图标"><el-input v-model="form.icon" placeholder="图标名或 URL" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="子模块"><el-input v-model="form.subModules" type="textarea" :rows="2" placeholder="如 预约,项目,套餐,会员卡,库存" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="字段模板 JSON"><el-input v-model="form.fieldTemplate" type="textarea" :rows="4" placeholder='{"positions":["美容师"],"stationTypes":["床位"]}' /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="默认提成 JSON"><el-input v-model="form.defaultCommission" type="textarea" :rows="3" placeholder='{"rate":10,"tiers":[]}' /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
        </el-row>
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
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const editVisible = ref(false)
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/platform/biz-plugins/page', { params: { page: page.value, size: size.value } })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { status: 1, medical: 0, defaultEnabled: 0, sort: 0 })
  editVisible.value = true
}
async function save() {
  if (!form.code || !form.name) { ElMessage.warning('请填写编码和名称'); return }
  saving.value = true
  try {
    if (form.id) await request.put('/platform/biz-plugins', form)
    else await request.post('/platform/biz-plugins', form)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally { saving.value = false }
}
async function toggle(row: any, v: any) {
  await request.post(`/platform/biz-plugins/${row.id}/toggle`, null, { params: { status: v ? 1 : 0 } })
  ElMessage.success(v ? '插件已启用' : '插件已停用，租户入口同步关闭')
  load()
}
async function setDefault(row: any, v: any) {
  await request.post(`/platform/biz-plugins/${row.id}/default`, null, { params: { enabled: v ? 1 : 0 } })
  ElMessage.success('默认启用设置已更新')
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除品类「${row.name}」？已启用该品类的租户将无法继续使用。`, '删除品类', { type: 'warning' })
  await request.delete(`/platform/biz-plugins/${row.id}`)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>
