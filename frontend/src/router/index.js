// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'
import * as Lockr from 'lockr' // 引入 Lockr 获取持久化 Token

// 导入组件
import Login from '../views/Login.vue'
import Dashboard from '@/views/Dashboard.vue'

// 定义路由规则
const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  // ===== 3D 仓库数字孪生控制台 =====
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }  // 开发阶段先不启用
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// ⚡ 路由守卫：联合 Lockr 缓存进行双重鉴权
router.beforeEach((to, from, next) => {
  // 同时检测 Vuex 与 Lockr 缓存中的 Admin-Token，防止刷新掉线
  const token = store.state.token || Lockr.get('Admin-Token')

  console.log('路由守卫检查:', { to: to.path, token: !!token })

  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录，跳转到登录页
    console.log('未登录，安全拦截，跳转到登录页')
    next('/login')
  } else if (to.path === '/login' && token) {
    // 已登录但试图重复访问登录页，直接送去 3D 控制大屏
    console.log('已登录，直接引导至控制台大屏')
    next('/dashboard')
  } else {
    next()
  }
})

export default router