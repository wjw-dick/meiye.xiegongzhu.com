import dayjs from 'dayjs'

export function money(v: any, digits = 2) {
  const n = Number(v || 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: digits, maximumFractionDigits: digits })
}

export function fmt(v: any, pattern = 'YYYY-MM-DD HH:mm') {
  if (!v) return '-'
  const d = dayjs(v)
  return d.isValid() ? d.format(pattern) : String(v)
}

export function dateOnly(v: any) {
  return fmt(v, 'YYYY-MM-DD')
}

export function maskPhone(v: any) {
  if (!v) return '-'
  const s = String(v)
  return s.length >= 7 ? `${s.slice(0, 3)}****${s.slice(-4)}` : s
}

export function maskIdCard(v: any) {
  if (!v) return '-'
  const s = String(v)
  return s.length > 8 ? `${s.slice(0, 4)}**********${s.slice(-4)}` : s
}

export function statusText(status: any): string {
  const map: Record<string, string> = {
    PENDING: '待处理', CONFIRMED: '已确认', ARRIVED: '已到店', SERVING: '服务中', DONE: '已完成',
    CANCELLED: '已取消', NO_SHOW: '未到店', PAID: '已支付', VERIFIED: '已核销', REFUNDED: '已退款',
    ACTIVE: '正常', EXPIRED: '已过期', FROZEN: '已冻结', USED: '已使用', UNUSED: '未使用',
    TRIAL: '试用中', NORMAL: '正常运营', DISABLED: '已停用', DRAFT: '草稿', APPLIED: '待审核',
    APPROVED: '已审核', REJECTED: '已驳回', SETTLED: '已结算', UNPAID: '未结清', PARTIAL: '部分还款',
    FAILED: '失败', SUCCESS: '成功', SENT: '已发送', READ: '已读'
  }
  return map[String(status)] ?? String(status ?? '-')
}

export function statusTag(status: any): 'success' | 'warning' | 'danger' | 'info' | 'primary' {
  const v = String(status)
  if (['PAID', 'DONE', 'VERIFIED', 'ACTIVE', 'NORMAL', 'SUCCESS', 'SENT', 'SETTLED', 'APPROVED'].includes(v)) return 'success'
  if (['PENDING', 'CONFIRMED', 'ARRIVED', 'SERVING', 'TRIAL', 'DRAFT', 'APPLIED', 'UNPAID', 'PARTIAL', 'UNUSED'].includes(v)) return 'warning'
  if (['CANCELLED', 'NO_SHOW', 'EXPIRED', 'DISABLED', 'REJECTED', 'FAILED', 'REFUNDED'].includes(v)) return 'danger'
  return 'info'
}

export function bizTypeText(code: any) {
  const map: Record<string, string> = {
    BEAUTY: '美容美体', SPA: 'SPA 水疗', HAIR: '美发造型', NAIL: '美甲美睫',
    MEDICAL: '医疗美容', HEALTH: '养生调理', SLIM: '纤体塑形', TATTOO: '纹绣'
  }
  return map[String(code)] ?? String(code ?? '-')
}

export function download(url: string, params?: Record<string, any>) {
  const token = localStorage.getItem('meiye_token') || ''
  const drill = localStorage.getItem('meiye_drill_tenant') || ''
  const qs = new URLSearchParams()
  Object.entries(params || {}).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== '') qs.append(k, String(v))
  })
  const base = `${url}${url.includes('?') ? '&' : '?'}${qs.toString()}`
  fetch(base, { headers: { Authorization: `Bearer ${token}`, 'X-Tenant-Id': drill } })
    .then(async (res) => {
      if (!res.ok) {
        const text = await res.text()
        let msg = text
        try { msg = JSON.parse(text).msg || text } catch (e) { /* ignore */ }
        throw new Error(msg || '下载失败')
      }
      const blob = await res.blob()
      const cd = res.headers.get('Content-Disposition') || ''
      const match = cd.match(/filename\*?=(?:UTF-8'')?"?([^";]+)"?/i)
      const name = match ? decodeURIComponent(match[1]) : 'download'
      const a = document.createElement('a')
      a.href = URL.createObjectURL(blob)
      a.download = name
      a.click()
      URL.revokeObjectURL(a.href)
    })
    .catch((e) => {
      alert(e.message || '下载失败')
    })
}
