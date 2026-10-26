<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { portal } from '../api'
import { useI18n } from '../i18n'
import { setSeo } from '../seo'

const { t } = useI18n()
const list = ref<any[]>([])
const loading = ref(true)

onMounted(async () => {
  setSeo({ title: t('stores.title'), description: '查找附近门店地址、电话与服务范围。' })
  try { list.value = await portal.stores() } finally { loading.value = false }
})
</script>

<template>
  <div>
    <section class="page-hero">
      <div class="container">
        <div class="eyebrow">Stores</div>
        <h1>{{ t('stores.title') }}</h1>
        <p>全国连锁门店，就近选择专业服务。</p>
      </div>
    </section>
    <div class="page-body">
      <div class="container">
        <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
        <div v-else-if="!list.length" class="empty-state">{{ t('common.empty') }}</div>
        <div v-else class="store-list">
          <div v-for="s in list" :key="s.id" class="store-card">
            <span class="tag">{{ s.code }}</span>
            <h3 style="margin-top:10px">{{ s.name }}</h3>
            <p class="muted small">{{ s.description || '专业门店' }}</p>
            <div class="store-line"><b>{{ t('stores.address') }}</b><span>{{ s.address }}</span></div>
            <div class="store-line"><b>{{ t('stores.phone') }}</b><span>{{ s.phone }}</span></div>
            <div class="store-line"><b>服务范围</b><span>{{ s.serviceScope }}</span></div>
            <RouterLink :to="`/stores/${s.id}`" class="btn btn-ghost btn-sm" style="margin-top:8px">{{ t('common.more') }}</RouterLink>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
