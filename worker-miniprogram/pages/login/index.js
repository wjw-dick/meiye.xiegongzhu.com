const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: { tenantCode: '5MY', phone: '', password: '', loading: false, error: '' },

  onLoad(options) {
    const app = getApp()
    this.setData({ tenantCode: options.tenant || app.globalData.tenantCode || '5MY' })
    if (app.globalData.token) wx.reLaunch({ url: '/pages/index/index' })
  },

  onInput(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value, error: '' }) },

  login() {
    const app = getApp()
    const { tenantCode, phone, password } = this.data
    if (!tenantCode || !phone || !password) return this.setData({ error: '请填写门店编码、手机号和密码' })
    this.setData({ loading: true, error: '' })
    api.post('/api/auth/login', {
      username: phone.trim(), password, tenantCode: tenantCode.trim().toUpperCase(), userType: 'TECHNICIAN'
    }).then((data) => {
      app.setAuth(data.token, tenantCode.trim().toUpperCase(), data.user)
      util.toast('登录成功', 'success')
      wx.reLaunch({ url: '/pages/index/index' })
    }).catch((e) => this.setData({ error: e.message }))
      .finally(() => this.setData({ loading: false }))
  },

  demo() { this.setData({ tenantCode: '5MY', phone: '13900000001', password: '123456', error: '' }) }
})
