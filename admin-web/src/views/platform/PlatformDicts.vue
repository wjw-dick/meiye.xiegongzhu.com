<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">全局标准字典</h2>
        <div class="page-subtitle">项目分类、技师岗位、工位类型、会员等级等平台级标准字典</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增字典项</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="dictType" placeholder="字典类型" clearable filterable allow-create style="width: 220px" @change="search">
        <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="dictType" label="字典类型" width="220" />
      <el-table-column prop="label" label="显示名称" min-width="180" />
      <el-table-column prop="value" label="字典值" width="180" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 个字典项</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑字典项' : '新增字典项'" width="520px">
      <el-form label-width="100px">
        <el-form-item label="字典类型" required>
          <el-select v-model="form.dictType" filterable allow-create style="width: 100%">
            <el-option v-for="t in dictTypes" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="显示名称" required><el-input v-model="form.label" /></el-form-item>
        <el-form-item label="字典值" required><el-input v-model="form.value" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const dictType = ref('')
const editVisible = ref(false)
const form = reactive<any>({})
const dictTypes = computed(() => Array.from(new Set(rows.value.map((r) => r.dictType))))

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/platform/dicts/page', { params: { page: page.value, size: size.value, dictType: dictType.value || undefined } })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { dictType.value = ''; search() }
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { status: 1, sort: 0, dictType: dictType.value })
  editVisible.value = true
}
async function save() {
  if (!form.dictType || !form.label || !form.value) { ElMessage.warning('请填写完整信息'); return }
  saving.value = true
  try {
    if (form.id) await request.put('/platform/dicts', form)
    else await request.post('/platform/dicts', form)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally { saving.value = false }
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该字典项？', '删除', { type: 'warning' })
  await request.delete(`/platform/dicts/${row.id}`)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>
