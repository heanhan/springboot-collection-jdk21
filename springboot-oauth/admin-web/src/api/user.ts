import { request } from '@/utils/request'
import type { PageResult, SysUser } from './types'

export function pageUsers(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request<PageResult<SysUser>>({ url: '/admin/users', method: 'get', params })
}

export function getUser(id: number) {
  return request<SysUser>({ url: `/admin/users/${id}`, method: 'get' })
}

export function createUser(data: SysUser) {
  return request<SysUser>({ url: '/admin/users', method: 'post', data })
}

export function updateUser(data: SysUser) {
  return request<SysUser>({ url: '/admin/users', method: 'put', data })
}

export function deleteUser(id: number) {
  return request<void>({ url: `/admin/users/${id}`, method: 'delete' })
}

export function changeUserStatus(id: number, status: number) {
  return request<void>({ url: `/admin/users/${id}/status`, method: 'put', params: { status } })
}

export function resetUserPassword(id: number, password: string) {
  return request<void>({ url: `/admin/users/${id}/password`, method: 'put', params: { password } })
}

export function getUserRoleIds(id: number) {
  return request<number[]>({ url: `/admin/users/${id}/roles`, method: 'get' })
}

export function assignUserRoles(id: number, ids: number[]) {
  return request<void>({ url: `/admin/users/${id}/roles`, method: 'put', data: { ids } })
}
