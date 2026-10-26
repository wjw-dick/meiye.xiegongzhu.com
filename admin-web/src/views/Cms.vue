<template>
  <div class="page-card">
    <div class="page-header">
      <div>
        <h2 class="page-title">门户内容</h2>
        <div class="page-subtitle">公告、资讯、品牌故事中英双语内容与 SEO 配置，发布后门户即时生效</div>
      </div>
      <div class="flex gap-8">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="openEdit(null)">新建内容</el-button>
      </div>
    </div>

    <div class="filter-bar">
      <el-input v-model="query.kw" placeholder="标题关键字" clearable style="width: 200px" @keyup.enter="search" />
      <el-select v-model="query.type" placeholder="内容类型" clearable style="width: 140px" @change="search">
        <el-option label="资讯" value="NEWS" /><el-option label="公告" value="NOTICE" /><el-option label="单页" value="PAGE" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="search">
        <el-option label="已发布" :value="1" /><el-option label="草稿" :value="0" />
      </el-select>
      <el-select v-model="query.lang" placeholder="语言" clearable style="width: 120px" @change="search">
        <el-option label="中文" value="zh-CN" /><el-option label="English" value="en-US" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button @click="reset">重置</el-button>
    </div>

    <el-table :data="rows" v-loading="loading" stripe border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="封面" width="80">
        <template #default="{ row }">
          <el-image v-if="row.cover" :src="row.cover" style="width: 48px; height: 48px; border-radius: 6px" fit="cover" />
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">{{ typeText(row.type) }}</template>
      </el-table-column>
      <el-table-column label="语言" width="90"><template #default="{ row }">{{ row.lang === 'en-US' ? 'EN' : '中文' }}</template></el-table-column>
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag></template>
      </el-table-column>
      <el-table-column label="发布时间" width="160"><template #default="{ row }">{{ fmt(row.publishedAt) }}</template></el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 1" link type="success" @click="publish(row)">发布</el-button>
          <el-button v-else link type="warning" @click="offline(row)">下线</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="flex-between mt-16">
      <span class="text-muted">共 {{ total }} 篇内容</span>
      <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                     layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </div>

    <el-dialog v-model="editVisible" :title="form.id ? '编辑内容' : '新建内容'" width="860px" destroy-on-close top="6vh">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="标题" required><el-input v-model="form.title" /></el-form-item></el-col>
          <el-col :span="6">
            <el-form-item label="类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="资讯" value="NEWS" /><el-option label="公告" value="NOTICE" /><el-option label="单页" value="PAGE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="语言">
              <el-select v-model="form.lang" style="width: 100%">
                <el-option label="中文" value="zh-CN" /><el-option label="English" value="en-US" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="英文标题"><el-input v-model="form.titleEn" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="发布"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="封面图">
              <ImageUpload v-model="form.cover" dir="cms" tip="建议 16:9 或 4:3，单张不超过 10MB，自动上传对象存储" />
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
        <el-tabs v-model="contentTab">
          <el-tab-pane name="zh">
            <template #label><span class="lang-tab">中文正文<i v-if="!form.content" class="dot" /></span></template>
            <RichEditor v-model="form.content" :height="360" placeholder="中文正文，支持标题、加粗、列表、图片、链接" />
          </el-tab-pane>
          <el-tab-pane name="en">
            <template #label><span class="lang-tab">English<i v-if="!form.contentEn" class="dot" /></span></template>
            <RichEditor v-model="form.contentEn" :height="360" placeholder="English content, images are uploaded to object storage automatically" />
          </el-tab-pane>
          <el-tab-pane label="SEO" name="seo">
            <el-form-item label="SEO 关键字"><el-input v-model="form.seoKeywords" /></el-form-item>
            <el-form-item label="SEO 描述"><el-input v-model="form.seoDescription" type="textarea" :rows="3" /></el-form-item>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, Plus } from '@element-plus/icons-vue'
import request from '@/api/request'
import ImageUpload from '@/components/ImageUpload.vue'
import RichEditor from '@/components/RichEditor.vue'
import { fmt } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ kw: '', type: '', status: '', lang: '' })
const editVisible = ref(false)
const contentTab = ref('zh')
const form = reactive<any>({})

function typeText(v: any) { return ({ NEWS: '资讯', NOTICE: '公告', PAGE: '单页' } as any)[v] || v }
async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v !== '' && v !== null && v !== undefined) params[k] = v })
    const res: any = await request.get('/tenant/cms/articles/page', { params })
    rows.value = res.data.records || []
    total.value = res.data.total || 0
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { kw: '', type: '', status: '', lang: '' }); search() }
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? JSON.parse(JSON.stringify(row)) : { type: 'NEWS', lang: 'zh-CN', status: 0, sort: 0 })
  contentTab.value = 'zh'
  editVisible.value = true
}
async function save() {
  if (!form.title) { ElMessage.warning('请填写标题'); return }
  saving.value = true
  try {
    if (form.id) await request.put('/tenant/cms/articles', form)
    else await request.post('/tenant/cms/articles', form)
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally { saving.value = false }
}
async function publish(row: any) {
  await request.post(`/tenant/cms/articles/publish/${row.id}`)
  ElMessage.success('已发布')
  load()
}
async function offline(row: any) {
  await request.post(`/tenant/cms/articles/offline/${row.id}`)
  ElMessage.success('已下线')
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm('确认删除该内容？', '删除内容', { type: 'warning' })
  await request.delete(`/tenant/cms/articles/${row.id}`)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<style scoped>
.lang-tab { display: inline-flex; align-items: center; gap: 4px; }
.lang-tab .dot { width: 7px; height: 7px; border-radius: 50%; background: #f56c6c; display: inline-block; }
</style>
