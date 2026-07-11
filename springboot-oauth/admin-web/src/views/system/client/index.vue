<template>
  <div>
    <el-card shadow="never">
      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="客户端ID/名称"
          clearable
          style="width: 220px"
          @keyup.enter="loadData"
        />
        <el-button type="primary" :icon="Search" @click="loadData">查询</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        <el-button type="success" :icon="Plus" @click="openCreate">新增客户端</el-button>
      </div>

      <el-table :data="list" v-loading="loading" border stripe style="margin-top: 14px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="clientId" label="客户端ID" />
        <el-table-column prop="clientName" label="客户端名称" />
        <el-table-column prop="scopes" label="授权范围" show-overflow-tooltip />
        <el-table-column prop="grantTypes" label="授权类型" show-overflow-tooltip />
        <el-table-column prop="accessTokenValidity" label="Token有效期(秒)" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">启用</el-tag>
            <el-tag v-else type="danger">禁用</el-tag>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑客户端' : '新增客户端'" width="520px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px">
        <el-form-item label="客户端ID" prop="clientId">
          <el-input v-model="form.clientId" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="客户端密钥" prop="clientSecret">
          <el-input v-model="form.clientSecret" :placeholder="isEdit ? '留空则不修改' : '请输入密钥'" />
        </el-form-item>
        <el-form-item label="客户端名称" prop="clientName">
          <el-input v-model="form.clientName" />
        </el-form-item>
        <el-form-item label="授权范围" prop="scopes">
          <el-input v-model="form.scopes" placeholder="如 read,write" />
        </el-form-item>
        <el-form-item label="授权类型" prop="grantTypes">
          <el-input v-model="form.grantTypes" placeholder="如 client_credentials" />
        </el-form-item>
        <el-form-item label="Token有效期(秒)" prop="accessTokenValidity">
          <el-input-number v-model="form.accessTokenValidity" :min="60" :step="60" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
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
import { pageClients, createClient, updateClient, deleteClient } from '@/api/client'
import type { OAuthClient } from '@/api/types'

const loading = ref(false)
const saving = ref(false)
const list = ref<OAuthClient[]>([])
const total = ref(0)
const query = reactive({ keyword: '', pageNum: 1, pageSize: 10 })

async function loadData() {
  loading.value = true
  try {
    const data = await pageClients(query)
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
const form = reactive<OAuthClient>({
  clientId: '',
  clientSecret: '',
  clientName: '',
  scopes: '',
  grantTypes: 'client_credentials',
  accessTokenValidity: 1800,
  status: 1
})
const formRules: FormRules = {
  clientId: [{ required: true, message: '请输入客户端ID', trigger: 'blur' }]
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, {
    id: undefined,
    clientId: '',
    clientSecret: '',
    clientName: '',
    scopes: '',
    grantTypes: 'client_credentials',
    accessTokenValidity: 1800,
    status: 1
  })
  dialogVisible.value = true
}

function openEdit(row: OAuthClient) {
  isEdit.value = true
  Object.assign(form, { ...row, clientSecret: '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      isEdit.value ? await updateClient(form) : await createClient(form)
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    } finally {
      saving.value = false
    }
  })
}

function handleDelete(row: OAuthClient) {
  ElMessageBox.confirm(`确定删除客户端「${row.clientId}」吗？`, '提示', { type: 'warning' }).then(
    async () => {
      await deleteClient(row.id!)
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
