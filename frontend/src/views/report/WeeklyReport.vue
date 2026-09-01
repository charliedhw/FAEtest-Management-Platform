<template>
  <div>
    <!-- 我的周报（FAE填写） -->
    <el-card v-if="canWrite" shadow="never" style="margin-bottom:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>我的周报</span>
          <el-button type="primary" size="small" @click="openForm()"><el-icon><Plus /></el-icon>写周报</el-button>
        </div>
      </template>
      <div v-if="myLatest" style="margin-bottom:12px">
        <el-alert type="info" :closable="false" show-icon>
          <template #title>
            本周（W{{ currentWeek }}）
            <template v-if="myLatest.weekNum === currentWeek && myLatest.year === currentYear">
              已提交周报「{{ myLatest.title }}」
              <el-button link type="primary" size="small" @click="openForm(myLatest)">编辑</el-button>
            </template>
            <template v-else>
              尚未提交周报，请及时填写
            </template>
          </template>
        </el-alert>
      </div>
      <div v-else>
        <el-alert type="warning" :closable="false" show-icon title="还没有提交过周报，点击右上角按钮开始写周报" />
      </div>
    </el-card>

    <!-- 周报列表 -->
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>{{ canSeeAll ? '周报列表（全部）' : '我的周报' }}</span>
          <div style="display:flex;gap:8px;align-items:center">
            <el-input-number v-model="filterYear" :min="2020" :max="2030" placeholder="年份" size="small" style="width:110px" @change="load" />
            <el-input-number v-model="filterWeek" :min="1" :max="53" placeholder="周数" size="small" style="width:100px" @change="load" />
            <el-input v-if="canSeeAll" v-model="filterAuthor" placeholder="搜索姓名" clearable size="small" style="width:140px" @clear="load" @keyup.enter="load" />
            <el-button size="small" @click="load">查询</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="周报名称" prop="title" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" @click="viewDetail(row)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="作者" prop="authorName" width="100" />
        <el-table-column label="部门" prop="deptName" width="120" />
        <el-table-column label="周数" width="80" align="center">
          <template #default="{ row }">W{{ row.weekNum }}</template>
        </el-table-column>
        <el-table-column label="年份" prop="year" width="80" align="center" />
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="!canSeeAll" label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="handleDelete(row)">
              <template #reference><el-button link type="danger" size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total, prev, pager, next" :total="total" :page-size="pageSize" :current-page="pageNum" @current-change="(p) => { pageNum = p; load() }" />
    </el-card>

    <!-- 写周报对话框 -->
    <el-dialog v-model="formVisible" :title="formData.id ? '编辑周报' : '写周报'" width="700px" :close-on-click-modal="false">
      <el-form :model="formData" label-width="110px">
        <el-form-item label="周报名称" required>
          <el-input v-model="formData.title" placeholder="如：w35-应用测试部-胡深" />
          <div style="font-size:12px;color:#999;margin-top:4px">格式：w{周数}-{部门}-{姓名}，当前为第 {{ currentWeek }} 周</div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="周数" required>
              <el-input-number v-model="formData.weekNum" :min="1" :max="53" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年份" required>
              <el-input-number v-model="formData.year" :min="2020" :max="2030" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="本周测试进展" required>
          <el-input v-model="formData.thisWeekProgress" type="textarea" :rows="5" placeholder="本周完成的测试工作内容..." />
        </el-form-item>
        <el-form-item label="存在问题">
          <el-input v-model="formData.problems" type="textarea" :rows="3" placeholder="遇到的问题和困难..." />
        </el-form-item>
        <el-form-item label="下周工作计划" required>
          <el-input v-model="formData.nextWeekPlan" type="textarea" :rows="3" placeholder="下周的工作安排和计划..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 查看周报详情 -->
    <el-dialog v-model="detailVisible" :title="detailData.title" width="700px">
      <el-descriptions :column="2" border style="margin-bottom:16px">
        <el-descriptions-item label="作者">{{ detailData.authorName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ detailData.deptName }}</el-descriptions-item>
        <el-descriptions-item label="周数">W{{ detailData.weekNum }}</el-descriptions-item>
        <el-descriptions-item label="年份">{{ detailData.year }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ formatDateTime(detailData.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-card shadow="never" style="margin-bottom:12px">
        <template #header><span style="font-weight:600;color:#409eff">本周测试进展</span></template>
        <div style="white-space:pre-wrap;line-height:1.8">{{ detailData.thisWeekProgress }}</div>
      </el-card>
      <el-card v-if="detailData.problems" shadow="never" style="margin-bottom:12px">
        <template #header><span style="font-weight:600;color:#e6a23c">存在问题</span></template>
        <div style="white-space:pre-wrap;line-height:1.8">{{ detailData.problems }}</div>
      </el-card>
      <el-card shadow="never">
        <template #header><span style="font-weight:600;color:#67c23a">下周工作计划</span></template>
        <div style="white-space:pre-wrap;line-height:1.8">{{ detailData.nextWeekPlan }}</div>
      </el-card>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { saveWeeklyReport, deleteWeeklyReport, getWeeklyReportPage, getMyLatestReport, getCurrentWeek } from '../../api'
import { formatDateTime } from '../../utils/format'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const list = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(20)
const filterYear = ref(null)
const filterWeek = ref(null)
const filterAuthor = ref('')
const myLatest = ref(null)
const currentWeek = ref(0)
const currentYear = ref(0)

// FAE测试组及以上角色可写周报
const canWrite = computed(() =>
  userStore.hasRole('TESTER') || userStore.hasRole('FAE_LEADER') || userStore.hasRole('ADMIN')
)
// 审批组/管理员/FAE负责人可查看所有周报
const canSeeAll = computed(() =>
  userStore.hasRole('APPROVER') || userStore.hasRole('ADMIN') || userStore.hasRole('FAE_LEADER')
)

// 表单
const formVisible = ref(false)
const saving = ref(false)
const formData = ref({ id: null, title: '', weekNum: 0, year: 0, thisWeekProgress: '', problems: '', nextWeekPlan: '' })

// 详情
const detailVisible = ref(false)
const detailData = ref({})

const load = async () => {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterYear.value) params.year = filterYear.value
    if (filterWeek.value) params.weekNum = filterWeek.value
    if (filterAuthor.value) params.authorName = filterAuthor.value
    // 非管理员只能看自己的
    if (!canSeeAll.value) params.authorName = userStore.realName
    const res = await getWeeklyReportPage(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadMyLatest = async () => {
  if (!canWrite.value) return
  try {
    const res = await getMyLatestReport()
    myLatest.value = res.data
  } catch { myLatest.value = null }
}

const loadCurrentWeek = async () => {
  const res = await getCurrentWeek()
  currentWeek.value = res.data.weekNum
  currentYear.value = res.data.year
}

const openForm = (row) => {
  if (row) {
    formData.value = { ...row }
  } else {
    const w = currentWeek.value
    const y = currentYear.value
    const name = userStore.realName || ''
    formData.value = {
      id: null,
      title: `w${w}-应用测试部-${name}`,
      weekNum: w,
      year: y,
      thisWeekProgress: '',
      problems: '',
      nextWeekPlan: ''
    }
  }
  formVisible.value = true
}

const handleSave = async () => {
  if (!formData.value.title) { ElMessage.warning('请填写周报名称'); return }
  if (!formData.value.thisWeekProgress) { ElMessage.warning('请填写本周测试进展'); return }
  if (!formData.value.nextWeekPlan) { ElMessage.warning('请填写下周工作计划'); return }
  saving.value = true
  try {
    await saveWeeklyReport(formData.value)
    ElMessage.success('周报已保存')
    formVisible.value = false
    load()
    loadMyLatest()
  } finally {
    saving.value = false
  }
}

const handleDelete = async (row) => {
  await deleteWeeklyReport(row.id)
  ElMessage.success('已删除')
  load()
  loadMyLatest()
}

const viewDetail = (row) => {
  detailData.value = row
  detailVisible.value = true
}

onMounted(async () => {
  await loadCurrentWeek()
  filterYear.value = currentYear.value
  load()
  loadMyLatest()
})
</script>
