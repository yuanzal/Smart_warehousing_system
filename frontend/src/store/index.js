import { createStore } from 'vuex'

export default createStore({
  state: () => ({
    role: localStorage.getItem('role') ? JSON.parse(localStorage.getItem('role')) : [],
    user: '',
    token: localStorage.getItem('token') ? localStorage.getItem('token') : '',
    imgShowRoad: import.meta.env.VITE_APP_Address,  // 注意环境变量访问方式变化
    fileUploadRoad: import.meta.env.VITE_APP_Address,
    mutiFile: '',
  }),
  getters: {
    getStorage(state) {
      if (!state.token) {
        state.token = localStorage.getItem('token')
        return state.token
      }
      return state.token
    },
  },
  mutations: {
    setToken(state, value) {
      state.token = value
      localStorage.setItem('token', value)
    },
  },
  actions: {
    // 可以在这里添加异步操作
  },
  modules: {
    // 可以在这里添加模块
  }
})
