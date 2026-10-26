<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">会员卡</h2>
        <div class="page-subtitle">开卡、储值充值、次卡核销、卡流水与到期/低余额提醒</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openIssue">开卡</el-button>
      </div>
    </div>

    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="会员卡列表" name="all" />
      <el-tab-pane label="即将到期" name="expiring" />
      <el-tab-pane label="低余额储值卡" name="low" />
    </el-tabs>

    <div v-if="tab === 'all'" class="filter-bar">
      <el-input v-model="query.kw" placeholder="卡号 / 会员姓名" clearable style="width: 200px" @keyup.enter="search" />
      <el-select v-model="query.type" placeholder="卡类型" clearable style="width: 140px" @change="search">
        <el-option v-for="t in cardTypeOptions" :key="t.value" :label="t.label" :value="t.value" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="search">
        <el-option label="正常" value="ACTIVE" /><el-option label="已过期" value="EXPIRED" /><el-option label="已冻结" value="FROZEN" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="cardNo" label="卡号" width="180" />
      <el-table-column prop="customerName" label="会员" width="110" />
      <el-table-column prop="name" label="卡名称" min-width="150" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ cardTypeText(row.type) }}</template>
      </el-table-column>
      <el-table-column label="余额" width="110"><template #default="{ row }">¥{{ money(row.balance) }}</template></el-table-column>
      <el-table-column prop="times" label="剩余次数" width="100" />
      <el-table-column label="折扣" width="80"><template #default="{ row }">{{ row.discount || '-' }}</template></el-table-column>
      <el-table-column label="有效期至" width="130"><template #default="{ row }">{{ row.expireAt ? fmt(row.expireAt, 'YYYY-MM-DD') : '长期有效' }}</template></el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ cardStatusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">流水</el-button>
          <el-button v-if="row.type === 'STORED'" link type="success" @click="openRecharge(row)">充值</el-button>
          <el-button link type="warning" @click="openConsume(row)">核销</el-button>
          <el-button link @click="printCard(row)">打印</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="tab === 'all'" class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 张卡</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="issueVisible" title="会员开卡" width="640px" destroy-on-close>
      <el-form label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="会员" required>
              <el-select v-model="issueForm.customerId" filterable remote :remote-method="searchCustomers" placeholder="搜索会员" style="width: 100%">
                <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${maskPhone(c.phone)}）`" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="卡种" required>
              <el-select v-model="issueForm.cardTypeId" filterable style="width: 100%" @change="onCardTypeChange">
                <el-option v-for="t in cardTypes" :key="t.id" :label="`${t.name}（¥${money(t.price)}）`" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店">
              <el-select v-model="issueForm.storeId" clearable style="width: 100%">
                <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="实收金额"><el-input-number v-model="issueForm.paidAmount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="附属主卡ID"><el-input-number v-model="issueForm.parentId" :min="1" style="width: 100%" placeholder="附属卡可选" /></el-form-item>
          </el-col>
        </el-row>
        <el-alert v-if="selectedType" type="info" :closable="false" show-icon
                  :title="`${selectedType.name}：面值 ¥${money(selectedType.faceValue)}，${selectedType.times || 0} 次，有效期 ${selectedType.validDays || 0} 天`" />
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doIssue">确认开卡</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rechargeVisible" title="储值充值" width="460px">
      <el-form label-width="100px">
        <el-form-item label="会员卡">{{ currentCard?.cardNo }} · {{ currentCard?.customerName }}</el-form-item>
        <el-form-item label="当前余额">¥{{ money(currentCard?.balance) }}</el-form-item>
        <el-form-item label="充值金额"><el-input-number v-model="rechargeForm.amount" :min="0.01" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="赠送金额"><el-input-number v-model="rechargeForm.bonus" :min="0" :precision="2" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doRecharge">确认充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="consumeVisible" title="会员卡核销" width="460px">
      <el-form label-width="100px">
        <el-form-item label="会员卡">{{ currentCard?.cardNo }} · {{ currentCard?.customerName }}</el-form-item>
        <el-form-item label="扣减金额"><el-input-number v-model="consumeForm.amount" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="扣减次数"><el-input-number v-model="consumeForm.times" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="关联订单"><el-input-number v-model="consumeForm.orderId" :min="1" style="width: 100%" placeholder="可选" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="consumeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doConsume">确认核销</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="卡流水" size="680px">
      <template v-if="cardDetail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="卡号">{{ cardDetail.card.cardNo }}</el-descriptions-item>
          <el-descriptions-item label="会员">{{ cardDetail.card.customerName || cardDetail.card.customerId }}</el-descriptions-item>
          <el-descriptions-item label="余额">¥{{ money(cardDetail.card.balance) }}</el-descriptions-item>
          <el-descriptions-item label="剩余次数">{{ cardDetail.card.times }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ cardStatusText(cardDetail.card.status) }}</el-descriptions-item>
          <el-descriptions-item label="有效期">{{ cardDetail.card.expireAt ? fmt(cardDetail.card.expireAt, 'YYYY-MM-DD') : '长期有效' }}</el-descriptions-item>
        </el-descriptions>
        <el-table :data="cardDetail.flows" size="small" border class="mt-16">
          <el-table-column label="类型" width="100"><template #default="{ row }">{{ flowTypeText(row.type) }}</template></el-table-column>
          <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
          <el-table-column prop="times" label="次数" width="80" />
          <el-table-column prop="balanceAfter" label="余额" width="100" />
          <el-table-column prop="remark" label="备注" min-width="160" />
          <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        </el-table>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt, maskPhone, statusTag, download } from '@/utils/format'

const cardTypeOptions = [
  { label: '计次卡', value: 'COUNT' }, { label: '时间卡', value: 'TIME' }, { label: '套餐卡', value: 'PACKAGE' },
  { label: '组合礼包', value: 'COMBO' }, { label: '储值卡', value: 'STORED' }, { label: '附属卡', value: 'SUB' }
]
const tab = ref('all')
const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ kw: '', type: '', status: '' })
const cardTypes = ref<any[]>([])
const stores = ref<any[]>([])
const customers = ref<any[]>([])

const issueVisible = ref(false)
const issueForm = reactive<any>({ customerId: null, cardTypeId: null, storeId: null, paidAmount: 0, parentId: null })
const selectedType = computed(() => cardTypes.value.find((t) => t.id === issueForm.cardTypeId))

const rechargeVisible = ref(false)
const rechargeForm = reactive<any>({ amount: 0, bonus: 0 })
const consumeVisible = ref(false)
const consumeForm = reactive<any>({ amount: 0, times: 0, orderId: null })
const currentCard = ref<any>(null)
const detailVisible = ref(false)
const cardDetail = ref<any>(null)

function cardTypeText(v: any) { return cardTypeOptions.find((t) => t.value === v)?.label || v }
function cardStatusText(v: any) {
  return ({ ACTIVE: '正常', EXPIRED: '已过期', FROZEN: '已冻结', USED: '已用完' } as any)[v] || v
}
function flowTypeText(v: any) {
  return ({ ISSUE: '开卡', RECHARGE: '充值', CONSUME: '核销', REFUND: '退款', ADJUST: '调整', EXPIRE: '过期' } as any)[v] || v
}

async function load() {
  loading.value = true
  try {
    if (tab.value === 'expiring') {
      const res: any = await request.get('/tenant/member-cards/expiring', { params: { days: 30 } })
      rows.value = res.data || []
      total.value = rows.value.length
    } else if (tab.value === 'low') {
      const res: any = await request.get('/tenant/member-cards/low-balance', { params: { threshold: 100 } })
      rows.value = res.data || []
      total.value = rows.value.length
    } else {
      const res: any = await request.get('/tenant/member-cards/page', { params: { page: page.value, size: size.value, ...query } })
      rows.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { kw: '', type: '', status: '' }); search() }

async function loadBase() {
  const [ct, st] = await Promise.all([
    request.get('/tenant/card-types/list'), request.get('/tenant/stores/list')
  ]) as any[]
  cardTypes.value = ct.data || []
  stores.value = st.data || []
}
async function searchCustomers(kw: string) {
  const res: any = await request.get('/tenant/customers/page', { params: { page: 1, size: 20, kw } })
  customers.value = res.data.records || []
}
function openIssue() {
  Object.assign(issueForm, { customerId: null, cardTypeId: null, storeId: null, paidAmount: 0, parentId: null })
  searchCustomers('')
  issueVisible.value = true
}
function onCardTypeChange(id: any) {
  const t = cardTypes.value.find((x) => x.id === id)
  if (t) issueForm.paidAmount = Number(t.price || 0)
}
async function doIssue() {
  if (!issueForm.customerId || !issueForm.cardTypeId) { ElMessage.warning('请选择会员和卡种'); return }
  saving.value = true
  try {
    await request.post('/tenant/member-cards/issue', issueForm)
    ElMessage.success('开卡成功')
    issueVisible.value = false
    load()
  } finally { saving.value = false }
}
function openRecharge(row: any) {
  currentCard.value = row
  Object.assign(rechargeForm, { amount: 0, bonus: 0 })
  rechargeVisible.value = true
}
async function doRecharge() {
  if (!rechargeForm.amount) { ElMessage.warning('请填写充值金额'); return }
  saving.value = true
  try {
    await request.post(`/tenant/member-cards/${currentCard.value.id}/recharge`, rechargeForm)
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    load()
  } finally { saving.value = false }
}
function openConsume(row: any) {
  currentCard.value = row
  Object.assign(consumeForm, { amount: 0, times: 0, orderId: null })
  consumeVisible.value = true
}
async function doConsume() {
  saving.value = true
  try {
    await request.post(`/tenant/member-cards/${currentCard.value.id}/consume`, consumeForm)
    ElMessage.success('核销成功')
    consumeVisible.value = false
    load()
  } finally { saving.value = false }
}
async function openDetail(row: any) {
  const res: any = await request.get(`/tenant/member-cards/${row.id}`)
  cardDetail.value = res.data
  detailVisible.value = true
}
function printCard(row: any) { download(`/api/tenant/pdf/card/${row.id}`) }

onMounted(async () => { await loadBase(); load() })
</script>
