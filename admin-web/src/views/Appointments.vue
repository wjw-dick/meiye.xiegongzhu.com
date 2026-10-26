<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">预约排班</h2>
        <div class="page-subtitle">分时预约、预约看板、改约/取消、技师调度、冲突检测、超时释放</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">新建预约</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-date-picker v-model="boardDate" type="date" value-format="YYYY-MM-DD" style="width: 160px" @change="loadBoard" />
      <el-select v-model="boardStore" placeholder="全部门店" clearable style="width: 170px" @change="loadBoard">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-radio-group v-model="view" @change="onViewChange">
        <el-radio-button label="board">预约看板</el-radio-button>
        <el-radio-button label="list">预约列表</el-radio-button>
      </el-radio-group>
    </div>

    <div v-show="view === 'board'" class="board" v-loading="loading">
      <div v-if="!board.length" class="empty-board">当日暂无预约</div>
      <div v-for="item in board" :key="item.id" class="appt-card" :class="'st-' + item.status">
        <div class="appt-time">{{ dayjs(item.start_time).format('HH:mm') }} - {{ dayjs(item.end_time).format('HH:mm') }}</div>
        <div class="appt-main">
          <div class="appt-customer">{{ item.customer_name }} <span class="phone">{{ maskPhone(item.customer_phone) }}</span></div>
          <div class="appt-item">{{ item.item_name || '套餐服务' }} · {{ item.technician_name || '待分配' }}</div>
        </div>
        <div class="appt-right">
          <el-tag :type="statusType(item.status)" size="small">{{ statusLabel(item.status) }}</el-tag>
          <el-dropdown trigger="click" @command="(cmd: string) => handleAction(item, cmd)">
            <el-button size="small" text :icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="item.status === 'PENDING'" command="confirm">确认预约</el-dropdown-item>
                <el-dropdown-item v-if="item.status === 'CONFIRMED'" command="arrive">确认到店</el-dropdown-item>
                <el-dropdown-item v-if="item.status === 'ARRIVED'" command="start">开始服务</el-dropdown-item>
                <el-dropdown-item v-if="item.status === 'SERVING'" command="done">完成服务</el-dropdown-item>
                <el-dropdown-item command="remind">催到店提醒</el-dropdown-item>
                <el-dropdown-item command="logs">操作记录</el-dropdown-item>
                <el-dropdown-item command="cancel" divided>取消预约</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <div v-show="view === 'list'">
      <div class="filter-bar">
        <el-input v-model="query.kw" placeholder="预约编号" clearable style="width: 190px" @keyup.enter="loadList" />
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="loadList">
          <el-option v-for="(v, k) in statusMap" :key="k" :label="v" :value="k" />
        </el-select>
        <el-select v-model="query.storeId" placeholder="门店" clearable style="width: 160px" @change="loadList">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="appointmentNo" label="预约编号" width="190" />
        <el-table-column prop="bizType" label="品类" width="110" />
        <el-table-column prop="customerId" label="客户ID" width="90" />
        <el-table-column prop="technicianId" label="技师ID" width="90" />
        <el-table-column prop="startTime" label="预约时间" width="170">
          <template #default="{ row }">{{ fmt(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="170">
          <template #default="{ row }">{{ fmt(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="100" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PENDING'" link type="primary" @click="handleAction(row, 'confirm')">确认</el-button>
            <el-button v-if="row.status === 'CONFIRMED'" link type="primary" @click="handleAction(row, 'arrive')">到店</el-button>
            <el-button v-if="row.status === 'ARRIVED'" link type="primary" @click="handleAction(row, 'start')">开始</el-button>
            <el-button v-if="row.status === 'SERVING'" link type="success" @click="handleAction(row, 'done')">完成</el-button>
            <el-button link type="warning" @click="handleAction(row, 'cancel')">取消</el-button>
            <el-button link @click="handleAction(row, 'logs')">记录</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="flex-between mt-16">
        <span class="text-muted">共 {{ total }} 条</span>
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                       layout="sizes, prev, pager, next" :page-sizes="[10, 20, 50]"
                       @size-change="loadList" @current-change="loadList" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="新建预约" width="720px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户" required>
              <el-select v-model="form.customerId" filterable remote :remote-method="searchCustomers" style="width: 100%" placeholder="输入姓名/手机号搜索">
                <el-option v-for="c in customers" :key="c.id" :label="`${c.name} / ${maskPhone(c.phone)}`" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店" required>
              <el-select v-model="form.storeId" style="width: 100%" @change="onStoreChange">
                <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务品类" required>
              <el-select v-model="form.bizType" style="width: 100%" @change="onItemChange">
                <el-option v-for="b in bizTypes" :key="b.code" :label="b.name" :value="b.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务项目" required>
              <el-select v-model="form.itemId" filterable style="width: 100%" @change="onItemChange">
                <el-option v-for="i in filteredItems" :key="i.id" :label="`${i.name}（${i.duration || 30}分钟）`" :value="i.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预约日期">
              <el-date-picker v-model="form.date" type="date" value-format="YYYY-MM-DD" style="width: 100%" @change="loadSlots" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技师">
              <el-select v-model="form.technicianId" clearable style="width: 100%" @change="loadSlots">
                <el-option v-for="t in techs" :key="t.id" :label="t.name" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="可选时段">
              <div class="slot-grid">
                <div v-for="(s, idx) in slots" :key="idx" class="slot" :class="{ active: form.startTime === s.time }" @click="pickSlot(s)">
                  {{ dayjs(s.time).format('HH:mm') }} · {{ s.technicianName }}
                </div>
                <span v-if="!slots.length" class="text-muted">该日期暂无可约时段，请检查排班</span>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">提交预约</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="logsVisible" title="预约操作记录" width="620px">
      <el-timeline>
        <el-timeline-item v-for="l in logs" :key="l.id" :timestamp="fmt(l.createdAt)">
          {{ l.action }} {{ l.remark || '' }}
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, MoreFilled } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import request from '@/api/request'

const statusMap: Record<string, string> = {
  PENDING: '待确认', CONFIRMED: '已确认', ARRIVED: '已到店', SERVING: '服务中',
  DONE: '已完成', CANCELLED: '已取消', NO_SHOW: '未到店'
}

const view = ref<'board' | 'list'>('board')
const boardDate = ref(dayjs().format('YYYY-MM-DD'))
const boardStore = ref<number | ''>('')
const board = ref<any[]>([])
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const logsVisible = ref(false)
const logs = ref<any[]>([])
const stores = ref<any[]>([])
const customers = ref<any[]>([])
const items = ref<any[]>([])
const techs = ref<any[]>([])
const bizTypes = ref<any[]>([])
const slots = ref<any[]>([])
const query = reactive<any>({ kw: '', status: '', storeId: '' })
const form = reactive<any>({ date: dayjs().format('YYYY-MM-DD') })

const filteredItems = computed(() => items.value.filter((i) => !form.bizType || i.bizType === form.bizType))

async function loadBoard() {
  loading.value = true
  try {
    const res: any = await request.get('/tenant/appointments/board', {
      params: { date: boardDate.value, storeId: boardStore.value || undefined }
    })
    board.value = res.data || []
  } finally { loading.value = false }
}

async function loadList() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) params[k] = v })
    const res: any = await request.get('/tenant/appointments/page', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}

function loadAll() {
  loadBoard()
  loadList()
}

function onViewChange() {
  if (view.value === 'board') loadBoard()
  else loadList()
}

function statusType(s: string) {
  if (['CONFIRMED', 'ARRIVED', 'SERVING', 'DONE'].includes(s)) return 'success'
  if (s === 'PENDING') return 'warning'
  if (['CANCELLED', 'NO_SHOW'].includes(s)) return 'danger'
  return 'info'
}
function statusLabel(s: string) { return statusMap[s] || s }
function maskPhone(p?: string) { return p && p.length >= 7 ? `${p.slice(0, 3)}****${p.slice(-4)}` : p || '-' }
function fmt(v: any) { return v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-' }

async function handleAction(row: any, cmd: string) {
  if (cmd === 'logs') {
    const res: any = await request.get(`/tenant/appointments/${row.id}/logs`)
    logs.value = res.data || []
    logsVisible.value = true
    return
  }
  if (cmd === 'cancel') {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约', { inputValue: '客户行程变更' })
    await request.post(`/tenant/appointments/${row.id}/cancel`, null, { params: { reason: value } })
    ElMessage.success('预约已取消')
    loadAll()
    return
  }
  await request.post(`/tenant/appointments/${row.id}/${cmd}`)
  ElMessage.success('操作成功')
  loadAll()
}

async function searchCustomers(kw: string) {
  const res: any = await request.get('/tenant/customers/page', { params: { page: 1, size: 20, kw } })
  customers.value = res.data.records || []
}

async function loadSlots() {
  if (!form.storeId || !form.date) return
  const res: any = await request.get('/tenant/appointments/slots', {
    params: { storeId: form.storeId, itemId: form.itemId, date: form.date, technicianId: form.technicianId || undefined }
  })
  slots.value = res.data || []
}

function pickSlot(s: any) {
  form.startTime = s.time
  form.endTime = s.endTime
  form.technicianId = s.technicianId
}

function onStoreChange() {
  form.itemId = undefined
  form.technicianId = undefined
  loadSlots()
  loadTechs()
}

function onItemChange() {
  const item = items.value.find((i) => i.id === form.itemId)
  if (item?.bizType && !form.bizType) form.bizType = item.bizType
  loadSlots()
}

async function loadTechs() {
  if (!form.storeId) return
  const res: any = await request.get('/tenant/appointments/available-techs', {
    params: { storeId: form.storeId, bizType: form.bizType || undefined }
  })
  techs.value = res.data || []
}

async function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, { date: dayjs().format('YYYY-MM-DD'), storeId: stores.value[0]?.id, bizType: bizTypes.value[0]?.code })
  customers.value = []
  slots.value = []
  dialogVisible.value = true
  await Promise.all([loadTechs(), loadSlots()])
}

async function save() {
  if (!form.customerId || !form.storeId || !form.itemId || !form.startTime) {
    ElMessage.warning('请完整选择客户、门店、项目与时段')
    return
  }
  saving.value = true
  try {
    await request.post('/tenant/appointments/create', {
      customerId: form.customerId,
      storeId: form.storeId,
      technicianId: form.technicianId,
      itemId: form.itemId,
      bizType: form.bizType,
      startTime: form.startTime,
      endTime: form.endTime,
      remark: form.remark
    })
    ElMessage.success('预约创建成功')
    dialogVisible.value = false
    loadAll()
  } finally { saving.value = false }
}

onMounted(async () => {
  loadAll()
  const [s, i, b] = await Promise.all([
    request.get('/tenant/stores/list'),
    request.get('/tenant/service-items/list'),
    request.get('/tenant/biz-types')
  ])
  stores.value = (s as any).data || []
  items.value = (i as any).data || []
  bizTypes.value = ((b as any).data || []).filter((x: any) => x.enabled)
})
</script>

<style scoped>
.board { display: flex; flex-direction: column; gap: 10px; }
.empty-board { padding: 60px; text-align: center; color: #9aa0b2; background: #fafafd; border-radius: 12px; }
.appt-card {
  display: flex; align-items: center; gap: 16px; padding: 14px 18px; border-radius: 12px;
  background: #fafafd; border-left: 4px solid #6b4eff;
}
.appt-card.st-PENDING { border-left-color: #e6a23c; }
.appt-card.st-CONFIRMED { border-left-color: #409eff; }
.appt-card.st-SERVING { border-left-color: #67c23a; }
.appt-card.st-DONE { border-left-color: #909399; opacity: 0.8; }
.appt-card.st-CANCELLED, .appt-card.st-NO_SHOW { border-left-color: #f56c6c; opacity: 0.7; }
.appt-time { width: 120px; font-weight: 600; color: #4a4f63; }
.appt-main { flex: 1; }
.appt-customer { font-size: 15px; font-weight: 600; }
.appt-customer .phone { font-size: 12px; color: #9aa0b2; margin-left: 8px; font-weight: 400; }
.appt-item { font-size: 12px; color: #8a90a2; margin-top: 3px; }
.appt-right { display: flex; align-items: center; gap: 8px; }
.slot-grid { display: flex; flex-wrap: wrap; gap: 8px; }
.slot { padding: 7px 12px; border: 1px solid #e3e5ee; border-radius: 8px; cursor: pointer; font-size: 12px; transition: all 0.2s; }
.slot:hover { border-color: #6b4eff; color: #6b4eff; }
.slot.active { background: #6b4eff; border-color: #6b4eff; color: #fff; }
:deep(.board .el-tag) { margin-right: 2px; }
</style>
