const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: { tab: '', date: '', list: [], loading: false },

  onShow() {
    if (!util.ensureLogin()) return
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1, list: getApp().globalData.tabBar })
    }
    this.load()
  },

  load() {
    this.setData({ loading: true })
    api.get('/api/worker/appointments/page', { page: 1, size: 50, status: this.data.tab, date: this.data.date })
      .then((p) => this.setData({ list: p.records || [], loading: false }))
      .catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  switchTab(e) { this.setData({ tab: e.currentTarget.dataset.tab }, this.load) },
  onDate(e) { this.setData({ date: e.detail.value }, this.load) },

  act(e) {
    const { id, action, label } = e.currentTarget.dataset
    util.confirm('确认' + label + '？').then((ok) => {
      if (!ok) return
      api.post('/api/worker/appointments/' + id + '/' + action)
        .then(() => { util.toast(label + '成功', 'success'); this.load() })
        .catch((err) => util.toast(err.message))
    })
  },

  goLog(e) {
    wx.navigateTo({ url: '/pages/logs/index?appointmentId=' + e.currentTarget.dataset.id })
  }
})
