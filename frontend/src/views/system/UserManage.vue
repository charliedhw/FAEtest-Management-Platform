<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="用户名/姓名" style="width:180px" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.groupId" placeholder="按分组筛选" clearable style="width:160px" @change="load">
          <el-option v-for="g in groupList" :key="g.id" :label="g.groupName" :value="g.id" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <template v-if="selectedUsers.length > 0">
          <el-divider direction="vertical" />
          <span style="font-size:13px;color:#606266">已选 {{ selectedUsers.length }} 人</span>
          <el-button type="warning" size="small" @click="handleBatchReset">批量修改密码</el-button>
          <el-button type="primary" size="small" @click="openGroupSelect">加入分组</el-button>
          <el-button type="danger" size="small" @click="handleBatchDelete">批量删除</el-button>
        </template>
        <el-upload :show-file-list="false" :http-request="handleImport" accept=".xlsx,.xls" style="margin-left:auto">
          <el-button type="success"><el-icon><Upload /></el-icon>导入人员</el-button>
        </el-upload>
        <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>新增用户</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe @selection-change="(rows) => selectedUsers = rows">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="warning" size="small" @click="handleReset(row)">重置密码</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,sizes,prev,pager,next" :total="total" :page-sizes="[10,20,50]" v-model:page-size="query.pageSize" :current-page="query.pageNum" @size-change="(s) => { query.pageSize = s; query.pageNum = 1; load() }" @current-change="(p) => { query.pageNum = p; load() }" />
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码"><el-input v-model="form.password" placeholder="默认 Sugon@123" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleIds" multiple style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status"><el-radio :value="1">启用</el-radio><el-radio :value="0">停用</el-radio></el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 单个/批量自定义密码重置 -->
    <el-dialog v-model="resetVisible" :title="resetMode === 'batch' ? '批量修改密码' : '重置密码'" width="480px" :close-on-click-modal="false">
      <el-alert
        :title="resetMode === 'batch' ? `将为选中的 ${resetTargetIds.length} 个用户设置相同的新密码` : `正在重置【${resetTargetName}】的密码`"
        type="warning"
        :closable="false"
        style="margin-bottom:16px"
      />
      <el-form label-width="90px" @submit.prevent>
        <el-form-item label="新密码" required>
          <el-input v-model="resetForm.newPassword" type="password" show-password autocomplete="new-password" placeholder="至少8个字符" />
        </el-form-item>
        <el-form-item label="确认密码" required>
          <el-input v-model="resetForm.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="请再次输入新密码" @keyup.enter="handleConfirmReset" />
        </el-form-item>
      </el-form>
      <div class="password-tip">默认为 Sugon@123，可直接修改；UTF-8 编码后不超过 72 字节。</div>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleConfirmReset">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 导入结果 -->
    <el-dialog v-model="importResultVisible" title="导入结果" width="600px">
      <el-alert type="success" :closable="false" style="margin-bottom:10px">
        成功创建 {{ importResult.created }} 个账号，跳过 {{ importResult.skipped }} 个（已存在）
      </el-alert>
      <div v-if="importResult.errors && importResult.errors.length > 0" style="margin-bottom:10px">
        <el-alert type="warning" :closable="false" title="以下记录存在问题:" />
        <div v-for="(e, i) in importResult.errors" :key="i" style="font-size:12px;color:#e6a23c;padding:2px 0">{{ e }}</div>
      </div>
      <div style="max-height:300px;overflow-y:auto;font-size:12px;color:#666">
        <div v-for="(d, i) in importResult.details" :key="i" style="padding:2px 0">{{ d }}</div>
      </div>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">确定</el-button>
      </template>
    </el-dialog>

    <!-- 加入分组 -->
    <el-dialog v-model="groupSelectVisible" title="加入分组" width="420px">
      <div style="margin-bottom:10px;font-size:13px;color:#606266">
        将选中的 {{ selectedUsers.length }} 人添加到指定分组：
      </div>
      <el-select v-model="targetGroupId" placeholder="请选择分组" style="width:100%">
        <el-option v-for="g in groupList" :key="g.id" :label="g.groupName" :value="g.id" />
      </el-select>
      <template #footer>
        <el-button @click="groupSelectVisible = false">取消</el-button>
        <el-button type="primary" :loading="groupSaving" @click="handleAddToGroup">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, saveUser, resetPassword, deleteUser, importUsers, getAllRoles, getUserRoleIds, batchDeleteUsers, batchResetPassword, getGroupList, addGroupMembers } from '../../api'
import { formatDateTime } from '../../utils/format'
import request from '../../utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const roles = ref([])
const query = ref({ pageNum: 1, pageSize: 10, keyword: '', groupId: null })
const formVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref({})
const form = ref({ roleIds: [] })
// 批量操作
const selectedUsers = ref([])
const groupSelectVisible = ref(false)
const groupList = ref([])
const targetGroupId = ref(null)
const groupSaving = ref(false)
// 单个/批量密码重置
const resetVisible = ref(false)
const resetting = ref(false)
const resetMode = ref('single')
const resetTargetIds = ref([])
const resetTargetName = ref('')
const resetForm = ref({ newPassword: 'Sugon@123', confirmPassword: 'Sugon@123' })

const load = async () => {
  loading.value = true
  try {
    const params = { pageNum: query.value.pageNum, pageSize: query.value.pageSize, keyword: query.value.keyword }
    if (query.value.groupId) params.groupId = query.value.groupId
    const res = await getUserPage(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const loadRoles = async () => {
  // 从后端加载真实角色列表
  const res = await getAllRoles()
  roles.value = res.data
}

const openForm = async (row) => {
  if (row) {
    // 编辑: 回填用户已有的角色
    const res = await getUserRoleIds(row.id)
    form.value = { ...row, roleIds: res.data || [] }
  } else {
    form.value = { roleIds: [], status: 1 }
  }
  formVisible.value = true
}
const handleSave = async () => { await saveUser(form.value); ElMessage.success('保存成功'); formVisible.value = false; load() }
const openResetDialog = (mode, users) => {
  resetMode.value = mode
  resetTargetIds.value = users.map(u => u.id)
  resetTargetName.value = mode === 'single' ? users[0].realName : ''
  resetForm.value = { newPassword: 'Sugon@123', confirmPassword: 'Sugon@123' }
  resetVisible.value = true
}
const handleReset = (row) => openResetDialog('single', [row])
const handleConfirmReset = async () => {
  const password = resetForm.value.newPassword
  if (!password || password.length < 8) {
    ElMessage.warning('自定义密码不能少于8个字符')
    return
  }
  if (new TextEncoder().encode(password).length > 72) {
    ElMessage.warning('密码过长，UTF-8编码后不能超过72字节')
    return
  }
  if (password !== resetForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  resetting.value = true
  try {
    if (resetMode.value === 'single') {
      await resetPassword(resetTargetIds.value[0], password)
      ElMessage.success('密码已重置')
    } else {
      const res = await batchResetPassword(resetTargetIds.value, password)
      ElMessage.success(`已重置 ${res.data} 个用户密码`)
    }
    resetVisible.value = false
  } finally {
    resetting.value = false
  }
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除用户【${row.realName}(${row.username})】？此操作不可恢复`, '警告', { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' })
  await deleteUser(row.id); ElMessage.success('已删除'); load()
}

// 批量删除
const handleBatchDelete = async () => {
  const names = selectedUsers.value.map(u => u.realName).join('、')
  await ElMessageBox.confirm(
    `确认删除选中的 ${selectedUsers.value.length} 个用户（${names}）？此操作不可恢复，admin账号和当前登录人将自动跳过`,
    '警告', { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' })
  const res = await batchDeleteUsers(selectedUsers.value.map(u => u.id))
  ElMessage.success(`已删除 ${res.data} 个用户`)
  load()
}

// 批量修改密码
const handleBatchReset = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择用户')
    return
  }
  openResetDialog('batch', selectedUsers.value)
}

// 加入分组
const openGroupSelect = async () => {
  const res = await getGroupList()
  groupList.value = res.data || []
  targetGroupId.value = null
  groupSelectVisible.value = true
}
const handleAddToGroup = async () => {
  if (!targetGroupId.value) { ElMessage.warning('请选择分组'); return }
  groupSaving.value = true
  try {
    const gname = groupList.value.find(g => g.id === targetGroupId.value)?.groupName
    const res = await addGroupMembers(targetGroupId.value, selectedUsers.value.map(u => u.id))
    ElMessage.success(`已将 ${res.data} 人加入分组「${gname}」（已在组内的自动跳过）`)
    groupSelectVisible.value = false
  } finally {
    groupSaving.value = false
  }
}

// 导入人员
const handleImport = async ({ file }) => {
  const hide = ElMessage.info({ message: '正在导入...', duration: 0 })
  try {
    const res = await importUsers(file)
    importResult.value = res.data
    importResultVisible.value = true
    load()
  } finally {
    hide.close()
  }
}

const loadGroups = async () => {
  const res = await getGroupList()
  groupList.value = res.data || []
}

onMounted(() => { load(); loadRoles(); loadGroups() })
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 16px; align-items: center; gap: 8px; }
.password-tip { color: #909399; font-size: 12px; margin: -4px 0 0 90px; }
</style>
