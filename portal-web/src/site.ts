import { reactive } from 'vue'
import { portal } from './api'

export const site = reactive<{
  loaded: boolean
  tenant: any
  config: any
  bizTypes: any[]
  stores: any[]
  banners: any[]
}>({ loaded: false, tenant: null, config: null, bizTypes: [], stores: [], banners: [] })

let pending: Promise<void> | null = null

export function loadSite(): Promise<void> {
  if (site.loaded) return Promise.resolve()
  if (pending) return pending
  pending = portal.home().then((d: any) => {
    site.tenant = d.tenant || null
    site.config = d.config || null
    site.bizTypes = d.bizTypes || []
    site.stores = d.stores || []
    site.banners = d.banners || []
    site.loaded = true
  }).finally(() => { pending = null })
  return pending
}

export function storeName(): string {
  return site.config?.name || site.tenant?.name || '美业大健康'
}

export function servicePhone(): string {
  return site.config?.servicePhone || site.tenant?.phone || ''
}
