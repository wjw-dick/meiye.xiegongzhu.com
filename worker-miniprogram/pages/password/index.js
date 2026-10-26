const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: { oldPassword: '', newPassword: '', confirmPassword: '', busy: false, error: '' },
  onInput(e) { this.setData({ [e.currentTarget.dataset.field]: e.detail.value, error: '' }) },
  submit() {
    const { oldPassword, newPassword, confirmPassword } = this.data
    if (!oldPassword || !newPassword) return this.setData({ error: '请填写完整' })
    if (newPassword.length < 6) return this.setData({ error: '新密码至少 6 位' })
    if (newPassword !== confirmPassword) return this.setData({ error: '两次输入的新密码不一致' })
    this.setData({ busy: true, error: '' })
    api.post('/api/auth/password', { oldPassword, newPassword })
      .then(() => { util.toast('密码修改成功', 'success'); setTimeout(() => wx.navigateBack(), 800) })
      .catch((e) => this.setData({ error: e.message }))
      .finally(() => this.setData({ busy: false }))
  }
})
