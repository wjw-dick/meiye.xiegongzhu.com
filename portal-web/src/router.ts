import { createRouter, createWebHistory } from 'vue-router'
import { setSeo } from './seo'

const routes = [
  { path: '/', name: 'home', component: () => import('./views/Home.vue'), meta: { title: '首页' } },
  { path: '/items', name: 'items', component: () => import('./views/Items.vue'), meta: { title: '服务项目' } },
  { path: '/items/:id', name: 'item', component: () => import('./views/ItemDetail.vue'), meta: { title: '项目详情' } },
  { path: '/packages', name: 'packages', component: () => import('./views/Packages.vue'), meta: { title: '优惠套餐' } },
  { path: '/packages/:id', name: 'package', component: () => import('./views/PackageDetail.vue'), meta: { title: '套餐详情' } },
  { path: '/stores', name: 'stores', component: () => import('./views/Stores.vue'), meta: { title: '门店' } },
  { path: '/stores/:id', name: 'store', component: () => import('./views/StoreDetail.vue'), meta: { title: '门店详情' } },
  { path: '/news', name: 'news', component: () => import('./views/Articles.vue'), meta: { title: '资讯' } },
  { path: '/news/:id', name: 'article', component: () => import('./views/ArticleDetail.vue'), meta: { title: '资讯详情' } },
  { path: '/share', name: 'share', component: () => import('./views/ShareQuery.vue'), meta: { title: '订单查询' } },
  { path: '/:pathMatch(.*)*', name: 'notfound', component: () => import('./views/NotFound.vue'), meta: { title: '页面不存在' } }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, saved) {
    return saved || { top: 0 }
  }
})

router.afterEach((to) => {
  const title = (to.meta.title as string) || '首页'
  if (!to.name || to.name === 'home') return
  setSeo({ title })
})

export default router
