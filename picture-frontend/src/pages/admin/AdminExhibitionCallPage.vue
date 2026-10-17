<template>
  <div class="call-admin">
    <div class="page-title">
      <div>
        <span>OPERATIONS / CALL FOR WORKS</span>
        <h1>征稿与投稿管理</h1>
        <p>管理端发起征稿启事，用户在专题页投稿；采纳后作品自动进入专题展品列表</p>
      </div>
      <a-space>
        <a-select v-model:value="exhibitionId" style="width: 260px" placeholder="选择专题"
                  :options="exhibitionOptions" :loading="exhibitionsLoading" @change="onExhibitionChange" />
        <a-button type="primary" :disabled="!exhibitionId" @click="openCallForm()">新建征稿启事</a-button>
      </a-space>
    </div>

    <a-card :bordered="false" class="panel">
      <a-tabs v-model:activeKey="tab">
        <!-- ============ 征稿启事 ============ -->
        <a-tab-pane key="calls" tab="征稿启事">
          <a-table :data-source="calls" :columns="callColumns" :loading="callsLoading" row-key="id" size="middle"
                   :pagination="false">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 'OPEN' ? 'green' : record.status === 'CLOSED' ? 'default' : 'orange'">
                  {{ statusText(record.status) }}
                </a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'deadline'">
                <span :class="{ expired: isExpired(record.deadline) }">{{ fmt(record.deadline) || '不限' }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a-space>
                  <a-button v-if="record.status !== 'OPEN'" type="link" size="small" @click="changeStatus(record, 'OPEN')">开启投稿</a-button>
                  <a-button v-else type="link" size="small" @click="changeStatus(record, 'CLOSED')">关闭投稿</a-button>
                  <a-button type="link" size="small" @click="openCallForm(record)">编辑</a-button>
                  <a-button type="link" size="small" danger @click="removeCall(record)">删除</a-button>
                </a-space>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!callsLoading && !calls.length" description="该专题还没有征稿启事" />
        </a-tab-pane>

        <!-- ============ 投稿管理 ============ -->
        <a-tab-pane key="submissions" tab="投稿管理">
          <a-space style="margin-bottom: 14px">
            <a-select v-model:value="activeCallId" style="width: 300px" placeholder="选择征稿启事"
                      :options="callOptions" @change="loadSubmissions" />
            <a-radio-group v-model:value="submissionStatus" button-style="solid" size="small" @change="loadSubmissions">
              <a-radio-button value="">全部</a-radio-button>
              <a-radio-button value="PENDING">待评选</a-radio-button>
              <a-radio-button value="ACCEPTED">已采纳</a-radio-button>
              <a-radio-button value="REJECTED">已淘汰</a-radio-button>
            </a-radio-group>
            <a-button :loading="submissionsLoading" @click="loadSubmissions">刷新</a-button>
          </a-space>

          <a-table :data-source="submissions" :columns="submissionColumns" :loading="submissionsLoading" row-key="id"
                   size="middle" :pagination="{ current: page, pageSize: 10, total, onChange: onPageChange }">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'work'">
                <div class="work-cell">
                  <img v-if="record.workThumb" :src="record.workThumb" alt="" />
                  <div>
                    <strong>{{ record.workTitle || '未命名作品' }}</strong>
                    <span class="muted">{{ record.caption || '无投稿简述' }}</span>
                  </div>
                </div>
              </template>
              <template v-else-if="column.dataIndex === 'userName'">
                <a-avatar :src="record.userAvatar" :size="26">{{ (record.userName || '用').slice(0, 1) }}</a-avatar>
                <span style="margin-left: 6px">{{ record.userName || '用户' }}</span>
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 'ACCEPTED' ? 'green' : record.status === 'REJECTED' ? 'red' : 'orange'">
                  {{ submissionStatusText(record.status) }}
                </a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a-space v-if="record.status === 'PENDING'">
                  <a-button type="primary" size="small" @click="openAccept(record)">采纳</a-button>
                  <a-button size="small" danger @click="reject(record)">淘汰</a-button>
                </a-space>
                <span v-else class="muted">{{ record.reviewerName || '管理员' }} 已处理</span>
              </template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 征稿表单 -->
    <a-modal v-model:open="callFormOpen" :title="form.id ? '编辑征稿启事' : '新建征稿启事'" @ok="saveCall">
      <a-form layout="vertical">
        <a-form-item label="征稿标题" required>
          <a-input v-model:value="form.title" placeholder="例如：城市夜景征稿" />
        </a-form-item>
        <a-form-item label="征稿说明">
          <a-textarea v-model:value="form.description" :rows="3" placeholder="主题要求、评选标准等" />
        </a-form-item>
        <a-form-item label="截稿时间">
          <a-date-picker v-model:value="form.deadline" show-time style="width: 100%" placeholder="不填表示不限时" />
        </a-form-item>
        <a-form-item label="每人限投数量">
          <a-input-number v-model:value="form.maxSubmissionsPerUser" :min="1" :max="20" style="width: 160px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="form.status">
            <a-radio value="DRAFT">草稿</a-radio>
            <a-radio value="OPEN">开放投稿</a-radio>
            <a-radio value="CLOSED">已关闭</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 采纳（写策展说明） -->
    <a-modal v-model:open="acceptOpen" title="采纳投稿" :confirm-loading="reviewing" ok-text="确认采纳" @ok="doAccept">
      <p class="muted">采纳后，该作品会自动加入专题展品列表（可填写策展人说明）</p>
      <a-textarea v-model:value="acceptCaption" :rows="3" placeholder="策展说明（留空则使用投稿简述）" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  changeExhibitionCallStatus, deleteExhibitionCall, listExhibitionCalls, listExhibitions,
  listSubmissions, reviewSubmission, saveExhibitionCall,
} from '@/api/photoCommunity'

const exhibitionsLoading = ref(false)
const exhibitionId = ref<string | undefined>(undefined)
const exhibitions = ref<any[]>([])
const calls = ref<any[]>([])
const callsLoading = ref(false)
const tab = ref('calls')
const activeCallId = ref<string | undefined>(undefined)
const submissionStatus = ref('')
const submissions = ref<any[]>([])
const submissionsLoading = ref(false)
const page = ref(1)
const total = ref(0)
const callFormOpen = ref(false)
const acceptOpen = ref(false)
const reviewing = ref(false)
const acceptCaption = ref('')
const currentSubmission = ref<any>(null)
const form = ref<any>({ id: undefined, title: '', description: '', deadline: null, maxSubmissionsPerUser: 1, status: 'OPEN' })

const exhibitionOptions = computed(() => exhibitions.value.map((item) => ({ label: item.title, value: String(item.id) })))
const callOptions = computed(() => calls.value.map((item) => ({
  label: item.title + '（' + statusText(item.status) + '）',
  value: String(item.id),
})))

const callColumns = [
  { title: '征稿标题', dataIndex: 'title', ellipsis: true },
  { title: '状态', dataIndex: 'status', width: 110 },
  { title: '截稿时间', dataIndex: 'deadline', width: 190 },
  { title: '每人限投', dataIndex: 'maxSubmissionsPerUser', width: 100 },
  { title: '操作', dataIndex: 'action', width: 260 },
]
const submissionColumns = [
  { title: '投稿作品', dataIndex: 'work', ellipsis: true },
  { title: '作者', dataIndex: 'userName', width: 180 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '投稿时间', dataIndex: 'createTime', width: 180 },
  { title: '操作', dataIndex: 'action', width: 200 },
]

function statusText(status: string) {
  return ({ DRAFT: '草稿', OPEN: '征稿中', CLOSED: '已关闭' } as Record<string, string>)[status] || status
}
function submissionStatusText(status: string) {
  return ({ PENDING: '待评选', ACCEPTED: '已采纳', REJECTED: '已淘汰' } as Record<string, string>)[status] || status
}
function fmt(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}
function isExpired(value?: string) {
  return !!value && new Date(value).getTime() < Date.now()
}

async function loadExhibitions() {
  exhibitionsLoading.value = true
  try {
    const res: any = await listExhibitions({ current: 1, pageSize: 50 })
    exhibitions.value = res.data.code === 0 ? (res.data.data?.records || []) : []
    if (!exhibitionId.value && exhibitions.value.length) {
      exhibitionId.value = String(exhibitions.value[0].id)
      await loadCalls()
    }
  } finally {
    exhibitionsLoading.value = false
  }
}

async function onExhibitionChange() {
  await loadCalls()
}

async function loadCalls() {
  if (!exhibitionId.value) return
  callsLoading.value = true
  try {
    const res: any = await listExhibitionCalls(exhibitionId.value)
    calls.value = res.data.code === 0 ? (res.data.data || []) : []
    if (calls.value.length && !calls.value.some((c) => String(c.id) === String(activeCallId.value))) {
      activeCallId.value = String(calls.value[0].id)
    }
    if (tab.value === 'submissions') await loadSubmissions()
  } finally {
    callsLoading.value = false
  }
}

function openCallForm(record?: any) {
  if (record) {
    form.value = { ...record, deadline: record.deadline ? new Date(record.deadline) : null }
  } else {
    form.value = { id: undefined, title: '', description: '', deadline: null, maxSubmissionsPerUser: 1, status: 'OPEN' }
  }
  callFormOpen.value = true
}

async function saveCall() {
  if (!form.value.title?.trim()) return message.warning('请填写征稿标题')
  const payload: Record<string, unknown> = {
    title: form.value.title,
    description: form.value.description,
    deadline: form.value.deadline ? new Date(form.value.deadline).toISOString() : null,
    maxSubmissionsPerUser: form.value.maxSubmissionsPerUser,
    status: form.value.status,
  }
  if (form.value.id) {
    payload.id = form.value.id
  } else {
    payload.exhibitionId = exhibitionId.value
  }
  const res: any = await saveExhibitionCall(payload, !!form.value.id)
  if (res.data.code === 0) {
    message.success(form.value.id ? '已保存' : '征稿启事已创建')
    callFormOpen.value = false
    await loadCalls()
  } else {
    message.error(res.data.message || '保存失败')
  }
}

async function changeStatus(record: any, status: string) {
  const res: any = await changeExhibitionCallStatus({ id: record.id, status })
  if (res.data.code === 0) {
    message.success(status === 'OPEN' ? '已开启投稿' : '已关闭投稿')
    await loadCalls()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

function removeCall(record: any) {
  Modal.confirm({
    title: '删除征稿启事',
    content: '删除后用户将无法投稿，已采纳的展品不受影响。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      const res: any = await deleteExhibitionCall(record.id)
      if (res.data.code === 0) {
        message.success('已删除')
        await loadCalls()
      } else {
        message.error(res.data.message || '删除失败')
      }
    },
  })
}

async function loadSubmissions() {
  if (!activeCallId.value) return
  submissionsLoading.value = true
  try {
    const res: any = await listSubmissions({
      callId: activeCallId.value,
      status: submissionStatus.value || undefined,
      current: page.value,
      size: 10,
    })
    if (res.data.code === 0) {
      submissions.value = res.data.data?.page?.records || []
      total.value = Number(res.data.data?.page?.total || 0)
    }
  } finally {
    submissionsLoading.value = false
  }
}

function onPageChange(pagination: any) {
  page.value = pagination.current
  void loadSubmissions()
}

function openAccept(record: any) {
  currentSubmission.value = record
  acceptCaption.value = record.caption || ''
  acceptOpen.value = true
}

async function doAccept() {
  reviewing.value = true
  try {
    const res: any = await reviewSubmission({ id: currentSubmission.value.id, accept: true, caption: acceptCaption.value })
    if (res.data.code === 0) {
      message.success('已采纳，作品已加入专题展品')
      acceptOpen.value = false
      await loadSubmissions()
    } else {
      message.error(res.data.message || '采纳失败')
    }
  } finally {
    reviewing.value = false
  }
}

async function reject(record: any) {
  const res: any = await reviewSubmission({ id: record.id, accept: false })
  if (res.data.code === 0) {
    message.success('已淘汰')
    await loadSubmissions()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

onMounted(loadExhibitions)
</script>

<style scoped>
.page-title { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title span { color: var(--text-3); font-size: 12px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); }
.work-cell { display: flex; align-items: center; gap: 10px; }
.work-cell img { width: 62px; height: 44px; object-fit: cover; border-radius: var(--radius-sm); }
.work-cell strong { display: block; font-size: var(--font-2); }
.work-cell span { font-size: 12px; }
.expired { color: var(--danger, #c4564a); }
</style>
