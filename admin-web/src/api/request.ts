import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 30000
})

request.interceptors.request.use((config: any) => {
  const token = localStorage.getItem('meiye_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const drillTenant = localStorage.getItem('meiye_drill_tenant')
  if (drillTenant) {
    config.headers['X-Tenant-Id'] = drillTenant
  }
  return config
})

request.interceptors.response.use(
  (res) => {
    const data = res.data
    if (data && typeof data.code === 'number' && data.code !== 200) {
      if (data.code === 401) {
        localStorage.removeItem('meiye_token')
        localStorage.removeItem('meiye_user')
        if (location.hash !== '#/login') {
          location.hash = '#/login'
        }
      }
      ElMessage.error(data.msg || '请求失败')
      return Promise.reject(new Error(data.msg || '请求失败'))
    }
    return data
  },
  (err) => {
    ElMessage.error(err?.response?.data?.msg || err.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
