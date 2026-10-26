<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { api, money, bizName } from '../api'

const list = ref<any[]>([])
const loading = ref(true)
const active = ref('')
const types = computed(() => Array.from(new Set(list.value.map((i: any) => i.bizType).filter(Boolean))))
const filtered = computed(() => active.value ? list.value.filter((i: any) => i.bizType === active.value) : list.value)

onMounted(async () => {
  try { list.value = await api('/api/customer/items') } finally { loading.value = false }
})
</script>

<template>
  <div>
    <div class="chips" style="padding:14px 14px 4px">
      <button class="chip" :class="{ active: !active }" @click="active = ''">全部</button>
      <button v-for="t in types" :key="t" class="chip" :class="{ active: active === t }" @click="active = t">{{ bizName[t] || t }}</button>
    </div>
    <div class="section">
      <div v-if="loading" class="loading">加载中…</div>
      <div v-else-if="!filtered.length" class="empty">暂无可预约项目</div>
      <div v-else class="list">
        <RouterLink v-for="it in filtered" :key="it.id" :to="`/booking?itemId=${it.id}`" class="item-card">
          <img v-if="it.image" :src="it.image" class="item-img" :alt="it.name" />
          <div v-else class="item-img" style="display:grid;place-items:center;color:#c99">美</div>
          <div style="flex:1;min-width:0">
            <div class="row between">
              <h4>{{ it.name }}</h4>
              <span class="pill">{{ bizName[it.bizType] || it.bizType }}</span>
            </div>
            <div class="desc">{{ it.description || it.category || '专业服务' }}</div>
            <div class="row between" style="margin-top:8px">
              <span class="price">{{ money(it.price) }}<span class="small muted" style="font-weight:400"> / {{ it.duration }}分钟</span></span>
              <span class="btn sm">预约</span>
            </div>
          </div>
        </RouterLink>
      </div>
    </div>
  </div>
</template>
