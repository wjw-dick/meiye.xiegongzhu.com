<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { portal } from '../api'
import { useI18n } from '../i18n'
import { setSeo } from '../seo'

const route = useRoute()
const { t } = useI18n()
const article = ref<any>(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    article.value = await portal.article(route.params.id as string)
    setSeo({
      title: article.value.title,
      description: article.value.summary || String(article.value.content || '').replace(/<[^>]+>/g, '').slice(0, 120),
      jsonLd: {
        '@context': 'https://schema.org', '@type': 'Article', headline: article.value.title,
        datePublished: article.value.publishedAt || article.value.createdAt
      }
    })
  } catch (e: any) { error.value = e.message } finally { loading.value = false }
})
</script>

<template>
  <div>
    <section class="page-hero">
      <div class="container">
        <RouterLink to="/news" class="muted small">← {{ t('common.back') }}</RouterLink>
        <h1 v-if="article" style="margin-top:12px">{{ article.title }}</h1>
        <p v-if="article" class="muted">{{ (article.publishedAt || article.createdAt || '').slice(0, 10) }}</p>
      </div>
    </section>
    <div class="page-body">
      <div class="container" style="max-width:820px">
        <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
        <div v-else-if="error" class="empty-state">{{ error }}</div>
        <article v-else-if="article" class="panel prose">
          <img v-if="article.cover" :src="article.cover" :alt="article.title" style="width:100%;border-radius:14px;margin-bottom:20px" />
          <p v-if="article.summary" class="muted">{{ article.summary }}</p>
          <div v-html="article.content || '<p>暂无内容</p>'"></div>
        </article>
      </div>
    </div>
  </div>
</template>
