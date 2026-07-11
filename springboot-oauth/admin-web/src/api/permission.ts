import { request } from '@/utils/request'
import type { PageResult, SysPermission } from './types'

export function pagePermissions(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request<PageResult<SysPermission>>({ url: '/admin/permissions', method: 'get', params })
}

export function listAllPermissions() {
  return request<SysPermission[]>({ url: '/admin/permissions/all', method: 'get' })
}

export function createPermission(data: SysPermission) {
  return request<SysPermission>({ url: '/admin/permissions', method: 'post', data })
}

export function updatePermission(data: SysPermission) {
  return request<SysPermission>({ url: '/admin/permissions', method: 'put', data })
}

export function deletePermission(id: number) {
  return request<void>({ url: `/admin/permissions/${id}`, method: 'delete' })
}
