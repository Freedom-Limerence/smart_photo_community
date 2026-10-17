<template>
  <div class="appeal-page">
    <div class="page-title">
      <div>
        <span>OPERATIONS / APPEALS</span>
        <h1>申诉处理</h1>
        <p>作者对「未通过审核」的图片提交申诉；受理后图片重新上架，结果会通知作者</p>
      </div>
      <a-space>
        <a-tag :color="pendingCount > 0 ? 'red' : 'green'">待处理 {{ pendingCount }}</a-tag>
        <a-radio-group v-model:value="status" button-style="solid" size="small" @change="reload">
          <a-radio-button value="PENDING">待处理</a-radio-button>
          <a-radio-button value="ACCEPTED">已受理</a-radio-button>
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
          <template v-if="column.dataIndex === 'picture'">
            <a-space>
              <img v-if="record.thumbnailUrl" :src="record.thumbnailUrl" class="thumb" alt="" />
              <div>
                <a @click="router.push('/picture/' + record.pictureId)">{{ record.pictureName || ('#' + record.pictureId) }}</a>
                <div class="muted small">{{ record.reviewMessage || '' }}</div>
              </div>
            </a-space>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space v-if="record.status === 'PENDING'">
              <a-button type="primary" size="small" @click="openHandle(record, true)">受理并重新上架</a-button>
              <a-button size="small" @click="openHandle(record, false)">驳回</a-button>
            </a-space>
            <span v-else class="muted">{{ record.handleResult || '已处理' }}</span>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="handleOpen" :title="accepted ? '受理申诉（图片重新上架）' : '驳回申诉'" @ok="doHandle">
      <p class="muted">申诉理由：{{ current?.reason }}</p>
      <a-textarea v-model:value="handleResult" :rows="3" :placeholder="accepted ? '复核结论（会通知作者）' : '驳回原因（会通知作者）'" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { handleAppeal, listAdminAppeals } from '@/api/notification'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const status = ref('PENDING')
const page = ref(1)
const total = ref(0)
const pendingCount = ref(0)
const handleOpen = ref(false)
const accepted = ref(true)
const handleResult = ref('')
const current = ref<any>(null)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '图片', dataIndex: 'picture', ellipsis: true },
  { title: '申诉理由', dataIndex: 'reason', ellipsis: true },
  { title: '申诉人', dataIndex: 'userId', width: 180 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '操作', dataIndex: 'action', width: 230 },
]

function statusText(s: string) { return ({ PENDING: '待处理', ACCEPTED: '已受理', REJECTED: '已驳回' } as any)[s] || s }
function statusColor(s: string) { return s === 'PENDING' ? 'orange' : s === 'ACCEPTED' ? 'green' : 'default' }

function openHandle(record: any, isAccepted: boolean) {
  current.value = record
  accepted.value = isAccepted
  handleResult.value = ''
  handleOpen.value = true
}

async function doHandle() {
  const res: any = await handleAppeal({ id: current.value.id, action: accepted.value ? 'ACCEPT' : 'REJECT', result: handleResult.value })
  if (res.data.code === 0) {
    message.success(accepted.value ? '已受理，图片已重新上架' : '已驳回，作者会收到通知')
    handleOpen.value = false
    await load()
  } else {
    message.error(res.data.message || '处理失败')
  }
}

async function load() {
  loading.value = true
  try {
    const res: any = await listAdminAppeals({ status: status.value || undefined, current: page.value, size: 10 })
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
.thumb { width: 56px; height: 40px; object-fit: cover; border-radius: var(--radius-sm); }
.small { font-size: 12px; }
</style>
