<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="角色名/编码"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
        />
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新增角色</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="margin-top: 14px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleName" label="角色名称" />
        <el-table-column prop="roleCode" label="角色编码" />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="openAssignPerm(row)">分配权限</el-button>
            <el-button link type="success" @click="openAssignMenu(row)">分配菜单</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="460px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="permDialogVisible" title="分配权限" width="480px">
      <el-checkbox-group v-model="checkedPermIds">
        <el-checkbox v-for="p in allPerms" :key="p.id" :value="p.id" style="display: block">
          {{ p.permName }}（{{ p.permCode }}）
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitPerms">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配菜单 -->
    <el-dialog v-model="menuDialogVisible" title="分配菜单" width="480px">
      <el-tree
        ref="menuTreeRef"
        :data="menuTreeData"
        show-checkbox
        node-key="id"
        :props="{ label: 'menuName', children: 'children' }"
        :default-checked-keys="checkedMenuIds"
        default-expand-all
      />
      <template #footer>
        <el-button @click="menuDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitMenus">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TreeInstance } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  pageRoles,
  createRole,
  updateRole,
  deleteRole,
  getRolePermissionIds,
  assignRolePermissions,
  getRoleMenuIds,
  assignRoleMenus
} from '@/api/role'
import { listAllPermissions } from '@/api/permission'
import { menuTree } from '@/api/menu'
import type { SysRole, SysPermission, SysMenu } from '@/api/types'

const loading = ref(false)
const saving = ref(false)
const list = ref<SysRole[]>([])
const total = ref(0)
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const data = await pageRoles(query)
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
const form = reactive<SysRole>({ roleName: '', roleCode: '', description: '' })
const formRules: FormRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { id: undefined, roleName: '', roleCode: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row: SysRole) {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      isEdit.value ? await updateRole(form) : await createRole(form)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

function handleDelete(row: SysRole) {
  ElMessageBox.confirm(`确定删除角色「${row.roleName}」吗？`, '提示', { type: 'warning' }).then(
    async () => {
      await deleteRole(row.id!)
      ElMessage.success('删除成功')
      loadData()
    }
  )
}

const currentRoleId = ref<number>()

// 分配权限
const permDialogVisible = ref(false)
const allPerms = ref<SysPermission[]>([])
const checkedPermIds = ref<number[]>([])

async function openAssignPerm(row: SysRole) {
  currentRoleId.value = row.id
  if (allPerms.value.length === 0) {
    allPerms.value = await listAllPermissions()
  }
  checkedPermIds.value = await getRolePermissionIds(row.id!)
  permDialogVisible.value = true
}

async function submitPerms() {
  saving.value = true
  try {
    await assignRolePermissions(currentRoleId.value!, checkedPermIds.value)
    ElMessage.success('分配成功')
    permDialogVisible.value = false
  } finally {
    saving.value = false
  }
}

// 分配菜单
const menuDialogVisible = ref(false)
const menuTreeRef = ref<TreeInstance>()
const menuTreeData = ref<SysMenu[]>([])
const checkedMenuIds = ref<number[]>([])

async function openAssignMenu(row: SysRole) {
  currentRoleId.value = row.id
  if (menuTreeData.value.length === 0) {
    menuTreeData.value = await menuTree()
  }
  checkedMenuIds.value = await getRoleMenuIds(row.id!)
  menuDialogVisible.value = true
}

async function submitMenus() {
  saving.value = true
  try {
    const ids = [
      ...(menuTreeRef.value?.getCheckedKeys() || []),
      ...(menuTreeRef.value?.getHalfCheckedKeys() || [])
    ] as number[]
    await assignRoleMenus(currentRoleId.value!, ids)
    ElMessage.success('分配成功')
    menuDialogVisible.value = false
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
