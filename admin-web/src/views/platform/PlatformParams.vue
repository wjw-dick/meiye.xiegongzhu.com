<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">平台参数</h2>
        <div class="page-subtitle">平台级全局参数：抽成比例、预约超时、预警天数、积分比例、提现手续费等</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :loading="saving" @click="saveAll">保存全部</el-button>
      </div>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="paramKey" label="参数键" width="260" />
      <el-table-column prop="name" label="参数名称" width="200" />
      <el-table-column label="参数值" min-width="240">
        <template #default="{ row }">
          <el-input v-model="row.paramValue" size="small" />
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="说明" min-width="240" show-overflow-tooltip />
      <el-table-column label="更新时间" width="170"><template #default="{ row }">{{ fmt(row.updatedAt) }}</template></el-table-column>
    </el-table>

    <div class="section-title mt-16">新增参数</div>
    <div class="filter-bar">
      <el-input v-model="addForm.paramKey" placeholder="参数键，如 points_per_yuan" style="width: 230px" />
      <el-input v-model="addForm.name" placeholder="参数名称" style="width: 180px" />
      <el-input v-model="addForm.paramValue" placeholder="参数值" style="width: 180px" />
      <el-input v-model="addForm.remark" placeholder="说明" style="width: 240px" />
      <el-button type="primary" :icon="Plus" @click="add">新增</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt } from '@/utils/format'

const rows = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const addForm = reactive<any>({ paramKey: '', name: '', paramValue: '', remark: '' })

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/platform/params/list')
    rows.value = res.data || []
  } finally { loading.value = false }
}
async function saveAll() {
  saving.value = true
  try {
    const payload: Record<string, string> = {}
    rows.value.forEach((r) => { payload[r.paramKey] = r.paramValue })
    await request.put('/platform/params/batch', payload)
    ElMessage.success('平台参数已保存并实时生效')
    load()
  } finally { saving.value = false }
}
async function add() {
  if (!addForm.paramKey || !addForm.name) { ElMessage.warning('请填写参数键和名称'); return }
  await request.post('/platform/params', addForm)
  ElMessage.success('已新增')
  Object.assign(addForm, { paramKey: '', name: '', paramValue: '', remark: '' })
  load()
}
onMounted(load)
</script>

<style scoped>
.section-title { font-weight: 600; margin-bottom: 10px; padding-left: 10px; border-left: 3px solid var(--my-primary); }
</style>
