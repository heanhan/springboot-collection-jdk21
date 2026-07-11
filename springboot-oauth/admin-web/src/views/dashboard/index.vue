<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card shadow="hover" class="stat-card" :style="{ background: card.color }">
          <div class="stat-icon">
            <el-icon :size="28"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-title">{{ card.title }}</div>
            <div class="stat-value">{{ card.value }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="welcome" shadow="never">
      <h3>欢迎使用 OAuth 授权中心管理平台</h3>
      <p>当前登录用户：<b>{{ userInfo?.nickname || userInfo?.username }}</b></p>
      <p>拥有角色：
        <el-tag v-for="r in userInfo?.roles || []" :key="r" size="small" style="margin-right: 6px">
          {{ r }}
        </el-tag>
      </p>
      <p>技术栈：Vue3 + TypeScript + Vite + Element Plus，后端对接 springboot-oauth-auth (9000)。</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { pageUsers } from '@/api/user'
import { listAllRoles } from '@/api/role'
import { listAllPermissions } from '@/api/permission'
import { pageClients } from '@/api/client'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const stats = reactive({ users: 0, roles: 0, permissions: 0, clients: 0 })

const cards = computed(() => [
  { title: '用户总数', value: stats.users, icon: 'User', color: 'linear-gradient(135deg, #06b6d4 0%, #14b8a6 100%)' },
  { title: '角色总数', value: stats.roles, icon: 'UserFilled', color: 'linear-gradient(135deg, #0ea5e9 0%, #22d3ee 100%)' },
  { title: '权限总数', value: stats.permissions, icon: 'Key', color: 'linear-gradient(135deg, #14b8a6 0%, #34d399 100%)' },
  { title: '客户端总数', value: stats.clients, icon: 'Connection', color: 'linear-gradient(135deg, #3b82f6 0%, #06b6d4 100%)' }
])

async function loadStats() {
  try {
    const [u, r, p, c] = await Promise.all([
      pageUsers({ pageNum: 1, pageSize: 1 }),
      listAllRoles(),
      listAllPermissions(),
      pageClients({ pageNum: 1, pageSize: 1 })
    ])
    stats.users = u.totalElements
    stats.roles = r.length
    stats.permissions = p.length
    stats.clients = c.totalElements
  } catch (e) {
    // 忽略统计加载失败
  }
}

onMounted(loadStats)
</script>

<style scoped>
.stat-card {
  border-radius: 14px !important;
  color: #fff;
  transition: transform 0.25s, box-shadow 0.25s;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 28px rgba(31, 45, 90, 0.22) !important;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 58px;
  height: 58px;
  border-radius: 14px;
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-title {
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
  color: #fff;
}

.welcome {
  margin-top: 16px;
}

.welcome h3 {
  margin-top: 0;
  color: #0f172a;
}
</style>
