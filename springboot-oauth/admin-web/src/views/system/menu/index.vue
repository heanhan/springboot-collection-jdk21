<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-button type="success" :icon="Plus" @click="openCreate()">新增菜单</el-button>
        <el-button :icon="Refresh" @click="loadData">刷新</el-button>
      </div>

      <el-table
        :data="treeData"
        v-loading="loading"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
        style="margin-top: 14px"
      >
        <el-table-column prop="menuName" label="菜单名称" />
        <el-table-column prop="path" label="路由路径" />
        <el-table-column prop="component" label="组件" show-overflow-tooltip />
        <el-table-column prop="permission" label="权限标识" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.type === 0" type="warning">目录</el-tag>
            <el-tag v-else-if="row.type === 1">菜单</el-tag>
            <el-tag v-else type="info">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openCreate(row)">新增子级</el-button>
            <el-button link type="warning" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜单' : '新增菜单'" width="500px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="{ label: 'menuName', children: 'children', value: 'id' }"
            check-strictly
            clearable
            placeholder="不选则为顶级菜单"
            node-key="id"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="菜单名称" prop="menuName">
          <el-input v-model="form.menuName" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="0">目录</el-radio>
            <el-radio :value="1">菜单</el-radio>
            <el-radio :value="2">按钮</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="路由路径">
          <el-input v-model="form.path" />
        </el-form-item>
        <el-form-item label="组件">
          <el-input v-model="form.component" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" />
        </el-form-item>
        <el-form-item label="权限标识">
          <el-input v-model="form.permission" placeholder="如 sys:user:list" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
import { Plus, Refresh } from '@element-plus/icons-vue'
import { menuTree, createMenu, updateMenu, deleteMenu } from '@/api/menu'
import type { SysMenu } from '@/api/types'

const loading = ref(false)
const saving = ref(false)
const treeData = ref<SysMenu[]>([])
const parentOptions = ref<SysMenu[]>([])

async function loadData() {
  loading.value = true
  try {
    treeData.value = await menuTree()
    parentOptions.value = [{ id: 0, menuName: '顶级菜单', children: treeData.value } as SysMenu]
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<SysMenu>({
  menuName: '',
  parentId: 0,
  type: 1,
  path: '',
  component: '',
  icon: '',
  permission: '',
  sort: 0
})
const formRules: FormRules = {
  menuName: [{ required: true, message: '请输入菜单名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function reset() {
  Object.assign(form, {
    id: undefined,
    menuName: '',
    parentId: 0,
    type: 1,
    path: '',
    component: '',
    icon: '',
    permission: '',
    sort: 0
  })
}

function openCreate(row?: SysMenu) {
  isEdit.value = false
  reset()
  if (row) form.parentId = row.id
  dialogVisible.value = true
}

function openEdit(row: SysMenu) {
  isEdit.value = true
  reset()
  Object.assign(form, { ...row, children: undefined })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      isEdit.value ? await updateMenu(form) : await createMenu(form)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

function handleDelete(row: SysMenu) {
  ElMessageBox.confirm(`确定删除菜单「${row.menuName}」吗？`, '提示', { type: 'warning' }).then(
    async () => {
      await deleteMenu(row.id!)
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
}
</style>
