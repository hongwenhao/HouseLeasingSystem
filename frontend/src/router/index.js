/**
 * router/index.js —— 前端路由配置文件
 *
 * 使用 Vue Router 4（History 模式）管理所有页面路由。
 * 路由守卫负责鉴权：
 *   - requiresAuth: true  ——> 未登录时跳转至 /login
 *   - requiresRole: '角色' ——> 角色不匹配时跳转至首页
 */

import { createRouter, createWebHistory } from 'vue-router'
// 导入各页面视图组件
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import HouseList from '../views/HouseList.vue'
import HouseDetail from '../views/HouseDetail.vue'
import UserCenter from '../views/UserCenter.vue'
import LandlordCenter from '../views/LandlordCenter.vue'
import PublishHouse from '../views/PublishHouse.vue'
import OrderDetail from '../views/OrderDetail.vue'
import ContractDetail from '../views/ContractDetail.vue'
import AdminDashboard from '../views/AdminDashboard.vue'
import NotFound from '../views/NotFound.vue'

/** 路由表：定义 URL 路径与组件的映射关系 */
const routes = [
  { path: '/', component: Home },                  // 首页
  { path: '/login', component: Login },             // 登录页
  { path: '/register', component: Register },       // 注册页
  { path: '/houses', component: HouseList },        // 房源列表页
  { path: '/houses/:id', component: HouseDetail },  // 房源详情页（动态路由）
  // 以下路由需要登录（requiresAuth: true）
  { path: '/user-center', component: UserCenter, meta: { requiresAuth: true } },
  // 房东中心和发布房源页只允许 LANDLORD 角色访问
  { path: '/landlord-center', component: LandlordCenter, meta: { requiresAuth: true, requiresRole: 'LANDLORD' } },
  { path: '/publish-house', component: PublishHouse, meta: { requiresAuth: true, requiresRole: 'LANDLORD' } },
  { path: '/publish-house/:id', component: PublishHouse, meta: { requiresAuth: true, requiresRole: 'LANDLORD' } },
  { path: '/orders/:id', component: OrderDetail, meta: { requiresAuth: true } },
  { path: '/contracts/:id', component: ContractDetail, meta: { requiresAuth: true } },
  // 管理后台只允许 ADMIN 角色访问
  { path: '/admin', component: AdminDashboard, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  // 兜底路由：匹配所有未定义路径，显示 404 页面
  { path: '/:pathMatch(.*)*', component: NotFound }
]

/** 创建路由实例，使用 HTML5 History 模式（URL 不含 # 号） */
const router = createRouter({
  history: createWebHistory(),
  routes
})

/**
 * 全局前置路由守卫
 * 在每次路由跳转前执行鉴权检查：
 *   1. 若目标路由需要登录，且本地无 token，则重定向到 /login
 *   2. 若目标路由需要特定角色，且当前用户角色不符，则重定向到首页
 */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token') // 从本地存储读取 JWT Token
  const role = localStorage.getItem('role')   // 从本地存储读取用户角色
  if (to.meta.requiresAuth && !token) {
    // 未登录，跳转到登录页
    next('/login')
  } else if (to.meta.requiresRole && to.meta.requiresRole !== role) {
    // 角色不匹配，跳转到首页
    next('/')
  } else {
    next()
  }
})

export default router
