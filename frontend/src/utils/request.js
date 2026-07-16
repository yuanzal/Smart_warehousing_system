import axios from 'axios'
// 1. 替换 Element UI 为 Element Plus
import { ElMessage, ElMessageBox } from 'element-plus'
// Element Plus 需单独导入样式（也可在 main.js 全局导入）
import 'element-plus/dist/index.css'

import { removeAuth } from '@/utils/auth' // 保留原 auth 工具
import qs from 'qs' // 保留原 qs 序列化
import { debounce } from 'throttle-debounce' // 保留原防抖工具
import router from '../router/index.js'
import * as Lockr from "lockr"; // Vue3 路由实例（配置方式与 Vue2 不同，下文附路由配置）

// 2. 防抖函数：清除缓存并刷新（逻辑不变，仅调整 Element 提示）
const clearCacheEnterLogin = debounce(500, async () => {
  try {
    await removeAuth() // 若原 removeAuth 是 Promise，用 await 替代 then/catch
    location.reload() // 重新实例化路由，避免缓存问题
  } catch (error) {
    location.reload()
  }
})

// 3. 防抖错误提示：替换 Element UI 的 Message 为 Element Plus 的 ElMessage
const errorMessage = debounce(500, (message, type = 'error') => {
  ElMessage({
    showClose: true,
    message: message,
    duration: 1500,
    type: type // Element Plus 支持 'success'/'warning'/'error'/'info'
  })
})

// 4. 防抖确认弹窗：替换 Element UI 的 MessageBox 为 Element Plus 的 ElMessageBox
const confirmMessage = debounce(1000, async (message) => {
  try {
    await ElMessageBox.confirm(
      message,
      '提示',
      {
        confirmButtonText: '确定',
        showCancelButton: false,
        type: 'warning'
      }
    )
    clearCacheEnterLogin() // 确认后执行清除缓存
  } catch (error) {
    // 取消时的回调（原代码为空，保留兼容）
  }
})

// 5. 默认请求头配置（逻辑不变）
axios.defaults.headers.post['Content-Type'] = 'application/json;charset=UTF-8'

// 6. 创建 Axios 实例（逻辑不变）
const service = axios.create({
  baseURL: `${import.meta.env.VITE_APP_Address}`, // Vue3 Vite 环境变量（替代 Vue2 的 process.env）
  timeout: 600000 // 请求超时时间（与原一致）
})
const lockrToken = Lockr.get('Admin-Token');
const localToken = window.localStorage.getItem('token');
// 7. Request 拦截器（逻辑不变，仅优化条件判断写法）
service.interceptors.request.use(
    (config) => {
        // Token提纯逻辑不动
        const rawToken = Lockr.get('Admin-Token') || window.localStorage.getItem('token')
        let token = null
        if (rawToken) {
            if (typeof rawToken === 'object' && rawToken.adminToken) {
                token = rawToken.adminToken
            } else if (typeof rawToken === 'string' && rawToken.startsWith('{')) {
                try {
                    const parsed = JSON.parse(rawToken)
                    token = parsed.adminToken || rawToken
                } catch (e) {
                    token = rawToken
                }
            } else {
                token = rawToken
            }
        }
        if (token) {
            config.headers['Admin-Token'] = token
        } else {
            console.warn('【拦截器调试】未获取到有效 Token')
        }
        const method = config.method?.toLowerCase()
        if (['post','put','patch'].includes(method)) {
            config.headers['Content-Type'] = 'application/json;charset=UTF-8'
        }
        // ========== 修复后的格式处理 ==========
        const contentType = config.headers['Content-Type'] ?? ''
        const isJson = contentType.includes('application/json')
        const isFormData = contentType.includes('multipart/form-data')

        // JSON请求：不序列化，直接传对象
        if (isJson) {
            if (config.data === undefined || config.data === null) {
                config.data = {}
            }
        } else if (!isFormData && config.data) {
            // 仅普通表单场景序列化
            config.data = qs.stringify(config.data)
        }

        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 8. Response 拦截器（逻辑不变，仅替换 Element 组件）
service.interceptors.response.use(
  (response) => {
    // 处理文件流响应（如下载文件，逻辑不变）
    if (response.status === 200 && response.config.responseType === 'blob') {
      const hasDisposition = response.headers['content-disposition']
      const isPdf = response.headers['content-type']?.includes('application/pdf')
      if (hasDisposition || isPdf) {
        return response
      } else {
        // （可选）保留原文件流报错临时处理逻辑（按需启用）
        // const resultBlob = new Blob([response.data], { type: 'application/json' })
        // const fr = new FileReader()
        // fr.onload = () => {
        //   const result = JSON.parse(fr.result)
        //   if (result.msg) {
        //     errorMessage(result.msg, result.code === 1 ? 'success' : 'error')
        //   }
        // }
        // fr.readAsText(resultBlob)
        return response
      }
    }

    // 处理 JSON 响应（状态码非 0 时抛错）
    const res = response.data
    if (res.code !== 0) {
      // 302：登录失效（异地登录/Token过期）
      if (res.code === 302) {
        if (res.data?.extra === 1) {
          // 异地登录提示
          confirmMessage(`您的账号${res.data.extraTime}在别处登录。如非本人操作，则密码可能已泄漏，建议修改密码`)
        } else {
          // 普通登录失效：直接清除缓存
          clearCacheEnterLogin()
        }
      } 
      // 1005：跳转欢迎页（逻辑不变）
      else if (res.code === 1005) {
        router.push('/welcome')
      } 
      // 其他错误：提示错误信息
      else {
        if (res.msg) {
          errorMessage(res.msg)
        }
      }
      return Promise.reject(res)
    }

    // 成功响应：返回数据
    return res
  },
  (error) => {
    // 网络错误处理（逻辑不变，替换 Element 组件）
    if (error.response) {
      const { status, data } = error.response
      if (status === 500) {
        errorMessage('网络错误，请检查您的网络')
      } else if (data?.msg) {
        errorMessage(data.msg)
      }
    }
    return Promise.reject(error)
  }
)

export default service
