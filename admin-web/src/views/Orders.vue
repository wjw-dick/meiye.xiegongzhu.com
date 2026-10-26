<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">收银订单</h2>
        <div class="page-subtitle">开单、聚合支付、会员卡/优惠券/积分抵扣、挂账、核销与打印</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Download" @click="exportOrders">导出</el-button>
        <el-button type="primary" :icon="Plus" @click="openCreate">开单收银</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.kw" placeholder="订单号 / 客户姓名" clearable style="width: 200px" @keyup.enter="search" />
      <el-select v-model="query.status" placeholder="订单状态" clearable style="width: 140px" @change="search">
        <el-option v-for="s in statuses" :key="s" :label="statusText(s)" :value="s" />
      </el-select>
      <el-select v-model="query.storeId" placeholder="门店" clearable style="width: 160px" @change="search">
        <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="~"
                      start-placeholder="开始日期" end-placeholder="结束日期" style="width: 250px" @change="search" />
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="customerName" label="客户" width="110" />
      <el-table-column label="品类" width="110">
        <template #default="{ row }">{{ bizTypeText(row.bizType) }}</template>
      </el-table-column>
      <el-table-column prop="storeName" label="门店" width="130" />
      <el-table-column prop="technicianName" label="技师" width="100" />
      <el-table-column label="应收" width="110">
        <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="优惠" width="100">
        <template #default="{ row }">¥{{ money(row.discountAmount) }}</template>
      </el-table-column>
      <el-table-column label="实收" width="110">
        <template #default="{ row }"><b class="money">¥{{ money(row.paidAmount) }}</b></template>
      </el-table-column>
      <el-table-column label="支付方式" width="110">
        <template #default="{ row }">{{ payTypeText(row.payType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="statusTag(row.status)" size="small">{{ statusText(row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="下单时间" width="160">
        <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="success" @click="openPay(row)">收款</el-button>
          <el-button v-if="['PAID', 'VERIFIED'].includes(row.status)" link type="warning" @click="verify(row)">核销</el-button>
          <el-button v-if="row.status === 'VERIFIED'" link type="success" @click="complete(row)">完成</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" @click="cancel(row)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 笔订单</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
                     :page-sizes="[10, 20, 50, 100]" layout="sizes, prev, pager, next"
                     @size-change="load" @current-change="load" />
    </div>

    <!-- 开单 -->
    <el-dialog v-model="createVisible" title="开单收银" width="820px" destroy-on-close>
      <el-form :model="createForm" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户" required>
              <el-select v-model="createForm.customerId" filterable remote :remote-method="searchCustomers"
                         placeholder="输入姓名/手机号搜索" style="width: 100%" @change="onCustomerChange">
                <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${maskPhone(c.phone)} / 积分${c.points || 0}）`" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店">
              <el-select v-model="createForm.storeId" clearable style="width: 100%">
                <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="技师">
              <el-select v-model="createForm.technicianId" clearable filterable style="width: 100%">
                <el-option v-for="t in techs" :key="t.id" :label="`${t.name}（${t.position || ''}）`" :value="t.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="业务品类">
              <el-select v-model="createForm.bizType" clearable style="width: 100%">
                <el-option v-for="b in bizTypes.filter((x: any) => x.enabled)" :key="b.code" :label="b.name" :value="b.code" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="line-title flex-between">
          <span>服务项目</span>
          <el-button size="small" :icon="Plus" @click="addLine">添加项目</el-button>
        </div>
        <el-table :data="createForm.items" size="small" border>
          <el-table-column label="项目 / 套餐" min-width="240">
            <template #default="{ row }">
              <el-select v-model="row.itemId" filterable clearable placeholder="选择服务项目" style="width: 100%" @change="(v: any) => onItemChange(row, v)">
                <el-option v-for="i in items" :key="i.id" :label="`${i.name}（¥${money(i.price)} / ${i.duration || 0}分钟）`" :value="i.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120">
            <template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" size="small" style="width: 100%" /></template>
          </el-table-column>
          <el-table-column label="单价" width="110">
            <template #default="{ row }">¥{{ money(row.price) }}</template>
          </el-table-column>
          <el-table-column label="小计" width="110">
            <template #default="{ row }">¥{{ money(Number(row.price || 0) * Number(row.quantity || 1)) }}</template>
          </el-table-column>
          <el-table-column label="" width="70">
            <template #default="{ $index }"><el-button link type="danger" @click="createForm.items.splice($index, 1)">删除</el-button></template>
          </el-table-column>
        </el-table>
        <div class="total-line">合计：<b>¥{{ money(createTotal) }}</b></div>
        <el-form-item label="备注" class="mt-16">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button :loading="saving" @click="submitCreate(false)">仅开单</el-button>
        <el-button type="primary" :loading="saving" @click="submitCreate(true)">开单并收款</el-button>
      </template>
    </el-dialog>

    <!-- 收款 -->
    <el-dialog v-model="payVisible" title="订单收款" width="720px" destroy-on-close>
      <div v-if="payOrder" class="pay-head">
        <div>
          <div class="pay-no">{{ payOrder.orderNo }}</div>
          <div class="text-muted">{{ payOrder.customerName }} · {{ bizTypeText(payOrder.bizType) }}</div>
        </div>
        <div class="pay-amount">应收 ¥{{ money(payOrder.totalAmount) }}</div>
      </div>
      <el-form label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="优惠券">
              <el-select v-model="payForm.couponId" clearable placeholder="不使用" style="width: 100%">
                <el-option v-for="c in coupons" :key="c.id" :label="`${c.name}（${c.type === 'CASH' ? '减' + money(c.amount) : c.discount + '折'}）`" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="积分抵扣">
              <el-input-number v-model="payForm.pointsUse" :min="0" :max="customerPoints" style="width: 100%" />
              <div class="tip">100 积分抵 1 元，当前可用 {{ customerPoints }}</div>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="line-title flex-between">
          <span>支付明细（可组合支付）</span>
          <el-button size="small" :icon="Plus" @click="addPayment">添加支付方式</el-button>
        </div>
        <el-table :data="payForm.payments" size="small" border>
          <el-table-column label="支付方式" width="180">
            <template #default="{ row }">
              <el-select v-model="row.channel" style="width: 100%" @change="() => onChannelChange(row)">
                <el-option v-for="p in payTypes" :key="p.value" :label="p.label" :value="p.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="会员卡" min-width="180">
            <template #default="{ row }">
              <el-select v-if="['CARD', 'BALANCE'].includes(row.channel)" v-model="row.cardId" clearable placeholder="选择会员卡" style="width: 100%">
                <el-option v-for="c in cards" :key="c.id" :label="`${c.cardNo}（余额¥${money(c.balance)} / ${c.times || 0}次）`" :value="c.id" />
              </el-select>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="140">
            <template #default="{ row }"><el-input-number v-model="row.amount" :min="0" :precision="2" size="small" style="width: 100%" /></template>
          </el-table-column>
          <el-table-column label="次数" width="90">
            <template #default="{ row }">
              <el-input-number v-if="['CARD', 'BALANCE'].includes(row.channel)" v-model="row.times" :min="0" size="small" style="width: 100%" />
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="" width="70">
            <template #default="{ $index }"><el-button link type="danger" @click="payForm.payments.splice($index, 1)">删除</el-button></template>
          </el-table-column>
        </el-table>
        <div class="pay-summary">
          <span>已录入：¥{{ money(payTotal) }}</span>
          <span>预计优惠：¥{{ money(estimatedDiscount) }}</span>
          <el-checkbox v-model="payForm.allowDebt">差额挂账</el-checkbox>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPay">确认收款</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-drawer v-model="detailVisible" title="订单详情" size="720px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="订单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusTag(detail.order.status)">{{ statusText(detail.order.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="客户">{{ detail.customer?.name }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ maskPhone(detail.customer?.phone) }}</el-descriptions-item>
          <el-descriptions-item label="业务品类">{{ bizTypeText(detail.order.bizType) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ payTypeText(detail.order.payType) }}</el-descriptions-item>
          <el-descriptions-item label="应收">¥{{ money(detail.order.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="优惠">¥{{ money(detail.order.discountAmount) }}</el-descriptions-item>
          <el-descriptions-item label="实收">¥{{ money(detail.order.paidAmount) }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ fmt(detail.order.paidAt) }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="mt-16">服务项目</h4>
        <el-table :data="detail.items" size="small" border>
          <el-table-column prop="name" label="项目" min-width="180" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="单价" width="100"><template #default="{ row }">¥{{ money(row.price) }}</template></el-table-column>
          <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
          <el-table-column label="提成" width="100"><template #default="{ row }">¥{{ money(row.commissionAmount) }}</template></el-table-column>
        </el-table>
        <h4 class="mt-16">支付记录</h4>
        <el-table :data="detail.payments" size="small" border>
          <el-table-column prop="payNo" label="支付单号" min-width="170" />
          <el-table-column label="渠道" width="100"><template #default="{ row }">{{ payTypeText(row.channel) }}</template></el-table-column>
          <el-table-column label="金额" width="100"><template #default="{ row }">¥{{ money(row.amount) }}</template></el-table-column>
          <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmt(row.paidAt) }}</template></el-table-column>
        </el-table>
        <div class="flex gap-8 mt-16">
          <el-button :icon="Printer" @click="printReport(detail.order.id)">打印服务报告</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Download, Printer } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt, maskPhone, statusText, statusTag, bizTypeText, download } from '@/utils/format'

const statuses = ['PENDING', 'PAID', 'VERIFIED', 'DONE', 'CANCELLED']
const payTypes = [
  { label: '现金', value: 'CASH' }, { label: '微信', value: 'WECHAT' }, { label: '支付宝', value: 'ALIPAY' },
  { label: '银行卡', value: 'BANK' }, { label: '会员卡余额', value: 'BALANCE' }, { label: '次卡核销', value: 'CARD' }
]

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ kw: '', status: '', storeId: '' })
const dateRange = ref<[string, string] | null>(null)

const stores = ref<any[]>([])
const techs = ref<any[]>([])
const items = ref<any[]>([])
const bizTypes = ref<any[]>([])
const customers = ref<any[]>([])
const coupons = ref<any[]>([])
const cards = ref<any[]>([])

const createVisible = ref(false)
const createForm = reactive<any>({ customerId: null, storeId: null, technicianId: null, bizType: '', remark: '', items: [] })
const createTotal = computed(() => createForm.items.reduce((s: number, x: any) => s + Number(x.price || 0) * Number(x.quantity || 1), 0))

const payVisible = ref(false)
const payOrder = ref<any>(null)
const payForm = reactive<any>({ couponId: null, pointsUse: 0, allowDebt: false, payments: [] })
const customerPoints = ref(0)
const payTotal = computed(() => payForm.payments.reduce((s: number, x: any) => s + Number(x.amount || 0), 0))
const estimatedDiscount = computed(() => {
  if (!payOrder.value) return 0
  const coupon = coupons.value.find((c) => c.id === payForm.couponId)
  let d = 0
  if (coupon) {
    d = coupon.type === 'CASH' ? Number(coupon.amount || 0) : Number(payOrder.value.totalAmount) * (100 - Number(coupon.discount || 100)) / 100
  }
  d += Number(payForm.pointsUse || 0) / 100
  return Math.min(d, Number(payOrder.value.totalAmount || 0))
})

const detailVisible = ref(false)
const detail = ref<any>(null)

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value, ...query }
    if (dateRange.value) { params.startDate = dateRange.value[0]; params.endDate = dateRange.value[1] }
    const res: any = await request.get('/tenant/orders/page', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() {
  Object.assign(query, { kw: '', status: '', storeId: '' })
  dateRange.value = null
  search()
}
function payTypeText(v: any) {
  return payTypes.find((p) => p.value === v)?.label || v || '-'
}
function exportOrders() {
  download('/api/tenant/orders/export', { status: query.status, startDate: dateRange.value?.[0], endDate: dateRange.value?.[1] })
}

async function loadBase() {
  const [s, t, i, b] = await Promise.all([
    request.get('/tenant/stores/list'), request.get('/tenant/technicians/list'),
    request.get('/tenant/service-items/list'), request.get('/tenant/biz-types')
  ]) as any[]
  stores.value = s.data || []
  techs.value = t.data || []
  items.value = i.data || []
  bizTypes.value = b.data || []
}
async function searchCustomers(kw: string) {
  const res: any = await request.get('/tenant/customers/page', { params: { page: 1, size: 20, kw } })
  customers.value = res.data.records || []
}
function openCreate() {
  Object.assign(createForm, { customerId: null, storeId: null, technicianId: null, bizType: '', remark: '', items: [] })
  addLine()
  createVisible.value = true
  searchCustomers('')
}
function addLine() { createForm.items.push({ itemId: null, quantity: 1, price: 0 }) }
function onItemChange(row: any, id: any) {
  const it = items.value.find((x) => x.id === id)
  row.price = it?.price || 0
  if (it && !createForm.bizType) createForm.bizType = it.bizType
}
async function onCustomerChange(id: any) {
  const c = customers.value.find((x) => x.id === id)
  customerPoints.value = c?.points || 0
  if (id) {
    const res: any = await request.get('/tenant/coupons/page', { params: { page: 1, size: 50, customerId: id, status: 'UNUSED' } })
    coupons.value = res.data.records || []
    const cr: any = await request.get('/tenant/member-cards/page', { params: { page: 1, size: 50, customerId: id, status: 'ACTIVE' } })
    cards.value = cr.data.records || []
  }
}
async function submitCreate(goPay: boolean) {
  if (!createForm.customerId) { ElMessage.warning('请选择客户'); return }
  if (!createForm.items.length || createForm.items.some((x: any) => !x.itemId)) { ElMessage.warning('请选择服务项目'); return }
  saving.value = true
  try {
    const payload = {
      customerId: createForm.customerId, storeId: createForm.storeId, technicianId: createForm.technicianId,
      bizType: createForm.bizType || undefined, remark: createForm.remark,
      items: createForm.items.map((x: any) => ({ itemId: x.itemId, quantity: x.quantity, technicianId: createForm.technicianId }))
    }
    const res: any = await request.post('/tenant/orders/create', payload)
    ElMessage.success('开单成功')
    createVisible.value = false
    load()
    if (goPay) {
      await onCustomerChange(createForm.customerId)
      openPay(res.data)
    }
  } finally { saving.value = false }
}

function openPay(row: any) {
  payOrder.value = row
  customerPoints.value = 0
  request.get('/tenant/customers/page', { params: { page: 1, size: 1, kw: row.customerName } }).then((r: any) => {
    const c = r.data.records?.[0]
    if (c) {
      customerPoints.value = c.points || 0
      onCustomerChange(c.id)
    }
  })
  Object.assign(payForm, { couponId: null, pointsUse: 0, allowDebt: false, payments: [{ channel: 'WECHAT', amount: Number(row.totalAmount), cardId: null, times: 0 }] })
  payVisible.value = true
}
function addPayment() { payForm.payments.push({ channel: 'CASH', amount: 0, cardId: null, times: 0 }) }
function onChannelChange(row: any) {
  if (!['CARD', 'BALANCE'].includes(row.channel)) { row.cardId = null; row.times = 0 }
}
async function submitPay() {
  const lines = payForm.payments.filter((x: any) => Number(x.amount) > 0)
  if (!lines.length) { ElMessage.warning('请填写支付金额'); return }
  saving.value = true
  try {
    await request.post(`/tenant/orders/${payOrder.value.id}/pay`, {
      payType: lines[0].channel, couponId: payForm.couponId || undefined,
      pointsUse: payForm.pointsUse || undefined, allowDebt: payForm.allowDebt,
      payments: lines.map((x: any) => ({ channel: x.channel, amount: x.amount, cardId: x.cardId, times: x.times }))
    })
    ElMessage.success('收款成功')
    payVisible.value = false
    load()
  } finally { saving.value = false }
}

async function openDetail(row: any) {
  const res: any = await request.get(`/tenant/orders/${row.id}`)
  detail.value = res.data
  detailVisible.value = true
}
async function verify(row: any) {
  await ElMessageBox.confirm(`确认核销订单 ${row.orderNo}？`, '核销确认', { type: 'warning' })
  await request.post(`/tenant/orders/${row.id}/verify`)
  ElMessage.success('核销成功')
  load()
}
async function complete(row: any) {
  await request.post(`/tenant/orders/${row.id}/complete`)
  ElMessage.success('订单已完成')
  load()
}
async function cancel(row: any) {
  const { value } = await ElMessageBox.prompt('请输入取消原因', '取消订单', { inputPlaceholder: '客户改期 / 重复下单' })
  await request.post(`/tenant/orders/${row.id}/cancel`, null, { params: { reason: value } })
  ElMessage.success('订单已取消')
  load()
}
function printReport(orderId: number) { download(`/api/tenant/pdf/service-report/${orderId}`) }

onMounted(async () => { await loadBase(); load() })
</script>

<style scoped>
.money { color: #e6542c; }
.line-title { font-weight: 600; margin: 8px 0 10px; }
.total-line { text-align: right; margin-top: 12px; font-size: 16px; }
.total-line b { color: #e6542c; font-size: 20px; }
.pay-head { display: flex; justify-content: space-between; align-items: center; background: #f7f8fc; border-radius: 10px; padding: 14px 18px; margin-bottom: 16px; }
.pay-no { font-size: 16px; font-weight: 600; }
.pay-amount { font-size: 22px; font-weight: 700; color: #e6542c; }
.pay-summary { display: flex; gap: 20px; align-items: center; justify-content: flex-end; margin-top: 12px; }
.tip { font-size: 11px; color: #9aa0b2; }
</style>
