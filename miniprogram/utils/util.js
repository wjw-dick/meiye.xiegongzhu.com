const STATUS = {
  PENDING: '待确认', CONFIRMED: '已确认', ARRIVED: '已到店', SERVING: '服务中', DONE: '已完成',
  NO_SHOW: '未到店', CANCELLED: '已取消', PAID: '已支付', UNPAID: '待支付', VERIFIED: '已核销',
  REFUNDED: '已退款', PARTIAL_REFUND: '部分退款', UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期',
  APPLIED: '审核中', APPROVED: '已通过', REJECTED: '已驳回', PAID_OUT: '已打款', VALID: '有效', WARN: '即将到期'
}

const BIZ = {
  BEAUTY: '美容', SKIN: '皮肤管理', HAIR: '美发', SPA: 'SPA',
  NAIL: '美甲美睫', DENTAL: '口腔', MEDBEAUTY: '轻医美', HEALTH: '大健康'
}

function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return '¥' + Number(v).toFixed(2)
}

function dt(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : ''
}

function dateOnly(v) {
  return v ? String(v).slice(0, 10) : ''
}

function toast(title, icon) {
  wx.showToast({ title: title || '', icon: icon || 'none', duration: 2200 })
}

function confirm(content, title) {
  return new Promise((resolve) => {
    wx.showModal({
      title: title || '提示', content, confirmColor: '#b4235a',
      success: (r) => resolve(!!r.confirm)
    })
  })
}

function ensureLogin() {
  const app = getApp()
  if (!app.globalData.token) {
    wx.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

module.exports = { STATUS, BIZ, money, dt, dateOnly, toast, confirm, ensureLogin }
