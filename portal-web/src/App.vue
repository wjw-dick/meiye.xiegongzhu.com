<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from './i18n'
import { loadSite, site, storeName, servicePhone } from './site'
import { portal } from './api'

const { t, lang, setLang } = useI18n()
const route = useRoute()
const menuOpen = ref(false)
const logoError = ref(false)

const nav = computed(() => [
  { to: '/', label: t('nav.home') },
  { to: '/items', label: t('nav.items') },
  { to: '/packages', label: t('nav.packages') },
  { to: '/stores', label: t('nav.stores') },
  { to: '/news', label: t('nav.news') },
  { to: '/share', label: t('nav.share') }
])

const bookingUrl = computed(() => {
  const base = import.meta.env.VITE_MP_H5_URL || 'http://127.0.0.1:5175'
  return `${base}/#/login?tenant=${site.tenant?.code || '5MY'}`
})

onMounted(async () => {
  const q = new URLSearchParams(location.search).get('lang')
  if (q === 'en' || q === 'zh') setLang(q)
  try { await loadSite() } catch { /* 首页会重试并提示 */ }
})

watch(() => route.fullPath, () => { menuOpen.value = false })
</script>

<template>
  <div class="app-shell">
    <header class="site-header">
      <div class="container header-inner">
        <RouterLink to="/" class="brand">
          <img v-if="site.config?.logo && !logoError" :src="site.config.logo" alt="logo" class="brand-logo" @error="logoError = true" />
          <span v-else class="brand-mark">美</span>
          <span class="brand-name">{{ storeName() }}</span>
        </RouterLink>
        <nav class="desktop-nav">
          <RouterLink v-for="n in nav" :key="n.to" :to="n.to" class="nav-link">{{ n.label }}</RouterLink>
        </nav>
        <div class="header-actions">
          <div class="lang-switch">
            <button :class="{ active: lang === 'zh' }" @click="setLang('zh')">中</button>
            <button :class="{ active: lang === 'en' }" @click="setLang('en')">EN</button>
          </div>
          <a class="btn btn-primary btn-sm desktop-only" :href="bookingUrl">{{ t('nav.booking') }}</a>
          <button class="menu-toggle" aria-label="menu" @click="menuOpen = !menuOpen">☰</button>
        </div>
      </div>
      <transition name="fade">
        <nav v-if="menuOpen" class="mobile-nav">
          <RouterLink v-for="n in nav" :key="n.to" :to="n.to">{{ n.label }}</RouterLink>
          <a :href="bookingUrl" class="btn btn-primary">{{ t('nav.booking') }}</a>
        </nav>
      </transition>
    </header>

    <main class="site-main">
      <RouterView />
    </main>

    <footer class="site-footer">
      <div class="container footer-grid">
        <div>
          <h4>{{ storeName() }}</h4>
          <p class="muted">{{ site.tenant?.remark || '专业美业与大健康服务，安心之选。' }}</p>
          <p class="muted" v-if="servicePhone()">客服电话：{{ servicePhone() }}</p>
        </div>
        <div>
          <h5>{{ t('nav.items') }}</h5>
          <RouterLink to="/items">{{ t('items.all') }}</RouterLink>
          <RouterLink to="/packages">{{ t('nav.packages') }}</RouterLink>
          <RouterLink to="/stores">{{ t('nav.stores') }}</RouterLink>
        </div>
        <div>
          <h5>{{ t('nav.share') }}</h5>
          <RouterLink to="/share">{{ t('nav.share') }}</RouterLink>
          <a :href="bookingUrl">{{ t('nav.booking') }}</a>
        </div>
      </div>
      <div class="container copyright">
        © {{ new Date().getFullYear() }} {{ storeName() }} · {{ t('footer.rights') }} · Powered by {{ t('footer.powered') }}
      </div>
    </footer>

    <a class="floating-book" :href="bookingUrl">{{ t('nav.booking') }}</a>
  </div>
</template>
