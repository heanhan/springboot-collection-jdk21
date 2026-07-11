<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-title">
        <h2>OAuth 授权中心</h2>
        <p>统一认证 · 权限管理平台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-form-item prop="captchaCode" v-if="captchaEnabled">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" :prefix-icon="Picture" />
            <div class="captcha-img" @click="refreshCaptcha" title="点击刷新">
              <img v-if="captcha.captchaImage" :src="captcha.captchaImage" alt="验证码" />
              <span v-else class="captcha-placeholder">加载中...</span>
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-tip">默认账号：admin / admin123</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock, Picture } from '@element-plus/icons-vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import type { CaptchaVO } from '@/api/types'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaEnabled = ref(true)

const form = reactive({
  username: 'admin',
  password: 'admin123',
  captchaKey: '',
  captchaCode: ''
})

const captcha = reactive<CaptchaVO>({
  captchaKey: '',
  captchaImage: '',
  expireSeconds: 0
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function refreshCaptcha() {
  try {
    const data = await getCaptcha()
    captcha.captchaKey = data.captchaKey
    captcha.captchaImage = data.captchaImage
    form.captchaKey = data.captchaKey
    captchaEnabled.value = true
  } catch (e) {
    // 后端可能关闭验证码，静默降级
    captchaEnabled.value = false
  }
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({
        username: form.username,
        password: form.password,
        captchaKey: form.captchaKey,
        captchaCode: form.captchaCode
      })
      await userStore.fetchUserInfo()
      ElMessage.success('登录成功')
      router.push('/')
    } catch (e) {
      refreshCaptcha()
    } finally {
      loading.value = false
    }
  })
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(-45deg, #0ea5e9, #06b6d4, #14b8a6, #22d3ee);
  background-size: 400% 400%;
  animation: gradientMove 14s ease infinite;
}

@keyframes gradientMove {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

.login-box {
  width: 380px;
  padding: 42px 38px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  box-shadow: 0 20px 50px rgba(31, 45, 90, 0.28);
}

.login-title {
  text-align: center;
  margin-bottom: 28px;
}

.login-title h2 {
  margin: 0 0 8px;
  font-size: 24px;
  background: linear-gradient(135deg, #0891b2 0%, #14b8a6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.login-title p {
  margin: 0;
  color: #999;
  font-size: 13px;
}

.captcha-row {
  display: flex;
  width: 100%;
  gap: 10px;
}

.captcha-img {
  width: 120px;
  height: 40px;
  cursor: pointer;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-placeholder {
  font-size: 12px;
  color: #999;
}

.login-tip {
  text-align: center;
  color: #aaa;
  font-size: 12px;
  margin-top: 8px;
}
</style>
