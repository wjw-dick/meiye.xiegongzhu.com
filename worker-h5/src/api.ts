export interface User {
  technicianId?: number
  tenantId?: number
  tenantCode?: string
  name?: string
  username?: string
  userType?: string
  role?: string
}

interface AuthState {
  token: string
  tenantCode: string
  user: User | null
}

const KEY = 'worker_auth'

export const auth: AuthState = (() => {
  try {
    return JSON.parse(localStorage.getItem(KEY) || '') || { token: '', tenantCode: '5MY', user: null }
  } catch {
    return { token: '', tenantCode: '5MY', user: null }
  }
})()

export function setAuth(token: string, tenantCode: string, user: User) {
  auth.token = token
  auth.tenantCode = tenantCode
  auth.user = user
  localStorage.setItem(KEY, JSON.stringify(auth))
}

export function clearAuth() {
  auth.token = ''
  auth.user = null
  localStorage.removeItem(KEY)
}

export async function api<T = any>(path: string, options: { method?: string; body?: any; query?: Record<string, any> } = {}): Promise<T> {
  let url = path
  if (options.query) {
    const q = new URLSearchParams()
    Object.entries(options.query).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') q.append(k, String(v)) })
    if ([...q].length) url += (url.includes('?') ? '&' : '?') + q.toString()
  }
  const headers: Record<string, string> = { 'Accept': 'application/json' }
  if (auth.token) headers['Authorization'] = 'Bearer ' + auth.token
  if (options.body !== undefined) headers['Content-Type'] = 'application/json'
  const res = await fetch(url, { method: options.method || 'GET', headers, body: options.body !== undefined ? JSON.stringify(options.body) : undefined })
  let body: any
  try { body = await res.json() } catch { throw new Error('网络异常，请稍后重试') }
  if (res.status === 401 || body.code === 401) {
    clearAuth()
    location.hash = '#/login'
    throw new Error(body.msg || '登录已过期')
  }
  if (!res.ok || body.code !== 200) throw new Error(body.msg || '请求失败')
  return body.data
}

export function money(v: any): string {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toFixed(2)
}

export function dt(v?: string | null): string {
  return v ? String(v).replace('T', ' ').slice(0, 16) : ''
}

export function timeOnly(v?: string | null): string {
  const s = dt(v)
  return s.length > 11 ? s.slice(11) : s
}

export const statusText: Record<string, string> = {
  PENDING: '待确认', CONFIRMED: '已确认', ARRIVED: '已到店', SERVING: '服务中', DONE: '已完成',
  NO_SHOW: '未到店', CANCELLED: '已取消', PAID: '已支付', UNPAID: '待支付', VERIFIED: '已核销',
  APPLIED: '审核中', APPROVED: '已通过', REJECTED: '已驳回', PAID_OUT: '已打款',
  VALID: '有效', EXPIRED: '已过期', WARN: '即将到期'
}
