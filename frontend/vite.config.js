import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      'vue': 'vue/dist/vue.esm-bundler.js'
    }
  },
  build: {
    sourcemap: false,
  },
  server: {
    // ===== 新增代理配置 =====
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      }
    },
    fs: {
      allow: ['..']
    }
  },
  optimizeDeps: {
    exclude: ['flvjs']
  }
})