<template>
  <div class="report-page">
    <div class="page-title">
      <div>
        <span>OPERATIONS / GOVERNANCE</span>
        <h1>举报处理中心</h1>
        <p>用户举报的受理与处置；处理结果会自动通知举报人</p>
      </div>
      <a-space>
        <a-tag :color="pendingCount > 0 ? 'red' : 'green'">待处理 {{ pendingCount }}</a-tag>
        <a-radio-group v-model:value="status" button-style="solid" size="small" @change="reload">
          <a-radio-button value="PENDING">待处理</a-radio-button>
          <a-radio-button value="RESOLVED">已处置</a-radio-button>
          <a-radio-button value="REJECTED">已驳回</a-radio-button>
          <a-radio-button value="">全部</a-radio-button>
        </a-radio-group>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </a-space>
    </div>

    <a-card :bordered="false" class="panel">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" size="middle"
               :pagination="{ current: page, pageSize: 10, total, onChange: onChange }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'target'">
            <a-tag>{{ targetText(record.targetType) }}</a-tag>
            <a-button type="link" size="small" @click="openTarget(record)">#{{ record.targetId }}</a-button>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'handleResult'">
            <span class="muted">{{ record.handleResult || '—' }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space v-if="record.status === 'PENDING'">
              <a-button type="primary" size="small" @click="openHandle(record, true)">处置</a-button>
              <a-button size="small" @click="openHandle(record, false)">驳回</a-button>
            </a-space>
            <span v-else class="muted">已处理</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="handleOpen" :title="handleResolved ? '处置举报' : '驳回举报'" @ok="doHandle">
      <p class="muted">举报理由：{{ current?.reason }}<span v-if="current?.detail">（{{ current?.detail }}）</span></p>
      <a-textarea v-model:value="handleResult" :rows="3" placeholder="处理说明（会通知举报人）" />
      <a-checkbox v-model:checked="alsoBan" class="ban-check" v-if="handleResolved && current?.targetType === 'work'">
        同时封禁该作品
      </a-checkbox>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { handleReport, listAdminReports, moderateContent } from '@/api/adminConsole'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const status = ref('PENDING')
const page = ref(1)
const total = ref(0)
const pendingCount = ref(0)
const handleOpen = ref(false)
const handleResolved = ref(true)
const handleResult = ref('')
const alsoBan = ref(false)
const current = ref<any>(null)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '被举报对象', dataIndex: 'target', width: 180 },
  { title: '理由', dataIndex: 'reason', width: 140 },
  { title: '补充说明', dataIndex: 'detail', ellipsis: true },
  { title: '举报人', dataIndex: 'reporterId', width: 190 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '处理结果', dataIndex: 'handleResult', width: 180 },
  { title: '操作', dataIndex: 'action', width: 170 },
]

const targetMap: Record<string, string> = { work: '作品', picture: '图片', comment: '评论', user: '用户' }
const statusMap: Record<string, string> = { PENDING: '待处理', RESOLVED: '已处置', REJECTED: '已驳回' }
function targetText(t: string) { return targetMap[t] || t }
function statusText(s: string) { return statusMap[s] || s }
function statusColor(s: string) { return s === 'PENDING' ? 'red' : s === 'RESOLVED' ? 'green' : 'default' }

function openTarget(record: any) {
  if (record.targetType === 'work') router.push('/works/' + record.targetId)
  else if (record.targetType === 'picture') router.push('/picture/' + record.targetId)
}

function openHandle(record: any, resolved: boolean) {
  current.value = record
  handleResolved.value = resolved
  handleResult.value = ''
  alsoBan.value = false
  handleOpen.value = true
}

async function doHandle() {
  const res: any = await handleReport({
    id: current.value.id,
    action: handleResolved.value ? 'RESOLVE' : 'REJECT',
    result: handleResult.value,
  })
  if (res.data.code !== 0) {
    message.error(res.data.message || '处理失败')
    return
  }
  if (handleResolved.value && alsoBan.value && current.value.targetType === 'work') {
    await moderateContent({ type: 'work', action: 'BAN', ids: [current.value.targetId], reason: handleResult.value })
  }
  message.success('已处理，举报人会收到通知')
  handleOpen.value = false
  await load()
}

async function load() {
  loading.value = true
  try {
    const res: any = await listAdminReports({ status: status.value || undefined, current: page.value, size: 10 })
    if (res.data.code === 0) {
      list.value = res.data.data?.records || []
      total.value = Number(res.data.data?.total || 0)
      pendingCount.value = Number(res.data.data?.pendingCount || 0)
    } else {
      message.error(res.data.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

function reload() { page.value = 1; void load() }
function onChange(p: any) { page.value = p.current; void load() }

onMounted(load)
</script>

<style scoped>
.page-title { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title span { color: var(--text-3); font-size: 12px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); }
.ban-check { display: block; margin-top: 12px; }
</style>
