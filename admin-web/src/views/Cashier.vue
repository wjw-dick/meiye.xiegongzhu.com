<template>
  <div>
    <div class="page-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">交接班</h2>
          <div class="page-subtitle">开班、现金清点、系统金额对账与交接班历史</div>
        </div>
        <div class="flex gap-8">
          <el-select v-model="storeId" placeholder="选择门店" style="width: 200px" @change="loadAll">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-button :icon="Refresh" @click="loadAll">刷新</el-button>
        </div>
      </div>

      <div v-if="current" class="shift-card">
        <div>
          <div class="tag-line"><el-tag type="success" effect="dark">进行中</el-tag><span class="text-muted">开班时间 {{ fmt(current.startAt) }}</span></div>
          <div class="shift-name">{{ current.cashierName || '当前收银员' }} · 门店 #{{ current.storeId }}</div>
          <div class="text-muted">备用金 ¥{{ money(current.openingCash) }} ｜ 系统收款 ¥{{ money(current.systemAmount) }}</div>
        </div>
        <el-button type="danger" plain @click="openClose">结束交接班</el-button>
      </div>
      <div v-else class="shift-card empty">
        <div>
          <div class="shift-name">当前门店暂无进行中的交接班</div>
          <div class="text-muted">开班前请清点备用金，收银过程中系统自动归集收款金额</div>
        </div>
        <el-button type="primary" :disabled="!storeId" @click="openOpen">开始交接班</el-button>
      </div>
    </div>

    <div class="page-card mt-16">
      <div class="card-title">交接班历史</div>
      <el-table :data="shifts" v-loading="loading" stripe border>
        <el-table-column prop="id" label="班次" width="80" />
        <el-table-column prop="cashierName" label="收银员" width="120" />
        <el-table-column label="开班时间" width="170"><template #default="{ row }">{{ fmt(row.startAt) }}</template></el-table-column>
        <el-table-column label="结班时间" width="170"><template #default="{ row }">{{ fmt(row.endAt) }}</template></el-table-column>
        <el-table-column label="备用金" width="110"><template #default="{ row }">¥{{ money(row.openingCash) }}</template></el-table-column>
        <el-table-column label="系统金额" width="110"><template #default="{ row }">¥{{ money(row.systemAmount) }}</template></el-table-column>
        <el-table-column label="实收现金" width="110"><template #default="{ row }">¥{{ money(row.closingCash) }}</template></el-table-column>
        <el-table-column label="差异" width="110">
          <template #default="{ row }">
            <span :class="{ danger: Number(row.diffAmount) < 0, success: Number(row.diffAmount) > 0 }">
              {{ row.diffAmount === null || row.diffAmount === undefined ? '-' : '¥' + money(row.diffAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">{{ row.status === 'OPEN' ? '进行中' : '已结班' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="160" />
      </el-table>
    </div>

    <el-dialog v-model="openVisible" title="开始交接班" width="440px">
      <el-form label-width="90px">
        <el-form-item label="门店">
          <el-select v-model="form.storeId" style="width: 100%">
            <el-option v-for="s in stores" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备用金"><el-input-number v-model="form.openingCash" :min="0" :precision="2" style="width: 100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="openVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doOpen">确认开班</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="closeVisible" title="结束交接班" width="480px">
      <el-alert type="info" :closable="false" show-icon class="mb-16"
                :title="`系统收款 ¥${money(current?.systemAmount)}，请填写实际现金并按需备注差异原因`" />
      <el-form label-width="100px">
        <el-form-item label="实收现金"><el-input-number v-model="closeForm.closingCash" :min="0" :precision="2" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="closeForm.remark" type="textarea" :rows="2" placeholder="差异说明 / 交接事项" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doClose">确认结班</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/api/request'
import { money, fmt } from '@/utils/format'

const stores = ref<any[]>([])
const storeId = ref<number | null>(null)
const current = ref<any>(null)
const shifts = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const openVisible = ref(false)
const closeVisible = ref(false)
const form = reactive<any>({ storeId: null, openingCash: 0 })
const closeForm = reactive<any>({ closingCash: 0, remark: '' })

async function loadStores() {
  const res: any = await request.get('/tenant/stores/list')
  stores.value = res.data || []
  if (!storeId.value && stores.value.length) storeId.value = stores.value[0].id
}
async function loadAll() {
  if (!storeId.value) return
  loading.value = true
  try {
    const [cur, list] = await Promise.all([
      request.get('/tenant/orders/shifts/current', { params: { storeId: storeId.value } }),
      request.get('/tenant/orders/shifts/page', { params: { storeId: storeId.value } })
    ]) as any[]
    current.value = cur.data || null
    shifts.value = list.data || []
  } catch (e) {
    current.value = null
  } finally { loading.value = false }
}
function openOpen() {
  form.storeId = storeId.value
  form.openingCash = 0
  openVisible.value = true
}
async function doOpen() {
  saving.value = true
  try {
    await request.post('/tenant/orders/shifts/open', form)
    ElMessage.success('已开班')
    openVisible.value = false
    loadAll()
  } finally { saving.value = false }
}
function openClose() {
  closeForm.closingCash = Number(current.value?.systemAmount || 0)
  closeForm.remark = ''
  closeVisible.value = true
}
async function doClose() {
  saving.value = true
  try {
    await request.post(`/tenant/orders/shifts/${current.value.id}/close`, closeForm)
    ElMessage.success('已结班')
    closeVisible.value = false
    loadAll()
  } finally { saving.value = false }
}

onMounted(async () => { await loadStores(); loadAll() })
</script>

<style scoped>
.shift-card { display: flex; justify-content: space-between; align-items: center; gap: 16px; background: linear-gradient(120deg, #f3f0ff, #f8f9ff); border: 1px solid #e7e3ff; border-radius: 12px; padding: 20px; }
.shift-card.empty { background: #fafbfe; border-style: dashed; }
.shift-name { font-size: 17px; font-weight: 600; margin: 6px 0; }
.tag-line { display: flex; align-items: center; gap: 10px; }
.card-title { font-weight: 600; margin-bottom: 14px; }
.danger { color: #e6542c; }
.success { color: #22a06b; }
</style>
