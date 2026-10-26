const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'order',
    orders: [],
    loading: true,
    techs: [],
    techIndex: 0,
    score: 5,
    content: '',
    reviews: [],
    aftersales: [],
    asOrders: [],
    asOrderIndex: 0,
    asType: 'REFUND',
    asReason: '',
    submitting: false
  },

  onLoad(options) {
    if (!util.ensureLogin()) return
    if (options.tab) this.setData({ tab: options.tab })
    this.loadOrders()
    this.loadOthers()
  },

  onShow() {
    if (!util.ensureLogin()) return
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2, list: getApp().globalData.tabBar })
    }
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    if (e.currentTarget.dataset.tab === 'review') this.loadOthers()
    if (e.currentTarget.dataset.tab === 'aftersale') this.loadOthers()
  },

  loadOrders() {
    this.setData({ loading: true })
    api.get('/api/customer/orders/page', { page: 1, size: 50 })
      .then((p) => this.setData({ orders: (p.records || []).map((r) => r.order || r), loading: false }))
      .catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  loadOthers() {
    api.get('/api/customer/technicians').then((techs) => this.setData({ techs })).catch(() => {})
    api.get('/api/customer/reviews').then((reviews) => this.setData({ reviews: reviews || [] })).catch(() => {})
    api.get('/api/customer/after-sales').then((list) => this.setData({ aftersales: list || [] })).catch(() => {})
    api.get('/api/customer/orders/page', { page: 1, size: 30 })
      .then((p) => this.setData({ asOrders: (p.records || []).map((r) => r.order || r) }))
      .catch(() => {})
  },

  pay(e) {
    const id = e.currentTarget.dataset.id
    util.confirm('确认支付该订单？').then((ok) => {
      if (!ok) return
      api.post('/api/customer/orders/' + id + '/pay', { payType: 'WECHAT' })
        .then(() => { util.toast('支付成功', 'success'); this.loadOrders() })
        .catch((err) => util.toast(err.message))
    })
  },

  copy(e) {
    wx.setClipboardData({ data: e.currentTarget.dataset.text })
  },

  onTech(e) { this.setData({ techIndex: Number(e.detail.value) }) },
  onScore(e) { this.setData({ score: Number(e.currentTarget.dataset.score) }) },
  onContent(e) { this.setData({ content: e.detail.value }) },
  onAsType(e) { this.setData({ asType: e.currentTarget.dataset.type }) },
  onAsOrder(e) { this.setData({ asOrderIndex: Number(e.detail.value) }) },
  onAsReason(e) { this.setData({ asReason: e.detail.value }) },

  submitReview() {
    const { techs, techIndex, score, content } = this.data
    if (!content) return util.toast('请填写评价内容')
    this.setData({ submitting: true })
    api.post('/api/customer/reviews', { technicianId: techs[techIndex] ? techs[techIndex].id : null, score, content })
      .then(() => { util.toast('评价成功', 'success'); this.setData({ content: '', submitting: false }); this.loadOthers() })
      .catch((e) => { util.toast(e.message); this.setData({ submitting: false }) })
  },

  submitAfterSale() {
    const { asOrders, asOrderIndex, asType, asReason } = this.data
    if (!asReason) return util.toast('请填写申请原因')
    this.setData({ submitting: true })
    api.post('/api/customer/after-sales', { orderId: asOrders[asOrderIndex] ? asOrders[asOrderIndex].id : null, type: asType, reason: asReason })
      .then(() => { util.toast('申请已提交', 'success'); this.setData({ asReason: '', submitting: false }); this.loadOthers() })
      .catch((e) => { util.toast(e.message); this.setData({ submitting: false }) })
  }
})
