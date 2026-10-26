const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'menu',
    profile: {},
    tech: {},
    certs: [],
    reviews: [],
    agreements: { show: false, type: 'privacy', content: '' },
    tenantCode: '5MY'
  },

  onLoad(options) {
    if (!util.ensureLogin()) return
    this.setData({ tab: options.tab || 'menu', tenantCode: getApp().globalData.tenantCode })
    this.load()
  },

  onShow() {
    if (!util.ensureLogin()) return
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 3, list: getApp().globalData.tabBar })
    }
  },

  load() {
    api.get('/api/worker/profile').then((d) => this.setData({ profile: d, tech: d.technician || {}, certs: d.certs || [] })).catch(() => {})
    api.get('/api/worker/reviews').then((reviews) => this.setData({ reviews: reviews || [] })).catch(() => {})
  },

  setTab(e) { this.setData({ tab: e.currentTarget.dataset.tab }) },

  go(e) {
    const url = e.currentTarget.dataset.url
    if (url.indexOf('/pages/appointments/index') === 0 || url.indexOf('/pages/orders/index') === 0) wx.switchTab({ url })
    else wx.navigateTo({ url })
  },

  showAgreement(e) {
    const type = e.currentTarget.dataset.type
    this.setData({
      agreements: {
        show: true, type,
        content: type === 'privacy'
          ? '隐私政策\n\n我们仅收集为你提供排班、接单、结算与提现服务所必需的个人信息，包括手机号、身份证明、证书与收入记录。敏感信息加密存储，展示脱敏。未经授权不会向第三方提供。你可联系门店查询、更正个人信息。'
          : '技师服务规范\n\n1. 严格遵守门店排班，提前到岗，规范着装。\n2. 服务前确认客户需求与禁忌，规范操作流程。\n3. 妥善保管客户隐私，不得外泄客户资料。\n4. 服务完成后及时填写服务日志并确认核销。\n5. 严禁私下收款、飞单或引导客户脱离平台交易。'
      }
    })
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
