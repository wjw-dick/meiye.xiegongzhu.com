const DEFAULT_TABS = [
  { pagePath: '/pages/index/index', text: '首页', icon: '⌂' },
  { pagePath: '/pages/appointment/index', text: '预约', icon: '✚' },
  { pagePath: '/pages/order/index', text: '订单', icon: '☰' },
  { pagePath: '/pages/mine/index', text: '我的', icon: '☺' }
]

App({
  globalData: {
    baseUrl: 'http://127.0.0.1:8081',
    tenantCode: '5MY',
    token: '',
    user: null,
    profile: null,
    tabBar: DEFAULT_TABS,
    lang: 'zh'
  },

  onLaunch() {
    const token = wx.getStorageSync('token')
    const tenantCode = wx.getStorageSync('tenantCode')
    const user = wx.getStorageSync('user')
    const lang = wx.getStorageSync('lang')
    if (token) this.globalData.token = token
    if (tenantCode) this.globalData.tenantCode = tenantCode
    if (user) this.globalData.user = user
    if (lang) this.globalData.lang = lang
    this.globalData.tabBar = wx.getStorageSync('tabBar') || DEFAULT_TABS
    this.initTabBar()
  },

  initTabBar() {
    if (!this.globalData.token) return
    const api = require('./utils/api')
    api.get('/api/menus', { target: 'MINIAPP' }).then((mc) => {
      if (!mc || !mc.content) return
      try {
        const list = JSON.parse(mc.content)
        if (Array.isArray(list) && list.length) {
          const tabs = list.map((t, i) => ({
            pagePath: t.pagePath,
            text: t.text || t.title || '菜单',
            icon: t.icon || ['⌂', '✚', '☰', '☺', '·'][i] || '·'
          }))
          this.globalData.tabBar = tabs
          wx.setStorageSync('tabBar', tabs)
          const page = getCurrentPages().slice(-1)[0]
          if (page && typeof page.getTabBar === 'function') {
            const bar = page.getTabBar()
            if (bar) bar.setData({ list: tabs, selected: bar.data.selected })
          }
        }
      } catch (e) { /* keep default */ }
    }).catch(() => {})
  },

  setAuth(token, tenantCode, user) {
    this.globalData.token = token
    this.globalData.tenantCode = tenantCode
    this.globalData.user = user
    wx.setStorageSync('token', token)
    wx.setStorageSync('tenantCode', tenantCode)
    wx.setStorageSync('user', user)
    this.initTabBar()
  },

  clearAuth() {
    this.globalData.token = ''
    this.globalData.user = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('user')
  }
})
