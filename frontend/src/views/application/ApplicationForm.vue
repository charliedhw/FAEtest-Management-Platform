<template>
  <el-card shadow="never">
    <template #header><span>发起测试申请</span></template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" style="max-width:860px">
      <el-form-item label="客户名称" prop="customerName"><el-input v-model="form.customerName" placeholder="请输入客户名称" /></el-form-item>
      <el-form-item label="项目名称" prop="projectName"><el-input v-model="form.projectName" placeholder="请输入项目名称" /></el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="所属区域" prop="region"><el-input v-model="form.region" placeholder="如:北京" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="项目SPM号" prop="spmNo"><el-input v-model="form.spmNo" placeholder="请输入SPM号" /></el-form-item></el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="项目阶段" prop="projectStage">
            <el-select v-model="form.projectStage" placeholder="请选择项目阶段" style="width:100%">
              <el-option v-for="d in dicts.project_stage" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="招标状态" prop="bidStatus">
            <el-select v-model="form.bidStatus" placeholder="请选择招标状态" style="width:100%">
              <el-option v-for="d in dicts.bid_status" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="项目销售" prop="salesId">
            <el-select v-model="form.salesId" placeholder="请选择销售" filterable style="width:100%" @change="onSalesChange">
              <el-option v-for="u in salesList" :key="u.id" :label="u.realName" :value="u.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="方案售前" prop="presalesId">
            <el-select v-model="form.presalesId" placeholder="请选择售前" filterable style="width:100%" @change="onPresalesChange">
              <el-option v-for="u in presalesList" :key="u.id" :label="u.realName" :value="u.id" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="需求描述" prop="requirement"><el-input v-model="form.requirement" type="textarea" :rows="3" placeholder="请描述测试需求背景" /></el-form-item>
      <el-form-item label="测试计划内容" prop="testPlan"><el-input v-model="form.testPlan" type="textarea" :rows="3" placeholder="测试计划及内容" /></el-form-item>
      <el-form-item label="测试类型" prop="testType">
        <el-select v-model="testTypeArr" multiple placeholder="请选择测试类型" style="width:100%" @change="form.testType = JSON.stringify(testTypeArr)">
          <el-option v-for="d in dicts.test_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备类型">
        <el-select v-model="deviceTypeArr" multiple placeholder="请选择设备类型" style="width:100%" @change="form.deviceType = deviceTypeArr.join(',')">
          <el-option v-for="d in dicts.device_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
        </el-select>
      </el-form-item>
      <el-form-item label="硬件配置"><el-input v-model="form.hardwareConfig" type="textarea" :rows="2" placeholder="CPU/内存/网络/存储等" /></el-form-item>
      <el-form-item label="软件及应用" prop="softwareApp"><el-input v-model="form.softwareApp" type="textarea" :rows="2" placeholder="涉及软件及应用" /></el-form-item>
      <el-form-item label="测试方式" prop="testMethod">
        <el-select v-model="form.testMethod" placeholder="请选择测试方式" style="width:100%">
          <el-option v-for="d in dicts.test_method" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
        </el-select>
      </el-form-item>
      <el-row :gutter="16">
        <el-col :span="12"><el-form-item label="申请天数" prop="applyDays"><el-input-number v-model="form.applyDays" :min="1" :max="365" style="width:100%" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="期望资源类型" prop="expectResourceType">
            <el-select v-model="form.expectResourceType" placeholder="请选择" style="width:100%">
              <el-option v-for="d in dicts.resource_type" :key="d.dictValue" :label="d.dictLabel" :value="d.dictValue" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
        <el-button @click="handleDraft">保存草稿</el-button>
        <el-upload :show-file-list="false" :http-request="handleWordImport" accept=".docx" style="display:inline-block;margin:0 8px">
          <el-button type="success" plain><el-icon><Document /></el-icon>导入Word申请表</el-button>
        </el-upload>
        <el-button @click="$router.back()">取消</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { submitApplication, saveDraft, getAllDict, getApplicationDetail, listUserByRole, importWord } from '../../api'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const submitting = ref(false)
const testTypeArr = ref([])
const deviceTypeArr = ref([])
const dicts = ref({})
const salesList = ref([])
const presalesList = ref([])
const form = reactive({
  id: null, customerName: '', projectName: '', region: '', spmNo: '',
  projectStage: '', bidStatus: '', testMethod: '',
  requirement: '', testPlan: '', testType: '', deviceType: '',
  hardwareConfig: '', softwareApp: '', applyPeriod: '', expectResourceType: '', applyDays: 10,
  salesId: null, salesName: '', presalesId: null, presalesName: ''
})
const rules = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  projectName: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  region: [{ required: true, message: '请输入区域', trigger: 'blur' }],
  spmNo: [{ required: true, message: '请输入项目SPM号', trigger: 'blur' }],
  projectStage: [{ required: true, message: '请选择项目阶段', trigger: 'change' }],
  requirement: [{ required: true, message: '请输入需求描述', trigger: 'blur' }],
  testPlan: [{ required: true, message: '请输入测试计划内容', trigger: 'blur' }],
  softwareApp: [{ required: true, message: '请输入软件及应用', trigger: 'blur' }],
  salesId: [{ required: true, message: '请选择项目销售', trigger: 'change' }],
  presalesId: [{ required: true, message: '请选择方案售前', trigger: 'change' }],
  expectResourceType: [{ required: true, message: '请选择期望资源类型', trigger: 'change' }],
  applyDays: [{ required: true, message: '请输入申请天数', trigger: 'blur' }],
  testType: [{ required: true, validator: (r, v, cb) => testTypeArr.value.length > 0 ? cb() : cb(new Error('请选择测试类型')), trigger: 'change' }]
}

const onSalesChange = (id) => {
  const u = salesList.value.find(x => x.id === id)
  form.salesName = u ? u.realName : ''
}
const onPresalesChange = (id) => {
  const u = presalesList.value.find(x => x.id === id)
  form.presalesName = u ? u.realName : ''
}

const handleSubmit = async () => {
  await formRef.value.validate()
  form.testType = JSON.stringify(testTypeArr.value)
  form.deviceType = deviceTypeArr.value.join(',')
  // 申请周期与天数合并：用天数填充applyPeriod
  form.applyPeriod = form.applyDays + '天'
  submitting.value = true
  try {
    await submitApplication(form)
    ElMessage.success('提交成功，等待审批')
    router.push('/application')
  } finally {
    submitting.value = false
  }
}

const handleDraft = async () => {
  form.testType = JSON.stringify(testTypeArr.value)
  form.deviceType = deviceTypeArr.value.join(',')
  form.applyPeriod = form.applyDays + '天'
  await saveDraft(form)
  ElMessage.success('草稿已保存')
  router.push('/application')
}

// Word导入
const handleWordImport = async ({ file }) => {
  try {
    const res = await importWord(file)
    const { fields, warnings } = res.data
    // 构建确认消息
    const fieldLabels = {
      customerName: '客户名称', projectName: '项目名称', spmNo: 'SPM编号',
      requirement: '需求描述', testPlan: '测试计划', hardwareConfig: '硬件配置',
      testStartTime: '开始时间', testEndTime: '结束时间', applyDays: '申请天数',
      expectResourceType: '期望资源类型', testMethod: '测试方式',
      salesName: '销售人员', presalesName: '方案售前', deviceType: '设备类型',
      testType: '测试类型', softwareApp: '软件及应用', contactPerson: '联系人'
    }
    const lines = []
    for (const [k, v] of Object.entries(fields)) {
      if (k === 'salesId' || k === 'presalesId') continue
      const label = fieldLabels[k] || k
      const display = Array.isArray(v) ? v.join('、') : String(v)
      lines.push(`<b>${label}</b>：${display.length > 80 ? display.substring(0, 80) + '...' : display}`)
    }
    if (warnings && warnings.length > 0) {
      lines.push('<br/><span style="color:#e6a23c">⚠️ 注意事项：</span>')
      warnings.forEach(w => lines.push(`<span style="color:#e6a23c">• ${w}</span>`))
    }
    await ElMessageBox.confirm(
      `<div style="max-height:400px;overflow-y:auto">${lines.join('<br/>')}</div>`,
      'Word导入预览（确认后将自动填充表单）',
      { confirmButtonText: '确认导入', cancelButtonText: '取消', dangerouslyUseHTMLString: true, type: 'info' }
    )
    // 填充表单
    applyImportFields(fields)
    ElMessage.success('导入成功，请确认后提交')
  } catch (e) {
    if (e !== 'cancel') {
      // 错误消息已由拦截器处理
    }
  }
}

// 将解析字段填充到表单
const applyImportFields = (fields) => {
  if (fields.customerName) form.customerName = fields.customerName
  if (fields.projectName) form.projectName = fields.projectName
  if (fields.spmNo) form.spmNo = fields.spmNo
  if (fields.requirement) form.requirement = fields.requirement
  if (fields.testPlan) form.testPlan = fields.testPlan
  if (fields.hardwareConfig) form.hardwareConfig = fields.hardwareConfig
  if (fields.deviceType) {
    form.deviceType = fields.deviceType
    deviceTypeArr.value = String(fields.deviceType).split(/[,，、]/).map(s => s.trim()).filter(Boolean)
  }
  if (fields.softwareApp) form.softwareApp = fields.softwareApp
  if (fields.applyDays) form.applyDays = fields.applyDays
  if (fields.expectResourceType) form.expectResourceType = fields.expectResourceType
  if (fields.testMethod) form.testMethod = fields.testMethod
  if (fields.testType && Array.isArray(fields.testType)) {
    testTypeArr.value = fields.testType
    form.testType = JSON.stringify(fields.testType)
  }
  // 人员匹配
  if (fields.salesId) {
    form.salesId = fields.salesId
    form.salesName = fields.salesName || ''
  }
  if (fields.presalesId) {
    form.presalesId = fields.presalesId
    form.presalesName = fields.presalesName || ''
  }
}

onMounted(async () => {
  const [dictRes, salesRes, presalesRes] = await Promise.all([
    getAllDict(), listUserByRole('SALES'), listUserByRole('PRESALES')
  ])
  dicts.value = dictRes.data
  salesList.value = salesRes.data
  presalesList.value = presalesRes.data
  if (route.query.id) {
    const detail = await getApplicationDetail(route.query.id)
    Object.assign(form, detail.data)
    form.id = null
    try { testTypeArr.value = JSON.parse(detail.data.testType || '[]') } catch { testTypeArr.value = [] }
    deviceTypeArr.value = String(detail.data.deviceType || '').split(/[,，、]/).map(s => s.trim()).filter(Boolean)
  }
})
</script>
