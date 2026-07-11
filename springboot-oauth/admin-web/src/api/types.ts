// 统一返回体（管理接口）
export interface ResultBody<T = any> {
  code: number
  message: string
  result: T
}

// 分页返回（Spring Data Page）
export interface PageResult<T = any> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// 登录令牌
export interface TokenVO {
  accessToken: string
  refreshToken: string
  tokenType: string
  accessExpiresIn: number
  refreshExpiresIn: number
}

// 图形验证码
export interface CaptchaVO {
  captchaKey: string
  captchaImage: string
  expireSeconds: number
}

// 当前用户信息
export interface UserInfoVO {
  userId: number
  username: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  roles: string[]
  permissions: string[]
}

// 用户实体
export interface SysUser {
  id?: number
  username: string
  password?: string
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  status?: number
  createTime?: string
  updateTime?: string
}

// 角色实体
export interface SysRole {
  id?: number
  roleName: string
  roleCode: string
  description?: string
  status?: number
  createTime?: string
}

// 权限实体
export interface SysPermission {
  id?: number
  permName: string
  permCode: string
  type?: number
  url?: string
  method?: string
  description?: string
  status?: number
  createTime?: string
}

// 菜单实体
export interface SysMenu {
  id?: number
  parentId?: number
  menuName: string
  path?: string
  component?: string
  icon?: string
  type?: number
  permission?: string
  sort?: number
  visible?: number
  status?: number
  children?: SysMenu[]
}

// OAuth 客户端
export interface OAuthClient {
  id?: number
  clientId: string
  clientSecret?: string
  clientName?: string
  scopes?: string
  grantTypes?: string
  accessTokenValidity?: number
  status?: number
  createTime?: string
}
