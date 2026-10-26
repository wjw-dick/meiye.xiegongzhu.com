const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    tab: 'perf',
    perf: {},
    income: {},
    schedules: [],
    withdrawals: [],
    amount: '',
    remark: '',
    busy: false,
    loading: true
  },

  onLoad(options) {
    if (!util.ensureLogin()) return
    if (options.tab) this.setData({ tab: options.tab })
    this.load()
  },

  load() {
    this.setData({ loading: true })
    Promise.all([
      api.get('/api/worker/performance'),
      api.get('/api/worker/schedules'),
      api.get('/api/worker/withdrawals')
    ]).then(([perf, schedules, withdrawals]) => {
      this.setData({ perf, income: perf.income || {}, schedules, withdrawals, loading: false })
    }).catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  },

  switchTab(e) { this.setData({ tab: e.currentTarget.dataset.tab }) },
  onAmount(e) { this.setData({ amount: e.detail.value }) },
  onRemark(e) { this.setData({ remark: e.detail.value }) },

  withdraw() {
    const amount = Number(this.data.amount)
    if (!amount || amount <= 0) return util.toast('请输入提现金额')
    this.setData({ busy: true })
    api.post('/api/worker/withdrawals', { amount, remark: this.data.remark })
      .then(() => {
        util.toast('提现申请已提交', 'success')
        this.setData({ amount: '', remark: '', busy: false })
        this.load()
      }).catch((e) => { util.toast(e.message); this.setData({ busy: false }) })
  }
})
