<template>
    <div class="login-wrapper">
        <div class="login-background"></div>
        <div class="login-container">
            <div class="login-logo-area">
                <div class="logo-circle">🤖</div>
                <h1>WMS 智能调度系统</h1>
                <p>Smart Warehousing Scheduling System</p>
            </div>

            <el-card class="login-card" shadow="always">
                <el-form
                        :model="loginForm"
                        :rules="rules"
                        ref="loginFormRef"
                        size="large"
                        label-position="top"
                >
                    <el-form-item label="管理员账户" prop="username">
                        <el-input
                                v-model="loginForm.username"
                                placeholder="请输入管理员账号"
                                clearable
                        />
                    </el-form-item>

                    <el-form-item label="安全密码" prop="password">
                        <el-input
                                v-model="loginForm.password"
                                type="password"
                                placeholder="请输入登录密码"
                                show-password
                                @keyup.enter="handleLogin"
                        />
                    </el-form-item>

                    <el-form-item style="margin-top: 30px;">
                        <el-button
                                type="primary"
                                :loading="loading"
                                class="login-submit-btn"
                                @click="handleLogin"
                        >
                            登 录
                        </el-button>
                    </el-form-item>
                </el-form>
            </el-card>
        </div>
    </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { login } from '@/api/login' // 引入原 api 接口
import { addAuth } from '@/utils/auth' // 引入原注入授权函数
import { ElMessage } from 'element-plus'
import * as Lockr from 'lockr'

const router = useRouter()
const store = useStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
    username: '',
    password: ''
})

const rules = {
    username: [{ required: true, message: '请输入管理员账户', trigger: 'blur' }],
    password: [{ required: true, message: '请输入安全密码', trigger: 'blur' }]
}

const handleLogin = async () => {
    if (!loginFormRef.value) return

    await loginFormRef.value.validate(async (valid) => {
        if (valid) {
            loading.value = true
            try {
                // 调用原登录接口，传入账号密码对象
                const res = await login({
                    username: loginForm.username,
                    password: loginForm.password
                })

                // 根据原 request.js 规范，成功响应直接返回业务数据
                const token = res.data?.token || res.data

                if (token) {
                    // 1. 同步到本地 Lockr 与 window.localStorage（与 login.js 格式完美对齐）
                    Lockr.set('Admin-Token', token)
                    window.localStorage.token = token

                    // 2. 调用原 auth.js 的 addAuth，将 Admin-Token 注入 Axios header
                    await addAuth(token)

                    // 3. 将 Token 更新到 Vuex store，激活路由守卫防线
                    store.commit('SET_TOKEN', token)

                    ElMessage({
                        type: 'success',
                        message: '系统验证通过，欢迎进入控制塔！',
                        duration: 2000
                    })

                    // 4. 重定向至 3D 孪生大屏
                    router.push('/dashboard')
                } else {
                    ElMessage.error('授权证书颁发异常，请联系系统组')
                }
            } catch (err) {
                console.error('登录异常', err)
                // 异常提示已由底层的 request.js 统一 ElMessage 拦截处理，此处防重复报错
            } finally {
                loading.value = false
            }
        }
    })
}
</script>

<style scoped>
.login-wrapper {
    position: relative;
    width: 100vw;
    height: 100vh;
    background-color: #0b0f19;
    display: flex;
    justify-content: center;
    align-items: center;
    overflow: hidden;
}

.login-background {
    position: absolute;
    width: 120%;
    height: 120%;
    background: radial-gradient(circle at 50% 50%, #1e293b 0%, #0f172a 70%, #020617 100%);
    z-index: 1;
}

.login-container {
    position: relative;
    z-index: 10;
    width: 420px;
    display: flex;
    flex-direction: column;
}

.login-logo-area {
    text-align: center;
    margin-bottom: 24px;
}

.logo-circle {
    font-size: 40px;
    margin-bottom: 12px;
}

.login-logo-area h1 {
    color: #f8fafc;
    font-size: 26px;
    font-weight: 700;
    margin: 0 0 6px 0;
    letter-spacing: 1px;
}

.login-logo-area p {
    color: #64748b;
    font-size: 13px;
    margin: 0;
    text-transform: uppercase;
}

.login-card {
    background-color: rgba(30, 41, 59, 0.85) !important;
    border: 1px solid #334155 !important;
    border-radius: 12px !important;
}

:deep(.el-form-item__label) {
    color: #94a3b8 !important;
    font-weight: 500;
}

:deep(.el-input__wrapper) {
    background-color: #0f172a !important;
    border: 1px solid #334155 !important;
    box-shadow: none !important;
}

:deep(.el-input__inner) {
    color: #f8fafc !important;
}

.login-submit-btn {
    width: 100%;
    background-color: #38bdf8 !important;
    border-color: #38bdf8 !important;
    color: #0f172a !important;
    font-weight: bold !important;
}

.login-submit-btn:hover {
    background-color: #0ea5e9 !important;
    opacity: 0.9;
}
</style>