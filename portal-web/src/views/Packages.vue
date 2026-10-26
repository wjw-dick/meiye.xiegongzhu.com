<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { portal, money } from '../api'
import { useI18n, bizName } from '../i18n'
import { setSeo } from '../seo'

const { t } = useI18n()
const list = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  setSeo({ title: t('packages.title'), description: '美业大健康优惠套餐，多次服务、储值优惠，到店核销。' })
  try { list.value = await portal.packages() } finally { loading.value = false }
})
</script>

<template>
  <div>
    <section class="page-hero">
      <div class="container">
        <div class="eyebrow">Packages</div>
        <h1>{{ t('packages.title') }}</h1>
        <p>多次卡与储值套餐，支持门店核销与余额查询。</p>
      </div>
    </section>
    <div class="page-body">
      <div class="container">
        <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
        <div v-else-if="!list.length" class="empty-state">{{ t('common.empty') }}</div>
        <div v-else class="card-grid three">
          <RouterLink v-for="p in list" :key="p.id" class="card" :to="`/packages/${p.id}`">
            <div class="card-body">
              <span class="tag gold">{{ p.type === 'TIMES' ? '次卡' : '储值卡' }}</span>
              <span class="tag" v-if="p.bizType" style="margin-left:6px">{{ bizName(p.bizType) }}</span>
              <h3 class="card-title" style="margin-top:8px">{{ p.name }}</h3>
              <p class="card-desc">{{ p.description || '套餐内容以门店说明为准' }}</p>
              <div class="kv"><span>{{ t('common.times') }}</span><span>{{ p.times || '-' }}</span></div>
              <div class="kv"><span>{{ t('common.valid') }}</span><span>{{ p.validDays || '-' }} {{ t('common.days') }}</span></div>
              <div class="card-foot">
                <span class="price">{{ money(p.price) }}</span>
                <span class="tag">{{ t('common.book') }}</span>
              </div>
            </div>
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
