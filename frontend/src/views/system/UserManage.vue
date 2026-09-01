<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="用户名/姓名" style="width:200px" clearable @keyup.enter="load" @clear="load" />
        <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
        <el-upload :show-file-list="false" :http-request="handleImport" accept=".xlsx,.xls" style="margin-left:auto">
          <el-button type="success"><el-icon><Upload /></el-icon>导入人员</el-button>
        </el-upload>
        <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>新增用户</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
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
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" @current-change="(p) => { query.pageNum = p; load() }" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserPage, saveUser, resetPassword, deleteUser, importUsers, getAllRoles, getUserRoleIds } from '../../api'
import { formatDateTime } from '../../utils/format'
import request from '../../utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const roles = ref([])
const query = ref({ pageNum: 1, pageSize: 10, keyword: '' })
const formVisible = ref(false)
const importResultVisible = ref(false)
const importResult = ref({})
const form = ref({ roleIds: [] })

const load = async () => {
  loading.value = true
  try {
    const res = await getUserPage(query.value)
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
const handleReset = async (row) => {
  await ElMessageBox.confirm(`确认重置【${row.realName}】的密码为默认密码？`, '提示', { type: 'warning' })
  await resetPassword(row.id); ElMessage.success('密码已重置为 Sugon@123')
}
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除用户【${row.realName}(${row.username})】？此操作不可恢复`, '警告', { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' })
  await deleteUser(row.id); ElMessage.success('已删除'); load()
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

onMounted(() => { load(); loadRoles() })
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 16px; align-items: center; gap: 8px; }
</style>
