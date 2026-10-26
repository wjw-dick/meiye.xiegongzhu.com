<template>
  <div>
    <div class="stat-grid mb-16">
      <div class="stat-card">
        <div class="label">库存品项</div>
        <div class="value">{{ summary.items || 0 }}</div>
        <div class="hint">耗材 / 产品 SKU</div>
      </div>
      <div class="stat-card">
        <div class="label">库存成本</div>
        <div class="value">¥{{ money(summary.stockValue) }}</div>
        <div class="hint">按成本价汇总</div>
      </div>
      <div class="stat-card">
        <div class="label">低库存预警</div>
        <div class="value" :class="{ danger: Number(summary.lowStock) > 0 }">{{ summary.lowStock || 0 }}</div>
        <div class="hint">库存低于预警线</div>
      </div>
      <div class="stat-card">
        <div class="label">操作</div>
        <div class="value" style="font-size: 16px; margin-top: 10px">
          <el-button size="small" @click="openFlows(null)">查看全部流水</el-button>
        </div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">耗材库存</h2>
          <div class="page-subtitle">入库、出库、项目领用、库存流水与低库存预警</div>
        </div>
        <div class="flex gap-8">
          <el-button :icon="Refresh" @click="load">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openEdit(null)">新增库存品</el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-input v-model="query.kw" placeholder="名称 / SKU" clearable style="width: 200px" @keyup.enter="search" />
        <el-select v-model="query.bizType" placeholder="业务品类" clearable style="width: 160px" @change="search">
          <el-option v-for="b in bizTypes" :key="b.code" :label="b.name" :value="b.code" />
        </el-select>
        <el-select v-model="query.storeId" placeholder="门店" clearable style="width: 160px" @change="search">
          <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="sku" label="SKU" width="130" />
        <el-table-column label="品类" width="110"><template #default="{ row }">{{ bizTypeText(row.bizType) }}</template></el-table-column>
        <el-table-column label="库存" width="110">
          <template #default="{ row }">
            <span :class="{ danger: Number(row.stock) <= Number(row.warnStock) }">{{ row.stock }} {{ row.unit }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预警线" width="100"><template #default="{ row }">{{ row.warnStock }} {{ row.unit }}</template></el-table-column>
        <el-table-column label="成本价" width="110"><template #default="{ row }">¥{{ money(row.costPrice) }}</template></el-table-column>
        <el-table-column label="库存成本" width="120"><template #default="{ row }">¥{{ money(Number(row.stock || 0) * Number(row.costPrice || 0)) }}</template></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right">
          <template #default="{ row }">
            <el-button link type="success" @click="openStock(row, 'IN')">入库</el-button>
            <el-button link type="warning" @click="openStock(row, 'OUT')">出库</el-button>
            <el-button link type="primary" @click="openStock(row, 'USE')">领用</el-button>
            <el-button link @click="openFlows(row)">流水</el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex-between mt-16">
        <span class="text-muted">共 {{ total }} 个库存品</span>
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                       layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑库存品' : '新增库存品'" width="640px">
      <el-form label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="SKU"><el-input v-model="form.sku" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="业务品类">
              <el-select v-model="form.bizType" clearable style="width: 100%">
                <el-option v-for="b in bizTypes.filter((x: any) => x.enabled)" :key="b.code" :label="b.name" :value="b.code" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门店">
              <el-select v-model="form.storeId" clearable style="width: 100%">
                <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8"><el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="成本价"><el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="预警线"><el-input-number v-model="form.warnStock" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="stockVisible" :title="stockTitle" width="500px">
      <el-form label-width="100px">
        <el-form-item label="库存品">{{ current?.name }}（当前 {{ current?.stock }} {{ current?.unit }}）</el-form-item>
        <el-form-item label="数量" required><el-input-number v-model="stockForm.quantity" :min="0.01" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item v-if="stockForm.type === 'IN'" label="成本价"><el-input-number v-model="stockForm.cost" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item v-if="stockForm.type === 'USE'" label="关联订单"><el-input-number v-model="stockForm.orderId" :min="1" style="width: 100%" placeholder="可选" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="stockForm.remark" placeholder="采购入库 / 项目领用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doStock">确认</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="flowsVisible" :title="flowTitle" size="700px">
      <el-table :data="flows" size="small" border>
        <el-table-column label="类型" width="90"><template #default="{ row }">{{ flowType(row.type) }}</template></el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" />
        <el-table-column label="成本" width="100"><template #default="{ row }">¥{{ money(row.cost) }}</template></el-table-column>
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt, bizTypeText } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ kw: '', bizType: '', storeId: null })
const stores = ref<any[]>([])
const bizTypes = ref<any[]>([])
const summary = ref<any>({})
const editVisible = ref(false)
const form = reactive<any>({})
const stockVisible = ref(false)
const stockForm = reactive<any>({ type: 'IN', quantity: 1, cost: 0, orderId: null, remark: '' })
const current = ref<any>(null)
const flowsVisible = ref(false)
const flows = ref<any[]>([])
const flowItem = ref<any>(null)
const stockTitle = computed(() => stockForm.type === 'IN' ? '库存入库' : stockForm.type === 'OUT' ? '库存出库' : '项目领用')
const flowTitle = computed(() => flowItem.value ? `${flowItem.value.name} · 库存流水` : '全部库存流水')

function flowType(v: any) { return ({ IN: '入库', OUT: '出库', USE: '领用', ADJUST: '调整' } as any)[v] || v }
async function load() {
  loading.value = true
  try {
    const [list, sum] = await Promise.all([
      request.get('/tenant/inventory/page', { params: { page: page.value, size: size.value, ...query } }),
      request.get('/tenant/inventory/summary')
    ]) as any[]
    rows.value = list.data.records || []
    total.value = list.data.total || 0
    summary.value = sum.data || {}
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { kw: '', bizType: '', storeId: null }); search() }
async function loadBase() {
  const [st, bt] = await Promise.all([request.get('/tenant/stores/list'), request.get('/tenant/biz-types')]) as any[]
  stores.value = st.data || []
  bizTypes.value = bt.data || []
}
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? JSON.parse(JSON.stringify(row)) : { unit: '件', stock: 0, costPrice: 0, warnStock: 0, status: 1 })
  editVisible.value = true
}
async function save() {
  if (!form.name) { ElMessage.warning('请填写名称'); return }
  saving.value = true
  try {
    if (form.id) await request.put('/tenant/inventory', form)
    else await request.post('/tenant/inventory', form)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally { saving.value = false }
}
function openStock(row: any, type: string) {
  current.value = row
  Object.assign(stockForm, { type, quantity: 1, cost: row.costPrice || 0, orderId: null, remark: '' })
  stockVisible.value = true
}
async function doStock() {
  saving.value = true
  try {
    const url = `/tenant/inventory/${current.value.id}/${stockForm.type.toLowerCase()}`
    await request.post(url, null, { params: { quantity: stockForm.quantity, cost: stockForm.type === 'IN' ? stockForm.cost : undefined, orderId: stockForm.type === 'USE' ? stockForm.orderId : undefined, remark: stockForm.remark } })
    ElMessage.success('操作成功')
    stockVisible.value = false
    load()
  } finally { saving.value = false }
}
async function openFlows(row: any) {
  flowItem.value = row
  const res: any = await request.get('/tenant/inventory/flows', { params: { itemId: row?.id } })
  flows.value = res.data || []
  flowsVisible.value = true
}
onMounted(async () => { await loadBase(); load() })
</script>

<style scoped>
.danger { color: #e6542c; font-weight: 600; }
</style>
