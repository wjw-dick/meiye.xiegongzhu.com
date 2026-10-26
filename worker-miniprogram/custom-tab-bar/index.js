Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/index/index', text: '工作台', icon: '⌂' },
      { pagePath: '/pages/appointments/index', text: '预约', icon: '◷' },
      { pagePath: '/pages/orders/index', text: '核销', icon: '▤' },
      { pagePath: '/pages/mine/index', text: '我的', icon: '☺' }
    ]
  },
  attached() {
    const app = getApp()
    if (app.globalData.tabBar) this.setData({ list: app.globalData.tabBar })
  },
  methods: {
    switchTab(e) {
      wx.switchTab({ url: e.currentTarget.dataset.path })
    }
  }
})
