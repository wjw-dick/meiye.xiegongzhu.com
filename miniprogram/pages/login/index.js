const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    tenantCode: '5MY',
    phone: '',
    password: '',
    loading: false,
    wxLoading: false,
    error: ''
  },

  onLoad(options) {
    const app = getApp()
    this.setData({ tenantCode: options.tenant || app.globalData.tenantCode || '5MY' })
    if (app.globalData.token) wx.reLaunch({ url: '/pages/index/index' })
  },

  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value, error: '' })
  },

  wxLogin() {
    const app = getApp()
    const that = this
    if (!this.data.tenantCode) return this.setData({ error: '请填写门店编码' })
    this.setData({ wxLoading: true, error: '' })
    wx.login({
      success(res) {
        if (!res.code) {
          that.setData({ wxLoading: false, error: '微信授权失败，请使用手机号登录' })
          return
        }
        api.post('/api/auth/wechat/MINI', {
          tenantCode: that.data.tenantCode.trim().toUpperCase(),
          code: res.code,
          nickname: '微信用户'
        }).then((data) => {
          app.setAuth(data.token, that.data.tenantCode.trim().toUpperCase(), data.user)
          util.toast('登录成功', 'success')
          wx.reLaunch({ url: '/pages/index/index' })
        }).catch((e) => {
          that.setData({ error: '微信登录未配置或失败：' + e.message + '，请使用手机号登录' })
        }).finally(() => that.setData({ wxLoading: false }))
      },
      fail() { that.setData({ wxLoading: false, error: '微信授权失败' }) }
    })
  },

  login() {
    const app = getApp()
    const { tenantCode, phone, password } = this.data
    if (!tenantCode || !phone || !password) return this.setData({ error: '请填写门店编码、手机号和密码' })
    this.setData({ loading: true, error: '' })
    api.post('/api/auth/login', {
      username: phone.trim(), password, tenantCode: tenantCode.trim().toUpperCase(), userType: 'CUSTOMER'
    }).then((data) => {
      app.setAuth(data.token, tenantCode.trim().toUpperCase(), data.user)
      util.toast('登录成功', 'success')
      wx.reLaunch({ url: '/pages/index/index' })
    }).catch((e) => this.setData({ error: e.message }))
      .finally(() => this.setData({ loading: false }))
  },

  demo() {
    this.setData({ tenantCode: '5MY', phone: '13700000001', password: '123456', error: '' })
  }
})
