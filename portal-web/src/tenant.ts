const params = new URLSearchParams(location.search)
const host = location.hostname.split('.')[0] || ''
const fromHost = /^[0-9a-z]{2,8}$/i.test(host) && !['www', 'localhost', '127'].includes(host) ? host.toUpperCase() : ''
export const tenantCode: string =
  (params.get('tenant') || localStorage.getItem('portal_tenant') || fromHost || import.meta.env.VITE_DEFAULT_TENANT || '5MY').toUpperCase()

if (params.get('tenant')) localStorage.setItem('portal_tenant', tenantCode)

export function withTenant(path: string): string {
  const sep = path.includes('?') ? '&' : '?'
  return path + sep + 'tenant=' + encodeURIComponent(tenantCode)
}
