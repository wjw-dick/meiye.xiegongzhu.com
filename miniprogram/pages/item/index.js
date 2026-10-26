const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: { item: {}, techs: [], stores: [], loading: true, error: '' },

  onLoad(options) {
    if (!util.ensureLogin()) return
    this.id = options.id
    api.get('/api/public/portal/' + getApp().globalData.tenantCode + '/items/' + options.id)
      .then((d) => this.setData({ item: d.item || {}, techs: d.technicians || [], stores: d.stores || [], loading: false }))
      .catch((e) => this.setData({ loading: false, error: e.message }))
  },

  book() {
    wx.switchTab({ url: '/pages/appointment/index' })
  }
})
