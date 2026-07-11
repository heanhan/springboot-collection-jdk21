import { request } from '@/utils/request'
import type { PageResult, SysRole } from './types'

export function pageRoles(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request<PageResult<SysRole>>({ url: '/admin/roles', method: 'get', params })
}

export function listAllRoles() {
  return request<SysRole[]>({ url: '/admin/roles/all', method: 'get' })
}

export function createRole(data: SysRole) {
  return request<SysRole>({ url: '/admin/roles', method: 'post', data })
}

export function updateRole(data: SysRole) {
  return request<SysRole>({ url: '/admin/roles', method: 'put', data })
}

export function deleteRole(id: number) {
  return request<void>({ url: `/admin/roles/${id}`, method: 'delete' })
}

export function getRolePermissionIds(id: number) {
  return request<number[]>({ url: `/admin/roles/${id}/permissions`, method: 'get' })
}

export function assignRolePermissions(id: number, ids: number[]) {
  return request<void>({ url: `/admin/roles/${id}/permissions`, method: 'put', data: { ids } })
}

export function getRoleMenuIds(id: number) {
  return request<number[]>({ url: `/admin/roles/${id}/menus`, method: 'get' })
}

export function assignRoleMenus(id: number, ids: number[]) {
  return request<void>({ url: `/admin/roles/${id}/menus`, method: 'put', data: { ids } })
}
