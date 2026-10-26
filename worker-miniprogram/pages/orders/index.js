const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: { tab: '', list: [], loading: false },

  onShow() {
    if (!util.ensureLogin()) return
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 2, list: getApp().globalData.tabBar })
    }
    this.load()
  },

  load() {
    this.setData({ loading: true })
    api.get('/api/worker/orders/page', { page: 1, size: 50, status: this.data.tab })
      .then((p) => this.setData({ list: p.records || [], loading: false }))
      .catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  switchTab(e) { this.setData({ tab: e.currentTarget.dataset.tab }, this.load) },

  verify(e) {
    const id = e.currentTarget.dataset.id
    util.confirm('确认核销该订单？').then((ok) => {
      if (!ok) return
      api.post('/api/worker/orders/' + id + '/verify')
        .then(() => { util.toast('核销成功', 'success'); this.load() })
        .catch((err) => util.toast(err.message))
    })
  }
})
