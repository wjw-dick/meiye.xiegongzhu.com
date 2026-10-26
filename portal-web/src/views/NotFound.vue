<script setup lang="ts">
import { setSeo } from '../seo'
setSeo({ title: '页面不存在' })
</script>

<template>
  <div class="page-body">
    <div class="container">
      <div class="empty-state">
        <h1 style="font-size:64px;margin:0;color:var(--brand)">404</h1>
        <p>你访问的页面不存在或已下线。</p>
        <RouterLink to="/" class="btn btn-primary">返回首页</RouterLink>
      </div>
    </div>
  </div>
</template>
