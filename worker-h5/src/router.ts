import { createRouter, createWebHashHistory } from 'vue-router'
import { auth } from './api'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { public: true, tab: false, title: '技师登录' } },
  { path: '/', redirect: '/home' },
  { path: '/home', component: () => import('./views/Home.vue'), meta: { tab: 'home', title: '工作台' } },
  { path: '/appointments', component: () => import('./views/Appointments.vue'), meta: { tab: 'appt', title: '我的预约' } },
  { path: '/orders', component: () => import('./views/Orders.vue'), meta: { tab: 'orders', title: '订单核销' } },
  { path: '/schedule', component: () => import('./views/Schedule.vue'), meta: { tab: 'home', title: '我的排班' } },
  { path: '/performance', component: () => import('./views/Performance.vue'), meta: { tab: 'mine', title: '业绩与提成' } },
  { path: '/withdrawals', component: () => import('./views/Withdrawals.vue'), meta: { tab: 'mine', title: '提现管理' } },
  { path: '/logs', component: () => import('./views/Logs.vue'), meta: { tab: 'mine', title: '服务日志' } },
  { path: '/reviews', component: () => import('./views/Reviews.vue'), meta: { tab: 'mine', title: '客户评价' } },
  { path: '/certs', component: () => import('./views/Certs.vue'), meta: { tab: 'mine', title: '我的证书' } },
  { path: '/messages', component: () => import('./views/Messages.vue'), meta: { tab: 'mine', title: '消息通知' } },
  { path: '/profile', component: () => import('./views/Profile.vue'), meta: { tab: 'mine', title: '个人中心' } },
  { path: '/password', component: () => import('./views/Password.vue'), meta: { tab: false, title: '修改密码' } },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

router.beforeEach((to) => {
  if (!to.meta.public && !auth.token) {
    return { path: '/login', query: { redirect: to.fullPath, tenant: auth.tenantCode } }
  }
  return true
})

export default router
