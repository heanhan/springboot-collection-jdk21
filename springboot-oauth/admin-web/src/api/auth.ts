import { request } from '@/utils/request'
import type { CaptchaVO, TokenVO, UserInfoVO, SysMenu } from './types'

/** 获取图形验证码 */
export function getCaptcha() {
  return request<CaptchaVO>({ url: '/auth/captcha', method: 'get' })
}

/** 登录（返回裸 TokenVO） */
export function login(data: {
  username: string
  password: string
  captchaKey?: string
  captchaCode?: string
}) {
  return request<TokenVO>({ url: '/auth/login', method: 'post', data })
}

/** 注册 */
export function register(data: {
  username: string
  password: string
  nickname?: string
  phone?: string
  email?: string
}) {
  return request<number>({ url: '/auth/register', method: 'post', data })
}

/** 当前登录用户信息 */
export function getCurrentUser() {
  return request<UserInfoVO>({ url: '/auth/current', method: 'get' })
}

/** 当前用户菜单树 */
export function getMyMenus() {
  return request<SysMenu[]>({ url: '/auth/menus', method: 'get' })
}

/** 登出 */
export function logout() {
  return request<any>({ url: '/auth/logout', method: 'post' })
}
