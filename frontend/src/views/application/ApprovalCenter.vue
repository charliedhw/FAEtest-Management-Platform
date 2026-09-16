<template>
  <div>
    <el-card shadow="never">
      <template #header><span>待办审批</span></template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="appNo" label="申请单号" width="140" />
        <el-table-column prop="customerName" label="客户名称" min-width="140" show-overflow-tooltip />
        <el-table-column label="项目名称" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="openDetail(row)">{{ row.projectName }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="applicantName" label="申请人" width="90" />
        <el-table-column label="当前节点" width="120">
          <template #default="{ row }"><el-tag type="warning">{{ nodeMap[row.currentNode] || row.currentNode }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="applyDays" label="申请天数" width="90" />
        <el-table-column label="排期" width="170">
          <template #default="{ row }">
            <template v-if="row.scheduleStartTime">
              <el-tag type="success" size="small">{{ row.scheduleStartTime }} ~ {{ row.scheduleEndTime }}</el-tag>
            </template>
            <el-tag v-else-if="row.currentNode === 'ASSIGN'" type="info" size="small">未排期</el-tag>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openApproval(row, 'APPROVE')">通过</el-button>
            <el-button link type="danger" size="small" @click="openApproval(row, 'REJECT')">驳回</el-button>
            <el-button v-if="row.currentNode === 'ASSIGN' && canSchedule" link type="warning" size="small" @click="openSchedule(row)">排期</el-button>
            <el-button v-if="row.currentNode === 'ASSIGN'" link type="success" size="small" @click="openAssign(row)">分配</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" @current-change="(p) => { query.pageNum = p; load() }" />
    </el-card>

    <!-- 审批对话框 -->
    <el-dialog v-model="approvalVisible" :title="approvalAction === 'APPROVE' ? '审批通过' : '驳回申请'" width="500px">
      <el-form label-width="80px">
        <el-form-item label="项目">{{ currentRow.projectName }}</el-form-item>
        <el-form-item v-if="approvalAction === 'REJECT'" label="驳回原因">
          <el-select v-model="approvalForm.rejectReason" placeholder="请选择" style="width:100%">
            <el-option v-for="d in dicts.reject_reason" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见"><el-input v-model="approvalForm.opinion" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button :type="approvalAction === 'APPROVE' ? 'primary' : 'danger'" @click="handleApproval">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配对话框 -->
    <el-dialog v-model="assignVisible" title="分配资源与人员" width="500px">
      <el-form label-width="100px">
        <el-form-item label="项目">{{ currentRow.projectName }}</el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="assignForm.resourceType" style="width:100%">
            <el-option v-for="d in dicts.resource_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
        </el-form-item>
        <el-form-item label="测试人员">
          <el-select v-model="assignTesterArr" multiple style="width:100%">
            <el-option v-for="u in testers" :key="u.id" :label="u.realName" :value="String(u.id)" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定分配</el-button>
      </template>
    </el-dialog>

    <!-- 排期对话框 -->
    <el-dialog v-model="scheduleVisible" title="项目排期" width="500px">
      <el-form label-width="100px">
        <el-form-item label="项目">{{ currentRow.projectName }}</el-form-item>
        <el-form-item label="计划测试时间" required>
          <el-date-picker v-model="scheduleRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="排期说明">
          <el-input v-model="scheduleForm.scheduleRemark" type="textarea" :rows="2" placeholder="如：等508资源释放 / 等测试人员到位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scheduleVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSchedule">确定排期</el-button>
      </template>
    </el-dialog>

    <!-- 项目申请详情抽屉 -->
    <el-drawer v-model="detailVisible" title="项目申请详情" size="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="申请单号">{{ detail.appNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ detail.customerName }}</el-descriptions-item>
        <el-descriptions-item label="项目名称">{{ detail.projectName }}</el-descriptions-item>
        <el-descriptions-item label="所属区域">{{ detail.region }}</el-descriptions-item>
        <el-descriptions-item label="项目SPM号">{{ detail.spmNo }}</el-descriptions-item>
        <el-descriptions-item label="项目销售">{{ detail.salesName }}</el-descriptions-item>
        <el-descriptions-item label="方案售前">{{ detail.presalesName }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="需求描述">{{ detail.requirement }}</el-descriptions-item>
        <el-descriptions-item label="测试计划内容">{{ detail.testPlan }}</el-descriptions-item>
        <el-descriptions-item label="测试类型">{{ formatTestType(detail.testType) }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ detail.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="硬件配置">{{ detail.hardwareConfig }}</el-descriptions-item>
        <el-descriptions-item label="软件及应用">{{ detail.softwareApp }}</el-descriptions-item>
        <el-descriptions-item label="申请天数">{{ detail.applyDays }} 天</el-descriptions-item>
        <el-descriptions-item label="期望资源类型">{{ detail.expectResourceType }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.scheduleStartTime" label="排期时间">{{ detail.scheduleStartTime }} ~ {{ detail.scheduleEndTime }}（{{ detail.scheduleByName }}）</el-descriptions-item>
        <el-descriptions-item v-if="detail.scheduleRemark" label="排期说明">{{ detail.scheduleRemark }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因"><span style="color:#f56c6c">{{ detail.rejectReason }}</span></el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTodoList, approveApplication, assignApplication, scheduleApplication, getAllDict, listUserByRole } from '../../api'
import { useUserStore } from '../../store/user'
import { formatDateTime, formatTestType } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10 })
const dicts = ref({})
const testers = ref([])
const detailVisible = ref(false)
const detail = ref({})

const openDetail = (row) => { detail.value = row; detailVisible.value = true }

const approvalVisible = ref(false)
const approvalAction = ref('APPROVE')
const currentRow = ref({})
const approvalForm = ref({ appId: null, action: '', opinion: '', rejectReason: '' })

const assignVisible = ref(false)
const assignForm = ref({ appId: null, testerIds: '', resourceType: '' })
const assignTesterArr = ref([])

// 排期
const userStore = useUserStore()
const canSchedule = computed(() => userStore.hasRole('SCHEDULER') || userStore.hasRole('ADMIN'))
const scheduleVisible = ref(false)
const scheduleRange = ref([])
const scheduleForm = ref({ appId: null, scheduleStartTime: '', scheduleEndTime: '', scheduleRemark: '' })

const nodeMap = { PRESALES_EVAL: '售前评估', APPROVAL: '测试审批组审批', LEADER_APPROVAL: '领导审批', ASSIGN: 'daihw分配任务' }

const load = async () => {
  loading.value = true
  try {
    const res = await getTodoList(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const openApproval = (row, action) => {
  currentRow.value = row
  approvalAction.value = action
  approvalForm.value = { appId: row.id, action, opinion: '', rejectReason: '' }
  approvalVisible.value = true
}

const handleApproval = async () => {
  await approveApplication(approvalForm.value)
  ElMessage.success(approvalAction.value === 'APPROVE' ? '已通过' : '已驳回')
  approvalVisible.value = false
  load()
}

const openAssign = (row) => {
  currentRow.value = row
  assignForm.value = { appId: row.id, testerIds: '', resourceType: row.expectResourceType || '' }
  assignTesterArr.value = []
  assignVisible.value = true
}

const handleAssign = async () => {
  assignForm.value.testerIds = assignTesterArr.value.join(',')
  await assignApplication(assignForm.value)
  ElMessage.success('分配成功')
  assignVisible.value = false
  load()
}

const openSchedule = (row) => {
  currentRow.value = row
  scheduleForm.value = { appId: row.id, scheduleStartTime: row.scheduleStartTime || '', scheduleEndTime: row.scheduleEndTime || '', scheduleRemark: row.scheduleRemark || '' }
  scheduleRange.value = row.scheduleStartTime ? [row.scheduleStartTime, row.scheduleEndTime] : []
  scheduleVisible.value = true
}

const handleSchedule = async () => {
  if (!scheduleRange.value || scheduleRange.value.length !== 2) {
    ElMessage.warning('请选择排期起止时间')
    return
  }
  scheduleForm.value.scheduleStartTime = scheduleRange.value[0]
  scheduleForm.value.scheduleEndTime = scheduleRange.value[1]
  await scheduleApplication(scheduleForm.value)
  ElMessage.success('排期成功')
  scheduleVisible.value = false
  load()
}

onMounted(async () => {
  load()
  const [dictRes, testerRes] = await Promise.all([getAllDict(), listUserByRole('TESTER')])
  dicts.value = dictRes.data
  testers.value = testerRes.data
})
</script>
