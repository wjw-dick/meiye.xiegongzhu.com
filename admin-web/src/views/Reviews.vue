<template>
  <div>
    <div class="stat-grid mb-16">
      <div class="stat-card">
        <div class="label">评价总数</div>
        <div class="value">{{ stats.total || 0 }}</div>
        <div class="hint">来自客户小程序 / 服务后评价</div>
      </div>
      <div class="stat-card">
        <div class="label">平均评分</div>
        <div class="value">{{ Number(stats.avgScore || 0).toFixed(1) }}</div>
        <div class="hint"><el-rate :model-value="Number(stats.avgScore || 0)" disabled show-score score-template="{value}" /></div>
      </div>
      <div class="stat-card">
        <div class="label">待回复</div>
        <div class="value">{{ pendingReply }}</div>
        <div class="hint">建议 24 小时内回复</div>
      </div>
    </div>

    <div class="page-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">评价管理</h2>
          <div class="page-subtitle">评分、标签、图文评价、商家回复与展示状态</div>
        </div>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>

      <div class="filter-bar">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 140px" @change="search">
          <el-option label="已发布" value="PUBLISHED" /><el-option label="已隐藏" value="HIDDEN" />
        </el-select>
        <el-select v-model="query.score" placeholder="评分" clearable style="width: 120px" @change="search">
          <el-option v-for="n in 5" :key="n" :label="`${n} 星`" :value="n" />
        </el-select>
        <el-input v-model="query.technicianId" placeholder="技师ID" clearable style="width: 140px" @keyup.enter="search" />
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe border>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="评分" width="150">
          <template #default="{ row }"><el-rate :model-value="row.score" disabled size="small" /></template>
        </el-table-column>
        <el-table-column prop="customerId" label="会员ID" width="100" />
        <el-table-column prop="technicianId" label="技师ID" width="100" />
        <el-table-column prop="content" label="评价内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="tags" label="标签" width="150" show-overflow-tooltip />
        <el-table-column prop="reply" label="商家回复" min-width="200" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">{{ row.status === 'PUBLISHED' ? '已发布' : '已隐藏' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="时间" width="150"><template #default="{ row }">{{ fmt(row.createdAt, 'YYYY-MM-DD HH:mm') }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openReply(row)">回复</el-button>
            <el-button link :type="row.status === 'PUBLISHED' ? 'warning' : 'success'" @click="toggle(row)">{{ row.status === 'PUBLISHED' ? '隐藏' : '展示' }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="flex-between mt-16">
        <span class="text-muted">共 {{ total }} 条评价</span>
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" :page-sizes="[10, 20, 50]"
                       layout="sizes, prev, pager, next" @size-change="load" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="replyVisible" title="回复评价" width="560px">
      <el-form label-width="90px">
        <el-form-item label="评价内容">{{ current?.content }}</el-form-item>
        <el-form-item label="商家回复">
          <el-input v-model="replyText" type="textarea" :rows="3" placeholder="感谢您的评价，我们会持续改进服务" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doReply">保存回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Search } from '@element-plus/icons-vue'
import request from '@/api/request'
import { fmt } from '@/utils/format'

const rows = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const saving = ref(false)
const query = reactive<any>({ status: '', score: '', technicianId: '' })
const stats = ref<any>({})
const replyVisible = ref(false)
const replyText = ref('')
const current = ref<any>(null)
const pendingReply = computed(() => rows.value.filter((r) => !r.reply).length)

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    Object.entries(query).forEach(([k, v]) => { if (v !== '' && v !== null) params[k] = v })
    const [list, report] = await Promise.all([
      request.get('/tenant/reviews/page', { params }),
      request.get('/tenant/reports/marketing')
    ]) as any[]
    rows.value = list.data.records || []
    total.value = list.data.total || 0
    stats.value = report.data.reviews || {}
  } finally { loading.value = false }
}
function search() { page.value = 1; load() }
function reset() { Object.assign(query, { status: '', score: '', technicianId: '' }); search() }
function openReply(row: any) {
  current.value = row
  replyText.value = row.reply || ''
  replyVisible.value = true
}
async function doReply() {
  saving.value = true
  try {
    await request.put('/tenant/reviews', { ...current.value, reply: replyText.value, repliedAt: new Date().toISOString().slice(0, 19).replace('T', ' ') })
    ElMessage.success('回复已保存')
    replyVisible.value = false
    load()
  } finally { saving.value = false }
}
async function toggle(row: any) {
  await request.put('/tenant/reviews', { ...row, status: row.status === 'PUBLISHED' ? 'HIDDEN' : 'PUBLISHED' })
  ElMessage.success('状态已更新')
  load()
}
onMounted(load)
</script>
