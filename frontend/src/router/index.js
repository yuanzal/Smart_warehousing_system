// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'
import * as Lockr from 'lockr'

// 导入页面组件
import Login from '../views/Login.vue'
import Layout from '../views/Layout.vue' // 导入布局父壳子
import Dashboard from '../views/Dashboard.vue' // 3D大屏子组件
import UserManagement from '../views/UserManagement.vue' // 用户管理子组件
import Placeholder from '../views/Placeholder.vue' // 统一开发占位组件
import DeviceManagement from '@/views/DeviceManagement.vue'

// 定义路由规则
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { requiresAuth: false }
  },
  // ===== 新增：3D 仓库数字孪生页面 =====
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: false }  // 开发阶段无需登录，后续可改为 true
  },
  // ===== 新增：设备管理页面 =====
  {
    path: '/device-management',
    name: 'DeviceManagement',
    component: DeviceManagement,
    meta: {requiresAuth: false}  // 开发阶段无需登录，后续可改为 true
  },
  {
    path: '/',
    component: Layout, // 访问根目录及其子路由，都先加载 Layout 壳子
    redirect: '/dashboard',
    meta: { requiresAuth: true }, // 父路由统一鉴权
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: Dashboard,
        meta: { title: '3D 数字孪生控制塔', requiresAuth: true }
      },
      {
        path: 'packages',
        name: 'Packages',
        component: Placeholder,
        meta: { title: '包裹主档案管理', requiresAuth: true }
      },
      {
        path: 'locations',
        name: 'Locations',
        component: Placeholder,
        meta: { title: '货位基础堆垛管理', requiresAuth: true }
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: Placeholder,
        meta: { title: '智能分拣调度台', requiresAuth: true }
      },
      {
        path: 'users',
        name: 'Users',
        component: UserManagement,
        meta: { title: '系统用户管理', requiresAuth: true }
      }
    ]
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

// 路由守卫：联合 Lockr 缓存进行双重鉴权与状态同步
router.beforeEach((to, from, next) => {
  const localToken = Lockr.get('Admin-Token')
  const storeToken = store.state.token

  if (localToken && !storeToken) {
    store.commit('SET_TOKEN', localToken)
  }

  const token = storeToken || localToken

  if (to.meta.requiresAuth && !token) {
    // 需要登录但未登录，跳转到登录页
    console.log('未登录，跳转到登录页')
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router