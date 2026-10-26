<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { portal, money } from '../api'
import { useI18n, bizName } from '../i18n'
import { setSeo } from '../seo'

const route = useRoute()
const { t } = useI18n()
const detail = ref<any>(null)
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    detail.value = await portal.store(route.params.id as string)
    const s = detail.value.store
    setSeo({ title: s.name, description: `${s.name}，地址：${s.address}，电话：${s.phone}。` })
  } catch (e: any) { error.value = e.message } finally { loading.value = false }
})
</script>

<template>
  <div class="page-body">
    <div class="container">
      <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
      <div v-else-if="error" class="empty-state">{{ error }}</div>
      <template v-else-if="detail">
        <div class="panel">
          <span class="tag">{{ detail.store.code }}</span>
          <h1 style="margin-top:10px">{{ detail.store.name }}</h1>
          <p class="muted">{{ detail.store.description || '欢迎光临本店' }}</p>
          <div class="kv"><span>{{ t('stores.address') }}</span><span>{{ detail.store.address }}</span></div>
          <div class="kv"><span>{{ t('stores.phone') }}</span><span>{{ detail.store.phone }}</span></div>
          <div class="kv"><span>营业时间</span><span>10:00 - 21:00</span></div>
          <div class="kv"><span>服务范围</span><span>{{ detail.store.serviceScope }}</span></div>
        </div>
        <div class="panel" v-if="(detail.technicians || []).length">
          <h3>{{ t('item.techs') }}</h3>
          <div class="tech-row">
            <div v-for="tech in detail.technicians" :key="tech.id" class="tech-card">
              <div class="tech-avatar"><span>{{ tech.name?.slice(0, 1) }}</span></div>
              <b>{{ tech.name }}</b>
              <div class="muted small">{{ tech.position }}</div>
              <div class="small">★ {{ tech.rating || '-' }}</div>
            </div>
          </div>
        </div>
        <div class="panel">
          <h3>{{ t('items.title') }}</h3>
          <div class="card-grid">
            <RouterLink v-for="it in (detail.items || []).slice(0, 8)" :key="it.id" class="card" :to="`/items/${it.id}`">
              <div class="card-body">
                <span class="tag">{{ bizName(it.bizType) }}</span>
                <h3 class="card-title">{{ it.name }}</h3>
                <div class="card-foot">
                  <span class="price">{{ money(it.price) }}</span>
                  <span class="muted small">{{ it.duration }} {{ t('common.minutes') }}</span>
                </div>
              </div>
            </RouterLink>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>
