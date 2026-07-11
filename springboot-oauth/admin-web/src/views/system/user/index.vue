<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="用户名/昵称"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
        />
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="margin-top: 14px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="(v: any) => toggleStatus(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openAssignRole(row)">分配角色</el-button>
            <el-button link type="info" @click="openResetPwd(row)">重置密码</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 14px; justify-content: flex-end"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="(p: number) => { query.pageNum = p; loadData() }"
        @size-change="(s: number) => { query.pageSize = s; query.pageNum = 1; loadData() }"
      />
    </el-card>

    <!-- 新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <el-checkbox-group v-model="checkedRoleIds">
        <el-checkbox v-for="r in allRoles" :key="r.id" :value="r.id" style="display: block">
          {{ r.roleName }}（{{ r.roleCode }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitRoles">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="420px">
      <el-input v-model="newPassword" type="password" show-password placeholder="请输入新密码" />
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitResetPwd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  pageUsers,
  createUser,
  updateUser,
  deleteUser,
  changeUserStatus,
  resetUserPassword,
  getUserRoleIds,
  assignUserRoles
} from '@/api/user'
import { listAllRoles } from '@/api/role'
import type { SysUser, SysRole } from '@/api/types'

const loading = ref(false)
const saving = ref(false)
const list = ref<SysUser[]>([])
const total = ref(0)
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const data = await pageUsers(query)
    list.value = data.content
    total.value = data.totalElements
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.keyword = ''
  query.pageNum = 1
  loadData()
}

// 新增/编辑
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<SysUser>({ username: '', password: '', nickname: '', phone: '', email: '' })
const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { id: undefined, username: '', password: '', nickname: '', phone: '', email: '' })
  dialogVisible.value = true
}

function openEdit(row: SysUser) {
  isEdit.value = true
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      isEdit.value ? await updateUser(form) : await createUser(form)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

async function toggleStatus(row: SysUser, val: boolean) {
  try {
    await changeUserStatus(row.id!, val ? 1 : 0)
    row.status = val ? 1 : 0
    ElMessage.success('状态已更新')
  } catch (e) {
    loadData()
  }
}

function handleDelete(row: SysUser) {
  ElMessageBox.confirm(`确定删除用户「${row.username}」吗？`, '提示', { type: 'warning' }).then(
    async () => {
      await deleteUser(row.id!)
      ElMessage.success('删除成功')
      loadData()
    }
  )
}

// 分配角色
const roleDialogVisible = ref(false)
const allRoles = ref<SysRole[]>([])
const checkedRoleIds = ref<number[]>([])
const currentUserId = ref<number>()

async function openAssignRole(row: SysUser) {
  currentUserId.value = row.id
  if (allRoles.value.length === 0) {
    allRoles.value = await listAllRoles()
  }
  checkedRoleIds.value = await getUserRoleIds(row.id!)
  roleDialogVisible.value = true
}

async function submitRoles() {
  saving.value = true
  try {
    await assignUserRoles(currentUserId.value!, checkedRoleIds.value)
    ElMessage.success('分配成功')
    roleDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

// 重置密码
const pwdDialogVisible = ref(false)
const newPassword = ref('')

function openResetPwd(row: SysUser) {
  currentUserId.value = row.id
  newPassword.value = ''
  pwdDialogVisible.value = true
}

async function submitResetPwd() {
  if (!newPassword.value) {
    ElMessage.warning('请输入新密码')
    return
  }
  saving.value = true
  try {
    await resetUserPassword(currentUserId.value!, newPassword.value)
    ElMessage.success('密码已重置')
    pwdDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
