<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '210px'" class="aside">
      <div class="logo">
        <el-icon class="logo-icon"><Lock /></el-icon>
        <span v-if="!collapsed" class="logo-text">OAuth 授权中心</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        router
        background-color="transparent"
        text-color="rgba(255,255,255,0.75)"
        active-text-color="#ffffff"
      >
        <template v-for="item in menuRoutes" :key="item.path">
          <el-menu-item :index="item.fullPath">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <component :is="collapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <span class="page-title">{{ currentTitle }}</span>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><Avatar /></el-icon>
              <span class="nickname">{{ nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)

// 从路由表提取主菜单（排除 hidden）
const menuRoutes = computed(() => {
  const children = router.options.routes.find((r) => r.path === '/')?.children || []
  return children
    .filter((c) => !c.meta?.hidden)
    .map((c) => ({
      fullPath: '/' + c.path,
      path: c.path,
      title: (c.meta?.title as string) || c.name?.toString() || '',
      icon: (c.meta?.icon as string) || 'Menu'
    }))
})

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => (route.meta?.title as string) || '')
const nickname = computed(
  () => userStore.userInfo?.nickname || userStore.userInfo?.username || '未登录'
)

function handleCommand(command: string) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' }).then(async () => {
      await userStore.logout()
      router.push('/login')
    })
  }
}

onMounted(() => {
  if (!userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {})
  }
})
</script>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: var(--aside-gradient);
  transition: width 0.28s;
  overflow: hidden;
  box-shadow: 2px 0 12px rgba(31, 45, 90, 0.12);
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-weight: bold;
  font-size: 16px;
  letter-spacing: 0.5px;
  background: rgba(255, 255, 255, 0.08);
}

.logo-icon {
  font-size: 22px;
}

.logo-text {
  white-space: nowrap;
}

.aside :deep(.el-menu) {
  border-right: none;
  padding: 8px;
}

.aside :deep(.el-menu-item) {
  border-radius: 8px;
  margin-bottom: 4px;
  height: 46px;
}

.aside :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.12) !important;
}

.aside :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(6, 182, 212, 0.9) 0%, rgba(20, 184, 166, 0.9) 100%) !important;
  color: #fff !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(20, 184, 166, 0.35);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  box-shadow: 0 2px 10px rgba(31, 45, 90, 0.06);
  position: relative;
  z-index: 2;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #14b8a6;
  transition: transform 0.2s;
}

.collapse-btn:hover {
  transform: scale(1.15);
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
  padding: 6px 12px;
  border-radius: 20px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #e7f8f5;
}

.user-info .nickname {
  font-weight: 500;
  color: #4b5563;
}

.main {
  background: #f1f5f9;
  padding: 18px;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
