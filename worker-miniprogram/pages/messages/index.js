const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: { list: [], loading: true },
  onLoad() {
    if (!util.ensureLogin()) return
    api.get('/api/worker/messages')
      .then((list) => this.setData({ list: list || [], loading: false }))
      .catch((e) => { this.setData({ loading: false }); util.toast(e.message) })
  }
})
