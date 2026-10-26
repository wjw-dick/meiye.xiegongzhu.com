import { reactive, computed } from 'vue'

type Lang = 'zh' | 'en'

const messages: Record<string, Record<Lang, string>> = {
  'nav.home': { zh: '首页', en: 'Home' },
  'nav.items': { zh: '服务项目', en: 'Services' },
  'nav.packages': { zh: '优惠套餐', en: 'Packages' },
  'nav.stores': { zh: '门店', en: 'Stores' },
  'nav.news': { zh: '资讯', en: 'News' },
  'nav.share': { zh: '订单查询', en: 'Order Lookup' },
  'nav.booking': { zh: '在线预约', en: 'Book Now' },
  'common.book': { zh: '立即预约', en: 'Book' },
  'common.more': { zh: '查看全部', en: 'View all' },
  'common.back': { zh: '返回', en: 'Back' },
  'common.loading': { zh: '加载中…', en: 'Loading…' },
  'common.empty': { zh: '暂无数据', en: 'No data' },
  'common.minutes': { zh: '分钟', en: 'min' },
  'common.times': { zh: '次', en: 'times' },
  'common.valid': { zh: '有效期', en: 'Valid' },
  'common.days': { zh: '天', en: 'days' },
  'home.heroTag': { zh: '专业 · 安心 · 有温度', en: 'Professional · Trusted · Caring' },
  'home.heroTitle': { zh: '遇见更好的自己', en: 'Meet a better you' },
  'home.heroSub': { zh: '面部护理、皮肤管理、美发造型、SPA 芳疗、口腔与轻医美，一站预约。', en: 'Facials, skin care, hair styling, spa, dental and aesthetic care — book in one place.' },
  'home.featured': { zh: '臻选项目', en: 'Featured services' },
  'home.packages': { zh: '超值套餐', en: 'Value packages' },
  'home.stores': { zh: '附近门店', en: 'Our stores' },
  'home.news': { zh: '品牌动态', en: 'Latest news' },
  'home.biz': { zh: '服务品类', en: 'Categories' },
  'items.title': { zh: '服务项目', en: 'Services' },
  'items.all': { zh: '全部', en: 'All' },
  'item.duration': { zh: '服务时长', en: 'Duration' },
  'item.techs': { zh: '服务技师', en: 'Technicians' },
  'item.stores': { zh: '可服务门店', en: 'Available stores' },
  'packages.title': { zh: '优惠套餐', en: 'Packages' },
  'stores.title': { zh: '门店信息', en: 'Stores' },
  'stores.address': { zh: '地址', en: 'Address' },
  'stores.phone': { zh: '电话', en: 'Phone' },
  'news.title': { zh: '资讯中心', en: 'News' },
  'share.title': { zh: '订单进度查询', en: 'Order progress lookup' },
  'share.desc': { zh: '输入订单号与手机号后四位，查询服务进度（仅展示脱敏信息）。', en: 'Enter order number and last 4 digits of phone to check progress (masked).' },
  'share.orderNo': { zh: '订单号', en: 'Order No.' },
  'share.last4': { zh: '手机后四位', en: 'Phone last 4' },
  'share.query': { zh: '查询', en: 'Search' },
  'footer.rights': { zh: '保留所有权利', en: 'All rights reserved' },
  'footer.powered': { zh: '美业大健康数字化平台', en: 'Beauty & Wellness SaaS' }
}

const state = reactive<{ lang: Lang }>({
  lang: (localStorage.getItem('portal_lang') as Lang) || (navigator.language.startsWith('en') ? 'en' : 'zh')
})

export function useI18n() {
  const t = (key: string) => messages[key]?.[state.lang] ?? key
  const lang = computed(() => state.lang)
  const setLang = (l: Lang) => {
    state.lang = l
    localStorage.setItem('portal_lang', l)
    document.documentElement.lang = l === 'zh' ? 'zh-CN' : 'en'
  }
  return { t, lang, setLang }
}

export const bizTypeName: Record<string, { zh: string; en: string }> = {
  BEAUTY: { zh: '美容', en: 'Beauty' },
  SKIN: { zh: '皮肤管理', en: 'Skin care' },
  HAIR: { zh: '美发', en: 'Hair' },
  SPA: { zh: 'SPA 芳疗', en: 'Spa' },
  NAIL: { zh: '美甲美睫', en: 'Nails & Lashes' },
  DENTAL: { zh: '口腔', en: 'Dental' },
  MEDBEAUTY: { zh: '轻医美', en: 'Aesthetics' },
  HEALTH: { zh: '大健康', en: 'Wellness' }
}

export function bizName(code?: string): string {
  if (!code) return ''
  const n = bizTypeName[code]
  return n ? n[state.lang] : code
}
