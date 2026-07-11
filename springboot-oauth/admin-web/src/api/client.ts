import { request } from '@/utils/request'
import type { OAuthClient, PageResult } from './types'

export function pageClients(params: { keyword?: string; pageNum: number; pageSize: number }) {
  return request<PageResult<OAuthClient>>({ url: '/admin/clients', method: 'get', params })
}

export function getClient(id: number) {
  return request<OAuthClient>({ url: `/admin/clients/${id}`, method: 'get' })
}

export function createClient(data: OAuthClient) {
  return request<OAuthClient>({ url: '/admin/clients', method: 'post', data })
}

export function updateClient(data: OAuthClient) {
  return request<OAuthClient>({ url: '/admin/clients', method: 'put', data })
}

export function deleteClient(id: number) {
  return request<void>({ url: `/admin/clients/${id}`, method: 'delete' })
}
