<template>
  <div class="page-card" v-loading="loading">
    <div class="page-header">
      <div>
        <h2 class="page-title">对象存储</h2>
        <div class="page-subtitle">
          图片、证书、报告 PDF 统一存对象存储；切换驱动后历史文件仍可读，新文件写入新驱动
        </div>
      </div>
      <div class="flex gap-8">
        <el-tag :type="tested ? 'success' : 'info'" effect="plain">
          {{ tested ? '连接测试已通过' : '保存前需先通过连接测试' }}
        </el-tag>
        <el-button :icon="Connection" :loading="testing" :disabled="!canTest" @click="test">连接测试</el-button>
        <el-button type="primary" :icon="Check" :loading="saving" :disabled="!tested" @click="save">保存配置</el-button>
      </div>
    </div>

    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="密钥以 AES 加密存储，回显仅显示掩码；如需重新测试或更换密钥，请重新输入 AccessKey / SecretKey。" />

    <div class="driver-grid">
      <div
        v-for="d in drivers"
        :key="d.code"
        class="driver-card"
        :class="{ active: form.driver === d.code, current: currentDriver === d.code }"
        @click="selectDriver(d.code)"
      >
        <div class="driver-head">
          <div class="driver-icon"><el-icon><component :is="d.icon" /></el-icon></div>
          <el-tag v-if="currentDriver === d.code" size="small" type="success" effect="dark">使用中</el-tag>
        </div>
        <div class="driver-name">{{ d.name }}</div>
        <div class="driver-desc">{{ d.desc }}</div>
        <div class="driver-check" v-if="form.driver === d.code"><el-icon><CircleCheckFilled /></el-icon> 已选择</div>
      </div>
    </div>

    <div class="config-card">
      <div class="section-title">
        {{ driverName }} 配置
        <span v-if="configured" class="text-muted" style="font-weight: 400; font-size: 12px; margin-left: 8px">
          当前已保存配置，密钥显示为掩码
        </span>
      </div>

      <el-form label-width="130px" class="config-form" @change="markDirty">
        <el-row :gutter="24">
          <template v-if="form.driver === 'LOCAL'">
            <el-col :span="12">
              <el-form-item label="本地目录">
                <el-input v-model="form.localDir" placeholder="/opt/meiye/data/uploads" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="访问前缀">
                <el-input v-model="form.publicBase" placeholder="/api/files" />
                <div class="field-tip">静态资源对外访问的基础路径，需与 Nginx 一致</div>
              </el-form-item>
            </el-col>
          </template>

          <template v-else>
            <el-col :span="12">
              <el-form-item label="Endpoint" required>
                <el-input v-model="form.endpoint" :placeholder="form.driver === 'OSS' ? 'https://oss-cn-hangzhou.aliyuncs.com' : form.driver === 'COS' ? 'https://cos.ap-guangzhou.myqcloud.com' : form.driver === 'MINIO' ? 'https://minio.example.com' : 'https://s3.ap-east-1.amazonaws.com'" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Bucket" required>
                <el-input v-model="form.bucket" placeholder="meiye" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="AccessKey" required>
                <el-input v-model="form.accessKey" type="password" show-password autocomplete="new-password" placeholder="请输入 AccessKey" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="SecretKey" required>
                <el-input v-model="form.secretKey" type="password" show-password autocomplete="new-password" placeholder="请输入 SecretKey" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Region">
                <el-input v-model="form.region" placeholder="ap-guangzhou / cn-hangzhou" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="自定义域名">
                <el-input v-model="form.domain" placeholder="https://cdn.example.com（可选）" />
                <div class="field-tip">配置后文件 URL 使用该域名，需完成 CDN/CORS 配置</div>
              </el-form-item>
            </el-col>
          </template>
        </el-row>
      </el-form>

      <el-alert v-if="testError" type="error" :closable="false" show-icon :title="testError" class="mt-8" />
      <div class="driver-hint">
        <el-icon><InfoFilled /></el-icon>
        <span>{{ driverHint }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, CircleCheckFilled, Cloudy, Coin, Connection, FolderOpened, Box, InfoFilled } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const scope = computed(() => (route.path.startsWith('/platform') ? 'platform' : 'tenant'))

const drivers = [
  { code: 'LOCAL', name: '本地存储', icon: FolderOpened, desc: '服务器本地磁盘，零依赖，适合单机与测试' },
  { code: 'MINIO', name: 'MinIO', icon: Coin, desc: '自建 S3 兼容对象存储，数据自主可控' },
  { code: 'OSS', name: '阿里云 OSS', icon: Cloudy, desc: '阿里云对象存储，支持 CDN 与图片处理' },
  { code: 'COS', name: '腾讯云 COS', icon: Cloudy, desc: '腾讯云对象存储，与微信生态集成便利' },
  { code: 'S3', name: 'AWS S3', icon: Box, desc: 'Amazon S3 及任意 S3 兼容服务' }
]

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const configured = ref(false)
const currentDriver = ref('LOCAL')
const tested = ref(false)
const testError = ref('')
const form = reactive<any>({
  driver: 'LOCAL', endpoint: '', accessKey: '', secretKey: '', bucket: 'meiye',
  region: '', domain: '', localDir: '', publicBase: '/api/files'
})

const driverName = computed(() => drivers.find(d => d.code === form.driver)?.name || form.driver)
const driverHint = computed(() => {
  if (form.driver === 'LOCAL') return '本地存储需确保目录对后端进程可写，并已纳入备份；多实例部署请使用共享存储。'
  if (form.driver === 'MINIO') return 'MinIO 需保证 Bucket 存在且允许 PutObject；生产建议启用 HTTPS 与版本保护。'
  if (form.driver === 'OSS') return '阿里云子账号需授予 oss:PutObject / oss:GetObject 权限，Bucket 跨域需允许业务域名。'
  if (form.driver === 'COS') return '腾讯云子账号需授予 COS 读写权限，并在存储桶配置跨域访问 CORS。'
  return 'S3 需确认 Region 与 Endpoint 匹配；私有 Bucket 需通过预签名或 CDN 回源访问。'
})

const requiredFilled = computed(() => {
  if (form.driver === 'LOCAL') return !!form.localDir
  return !!form.endpoint && !!form.accessKey && !!form.secretKey && !!form.bucket
})
const canTest = computed(() => requiredFilled.value && !testing.value)
const signature = computed(() => JSON.stringify({ ...form }))

function markDirty() {
  tested.value = false
  testError.value = ''
}

function selectDriver(code: string) {
  if (form.driver === code) return
  form.driver = code
  if (code === 'LOCAL' && !form.localDir) form.localDir = '/opt/meiye/data/uploads'
  markDirty()
}

async function load() {
  loading.value = true
  try {
    const res: any = await request.get(`/${scope.value}/storage`)
    const d = res.data || {}
    Object.assign(form, {
      driver: d.driver || 'LOCAL',
      endpoint: d.endpoint || '',
      accessKey: d.accessKey || '',
      secretKey: d.secretKey || '',
      bucket: d.bucket || 'meiye',
      region: d.region || '',
      domain: d.domain || '',
      localDir: d.localDir || '',
      publicBase: d.publicBase || '/api/files'
    })
    configured.value = !!d.configured
    currentDriver.value = d.driver || 'LOCAL'
    tested.value = false
  } finally {
    loading.value = false
  }
}

async function test() {
  testing.value = true
  testError.value = ''
  try {
    await request.post(`/${scope.value}/storage/test`, { ...form })
    tested.value = true
    ElMessage.success('连接测试通过，可以保存')
  } catch (e: any) {
    tested.value = false
    testError.value = e?.message || '连接测试失败'
  } finally {
    testing.value = false
  }
}

async function save() {
  if (!tested.value) {
    ElMessage.warning('请先通过连接测试')
    return
  }
  saving.value = true
  try {
    const res: any = await request.post(`/${scope.value}/storage`, { ...form })
    const d = res.data || {}
    form.accessKey = d.accessKey || '******'
    form.secretKey = d.secretKey || '******'
    configured.value = true
    currentDriver.value = d.driver || form.driver
    tested.value = false
    ElMessage.success('对象存储配置已保存并生效')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.driver-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px; }
.driver-card {
  position: relative; border: 1.5px solid #e6e9f2; border-radius: 14px; padding: 16px;
  background: #fff; cursor: pointer; transition: all .18s ease; min-height: 136px;
}
.driver-card:hover { border-color: #c9bcff; box-shadow: 0 8px 24px rgba(82, 65, 160, .08); transform: translateY(-1px); }
.driver-card.active { border-color: var(--my-primary, #6b4eff); background: #faf8ff; }
.driver-head { display: flex; justify-content: space-between; align-items: flex-start; }
.driver-icon {
  width: 38px; height: 38px; border-radius: 10px; background: #f1edff; color: #6b4eff;
  display: flex; align-items: center; justify-content: center; font-size: 20px;
}
.driver-name { font-weight: 600; font-size: 14px; margin-top: 12px; }
.driver-desc { font-size: 11.5px; color: #8b90a3; line-height: 1.6; margin-top: 6px; }
.driver-check { font-size: 11px; color: #6b4eff; display: flex; align-items: center; gap: 3px; margin-top: 6px; }
.config-card { margin-top: 20px; border: 1px solid #eef0f6; border-radius: 14px; padding: 20px 22px 12px; background: #fff; }
.section-title { font-weight: 600; font-size: 15px; margin-bottom: 16px; padding-left: 10px; border-left: 3px solid var(--my-primary); }
.config-form { max-width: 980px; }
.field-tip { font-size: 11px; color: #9aa0b2; line-height: 1.5; margin-top: 4px; }
.driver-hint { display: flex; gap: 8px; align-items: flex-start; margin-top: 8px; padding: 10px 12px; background: #f7f8fc; border-radius: 8px; color: #7d8398; font-size: 12px; line-height: 1.7; }
@media (max-width: 1200px) { .driver-grid { grid-template-columns: repeat(2, 1fr); } }
</style>
