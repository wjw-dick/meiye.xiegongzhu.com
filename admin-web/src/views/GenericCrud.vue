<template>
  <div class="page-card" v-if="cfg">
    <div class="page-header">
      <div>
        <h2 class="page-title">{{ cfg.title }}</h2>
        <div class="page-subtitle">{{ cfg.subtitle }}</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增</el-button>
      </div>
    </div>

    <div class="filter-bar" :class="{ collapsed: filterCollapsed }">
      <template v-if="!filterCollapsed">
        <template v-for="f in filterFields" :key="f.key">
          <el-select
            v-if="f.type === 'select' || f.type === 'remote'"
            v-model="filters[f.key]"
            :placeholder="f.label"
            clearable
            style="width: 180px"
            @change="search"
          >
            <el-option
              v-for="o in fieldOptions(f)"
              :key="String(o.value)"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
          <el-input
            v-else
            v-model="filters[f.key]"
            :placeholder="f.label"
            clearable
            style="width: 180px"
            @keyup.enter="search"
            @clear="search"
          />
        </template>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button :icon="RefreshLeft" @click="reset">重置</el-button>
      </template>
      <el-button link type="primary" @click="filterCollapsed = !filterCollapsed">
        {{ filterCollapsed ? '展开筛选' : '收起筛选' }}
        <el-icon class="ml-4"><ArrowDown v-if="filterCollapsed" /><ArrowUp v-else /></el-icon>
      </el-button>
    </div>

    <el-skeleton v-if="loading && !rows.length" :rows="7" animated class="list-skeleton" />
    <el-table v-else :data="rows" v-loading="loading" stripe border style="width: 100%">
      <el-table-column v-for="c in cfg.columns" :key="c.key" :prop="c.key" :label="c.label"
                       :width="c.width" :min-width="c.minWidth" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-if="c.type === 'tag'" :type="tagType(c, row[c.key])" effect="light">
            {{ display(c, row[c.key]) }}
          </el-tag>
          <el-switch v-else-if="c.type === 'switch'" :model-value="Number(row[c.key]) === 1" disabled />
          <span v-else-if="c.type === 'image'">
            <el-image v-if="row[c.key]" :src="row[c.key]" style="width: 46px; height: 46px; border-radius: 6px" fit="cover" />
            <span v-else class="text-muted">-</span>
          </span>
          <span v-else-if="c.type === 'money'" class="money">¥{{ money(row[c.key]) }}</span>
          <span v-else>{{ display(c, row[c.key]) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除该记录？" @confirm="remove(row)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无数据，点击右上角「新增」开始录入" :image-size="96" />
      </template>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 条</span>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50, 100]"
        layout="sizes, prev, pager, next"
        :total="total"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? `编辑${cfg.title}` : `新增${cfg.title}`" width="720px" destroy-on-close>
      <el-form :model="form" label-width="130px" label-position="right">
        <el-row :gutter="16">
          <el-col v-for="f in formFields" :key="f.key" :span="f.span || 12">
            <el-form-item :label="f.label" :required="f.required">
              <el-input v-if="f.type === 'password'" v-model="form[f.key]" type="password" show-password :placeholder="f.placeholder" />
              <el-input v-else-if="!f.type || f.type === 'input'" v-model="form[f.key]" :placeholder="f.placeholder" />
              <el-input v-else-if="f.type === 'textarea'" v-model="form[f.key]" type="textarea" :rows="3" :placeholder="f.placeholder" />
              <el-input-number v-else-if="f.type === 'number' || f.type === 'money'" v-model="form[f.key]" :precision="f.precision ?? (f.type === 'money' ? 2 : 0)" :min="f.min" :max="f.max" style="width: 100%" />
              <el-select v-else-if="f.type === 'select' || f.type === 'remote'" v-model="form[f.key]" clearable filterable style="width: 100%" :placeholder="f.placeholder || `请选择${f.label}`">
                <el-option v-for="o in fieldOptions(f)" :key="String(o.value)" :label="o.label" :value="o.value" />
              </el-select>
              <el-date-picker v-else-if="f.type === 'date'" v-model="form[f.key]" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              <el-date-picker v-else-if="f.type === 'datetime'" v-model="form[f.key]" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
              <el-switch v-else-if="f.type === 'switch'" v-model="form[f.key]" :active-value="1" :inactive-value="0" />
              <div v-else>
                <el-input v-model="form[f.key]" />
              </div>
              <div v-if="f.tip" class="field-tip">{{ f.tip }}</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh, Plus, Search, RefreshLeft, ArrowDown, ArrowUp } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import request from '@/api/request'
import { resources, Field, Column, Option } from '@/config/resources'

const route = useRoute()
const cfg = computed(() => resources[String(route.meta.resource || '')])
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const filterCollapsed = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const filters = reactive<Record<string, any>>({})
const form = reactive<Record<string, any>>({})
const remoteOptions = reactive<Record<string, Option[]>>({})

const filterFields = computed(() => (cfg.value?.fields || []).filter((f) => f.filter))
const formFields = computed(() => (cfg.value?.fields || []).filter((f) => !(editing.value && f.hiddenOnEdit)))

watch(cfg, () => {
  Object.keys(filters).forEach((k) => delete filters[k])
  page.value = 1
  load()
})

function cleanParams() {
  const params: Record<string, any> = { page: page.value, size: size.value }
  Object.entries(filters).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined) params[k] = v
  })
  return params
}

async function load() {
  if (!cfg.value) return
  loading.value = true
  try {
    const res: any = await request.get(`${cfg.value.endpoint}/page`, { params: cleanParams() })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  load()
}

function reset() {
  Object.keys(filters).forEach((k) => delete filters[k])
  search()
}

function openCreate() {
  editing.value = false
  Object.keys(form).forEach((k) => delete form[k])
  for (const f of formFields.value) {
    form[f.key] = f.type === 'switch' ? 1 : undefined
  }
  dialogVisible.value = true
}

function openEdit(row: any) {
  editing.value = true
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  dialogVisible.value = true
}

async function save() {
  if (!cfg.value) return
  for (const f of formFields.value) {
    if (f.required && (form[f.key] === undefined || form[f.key] === null || form[f.key] === '')) {
      ElMessage.warning(`请填写${f.label}`)
      return
    }
  }
  if (editing.value && 'password' in form && !form.password) {
    delete form.password
  }
  saving.value = true
  try {
    if (editing.value) {
      await request.put(cfg.value.endpoint, form)
      ElMessage.success('保存成功')
    } else {
      await request.post(cfg.value.endpoint, form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove(row: any) {
  await request.delete(`${cfg.value!.endpoint}/${row.id}`)
  ElMessage.success('删除成功')
  load()
}

function display(c: Column, value: any) {
  if (value === null || value === undefined || value === '') return '-'
  if (c.options) {
    return c.options.find((o) => String(o.value) === String(value))?.label ?? value
  }
  if (c.map) {
    return c.map[String(value)] ?? value
  }
  if (c.type === 'date') return dayjs(value).format('YYYY-MM-DD')
  if (c.type === 'datetime') return dayjs(value).format('YYYY-MM-DD HH:mm')
  return value
}

function tagType(c: Column, value: any) {
  if (c.tagType && c.tagType[String(value)]) return c.tagType[String(value)] as any
  if (c.options && c.options.length === 2) return Number(value) === 1 ? 'success' : 'info'
  const v = String(value)
  if (['PAID', 'DONE', 'VERIFIED', 'ACTIVE', 'UNUSED', 'NORMAL', 'CONFIRMED'].includes(v)) return 'success'
  if (['PENDING', 'APPLIED', 'DRAFT', 'UNPAID'].includes(v)) return 'warning'
  if (['CANCELLED', 'REJECTED', 'NO_SHOW', 'EXPIRED', 'DISABLED'].includes(v)) return 'danger'
  return 'info'
}

function money(v: any) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function fieldOptions(f: Field): Option[] {
  if (f.options) return f.options
  if (f.type === 'remote' && f.remote) return remoteOptions[f.remote] || []
  return []
}

async function loadRemote() {
  const sources = new Set((cfg.value?.fields || []).filter((f) => f.remote).map((f) => f.remote!))
  const map: Record<string, { url: string; label: (x: any) => string; value?: (x: any) => any }> = {
    stores: { url: '/tenant/stores/list', label: (x) => x.name },
    customers: { url: '/tenant/customers/list', label: (x) => `${x.name}${x.phone ? ' / ' + x.phone : ''}` },
    technicians: { url: '/tenant/technicians/list', label: (x) => `${x.name}（${x.techNo || ''}）` },
    items: { url: '/tenant/service-items/list', label: (x) => x.name },
    packages: { url: '/tenant/service-packages/list', label: (x) => x.name },
    cardTypes: { url: '/tenant/card-types/list', label: (x) => x.name },
    bizTypes: { url: '/tenant/biz-types', label: (x) => `${x.name}${x.enabled ? '' : '（未启用）'}`, value: (x) => x.code }
  }
  for (const s of sources) {
    if (remoteOptions[s] || !map[s]) continue
    try {
      const res: any = await request.get(map[s].url)
      const list = Array.isArray(res.data) ? res.data : res.data?.records || []
      const mapper = map[s].value || ((x: any) => x.id)
      remoteOptions[s] = list.map((x: any) => ({ label: map[s].label(x), value: mapper(x) }))
    } catch (e) {
      remoteOptions[s] = []
    }
  }
}

onMounted(async () => {
  await loadRemote()
  load()
})
</script>

<style scoped>
.money { font-weight: 600; color: #2f3448; }
.field-tip { font-size: 11px; color: #9aa0b2; line-height: 1.4; margin-top: 2px; }
.list-skeleton { padding: 18px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; margin-bottom: 12px; }
.filter-bar.collapsed { justify-content: flex-end; }
.ml-4 { margin-left: 4px; }
</style>
