const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    loading: true,
    error: '',
    customer: {},
    cards: [],
    coupons: [],
    appointments: [],
    items: [],
    stores: [],
    notices: [],
    quick: [
      { label: '在线预约', icon: '✚', url: '/pages/appointment/index' },
      { label: '我的订单', icon: '☰', url: '/pages/order/index' },
      { label: '会员卡包', icon: '▣', url: '/pages/cards/index' },
      { label: '优惠券', icon: '◈', url: '/pages/mine/index?tab=coupon' },
      { label: '积分', icon: '✦', url: '/pages/mine/index?tab=point' },
      { label: '消息', icon: '✉', url: '/pages/messages/index' },
      { label: '评价', icon: '★', url: '/pages/order/index?tab=review' },
      { label: '售后', icon: '↺', url: '/pages/order/index?tab=aftersale' }
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
    this.setData({ loading: true, error: '' })
    api.get('/api/customer/home').then((d) => {
      this.setData({
        customer: d.customer || {},
        cards: d.cards || [],
        coupons: d.coupons || [],
        appointments: d.appointments || [],
        items: d.items || [],
        stores: d.stores || [],
        notices: d.notices || [],
        loading: false
      })
    }).catch((e) => this.setData({ loading: false, error: e.message }))
  },

  go(e) {
    const url = e.currentTarget.dataset.url
    if (url.indexOf('/pages/appointment/index') === 0) wx.switchTab({ url })
    else wx.navigateTo({ url })
  },

  goItem(e) {
    wx.navigateTo({ url: '/pages/item/index?id=' + e.currentTarget.dataset.id })
  },

  goMine() { wx.switchTab({ url: '/pages/mine/index' }) },

  goOrders() { wx.switchTab({ url: '/pages/order/index' }) },

  onPullDownRefresh() {
    this.load()
    wx.stopPullDownRefresh()
  }
})
