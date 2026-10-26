<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">租户管理</h2>
        <div class="page-subtitle">租户开通、配额、状态、续期、品类初始化与经营概览下钻</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">开通租户</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.kw" placeholder="编码 / 名称 / 联系人" clearable style="width: 220px" @keyup.enter="search" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="search">
        <el-option v-for="s in ['TRIAL', 'NORMAL', 'DISABLED', 'EXPIRED']" :key="s" :label="statusText(s)" :value="s" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="code" label="租户编码" width="110" />
      <el-table-column prop="name" label="租户名称" min-width="170" />
      <el-table-column prop="contact" label="联系人" width="110" />
      <el-table-column prop="phone" label="联系电话" width="140" />
      <el-table-column label="配额（门店/客户/技师）" width="200">
        <template #default="{ row }">{{ row.storeLimit }} / {{ row.customerLimit }} / {{ row.technicianLimit }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="开通时间" width="120"><template #default="{ row }">{{ dateOnly(row.openAt) }}</template></el-table-column>
      <el-table-column label="到期时间" width="120">
        <template #default="{ row }">
          <span :class="{ danger: isExpiring(row.expireAt) }">{{ dateOnly(row.expireAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openOverview(row)">概览</el-button>
          <el-button link type="success" @click="renew(row)">续期</el-button>
          <el-button link :type="row.status === 'DISABLED' ? 'success' : 'warning'" @click="toggleStatus(row)">{{ row.status === 'DISABLED' ? '启用' : '停用' }}</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 个租户</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="createVisible" title="开通租户" width="720px" destroy-on-close>
      <el-form :model="form" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="租户编码" required><el-input v-model="form.code" placeholder="如 5MY" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="租户名称" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="门店上限"><el-input-number v-model="form.storeLimit" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="客户上限"><el-input-number v-model="form.customerLimit" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="技师上限"><el-input-number v-model="form.technicianLimit" :min="1" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="到期时间"><el-date-picker v-model="form.expireAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="初始管理员"><el-input v-model="form.adminUsername" placeholder="默认 admin" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="初始密码"><el-input v-model="form.adminPassword" type="password" show-password placeholder="默认 admin123" /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="初始品类">
              <el-checkbox-group v-model="form.bizTypes">
                <el-checkbox v-for="p in plugins" :key="p.code" :label="p.code">{{ p.name }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ form.id ? '保存' : '开通' }}</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="overviewVisible" :title="`租户概览 · ${current?.name}`" size="720px">
      <template v-if="overview">
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="客户">{{ overview.customers || 0 }}</el-descriptions-item>
          <el-descriptions-item label="技师">{{ overview.technicians || 0 }}</el-descriptions-item>
          <el-descriptions-item label="门店">{{ overview.stores || 0 }}</el-descriptions-item>
          <el-descriptions-item label="预约">{{ overview.appointments || 0 }}</el-descriptions-item>
          <el-descriptions-item label="订单">{{ overview.orders || 0 }}</el-descriptions-item>
          <el-descriptions-item label="营收">¥{{ money(overview.revenue) }}</el-descriptions-item>
          <el-descriptions-item label="储值余额">¥{{ money(overview.storedBalance) }}</el-descriptions-item>
          <el-descriptions-item label="到店率">{{ overview.fulfillRate || 0 }}%</el-descriptions-item>
          <el-descriptions-item label="客单价">¥{{ money(overview.avgOrder) }}</el-descriptions-item>
        </el-descriptions>
        <div class="section-title mt-16">已启用品类</div>
        <div class="flex gap-8" style="flex-wrap: wrap">
          <el-tag v-for="b in tenantBiz" :key="b.id" :type="b.enabled === 1 ? 'success' : 'info'" effect="plain">
            {{ b.bizType }}{{ b.enabled === 1 ? '' : '（未启用）' }}
          </el-tag>
          <span v-if="!tenantBiz.length" class="text-muted">暂无品类配置</span>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, statusText, statusTag, dateOnly } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ kw: '', status: '' })
const plugins = ref<any[]>([])
const createVisible = ref(false)
const form = reactive<any>({})
const overviewVisible = ref(false)
const overview = ref<any>(null)
const tenantBiz = ref<any[]>([])
const current = ref<any>(null)

function isExpiring(v: any) {
  if (!v) return false
  const d = new Date(v).getTime()
  return d < Date.now() + 30 * 86400000
}
async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/platform/tenants/page', { params: { page: page.value, size: size.value, ...query } })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { kw: '', status: '' }); search() }
async function loadPlugins() {
  const res: any = await request.get('/platform/biz-plugins/list')
  plugins.value = (res.data || []).filter((p: any) => p.status === 1)
}
function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { storeLimit: 1, customerLimit: 1000, technicianLimit: 50, adminUsername: 'admin', adminPassword: 'admin123', bizTypes: [] })
  createVisible.value = true
}
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  createVisible.value = true
}
async function save() {
  if (!form.code || !form.name) { ElMessage.warning('请填写编码和名称'); return }
  saving.value = true
  try {
    if (form.id) await request.put('/platform/tenants', form)
    else await request.post('/platform/tenants/create', form)
    ElMessage.success(form.id ? '已保存' : '租户已开通')
    createVisible.value = false
    load()
  } finally { saving.value = false }
}
async function toggleStatus(row: any) {
  const target = row.status === 'DISABLED' ? 'NORMAL' : 'DISABLED'
  await ElMessageBox.confirm(`确认将租户「${row.name}」${target === 'DISABLED' ? '停用' : '启用'}？`, '状态变更', { type: 'warning' })
  await request.post(`/platform/tenants/${row.id}/status`, null, { params: { status: target } })
  ElMessage.success('状态已更新')
  load()
}
async function renew(row: any) {
  const { value } = await ElMessageBox.prompt('请输入续期天数', `续期 · ${row.name}`, {
    inputValue: '365', inputPattern: /^\d+$/, inputErrorMessage: '请输入数字'
  })
  await request.post(`/platform/tenants/${row.id}/renew`, null, { params: { days: Number(value) } })
  ElMessage.success('续期成功')
  load()
}
async function openOverview(row: any) {
  current.value = row
  const [o, b] = await Promise.all([
    request.get(`/platform/tenants/${row.id}/overview`),
    request.get(`/platform/tenants/${row.id}/biz-types`)
  ]) as any[]
  overview.value = o.data || {}
  tenantBiz.value = b.data || []
  overviewVisible.value = true
}
onMounted(async () => { await loadPlugins(); load() })
</script>

<style scoped>
.danger { color: #e6542c; font-weight: 600; }
.section-title { font-weight: 600; margin-bottom: 10px; padding-left: 10px; border-left: 3px solid var(--my-primary); }
</style>
