<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const showTab = computed(() => !!route.meta.tab)
const active = computed(() => route.meta.tab)
const canBack = computed(() => !route.meta.tab && route.path !== '/login')

const tabs = [
  { key: 'home', to: '/home', label: '首页', icon: '⌂' },
  { key: 'book', to: '/items', label: '预约', icon: '✚' },
  { key: 'orders', to: '/orders', label: '订单', icon: '☰' },
  { key: 'mine', to: '/profile', label: '我的', icon: '☺' }
]
</script>

<template>
  <div class="app">
    <header class="topbar" v-if="route.path !== '/login'">
      <button v-if="canBack" class="back" @click="router.back()">‹</button>
      <span class="title">{{ route.meta.title || '' }}</span>
    </header>
    <main class="page" :class="{ 'with-tab': showTab }">
      <RouterView />
    </main>
    <nav class="tabbar" v-if="showTab">
      <RouterLink v-for="t in tabs" :key="t.key" :to="t.to" class="tab" :class="{ active: active === t.key }">
        <span class="tab-icon">{{ t.icon }}</span>
        <span>{{ t.label }}</span>
      </RouterLink>
    </nav>
  </div>
</template>
