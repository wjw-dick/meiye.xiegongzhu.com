<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { portal, money, dateText } from '../api'
import { useI18n, bizName } from '../i18n'
import { site } from '../site'
import { setSeo } from '../seo'

const route = useRoute()
const { t } = useI18n()
const detail = ref<any>(null)
const loading = ref(true)
const error = ref('')
const slotDate = ref(new Date(Date.now() + 86400000).toISOString().slice(0, 10))
const slots = ref<any[]>([])
const slotLoading = ref(false)

const booking = computed(() => {
  const base = import.meta.env.VITE_MP_H5_URL || 'http://127.0.0.1:5175'
  return `${base}/#/login?tenant=${site.tenant?.code || '5MY'}&itemId=${route.params.id}`
})

async function loadSlots() {
  slotLoading.value = true
  try {
    const stores = detail.value?.stores || []
    if (stores.length) slots.value = await portal.slots(stores[0].id, slotDate.value, Number(route.params.id))
  } catch { slots.value = [] } finally { slotLoading.value = false }
}

onMounted(async () => {
  try {
    detail.value = await portal.item(route.params.id as string)
    const it = detail.value.item
    setSeo({
      title: it.name,
      description: it.description || `${it.name}，${it.duration}分钟，${money(it.price)}。`,
      keywords: `${it.name},${bizName(it.bizType)},预约`,
      jsonLd: {
        '@context': 'https://schema.org', '@type': 'Service', name: it.name,
        description: it.description || '', offers: { '@type': 'Offer', price: it.price, priceCurrency: 'CNY' }
      }
    })
    await loadSlots()
  } catch (e: any) { error.value = e.message } finally { loading.value = false }
})
</script>

<template>
  <div>
    <div class="page-body">
      <div class="container">
        <div v-if="loading" class="loading">{{ t('common.loading') }}</div>
        <div v-else-if="error" class="empty-state">{{ error }}</div>
        <template v-else-if="detail">
          <div class="split">
            <div>
              <div class="detail-cover">
                <img v-if="detail.item.image" :src="detail.item.image" :alt="detail.item.name" />
              </div>
              <div class="panel" style="margin-top: 20px">
                <span class="tag">{{ bizName(detail.item.bizType) }}</span>
                <h1 style="margin-top: 12px">{{ detail.item.name }}</h1>
                <p class="muted">{{ detail.item.description || '专业服务，具体以门店说明为准。' }}</p>
                <div class="kv"><span>{{ t('item.duration') }}</span><span>{{ detail.item.duration }} {{ t('common.minutes') }}</span></div>
                <div class="kv"><span>门店价</span><span class="price">{{ money(detail.item.price) }}</span></div>
                <div class="kv" v-if="detail.item.memberPrice"><span>会员价</span><span>{{ money(detail.item.memberPrice) }}</span></div>
                <div class="kv"><span>服务流程</span><span>咨询建档 → 专业服务 → 术后关怀</span></div>
              </div>
            </div>
            <div>
              <div class="panel">
                <h3>{{ t('common.book') }}</h3>
                <p class="muted small">选择门店与日期查看可预约时段，登录后可完成预约。</p>
                <div class="field">
                  <label>{{ t('item.stores') }}</label>
                  <select v-model="detail.stores[0].id" style="width:100%;padding:12px;border:1px solid var(--line);border-radius:12px">
                    <option v-for="s in detail.stores" :key="s.id" :value="s.id">{{ s.name }}</option>
                  </select>
                </div>
                <div class="field" style="margin-top:12px">
                  <label>日期</label>
                  <input v-model="slotDate" type="date" @change="loadSlots" />
                </div>
                <div v-if="slotLoading" class="muted small" style="margin-top:14px">查询时段中…</div>
                <div v-else style="margin-top:14px">
                  <div v-if="!slots.length" class="muted small">当日暂无可约时段，可联系门店。</div>
                  <div class="filter-row" v-else>
                    <span v-for="(s, i) in slots.slice(0, 8)" :key="i" class="filter-chip" style="font-size:12px;padding:5px 10px">
                      {{ s.start || s.startTime || s.time }}
                    </span>
                  </div>
                </div>
                <a class="btn btn-primary" style="width:100%;margin-top:20px" :href="booking">{{ t('nav.booking') }}</a>
              </div>
              <div class="panel" v-if="(detail.technicians || []).length">
                <h3>{{ t('item.techs') }}</h3>
                <div class="tech-row">
                  <div v-for="tech in detail.technicians" :key="tech.id" class="tech-card">
                    <div class="tech-avatar">
                      <img v-if="tech.avatar" :src="tech.avatar" :alt="tech.name" style="width:100%;height:100%;border-radius:50%;object-fit:cover" />
                      <span v-else>{{ tech.name?.slice(0, 1) }}</span>
                    </div>
                    <b>{{ tech.name }}</b>
                    <div class="muted small">{{ tech.position }}</div>
                    <div class="small">★ {{ tech.rating || '-' }} · {{ tech.serviceCount || 0 }} 单</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>
