<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { portal, money } from '../api'
import { useI18n } from '../i18n'
import { setSeo } from '../seo'

const route = useRoute()
const { t } = useI18n()
const pkg = ref<any>(null)
const loading = ref(true)
const error = ref('')
const bookingUrl = computed(() => `${import.meta.env.VITE_MP_H5_URL || 'http://127.0.0.1:5175'}/#/login`)

onMounted(async () => {
  try {
    pkg.value = await portal.pkg(route.params.id as string)
    setSeo({ title: pkg.value.name, description: pkg.value.description || '优惠套餐详情' })
  } catch (e: any) { error.value = e.message } finally { loading.value = false }
})
</script>

<template>
  <div class="page-body">
    <div class="container">
      <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
      <div v-else-if="error" class="empty-state">{{ error }}</div>
      <div v-else-if="pkg" class="split">
        <div class="panel">
          <span class="tag gold">{{ pkg.type === 'TIMES' ? '次卡套餐' : '储值套餐' }}</span>
          <h1 style="margin-top:12px">{{ pkg.name }}</h1>
          <p class="muted">{{ pkg.description || '套餐详情以门店说明为准。' }}</p>
          <div class="kv"><span>套餐价格</span><span class="price">{{ money(pkg.price) }}</span></div>
          <div class="kv" v-if="pkg.faceValue"><span>面值</span><span>{{ money(pkg.faceValue) }}</span></div>
          <div class="kv"><span>包含次数</span><span>{{ pkg.times || '-' }}</span></div>
          <div class="kv"><span>有效期</span><span>{{ pkg.validDays || '-' }} {{ t('common.days') }}</span></div>
          <div class="kv"><span>适用品类</span><span>{{ pkg.bizType || '全场通用' }}</span></div>
          <div class="prose" style="margin-top:20px">
            <h3>购买须知</h3>
            <p>套餐购买后可在会员卡包中查看，到店出示会员码核销；具体使用规则以门店公示为准。</p>
          </div>
          <a class="btn btn-primary" :href="bookingUrl">{{ t('common.book') }}</a>
        </div>
        <div class="panel">
          <h3>套餐明细</h3>
          <p class="muted small">套餐内容与次数以系统配置为准。</p>
          <pre style="white-space:pre-wrap;font-size:13px;color:#574e5b;background:#fbf7f9;border-radius:12px;padding:16px">{{ pkg.itemsJson || '暂无明细' }}</pre>
        </div>
      </div>
    </div>
  </div>
</template>
