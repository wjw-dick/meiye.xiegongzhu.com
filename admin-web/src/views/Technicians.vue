<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">技师 / 服务人员档案</h2>
        <div class="page-subtitle">岗位技能、健康证/资质证书到期预警、排班轮牌、服务统计与业绩</div>
      </div>
      <div class="flex gap-8">
        <el-upload :show-file-list="false" :http-request="doImport" accept=".xlsx,.xls">
          <el-button :icon="Upload">批量导入</el-button>
        </el-upload>
        <el-button :icon="Download" @click="exportExcel">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增技师</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.kw" placeholder="姓名 / 编号 / 手机号" clearable style="width: 220px" @keyup.enter="search" />
      <el-select v-model="query.storeId" placeholder="所属门店" clearable style="width: 170px" @change="search">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-select v-model="query.position" placeholder="岗位" clearable style="width: 150px" @change="search">
        <el-option v-for="p in positions" :key="p" :label="p" :value="p" />
      </el-select>
      <el-select v-model="query.certStatus" placeholder="证书状态" clearable style="width: 140px" @change="search">
        <el-option label="正常" value="OK" /><el-option label="即将到期" value="WARN" /><el-option label="已过期" value="EXPIRED" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="techNo" label="技师编号" width="180" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="手机号" width="125">
        <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
      </el-table-column>
      <el-table-column prop="position" label="岗位" width="110" />
      <el-table-column prop="skills" label="技能品类" min-width="150" show-overflow-tooltip />
      <el-table-column label="所属门店" width="140">
        <template #default="{ row }">{{ storeName(row.storeId) }}</template>
      </el-table-column>
      <el-table-column label="证书" width="100">
        <template #default="{ row }">
          <el-tag :type="row.certStatus === 'OK' ? 'success' : row.certStatus === 'WARN' ? 'warning' : 'danger'" size="small">
            {{ row.certStatus === 'OK' ? '正常' : row.certStatus === 'WARN' ? '即将到期' : '已过期' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="serviceCount" label="服务次数" width="100" />
      <el-table-column label="业绩" width="120">
        <template #default="{ row }">¥{{ money(row.totalRevenue) }}</template>
      </el-table-column>
      <el-table-column prop="rating" label="评分" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '在职' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openStats(row)">统计</el-button>
          <el-button link type="primary" @click="openCerts(row)">证书</el-button>
          <el-button link type="primary" @click="openSchedules(row)">排班</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 1" link type="danger" @click="disable(row)">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 位技师</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                     :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next"
                     @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑技师' : '新增技师'" width="760px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号" required><el-input v-model="form.phone" maxlength="11" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证号" required><el-input v-model="form.idCard" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属门店">
            <el-select v-model="form.storeId" style="width: 100%">
              <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="岗位">
            <el-select v-model="form.position" allow-create filterable style="width: 100%">
              <el-option v-for="p in positions" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="技能品类">
            <el-select v-model="skillList" multiple allow-create filterable style="width: 100%">
              <el-option v-for="b in bizTypes" :key="b.code" :label="b.name" :value="b.code" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="入职日期">
            <el-date-picker v-model="form.entryAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="登录密码">
            <el-input v-model="form.password" type="password" show-password placeholder="默认 123456" />
          </el-form-item></el-col>
          <el-col :span="24"><el-form-item label="个人简介">
            <el-input v-model="form.bio" type="textarea" :rows="2" />
          </el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="certVisible" :title="`${current?.name || ''} · 资质证书`" size="60%">
      <div class="filter-bar">
        <el-button type="primary" :icon="Plus" @click="openCertForm()">新增证书</el-button>
        <span class="text-muted">距到期 ≤ {{ 30 }} 天的证书将以黄色标记，过期红色标记</span>
      </div>
      <el-table :data="certs" stripe>
        <el-table-column prop="certType" label="证书类型" width="140" />
        <el-table-column prop="certNo" label="证书编号" width="160" />
        <el-table-column prop="issuer" label="发证机关" min-width="160" />
        <el-table-column prop="expireAt" label="有效期至" width="120" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="certTag(row.expireAt)" size="small">{{ certLabel(row.expireAt) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCertForm(row)">编辑</el-button>
            <el-button link type="danger" @click="removeCert(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-drawer v-model="scheduleVisible" :title="`${current?.name || ''} · 排班`" size="60%">
      <div class="filter-bar">
        <el-button type="primary" :icon="Plus" @click="openScheduleForm()">新增排班</el-button>
      </div>
      <el-table :data="schedules" stripe>
        <el-table-column prop="workDate" label="日期" width="120" />
        <el-table-column prop="startTime" label="开始" width="100" />
        <el-table-column prop="endTime" label="结束" width="100" />
        <el-table-column prop="bizType" label="品类" width="120" />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openScheduleForm(row)">编辑</el-button>
            <el-button link type="danger" @click="removeSchedule(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog v-model="certFormVisible" title="证书信息" width="520px">
      <el-form :model="certForm" label-width="90px">
        <el-form-item label="证书类型"><el-input v-model="certForm.certType" placeholder="健康证/美容师证/育婴师证" /></el-form-item>
        <el-form-item label="证书编号"><el-input v-model="certForm.certNo" /></el-form-item>
        <el-form-item label="发证机关"><el-input v-model="certForm.issuer" /></el-form-item>
        <el-form-item label="有效期至">
          <el-date-picker v-model="certForm.expireAt" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="扫描件"><el-input v-model="certForm.fileUrl" placeholder="附件地址" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="certFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCert">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scheduleFormVisible" title="排班信息" width="520px">
      <el-form :model="scheduleForm" label-width="90px">
        <el-form-item label="日期">
          <el-date-picker v-model="scheduleForm.workDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-picker v-model="scheduleForm.startTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-time-picker v-model="scheduleForm.endTime" value-format="HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="业务品类">
          <el-select v-model="scheduleForm.bizType" style="width: 100%">
            <el-option v-for="b in bizTypes" :key="b.code" :label="b.name" :value="b.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="scheduleForm.status" style="width: 100%">
            <el-option label="可预约" value="AVAILABLE" /><el-option label="休息" value="OFF" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleFormVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchedule">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statsVisible" title="技师服务统计" width="620px">
      <div v-if="stats" class="stat-grid">
        <div class="stat-card"><div class="label">累计服务</div><div class="value">{{ stats.serviceCount || 0 }}</div></div>
        <div class="stat-card"><div class="label">累计业绩</div><div class="value">¥{{ money(stats.totalRevenue) }}</div></div>
        <div class="stat-card"><div class="label">评价均分</div><div class="value">{{ stats.rating || '-' }}</div></div>
        <div class="stat-card"><div class="label">提成累计</div><div class="value">¥{{ money(stats.commission) }}</div></div>
      </div>
      <pre v-if="stats" class="json-box">{{ JSON.stringify(stats, null, 2) }}</pre>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Upload, Download } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import request from '@/api/request'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const certVisible = ref(false)
const scheduleVisible = ref(false)
const certFormVisible = ref(false)
const scheduleFormVisible = ref(false)
const statsVisible = ref(false)
const editing = ref(false)
const current = ref<any>(null)
const certs = ref<any[]>([])
const schedules = ref<any[]>([])
const stats = ref<any>(null)
const stores = ref<any[]>([])
const bizTypes = ref<any[]>([])
const skillList = ref<string[]>([])
const positions = ['美容师', '皮肤管理师', 'SPA技师', '口腔医生', '护士', '产康师', '店长']
const query = reactive<any>({ kw: '', storeId: '', position: '', certStatus: '' })
const form = reactive<any>({})
const certForm = reactive<any>({})
const scheduleForm = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) params[k] = v })
    const res: any = await request.get('/tenant/technicians/page', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

function search() { page.value = 1; load() }
function reset() { Object.assign(query, { kw: '', storeId: '', position: '', certStatus: '' }); search() }
function maskPhone(p?: string) { return p && p.length >= 7 ? `${p.slice(0, 3)}****${p.slice(-4)}` : p || '-' }
function money(v: any) { return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function storeName(id: any) { return stores.value.find((s) => s.id === id)?.name || '-' }
function certTag(d: string) {
  if (!d) return 'info'
  const days = dayjs(d).diff(dayjs(), 'day')
  return days < 0 ? 'danger' : days <= 30 ? 'warning' : 'success'
}
function certLabel(d: string) {
  if (!d) return '长期'
  const days = dayjs(d).diff(dayjs(), 'day')
  return days < 0 ? '已过期' : days <= 30 ? `${days} 天后到期` : '有效'
}

function openCreate() {
  editing.value = false
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { status: 1 })
  skillList.value = []
  dialogVisible.value = true
}

function openEdit(row: any) {
  editing.value = true
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  delete form.password
  skillList.value = row.skills ? row.skills.split(',').filter(Boolean) : []
  dialogVisible.value = true
}

async function save() {
  if (!form.name || !form.phone || !form.idCard) { ElMessage.warning('姓名、手机号、身份证号为必填'); return }
  form.skills = skillList.value.join(',')
  saving.value = true
  try {
    if (editing.value) await request.put('/tenant/technicians', form)
    else await request.post('/tenant/technicians', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function disable(row: any) {
  await ElMessageBox.confirm(`确认禁用技师 ${row.name}？`, '提示', { type: 'warning' })
  await request.post(`/tenant/technicians/${row.id}/disable`)
  ElMessage.success('已禁用')
  load()
}

async function openCerts(row: any) {
  current.value = row
  await loadCerts()
  certVisible.value = true
}

async function loadCerts() {
  const res: any = await request.get(`/tenant/technicians/${current.value.id}/certs`)
  certs.value = res.data || []
}

function openCertForm(row?: any) {
  Object.keys(certForm).forEach((k) => delete certForm[k])
  if (row) Object.assign(certForm, row)
  certFormVisible.value = true
}

async function saveCert() {
  if (!certForm.certType) { ElMessage.warning('请填写证书类型'); return }
  if (certForm.id) await request.put(`/tenant/technicians/${current.value.id}/certs/${certForm.id}`, certForm)
  else await request.post(`/tenant/technicians/${current.value.id}/certs`, certForm)
  ElMessage.success('保存成功')
  certFormVisible.value = false
  loadCerts()
  load()
}

async function removeCert(row: any) {
  await ElMessageBox.confirm('确认删除该证书？', '提示', { type: 'warning' })
  await request.delete(`/tenant/technicians/${current.value.id}/certs/${row.id}`)
  ElMessage.success('已删除')
  loadCerts()
}

async function openSchedules(row: any) {
  current.value = row
  await loadSchedules()
  scheduleVisible.value = true
}

async function loadSchedules() {
  const res: any = await request.get(`/tenant/technicians/${current.value.id}/schedules`)
  schedules.value = res.data || []
}

function openScheduleForm(row?: any) {
  Object.keys(scheduleForm).forEach((k) => delete scheduleForm[k])
  if (row) Object.assign(scheduleForm, row)
  else Object.assign(scheduleForm, { workDate: dayjs().format('YYYY-MM-DD'), startTime: '09:00:00', endTime: '18:00:00', status: 'WORK' })
  scheduleFormVisible.value = true
}

async function saveSchedule() {
  if (!scheduleForm.workDate) { ElMessage.warning('请选择日期'); return }
  if (scheduleForm.id) await request.put(`/tenant/technicians/${current.value.id}/schedules/${scheduleForm.id}`, scheduleForm)
  else await request.post(`/tenant/technicians/${current.value.id}/schedules`, scheduleForm)
  ElMessage.success('保存成功')
  scheduleFormVisible.value = false
  loadSchedules()
}

async function removeSchedule(row: any) {
  await ElMessageBox.confirm('确认删除该排班？', '提示', { type: 'warning' })
  await request.delete(`/tenant/technicians/${current.value.id}/schedules/${row.id}`)
  ElMessage.success('已删除')
  loadSchedules()
}

async function openStats(row: any) {
  const res: any = await request.get(`/tenant/technicians/${row.id}/stats`)
  stats.value = res.data || {}
  statsVisible.value = true
}

function exportExcel() {
  const token = localStorage.getItem('meiye_token')
  fetch('/api/tenant/technicians/export', { headers: { Authorization: `Bearer ${token}` } })
    .then((r) => r.blob())
    .then((blob) => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = 'technicians.xlsx'
      a.click()
    })
}

async function doImport(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  const res: any = await request.post('/tenant/technicians/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  ElMessage.success(`导入完成：成功 ${res.data?.success || 0} 条，失败 ${res.data?.failed || 0} 条`)
  load()
}

onMounted(async () => {
  load()
  const [s, p] = await Promise.all([
    request.get('/tenant/stores/list'),
    request.get('/tenant/biz-types')
  ])
  stores.value = (s as any).data || []
  bizTypes.value = ((p as any).data || []).filter((x: any) => x.enabled)
})
</script>

<style scoped>
.json-box { background: #f7f7fb; border-radius: 8px; padding: 12px; font-size: 12px; max-height: 260px; overflow: auto; }
</style>
