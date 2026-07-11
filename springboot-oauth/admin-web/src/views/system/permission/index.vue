<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="权限名/编码"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
        />
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新增权限</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="margin-top: 14px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="permName" label="权限名称" />
        <el-table-column prop="permCode" label="权限编码" />
        <el-table-column prop="method" label="请求方法" width="100" />
        <el-table-column prop="url" label="URL" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑权限' : '新增权限'" width="480px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="权限名称" prop="permName">
          <el-input v-model="form.permName" />
        </el-form-item>
        <el-form-item label="权限编码" prop="permCode">
          <el-input v-model="form.permCode" placeholder="如 sys:user:list" />
        </el-form-item>
        <el-form-item label="请求方法" prop="method">
          <el-select v-model="form.method" clearable style="width: 100%">
            <el-option label="GET" value="GET" />
            <el-option label="POST" value="POST" />
            <el-option label="PUT" value="PUT" />
            <el-option label="DELETE" value="DELETE" />
            <el-option label="ALL" value="*" />
          </el-select>
        </el-form-item>
        <el-form-item label="URL" prop="url">
          <el-input v-model="form.url" placeholder="如 /admin/users/**" />
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
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Search, Refresh, Plus } from '@element-plus/icons-vue'
import {
  pagePermissions,
  createPermission,
  updatePermission,
  deletePermission
} from '@/api/permission'
import type { SysPermission } from '@/api/types'

const loading = ref(false)
const saving = ref(false)
const list = ref<SysPermission[]>([])
const total = ref(0)
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const data = await pagePermissions(query)
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

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<SysPermission>({ permName: '', permCode: '', method: '', url: '', description: '' })
const formRules: FormRules = {
  permName: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  permCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }]
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, { id: undefined, permName: '', permCode: '', method: '', url: '', description: '' })
  dialogVisible.value = true
}

function openEdit(row: SysPermission) {
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
      isEdit.value ? await updatePermission(form) : await createPermission(form)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

function handleDelete(row: SysPermission) {
  ElMessageBox.confirm(`确定删除权限「${row.permName}」吗？`, '提示', { type: 'warning' }).then(
    async () => {
      await deletePermission(row.id!)
      ElMessage.success('删除成功')
      loadData()
    }
  )
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
