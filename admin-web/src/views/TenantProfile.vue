<template>
  <div class="page-card" v-loading="loading">
    <div class="page-header">
      <div>
        <h2 class="page-title">租户配置</h2>
        <div class="page-subtitle">品牌信息、经营参数、预约规则、积分与预警配置（按租户隔离）</div>
      </div>
      <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
    </div>

    <el-row :gutter="20">
      <el-col :span="10">
        <div class="section-title">基础信息</div>
        <el-form label-width="110px">
          <el-form-item label="租户编码"><el-input :model-value="profile.tenant?.code" disabled /></el-form-item>
          <el-form-item label="租户名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="服务电话"><el-input v-model="form.servicePhone" /></el-form-item>
          <el-form-item label="品牌 LOGO">
            <ImageUpload v-model="form.logo" dir="brand" tip="建议正方形 512×512，用于后台与门户展示" />
          </el-form-item>
          <el-form-item label="状态">
            <el-tag :type="profile.tenant?.status === 'NORMAL' ? 'success' : 'warning'">{{ profile.tenant?.status }}</el-tag>
          </el-form-item>
          <el-form-item label="到期时间">{{ fmt(profile.tenant?.expireAt, 'YYYY-MM-DD') }}</el-form-item>
          <el-form-item label="门店上限">{{ profile.tenant?.storeLimit }}</el-form-item>
          <el-form-item label="客户上限">{{ profile.tenant?.customerLimit }}</el-form-item>
        </el-form>
      </el-col>
      <el-col :span="14">
        <div class="section-title">经营参数</div>
        <el-form label-width="140px">
          <el-row :gutter="16">
            <el-col :span="12"><el-form-item label="默认提成比例(%)"><el-input-number v-model="config.commissionRate" :min="0" :max="100" :precision="2" style="width: 100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="积分兑换比例"><el-input-number v-model="config.pointsPerYuan" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="预约超时(分钟)"><el-input-number v-model="config.appointmentTimeoutMinutes" :min="5" style="width: 100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="预约提前天数"><el-input-number v-model="config.appointmentAdvanceDays" :min="1" style="width: 100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="卡到期预警(天)"><el-input-number v-model="config.cardWarnDays" :min="1" style="width: 100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="证书预警(天)"><el-input-number v-model="config.certWarnDays" :min="1" style="width: 100%" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="跨店通兑"><el-switch v-model="config.crossStore" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="允许挂账"><el-switch v-model="config.allowDebt" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="开票主体"><el-input v-model="config.invoiceTitle" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="门店地址"><el-input v-model="config.address" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="预约须知"><el-input v-model="config.bookingNotice" type="textarea" :rows="3" /></el-form-item></el-col>
          </el-row>
        </el-form>
      </el-col>
    </el-row>

    <div class="section-title mt-16">多端与存储</div>
    <div class="quick-links">
      <div class="quick-card" @click="$router.push('/wechat')">
        <div class="quick-title">微信与 App 配置</div>
        <div class="quick-desc">公众号 / 小程序 / 移动应用 AppID、密钥与模板消息</div>
        <el-button link type="primary">前往配置</el-button>
      </div>
      <div class="quick-card" @click="$router.push('/storage')">
        <div class="quick-title">对象存储</div>
        <div class="quick-desc">本地 / MinIO / OSS / COS / S3 五驱动切换与连接测试</div>
        <el-button link type="primary">前往配置</el-button>
      </div>
      <div class="quick-card" @click="$router.push('/menus')">
        <div class="quick-title">菜单可视化</div>
        <div class="quick-desc">小程序 tabBar、H5 导航、公众号菜单统一配置</div>
        <el-button link type="primary">前往配置</el-button>
      </div>
    </div>

    <div class="section-title mt-16">已启用品类</div>
    <div class="biz-list">
      <el-tag v-for="b in profile.bizTypes?.filter((x: any) => x.enabled)" :key="b.code" type="success" effect="plain" size="large">
        {{ b.name }}<span v-if="b.medical" class="medical">（医疗）</span>
      </el-tag>
      <span v-if="!profile.bizTypes?.some((x: any) => x.enabled)" class="text-muted">尚未启用品类，请前往「品类开关」开启</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import ImageUpload from '@/components/ImageUpload.vue'
import { fmt } from '@/utils/format'

const loading = ref(false)
const saving = ref(false)
const profile = ref<any>({})
const form = reactive<any>({ name: '', servicePhone: '', logo: '' })
const config = reactive<any>({
  commissionRate: 5, pointsPerYuan: 1, appointmentTimeoutMinutes: 30, appointmentAdvanceDays: 30,
  cardWarnDays: 15, certWarnDays: 30, crossStore: false, allowDebt: true, invoiceTitle: '', address: '', bookingNotice: ''
})

async function load() {
  loading.value = true
  try {
    const res: any = await request.get('/tenant/profile')
    profile.value = res.data || {}
    form.name = profile.value.config?.name || profile.value.tenant?.name || ''
    form.servicePhone = profile.value.config?.servicePhone || ''
    form.logo = profile.value.config?.logo || ''
    Object.assign(config, profile.value.configMap || {})
  } finally { loading.value = false }
}
async function save() {
  saving.value = true
  try {
    await request.put('/tenant/profile', { ...form, config })
    ElMessage.success('配置已保存')
    load()
  } finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.section-title { font-weight: 600; font-size: 15px; margin-bottom: 14px; padding-left: 10px; border-left: 3px solid var(--my-primary); }
.biz-list { display: flex; flex-wrap: wrap; gap: 10px; }
.quick-links { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; }
.quick-card { border: 1px solid #eef0f6; border-radius: 12px; padding: 16px; background: #fff; transition: all .16s ease; }
.quick-card:hover { border-color: #c9bcff; box-shadow: 0 8px 22px rgba(82, 65, 160, .07); }
.quick-title { font-weight: 600; font-size: 14px; }
.quick-desc { font-size: 12px; color: #8b90a3; margin: 6px 0 8px; line-height: 1.6; min-height: 38px; }
.medical { color: #e6a23c; }
</style>
