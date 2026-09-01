<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <div style="display:flex;flex-wrap:wrap;gap:8px;align-items:center">
          <el-input v-model="query.keyword" placeholder="项目名/客户/编号/SPM" style="width:190px" clearable @keyup.enter="load" @clear="load" />
          <el-select v-model="query.status" placeholder="状态" clearable style="width:100px">
            <el-option v-for="(v,k) in statusMap" :key="k" :label="v" :value="k" />
          </el-select>
          <el-select v-model="query.region" placeholder="区域" clearable style="width:100px">
            <el-option v-for="r in regions" :key="r" :label="r" :value="r" />
          </el-select>
          <el-select v-model="query.tester" placeholder="测试人员" clearable filterable style="width:120px">
            <el-option v-for="t in testerOptions" :key="t" :label="t" :value="t" />
          </el-select>
          <el-select v-model="query.testType" placeholder="测试类型" clearable style="width:120px">
            <el-option v-for="d in dicts.test_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
          </el-select>
          <el-select v-model="query.deviceType" placeholder="测试设备" clearable filterable style="width:120px">
            <el-option v-for="d in deviceOptions" :key="d" :label="d" :value="d" />
          </el-select>
          <el-select v-model="query.period" placeholder="测试周期" clearable style="width:120px">
            <el-option label="7天内" value="LE7" />
            <el-option label="8-15天" value="8-15" />
            <el-option label="16-30天" value="16-30" />
            <el-option label="31-90天" value="31-90" />
            <el-option label="90天以上" value="GT90" />
          </el-select>
          <el-date-picker v-model="query.testStartFrom" type="date" placeholder="测试开始时间" value-format="YYYY-MM-DD" style="width:150px" @change="load" />
          <span style="color:#999">至</span>
          <el-date-picker v-model="query.testStartTo" type="date" placeholder="测试结束时间" value-format="YYYY-MM-DD" style="width:150px" @change="load" />
          <el-button type="primary" @click="load">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </div>
        <div style="display:flex;gap:8px">
          <el-popover placement="bottom-end" width="220" trigger="click">
            <template #reference>
              <el-button><el-icon><SetUp /></el-icon>列设置</el-button>
            </template>
            <div style="max-height:300px;overflow-y:auto">
              <el-checkbox v-for="col in allColumns" :key="col.prop" v-model="col.visible" style="display:block;margin-bottom:4px">{{ col.label }}</el-checkbox>
            </div>
          </el-popover>
          <el-button type="success" @click="handleExport"><el-icon><Download /></el-icon>导出Excel</el-button>
        </div>
      </div>
      <el-table :data="list" v-loading="loading" stripe :key="tableKey">
        <template v-for="col in visibleColumns" :key="col.prop">
          <el-table-column :prop="col.prop" :label="col.label" :width="colWidths[col.prop]" :min-width="col.minWidth" :show-overflow-tooltip="col.tooltip !== false">
            <template #header>
              <div class="col-header">
                <span>{{ col.label }}</span>
                <span class="col-resize-handle" @mousedown="(e) => startResize(e, col.prop)"></span>
              </div>
            </template>
            <template v-if="col.custom" #default="{ row }">
              <template v-if="col.prop === 'status'">
                <el-tag :type="statusType(row.status)">{{ statusMap[row.status] || row.status }}</el-tag>
              </template>
              <template v-else-if="col.prop === 'isKeyProject'">
                <el-tag v-if="row.isKeyProject === 1" type="danger" size="small">重点</el-tag>
              </template>
              <template v-else-if="col.prop === 'updateTime'">
                {{ formatDateTime(row.updateTime) }}
              </template>
              <template v-else-if="col.prop === 'testType'">
                {{ formatTestType(row.testType) }}
              </template>
            </template>
          </el-table-column>
        </template>
        <el-table-column label="操作" :width="colWidths.__ops || 150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push(`/project/${row.id}`)">详情</el-button>
            <el-button v-if="canDelete" link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,prev,pager,next,sizes" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" :page-sizes="[10,20,50]" @current-change="(p) => { query.pageNum = p; load() }" @size-change="(s) => { query.pageSize = s; load() }" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProjectPage, deleteProject, getAllDict, getDimensionStats } from '../../api'
import { useUserStore } from '../../store/user'
import { formatDateTime, formatTestType } from '../../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = ref({ pageNum: 1, pageSize: 10, status: '', region: '', keyword: '', tester: '', testType: '', deviceType: '', period: '', testStartFrom: '', testStartTo: '' })
const regions = ref(['北京','上海','浙江','江苏','广东','四川','安徽','湖北','湖南','深圳','山东','天津','重庆','福建','吉林','甘肃','贵州','辽宁','广西','西安','成都','武汉','东北'])
const dicts = ref({})
const testerOptions = ref([])
const deviceOptions = ref([])
const tableKey = ref(0)

const userStore = useUserStore()
const canDelete = computed(() => userStore.hasRole('ADMIN') || userStore.hasRole('RESOURCE_ADMIN') || userStore.hasRole('FAE_LEADER'))

const statusMap = { NOT_START: '未开始', IN_PROGRESS: '进行中', PAUSED: '暂停', COMPLETED: '已完成', CLOSED: '关闭', REJECTED: '已驳回' }
const statusType = (s) => ({ NOT_START: 'info', IN_PROGRESS: 'warning', PAUSED: 'warning', COMPLETED: 'success', CLOSED: 'info', REJECTED: 'danger' }[s] || '')

// ===== 列自定义 =====
const defaultColumns = [
  { prop: 'projectNo', label: '项目编号', width: 130, visible: true },
  { prop: 'customerName', label: '客户名称', width: 150, minWidth: 140, visible: true },
  { prop: 'projectName', label: '项目名称', width: 220, minWidth: 180, visible: true },
  { prop: 'region', label: '区域', width: 90, visible: true },
  { prop: 'projectStage', label: '项目阶段', width: 90, visible: true },
  { prop: 'bidStatus', label: '招标状态', width: 90, visible: true },
  { prop: 'salesName', label: '销售', width: 90, visible: true },
  { prop: 'presalesName', label: '售前', width: 90, visible: false },
  { prop: 'testerNames', label: '测试人员', width: 120, visible: true },
  { prop: 'testType', label: '测试类型', width: 110, visible: false, custom: true },
  { prop: 'deviceType', label: '测试设备', width: 110, visible: false },
  { prop: 'status', label: '状态', width: 90, visible: true, custom: true, tooltip: false },
  { prop: 'isKeyProject', label: '重点', width: 70, visible: true, custom: true, tooltip: false },
  { prop: 'testStartTime', label: '开始时间', width: 110, visible: false },
  { prop: 'testEndTime', label: '结束时间', width: 110, visible: false },
  { prop: 'updateTime', label: '更新时间', width: 160, visible: true, custom: true, tooltip: false }
]

const allColumns = ref(loadColumns())
const colWidths = ref(loadColWidths())

function loadColumns() {
  try {
    const saved = JSON.parse(localStorage.getItem('project_columns') || 'null')
    if (Array.isArray(saved)) {
      return defaultColumns.map(dc => {
        const s = saved.find(x => x.prop === dc.prop)
        return { ...dc, visible: s ? s.visible : dc.visible }
      })
    }
  } catch {}
  return defaultColumns.map(c => ({ ...c }))
}

function loadColWidths() {
  try {
    const saved = JSON.parse(localStorage.getItem('project_col_widths') || 'null')
    if (saved) return saved
  } catch {}
  const w = {}
  defaultColumns.forEach(c => { w[c.prop] = c.width })
  return w
}

const visibleColumns = computed(() => allColumns.value.filter(c => c.visible))

const saveColumns = () => {
  localStorage.setItem('project_columns', JSON.stringify(allColumns.value.map(c => ({ prop: c.prop, visible: c.visible }))))
}
watch(allColumns, saveColumns, { deep: true })

// ===== 列宽拖动 =====
let resizeState = null
const startResize = (e, prop) => {
  e.preventDefault()
  e.stopPropagation()
  const startX = e.clientX
  const startWidth = colWidths.value[prop] || 100
  resizeState = { prop, startX, startWidth }
  document.addEventListener('mousemove', onResizing)
  document.addEventListener('mouseup', stopResize)
}
const onResizing = (e) => {
  if (!resizeState) return
  const diff = e.clientX - resizeState.startX
  const newWidth = Math.max(60, resizeState.startWidth + diff)
  colWidths.value[resizeState.prop] = newWidth
}
const stopResize = () => {
  if (resizeState) {
    localStorage.setItem('project_col_widths', JSON.stringify(colWidths.value))
  }
  resizeState = null
  document.removeEventListener('mousemove', onResizing)
  document.removeEventListener('mouseup', stopResize)
}

const load = async () => {
  loading.value = true
  try {
    const res = await getProjectPage(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value = { pageNum: 1, pageSize: query.value.pageSize, status: '', region: '', keyword: '', tester: '', testType: '', deviceType: '', period: '', testStartFrom: '', testStartTo: '' }
  load()
}

const loadFilterOptions = async () => {
  const [dictRes, testerRes, deviceRes] = await Promise.all([
    getAllDict(),
    getDimensionStats('tester'),
    getDimensionStats('deviceType')
  ])
  dicts.value = dictRes.data
  testerOptions.value = Object.keys(testerRes.data || {})
  deviceOptions.value = Object.keys(deviceRes.data || {})
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(`确认删除项目【${row.projectName}】？将同时删除该项目的进展记录和测试报告，不可恢复！`, '警告', { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' })
  await deleteProject(row.id)
  ElMessage.success('项目已删除')
  load()
}

const handleExport = () => {
  const params = new URLSearchParams()
  if (query.value.status) params.append('status', query.value.status)
  if (query.value.region) params.append('region', query.value.region)
  const token = localStorage.getItem('token')
  window.open(`/api/project/export?${params.toString()}&token=${token}`, '_blank')
}

onMounted(() => { load(); loadFilterOptions() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 8px; }
.col-header { display: flex; align-items: center; justify-content: space-between; }
.col-resize-handle { display: inline-block; width: 8px; height: 20px; cursor: col-resize; border-right: 2px solid #dcdfe6; margin-left: 4px; }
.col-resize-handle:hover { border-right-color: #409eff; }
</style>
