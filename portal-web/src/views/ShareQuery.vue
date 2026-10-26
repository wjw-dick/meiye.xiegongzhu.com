<script setup lang="ts">
import { ref } from 'vue'
import { portal, money } from '../api'
import { useI18n } from '../i18n'
import { setSeo } from '../seo'
import { tenantCode } from '../tenant'

const { t } = useI18n()
const orderNo = ref('')
const last4 = ref('')
const loading = ref(false)
const error = ref('')
const result = ref<any>(null)

setSeo({ title: t('share.title'), description: t('share.desc') })

async function query() {
  error.value = ''
  result.value = null
  if (!orderNo.value.trim() || !/^\d{4}$/.test(last4.value)) {
    error.value = '请输入订单号和手机号后四位'
    return
  }
  loading.value = true
  try {
    result.value = await portal.shareOrder(orderNo.value.trim(), last4.value)
  } catch (e: any) {
    error.value = e.message || '查询失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <section class="page-hero">
      <div class="container">
        <div class="eyebrow">Order Lookup</div>
        <h1>{{ t('share.title') }}</h1>
        <p>{{ t('share.desc') }}</p>
      </div>
    </section>
    <div class="page-body">
      <div class="container" style="max-width:560px">
        <div class="panel">
          <div class="form-grid">
            <div class="field">
              <label>{{ t('share.orderNo') }}</label>
              <input v-model="orderNo" placeholder="如 5MY-ORD-2026-0001" @keyup.enter="query" />
            </div>
            <div class="field">
              <label>{{ t('share.last4') }}</label>
              <input v-model="last4" maxlength="4" inputmode="numeric" placeholder="0000" @keyup.enter="query" />
            </div>
            <button class="btn btn-primary" :disabled="loading" @click="query">{{ loading ? '查询中…' : t('share.query') }}</button>
          </div>
          <div v-if="error" class="alert error">{{ error }}</div>
          <div v-if="result" class="alert info" style="margin-top:20px">
            <div class="kv"><span>订单号</span><span>{{ result.orderNo }}</span></div>
            <div class="kv"><span>客户</span><span>{{ result.customerName }}</span></div>
            <div class="kv"><span>手机</span><span>{{ result.phone }}</span></div>
            <div class="kv" v-for="(v, k) in result" :key="k" v-show="!['orderNo','customerName','phone'].includes(String(k))">
              <span>{{ k }}</span><span>{{ v }}</span>
            </div>
          </div>
          <p class="muted small" style="margin-top:16px">当前门店编码：{{ tenantCode }}。为保护隐私，仅展示脱敏后的订单进度信息。</p>
        </div>
      </div>
    </div>
  </div>
</template>
