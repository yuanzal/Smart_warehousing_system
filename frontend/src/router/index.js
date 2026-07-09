// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

// 导入组件（可以根据实际项目结构调整）
import Login from '../views/Login.vue'

// 定义路由规则
const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login'
  }
]

// 创建路由实例
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL), // 使用 HTML5 history 模式
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = store.state.token
  
  console.log('路由守卫检查:', { to: to.path, token: !!token })
  
  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录，跳转到登录页
    console.log('未登录，跳转到登录页')
    next('/login')
  } else if (to.path === '/login' && token) {
    // 已登录但访问登录页，跳转到首页
    console.log('已登录，跳转到首页')
    next('/home')
  } else if (to.path === '/' && !token) {
    // 访问根路径且未登录，跳转到登录页
    console.log('访问根路径未登录，跳转到登录页')
    next('/login')
  } else if (to.path === '/' && token) {
    // 访问根路径且已登录，跳转到首页
    console.log('访问根路径已登录，跳转到首页')
    next('/home')
  } else {
    next()
  }
})

export default router
