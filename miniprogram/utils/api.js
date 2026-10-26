const app = () => getApp()

function request(path, options = {}) {
  const a = app()
  const base = a.globalData.baseUrl
  const token = a.globalData.token
  let url = base + path
  if (options.query) {
    const parts = []
    Object.keys(options.query).forEach((k) => {
      const v = options.query[k]
      if (v !== undefined && v !== null && v !== '') parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(v))
    })
    if (parts.length) url += (url.indexOf('?') >= 0 ? '&' : '?') + parts.join('&')
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method: options.method || 'GET',
      data: options.body,
      header: Object.assign({ 'Content-Type': 'application/json' }, token ? { Authorization: 'Bearer ' + token } : {}),
      timeout: 20000,
      success(res) {
        const body = res.data || {}
        if (res.statusCode === 401 || body.code === 401) {
          a.clearAuth()
          wx.reLaunch({ url: '/pages/login/index' })
          reject(new Error(body.msg || '登录已过期'))
          return
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 200) resolve(body.data)
        else reject(new Error(body.msg || '请求失败(' + res.statusCode + ')'))
      },
      fail(err) {
        reject(new Error(err.errMsg && err.errMsg.indexOf('timeout') >= 0 ? '请求超时，请检查网络' : '网络异常，请稍后重试'))
      }
    })
  })
}

module.exports = {
  get: (path, query) => request(path, { query }),
  post: (path, body, query) => request(path, { method: 'POST', body, query }),
  put: (path, body) => request(path, { method: 'PUT', body }),
  del: (path) => request(path, { method: 'DELETE' })
}
