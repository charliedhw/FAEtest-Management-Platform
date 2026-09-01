<template>
  <div>
    <!-- 顶部工具栏 -->
    <div style="display:flex;justify-content:flex-end;margin-bottom:12px">
      <el-button type="primary" @click="openExport"><el-icon><Download /></el-icon>导出统计报表(PDF)</el-button>
    </div>
    <!-- 顶部统计卡片 -->
    <el-row :gutter="16" class="stat-cards">
      <el-col :span="6"><el-card shadow="hover"><div class="stat-card"><div class="stat-num">{{ total }}</div><div class="stat-label">项目总数</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="stat-card"><div class="stat-num" style="color:#e6a23c">{{ inProgress }}</div><div class="stat-label">进行中</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="stat-card"><div class="stat-num" style="color:#67c23a">{{ completed }}</div><div class="stat-label">已完成</div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="stat-card"><div class="stat-num" style="color:#f56c6c">{{ paused }}</div><div class="stat-label">暂停</div></div></el-card></el-col>
    </el-row>

    <!-- 可自定义维度的图表卡片 -->
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12" v-for="(slot, idx) in slots" :key="idx">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="chart-title">{{ dimLabel(slot.dimension) }}</span>
              <div style="display:flex;gap:6px;align-items:center">
                <!-- 时间维度选中时,显示月/季/半年/年切换 -->
                <el-radio-group v-if="slot.dimension === 'timeDimension'" v-model="slot.timeUnit" size="small" @change="onDimChange(idx)">
                  <el-radio-button value="month">月</el-radio-button>
                  <el-radio-button value="quarter">季</el-radio-button>
                  <el-radio-button value="halfYear">半年</el-radio-button>
                  <el-radio-button value="year">年</el-radio-button>
                </el-radio-group>
                <el-button size="small" text type="primary" @click="exportImage(idx)">
                  <el-icon><Download /></el-icon>图片
                </el-button>
                <el-select v-model="slot.dimension" size="small" style="width:150px" @change="onDimChange(idx)">
                  <el-option v-for="d in dimensionOptions" :key="d.value" :label="d.label" :value="d.value" />
                </el-select>
              </div>
            </div>
          </template>
          <div :ref="el => setChartRef(el, idx)" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 导出PDF对话框 -->
    <el-dialog v-model="exportVisible" title="导出统计报表" width="500px">
      <el-checkbox-group v-model="exportDims">
        <el-checkbox v-for="d in dimensionOptions" :key="d.value" :value="d.value" style="display:block;margin-bottom:6px">{{ d.label }}</el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="exportPdf">导出PDF</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { jsPDF } from 'jspdf'
import { ElMessage } from 'element-plus'
import { getDashboard, getDimensionStats, getTimeDimension } from '../../api'

const total = ref(0)
const inProgress = ref(0)
const completed = ref(0)
const paused = ref(0)

// 导出相关
const exportVisible = ref(false)
const exporting = ref(false)
const exportDims = ref(['status', 'testType', 'region', 'monthly'])

// 维度选项
const dimensionOptions = [
  { label: '项目状态分布', value: 'status' },
  { label: '测试类型分布', value: 'testType' },
  { label: '测试设备类型分布', value: 'deviceType' },
  { label: '测试周期分布', value: 'period' },
  { label: '区域分布', value: 'region' },
  { label: '月度申请趋势', value: 'monthly' },
  { label: '测试时间维度统计', value: 'timeDimension' },
  { label: '销售项目排行', value: 'sales' },
  { label: '售前项目排行', value: 'presales' },
  { label: '测试人员工作量', value: 'tester' },
  { label: '资源属性分布', value: 'internalResource' },
  { label: '中标状态分布', value: 'bidStatus' }
]

// 4个图表槽位，默认维度，从localStorage恢复
const defaultSlots = ['status', 'testType', 'region', 'monthly']
const slots = ref(loadSlots())

function loadSlots() {
  try {
    const saved = JSON.parse(localStorage.getItem('dashboard_slots') || 'null')
    if (Array.isArray(saved) && saved.length === 4) {
      return saved.map(d => ({ dimension: d, timeUnit: 'month' }))
    }
  } catch {}
  return defaultSlots.map(d => ({ dimension: d, timeUnit: 'month' }))
}

const chartRefs = ref({})
const chartInstances = ref({})

const setChartRef = (el, idx) => {
  if (el) chartRefs.value[idx] = el
}

const dimLabel = (dim) => {
  const found = dimensionOptions.find(d => d.value === dim)
  return found ? found.label : dim
}

// 判断维度适合的图表类型
const chartTypeFor = (dim) => {
  if (['status', 'testType', 'deviceType', 'period', 'internalResource', 'bidStatus'].includes(dim)) return 'pie'
  if (['monthly'].includes(dim)) return 'line'
  return 'bar' // region, sales, presales, tester, timeDimension
}

const renderChart = async (idx) => {
  const slot = slots.value[idx]
  const dim = slot.dimension
  // 时间维度走专门接口
  const res = dim === 'timeDimension'
    ? await getTimeDimension(slot.timeUnit || 'month')
    : await getDimensionStats(dim)
  const data = res.data || {}
  const el = chartRefs.value[idx]
  if (!el) return
  if (chartInstances.value[idx]) {
    chartInstances.value[idx].dispose()
  }
  const chart = echarts.init(el)
  chartInstances.value[idx] = chart

  const type = chartTypeFor(dim)
  const entries = Object.entries(data)

  if (type === 'pie') {
    chart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', right: 10, top: 'center', type: 'scroll' },
      series: [{
        type: 'pie', radius: ['35%', '60%'], center: ['42%', '50%'],
        data: entries.map(([k, v]) => ({ name: k, value: v })),
        label: { formatter: '{b}: {c}' }
      }]
    })
  } else if (type === 'line') {
    const keys = entries.map(e => e[0]).sort()
    chart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, bottom: 40, top: 30 },
      xAxis: { type: 'category', data: keys },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: keys.map(k => data[k]), smooth: true, areaStyle: {}, itemStyle: { color: '#1a2a6c' } }]
    })
  } else {
    // bar: 时间维度按时间排序,其他按数值从高到低排序
    const sorted = dim === 'timeDimension'
      ? entries.slice().sort((a, b) => String(a[0]).localeCompare(String(b[0])))
      : entries.slice().sort((a, b) => (b[1] || 0) - (a[1] || 0))
    const keys = sorted.map(e => e[0])
    chart.setOption({
      tooltip: {},
      grid: { left: 40, right: 20, bottom: 70, top: 30 },
      xAxis: { type: 'category', data: keys, axisLabel: { rotate: 35, interval: 0 } },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: sorted.map(e => e[1]), itemStyle: { color: dim === 'timeDimension' ? '#1a2a6c' : '#c41e3a' } }]
    })
  }
}

const onDimChange = (idx) => {
  // 保存到localStorage
  localStorage.setItem('dashboard_slots', JSON.stringify(slots.value.map(s => s.dimension)))
  renderChart(idx)
}

// ===== 导出功能 =====
// 构建某维度的echarts配置
const buildOption = (dim, data) => {
  const type = chartTypeFor(dim)
  const entries = Object.entries(data || {})
  const base = {
    backgroundColor: '#fff',
    title: { text: dimLabel(dim), left: 'center', top: 10, textStyle: { fontSize: 18, color: '#1a2a6c' } }
  }
  if (type === 'pie') {
    return { ...base, series: [{ type: 'pie', radius: ['30%', '55%'], center: ['45%', '58%'], data: entries.map(([k, v]) => ({ name: k, value: v })), label: { formatter: '{b}: {c}', fontSize: 12 } }], legend: { orient: 'vertical', right: 10, top: 'middle', type: 'scroll' } }
  } else if (type === 'line') {
    const keys = entries.map(e => e[0]).sort()
    return { ...base, grid: { left: 50, right: 30, bottom: 50, top: 60 }, xAxis: { type: 'category', data: keys }, yAxis: { type: 'value' }, series: [{ type: 'line', data: keys.map(k => data[k]), smooth: true, areaStyle: {}, itemStyle: { color: '#1a2a6c' } }] }
  }
  const sortedBar = entries.slice().sort((a, b) => (b[1] || 0) - (a[1] || 0))
  const keys = sortedBar.map(e => e[0])
  return { ...base, grid: { left: 50, right: 30, bottom: 90, top: 60 }, xAxis: { type: 'category', data: keys, axisLabel: { rotate: 35, interval: 0, fontSize: 11 } }, yAxis: { type: 'value' }, series: [{ type: 'bar', data: sortedBar.map(e => e[1]), itemStyle: { color: '#c41e3a' } }] }
}

// 离屏渲染指定维度为图片
const renderDimImage = (dim) => {
  return new Promise(async (resolve, reject) => {
    try {
      const res = await getDimensionStats(dim)
      const data = res.data || {}
      const container = document.createElement('div')
      // 必须在可视布局中才能正确渲染(用fixed移出视口但保留布局)
      container.style.cssText = 'position:fixed;top:0;left:0;width:800px;height:480px;z-index:-9999;pointer-events:none;'
      document.body.appendChild(container)
      const chart = echarts.init(container, null, { width: 800, height: 480 })
      chart.setOption({ ...buildOption(dim, data), animation: false })
      // 等待渲染完成事件
      chart.on('finished', () => {
        const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
        chart.dispose()
        document.body.removeChild(container)
        resolve(url)
      })
      // 兜底: 500ms 超时强制取图
      setTimeout(() => {
        try {
          const url = chart.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#fff' })
          chart.dispose()
          if (document.body.contains(container)) document.body.removeChild(container)
          resolve(url)
        } catch (e) { reject(e) }
      }, 500)
    } catch (e) {
      reject(e)
    }
  })
}

// 导出单个图表为PNG
const exportImage = async (idx) => {
  const dim = slots.value[idx].dimension
  const url = await renderDimImage(dim)
  const link = document.createElement('a')
  link.href = url
  link.download = `${dimLabel(dim)}.png`
  link.click()
  ElMessage.success('图片已导出')
}

const openExport = () => { exportVisible.value = true }

// 导出选中维度为PDF
const exportPdf = async () => {
  if (exportDims.value.length === 0) { ElMessage.warning('请至少选择一个分布类型'); return }
  exporting.value = true
  try {
    const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' })
    const pageW = pdf.internal.pageSize.getWidth()
    const pageH = pdf.internal.pageSize.getHeight()
    for (let i = 0; i < exportDims.value.length; i++) {
      const dim = exportDims.value[i]
      const img = await renderDimImage(dim)
      if (i > 0) pdf.addPage()
      // 图表图片铺满页面(留边距)
      const margin = 15
      const imgW = pageW - margin * 2
      const imgH = (imgW * 480) / 800
      pdf.addImage(img, 'PNG', margin, (pageH - imgH) / 2, imgW, imgH)
    }
    pdf.save('项目统计报表.pdf')
    ElMessage.success('PDF已导出')
    exportVisible.value = false
  } catch (e) {
    ElMessage.error('导出失败: ' + e.message)
  } finally {
    exporting.value = false
  }
}

const handleResize = () => {
  Object.values(chartInstances.value).forEach(c => c && c.resize())
}

onMounted(async () => {
  const res = await getDashboard()
  const d = res.data
  total.value = d.total || 0
  inProgress.value = d.status?.IN_PROGRESS || 0
  completed.value = d.status?.COMPLETED || 0
  paused.value = d.status?.PAUSED || 0

  await nextTick()
  for (let i = 0; i < slots.value.length; i++) {
    await renderChart(i)
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  Object.values(chartInstances.value).forEach(c => c && c.dispose())
})
</script>

<style scoped>
.stat-cards .el-card { border-radius: 8px; }
.stat-card { text-align: center; padding: 10px 0; }
.stat-num { font-size: 32px; font-weight: bold; color: #1a2a6c; }
.stat-label { font-size: 14px; color: #999; margin-top: 4px; }
.chart-card { border-radius: 8px; }
.chart-header { display: flex; justify-content: space-between; align-items: center; }
.chart-title { font-weight: bold; font-size: 15px; }
</style>
