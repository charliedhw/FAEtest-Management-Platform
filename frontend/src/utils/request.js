import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { clearAuth } from './auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 统一处理未登录/登录过期
const handleUnauthorized = () => {
  clearAuth()
  if (router.currentRoute.value.path !== '/login') {
    ElMessage.error('登录已过期，请重新登录')
    router.push('/login')
  }
}

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        handleUnauthorized()
        return Promise.reject(new Error(res.msg))
      }
      ElMessage.error(res.msg || '请求失败')
      return Promise.reject(new Error(res.msg))
    }
    return res
  },
  error => {
    // HTTP 状态码 401 (JWT过期/无效)
    if (error.response && error.response.status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
