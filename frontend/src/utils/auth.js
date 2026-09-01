/**
 * 解析 JWT token, 判断是否有效(未过期)
 * @returns true=有效 false=无效或已过期
 */
export function isTokenValid(token) {
  if (!token) return false
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return false
    // base64url 解码 payload
    const payload = JSON.parse(decodeURIComponent(atob(parts[1].replace(/-/g, '+').replace(/_/g, '/')).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')))
    if (!payload.exp) return true // 无过期时间视为有效
    const now = Math.floor(Date.now() / 1000)
    return payload.exp > now
  } catch (e) {
    return false
  }
}

/**
 * 清除登录缓存
 */
export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  localStorage.removeItem('realName')
  localStorage.removeItem('roles')
}
