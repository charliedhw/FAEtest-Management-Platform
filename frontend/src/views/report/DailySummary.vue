<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>日报</span>
        <div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap">
          <el-button @click="shiftDate(-1)">前一天</el-button>
          <el-date-picker v-model="selectDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" :clearable="dimension === 'time'" style="width:150px" @change="resetAndLoad" />
          <el-button @click="shiftDate(1)">后一天</el-button>
          <el-input v-model="keyword" placeholder="搜索项目名称/编号/客户" clearable style="width:220px" @clear="resetAndLoad" @keyup.enter="resetAndLoad">
            <template #append><el-button @click="resetAndLoad"><el-icon><Search /></el-icon></el-button></template>
          </el-input>
        </div>
      </div>
    </template>
    <el-tabs v-model="dimension" @tab-change="changeDimension">
      <el-tab-pane label="按时间汇总" name="time" />
      <el-tab-pane label="按项目查看" name="project" />
    </el-tabs>
    <div v-if="selectDate" style="margin-bottom:12px">
      <el-tag type="primary" effect="plain">{{ selectDate }} 日报汇总</el-tag>
      <span style="margin-left:8px;color:#666;font-size:13px">
        {{ dimension === 'project' ? `共 ${total} 个进行中项目，未填报表示所选日期没有进度记录` : `共 ${total} 个项目有进展记录` }}
      </span>
    </div>
    <el-table :data="list" v-loading="loading" stripe :row-class-name="rowClassName">
      <el-table-column label="项目编号" prop="projectNo" width="140" />
      <el-table-column label="项目名称" min-width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/project/${row.projectId}`)">{{ row.projectName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="客户名称" prop="customerName" min-width="150" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusMap[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="测试人员" prop="testerNames" width="120" />
      <el-table-column v-if="dimension === 'project'" label="当日填报" width="100">
        <template #default="{ row }">
          <el-tag :type="row.progressCount > 0 ? 'success' : 'danger'" size="small">{{ row.progressCount > 0 ? '已填报' : '未填报' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="测试周期" width="180">
        <template #default="{ row }">
          {{ formatDate(row.testStartTime) }} ~ {{ formatDate(row.testEndTime) }}
        </template>
      </el-table-column>
      <el-table-column :label="selectDate ? '当日进展' : '最新进展'" min-width="250">
        <template #default="{ row }">
          <template v-if="dimension === 'project'">
            <div v-for="progress in row.dailyProgress" :key="progress.id" class="daily-progress">
              <div class="progress-content">{{ progress.content }}</div>
              <div class="progress-meta">{{ progress.progressDate }} · {{ progress.createByName }}</div>
            </div>
            <span v-if="!row.progressCount" style="color:#f56c6c">当日暂无项目进度</span>
          </template>
          <div v-else-if="row.latestProgressContent" class="latest-progress">
            <div class="progress-content">{{ row.latestProgressContent.length > 100 ? row.latestProgressContent.substring(0, 100) + '...' : row.latestProgressContent }}</div>
            <div class="progress-meta">{{ row.latestProgressDate }} · {{ row.latestProgressBy }}</div>
          </div>
          <span v-else style="color:#999">暂无进展</span>
        </template>
      </el-table-column>
      <el-table-column label="进展数" prop="progressCount" width="80" align="center" />
    </el-table>
    <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next" :total="total" :page-size="pageSize" :current-page="pageNum" @current-change="(p) => { pageNum = p; load() }" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import dayjs from 'dayjs'
import { Search } from '@element-plus/icons-vue'
import { getProgressSummary, getProjectDaily } from '../../api'
import { formatDate } from '../../utils/format'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')
const dimension = ref('time')
const selectDate = ref(dayjs().format('YYYY-MM-DD'))
let requestId = 0

const statusMap = { NOT_START: '未开始', IN_PROGRESS: '进行中', PAUSED: '暂停', COMPLETED: '已完成', CLOSED: '关闭', REJECTED: '已驳回' }
const statusType = (s) => ({ NOT_START: 'info', IN_PROGRESS: 'warning', PAUSED: 'warning', COMPLETED: 'success', CLOSED: 'info', REJECTED: 'danger' }[s] || '')

const load = async () => {
  const id = ++requestId
  loading.value = true
  list.value = []
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined }
    if (selectDate.value) params.date = selectDate.value
    const res = await (dimension.value === 'project' ? getProjectDaily(params) : getProgressSummary(params))
    if (id !== requestId) return
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    if (id === requestId) loading.value = false
  }
}

const resetAndLoad = () => {
  pageNum.value = 1
  return load()
}
const changeDimension = () => {
  if (dimension.value === 'project' && !selectDate.value) selectDate.value = dayjs().format('YYYY-MM-DD')
  return resetAndLoad()
}
const shiftDate = (days) => {
  selectDate.value = dayjs(selectDate.value || undefined).add(days, 'day').format('YYYY-MM-DD')
  return resetAndLoad()
}
const rowClassName = ({ row }) => dimension.value === 'project' && row.progressCount === 0 ? 'missing-progress' : ''

onMounted(load)
</script>

<style scoped>
.latest-progress { line-height: 1.4; }
.progress-content { font-size: 13px; color: #333; white-space: pre-wrap; }
.progress-meta { font-size: 12px; color: #999; margin-top: 2px; }
.daily-progress + .daily-progress { margin-top: 8px; padding-top: 8px; border-top: 1px solid #eee; }
:deep(.el-table .missing-progress) { --el-table-tr-bg-color: #fef0f0; }
</style>
