const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    dash: {},
    profile: {},
    income: {},
    tech: {},
    loading: true,
    quick: [
      { label: '我的排班', icon: '◷', url: '/pages/performance/index?tab=schedule' },
      { label: '我的预约', icon: '☰', url: '/pages/appointments/index' },
      { label: '订单核销', icon: '▤', url: '/pages/orders/index' },
      { label: '业绩提成', icon: '↗', url: '/pages/performance/index' },
      { label: '服务日志', icon: '✎', url: '/pages/logs/index' },
      { label: '客户评价', icon: '★', url: '/pages/mine/index?tab=review' },
      { label: '资质证书', icon: '✓', url: '/pages/mine/index?tab=cert' },
      { label: '消息', icon: '✉', url: '/pages/messages/index' }
    ]
  },

  onShow() {
    if (!util.ensureLogin()) return
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0, list: getApp().globalData.tabBar })
    }
    this.load()
  },

  load() {
    this.setData({ loading: true })
    Promise.all([api.get('/api/worker/dashboard'), api.get('/api/worker/profile'), api.get('/api/worker/performance')])
      .then(([dash, profile, perf]) => {
        this.setData({
          dash, profile, income: perf.income || {}, tech: profile.technician || {},
          loading: false
        })
      }).catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  go(e) {
    const url = e.currentTarget.dataset.url
    if (url.indexOf('/pages/appointments/index') === 0 || url.indexOf('/pages/orders/index') === 0) wx.switchTab({ url })
    else wx.navigateTo({ url })
  }
})
