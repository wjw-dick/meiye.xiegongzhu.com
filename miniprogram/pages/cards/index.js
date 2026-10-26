const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: { cards: [], loading: true, active: null, flows: [], amount: '', busy: false },

  onLoad() { if (util.ensureLogin()) this.load() },

  load() {
    this.setData({ loading: true })
    api.get('/api/customer/cards')
      .then((cards) => this.setData({ cards, loading: false }))
      .catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  open(e) {
    const card = this.data.cards[e.currentTarget.dataset.index]
    this.setData({ active: card })
    api.get('/api/customer/cards/' + card.id + '/flows')
      .then((flows) => this.setData({ flows }))
      .catch(() => this.setData({ flows: [] }))
  },

  close() { this.setData({ active: null, flows: [], amount: '' }) },

  onAmount(e) { this.setData({ amount: e.detail.value }) },

  recharge() {
    const amount = Number(this.data.amount)
    if (!this.data.active || !amount || amount <= 0) return util.toast('请输入充值金额')
    this.setData({ busy: true })
    api.post('/api/customer/cards/' + this.data.active.id + '/recharge', null, { amount })
      .then(() => {
        util.toast('充值成功', 'success')
        this.setData({ amount: '', busy: false })
        this.load()
        api.get('/api/customer/cards/' + this.data.active.id + '/flows').then((flows) => this.setData({ flows }))
      }).catch((e) => { util.toast(e.message); this.setData({ busy: false }) })
  }
})
