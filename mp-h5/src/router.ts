import { createRouter, createWebHashHistory } from 'vue-router'
import { auth } from './api'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { public: true, tab: false, title: '会员登录' } },
  { path: '/', redirect: '/home' },
  { path: '/home', component: () => import('./views/Home.vue'), meta: { tab: 'home', title: '会员中心' } },
  { path: '/items', component: () => import('./views/Items.vue'), meta: { tab: 'book', title: '服务项目' } },
  { path: '/booking', component: () => import('./views/Booking.vue'), meta: { tab: 'book', title: '在线预约' } },
  { path: '/appointments', component: () => import('./views/Appointments.vue'), meta: { tab: 'orders', title: '我的预约' } },
  { path: '/orders', component: () => import('./views/Orders.vue'), meta: { tab: 'orders', title: '我的订单' } },
  { path: '/cards', component: () => import('./views/Cards.vue'), meta: { tab: 'mine', title: '会员卡包' } },
  { path: '/coupons', component: () => import('./views/Coupons.vue'), meta: { tab: 'mine', title: '优惠券' } },
  { path: '/points', component: () => import('./views/Points.vue'), meta: { tab: 'mine', title: '积分明细' } },
  { path: '/messages', component: () => import('./views/Messages.vue'), meta: { tab: 'mine', title: '消息通知' } },
  { path: '/reviews', component: () => import('./views/Reviews.vue'), meta: { tab: 'mine', title: '服务评价' } },
  { path: '/after-sales', component: () => import('./views/AfterSales.vue'), meta: { tab: 'mine', title: '售后服务' } },
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
