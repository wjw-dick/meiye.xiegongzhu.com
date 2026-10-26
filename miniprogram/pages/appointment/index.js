const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'book',
    items: [],
    packages: [],
    stores: [],
    storeIndex: 0,
    techs: [],
    techIndex: 0,
    itemIndex: 0,
    date: '',
    slots: [],
    slot: null,
    remark: '',
    payNow: false,
    loading: false,
    submitting: false,
    appointments: [],
    apptLoading: false,
    today: ''
  },

  onLoad(options) {
    if (!util.ensureLogin()) return
    const d = new Date(Date.now() + 86400000)
    const date = d.toISOString().slice(0, 10)
    const today = new Date().toISOString().slice(0, 10)
    this.setData({ date, today })
    this.pendingItemId = options.itemId || null
    this.loadBase()
  },

  onShow() {
    if (!util.ensureLogin()) return
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1, list: getApp().globalData.tabBar })
    }
    if (this.data.appointments.length === 0) this.loadAppointments()
  },

  loadBase() {
    this.setData({ loading: true })
    Promise.all([api.get('/api/customer/items'), api.get('/api/customer/packages'), api.get('/api/customer/stores')])
      .then(([items, packages, stores]) => {
        let itemIndex = 0
        if (this.pendingItemId) {
          const idx = items.findIndex((i) => String(i.id) === String(this.pendingItemId))
          if (idx >= 0) itemIndex = idx
        }
        this.setData({ items, packages, stores, itemIndex, loading: false }, () => {
          if (stores.length) this.loadTechs()
          this.loadSlots()
        })
      }).catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  loadTechs() {
    const { stores, storeIndex, items, itemIndex } = this.data
    if (!stores.length) return
    api.get('/api/customer/technicians', { storeId: stores[storeIndex].id, bizType: items[itemIndex] && items[itemIndex].bizType })
      .then((techs) => this.setData({ techs, techIndex: 0 }))
      .catch(() => this.setData({ techs: [] }))
  },

  loadSlots() {
    const { stores, storeIndex, items, itemIndex, date } = this.data
    if (!stores.length || !items.length) return
    api.get('/api/customer/slots', { storeId: stores[storeIndex].id, itemId: items[itemIndex].id, date })
      .then((slots) => this.setData({ slots, slot: null }))
      .catch(() => this.setData({ slots: [] }))
  },

  switchTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    if (e.currentTarget.dataset.tab === 'mine') this.loadAppointments()
  },

  onItem(e) { this.setData({ itemIndex: Number(e.detail.value), slot: null }, () => { this.loadTechs(); this.loadSlots() }) },
  onStore(e) { this.setData({ storeIndex: Number(e.detail.value), slot: null }, () => { this.loadTechs(); this.loadSlots() }) },
  onTech(e) { this.setData({ techIndex: Number(e.detail.value) }) },
  onDate(e) { this.setData({ date: e.detail.value }, this.loadSlots) },
  onSlot(e) { this.setData({ slot: this.data.slots[e.currentTarget.dataset.index] }) },
  onRemark(e) { this.setData({ remark: e.detail.value }) },
  onPay(e) { this.setData({ payNow: e.detail.value }) },

  submit() {
    const { items, itemIndex, stores, storeIndex, techs, techIndex, slot, remark, payNow } = this.data
    if (!slot) return util.toast('请选择预约时段')
    const item = items[itemIndex]
    const store = stores[storeIndex]
    const tech = techs[techIndex]
    this.setData({ submitting: true })
    api.post('/api/customer/appointments', {
      storeId: store.id, itemId: item.id, technicianId: tech ? tech.id : null,
      bizType: item.bizType, startTime: slot.time, remark
    }).then((appt) => {
      if (payNow && Number(item.price) > 0) {
        return api.post('/api/customer/orders', {
          storeId: store.id, technicianId: tech ? tech.id : null, appointmentId: appt.id,
          bizType: item.bizType, items: [{ itemId: item.id, quantity: 1, technicianId: tech ? tech.id : null }]
        }).then((order) => api.post('/api/customer/orders/' + order.id + '/pay', { payType: 'WECHAT' }))
          .then(() => appt)
      }
      return appt
    }).then((appt) => {
      wx.showModal({
        title: '预约成功', content: '预约号 ' + appt.appointmentNo + '，请按时到店。',
        showCancel: false, confirmColor: '#b4235a',
        success: () => this.setData({ tab: 'mine', slot: null, remark: '', payNow: false }, this.loadAppointments)
      })
    }).catch((e) => util.toast(e.message)).finally(() => this.setData({ submitting: false }))
  },

  loadAppointments() {
    this.setData({ apptLoading: true })
    api.get('/api/customer/appointments/page', { page: 1, size: 30 })
      .then((p) => this.setData({ appointments: p.records || [], apptLoading: false }))
      .catch((e) => { this.setData({ apptLoading: false }); util.toast(e.message) })
  },

  cancel(e) {
    const id = e.currentTarget.dataset.id
    util.confirm('确认取消该预约？').then((ok) => {
      if (!ok) return
      api.post('/api/customer/appointments/' + id + '/cancel', null, { reason: '客户取消' })
        .then(() => { util.toast('已取消'); this.loadAppointments() })
        .catch((err) => util.toast(err.message))
    })
  },

  goBook() { this.setData({ tab: 'book' }) }
})
