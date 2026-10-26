<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { portal, money } from '../api'
import { useI18n, bizName } from '../i18n'
import { loadSite, site, storeName, servicePhone } from '../site'
import { setSeo } from '../seo'

const { t } = useI18n()
const loading = ref(true)
const error = ref('')
const data = ref<any>({})

onMounted(async () => {
  try {
    await loadSite()
    data.value = await portal.home()
    setSeo({
      title: `${storeName()} · 在线预约`,
      description: `${storeName()}提供${(data.value.bizTypes || []).map((b: any) => bizName(b.bizType || b.code)).join('、')}等服务，在线预约、会员卡包、优惠套餐一站直达。`,
      keywords: `${storeName()},美容,皮肤管理,预约`,
      jsonLd: {
        '@context': 'https://schema.org',
        '@type': 'HealthAndBeautyBusiness',
        name: storeName(),
        telephone: servicePhone(),
        address: (data.value.stores || [])[0]?.address || '',
        aggregateRating: { '@type': 'AggregateRating', ratingValue: '4.9', reviewCount: 128 }
      }
    })
  } catch (e: any) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

const reload = () => window.location.reload()

const booking = (itemId?: number) => {
  const base = import.meta.env.VITE_MP_H5_URL || 'http://127.0.0.1:5175'
  return `${base}/#/login?tenant=${site.tenant?.code || '5MY'}${itemId ? '&itemId=' + itemId : ''}`
}
</script>

<template>
  <div>
    <section class="hero">
      <div class="container hero-inner">
        <div>
          <span class="hero-tag">{{ t('home.heroTag') }}</span>
          <h1>{{ t('home.heroTitle') }}</h1>
          <p>{{ t('home.heroSub') }}</p>
          <div class="hero-actions">
            <a class="btn btn-primary" :href="booking()">{{ t('nav.booking') }}</a>
            <RouterLink class="btn btn-ghost" to="/items">{{ t('common.more') }}</RouterLink>
          </div>
          <div class="hero-stats">
            <div><b>{{ (data.stores || []).length }}</b><span>{{ t('nav.stores') }}</span></div>
            <div><b>{{ (data.items || []).length }}+</b><span>{{ t('nav.items') }}</span></div>
            <div><b>4.9</b><span>Rating</span></div>
          </div>
        </div>
        <div class="hero-card">
          <h4>{{ t('home.biz') }}</h4>
          <ul>
            <li v-for="b in (data.bizTypes || []).slice(0, 6)" :key="b.bizType || b.code">
              <span>{{ bizName(b.bizType || b.code) }}</span>
              <span>›</span>
            </li>
          </ul>
        </div>
      </div>
    </section>

    <div v-if="loading" class="container loading">{{ t('common.loading') }}</div>
    <div v-else-if="error" class="container loading">
      <div class="empty-state">
        <p>{{ error }}</p>
        <button class="btn btn-ghost" @click="reload">重试</button>
      </div>
    </div>
    <template v-else>
      <section class="section">
        <div class="container">
          <div class="section-head">
            <div>
              <div class="eyebrow">Categories</div>
              <h2>{{ t('home.biz') }}</h2>
              <p>按品类发现适合你的专业服务</p>
            </div>
          </div>
          <div class="category-grid">
            <RouterLink v-for="b in data.bizTypes" :key="b.bizType || b.code" class="category-card" :to="`/items?bizType=${b.bizType || b.code}`">
              <div class="category-icon">{{ (bizName(b.bizType || b.code) || '·').slice(0, 1) }}</div>
              <div>{{ bizName(b.bizType || b.code) }}</div>
            </RouterLink>
          </div>
        </div>
      </section>

      <section class="section alt">
        <div class="container">
          <div class="section-head">
            <div>
              <div class="eyebrow">Featured</div>
              <h2>{{ t('home.featured') }}</h2>
              <p>门店热销，专业技师一对一服务</p>
            </div>
            <RouterLink to="/items" class="btn btn-ghost btn-sm">{{ t('common.more') }}</RouterLink>
          </div>
          <div class="card-grid">
            <RouterLink v-for="it in data.items" :key="it.id" class="card" :to="`/items/${it.id}`">
              <div class="card-cover">
                <img v-if="it.image" :src="it.image" :alt="it.name" loading="lazy" />
              </div>
              <div class="card-body">
                <span class="tag">{{ bizName(it.bizType) }}</span>
                <h3 class="card-title">{{ it.name }}</h3>
                <p class="card-desc">{{ it.description || it.category || '专业服务，欢迎到店体验' }}</p>
                <div class="card-foot">
                  <span class="price">{{ money(it.price) }}<small>/ {{ it.duration }}{{ t('common.minutes') }}</small></span>
                  <span class="tag gold">{{ t('common.book') }}</span>
                </div>
              </div>
            </RouterLink>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="container">
          <div class="section-head">
            <div>
              <div class="eyebrow">Packages</div>
              <h2>{{ t('home.packages') }}</h2>
              <p>多次服务更划算，支持到店核销</p>
            </div>
            <RouterLink to="/packages" class="btn btn-ghost btn-sm">{{ t('common.more') }}</RouterLink>
          </div>
          <div class="card-grid three">
            <RouterLink v-for="p in (data.packages || []).slice(0, 3)" :key="p.id" class="card" :to="`/packages/${p.id}`">
              <div class="card-body">
                <span class="tag gold">{{ p.type === 'TIMES' ? '次卡套餐' : '储值套餐' }}</span>
                <h3 class="card-title">{{ p.name }}</h3>
                <p class="card-desc">{{ p.description || '套餐明细以门店说明为准' }}</p>
                <div class="kv"><span>{{ t('common.valid') }}</span><span>{{ p.validDays }}{{ t('common.days') }}</span></div>
                <div class="card-foot">
                  <span class="price">{{ money(p.price) }}</span>
                  <span class="tag">{{ p.times ? p.times + t('common.times') : '储值' }}</span>
                </div>
              </div>
            </RouterLink>
          </div>
        </div>
      </section>

      <section class="section alt">
        <div class="container">
          <div class="section-head">
            <div>
              <div class="eyebrow">Stores</div>
              <h2>{{ t('home.stores') }}</h2>
              <p>选择离你最近的门店</p>
            </div>
            <RouterLink to="/stores" class="btn btn-ghost btn-sm">{{ t('common.more') }}</RouterLink>
          </div>
          <div class="store-list">
            <div v-for="s in (data.stores || []).slice(0, 3)" :key="s.id" class="store-card">
              <h3>{{ s.name }}</h3>
              <p class="muted small">{{ s.description || '专业门店，欢迎光临' }}</p>
              <div class="store-line"><b>{{ t('stores.address') }}</b><span>{{ s.address }}</span></div>
              <div class="store-line"><b>{{ t('stores.phone') }}</b><span>{{ s.phone }}</span></div>
              <RouterLink :to="`/stores/${s.id}`" class="btn btn-ghost btn-sm">{{ t('common.more') }}</RouterLink>
            </div>
          </div>
        </div>
      </section>

      <section class="section" v-if="(data.articles || []).length">
        <div class="container">
          <div class="section-head">
            <div>
              <div class="eyebrow">News</div>
              <h2>{{ t('home.news') }}</h2>
            </div>
            <RouterLink to="/news" class="btn btn-ghost btn-sm">{{ t('common.more') }}</RouterLink>
          </div>
          <div class="article-list">
            <RouterLink v-for="a in (data.articles || []).slice(0, 3)" :key="a.id" class="article-card" :to="`/news/${a.id}`">
              <div class="article-date">{{ (a.publishedAt || a.createdAt || '').slice(0, 10) }}</div>
              <h3 class="card-title">{{ a.title }}</h3>
              <p class="card-desc">{{ a.summary || '点击查看详情' }}</p>
            </RouterLink>
          </div>
        </div>
      </section>

      <section class="section alt">
        <div class="container">
          <div class="cta-band">
            <div>
              <h2>{{ t('share.title') }}</h2>
              <p>{{ t('share.desc') }}</p>
            </div>
            <RouterLink to="/share" class="btn btn-primary">{{ t('share.query') }}</RouterLink>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>
