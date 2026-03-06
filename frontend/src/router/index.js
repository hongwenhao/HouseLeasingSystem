import { createRouter, createWebHistory } from 'vue-router'
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

const routes = [
  { path: '/', component: Home },
  { path: '/login', component: Login },
  { path: '/register', component: Register },
  { path: '/houses', component: HouseList },
  { path: '/houses/:id', component: HouseDetail },
  { path: '/user-center', component: UserCenter, meta: { requiresAuth: true } },
  { path: '/landlord-center', component: LandlordCenter, meta: { requiresAuth: true, requiresRole: 'LANDLORD' } },
  { path: '/publish-house', component: PublishHouse, meta: { requiresAuth: true, requiresRole: 'LANDLORD' } },
  { path: '/publish-house/:id', component: PublishHouse, meta: { requiresAuth: true, requiresRole: 'LANDLORD' } },
  { path: '/orders/:id', component: OrderDetail, meta: { requiresAuth: true } },
  { path: '/contracts/:id', component: ContractDetail, meta: { requiresAuth: true } },
  { path: '/admin', component: AdminDashboard, meta: { requiresAuth: true, requiresRole: 'ADMIN' } },
  { path: '/:pathMatch(.*)*', component: NotFound }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresRole && to.meta.requiresRole !== role) {
    next('/')
  } else {
    next()
  }
})

export default router
