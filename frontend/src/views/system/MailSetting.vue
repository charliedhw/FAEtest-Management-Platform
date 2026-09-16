<template>
  <el-card shadow="never" style="max-width:720px">
    <template #header><span>邮件设置（平台对外发信账号）</span></template>
    <el-form :model="form" label-width="120px">
      <el-form-item label="启用邮件通知"><el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" /></el-form-item>
      <el-form-item label="SMTP主机"><el-input v-model="form.host" placeholder="如 smtp.sugon.com" /></el-form-item>
      <el-form-item label="SMTP端口"><el-input-number v-model="form.port" :min="1" :max="65535" style="width:100%" /></el-form-item>
      <el-form-item label="使用SSL"><el-switch v-model="form.useSsl" :active-value="1" :inactive-value="0" /></el-form-item>
      <el-form-item label="邮箱账号"><el-input v-model="form.username" placeholder="发件邮箱账号" /></el-form-item>
      <el-form-item label="授权码"><el-input v-model="form.password" type="password" show-password placeholder="留空表示不修改" /></el-form-item>
      <el-form-item label="发件人地址"><el-input v-model="form.fromAddr" placeholder="默认使用邮箱账号" /></el-form-item>
      <el-form-item label="发件人名称"><el-input v-model="form.fromName" placeholder="测试项目管理平台" /></el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        <el-input v-model="testAddr" placeholder="测试收件邮箱" style="width:220px;margin:0 8px" />
        <el-button :loading="testing" @click="handleTest">发送测试邮件</el-button>
      </el-form-item>
    </el-form>
    <el-alert type="info" :closable="false" show-icon title="说明：流程节点（提交/审批/驳回/排期/分配）会自动给被指派人发送简洁邮件，内容含待办处理链接。收件邮箱取用户的邮箱字段，无邮箱则跳过。" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMailConfig, saveMailConfig, testMail } from '../../api'

const saving = ref(false)
const testing = ref(false)
const testAddr = ref('')
const form = ref({ enabled: 0, host: '', port: 465, useSsl: 1, username: '', password: '', fromAddr: '', fromName: '测试项目管理平台' })

const load = async () => {
  const res = await getMailConfig()
  if (res.data) form.value = { ...form.value, ...res.data, password: '' }
}
const handleSave = async () => {
  saving.value = true
  try { await saveMailConfig(form.value); ElMessage.success('已保存'); load() } finally { saving.value = false }
}
const handleTest = async () => {
  if (!testAddr.value) { ElMessage.warning('请输入测试收件邮箱'); return }
  testing.value = true
  try { await testMail(testAddr.value); ElMessage.success('测试邮件已发送，请查收') } finally { testing.value = false }
}
onMounted(load)
</script>
