<template>
    <el-container class="layout-container">
        <el-aside width="240px" class="layout-aside">
            <div class="sidebar-brand">
                <span class="brand-icon">📦</span>
                <span class="brand-text">WMS 调度大厅</span>
            </div>

            <el-menu
                    :default-active="activeMenu"
                    class="sidebar-menu"
                    background-color="#1e293b"
                    text-color="#94a3b8"
                    active-text-color="#38bdf8"
                    @select="handleMenuSelect"
            >
                <el-menu-item index="dashboard">
                    <span class="menu-emoji">🖥️</span>
                    <span>3D 数字孪生控制塔</span>
                </el-menu-item>
                <el-menu-item index="packages">
                    <span class="menu-emoji">✉️</span>
                    <span>包裹主档案管理</span>
                </el-menu-item>
                <el-menu-item index="locations">
                    <span class="menu-emoji">🧱</span>
                    <span>货位基础堆垛管理</span>
                </el-menu-item>
                <el-menu-item index="tasks">
                    <span class="menu-emoji">⚙️</span>
                    <span>智能分拣调度台</span>
                </el-menu-item>
                <el-menu-item index="users">
                    <span class="menu-emoji">👥
                    </span>
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
                <div v-if="activeMenu === 'dashboard'" key="module-dashboard" class="dashboard-scene-wrapper">
                    <div class="scene-overlay-card">
                        <h3>🛸 智能仓库 3D 数字孪生大屏</h3>
                        <p>集成多源传感器与AGV实时定位上报。当前坐标跟踪就绪。</p>
                    </div>
                    <ThreeScene class="three-viewport" />
                </div>

                <div v-else-if="activeMenu === 'users'" key="module-users">
                    <UserManagement />
                </div>

                <div v-else class="placeholder-module" key="module-placeholder">
                    <el-card class="placeholder-card">
                        <h2>🛠️ {{ menuTitles[activeMenu] }} 正在加载</h2>
                        <p>该功能页面（属于 Feature 2.1 Story 2/3 及 Feature 2.3）正在敏捷开发中...</p>
                        <el-empty description="开发组正在全力拼搏中" />
                    </el-card>
                </div>
            </el-main>
        </el-container>
    </el-container>
</template>

<script setup>
import UserManagement from './UserManagement.vue'
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { removeAuth } from '@/utils/auth' // 引入原退出清除逻辑
import * as Lockr from 'lockr'
import { ElMessage } from 'element-plus'

// 💡 动态引入你在 elements/components 中自研的 3D 三维场景渲染组件！
import ThreeScene from '../components/ThreeScene.vue'

const router = useRouter()
const activeMenu = ref('dashboard')

const menuTitles = reactive({
    dashboard: '3D 数字孪生控制塔',
    packages: '包裹主档案管理',
    locations: '货位基础堆垛管理',
    tasks: '智能分拣调度台',
    users: '系统用户管理'
})

const handleMenuSelect = (index) => {
    activeMenu.value = index
}

const handleCommand = async (command) => {
    if (command === 'logout') {
        // 第一步：优先清空持久化存储（永久解决残留Token）
        Lockr.rm('Admin-Token')
        localStorage.removeItem('token')

        try {
            // 此时就算接口401、mutation报错，缓存已删，不影响跳转
            await removeAuth()
            ElMessage.success('安全登出成功')
        } catch (err) {
            console.error('后端注销异常', err)
            ElMessage.warning('服务端注销失败，本地凭证已清除')
        } finally {
            // 强制替换路由，无历史回退
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

/* 3D 容器布局 */
.dashboard-scene-wrapper {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: 8px;
    overflow: hidden;
    border: 1px solid #334155;
}

.scene-overlay-card {
    position: absolute;
    top: 20px;
    left: 20px;
    z-index: 99;
    background-color: rgba(15, 23, 42, 0.8);
    padding: 16px 24px;
    border-radius: 8px;
    border: 1px solid #38bdf8;
    box-shadow: 0 4px 12px rgba(0,0,0,0.5);
    pointer-events: none;
}

.scene-overlay-card h3 {
    margin: 0 0 6px 0;
    color: #38bdf8;
}

.scene-overlay-card p {
    margin: 0;
    font-size: 12px;
    color: #94a3b8;
}

.three-viewport {
    width: 100%;
    height: 100%;
}

/* 占位页布局 */
.placeholder-module {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}

.placeholder-card {
    background-color: #1e293b !important;
    border: 1px solid #334155 !important;
    width: 500px;
    text-align: center;
    padding: 40px;
}

.placeholder-card h2 {
    color: #f8fafc;
    margin-bottom: 8px;
}

.placeholder-card p {
    color: #64748b;
    margin-bottom: 24px;
}
</style>