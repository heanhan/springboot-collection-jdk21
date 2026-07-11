import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getCurrentUser, login as loginApi, logout as logoutApi } from '@/api/auth'
import { setToken, clearToken, getToken } from '@/utils/request'
import type { UserInfoVO } from '@/api/types'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(getToken() || '')
  const userInfo = ref<UserInfoVO | null>(null)

  async function login(payload: {
    username: string
    password: string
    captchaKey?: string
    captchaCode?: string
  }) {
    const tokenVO = await loginApi(payload)
    token.value = tokenVO.accessToken
    setToken(tokenVO.accessToken)
    return tokenVO
  }

  async function fetchUserInfo() {
    const info = await getCurrentUser()
    userInfo.value = info
    return info
  }

  async function logout() {
    try {
      await logoutApi()
    } catch (e) {
      // 忽略登出接口异常，前端强制清理
    }
    reset()
  }

  function reset() {
    token.value = ''
    userInfo.value = null
    clearToken()
  }

  return { token, userInfo, login, fetchUserInfo, logout, reset }
})
