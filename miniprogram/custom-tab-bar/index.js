Component({
  data: {
    selected: 0,
    list: [
      { pagePath: '/pages/index/index', text: '首页', icon: '⌂' },
      { pagePath: '/pages/appointment/index', text: '预约', icon: '✚' },
      { pagePath: '/pages/order/index', text: '订单', icon: '☰' },
      { pagePath: '/pages/mine/index', text: '我的', icon: '☺' }
    ]
  },
  attached() {
    const app = getApp()
    if (app.globalData.tabBar) this.setData({ list: app.globalData.tabBar })
  },
  methods: {
    switchTab(e) {
      const path = e.currentTarget.dataset.path
      wx.switchTab({ url: path })
    }
  }
})
