<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">微信配置与运营</h2>
        <div class="page-subtitle">公众号 / 小程序 / 开放平台 / App UnionID 底座，密钥 AES 加密、页面掩码展示</div>
      </div>
      <div class="flex gap-8">
        <el-tag :type="mock ? 'info' : 'success'" effect="plain">{{ mock ? '模拟模式' : '正式模式' }}</el-tag>
        <el-button :icon="Refresh" @click="loadConfig">刷新</el-button>
      </div>
    </div>

    <el-tabs v-model="tab">
      <el-tab-pane label="连接配置" name="config">
        <el-alert type="warning" :closable="false" show-icon class="mb-16"
                  title="密钥保存后仅显示掩码；如需更换请直接覆盖输入。正式环境需在服务端关闭 meiye.wechat.mock 并配置公网回调地址。" />
        <el-form :model="form" label-width="150px" style="max-width: 860px">
          <el-divider content-position="left">微信小程序</el-divider>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="小程序 AppID"><el-input v-model="form.miniAppId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="小程序 Secret"><el-input v-model="form.miniSecret" show-password placeholder="已加密保存，留空不修改" /></el-form-item></el-col>
          </el-row>
          <el-divider content-position="left">微信公众号</el-divider>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="公众号 AppID"><el-input v-model="form.mpAppId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="公众号 Secret"><el-input v-model="form.mpSecret" show-password placeholder="已加密保存，留空不修改" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="Token"><el-input v-model="form.mpToken" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="EncodingAESKey"><el-input v-model="form.mpAesKey" show-password /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="回调地址"><el-input v-model="form.mpUrl" placeholder="https://your-domain.com/api/public/wechat/{tenantCode}/callback" /></el-form-item></el-col>
          </el-row>
          <el-divider content-position="left">开放平台 / App</el-divider>
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="开放平台 UnionID AppID"><el-input v-model="form.unionAppId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="App AppID"><el-input v-model="form.appAppId" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="App Secret"><el-input v-model="form.appSecret" show-password placeholder="已加密保存，留空不修改" /></el-form-item></el-col>
            <el-col :span="12">
              <el-form-item label="推送渠道">
                <el-select v-model="form.pushChannel" style="width: 100%">
                  <el-option label="公众号模板消息" value="WECHAT_MP" /><el-option label="小程序订阅消息" value="WECHAT_MINI" />
                  <el-option label="短信" value="SMS" /><el-option label="站内消息" value="INNER" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24"><el-form-item label="订阅消息模板ID"><el-input v-model="form.subscribeIds" placeholder="多个用逗号分隔" /></el-form-item></el-col>
          </el-row>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
            <el-button :loading="testing" @click="test">测试连接</el-button>
            <el-button @click="openAuth">生成网页授权 URL</el-button>
            <el-button @click="syncMenu">同步公众号菜单</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="粉丝管理" name="fans">
        <div class="filter-bar">
          <el-select v-model="fanQuery.subscribed" placeholder="关注状态" clearable style="width: 140px" @change="loadFans">
            <el-option label="已关注" :value="1" /><el-option label="已取关" :value="0" />
          </el-select>
          <el-button type="primary" :icon="Search" @click="loadFans">查询</el-button>
          <el-button type="primary" plain @click="openMass">定向群发</el-button>
        </div>
        <el-table :data="fans" v-loading="fanLoading" stripe border>
          <el-table-column prop="openid" label="OpenID" min-width="220" show-overflow-tooltip />
          <el-table-column prop="unionId" label="UnionID" min-width="220" show-overflow-tooltip />
          <el-table-column prop="nickname" label="昵称" width="130" />
          <el-table-column prop="tags" label="标签" width="160" />
          <el-table-column label="关注" width="90">
            <template #default="{ row }"><el-tag :type="row.subscribed === 1 ? 'success' : 'info'" size="small">{{ row.subscribed === 1 ? '已关注' : '已取关' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="关注时间" width="160"><template #default="{ row }">{{ fmt(row.subscribeAt) }}</template></el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{ row }"><el-button link type="primary" @click="openTag(row)">打标签</el-button></template>
          </el-table-column>
        </el-table>
        <div class="flex-between mt-16">
          <span class="text-muted">共 {{ fanTotal }} 位粉丝</span>
          <el-pagination v-model:current-page="fanPage" v-model:page-size="fanSize" :total="fanTotal" :page-sizes="[10, 20, 50]"
                         layout="sizes, prev, pager, next" @size-change="loadFans" @current-change="loadFans" />
        </div>
      </el-tab-pane>

      <el-tab-pane label="图文素材" name="materials">
        <div class="filter-bar">
          <el-select v-model="materialType" placeholder="素材类型" clearable style="width: 140px" @change="loadMaterials">
            <el-option label="图文" value="NEWS" /><el-option label="图片" value="IMAGE" /><el-option label="视频" value="VIDEO" />
          </el-select>
          <el-button type="primary" :icon="Plus" @click="openMaterial(null)">新建素材</el-button>
        </div>
        <el-table :data="materials" stripe border>
          <el-table-column prop="title" label="标题" min-width="220" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="mediaId" label="MediaID" min-width="180" show-overflow-tooltip />
          <el-table-column prop="url" label="链接" min-width="220" show-overflow-tooltip />
          <el-table-column label="时间" width="160"><template #default="{ row }">{{ fmt(row.createdAt) }}</template></el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMaterial(row)">编辑</el-button>
              <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="标签管理" name="tags">
        <div class="filter-bar">
          <el-input v-model="newTag" placeholder="新标签名称" style="width: 220px" />
          <el-button type="primary" @click="addTag">添加标签</el-button>
        </div>
        <el-table :data="tagList" stripe border>
          <el-table-column prop="name" label="标签" min-width="180" />
          <el-table-column prop="fanCount" label="粉丝数" width="120" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }"><el-button link type="danger" @click="removeTag(row)">删除</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="authVisible" title="网页授权 URL" width="640px">
      <el-form label-width="100px">
        <el-form-item label="回调页面"><el-input v-model="redirectUrl" placeholder="https://your-domain.com/#/login" /></el-form-item>
        <el-form-item label="授权链接">
          <el-input :model-value="authUrl" type="textarea" :rows="3" readonly />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="authVisible = false">关闭</el-button>
        <el-button type="primary" @click="genAuth">生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="tagVisible" title="粉丝打标签" width="440px">
      <el-form label-width="90px">
        <el-form-item label="粉丝">{{ fanCurrent?.nickname || fanCurrent?.openid }}</el-form-item>
        <el-form-item label="标签"><el-input v-model="tagText" placeholder="多个标签用逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tagVisible = false">取消</el-button>
        <el-button type="primary" @click="saveTag">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="massVisible" title="公众号定向群发" width="560px">
      <el-form label-width="100px">
        <el-form-item label="标题"><el-input v-model="massForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="massForm.content" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="目标标签"><el-input v-model="massForm.tag" placeholder="留空为全部已关注粉丝" /></el-form-item>
        <el-alert type="info" :closable="false" show-icon title="单次最多发送 500 人，超出将自动截断并提示。" />
      </el-form>
      <template #footer>
        <el-button @click="massVisible = false">取消</el-button>
        <el-button type="primary" :loading="sending" @click="doMass">确认群发</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="materialVisible" :title="materialForm.id ? '编辑素材' : '新建素材'" width="620px">
      <el-form label-width="100px">
        <el-form-item label="标题" required><el-input v-model="materialForm.title" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="materialForm.type" style="width: 100%">
            <el-option label="图文" value="NEWS" /><el-option label="图片" value="IMAGE" /><el-option label="视频" value="VIDEO" />
          </el-select>
        </el-form-item>
        <el-form-item label="MediaID"><el-input v-model="materialForm.mediaId" /></el-form-item>
        <el-form-item label="链接"><el-input v-model="materialForm.url" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="materialForm.content" type="textarea" :rows="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="materialVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveMaterial">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt } from '@/utils/format'

const tab = ref('config')
const mock = ref(true)
const saving = ref(false)
const testing = ref(false)
const form = reactive<any>({})
const authVisible = ref(false)
const redirectUrl = ref('')
const authUrl = ref('')

const fans = ref<any[]>([])
const fanTotal = ref(0)
const fanPage = ref(1)
const fanSize = ref(10)
const fanLoading = ref(false)
const fanQuery = reactive<any>({ subscribed: null })
const tagVisible = ref(false)
const fanCurrent = ref<any>(null)
const tagText = ref('')

const materials = ref<any[]>([])
const materialType = ref('')
const materialVisible = ref(false)
const materialForm = reactive<any>({})

const tagList = ref<any[]>([])
const newTag = ref('')

const massVisible = ref(false)
const massForm = reactive<any>({ title: '', content: '', tag: '' })
const sending = ref(false)

async function loadConfig() {
  try {
    const res: any = await request.get('/tenant/wechat/config')
    Object.assign(form, res.data || {})
    if (!res.data?.configured) ElMessage.info('尚未配置微信参数，请完善后保存')
  } catch (e) { /* ignore */ }
}
async function saveConfig() {
  saving.value = true
  try {
    await request.put('/tenant/wechat/config', form)
    ElMessage.success('配置已保存并实时生效')
    loadConfig()
  } finally { saving.value = false }
}
async function test() {
  testing.value = true
  try {
    const res: any = await request.post('/tenant/wechat/config/test')
    ElMessage.success(res.data?.message || '连接成功')
    mock.value = res.data?.mode === 'MOCK'
  } finally { testing.value = false }
}
function openAuth() { authVisible.value = true }
async function genAuth() {
  const res: any = await request.get('/tenant/wechat/config/auth-url', { params: { redirect: redirectUrl.value } })
  authUrl.value = res.data?.url || ''
}
async function syncMenu() {
  const res: any = await request.post('/tenant/wechat/config/menu/sync')
  ElMessage.success(res.data?.message || '菜单同步完成')
}
async function loadFans() {
  fanLoading.value = true
  try {
    const res: any = await request.get('/tenant/wechat/fans/page', { params: { page: fanPage.value, size: fanSize.value, ...fanQuery } })
    fans.value = res.data.records || []
    fanTotal.value = res.data.total || 0
  } finally { fanLoading.value = false }
}
function openTag(row: any) { fanCurrent.value = row; tagText.value = row.tags || ''; tagVisible.value = true }
async function saveTag() {
  await request.post(`/tenant/wechat/fans/${fanCurrent.value.id}/tag`, null, { params: { tags: tagText.value } })
  ElMessage.success('标签已保存')
  tagVisible.value = false
  loadFans()
}
async function loadMaterials() {
  const res: any = await request.get('/tenant/wechat/materials', { params: { type: materialType.value } })
  materials.value = res.data || []
}
function openMaterial(row: any) {
  Object.keys(materialForm).forEach((k) => delete materialForm[k])
  Object.assign(materialForm, row ? { ...row } : { type: 'NEWS', status: 1 })
  materialVisible.value = true
}
async function saveMaterial() {
  if (!materialForm.title) { ElMessage.warning('请填写标题'); return }
  saving.value = true
  try {
    if (materialForm.id) await request.put('/tenant/wechat/materials', materialForm)
    else await request.post('/tenant/wechat/materials', materialForm)
    ElMessage.success('保存成功')
    materialVisible.value = false
    loadMaterials()
  } finally { saving.value = false }
}
async function removeMaterial(row: any) {
  await ElMessageBox.confirm('确认删除该素材？', '删除素材', { type: 'warning' })
  await request.delete(`/tenant/wechat/materials/${row.id}`)
  ElMessage.success('已删除')
  loadMaterials()
}
async function loadTags() {
  const res: any = await request.get('/tenant/wechat/tags')
  tagList.value = res.data || []
}
async function addTag() {
  if (!newTag.value) return
  await request.post('/tenant/wechat/tags', { name: newTag.value, fanCount: 0 })
  newTag.value = ''
  loadTags()
}
async function removeTag(row: any) {
  await request.delete(`/tenant/wechat/tags/${row.id}`)
  ElMessage.success('已删除')
  loadTags()
}
function openMass() {
  Object.assign(massForm, { title: '', content: '', tag: '' })
  massVisible.value = true
}
async function doMass() {
  if (!massForm.title || !massForm.content) { ElMessage.warning('请填写标题和内容'); return }
  sending.value = true
  try {
    const res: any = await request.post('/tenant/wechat/mass-send', null, { params: massForm })
    ElMessage.success(`目标 ${res.data?.targets || 0} 人，成功发送 ${res.data?.sent || 0} 条`)
    massVisible.value = false
  } finally { sending.value = false }
}

onMounted(async () => {
  await loadConfig(); loadFans(); loadMaterials(); loadTags()
})
</script>
