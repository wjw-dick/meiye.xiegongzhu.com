<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { portal } from '../api'
import { useI18n } from '../i18n'
import { setSeo } from '../seo'

const { t } = useI18n()
const list = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  setSeo({ title: t('news.title'), description: '门店公告、活动资讯与护理知识。' })
  try { list.value = await portal.articles('NOTICE') } finally { loading.value = false }
})
</script>

<template>
  <div>
    <section class="page-hero">
      <div class="container">
        <div class="eyebrow">News</div>
        <h1>{{ t('news.title') }}</h1>
        <p>门店公告、品牌活动与专业护理知识。</p>
      </div>
    </section>
    <div class="page-body">
      <div class="container">
        <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
        <div v-else-if="!list.length" class="empty-state">{{ t('common.empty') }}</div>
        <div v-else class="article-list">
          <RouterLink v-for="a in list" :key="a.id" class="article-card" :to="`/news/${a.id}`">
            <div class="article-date">{{ (a.publishedAt || a.createdAt || '').slice(0, 10) }}</div>
            <h3 class="card-title">{{ a.title }}</h3>
            <p class="card-desc">{{ a.summary || '点击查看详情' }}</p>
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
