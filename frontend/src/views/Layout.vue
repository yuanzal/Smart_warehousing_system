<template>
    <el-container class="layout-container">
        <el-aside width="240px" class="layout-aside">
            <div class="sidebar-brand">
                <span class="brand-icon">📦</span>
                <span class="brand-text">WMS 调度大厅</span>
            </div>

            <el-menu
                :default-active="activeMenu"
                :collapse-transition="false"
                class="sidebar-menu"
                background-color="#1e293b"
                text-color="#94a3b8"
                active-text-color="#38bdf8"
                @select="handleMenuSelect"
            >
                <el-menu-item index="/dashboard">
                    <span class="menu-emoji">🖥️</span>
                    <span>3D 数字孪生控制塔</span>
                </el-menu-item>
                <el-menu-item index="/packages">
                    <span class="menu-emoji">✉️</span>
                    <span>包裹主档案管理</span>
                </el-menu-item>
                <el-menu-item index="/locations">
                    <span class="menu-emoji">🧱</span>
                    <span>货位基础堆垛管理</span>
                </el-menu-item>
                <el-menu-item index="/tasks">
                    <span class="menu-emoji">⚙️</span>
                    <span>智能分拣调度台</span>
                </el-menu-item>
                <el-menu-item index="/users">
                    <span class="menu-emoji">👥</span>
                    <span>系统用户管理</span>
                </el-menu-item>
            </el-menu>
        </el-aside>

        <el-container class="main-container">
            <el-header class="layout-header">
                <div class="header-left">
          <span class="status-indicator">
            <span class="pulse-dot"></span>
            仓内物联网络：在线 (Redis高可用已降级保护)
          </span>
                </div>
                <div class="header-right">
                    <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-profile">
              <el-avatar :size="32" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="username">系统管理员</span>
            </span>
                        <template #dropdown>
                            <el-dropdown-menu class="dark-dropdown">
                                <el-dropdown-item command="profile">账号设置</el-dropdown-item>
                                <el-dropdown-item command="logout" divided style="color: #f43f5e;">退出登录</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
            </el-header>

            <el-main class="layout-main">
                <router-view v-slot="{ Component, route }">
                    <component :is="Component" :key="route.fullPath" />
                </router-view>
            </el-main>
        </el-container>
    </el-container>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import * as Lockr from 'lockr'
import { ElMessage } from 'element-plus'
import { removeAuth } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const activeMenu = ref(route.path)
watch(() => route.path, (newPath) => {
    activeMenu.value = newPath
})

const handleMenuSelect = (index) => {
    router.push(index)
}

const handleCommand = async (command) => {
    if (command === 'logout') {
        Lockr.rm('Admin-Token')
        localStorage.removeItem('token')
        try {
            await removeAuth()
            ElMessage.success('安全登出成功')
        } catch (err) {
            ElMessage.warning('服务端注销失败，本地凭证已清除')
        } finally {
            router.replace('/login')
        }
    }
}
</script>

<style scoped>
.layout-container {
    width: 100vw;
    height: 100vh;
    background-color: #0b0f19;
    color: #f8fafc;
}
.layout-aside {
    background-color: #1e293b;
    border-right: 1px solid #334155;
    display: flex;
    flex-direction: column;
}
.sidebar-brand {
    height: 60px;
    display: flex;
    align-items: center;
    padding: 0 20px;
    border-bottom: 1px solid #334155;
    background-color: #0f172a;
}
.brand-icon {
    font-size: 22px;
    margin-right: 8px;
}
.brand-text {
    font-size: 16px;
    font-weight: 700;
    color: #38bdf8;
    letter-spacing: 1px;
}
.sidebar-menu {
    border-right: none !important;
    flex: 1;
}
.menu-emoji {
    font-size: 16px;
    margin-right: 12px;
}
.layout-header {
    height: 60px;
    background-color: #1e293b;
    border-bottom: 1px solid #334155;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 24px;
}
.status-indicator {
    display: flex;
    align-items: center;
    font-size: 13px;
    color: #94a3b8;
}
.pulse-dot {
    width: 8px;
    height: 8px;
    background-color: #10b981;
    border-radius: 50%;
    margin-right: 8px;
    box-shadow: 0 0 8px #10b981;
}
.user-profile {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #e2e8f0;
}
.username {
    margin-left: 10px;
    font-size: 14px;
}
.layout-main {
    background-color: #0f172a;
    padding: 20px;
    overflow: hidden;
    position: relative;
}
</style>