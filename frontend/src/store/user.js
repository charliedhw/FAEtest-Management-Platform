import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || '',
    realName: localStorage.getItem('realName') || '',
    roles: JSON.parse(localStorage.getItem('roles') || '[]')
  }),
  getters: {
    isLogin: (state) => !!state.token,
    hasRole: (state) => (code) => state.roles.includes(code)
  },
  actions: {
    setLogin(data) {
      this.token = data.token
      this.userId = data.userId
      this.username = data.username
      this.realName = data.realName
      this.roles = data.roles || []
      localStorage.setItem('token', data.token)
      localStorage.setItem('userId', data.userId)
      localStorage.setItem('username', data.username)
      localStorage.setItem('realName', data.realName || '')
      localStorage.setItem('roles', JSON.stringify(data.roles || []))
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.realName = ''
      this.roles = []
      // 只清除认证相关,保留个性化设置(dashboard_slots等)
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('realName')
      localStorage.removeItem('roles')
    }
  }
})
