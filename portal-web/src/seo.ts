interface SeoInput {
  title: string
  description?: string
  keywords?: string
  image?: string
  jsonLd?: Record<string, any>
}

const SITE = '美业大健康服务平台'

function upsertMeta(selector: string, attrs: Record<string, string>) {
  let el = document.head.querySelector(selector) as HTMLMetaElement | null
  if (!el) {
    el = document.createElement('meta')
    document.head.appendChild(el)
  }
  Object.entries(attrs).forEach(([k, v]) => el!.setAttribute(k, v))
}

export function setSeo(input: SeoInput) {
  const full = input.title.includes(SITE) ? input.title : `${input.title} | ${SITE}`
  document.title = full
  upsertMeta('meta[name="description"]', { name: 'description', content: input.description || '美业大健康数字化门店，在线预约、会员卡包、项目套餐。' })
  upsertMeta('meta[name="keywords"]', { name: 'keywords', content: input.keywords || '美容,皮肤管理,美发,SPA,预约,会员卡' })
  upsertMeta('meta[property="og:title"]', { property: 'og:title', content: full })
  upsertMeta('meta[property="og:description"]', { property: 'og:description', content: input.description || '' })
  upsertMeta('meta[property="og:type"]', { property: 'og:type', content: 'website' })
  if (input.image) upsertMeta('meta[property="og:image"]', { property: 'og:image', content: input.image })

  // canonical + hreflang
  const base = location.origin + location.pathname
  const url = new URL(location.href)
  url.searchParams.set('lang', 'zh')
  setLink('canonical', url.toString())
  const langs: Array<[string, string]> = [
    ['zh-CN', withLang(base, 'zh')],
    ['en', withLang(base, 'en')],
    ['x-default', base]
  ]
  document.head.querySelectorAll('link[rel="alternate"][data-hreflang]').forEach(n => n.remove())
  langs.forEach(([code, href]) => {
    const l = document.createElement('link')
    l.rel = 'alternate'
    l.hreflang = code
    l.href = href
    l.dataset.hreflang = '1'
    document.head.appendChild(l)
  })

  document.head.querySelectorAll('script[data-seo-jsonld]').forEach(n => n.remove())
  if (input.jsonLd) {
    const s = document.createElement('script')
    s.type = 'application/ld+json'
    s.dataset.seoJsonld = '1'
    s.textContent = JSON.stringify(input.jsonLd)
    document.head.appendChild(s)
  }
}

function withLang(base: string, lang: string): string {
  const u = new URL(base)
  u.searchParams.set('lang', lang)
  return u.toString()
}

function setLink(rel: string, href: string) {
  let el = document.head.querySelector(`link[rel="${rel}"]`) as HTMLLinkElement | null
  if (!el) {
    el = document.createElement('link')
    el.rel = rel
    document.head.appendChild(el)
  }
  el.href = href
}
