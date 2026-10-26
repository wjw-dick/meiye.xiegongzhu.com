<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { portal, money } from '../api'
import { useI18n, bizName } from '../i18n'
import { loadSite, site } from '../site'
import { setSeo } from '../seo'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const items = ref<any[]>([])
const loading = ref(true)
const active = ref<string>((route.query.bizType as string) || '')

async function load() {
  loading.value = true
  try {
    items.value = await portal.items(active.value || undefined)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  setSeo({ title: t('items.title'), description: '浏览门店全部服务项目：面部护理、皮肤管理、美发、SPA、口腔与轻医美。' })
  await loadSite().catch(() => {})
  await load()
})

watch(active, (v) => {
  router.replace({ query: v ? { bizType: v } : {} })
  load()
})
watch(() => route.query.bizType, (v) => {
  if ((v as string || '') !== active.value) active.value = (v as string) || ''
})

const types = computed(() => (site.bizTypes || []).map((b: any) => ({ code: b.bizType || b.code, name: bizName(b.bizType || b.code) })))
</script>

<template>
  <div>
    <section class="page-hero">
      <div class="container">
        <div class="eyebrow">Services</div>
        <h1>{{ t('items.title') }}</h1>
        <p>全部服务明码标价，支持在线预约到店体验。</p>
      </div>
    </section>
    <div class="page-body">
      <div class="container">
        <div class="filter-row">
          <button class="filter-chip" :class="{ active: !active }" @click="active = ''">{{ t('items.all') }}</button>
          <button v-for="tp in types" :key="tp.code" class="filter-chip" :class="{ active: active === tp.code }" @click="active = tp.code">{{ tp.name }}</button>
        </div>
        <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
        <div v-else-if="!items.length" class="empty-state">{{ t('common.empty') }}</div>
        <div v-else class="card-grid">
          <RouterLink v-for="it in items" :key="it.id" class="card" :to="`/items/${it.id}`">
            <div class="card-cover"><img v-if="it.image" :src="it.image" :alt="it.name" loading="lazy" /></div>
            <div class="card-body">
              <span class="tag">{{ bizName(it.bizType) }}</span>
              <h3 class="card-title">{{ it.name }}</h3>
              <p class="card-desc">{{ it.description || it.category || '专业服务' }}</p>
              <div class="card-foot">
                <span class="price">{{ money(it.price) }}<small>/ {{ it.duration }}{{ t('common.minutes') }}</small></span>
                <span class="tag gold">{{ t('common.book') }}</span>
              </div>
            </div>
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
