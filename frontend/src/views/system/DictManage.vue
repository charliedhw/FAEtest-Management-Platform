<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-select v-model="currentType" placeholder="选择字典类型" style="width:200px" @change="load">
          <el-option v-for="t in dictTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
        <el-button type="primary" style="margin-left:auto" @click="openForm()"><el-icon><Plus /></el-icon>新增字典项</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="dictLabel" label="标签" width="200" />
        <el-table-column prop="dictValue" label="值" width="200" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="150" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑字典' : '新增字典'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="字典类型"><el-input v-model="form.dictType" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="值"><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDictByType, saveDict, deleteDict } from '../../api'

const loading = ref(false)
const list = ref([])
const currentType = ref('test_type')
const formVisible = ref(false)
const form = ref({})

const dictTypes = [
  { label: '测试类型', value: 'test_type' },
  { label: '资源类型', value: 'resource_type' },
  { label: '驳回原因', value: 'reject_reason' },
  { label: '中标状态', value: 'bid_status' }
]

const load = async () => {
  loading.value = true
  try {
    const res = await getDictByType(currentType.value)
    list.value = res.data
  } finally { loading.value = false }
}

const openForm = (row) => { form.value = row ? { ...row } : { dictType: currentType.value, sort: 0, status: 1 }; formVisible.value = true }
const handleSave = async () => { await saveDict(form.value); ElMessage.success('保存成功'); formVisible.value = false; load() }
const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除？', '提示', { type: 'warning' })
  await deleteDict(row.id); ElMessage.success('已删除'); load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; margin-bottom: 16px; }
</style>
