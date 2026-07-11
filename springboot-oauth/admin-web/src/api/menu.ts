import { request } from '@/utils/request'
import type { SysMenu } from './types'

export function listAllMenus() {
  return request<SysMenu[]>({ url: '/admin/menus', method: 'get' })
}

export function menuTree() {
  return request<SysMenu[]>({ url: '/admin/menus/tree', method: 'get' })
}

export function createMenu(data: SysMenu) {
  return request<SysMenu>({ url: '/admin/menus', method: 'post', data })
}

export function updateMenu(data: SysMenu) {
  return request<SysMenu>({ url: '/admin/menus', method: 'put', data })
}

export function deleteMenu(id: number) {
  return request<void>({ url: `/admin/menus/${id}`, method: 'delete' })
}
