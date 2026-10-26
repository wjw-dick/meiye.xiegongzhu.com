import { defineStore } from 'pinia'
import request from '@/api/request'

interface UserInfo {
  userId: number
  username: string
  name: string
  userType: string
  role: string
  tenantId?: number
  tenantCode?: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('meiye_token') || '',
    user: JSON.parse(localStorage.getItem('meiye_user') || 'null') as UserInfo | null,
    drillTenant: localStorage.getItem('meiye_drill_tenant') || '',
    drillTenantName: localStorage.getItem('meiye_drill_tenant_name') || ''
  }),
  getters: {
    isPlatform: (state) => state.user?.userType === 'PLATFORM',
    isTenant: (state) => state.user?.userType === 'TENANT',
    displayName: (state) => state.user?.name || ''
  },
  actions: {
    async login(payload: any) {
      const res: any = await request.post('/auth/login', payload)
      this.token = res.data.token
      this.user = res.data.user
      localStorage.setItem('meiye_token', this.token)
      localStorage.setItem('meiye_user', JSON.stringify(this.user))
      return res.data
    },
    setDrillTenant(id: string, name: string) {
      this.drillTenant = id
      this.drillTenantName = name
      if (id) {
        localStorage.setItem('meiye_drill_tenant', id)
        localStorage.setItem('meiye_drill_tenant_name', name)
      } else {
        localStorage.removeItem('meiye_drill_tenant')
        localStorage.removeItem('meiye_drill_tenant_name')
      }
    },
    async logout() {
      try {
        await request.post('/auth/logout')
      } catch (e) {
        // ignore
      }
      this.token = ''
      this.user = null
      this.setDrillTenant('', '')
      localStorage.removeItem('meiye_token')
      localStorage.removeItem('meiye_user')
    }
  }
})
