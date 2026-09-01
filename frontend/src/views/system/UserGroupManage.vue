<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <span style="font-weight:bold">用户组管理</span>
        <el-button type="primary" style="margin-left:auto" @click="openForm()"><el-icon><Plus /></el-icon>新增用户组</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="groupCode" label="组编码" width="160" />
        <el-table-column prop="groupName" label="组名称" width="180" />
        <el-table-column prop="leaderName" label="负责人" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.leaderName" type="success" size="small">{{ row.leaderName }}</el-tag>
            <span v-else style="color:#999">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openMembers(row)">成员管理</el-button>
            <el-button link type="warning" size="small" @click="openLeader(row)">设负责人</el-button>
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 组表单 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户组' : '新增用户组'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="组编码"><el-input v-model="form.groupCode" :disabled="!!form.id" placeholder="如 SALES_GROUP" /></el-form-item>
        <el-form-item label="组名称"><el-input v-model="form.groupName" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 成员管理 -->
    <el-dialog v-model="memberVisible" :title="`成员管理 - ${currentGroup.groupName}`" width="600px">
      <el-transfer v-model="memberIds" :data="allUserOptions" :titles="['所有用户', '组成员']" :props="{ key: 'id', label: 'realName' }" filterable />
      <template #footer>
        <el-button @click="memberVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveMembers">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设负责人 -->
    <el-dialog v-model="leaderVisible" :title="`设置负责人 - ${currentGroup.groupName}`" width="400px">
      <el-select v-model="selectedLeader" placeholder="选择负责人" filterable style="width:100%">
        <el-option v-for="u in allUsers" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="leaderVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveLeader">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGroupList, saveGroup, deleteGroup, getGroupMembers, setGroupMembers, setGroupLeader, listAllUser } from '../../api'

const loading = ref(false)
const list = ref([])
const allUsers = ref([])
const formVisible = ref(false)
const form = ref({})
const memberVisible = ref(false)
const leaderVisible = ref(false)
const currentGroup = ref({})
const memberIds = ref([])
const selectedLeader = ref(null)
const allUserOptions = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await getGroupList()
    list.value = res.data
  } finally { loading.value = false }
}

const loadUsers = async () => {
  const res = await listAllUser()
  allUsers.value = res.data
  allUserOptions.value = res.data.map(u => ({ id: u.id, realName: u.realName }))
}

const openForm = (row) => { form.value = row ? { ...row } : {}; formVisible.value = true }
const handleSave = async () => { await saveGroup(form.value); ElMessage.success('保存成功'); formVisible.value = false; load() }
const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除用户组【${row.groupName}】？`, '警告', { type: 'warning' })
  await deleteGroup(row.id); ElMessage.success('已删除'); load()
}

const openMembers = async (row) => {
  currentGroup.value = row
  const res = await getGroupMembers(row.id)
  memberIds.value = res.data.map(u => u.id)
  memberVisible.value = true
}
const handleSaveMembers = async () => {
  await setGroupMembers(currentGroup.value.id, memberIds.value)
  ElMessage.success('成员已更新'); memberVisible.value = false
}

const openLeader = (row) => { currentGroup.value = row; selectedLeader.value = row.leaderId; leaderVisible.value = true }
const handleSaveLeader = async () => {
  await setGroupLeader({ groupId: currentGroup.value.id, leaderId: selectedLeader.value })
  ElMessage.success('负责人已设置'); leaderVisible.value = false; load()
}

onMounted(() => { load(); loadUsers() })
</script>

<style scoped>
.toolbar { display: flex; align-items: center; margin-bottom: 16px; }
</style>
