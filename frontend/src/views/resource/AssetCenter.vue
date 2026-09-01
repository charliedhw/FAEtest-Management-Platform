<template>
  <div>
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>资产中心（已上线资产）</span>
          <el-tag type="info" size="small">只读</el-tag>
        </div>
      </template>
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="资产名称/编号" style="width:220px" clearable @keyup.enter="load" @clear="load" />
        <el-select v-model="query.type" placeholder="资产类型" clearable style="width:150px;margin-left:8px">
          <el-option v-for="d in dicts.resource_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
        </el-select>
        <el-button type="primary" style="margin-left:8px" @click="load">查询</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="resourceCode" label="资产编号" width="130" />
        <el-table-column prop="serialNo" label="设备序列号" width="140" show-overflow-tooltip />
        <el-table-column prop="resourceName" label="资产名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="resourceType" label="类型" width="110" />
        <el-table-column prop="hardwareConfig" label="硬件配置" min-width="220" show-overflow-tooltip />
        <el-table-column prop="location" label="位置" width="120" />
        <el-table-column label="使用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="{ IDLE: 'success', IN_USE: 'warning', MAINTENANCE: 'info' }[row.status]">{{ { IDLE: '空闲', IN_USE: '占用', MAINTENANCE: '维护' }[row.status] }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination style="margin-top:16px;justify-content:flex-end" background layout="total,prev,pager,next" :total="total" :page-size="query.pageSize" :current-page="query.pageNum" @current-change="(p) => { query.pageNum = p; load() }" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAssetPage, getAllDict } from '../../api'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const dicts = ref({})
const query = ref({ pageNum: 1, pageSize: 10, type: '', keyword: '' })

const load = async () => {
  loading.value = true
  try {
    const res = await getAssetPage(query.value)
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

onMounted(async () => {
  load()
  const res = await getAllDict()
  dicts.value = res.data
})
</script>

<style scoped>
.toolbar { margin-bottom: 16px; }
</style>
