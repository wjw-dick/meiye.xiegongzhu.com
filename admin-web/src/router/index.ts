import { createRouter, createWebHashHistory, RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '经营看板', icon: 'DataLine', group: '工作台' } },
      { path: 'appointments', name: 'Appointments', component: () => import('@/views/Appointments.vue'), meta: { title: '预约排班', icon: 'Calendar', group: '业务中心' } },
      { path: 'orders', name: 'Orders', component: () => import('@/views/Orders.vue'), meta: { title: '收银订单', icon: 'ShoppingCart', group: '业务中心' } },
      { path: 'cashier', name: 'Cashier', component: () => import('@/views/Cashier.vue'), meta: { title: '交接班', icon: 'Wallet', group: '业务中心' } },
      { path: 'customers', name: 'Customers', component: () => import('@/views/Customers.vue'), meta: { title: '客户档案', icon: 'User', group: '业务中心' } },
      { path: 'technicians', name: 'Technicians', component: () => import('@/views/Technicians.vue'), meta: { title: '技师档案', icon: 'Avatar', group: '业务中心' } },
      { path: 'member-cards', name: 'MemberCards', component: () => import('@/views/MemberCards.vue'), meta: { title: '会员卡', icon: 'CreditCard', group: '会员营销' } },
      { path: 'coupons', name: 'Coupons', component: () => import('@/views/GenericCrud.vue'), meta: { title: '优惠券', icon: 'Ticket', group: '会员营销', resource: 'coupons' } },
      { path: 'debts', name: 'Debts', component: () => import('@/views/GenericCrud.vue'), meta: { title: '欠款管理', icon: 'Money', group: '会员营销', resource: 'debts' } },
      { path: 'points', name: 'Points', component: () => import('@/views/Points.vue'), meta: { title: '会员积分', icon: 'Star', group: '会员营销' } },
      { path: 'marketing', name: 'Marketing', component: () => import('@/views/GenericCrud.vue'), meta: { title: '营销活动', icon: 'Present', group: '会员营销', resource: 'marketing' } },
      { path: 'distributions', name: 'Distributions', component: () => import('@/views/Distributions.vue'), meta: { title: '分销佣金', icon: 'Share', group: '会员营销' } },
      { path: 'reviews', name: 'Reviews', component: () => import('@/views/Reviews.vue'), meta: { title: '评价管理', icon: 'ChatDotRound', group: '会员营销' } },
      { path: 'follow-ups', name: 'FollowUps', component: () => import('@/views/FollowUps.vue'), meta: { title: '回访任务', icon: 'Phone', group: '会员营销' } },
      { path: 'after-sales', name: 'AfterSales', component: () => import('@/views/AfterSales.vue'), meta: { title: '售后返工', icon: 'RefreshLeft', group: '会员营销' } },
      { path: 'settlements', name: 'Settlements', component: () => import('@/views/Settlements.vue'), meta: { title: '结算分账', icon: 'Coin', group: '财务结算' } },
      { path: 'commission-rules', name: 'CommissionRules', component: () => import('@/views/GenericCrud.vue'), meta: { title: '提成规则', icon: 'TrendCharts', group: '财务结算', resource: 'commissionRules' } },
      { path: 'withdrawals', name: 'Withdrawals', component: () => import('@/views/Withdrawals.vue'), meta: { title: '技师提现', icon: 'BankCard', group: '财务结算' } },
      { path: 'inventory', name: 'Inventory', component: () => import('@/views/Inventory.vue'), meta: { title: '耗材库存', icon: 'Box', group: '供应链' } },
      { path: 'stores', name: 'Stores', component: () => import('@/views/GenericCrud.vue'), meta: { title: '门店管理', icon: 'OfficeBuilding', group: '基础设置', resource: 'stores' } },
      { path: 'workstations', name: 'Workstations', component: () => import('@/views/GenericCrud.vue'), meta: { title: '工位房间', icon: 'Grid', group: '基础设置', resource: 'workstations' } },
      { path: 'service-items', name: 'ServiceItems', component: () => import('@/views/GenericCrud.vue'), meta: { title: '服务项目', icon: 'Goods', group: '基础设置', resource: 'serviceItems' } },
      { path: 'service-packages', name: 'ServicePackages', component: () => import('@/views/GenericCrud.vue'), meta: { title: '套餐管理', icon: 'Files', group: '基础设置', resource: 'servicePackages' } },
      { path: 'service-flows', name: 'ServiceFlows', component: () => import('@/views/GenericCrud.vue'), meta: { title: '流程模板', icon: 'Memo', group: '基础设置', resource: 'serviceFlows' } },
      { path: 'card-types', name: 'CardTypes', component: () => import('@/views/GenericCrud.vue'), meta: { title: '卡种管理', icon: 'Postcard', group: '基础设置', resource: 'cardTypes' } },
      { path: 'users', name: 'Users', component: () => import('@/views/GenericCrud.vue'), meta: { title: '员工账号', icon: 'UserFilled', group: '基础设置', resource: 'tenantUsers' } },
      { path: 'dicts', name: 'Dicts', component: () => import('@/views/GenericCrud.vue'), meta: { title: '自定义字典', icon: 'Collection', group: '基础设置', resource: 'tenantDicts' } },
      { path: 'tenant-profile', name: 'TenantProfile', component: () => import('@/views/TenantProfile.vue'), meta: { title: '租户配置', icon: 'Setting', group: '系统管理' } },
      { path: 'storage', name: 'Storage', component: () => import('@/views/Storage.vue'), meta: { title: '对象存储', icon: 'FolderOpened', group: '系统管理' } },
      { path: 'biz-types', name: 'BizTypes', component: () => import('@/views/BizTypes.vue'), meta: { title: '品类开关', icon: 'Cpu', group: '系统管理' } },
      { path: 'cms', name: 'Cms', component: () => import('@/views/Cms.vue'), meta: { title: '门户内容', icon: 'Document', group: '系统管理' } },
      { path: 'wechat', name: 'Wechat', component: () => import('@/views/Wechat.vue'), meta: { title: '微信配置', icon: 'ChatLineSquare', group: '系统管理' } },
      { path: 'messages', name: 'Messages', component: () => import('@/views/Messages.vue'), meta: { title: '消息模板', icon: 'Bell', group: '系统管理' } },
      { path: 'menus', name: 'Menus', component: () => import('@/views/Menus.vue'), meta: { title: '菜单可视化', icon: 'Menu', group: '系统管理' } },
      { path: 'audit', name: 'Audit', component: () => import('@/views/Audit.vue'), meta: { title: '审计日志', icon: 'Tickets', group: '系统管理' } },
      { path: 'reports', name: 'Reports', component: () => import('@/views/Reports.vue'), meta: { title: '经营报表', icon: 'PieChart', group: '数据报表' } },
      { path: 'warnings', name: 'Warnings', component: () => import('@/views/Warnings.vue'), meta: { title: '到期预警', icon: 'Warning', group: '数据报表' } },
      { path: 'platform/dashboard', name: 'PlatformDashboard', component: () => import('@/views/platform/PlatformDashboard.vue'), meta: { title: '平台看板', icon: 'Monitor', group: '平台管理', platform: true } },
      { path: 'platform/tenants', name: 'PlatformTenants', component: () => import('@/views/platform/PlatformTenants.vue'), meta: { title: '租户管理', icon: 'OfficeBuilding', group: '平台管理', platform: true } },
      { path: 'platform/plugins', name: 'PlatformPlugins', component: () => import('@/views/platform/PlatformPlugins.vue'), meta: { title: '品类插件库', icon: 'Cpu', group: '平台管理', platform: true } },
      { path: 'platform/params', name: 'PlatformParams', component: () => import('@/views/platform/PlatformParams.vue'), meta: { title: '平台参数', icon: 'Tools', group: '平台管理', platform: true } },
      { path: 'platform/dicts', name: 'PlatformDicts', component: () => import('@/views/platform/PlatformDicts.vue'), meta: { title: '全局字典', icon: 'Collection', group: '平台管理', platform: true } },
      { path: 'platform/languages', name: 'PlatformLanguages', component: () => import('@/views/platform/PlatformLanguages.vue'), meta: { title: '多语言', icon: 'Switch', group: '平台管理', platform: true } },
      { path: 'platform/menus', name: 'PlatformMenus', component: () => import('@/views/platform/PlatformMenus.vue'), meta: { title: '平台菜单', icon: 'Menu', group: '平台管理', platform: true } },
      { path: 'platform/audit', name: 'PlatformAudit', component: () => import('@/views/platform/PlatformAudit.vue'), meta: { title: '平台审计', icon: 'Tickets', group: '平台管理', platform: true } },
      { path: 'platform/reports', name: 'PlatformReports', component: () => import('@/views/platform/PlatformReports.vue'), meta: { title: '平台报表', icon: 'PieChart', group: '平台管理', platform: true } },
      { path: 'platform/storage', name: 'PlatformStorage', component: () => import('@/views/Storage.vue'), meta: { title: '平台存储', icon: 'FolderOpened', group: '平台管理', platform: true } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  document.title = `${to.meta.title || '管理后台'} · 美业大健康数字化平台`
  if (!to.meta.public && !auth.token) {
    return { path: '/login' }
  }
  if (to.path === '/login' && auth.token) {
    return { path: '/dashboard' }
  }
  return true
})

export default router
