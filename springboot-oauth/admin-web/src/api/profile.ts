import { request } from '@/utils/request'
import type { UserInfoVO } from './types'

/** 查看个人资料 */
export function getProfile() {
  return request<UserInfoVO>({ url: '/user/profile', method: 'get' })
}

/** 更新个人资料 */
export function updateProfile(data: {
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
}) {
  return request<UserInfoVO>({ url: '/user/profile', method: 'put', data })
}

/** 修改密码 */
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return request<boolean>({ url: '/user/profile/password', method: 'put', data })
}
