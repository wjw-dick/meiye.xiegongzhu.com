<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">平台审计</h2>
        <div class="page-subtitle">平台超管操作、跨租户下钻只读访问与租户开通/停用等关键行为留痕</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button :icon="Download" @click="exportCsv">导出 CSV</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.operatorName" placeholder="操作人" clearable style="width: 150px" @keyup.enter="search" />
      <el-input v-model="query.module" placeholder="模块" clearable style="width: 150px" @keyup.enter="search" />
      <el-input v-model="query.action" placeholder="操作" clearable style="width: 150px" @keyup.enter="search" />
      <el-select v-model="query.userType" placeholder="用户类型" clearable style="width: 130px" @change="search">
        <el-option label="平台" value="PLATFORM" /><el-option label="租户" value="TENANT" />
      </el-select>
      <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width: 240px" @change="search" />
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="tenantId" label="租户ID" width="90" />
      <el-table-column prop="operatorName" label="操作人" width="140" />
      <el-table-column prop="userType" label="类型" width="100" />
      <el-table-column prop="module" label="模块" width="130" />
      <el-table-column prop="action" label="操作" width="150" />
      <el-table-column prop="target" label="对象" width="150" show-overflow-tooltip />
      <el-table-column prop="detail" label="详情" min-width="220" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column label="时间" width="170"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }"><el-button link type="primary" @click="openDetail(row)">详情</el-button></template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 条平台日志</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50, 100]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-drawer v-model="detailVisible" title="审计详情" size="560px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="操作人">{{ detail?.operatorName }}（{{ detail?.userType }}）</el-descriptions-item>
        <el-descriptions-item label="租户ID">{{ detail?.tenantId }}</el-descriptions-item>
        <el-descriptions-item label="模块 / 操作">{{ detail?.module }} / {{ detail?.action }}</el-descriptions-item>
        <el-descriptions-item label="对象">{{ detail?.target || '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ detail?.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ fmt(detail?.createdAt) }}</el-descriptions-item>
      </el-descriptions>
      <div class="section-title mt-16">详情</div>
      <pre class="detail-pre">{{ detail?.detail || '无' }}</pre>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const query = reactive<any>({ operatorName: '', module: '', action: '', userType: '' })
const range = ref<[string, string] | null>(null)
const detailVisible = ref(false)
const detail = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v) params[k] = v })
    if (range.value) { params.ge_createdAt = range.value[0]; params.le_createdAt = range.value[1] + ' 23:59:59' }
    const res: any = await request.get('/platform/audit/page', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { operatorName: '', module: '', action: '', userType: '' }); range.value = null; search() }
function openDetail(row: any) { detail.value = row; detailVisible.value = true }
function exportCsv() {
  const header = ['ID', '租户ID', '操作人', '类型', '模块', '操作', '对象', '详情', 'IP', '时间']
  const lines = [header.join(',')].concat(rows.value.map((r) => [
    r.id, r.tenantId, r.operatorName, r.userType, r.module, r.action, r.target, r.detail, r.ip, fmt(r.createdAt)
  ].map((v) => `"${String(v ?? '').replace(/"/g, '""')}"`).join(',')))
  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `platform-audit-${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  ElMessage.success('已导出当前页日志')
}
onMounted(load)
</script>

<style scoped>
.section-title { font-weight: 600; margin-bottom: 10px; padding-left: 10px; border-left: 3px solid var(--my-primary); }
.detail-pre { background: #f7f8fc; border-radius: 8px; padding: 14px; font-size: 12px; white-space: pre-wrap; word-break: break-all; }
</style>
