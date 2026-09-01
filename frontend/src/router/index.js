import { createRouter, createWebHistory } from 'vue-router'
import { isTokenValid, clearAuth } from '../utils/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/login/Login.vue') },
  {
    path: '/',
    component: () => import('../views/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/dashboard/Dashboard.vue'), meta: { title: '项目统计' } },
      { path: 'application', name: 'Application', component: () => import('../views/application/ApplicationList.vue'), meta: { title: '测试申请' } },
      { path: 'application/create', name: 'ApplicationCreate', component: () => import('../views/application/ApplicationForm.vue'), meta: { title: '发起申请' } },
      { path: 'approval', name: 'Approval', component: () => import('../views/application/ApprovalCenter.vue'), meta: { title: '审批中心' } },
      { path: 'project', name: 'Project', component: () => import('../views/project/ProjectList.vue'), meta: { title: '项目清单' } },
      { path: 'project/:id', name: 'ProjectDetail', component: () => import('../views/project/ProjectDetail.vue'), meta: { title: '项目详情' } },
      { path: 'report/daily', name: 'DailySummary', component: () => import('../views/report/DailySummary.vue'), meta: { title: '日报汇总' } },
      { path: 'report/weekly', name: 'WeeklyReport', component: () => import('../views/report/WeeklyReport.vue'), meta: { title: '周报' } },
      { path: 'resource', name: 'Resource', component: () => import('../views/resource/ResourceList.vue'), meta: { title: '资源管理' } },
      { path: 'resource/loan', name: 'Loan', component: () => import('../views/resource/LoanList.vue'), meta: { title: '借用管理' } },
      { path: 'asset', name: 'AssetCenter', component: () => import('../views/resource/AssetCenter.vue'), meta: { title: '资产中心' } },
      { path: 'system/user', name: 'UserManage', component: () => import('../views/system/UserManage.vue'), meta: { title: '用户管理' } },
      { path: 'system/group', name: 'UserGroupManage', component: () => import('../views/system/UserGroupManage.vue'), meta: { title: '用户组管理' } },
      { path: 'system/dict', name: 'DictManage', component: () => import('../views/system/DictManage.vue'), meta: { title: '字典管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  // token 不存在 或 已过期 -> 清除缓存,跳登录页
  if (to.path !== '/login') {
    if (!token || !isTokenValid(token)) {
      clearAuth()
      next('/login')
      return
    }
  }
  // 已登录访问登录页 -> 跳首页
  if (to.path === '/login' && token && isTokenValid(token)) {
    next('/dashboard')
    return
  }
  // 资源管理模块权限校验
  if (to.path.startsWith('/resource')) {
    const roles = JSON.parse(localStorage.getItem('roles') || '[]')
    const allowed = ['APPROVER', 'FAE_LEADER', 'RESOURCE_ADMIN', 'ADMIN']
    const hasPerm = roles.some(r => allowed.includes(r))
    if (!hasPerm) {
      next('/dashboard')
      return
    }
  }
  // 日报汇总：审批组/管理员/FAE负责人可见
  if (to.path === '/report/daily') {
    const roles = JSON.parse(localStorage.getItem('roles') || '[]')
    const allowed = ['APPROVER', 'FAE_LEADER', 'ADMIN']
    if (!roles.some(r => allowed.includes(r))) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router
