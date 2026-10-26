import { tenantCode } from './tenant'

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export async function api<T = any>(path: string): Promise<T> {
  const res = await fetch(path, { headers: { 'Accept': 'application/json' } })
  const body = (await res.json()) as ApiResult<T>
  if (!res.ok || body.code !== 200) throw new Error(body.msg || '请求失败')
  return body.data
}

export const portal = {
  home: () => api(`/api/public/portal/${tenantCode}/home`),
  items: (bizType?: string) => api(`/api/public/portal/${tenantCode}/items${bizType ? '?bizType=' + bizType : ''}`),
  item: (id: number | string) => api(`/api/public/portal/${tenantCode}/items/${id}`),
  packages: () => api(`/api/public/portal/${tenantCode}/packages`),
  pkg: (id: number | string) => api(`/api/public/portal/${tenantCode}/packages/${id}`),
  stores: () => api(`/api/public/portal/${tenantCode}/stores`),
  store: (id: number | string) => api(`/api/public/portal/${tenantCode}/stores/${id}`),
  articles: (type = 'NOTICE') => api(`/api/public/portal/${tenantCode}/articles?type=${type}`),
  article: (id: number | string) => api(`/api/public/portal/${tenantCode}/articles/${id}`),
  technicians: (storeId?: number) => api(`/api/public/portal/${tenantCode}/technicians${storeId ? '?storeId=' + storeId : ''}`),
  slots: (storeId: number, date: string, itemId?: number) =>
    api(`/api/public/portal/${tenantCode}/slots?storeId=${storeId}&date=${date}${itemId ? '&itemId=' + itemId : ''}`),
  shareOrder: (orderNo: string, phoneLast4: string) =>
    api(`/api/public/portal/${tenantCode}/share/order?orderNo=${encodeURIComponent(orderNo)}&phoneLast4=${encodeURIComponent(phoneLast4)}`)
}

export function money(v: number | string | null | undefined): string {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toFixed(2)
}

export function dateText(v?: string | null): string {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}
