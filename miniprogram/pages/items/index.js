const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'item',
    items: [],
    packages: [],
    filtered: [],
    types: [],
    active: '',
    loading: true
  },

  onLoad(options) {
    if (!util.ensureLogin()) return
    this.load(options.bizType || '')
  },

  load(bizType) {
    this.setData({ loading: true })
    Promise.all([
      api.get('/api/customer/items'),
      api.get('/api/customer/packages')
    ]).then(([items, packages]) => {
      const types = []
      items.forEach((i) => { if (i.bizType && types.indexOf(i.bizType) < 0) types.push(i.bizType) })
      this.setData({ items, packages, types, active: bizType, loading: false }, this.filter)
    }).catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  filter() {
    const { items, active, tab } = this.data
    this.setData({ filtered: tab === 'item' ? (active ? items.filter((i) => i.bizType === active) : items) : [] })
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab }, this.filter)
  },

  setType(e) {
    this.setData({ active: e.currentTarget.dataset.type }, this.filter)
  },

  goItem(e) {
    wx.navigateTo({ url: '/pages/item/index?id=' + e.currentTarget.dataset.id })
  },

  book(e) {
    wx.switchTab({ url: '/pages/appointment/index' })
  }
})
