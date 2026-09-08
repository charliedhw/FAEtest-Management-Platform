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
    // blob 响应(文件下载)直接放行，由调用方处理
    if (response.config.responseType === 'blob') {
      return response
    }
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
  async error => {
    // HTTP 状态码 401 (JWT过期/无效)
    if (error.response && error.response.status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    // blob 响应的业务错误：解析 JSON 提示
    if (error.response && error.response.config?.responseType === 'blob' && error.response.data instanceof Blob) {
      try {
        const text = await error.response.data.text()
        const json = JSON.parse(text)
        ElMessage.error(json.msg || '请求失败')
      } catch {
        ElMessage.error('请求失败')
      }
      return Promise.reject(error)
    }
    ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
