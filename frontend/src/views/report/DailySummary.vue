<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;justify-content:space-between;align-items:center">
        <span>日报（按时间维度汇总）</span>
        <div style="display:flex;gap:8px;align-items:center">
          <el-date-picker v-model="selectDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:150px" @change="load" />
          <el-input v-model="keyword" placeholder="搜索项目名称/编号/客户" clearable style="width:220px" @clear="load" @keyup.enter="load">
            <template #append><el-button @click="load"><el-icon><Search /></el-icon></el-button></template>
          </el-input>
        </div>
      </div>
    </template>
    <div v-if="selectDate" style="margin-bottom:12px">
      <el-tag type="primary" effect="plain">{{ selectDate }} 日报汇总</el-tag>
      <span style="margin-left:8px;color:#666;font-size:13px">共 {{ total }} 个项目有进展记录</span>
    </div>
    <el-table :data="list" v-loading="loading" stripe>
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
      <el-table-column label="测试周期" width="180">
        <template #default="{ row }">
          {{ formatDate(row.testStartTime) }} ~ {{ formatDate(row.testEndTime) }}
        </template>
      </el-table-column>
      <el-table-column :label="selectDate ? '当日进展' : '最新进展'" min-width="250">
        <template #default="{ row }">
          <div v-if="row.latestProgressContent" class="latest-progress">
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
import { Search } from '@element-plus/icons-vue'
import { getProgressSummary } from '../../api'
import { formatDate } from '../../utils/format'

const list = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const keyword = ref('')
// 默认选今天
const today = new Date()
const selectDate = ref(`${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`)

const statusMap = { NOT_START: '未开始', IN_PROGRESS: '进行中', PAUSED: '暂停', COMPLETED: '已完成', CLOSED: '关闭', REJECTED: '已驳回' }
const statusType = (s) => ({ NOT_START: 'info', IN_PROGRESS: 'warning', PAUSED: 'warning', COMPLETED: 'success', CLOSED: 'info', REJECTED: 'danger' }[s] || '')

const load = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined }
    if (selectDate.value) params.date = selectDate.value
    const res = await getProgressSummary(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.latest-progress { line-height: 1.4; }
.progress-content { font-size: 13px; color: #333; white-space: pre-wrap; }
.progress-meta { font-size: 12px; color: #999; margin-top: 2px; }
</style>
