<template>
  <div v-loading="loading">
    <el-card shadow="never" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>项目详情 - {{ project.projectName }}</span>
          <div>
            <el-tag :type="statusType(project.status)" style="margin-right:8px">{{ statusMap[project.status] || project.status }}</el-tag>
            <el-button size="small" @click="$router.back()">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="项目编号">{{ project.projectNo }}</el-descriptions-item>
        <el-descriptions-item label="客户名称">{{ project.customerName }}</el-descriptions-item>
        <el-descriptions-item label="所属区域">{{ project.region }}</el-descriptions-item>
        <el-descriptions-item label="SPM号">{{ project.spmNo }}</el-descriptions-item>
        <el-descriptions-item label="项目阶段">{{ project.projectStage }}</el-descriptions-item>
        <el-descriptions-item label="招标状态">{{ project.bidStatus }}</el-descriptions-item>
        <el-descriptions-item label="销售">{{ project.salesName }}</el-descriptions-item>
        <el-descriptions-item label="售前">{{ project.presalesName }}</el-descriptions-item>
        <el-descriptions-item label="测试人员">{{ project.testerNames }}</el-descriptions-item>
        <el-descriptions-item label="测试类型">{{ formatTestType(project.testType) }}</el-descriptions-item>
        <el-descriptions-item label="测试方式">{{ project.testMethod }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ project.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="申请周期">{{ project.applyPeriod }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDate(project.testStartTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDate(project.testEndTime) }}</el-descriptions-item>
        <el-descriptions-item label="中标金额(万)">{{ project.bidAmount }}</el-descriptions-item>
        <el-descriptions-item label="测试计划" :span="2">{{ project.testPlan }}</el-descriptions-item>
        <el-descriptions-item label="硬件配置" :span="2">{{ project.hardwareConfig }}</el-descriptions-item>
        <el-descriptions-item label="软件及应用" :span="2">{{ project.softwareApp }}</el-descriptions-item>
        <el-descriptions-item label="测试结论" :span="2">{{ project.testConclusion }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <div style="margin-top:16px">
        <template v-if="perm.startTest">
          <el-button v-if="project.status === 'NOT_START'" type="primary" size="small" @click="changeStatus('IN_PROGRESS')">开始测试</el-button>
          <el-button v-if="project.status === 'IN_PROGRESS'" type="warning" size="small" @click="changeStatus('PAUSED')">暂停</el-button>
          <el-button v-if="project.status === 'PAUSED'" type="primary" size="small" @click="changeStatus('IN_PROGRESS')">恢复</el-button>
          <el-button v-if="['IN_PROGRESS','PAUSED'].includes(project.status)" type="success" size="small" @click="changeStatus('COMPLETED')">完成</el-button>
        </template>
        <el-button size="small" type="primary" plain @click="openEdit">编辑信息</el-button>
        <el-button v-if="perm.setKey" size="small" @click="toggleKey">{{ project.isKeyProject === 1 ? '取消重点' : '设为重点' }}</el-button>
      </div>
    </el-card>

    <!-- 阶段任务与进度 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>阶段任务与项目进度</span>
          <el-button v-if="perm.editProgress" type="primary" size="small" @click="openStageForm()"><el-icon><Plus /></el-icon>拆分阶段任务</el-button>
        </div>
      </template>
      <!-- 进度总览 -->
      <div v-if="progressData.total > 0" style="margin-bottom:16px">
        <div style="display:flex;align-items:center;gap:16px;margin-bottom:8px">
          <el-progress :percentage="progressData.percent" :stroke-width="18" style="flex:1" :color="progressColor" />
          <span style="font-size:13px;color:#666">共 {{ progressData.total }} 个阶段 / 已完成 {{ progressData.done }} 个</span>
        </div>
        <!-- 阶段进度条(甘特式,按时间维度) -->
        <div class="stage-gantt">
          <div v-for="s in stageList" :key="s.id" class="stage-bar-row">
            <div class="stage-name" :title="s.stageName">{{ s.stageName }}</div>
            <div class="stage-track">
              <div class="stage-bar" :class="'stage-' + s.status" :style="{ width: stageTimePercent(s) + '%' }">
                <span class="stage-bar-text" v-if="stageTimePercent(s) > 12">{{ stageStatusText(s.status) }}</span>
              </div>
              <span class="stage-percent">{{ stageTimePercent(s) }}%</span>
            </div>
            <div class="stage-date">{{ formatDate(s.planStart) }} ~ {{ formatDate(s.planEnd) }}</div>
            <div class="stage-ops" v-if="perm.editProgress">
              <el-button link type="primary" size="small" @click="openStageForm(s)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDeleteStage(s)">删除</el-button>
            </div>
          </div>
          <!-- 阶段测试方案描述 -->
          <div v-for="s in stageList.filter(x => x.testDesc)" :key="'desc-' + s.id" class="stage-desc">
            <span class="stage-desc-name">【{{ s.stageName }}】测试方案：</span>{{ s.testDesc }}
          </div>
        </div>
      </div>
      <el-empty v-else description="尚未拆分阶段任务，测试开始前请先拆分" :image-size="80" />
    </el-card>

    <!-- 进展日报 -->
    <el-card shadow="never" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between">
          <span>测试进展</span>
          <el-button v-if="perm.editProgress" type="primary" size="small" @click="progressVisible = true"><el-icon><Plus /></el-icon>填写进展</el-button>
        </div>
      </template>
      <el-timeline v-if="progressList.length > 0">
        <el-timeline-item v-for="p in progressList" :key="p.id" :timestamp="`${p.progressDate} ${p.createByName}`" placement="top">
          <div style="white-space:pre-wrap">{{ p.content }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无进展记录" />
    </el-card>

    <!-- 测试报告 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between">
          <span>测试报告</span>
          <el-upload v-if="perm.editProgress" :show-file-list="false" :http-request="handleUpload" accept=".doc,.docx,.pdf,.xlsx,.pptx,.html,.zip">
            <el-button type="primary" size="small"><el-icon><Upload /></el-icon>上传报告</el-button>
          </el-upload>
        </div>
      </template>
      <el-table :data="reportList" size="small">
        <el-table-column prop="fileName" label="文件名" min-width="250" show-overflow-tooltip />
        <el-table-column prop="version" label="版本" width="60" />
        <el-table-column prop="uploadByName" label="上传人" width="90" />
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleDownload(row)">下载</el-button>
            <el-button link type="danger" size="small" @click="handleDeleteReport(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 填写进展 -->
    <el-dialog v-model="progressVisible" title="填写测试进展" width="600px">
      <el-form label-width="80px">
        <el-form-item label="日期"><el-date-picker v-model="progressForm.progressDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="progressForm.content" type="textarea" :rows="5" placeholder="今日测试进展..." /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddProgress">保存</el-button>
      </template>
    </el-dialog>

    <!-- 阶段任务表单 -->
    <el-dialog v-model="stageVisible" :title="stageForm.id ? '编辑阶段任务' : '拆分阶段任务'" width="550px">
      <el-form :model="stageForm" label-width="110px">
        <el-form-item label="阶段名称" required><el-input v-model="stageForm.stageName" placeholder="如:环境部署/功能测试/性能测试" /></el-form-item>
        <el-form-item label="测试方案描述"><el-input v-model="stageForm.testDesc" type="textarea" :rows="3" placeholder="说明当前阶段任务的测试要求、测试方案、预期目标等" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="计划开始"><el-date-picker v-model="stageForm.planStart" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计划结束"><el-date-picker v-model="stageForm.planEnd" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="阶段状态">
          <el-select v-model="stageForm.status" style="width:100%">
            <el-option label="未开始" value="NOT_START" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已完成" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="stageForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stageVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveStage">保存</el-button>
      </template>
    </el-dialog>

    <!-- 编辑项目信息 -->
    <el-dialog v-model="editVisible" title="编辑项目信息" width="700px">
      <el-form :model="editForm" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="客户名称"><el-input v-model="editForm.customerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="项目名称"><el-input v-model="editForm.projectName" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="所属区域"><el-input v-model="editForm.region" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="SPM号"><el-input v-model="editForm.spmNo" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="项目阶段">
              <el-select v-model="editForm.projectStage" style="width:100%" clearable>
                <el-option v-for="d in dicts.project_stage" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="招标状态">
              <el-select v-model="editForm.bidStatus" style="width:100%" clearable>
                <el-option v-for="d in dicts.bid_status" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="中标金额(万)"><el-input-number v-model="editForm.bidAmount" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="设备类型">
              <el-select v-model="editDeviceTypeArr" multiple style="width:100%" placeholder="请选择设备类型">
                <el-option v-for="d in dicts.device_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="测试类型"><el-input v-model="editForm.testType" placeholder="如：AI、测试" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="测试方式">
              <el-select v-model="editForm.testMethod" style="width:100%" clearable placeholder="请选择">
                <el-option v-for="d in dicts.test_method" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="测试人员"><el-input v-model="editForm.testerNames" placeholder="多个用顿号、分隔" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="申请周期"><el-input v-model="editForm.applyPeriod" placeholder="如：15天" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker v-model="editForm.testStartTime" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker v-model="editForm.testEndTime" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="期望资源类型"><el-input v-model="editForm.expectResourceType" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="是否内部资源">
              <el-select v-model="editForm.isInternalResource" style="width:100%" clearable placeholder="请选择">
                <el-option label="是" value="是" /><el-option label="否" value="否" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="测试计划"><el-input v-model="editForm.testPlan" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="硬件配置"><el-input v-model="editForm.hardwareConfig" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="软件及应用"><el-input v-model="editForm.softwareApp" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="测试结论"><el-input v-model="editForm.testConclusion" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProjectDetail, updateProjectStatus, updateProject, listProgress, addProgress, listReport, uploadReport, deleteReport, getAllDict, listStage, addStage, updateStage, deleteStage, getProjectProgress } from '../../api'
import { formatDateTime, formatDate, formatTestType } from '../../utils/format'

const route = useRoute()
const projectId = route.params.id
const loading = ref(false)
const project = ref({})
const progressList = ref([])
const reportList = ref([])
const dicts = ref({})
const progressVisible = ref(false)
const progressForm = ref({ projectId, progressDate: null, content: '' })
const editVisible = ref(false)
const saving = ref(false)
const editForm = ref({})
const editDeviceTypeArr = ref([])
// 阶段任务
const stageList = ref([])
const progressData = ref({ total: 0, done: 0, inProgress: 0, notStart: 0, percent: 0 })
const stageVisible = ref(false)
const stageForm = ref({})

// 进度条颜色
const progressColor = computed(() => {
  const p = progressData.value.percent
  if (p >= 100) return '#67c23a'
  if (p >= 50) return '#409eff'
  return '#e6a23c'
})

const stageStatusText = (s) => ({ NOT_START: '未开始', IN_PROGRESS: '进行中', DONE: '已完成' }[s] || s)
const stageNameOf = (id) => {
  const s = stageList.value.find(x => x.id === id)
  return s ? s.stageName : ''
}
// 阶段进度条: 按时间维度计算百分比
// 已完成=100%; 未开始=0%; 进行中=当前日期在计划起止时间中的进度
const stageTimePercent = (s) => {
  if (s.status === 'DONE') return 100
  if (s.status === 'NOT_START') return 0
  // 进行中: 按时间进度
  if (!s.planStart || !s.planEnd) return 50
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const start = new Date(s.planStart)
  const end = new Date(s.planEnd)
  if (today <= start) return 0
  if (today >= end) return 100
  const totalDays = (end - start) / (1000 * 60 * 60 * 24)
  if (totalDays <= 0) return 100
  const passedDays = (today - start) / (1000 * 60 * 60 * 24)
  return Math.min(100, Math.round((passedDays / totalDays) * 100))
}

// 按钮权限(来自后端 detail 返回的 permissions)
const perm = computed(() => {
  const p = project.value.permissions
  return {
    startTest: p?.startTest ?? false,
    updateBid: p?.updateBid ?? true,
    setKey: p?.setKey ?? false,
    editProgress: p?.editProgress ?? false
  }
})

const statusMap = { NOT_START: '未开始', IN_PROGRESS: '进行中', PAUSED: '暂停', COMPLETED: '已完成', CLOSED: '关闭', REJECTED: '已驳回' }
const statusType = (s) => ({ NOT_START: 'info', IN_PROGRESS: 'warning', PAUSED: 'warning', COMPLETED: 'success', CLOSED: 'info', REJECTED: 'danger' }[s] || '')

const load = async () => {
  loading.value = true
  try {
    const [pRes, progRes, repRes, stageRes, progDataRes] = await Promise.all([
      getProjectDetail(projectId), listProgress(projectId), listReport(projectId),
      listStage(projectId), getProjectProgress(projectId)
    ])
    project.value = pRes.data
    progressList.value = progRes.data
    reportList.value = repRes.data
    stageList.value = stageRes.data
    progressData.value = progDataRes.data
  } finally {
    loading.value = false
  }
}

const changeStatus = async (status) => {
  await updateProjectStatus({ id: projectId, status })
  ElMessage.success('状态已更新')
  load()
}

// 打开编辑对话框,回填当前数据
const openEdit = () => {
  editForm.value = { ...project.value }
  editDeviceTypeArr.value = String(project.value.deviceType || '').split(/[,，、]/).map(s => s.trim()).filter(Boolean)
  editVisible.value = true
}

// 保存编辑
const handleSaveEdit = async () => {
  saving.value = true
  try {
    editForm.value.deviceType = editDeviceTypeArr.value.join(',')
    await updateProject(editForm.value)
    ElMessage.success('项目信息已保存')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

const toggleKey = async () => {
  await updateProject({ id: projectId, isKeyProject: project.value.isKeyProject === 1 ? 0 : 1 })
  ElMessage.success('已更新')
  load()
}

// ===== 阶段任务 =====
const openStageForm = (row) => {
  stageForm.value = row ? { ...row } : { projectId, stageName: '', planStart: null, planEnd: null, status: 'NOT_START', sort: stageList.value.length + 1 }
  stageVisible.value = true
}
const handleSaveStage = async () => {
  if (!stageForm.value.stageName) { ElMessage.warning('请填写阶段名称'); return }
  if (stageForm.value.id) {
    await updateStage(stageForm.value)
  } else {
    await addStage(stageForm.value)
  }
  ElMessage.success('阶段任务已保存')
  stageVisible.value = false
  load()
}
const handleDeleteStage = async (row) => {
  await ElMessageBox.confirm(`确认删除阶段【${row.stageName}】？`, '提示', { type: 'warning' })
  await deleteStage(row.id)
  ElMessage.success('已删除')
  load()
}

const handleAddProgress = async () => {
  await addProgress(progressForm.value)
  ElMessage.success('进展已保存')
  progressVisible.value = false
  progressForm.value.content = ''
  load()
}

const handleUpload = async ({ file }) => {
  await uploadReport(projectId, file)
  ElMessage.success('上传成功')
  load()
}

const handleDownload = (row) => {
  const token = localStorage.getItem('token')
  window.open(`/api/report/download/${row.id}?token=${token}`, '_blank')
}

const handleDeleteReport = async (row) => {
  await ElMessageBox.confirm('确认删除该报告？', '提示', { type: 'warning' })
  await deleteReport(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(async () => {
  load()
  const res = await getAllDict()
  dicts.value = res.data
})
</script>

<style scoped>
/* 阶段进度条(甘特式) */
.stage-gantt { border: 1px solid #ebeef5; border-radius: 6px; padding: 8px 0; }
.stage-bar-row { display: flex; align-items: center; padding: 8px 12px; border-bottom: 1px solid #f5f5f5; gap: 10px; }
.stage-bar-row:last-child { border-bottom: none; }
.stage-name { width: 140px; font-size: 13px; font-weight: bold; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex-shrink: 0; }
.stage-track { flex: 1; background: #f0f0f0; border-radius: 10px; height: 24px; position: relative; overflow: hidden; display: flex; align-items: center; }
.stage-bar { height: 100%; border-radius: 10px 0 0 10px; display: flex; align-items: center; justify-content: center; transition: width 0.3s; }
.stage-bar-text { font-size: 12px; color: #fff; white-space: nowrap; }
.stage-percent { position: absolute; right: 8px; font-size: 12px; color: #606266; font-weight: bold; }
.stage-NOT_START { background: #c0c4cc; }
.stage-IN_PROGRESS { background: linear-gradient(90deg, #409eff, #66b1ff); }
.stage-DONE { background: linear-gradient(90deg, #67c23a, #85ce61); }
.stage-date { width: 180px; font-size: 12px; color: #999; flex-shrink: 0; }
.stage-ops { flex-shrink: 0; }
/* 阶段测试方案描述 */
.stage-desc { padding: 8px 12px; font-size: 13px; color: #666; border-top: 1px dashed #ebeef5; line-height: 1.6; }
.stage-desc-name { font-weight: bold; color: #1a2a6c; }
</style>
