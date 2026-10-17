<template>
  <div class="rbac-page">
    <div class="page-title">
      <div>
        <span>SYSTEM / ACCESS CONTROL</span>
        <h1>角色与权限</h1>
        <p>基于 RBAC 的后台权限矩阵：勾选即调整角色能力，保存后立即生效</p>
      </div>
      <a-space>
        <a-tag color="green">当前角色：{{ myRoleName }}</a-tag>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16">
        <a-col :xs="24" :lg="16">
          <a-card :bordered="false" class="panel" title="权限矩阵">
            <template #extra><span class="muted">勾选后点右上「保存」</span></template>
            <a-table :data-source="rows" :columns="columns" :pagination="false" row-key="module" size="small">
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'perm'">
                  <div class="perm-cell">
                    <a-tag :color="record.module === '系统' ? 'volcano' : 'default'">{{ record.module }}</a-tag>
                    <a-tooltip :title="record.description">
                      <code>{{ record.permCode }}</code>
                    </a-tooltip>
                    <span class="perm-name">{{ record.permName }}</span>
                  </div>
                </template>
                <template v-else-if="column.dataIndex === 'admin'">
                  <a-checkbox :checked="true" disabled />
                </template>
                <template v-else>
                  <a-checkbox :checked="draft[column.dataIndex]?.includes(record.permCode)"
                              :disabled="column.dataIndex === 'admin'"
                              @change="(e:any) => toggle(column.dataIndex, record.permCode, e.target.checked)" />
                </template>
              </template>
            </a-table>

            <div class="save-bar">
              <a-space v-for="role in editableRoles" :key="role.roleCode">
                <a-button type="primary" size="small" :loading="saving === role.roleCode" @click="save(role.roleCode)">
                  保存「{{ role.roleName }}」
                </a-button>
              </a-space>
            </div>
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="panel" title="角色说明">
            <ul class="role-list">
              <li v-for="role in roles" :key="role.roleCode">
                <div class="role-head">
                  <strong>{{ role.roleName }}</strong>
                  <code>{{ role.roleCode }}</code>
                  <a-tag v-if="role.roleCode === 'admin'" color="green">内置</a-tag>
                </div>
                <p class="muted">{{ role.description }}</p>
                <p class="role-perms">{{ (matrix[role.roleCode] || []).length }} 项权限</p>
              </li>
            </ul>
          </a-card>

          <a-card :bordered="false" class="panel assign" title="分配用户角色">
            <a-input v-model:value="assignUserId" placeholder="用户 ID（雪花 ID，从用户管理页复制）" allow-clear />
            <a-select v-model:value="assignRole" style="width: 100%; margin-top: 10px">
              <a-select-option v-for="role in roles" :key="role.roleCode" :value="role.roleCode">
                {{ role.roleName }}（{{ role.roleCode }}）
              </a-select-option>
            </a-select>
            <a-button type="primary" block style="margin-top: 12px" :loading="assigning" :disabled="!assignUserId" @click="assign">
              确认分配
            </a-button>
            <p class="muted tip">分配后该用户刷新页面即可看到对应后台菜单。</p>
          </a-card>
        </a-col>
      </a-row>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { assignUserRole, getRbacMatrix, getRbacMy, updateRolePermissions } from '@/api/adminConsole'

interface Role { roleCode: string; roleName: string; description?: string }
interface Perm { permCode: string; permName: string; module: string; description?: string; sortOrder?: number }

const loading = ref(false)
const saving = ref('')
const assigning = ref(false)
const roles = ref<Role[]>([])
const permissions = ref<Perm[]>([])
const matrix = ref<Record<string, string[]>>({})
const myRole = ref('')
const draft = reactive<Record<string, string[]>>({})
const assignUserId = ref('')
const assignRole = ref('user')

const editableRoles = computed(() => roles.value.filter((r) => r.roleCode !== 'admin' && r.roleCode !== 'banned'))
const myRoleName = computed(() => roles.value.find((r) => r.roleCode === myRole.value)?.roleName || myRole.value)

const rows = computed(() => permissions.value.map((p) => ({ ...p })))

const columns = computed(() => {
  const base: any[] = [{ title: '权限点', dataIndex: 'perm', width: 320 }]
  for (const role of roles.value) {
    base.push({ title: role.roleName, dataIndex: role.roleCode, width: 110, align: 'center' })
  }
  return base
})

function toggle(roleCode: string, permCode: string, checked: boolean) {
  const list = draft[roleCode] ? [...draft[roleCode]] : []
  const index = list.indexOf(permCode)
  if (checked && index < 0) list.push(permCode)
  if (!checked && index >= 0) list.splice(index, 1)
  draft[roleCode] = list
}

async function load() {
  loading.value = true
  try {
    const [mx, my]: any[] = await Promise.all([getRbacMatrix(), getRbacMy()])
    if (mx.data.code !== 0) {
      message.error(mx.data.message || '加载权限矩阵失败')
      return
    }
    roles.value = mx.data.data.roles || []
    permissions.value = mx.data.data.permissions || []
    matrix.value = mx.data.data.mapping || {}
    for (const role of roles.value) {
      draft[role.roleCode] = [...(matrix.value[role.roleCode] || [])]
    }
    if (my.data.code === 0) myRole.value = my.data.data.roleCode
  } finally {
    loading.value = false
  }
}

async function save(roleCode: string) {
  saving.value = roleCode
  try {
    const res: any = await updateRolePermissions({ roleCode, permCodes: draft[roleCode] || [] })
    if (res.data.code === 0) {
      message.success('已保存')
      await load()
    } else {
      message.error(res.data.message || '保存失败')
    }
  } finally {
    saving.value = ''
  }
}

async function assign() {
  assigning.value = true
  try {
    const res: any = await assignUserRole({ userId: assignUserId.value.trim(), roleCode: assignRole.value })
    if (res.data.code === 0) {
      message.success('角色已分配')
      assignUserId.value = ''
    } else {
      message.error(res.data.message || '分配失败')
    }
  } finally {
    assigning.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.rbac-page { display: flex; flex-direction: column; }
.page-title { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title span { color: var(--text-3); font-size: 12px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0 4px; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); margin-bottom: 16px; }
.perm-cell { display: flex; align-items: center; gap: 8px; }
.perm-name { color: var(--text-2); font-size: var(--font-2); }
.save-bar { display: flex; gap: 8px; justify-content: flex-end; margin-top: 16px; }
.role-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 14px; }
.role-head { display: flex; align-items: center; gap: 8px; }
.role-list p { margin: 4px 0 0; font-size: var(--font-2); }
.role-perms { color: var(--accent-hover); }
.assign .tip { margin: 10px 0 0; font-size: 12px; }
</style>
