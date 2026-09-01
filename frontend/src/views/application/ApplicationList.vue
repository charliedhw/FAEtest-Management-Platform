<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <div>
          <el-input v-model="query.keyword" placeholder="项目名/客户/单号" style="width:240px" clearable @clear="load" @keyup.enter="load" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width:140px;margin-left:8px">
            <el-option v-for="(v,k) in statusMap" :key="k" :label="v" :value="k" />
          </el-select>
          <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
        </div>
        <el-button type="primary" @click="$router.push('/application/create')">
          <el-icon><Plus /></el-icon>发起申请
        </el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="appNo" label="申请单号" width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="projectName" label="项目名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="region" label="区域" width="90" />
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'REJECTED' && row.applicantId == userStore.userId" link type="warning" size="small" @click="resubmit(row)">重新提交</el-button>
            <el-button v-if="['DRAFT','PENDING_PRESALES'].includes(row.status) && row.applicantId == userStore.userId" link type="danger" size="small" @click="handleWithdraw(row)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" @current-change="(p) => { query.pageNum = p; load() }" />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="申请详情" size="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请单号">{{ detail.appNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ detail.projectName }}</el-descriptions-item>
        <el-descriptions-item label="所属区域">{{ detail.region }}</el-descriptions-item>
        <el-descriptions-item label="SPM号">{{ detail.spmNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="需求描述">{{ detail.requirement }}</el-descriptions-item>
        <el-descriptions-item label="测试计划">{{ detail.testPlan }}</el-descriptions-item>
        <el-descriptions-item label="测试类型">{{ formatTestType(detail.testType) }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ detail.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="硬件配置">{{ detail.hardwareConfig }}</el-descriptions-item>
        <el-descriptions-item label="软件及应用">{{ detail.softwareApp }}</el-descriptions-item>
        <el-descriptions-item label="申请周期">{{ detail.applyPeriod }}</el-descriptions-item>
        <el-descriptions-item label="期望资源">{{ detail.expectResourceType }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status)">{{ statusMap[detail.status] || detail.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因"><span style="color:#f56c6c">{{ detail.rejectReason }}</span></el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getApplicationPage, withdrawApplication } from '../../api'
import { formatDateTime, formatTestType } from '../../utils/format'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref({})
const query = ref({ pageNum: 1, pageSize: 10, status: '', keyword: '' })

const statusMap = {
  DRAFT: '草稿', PENDING_PRESALES: '待售前评估', PENDING_APPROVAL: '待审批',
  PENDING_LEADER: '待领导审批', PENDING_ASSIGN: '待分配资源', ASSIGNED: '已分配',
  REJECTED: '已驳回', CLOSED: '已关闭'
}
const statusType = (s) => ({ DRAFT: 'info', PENDING_PRESALES: 'warning', PENDING_APPROVAL: 'warning', PENDING_LEADER: 'warning', PENDING_ASSIGN: 'warning', ASSIGNED: 'success', REJECTED: 'danger', CLOSED: 'info' }[s] || '')

const load = async () => {
  loading.value = true
  try {
    const res = await getApplicationPage(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const viewDetail = (row) => { detail.value = row; detailVisible.value = true }
const resubmit = (row) => { router.push({ path: '/application/create', query: { id: row.id } }) }
const handleWithdraw = async (row) => {
  await ElMessageBox.confirm('确认撤回该申请？', '提示', { type: 'warning' })
  await withdrawApplication(row.id)
  ElMessage.success('已撤回')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; }
</style>
