<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12"><el-card shadow="hover"><div ref="statusChart" style="height:350px"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="hover"><div ref="typeChart" style="height:350px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="12"><el-card shadow="hover"><div ref="regionChart" style="height:350px"></div></el-card></el-col>
      <el-col :span="12"><el-card shadow="hover"><div ref="monthlyChart" style="height:350px"></div></el-card></el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top:16px">
      <el-col :span="24">
        <el-card shadow="hover">
          <template #header><span>中标统计</span></template>
          <el-row :gutter="16" style="text-align:center">
            <el-col :span="6"><div class="stat-num">{{ bid.wonCount || 0 }}</div><div class="stat-label">已中标项目数</div></el-col>
            <el-col :span="6"><div class="stat-num" style="color:#c41e3a">{{ bid.wonAmount || 0 }}</div><div class="stat-label">中标总金额(万元)</div></el-col>
            <el-col :span="6"><div class="stat-num">{{ bid['未中标Count'] || 0 }}</div><div class="stat-label">未中标</div></el-col>
            <el-col :span="6"><div class="stat-num">{{ bid['未招标Count'] || 0 }}</div><div class="stat-label">未招标</div></el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboard } from '../../api'

const statusChart = ref(); const typeChart = ref(); const regionChart = ref(); const monthlyChart = ref()
const bid = ref({})
const statusMap = { NOT_START: '未开始', IN_PROGRESS: '进行中', PAUSED: '暂停', COMPLETED: '已完成', CLOSED: '关闭', REJECTED: '已驳回' }

onMounted(async () => {
  const res = await getDashboard()
  const d = res.data
  bid.value = d.bid || {}
  await nextTick()

  echarts.init(statusChart.value).setOption({
    title: { text: '项目状态分布', left: 'center' }, tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: '55%', data: Object.entries(d.status || {}).map(([k, v]) => ({ name: statusMap[k] || k, value: v })), label: { formatter: '{b}: {c} ({d}%)' } }]
  })
  echarts.init(typeChart.value).setOption({
    title: { text: '测试类型分布', left: 'center' }, tooltip: { trigger: 'item' },
    series: [{ type: 'pie', radius: ['35%', '60%'], data: Object.entries(d.testType || {}).map(([k, v]) => ({ name: k, value: v })), label: { formatter: '{b}: {c}' } }]
  })
  const regions = Object.keys(d.region || {})
  echarts.init(regionChart.value).setOption({
    title: { text: '区域分布', left: 'center' }, tooltip: {},
    xAxis: { type: 'category', data: regions, axisLabel: { rotate: 35 } }, yAxis: { type: 'value' },
    series: [{ type: 'bar', data: Object.values(d.region || {}), itemStyle: { color: '#c41e3a' } }]
  })
  const months = Object.keys(d.monthly || {}).sort()
  echarts.init(monthlyChart.value).setOption({
    title: { text: '月度申请趋势', left: 'center' }, tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: months }, yAxis: { type: 'value' },
    series: [{ type: 'line', data: months.map(m => d.monthly[m]), smooth: true, areaStyle: {}, itemStyle: { color: '#1a2a6c' } }]
  })
})
</script>

<style scoped>
.stat-num { font-size: 28px; font-weight: bold; color: #1a2a6c; }
.stat-label { font-size: 13px; color: #999; margin-top: 4px; }
</style>
