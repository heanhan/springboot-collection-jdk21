import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { getToken } from '@/utils/request'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'system/user',
        name: 'SysUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'User' }
      },
      {
        path: 'system/role',
        name: 'SysRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'UserFilled' }
      },
      {
        path: 'system/permission',
        name: 'SysPermission',
        component: () => import('@/views/system/permission/index.vue'),
        meta: { title: '权限管理', icon: 'Key' }
      },
      {
        path: 'system/menu',
        name: 'SysMenu',
        component: () => import('@/views/system/menu/index.vue'),
        meta: { title: '菜单管理', icon: 'Menu' }
      },
      {
        path: 'system/client',
        name: 'OAuthClient',
        component: () => import('@/views/system/client/index.vue'),
        meta: { title: '客户端管理', icon: 'Connection' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { title: '个人中心', icon: 'Avatar', hidden: true }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard', meta: { hidden: true } }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const hasToken = !!getToken()
  if (to.path === '/login') {
    hasToken ? next('/') : next()
    return
  }
  if (!hasToken) {
    next('/login')
    return
  }
  next()
})

export default router
