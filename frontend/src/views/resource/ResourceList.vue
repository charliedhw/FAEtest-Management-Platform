<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <el-input v-model="query.keyword" placeholder="资源名称/编号" style="width:200px" clearable @keyup.enter="load" @clear="load" />
          <el-select v-model="query.type" placeholder="资源类型" clearable style="width:140px;margin-left:8px">
            <el-option v-for="d in dicts.resource_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
          <el-select v-model="query.status" placeholder="状态" clearable style="width:120px;margin-left:8px">
            <el-option label="空闲" value="IDLE" /><el-option label="占用" value="IN_USE" /><el-option label="维护" value="MAINTENANCE" />
          </el-select>
          <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
        </div>
        <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>新增资源</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="resourceCode" label="资源编号" width="120" />
        <el-table-column prop="serialNo" label="设备序列号" width="140" show-overflow-tooltip />
        <el-table-column prop="resourceName" label="资源名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="resourceType" label="类型" width="110" />
        <el-table-column prop="hardwareConfig" label="硬件配置" min-width="200" show-overflow-tooltip />
        <el-table-column prop="factoryPrice" label="出厂价(元)" width="110" />
        <el-table-column prop="deptName" label="所属部门" width="120" />
        <el-table-column label="使用状态" width="90">
          <template #default="{ row }">
            <el-tag :type="{ IDLE: 'success', IN_USE: 'warning', MAINTENANCE: 'info' }[row.status]">{{ { IDLE: '空闲', IN_USE: '占用', MAINTENANCE: '维护' }[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上下线" width="90">
          <template #default="{ row }">
            <el-tag :type="row.onlineStatus === 'ONLINE' ? 'success' : 'info'">{{ row.onlineStatus === 'ONLINE' ? '已上线' : '已下线' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.onlineStatus !== 'ONLINE'" link type="success" size="small" @click="handleOnline(row)">上线</el-button>
            <el-button v-else link type="warning" size="small" @click="handleOffline(row)">下线</el-button>
            <el-button v-if="row.status === 'IDLE'" link type="success" size="small" @click="openBorrow(row)">借出</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" @current-change="(p) => { query.pageNum = p; load() }" />
    </el-card>

    <!-- 资源表单 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑资源' : '新增资源'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="资源编号"><el-input v-model="form.resourceCode" /></el-form-item>
        <el-form-item label="设备序列号"><el-input v-model="form.serialNo" placeholder="设备SN序列号" /></el-form-item>
        <el-form-item label="资源名称"><el-input v-model="form.resourceName" /></el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="form.resourceType" style="width:100%">
            <el-option v-for="d in dicts.resource_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="硬件配置"><el-input v-model="form.hardwareConfig" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="出厂价"><el-input-number v-model="form.factoryPrice" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="所属部门"><el-input v-model="form.deptName" /></el-form-item>
        <el-form-item label="位置"><el-input v-model="form.location" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 借出 -->
    <el-dialog v-model="borrowVisible" title="资源借出" width="500px">
      <el-form label-width="100px">
        <el-form-item label="资源">{{ currentResource.resourceName }}</el-form-item>
        <el-form-item label="关联项目" required>
          <el-select v-model="borrowForm.projectId" placeholder="选择关联的测试项目" filterable style="width:100%">
            <el-option v-for="p in projectList" :key="p.id" :label="`${p.projectNo} ${p.projectName}`" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="应还时间"><el-date-picker v-model="borrowForm.expectReturnTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="borrowForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="borrowVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBorrow">确认借出</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getResourcePage, saveResource, deleteResource, borrowResource, onlineResource, offlineResource, getAllDict, getProjectPage } from '../../api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dicts = ref({})
const projectList = ref([])
const query = ref({ pageNum: 1, pageSize: 10, type: '', status: '', keyword: '' })
const formVisible = ref(false)
const form = ref({})
const borrowVisible = ref(false)
const currentResource = ref({})
const borrowForm = ref({ resourceId: null, projectId: null, expectReturnTime: null, remark: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await getResourcePage(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const loadProjects = async () => {
  // 借出关联项目: 加载所有项目(管理员/审批组等看全部)
  const res = await getProjectPage({ pageNum: 1, pageSize: 200 })
  projectList.value = res.data.records
}

const openForm = (row) => { form.value = row ? { ...row } : {}; formVisible.value = true }
const handleSave = async () => { await saveResource(form.value); ElMessage.success('保存成功'); formVisible.value = false; load() }
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该资源？', '提示', { type: 'warning' })
  await deleteResource(row.id); ElMessage.success('已删除'); load()
}
const handleOnline = async (row) => {
  await onlineResource(row.id); ElMessage.success('已上线，销售/售前可在资产中心查看'); load()
}
const handleOffline = async (row) => {
  await ElMessageBox.confirm('下线后销售/售前将无法看到该资产，确认下线？', '提示', { type: 'warning' })
  await offlineResource(row.id); ElMessage.success('已下线'); load()
}
const openBorrow = (row) => {
  currentResource.value = row
  borrowForm.value = { resourceId: row.id, projectId: null, expectReturnTime: null, remark: '' }
  borrowVisible.value = true
}
const handleBorrow = async () => {
  if (!borrowForm.value.projectId) {
    ElMessage.warning('请选择关联的测试项目')
    return
  }
  await borrowResource(borrowForm.value); ElMessage.success('借出成功'); borrowVisible.value = false; load()
}

onMounted(async () => {
  load()
  loadProjects()
  const res = await getAllDict()
  dicts.value = res.data
})
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>
