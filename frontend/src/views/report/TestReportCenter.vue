<template>
  <div>
    <el-card shadow="never" style="margin-bottom:16px">
      <template #header>
        <div class="header-row">
          <span>测试报告</span>
          <span class="header-tip">完成项目前须填写测试结论并上传至少一份报告</span>
        </div>
      </template>
      <el-form inline @submit.prevent>
        <el-form-item label="项目">
          <el-input v-model="filters.keyword" placeholder="项目名称/编号/客户/SPM号" clearable style="width:240px" @keyup.enter="search" @clear="search" />
        </el-form-item>
        <el-form-item label="测试人员">
          <el-select v-model="filters.testerId" placeholder="全部人员" clearable filterable style="width:150px">
            <el-option v-for="user in testers" :key="user.id" :label="user.realName" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="测试类型">
          <el-select v-model="filters.testType" placeholder="全部类型" clearable style="width:160px">
            <el-option v-for="item in dicts.test_type || []" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备类型">
          <el-select v-model="filters.deviceType" placeholder="全部设备" clearable filterable style="width:170px">
            <el-option v-for="item in dicts.device_type || []" :key="item.dictValue" :label="item.dictLabel" :value="item.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search"><el-icon><Search /></el-icon>查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="header-row">
          <span>报告列表</span>
          <div class="batch-actions">
            <span v-if="selectedReports.length" class="selected-tip">已选 {{ selectedReports.length }} 份</span>
            <el-button type="primary" :loading="batchDownloading" :disabled="selectedReports.length === 0" @click="handleBatchDownload">
              <el-icon><Download /></el-icon>批量下载
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe @selection-change="(rows) => selectedReports = rows">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="fileName" label="报告文件" min-width="220" show-overflow-tooltip />
        <el-table-column label="项目名称" min-width="230" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push(`/project/${row.projectId}`)">{{ row.projectName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="projectNo" label="项目编号" width="150" show-overflow-tooltip />
        <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="testerNames" label="测试人员" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.testerNames || '—' }}</template>
        </el-table-column>
        <el-table-column label="测试类型" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ formatTestType(row.testType) || '—' }}</template>
        </el-table-column>
        <el-table-column prop="deviceType" label="设备类型" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">{{ row.deviceType || '—' }}</template>
        </el-table-column>
        <el-table-column label="项目状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.projectStatus)" size="small">{{ statusMap[row.projectStatus] || row.projectStatus || '—' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="65" align="center" />
        <el-table-column label="大小" width="90" align="right">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploadByName" label="上传人" width="90" />
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDownload(row)">下载</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无测试报告" /></template>
      </el-table>
      <el-pagination
        style="margin-top:16px;justify-content:flex-end"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :current-page="pageNum"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchDownloadReports, downloadReport, getAllDict, getTestReportPage, listUserByRole } from '../../api'
import { formatDateTime, formatTestType } from '../../utils/format'

defineOptions({ name: 'TestReportCenter' })

const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const selectedReports = ref([])
const batchDownloading = ref(false)
const testers = ref([])
const dicts = ref({})
const filters = reactive({ keyword: '', testerId: null, testType: '', deviceType: '' })
let requestSeq = 0

const statusMap = { NOT_START: '未开始', IN_PROGRESS: '进行中', PAUSED: '暂停', COMPLETED: '已完成', CLOSED: '关闭', REJECTED: '已驳回' }
const statusType = (status) => ({ COMPLETED: 'success', IN_PROGRESS: 'warning', PAUSED: 'warning', REJECTED: 'danger' }[status] || 'info')

const load = async () => {
  const seq = ++requestSeq
  loading.value = true
  try {
    const res = await getTestReportPage({ pageNum: pageNum.value, pageSize: pageSize.value, ...filters })
    if (seq !== requestSeq) return
    list.value = res.data.records || []
    total.value = res.data.total || 0
    selectedReports.value = []
  } finally {
    if (seq === requestSeq) loading.value = false
  }
}

const search = () => {
  pageNum.value = 1
  load()
}

const reset = () => {
  filters.keyword = ''
  filters.testerId = null
  filters.testType = ''
  filters.deviceType = ''
  search()
}

const handlePageChange = (page) => {
  pageNum.value = page
  load()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  load()
}

const formatFileSize = (size) => {
  if (size === null || size === undefined) return '—'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const showBlobError = async (resp) => {
  if (!resp.data.type?.includes('application/json')) return false
  const json = JSON.parse(await resp.data.text())
  ElMessage.error(json.msg || '下载失败')
  return true
}

const saveFile = (data, fileName) => {
  const url = window.URL.createObjectURL(new Blob([data]))
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  window.URL.revokeObjectURL(url)
}

const handleDownload = async (row) => {
  try {
    const resp = await downloadReport(row.id)
    if (await showBlobError(resp)) return
    saveFile(resp.data, row.fileName || '测试报告')
  } catch {
    // 请求拦截器已统一提示错误
  }
}

const handleBatchDownload = async () => {
  if (selectedReports.value.length === 0) return
  batchDownloading.value = true
  try {
    const resp = await batchDownloadReports(selectedReports.value.map(row => row.id))
    if (await showBlobError(resp)) return
    const disposition = resp.headers['content-disposition'] || ''
    const matched = disposition.match(/filename="?([^";]+)"?/)
    saveFile(resp.data, matched ? matched[1] : 'test_reports.zip')
    ElMessage.success(`已打包 ${selectedReports.value.length} 份报告`)
  } catch {
    // 请求拦截器已统一提示错误
  } finally {
    batchDownloading.value = false
  }
}

onMounted(() => {
  load()
  getAllDict().then(res => { dicts.value = res.data || {} }).catch(() => {})
  listUserByRole('TESTER').then(res => { testers.value = res.data || [] }).catch(() => {})
})
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; gap: 16px; }
.header-tip { color: #909399; font-size: 13px; font-weight: normal; }
.batch-actions { display: flex; align-items: center; gap: 12px; }
.selected-tip { color: #606266; font-size: 13px; }
:deep(.el-form-item) { margin-bottom: 8px; }
</style>
