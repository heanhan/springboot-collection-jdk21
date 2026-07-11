<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never" header="个人资料">
          <el-form ref="profileRef" :model="profileForm" label-width="80px">
            <el-form-item label="用户名">
              <el-input v-model="profileForm.username" disabled />
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="profileForm.nickname" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never" header="修改密码">
          <el-form ref="pwdRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getProfile, updateProfile, changePassword } from '@/api/profile'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const saving = ref(false)
const savingPwd = ref(false)

const profileRef = ref<FormInstance>()
const profileForm = reactive({ username: '', nickname: '', phone: '', email: '' })

async function loadProfile() {
  const info = await getProfile()
  profileForm.username = info.username
  profileForm.nickname = info.nickname || ''
  profileForm.phone = info.phone || ''
  profileForm.email = info.email || ''
}

async function saveProfile() {
  saving.value = true
  try {
    await updateProfile({
      nickname: profileForm.nickname,
      phone: profileForm.phone,
      email: profileForm.email
    })
    ElMessage.success('保存成功')
    await userStore.fetchUserInfo()
  } finally {
    saving.value = false
  }
}

const pwdRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function savePassword() {
  if (!pwdRef.value) return
  await pwdRef.value.validate(async (valid) => {
    if (!valid) return
    savingPwd.value = true
    try {
      await changePassword({
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword
      })
      ElMessage.success('密码修改成功')
      pwdForm.oldPassword = ''
      pwdForm.newPassword = ''
      pwdForm.confirmPassword = ''
    } finally {
      savingPwd.value = false
    }
  })
}

onMounted(loadProfile)
</script>
