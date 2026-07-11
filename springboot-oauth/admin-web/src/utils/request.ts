import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'oauth_access_token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000
})

// 请求拦截：附加 Bearer Token
service.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截：兼容裸对象（如 TokenVO）与 ResultBody{code,message,result}
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const data = response.data
    // 无 code 字段：裸对象直接返回（登录 TokenVO / JWK 等）
    if (data == null || typeof data.code === 'undefined') {
      return data
    }
    // ResultBody 结构
    if (data.code === 200) {
      return data.result
    }
    // 业务错误
    handleAuthError(data.code)
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || 'Error'))
  },
  (error) => {
    const status = error?.response?.status
    const body = error?.response?.data
    if (status === 401) {
      handleAuthError(401)
    } else if (body && body.code) {
      handleAuthError(body.code)
    }
    ElMessage.error(body?.message || error.message || '网络异常')
    return Promise.reject(error)
  }
)

// 认证失效码：102 登录超时；4011/4012/4013/4017 Token 相关；401
function handleAuthError(code: number) {
  const authErrorCodes = [401, 102, 4011, 4012, 4013, 4017]
  if (authErrorCodes.includes(code)) {
    clearToken()
    if (location.hash !== '#/login' && !location.pathname.endsWith('/login')) {
      location.href = '/login'
    }
  }
}

export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service.request<any, T>(config)
}

export default service
