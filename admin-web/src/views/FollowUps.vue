<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">回访任务</h2>
        <div class="page-subtitle">消费后自动生成回访、到期提醒与回访结果登记（时光机）</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="MagicStick" @click="generate">生成消费回访</el-button>
        <el-button type="warning" :icon="Plus" @click="openCreate">新建回访</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="search">
        <el-option label="待回访" value="PENDING" /><el-option label="已完成" value="DONE" /><el-option label="已取消" value="CANCELLED" />
      </el-select>
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 140px" @change="search">
        <el-option label="消费回访" value="CONSUME" /><el-option label="生日关怀" value="BIRTHDAY" /><el-option label="沉睡唤醒" value="SLEEP" /><el-option label="卡项到期" value="EXPIRE" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <el-tag v-if="dueCount" type="danger" effect="plain">今日到期 {{ dueCount }} 条</el-tag>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="customerId" label="会员ID" width="100" />
      <el-table-column prop="orderId" label="订单ID" width="100" />
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ typeText(row.type) }}</template>
      </el-table-column>
      <el-table-column prop="content" label="回访内容" min-width="220" show-overflow-tooltip />
      <el-table-column label="计划时间" width="160"><template #default="{ row }">{{ fmt(row.dueAt) }}</template></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="result" label="回访结果" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" link type="success" @click="complete(row)">完成回访</el-button>
          <el-button v-else link type="primary" @click="view(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 条回访任务</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="createVisible" title="新建回访任务" width="560px">
      <el-form label-width="100px">
        <el-form-item label="会员" required>
          <el-select v-model="form.customerId" filterable remote :remote-method="searchCustomers" placeholder="搜索会员" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${maskPhone(c.phone)}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="回访类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="消费回访" value="CONSUME" /><el-option label="生日关怀" value="BIRTHDAY" />
            <el-option label="沉睡唤醒" value="SLEEP" /><el-option label="卡项到期" value="EXPIRE" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划时间">
          <el-date-picker v-model="form.dueAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="回访内容"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="completeVisible" title="登记回访结果" width="520px">
      <el-form label-width="90px">
        <el-form-item label="回访内容">{{ current?.content }}</el-form-item>
        <el-form-item label="回访结果">
          <el-input v-model="resultText" type="textarea" :rows="3" placeholder="客户反馈、满意度、复购意向" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doComplete">确认完成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="viewVisible" title="回访详情" width="520px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="会员ID">{{ current?.customerId }}</el-descriptions-item>
        <el-descriptions-item label="回访类型">{{ typeText(current?.type) }}</el-descriptions-item>
        <el-descriptions-item label="计划时间">{{ fmt(current?.dueAt) }}</el-descriptions-item>
        <el-descriptions-item label="回访内容">{{ current?.content }}</el-descriptions-item>
        <el-descriptions-item label="回访结果">{{ current?.result || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search, Plus, MagicStick } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt, maskPhone, statusTag, statusText } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ status: '', type: '' })
const dueCount = ref(0)
const customers = ref<any[]>([])
const createVisible = ref(false)
const completeVisible = ref(false)
const viewVisible = ref(false)
const current = ref<any>(null)
const resultText = ref('')
const form = reactive<any>({ customerId: null, type: 'CONSUME', dueAt: '', content: '消费后回访：服务体验、效果与复购意向' })

function typeText(v: any) {
  return ({ CONSUME: '消费回访', BIRTHDAY: '生日关怀', SLEEP: '沉睡唤醒', EXPIRE: '卡项到期' } as any)[v] || v
}
async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v) params[k] = v })
    const [list, due] = await Promise.all([
      request.get('/tenant/follow-ups/page', { params }),
      request.get('/tenant/follow-ups/due')
    ]) as any[]
    rows.value = list.data.records || []
    total.value = list.data.total || 0
    dueCount.value = (due.data || []).length
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { status: '', type: '' }); search() }
async function searchCustomers(kw: string) {
  const res: any = await request.get('/tenant/customers/page', { params: { page: 1, size: 20, kw } })
  customers.value = res.data.records || []
}
function openCreate() {
  Object.assign(form, { customerId: null, type: 'CONSUME', dueAt: '', content: '消费后回访：服务体验、效果与复购意向' })
  searchCustomers('')
  createVisible.value = true
}
async function doCreate() {
  if (!form.customerId) { ElMessage.warning('请选择会员'); return }
  saving.value = true
  try {
    await request.post('/tenant/follow-ups', form)
    ElMessage.success('回访任务已创建')
    createVisible.value = false
    load()
  } finally { saving.value = false }
}
async function generate() {
  const res: any = await request.post('/tenant/follow-ups/generate', null, { params: { days: 7 } })
  ElMessage.success(`已生成 ${res.data?.created || 0} 条消费回访`)
  load()
}
function complete(row: any) {
  current.value = row
  resultText.value = ''
  completeVisible.value = true
}
async function doComplete() {
  saving.value = true
  try {
    await request.post(`/tenant/follow-ups/${current.value.id}/complete`, null, { params: { result: resultText.value } })
    ElMessage.success('回访已完成')
    completeVisible.value = false
    load()
  } finally { saving.value = false }
}
function view(row: any) { current.value = row; viewVisible.value = true }
onMounted(load)
</script>
