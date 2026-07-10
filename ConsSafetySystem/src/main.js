// src/main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router' // 引入路由配置
import './assets/tailwind.css'
import store from './store'
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css'

const app = createApp(App)
app.use(router)
app.use(store)
app.use(ElementPlus);
app.mount('#app')
