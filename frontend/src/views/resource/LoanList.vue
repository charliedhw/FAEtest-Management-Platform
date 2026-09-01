<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <el-select v-model="query.status" placeholder="状态" clearable style="width:140px">
            <el-option label="借出中" value="BORROWED" /><el-option label="已归还" value="RETURNED" /><el-option label="超期" value="OVERDUE" />
          </el-select>
          <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
        </div>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="resourceId" label="资源ID" width="90" />
        <el-table-column prop="borrowerName" label="借用人" width="100" />
        <el-table-column prop="deptName" label="费用归属部门" width="130" />
        <el-table-column label="借出时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.loanTime) }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.expectReturnTime) }}</template>
        </el-table-column>
        <el-table-column label="归还时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.actualReturnTime) }}</template>
        </el-table-column>
        <el-table-column prop="loanDays" label="借用天数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="{ BORROWED: 'warning', RETURNED: 'success', OVERDUE: 'danger' }[row.status]">{{ { BORROWED: '借出中', RETURNED: '已归还', OVERDUE: '超期' }[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'BORROWED'" link type="success" size="small" @click="handleReturn(row)">归还</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" @current-change="(p) => { query.pageNum = p; load() }" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getLoanPage, returnResource } from '../../api'
import { formatDateTime } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10, status: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await getLoanPage(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const handleReturn = async (row) => {
  await ElMessageBox.confirm('确认归还该资源？', '提示', { type: 'success' })
  await returnResource(row.id)
  ElMessage.success('已归还')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>
