<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>智慧农业温室大棚全自动化管控平台</h2>
        <p>Smart Agriculture Greenhouse Fully Automated Management Platform</p>
      </div>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
          >
            <!-- 通过 prefix 插槽插入自定义 SVG 图标 -->
            <template #prefix>
              <svg
                class="w-5 h-5 text-slate-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                ></path>
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          >
            <!-- 自定义密码框前缀 SVG 图标 -->
            <template #prefix>
              <svg
                class="w-5 h-5 text-slate-500"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
                ></path>
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <div class="remember-password">
            <el-checkbox v-model="loginForm.rememberPassword">
              记住密码
            </el-checkbox>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? "登录中..." : "登录" }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import { ElMessage } from "element-plus";
import { login } from "@/api/login";

const router = useRouter();
const store = useStore();

// 登录表单
const loginForm = reactive({
  username: "",
  password: "",
  rememberPassword: false,
});

// 表单验证规则
const loginRules = {
  username: [{ required: true, message: "请输入用户名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度不能少于6位", trigger: "blur" },
  ],
};

const loginFormRef = ref();
const loading = ref(false);

// 处理登录
const handleLogin = async () => {
  if (!loginFormRef.value) return;

  try {
    const valid = await loginFormRef.value.validate();
    if (!valid) return;

    loading.value = true;

    // 调用登录接口
    const response = await login({
      username: loginForm.username,
      password: loginForm.password,
    });

    if (response.code === 0) {
      // 登录成功
      const token = response.data.adminToken;

      // 存储token到vuex
      store.commit("setToken", token);

      // 如果选择记住密码，保存到本地存储
      if (loginForm.rememberPassword) {
        localStorage.setItem("rememberedUsername", loginForm.username);
        localStorage.setItem("rememberedPassword", loginForm.password);
      } else {
        // 清除记住的密码
        localStorage.removeItem("rememberedUsername");
        localStorage.removeItem("rememberedPassword");
      }

      ElMessage.success("登录成功");

      // 跳转到首页
      router.push("/home");
    } else {
      ElMessage.error(response.msg || "登录失败");
    }
  } catch (error) {
    console.error("登录失败:", error);
  } finally {
    loading.value = false;
  }
};

// 页面加载时检查是否有记住的密码
onMounted(() => {
  loginForm.username = "";
  loginForm.password = "";
  loginForm.rememberPassword = false;

  const rememberedUsername = localStorage.getItem("rememberedUsername");
  const rememberedPassword = localStorage.getItem("rememberedPassword");

  if (rememberedUsername && rememberedPassword) {
    loginForm.username = rememberedUsername;
    loginForm.password = rememberedPassword;
    loginForm.rememberPassword = true;
  }
});
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #3f4764 0%, #303750 100%);
  background-size: cover;
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h2 {
  color: #333;
  margin-bottom: 10px;
  font-size: 24px;
  font-weight: 600;
}

.login-header p {
  color: #666;
  font-size: 14px;
}

.login-form {
  width: 100%;
}

.remember-password {
  display: flex;
  justify-content: flex-start;
}

.login-btn {
  width: 100%;
  height: 45px;
  font-size: 16px;
  background: linear-gradient(135deg, #555d7e 0%, #424964 100%);
  border: none;
  border-radius: 6px;
}

.login-btn:hover {
  opacity: 0.9;
}

:deep(.el-input__wrapper) {
  border-radius: 6px;
}

:deep(.el-form-item) {
  margin-bottom: 20px;
}
</style>