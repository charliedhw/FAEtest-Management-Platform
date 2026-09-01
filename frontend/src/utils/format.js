/**
 * 格式化日期时间: 把 ISO 格式(带T)转为 'YYYY-MM-DD HH:mm:ss'
 */
export function formatDateTime(val) {
  if (!val) return ''
  if (typeof val !== 'string') return val
  // 2026-08-11T18:07:28 -> 2026-08-11 18:07:28
  return val.replace('T', ' ').substring(0, 19)
}

/**
 * 格式化日期: 'YYYY-MM-DD'
 */
export function formatDate(val) {
  if (!val) return ''
  if (typeof val !== 'string') return val
  return val.substring(0, 10)
}

/**
 * 格式化测试类型: '["AI","CPU"]' -> 'AI、CPU'
 */
export function formatTestType(val) {
  if (!val) return ''
  if (typeof val !== 'string') return val
  try {
    const arr = JSON.parse(val)
    if (Array.isArray(arr)) return arr.join('、')
    return val
  } catch {
    return val
  }
}
