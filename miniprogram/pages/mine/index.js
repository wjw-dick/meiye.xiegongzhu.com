const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'menu',
    customer: {},
    cards: [],
    coupons: [],
    points: { balance: 0, records: [] },
    lang: 'zh',
    tenantCode: '5MY',
    agreements: { show: false, type: 'privacy', content: '' }
  },

  onLoad(options) {
    if (!util.ensureLogin()) return
    if (options.tab) this.setData({ tab: options.tab })
    this.setData({ tenantCode: getApp().globalData.tenantCode, lang: getApp().globalData.lang || 'zh' })
    this.load()
  },

  onShow() {
    if (!util.ensureLogin()) return
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3, list: getApp().globalData.tabBar })
    }
  },

  load() {
    api.get('/api/customer/profile').then((d) => this.setData({ customer: d.customer || {}, cards: d.cards || [] })).catch(() => {})
    api.get('/api/customer/coupons').then((coupons) => this.setData({ coupons: coupons || [] })).catch(() => {})
    api.get('/api/customer/points').then((points) => this.setData({ points: points || {} })).catch(() => {})
  },

  go(e) {
    const url = e.currentTarget.dataset.url
    if (url === '/pages/index/index' || url === '/pages/order/index') wx.switchTab({ url })
    else wx.navigateTo({ url })
  },

  setTab(e) { this.setData({ tab: e.currentTarget.dataset.tab }) },

  toggleLang() {
    const lang = this.data.lang === 'zh' ? 'en' : 'zh'
    getApp().globalData.lang = lang
    wx.setStorageSync('lang', lang)
    this.setData({ lang })
    util.toast(lang === 'zh' ? '已切换为中文' : 'Switched to English')
  },

  showAgreement(e) {
    const type = e.currentTarget.dataset.type
    const content = type === 'privacy'
      ? '隐私政策\n\n我们仅在提供预约、会员与支付服务所必需的范围内收集和使用你的个人信息，包括手机号、服务记录与消费记录。敏感信息加密存储，展示时脱敏。未经授权不会向第三方提供，法律法规另有规定的除外。你可随时联系门店查询、更正或删除个人信息。'
      : '会员服务协议\n\n1. 会员账户仅限本人使用，请妥善保管密码。\n2. 预约成功后请按时到店，迟到超过 15 分钟门店有权调整服务时间。\n3. 会员卡、优惠券以页面展示的有效期与使用规则为准，过期不予补发（法律另有规定除外）。\n4. 服务过程中如有不适请立即告知技师。\n5. 本协议未尽事宜以门店公示及法律法规为准。'
    this.setData({ agreements: { show: true, type, content } })
  },

  closeAgreement() { this.setData({ 'agreements.show': false }) },

  noop() {},

  logout() {
    util.confirm('确认退出登录？').then((ok) => {
      if (!ok) return
      getApp().clearAuth()
      wx.reLaunch({ url: '/pages/login/index' })
    })
  }
})
