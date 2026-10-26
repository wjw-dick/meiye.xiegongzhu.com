<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">客户档案</h2>
        <div class="page-subtitle">会员档案、标签等级、消费充值、欠款、关联预约/订单/卡项/评价聚合</div>
      </div>
      <div class="flex gap-8">
        <el-upload :show-file-list="false" :http-request="doImport" accept=".xlsx,.xls">
          <el-button :icon="Upload">批量导入</el-button>
        </el-upload>
        <el-button :icon="Download" @click="exportExcel">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新增客户</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.kw" placeholder="姓名 / 编号 / 手机号" clearable style="width: 220px" @keyup.enter="search" />
      <el-select v-model="query.level" placeholder="会员等级" clearable style="width: 150px" @change="search">
        <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="search">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-select v-model="query.storeId" placeholder="所属门店" clearable style="width: 160px" @change="search">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="customerNo" label="会员编号" width="170" />
      <el-table-column prop="name" label="姓名" width="110" />
      <el-table-column label="手机号" width="130">
        <template #default="{ row }">{{ maskPhone(row.phone) }}</template>
      </el-table-column>
      <el-table-column prop="level" label="等级" width="110">
        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.level }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="tags" label="标签" min-width="150" show-overflow-tooltip>
        <template #default="{ row }">
          <el-tag v-for="t in splitTags(row.tags)" :key="t" size="small" style="margin-right: 4px">{{ t }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="90" />
      <el-table-column label="累计消费" width="120">
        <template #default="{ row }">¥{{ money(row.totalConsume) }}</template>
      </el-table-column>
      <el-table-column label="欠款" width="110">
        <template #default="{ row }">
          <span :class="{ danger: Number(row.debt) > 0 }">¥{{ money(row.debt) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="lastVisitAt" label="最近到店" width="160">
        <template #default="{ row }">{{ fmt(row.lastVisitAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openRelated(row)">关联视图</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openPoints(row)">积分</el-button>
          <el-button v-if="row.status === 1" link type="danger" @click="disable(row)">禁用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 位会员</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                     :page-sizes="[10, 20, 50]" layout="sizes, prev, pager, next"
                     @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑客户' : '新增客户'" width="720px" destroy-on-close>
      <el-form :model="form" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="手机号" required><el-input v-model="form.phone" maxlength="11" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="身份证号"><el-input v-model="form.idCard" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="性别">
            <el-select v-model="form.gender" clearable style="width: 100%">
              <el-option label="女" value="F" /><el-option label="男" value="M" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="生日">
            <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="会员等级">
            <el-select v-model="form.level" style="width: 100%">
              <el-option v-for="l in levels" :key="l" :label="l" :value="l" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属门店">
            <el-select v-model="form.storeId" clearable style="width: 100%">
              <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="跟进技师">
            <el-select v-model="form.followerId" clearable style="width: 100%">
              <el-option v-for="t in techs" :key="t.id" :label="t.name" :value="t.id" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="会员标签">
            <el-select v-model="tagList" multiple filterable allow-create default-first-option style="width: 100%">
              <el-option v-for="t in tagOptions" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="登录密码">
            <el-input v-model="form.password" type="password" show-password placeholder="新增可选，用于小程序登录" />
          </el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注">
            <el-input v-model="form.remark" type="textarea" :rows="2" />
          </el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="relatedVisible" title="客户关联视图" size="70%">
      <div v-if="related.customer" class="related-head">
        <el-avatar :size="48" style="background: var(--my-primary)">{{ related.customer.name?.slice(0, 1) }}</el-avatar>
        <div>
          <div style="font-size: 17px; font-weight: 600">{{ related.customer.name }}</div>
          <div class="text-muted">{{ related.customer.customerNo }} · {{ maskPhone(related.customer.phone) }} · {{ related.customer.level }}</div>
        </div>
        <div class="related-metrics">
          <div><b>¥{{ money(related.customer.totalConsume) }}</b><span>累计消费</span></div>
          <div><b>¥{{ money(related.customer.totalRecharge) }}</b><span>累计充值</span></div>
          <div><b>{{ related.customer.points || 0 }}</b><span>积分</span></div>
        </div>
      </div>
      <el-tabs>
        <el-tab-pane label="预约">
          <el-table :data="related.appointments || []" size="small">
            <el-table-column prop="appointmentNo" label="预约号" width="180" />
            <el-table-column prop="bizType" label="品类" width="110" />
            <el-table-column prop="startTime" label="开始时间" width="170" />
            <el-table-column prop="status" label="状态" width="100" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="订单">
          <el-table :data="related.orders || []" size="small">
            <el-table-column prop="orderNo" label="订单号" width="190" />
            <el-table-column prop="bizType" label="品类" width="110" />
            <el-table-column prop="paidAmount" label="实付" width="100" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="paidAt" label="支付时间" width="170" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="卡项">
          <el-table :data="related.cards || []" size="small">
            <el-table-column prop="cardNo" label="卡号" width="180" />
            <el-table-column prop="name" label="卡名称" min-width="140" />
            <el-table-column prop="balance" label="余额" width="100" />
            <el-table-column prop="times" label="次数" width="80" />
            <el-table-column prop="status" label="状态" width="100" />
            <el-table-column prop="expireAt" label="到期" width="170" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="评价">
          <el-table :data="related.reviews || []" size="small">
            <el-table-column prop="score" label="评分" width="80" />
            <el-table-column prop="content" label="内容" min-width="240" />
            <el-table-column prop="reply" label="回复" min-width="180" />
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="积分流水">
          <el-table :data="related.points || []" size="small">
            <el-table-column prop="points" label="积分" width="90" />
            <el-table-column prop="type" label="类型" width="90" />
            <el-table-column prop="remark" label="备注" min-width="200" />
            <el-table-column prop="createdAt" label="时间" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <el-dialog v-model="pointsVisible" title="调整积分" width="420px">
      <el-form label-width="90px">
        <el-form-item label="会员">{{ currentRow?.name }}</el-form-item>
        <el-form-item label="积分变动"><el-input-number v-model="pointForm.points" :step="10" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="pointForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsVisible = false">取消</el-button>
        <el-button type="primary" @click="savePoints">确定</el-button>
      </template>
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
const relatedVisible = ref(false)
const pointsVisible = ref(false)
const editing = ref(false)
const currentRow = ref<any>(null)
const related = ref<any>({})
const stores = ref<any[]>([])
const techs = ref<any[]>([])
const tagList = ref<string[]>([])
const levels = ['普通会员', '银卡会员', '金卡会员', '铂金会员', '黑金会员']
const tagOptions = ['敏感肌', '孕产', '学生', '高净值', '沉睡客户', '转介绍']
const query = reactive<any>({ kw: '', level: '', status: '', storeId: '' })
const form = reactive<any>({})
const pointForm = reactive<any>({ points: 0, remark: '后台调整' })

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) params[k] = v })
    const res: any = await request.get('/tenant/customers/page', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

function search() { page.value = 1; load() }
function reset() { Object.assign(query, { kw: '', level: '', status: '', storeId: '' }); search() }
function maskPhone(p?: string) { return p && p.length >= 7 ? `${p.slice(0, 3)}****${p.slice(-4)}` : p || '-' }
function money(v: any) { return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function fmt(v: any) { return v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-' }
function splitTags(v: string) { return v ? v.split(/[,，]/).filter(Boolean) : [] }

function openCreate() {
  editing.value = false
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { level: '普通会员', status: 1, gender: 'F' })
  tagList.value = []
  dialogVisible.value = true
}

function openEdit(row: any) {
  editing.value = true
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  delete form.password
  tagList.value = splitTags(row.tags)
  dialogVisible.value = true
}

async function save() {
  if (!form.name || !form.phone) { ElMessage.warning('姓名和手机号为必填'); return }
  form.tags = tagList.value.join(',')
  saving.value = true
  try {
    if (editing.value) await request.put('/tenant/customers', form)
    else await request.post('/tenant/customers', form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}

async function disable(row: any) {
  await ElMessageBox.confirm(`确认禁用客户 ${row.name}？`, '提示', { type: 'warning' })
  await request.post(`/tenant/customers/${row.id}/disable`)
  ElMessage.success('已禁用')
  load()
}

async function openRelated(row: any) {
  const res: any = await request.get(`/tenant/customers/${row.id}/related`)
  related.value = res.data || {}
  relatedVisible.value = true
}

function openPoints(row: any) {
  currentRow.value = row
  pointForm.points = 0
  pointForm.remark = '后台调整'
  pointsVisible.value = true
}

async function savePoints() {
  await request.post(`/tenant/customers/${currentRow.value.id}/points`, null, {
    params: { points: pointForm.points, remark: pointForm.remark }
  })
  ElMessage.success('积分调整成功')
  pointsVisible.value = false
  load()
}

function exportExcel() {
  const token = localStorage.getItem('meiye_token')
  const drill = localStorage.getItem('meiye_drill_tenant')
  const qs = new URLSearchParams()
  if (drill) qs.set('X-Tenant-Id', drill)
  fetch(`/api/tenant/customers/export?${qs.toString()}`, { headers: { Authorization: `Bearer ${token}` } })
    .then((r) => r.blob())
    .then((blob) => {
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = 'customers.xlsx'
      a.click()
    })
}

async function doImport(options: any) {
  const fd = new FormData()
  fd.append('file', options.file)
  const res: any = await request.post('/tenant/customers/import', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  ElMessage.success(`导入完成：成功 ${res.data?.success || 0} 条，失败 ${res.data?.failed || 0} 条`)
  load()
}

onMounted(async () => {
  load()
  const [s, t] = await Promise.all([
    request.get('/tenant/stores/list'),
    request.get('/tenant/technicians/list')
  ])
  stores.value = (s as any).data || []
  techs.value = (t as any).data || []
})
</script>

<style scoped>
.danger { color: #e74c3c; font-weight: 600; }
.related-head { display: flex; align-items: center; gap: 14px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f5; margin-bottom: 8px; }
.related-metrics { margin-left: auto; display: flex; gap: 36px; }
.related-metrics div { display: flex; flex-direction: column; align-items: flex-end; }
.related-metrics b { font-size: 18px; color: #2f3448; }
.related-metrics span { font-size: 12px; color: #9aa0b2; }
</style>
