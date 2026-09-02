import request from '../utils/request'

// 认证
export const login = (data) => request.post('/auth/login', data)

// 用户
export const getUserPage = (params) => request.get('/user/page', { params })
export const saveUser = (data) => request.post('/user/save', data)
export const changePassword = (data) => request.post('/user/changePassword', data)
export const resetPassword = (userId) => request.post(`/user/resetPassword/${userId}`)
export const listUserByRole = (roleCode) => request.get('/user/listByRole', { params: { roleCode } })
export const listAllUser = () => request.get('/user/listAll')
export const getUserInfo = () => request.get('/user/info')
export const deleteUser = (userId) => request.delete(`/user/${userId}`)
export const batchDeleteUsers = (userIds) => request.post('/user/batchDelete', userIds)
export const batchResetPassword = (userIds) => request.post('/user/batchResetPassword', userIds)
export const importUsers = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const getAllRoles = () => request.get('/user/roles')
export const getUserRoleIds = (userId) => request.get(`/user/roleIds/${userId}`)

// 用户组
export const getGroupList = () => request.get('/group/list')
export const saveGroup = (data) => request.post('/group/save', data)
export const deleteGroup = (id) => request.delete(`/group/${id}`)
export const getGroupMembers = (groupId) => request.get(`/group/members/${groupId}`)
export const setGroupMembers = (groupId, userIds) => request.post(`/group/members/${groupId}`, userIds)
export const addGroupMembers = (groupId, userIds) => request.post(`/group/addMembers/${groupId}`, userIds)
export const setGroupLeader = (data) => request.post('/group/leader', data)

// 申请
export const submitApplication = (data) => request.post('/application/submit', data)
export const saveDraft = (data) => request.post('/application/draft', data)
export const approveApplication = (data) => request.post('/application/approve', data)
export const assignApplication = (data) => request.post('/application/assign', data)
export const withdrawApplication = (id) => request.post(`/application/withdraw/${id}`)
export const getApplicationPage = (params) => request.get('/application/page', { params })
export const getTodoList = (params) => request.get('/application/todo', { params })
export const getApplicationDetail = (id) => request.get(`/application/${id}`)
export const importWord = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/application/importWord', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// 项目
export const getProjectPage = (params) => request.get('/project/page', { params })
export const getProjectDetail = (id) => request.get(`/project/${id}`)
export const updateProject = (data) => request.post('/project/update', data)
export const updateProjectStatus = (data) => request.post('/project/updateStatus', data)
export const updateProjectBid = (data) => request.post('/project/updateBid', data)
export const deleteProject = (id) => request.delete(`/project/${id}`)

// 进展
export const addProgress = (data) => request.post('/progress/add', data)
export const updateProgress = (data) => request.post('/progress/update', data)
export const deleteProgress = (id) => request.delete(`/progress/${id}`)
export const listProgress = (projectId) => request.get(`/progress/list/${projectId}`)

// 日报汇总
export const getProgressSummary = (params) => request.get('/progress/summary', { params })

// 周报
export const saveWeeklyReport = (data) => request.post('/weeklyReport/save', data)
export const deleteWeeklyReport = (id) => request.delete(`/weeklyReport/${id}`)
export const getWeeklyReportPage = (params) => request.get('/weeklyReport/page', { params })
export const getWeeklyReportDetail = (id) => request.get(`/weeklyReport/${id}`)
export const getMyLatestReport = () => request.get('/weeklyReport/myLatest')
export const getCurrentWeek = () => request.get('/weeklyReport/currentWeek')
export const getWeeklyPersonSummary = (params) => request.get('/weeklyReport/personSummary', { params })

// 阶段任务
export const addStage = (data) => request.post('/stage/add', data)
export const updateStage = (data) => request.post('/stage/update', data)
export const deleteStage = (id) => request.delete(`/stage/${id}`)
export const listStage = (projectId) => request.get(`/stage/list/${projectId}`)
export const getProjectProgress = (projectId) => request.get(`/stage/progress/${projectId}`)

// 资源
export const getResourcePage = (params) => request.get('/resource/page', { params })
export const saveResource = (data) => request.post('/resource/save', data)
export const deleteResource = (id) => request.delete(`/resource/${id}`)
export const onlineResource = (id) => request.post(`/resource/online/${id}`)
export const offlineResource = (id) => request.post(`/resource/offline/${id}`)
export const getAssetPage = (params) => request.get('/resource/asset/page', { params })
export const borrowResource = (data) => request.post('/resource/borrow', data)
export const returnResource = (loanId) => request.post(`/resource/return/${loanId}`)
export const getLoanPage = (params) => request.get('/resource/loan/page', { params })
export const getOverdueList = () => request.get('/resource/overdue')
export const getResourceStats = () => request.get('/resource/stats')

// 统计
export const getDashboard = () => request.get('/stats/dashboard')
export const getDimensionStats = (dimension) => request.get('/stats/dimension', { params: { dimension } })
export const getTimeDimension = (timeUnit) => request.get('/stats/timeDimension', { params: { timeUnit } })

// 报告
export const uploadReport = (projectId, file) => {
  const formData = new FormData()
  formData.append('projectId', projectId)
  formData.append('file', file)
  return request.post('/report/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const listReport = (projectId) => request.get(`/report/list/${projectId}`)
export const deleteReport = (id) => request.delete(`/report/${id}`)

// 通知
export const getNotifyPage = (params) => request.get('/notify/page', { params })
export const getUnreadCount = () => request.get('/notify/unreadCount')
export const markRead = (id) => request.post(`/notify/read/${id}`)
export const markAllRead = () => request.post('/notify/readAll')

// 字典
export const getDictByType = (type) => request.get(`/dict/type/${type}`)
export const getAllDict = () => request.get('/dict/all')
export const saveDict = (data) => request.post('/dict/save', data)
export const deleteDict = (id) => request.delete(`/dict/${id}`)
