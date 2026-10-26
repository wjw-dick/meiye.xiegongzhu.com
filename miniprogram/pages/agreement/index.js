Page({
  data: { type: 'privacy' },
  onLoad(options) {
    this.setData({ type: options.type || 'privacy' })
    wx.setNavigationBarTitle({ title: options.type === 'service' ? '会员服务协议' : '隐私政策' })
  }
})
