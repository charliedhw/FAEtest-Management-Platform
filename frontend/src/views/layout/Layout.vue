<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo" @click="$router.push('/dashboard')">
        <span v-if="!isCollapse">测试项目管理平台</span>
        <span v-else>测</span>
      </div>
      <el-menu :default-active="$route.path" :collapse="isCollapse" router background-color="#001529" text-color="#a6adb4" active-text-color="#fff">
        <el-menu-item index="/dashboard"><el-icon><DataAnalysis /></el-icon><template #title>项目统计</template></el-menu-item>
        <el-menu-item index="/application"><el-icon><DocumentAdd /></el-icon><template #title>测试申请</template></el-menu-item>
        <el-menu-item index="/approval"><el-icon><Stamp /></el-icon><template #title>审批中心</template></el-menu-item>
        <el-menu-item index="/project"><el-icon><FolderOpened /></el-icon><template #title>项目清单</template></el-menu-item>
        <el-menu-item index="/asset"><el-icon><OfficeBuilding /></el-icon><template #title>资产中心</template></el-menu-item>
        <el-menu-item v-if="canSeeDailySummary" index="/report/daily"><el-icon><DataLine /></el-icon><template #title>日报</template></el-menu-item>
        <el-menu-item v-if="canSeeWeeklyReport" index="/report/weekly"><el-icon><Notebook /></el-icon><template #title>周报</template></el-menu-item>
        <el-sub-menu index="resource" v-if="canSeeResource">
          <template #title><el-icon><Box /></el-icon><span>资源管理</span></template>
          <el-menu-item index="/resource">资源池</el-menu-item>
          <el-menu-item index="/resource/loan">借用管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="system" v-if="userStore.hasRole('ADMIN')">
          <template #title><el-icon><Setting /></el-icon><span>系统管理</span></template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/group">用户组管理</el-menu-item>
          <el-menu-item index="/system/dict">字典管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse"><Fold v-if="!isCollapse" /><Expand v-else /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ $route.meta.title || '项目统计' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notify-badge">
            <el-icon :size="20" @click="notifyVisible = true" style="cursor:pointer"><Bell /></el-icon>
          </el-badge>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" style="background:#c41e3a">{{ userStore.realName?.charAt(0) || 'U' }}</el-avatar>
              <span class="username">{{ userStore.realName || userStore.username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 通知抽屉 -->
    <el-drawer v-model="notifyVisible" title="消息通知" size="400px">
      <el-tabs v-model="notifyTab" @tab-change="onTabChange">
        <el-tab-pane name="unread">
          <template #label>未读消息 <el-badge v-if="unreadCount > 0" :value="unreadCount" type="danger" /></template>
          <div style="margin-bottom:10px">
            <el-button size="small" @click="handleReadAll" :disabled="unreadCount === 0">全部已读</el-button>
          </div>
          <div v-for="msg in unreadList" :key="msg.id" class="notify-item unread clickable" @click="handleClickMsg(msg)">
            <div class="notify-title">
              <el-icon v-if="msg.jumpUrl" style="vertical-align:middle;margin-right:4px"><Link /></el-icon>
              {{ msg.title }}
            </div>
            <div class="notify-content">{{ msg.content }}</div>
            <div class="notify-time">{{ formatDateTime(msg.createTime) }}</div>
          </div>
          <el-empty v-if="unreadList.length === 0" description="暂无未读消息" />
        </el-tab-pane>
        <el-tab-pane name="history" label="历史消息">
          <div v-for="msg in historyList" :key="msg.id" class="notify-item" :class="{ clickable: !!msg.jumpUrl }" @click="handleClickMsg(msg)">
            <div class="notify-title">
              <el-icon v-if="msg.jumpUrl" style="vertical-align:middle;margin-right:4px"><Link /></el-icon>
              {{ msg.title }}
            </div>
            <div class="notify-content">{{ msg.content }}</div>
            <div class="notify-time">{{ formatDateTime(msg.createTime) }}</div>
          </div>
          <el-empty v-if="historyList.length === 0" description="暂无历史消息" />
        </el-tab-pane>
      </el-tabs>
    </el-drawer>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePwd">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'
import { getNotifyPage, getUnreadCount, markRead, markAllRead, changePassword } from '../../api'
import { formatDateTime } from '../../utils/format'

const router = useRouter()
const userStore = useUserStore()
const isCollapse = ref(false)
const notifyVisible = ref(false)
const notifyTab = ref('unread')
const unreadList = ref([])
const historyList = ref([])
const unreadCount = ref(0)
const pwdVisible = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '' })

// 资源管理模块: 测试审批组(APPROVER)、FAE负责人(FAE_LEADER)、资源管理员(RESOURCE_ADMIN)、管理员(ADMIN)可见
const canSeeResource = computed(() =>
  userStore.hasRole('APPROVER') || userStore.hasRole('FAE_LEADER')
  || userStore.hasRole('RESOURCE_ADMIN') || userStore.hasRole('ADMIN')
)

// 日报汇总: 审批组(APPROVER)、FAE负责人(FAE_LEADER)、管理员(ADMIN)可见
const canSeeDailySummary = computed(() =>
  userStore.hasRole('APPROVER') || userStore.hasRole('FAE_LEADER') || userStore.hasRole('ADMIN')
)

// 周报: FAE测试组(TESTER)及以上角色可见
const canSeeWeeklyReport = computed(() =>
  userStore.hasRole('TESTER') || userStore.hasRole('FAE_LEADER')
  || userStore.hasRole('APPROVER') || userStore.hasRole('ADMIN')
)

const loadNotify = async () => {
  // 未读消息
  const unreadRes = await getNotifyPage({ pageNum: 1, pageSize: 50, isRead: 0 })
  unreadList.value = unreadRes.data.records
  // 历史(已读)消息
  const historyRes = await getNotifyPage({ pageNum: 1, pageSize: 50, isRead: 1 })
  historyList.value = historyRes.data.records
  // 未读数
  const countRes = await getUnreadCount()
  unreadCount.value = countRes.data
}

const onTabChange = () => { loadNotify() }

// 点击消息: 标记已读 + 跳转到对应流程页面
const handleClickMsg = async (msg) => {
  if (msg.isRead === 0) {
    await markRead(msg.id)
  }
  if (msg.jumpUrl) {
    notifyVisible.value = false
    router.push(msg.jumpUrl)
  }
  loadNotify()
}

const handleReadAll = async () => {
  await markAllRead()
  loadNotify()
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (cmd === 'password') {
    pwdVisible.value = true
  }
}

const handleChangePwd = async () => {
  await changePassword(pwdForm.value)
  ElMessage.success('密码修改成功，请重新登录')
  pwdVisible.value = false
  userStore.logout()
  router.push('/login')
}

onMounted(loadNotify)
</script>

<style scoped>
.layout-container { height: 100vh; }
.sidebar { background: #001529; transition: width 0.3s; overflow: hidden; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold; cursor: pointer; background: #002140; white-space: nowrap; }
.sidebar :deep(.el-menu) { border-right: none; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; box-shadow: 0 1px 4px rgba(0,21,41,0.08); }
.header-left { display: flex; align-items: center; gap: 16px; }
.collapse-btn { font-size: 20px; cursor: pointer; }
.header-right { display: flex; align-items: center; gap: 20px; }
.user-info { display: flex; align-items: center; gap: 8px; cursor: pointer; }
.username { font-size: 14px; }
.main { background: #f0f2f5; padding: 16px; overflow-y: auto; }
.notify-item { padding: 12px; border-bottom: 1px solid #f0f0f0; cursor: pointer; transition: background 0.2s; }
.notify-item.unread { background: #fff7e6; }
.notify-item.clickable:hover { background: #e6f4ff; }
.notify-item.clickable .notify-title { color: #409eff; }
.notify-title { font-weight: bold; margin-bottom: 4px; }
.notify-content { font-size: 13px; color: #666; margin-bottom: 4px; }
.notify-time { font-size: 12px; color: #999; }
</style>
