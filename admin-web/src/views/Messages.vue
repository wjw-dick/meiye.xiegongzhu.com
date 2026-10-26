<template>
  <div>
    <div class="page-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">消息模板</h2>
          <div class="page-subtitle">预约、收银、卡项、回访等业务节点消息模板与发送通道</div>
        </div>
        <div class="flex gap-8">
          <el-button :icon="Refresh" @click="loadTemplates">刷新</el-button>
          <el-button type="primary" :icon="Plus" @click="openTemplate(null)">新增模板</el-button>
        </div>
      </div>

      <el-table :data="templates" v-loading="loading" stripe border>
        <el-table-column prop="code" label="模板编码" width="190" />
        <el-table-column prop="name" label="模板名称" min-width="170" />
        <el-table-column prop="templateId" label="第三方模板ID" min-width="180" show-overflow-tooltip />
        <el-table-column label="通道" width="140">
          <template #default="{ row }">{{ channelText(row.channel) }}</template>
        </el-table-column>
        <el-table-column label="启用" width="90">
          <template #default="{ row }"><el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">{{ row.enabled === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTemplate(row)">编辑</el-button>
            <el-button link type="success" @click="openTest(row)">测试发送</el-button>
            <el-button link type="danger" @click="removeTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card mt-16">
      <div class="page-header">
        <div>
          <h2 class="page-title">发送日志</h2>
          <div class="page-subtitle">消息投递记录、失败原因与重试</div>
        </div>
        <div class="filter-bar" style="margin: 0">
          <el-select v-model="logQuery.status" placeholder="状态" clearable style="width: 130px" @change="searchLogs">
            <el-option label="成功" value="SENT" /><el-option label="失败" value="FAILED" />
          </el-select>
          <el-select v-model="logQuery.code" placeholder="业务节点" clearable style="width: 170px" @change="searchLogs">
            <el-option v-for="(name, code) in codes" :key="code" :label="name" :value="code" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="searchLogs">查询</el-button>
        </div>
      </div>
      <el-table :data="logs" v-loading="logLoading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="customerId" label="会员ID" width="100" />
        <el-table-column prop="code" label="节点" width="160" />
        <el-table-column prop="title" label="标题" width="160" />
        <el-table-column prop="content" label="内容" min-width="220" show-overflow-tooltip />
        <el-table-column label="通道" width="120"><template #default="{ row }">{{ channelText(row.channel) }}</template></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 'SENT' ? 'success' : 'danger'" size="small">{{ row.status === 'SENT' ? '成功' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="error" label="错误信息" min-width="160" show-overflow-tooltip />
        <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }"><el-button v-if="row.status !== 'SENT'" link type="primary" @click="retry(row)">重试</el-button></template>
        </el-table-column>
      </el-table>
      <div class="flex-between mt-16">
        <span class="text-muted">共 {{ logTotal }} 条日志</span>
        <el-pagination v-model:current-page="logPage" v-model:page-size="logSize" :total="logTotal" :page-sizes="[10, 20, 50]"
                       layout="sizes, prev, pager, next" @size-change="loadLogs" @current-change="loadLogs" />
      </div>
    </div>

    <el-dialog v-model="templateVisible" :title="form.id ? '编辑消息模板' : '新增消息模板'" width="640px">
      <el-form label-width="120px">
        <el-form-item label="业务节点" required>
          <el-select v-model="form.code" filterable allow-create style="width: 100%">
            <el-option v-for="(name, code) in codes" :key="code" :label="`${name}（${code}）`" :value="code" />
          </el-select>
        </el-form-item>
        <el-form-item label="模板名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="第三方模板ID"><el-input v-model="form.templateId" placeholder="微信订阅消息/模板消息 ID" /></el-form-item>
        <el-form-item label="发送通道">
          <el-select v-model="form.channel" style="width: 100%">
            <el-option label="公众号模板消息" value="WECHAT_MP" /><el-option label="小程序订阅消息" value="WECHAT_MINI" />
            <el-option label="短信" value="SMS" /><el-option label="站内消息" value="INNER" />
          </el-select>
        </el-form-item>
        <el-form-item label="字段映射 JSON"><el-input v-model="form.fieldsJson" type="textarea" :rows="3" placeholder='{"first":"标题","keyword1":"内容"}' /></el-form-item>
        <el-form-item label="启用"><el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="templateVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveTemplate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="testVisible" title="测试发送" width="520px">
      <el-form label-width="100px">
        <el-form-item label="模板">{{ testForm.code }} · {{ testForm.name }}</el-form-item>
        <el-form-item label="接收会员" required>
          <el-select v-model="testForm.customerId" filterable remote :remote-method="searchCustomers" placeholder="搜索会员" style="width: 100%">
            <el-option v-for="c in customers" :key="c.id" :label="`${c.name}（${maskPhone(c.phone)}）`" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题"><el-input v-model="testForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="testForm.content" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="testVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doTest">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt, maskPhone } from '@/utils/format'

const templates = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const codes = ref<Record<string, string>>({})
const templateVisible = ref(false)
const form = reactive<any>({})
const testVisible = ref(false)
const testForm = reactive<any>({ customerId: null, code: '', name: '', title: '', content: '' })
const customers = ref<any[]>([])
const logs = ref<any[]>([])
const logTotal = ref(0)
const logPage = ref(1)
const logSize = ref(10)
const logLoading = ref(false)
const logQuery = reactive<any>({ status: '', code: '' })

function channelText(v: any) {
  return ({ WECHAT_MP: '公众号', WECHAT_MINI: '小程序', SMS: '短信', INNER: '站内信' } as any)[v] || v || '-'
}
async function loadTemplates() {
  loading.value = true
  try {
    const [tpl, code] = await Promise.all([request.get('/tenant/message/templates'), request.get('/tenant/message/codes')]) as any[]
    templates.value = tpl.data || []
    codes.value = code.data || {}
  } finally { loading.value = false }
}
function openTemplate(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { channel: 'WECHAT_MP', enabled: 1 })
  templateVisible.value = true
}
async function saveTemplate() {
  if (!form.code || !form.name) { ElMessage.warning('请填写节点和名称'); return }
  saving.value = true
  try {
    if (form.id) await request.put('/tenant/message/templates', form)
    else await request.post('/tenant/message/templates', form)
    ElMessage.success('保存成功')
    templateVisible.value = false
    loadTemplates()
  } finally { saving.value = false }
}
async function removeTemplate(row: any) {
  await ElMessageBox.confirm('确认删除该模板？', '删除模板', { type: 'warning' })
  await request.delete(`/tenant/message/templates/${row.id}`)
  ElMessage.success('已删除')
  loadTemplates()
}
async function searchCustomers(kw: string) {
  const res: any = await request.get('/tenant/customers/page', { params: { page: 1, size: 20, kw } })
  customers.value = res.data.records || []
}
function openTest(row: any) {
  Object.assign(testForm, { customerId: null, code: row.code, name: row.name, title: row.name, content: '这是一条测试消息' })
  searchCustomers('')
  testVisible.value = true
}
async function doTest() {
  if (!testForm.customerId) { ElMessage.warning('请选择接收会员'); return }
  saving.value = true
  try {
    await request.post('/tenant/message/test', null, { params: { customerId: testForm.customerId, code: testForm.code, title: testForm.title, content: testForm.content } })
    ElMessage.success('测试消息已投递')
    testVisible.value = false
    loadLogs()
  } finally { saving.value = false }
}
async function loadLogs() {
  logLoading.value = true
  try {
    const res: any = await request.get('/tenant/message/logs/page', { params: { page: logPage.value, size: logSize.value, ...logQuery } })
    logs.value = res.data.records || []
    logTotal.value = res.data.total || 0
  } finally { logLoading.value = false }
}
function searchLogs() { logPage.value = 1; loadLogs() }
async function retry(row: any) {
  await request.post(`/tenant/message/logs/${row.id}/retry`)
  ElMessage.success('已重试')
  loadLogs()
}
onMounted(() => { loadTemplates(); loadLogs() })
</script>
