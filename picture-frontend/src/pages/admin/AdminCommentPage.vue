<template>
  <div class="cm-page">
    <div class="page-title">
      <div>
        <span>OPERATIONS / COMMENTS</span>
        <h1>评论管理</h1>
        <p>评论机审会自动隐藏命中风险词的内容；这里可以复核、恢复或补充隐藏</p>
      </div>
      <a-space>
        <a-tag :color="hiddenCount > 0 ? 'red' : 'green'">已隐藏 {{ hiddenCount }}</a-tag>
        <a-radio-group v-model:value="status" button-style="solid" size="small" @change="reload">
          <a-radio-button value="HIDDEN">已隐藏</a-radio-button>
          <a-radio-button value="VISIBLE">正常显示</a-radio-button>
          <a-radio-button value="">全部</a-radio-button>
        </a-radio-group>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </a-space>
    </div>

    <a-card :bordered="false" class="panel">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" size="middle"
               :pagination="{ current: page, pageSize: 10, total, onChange: onChange }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'content'">
            <div class="content">{{ record.content }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'HIDDEN' ? 'red' : 'green'">{{ record.status === 'HIDDEN' ? '已隐藏' : '正常' }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button v-if="record.status === 'HIDDEN'" type="primary" size="small" @click="moderate(record, 'RESTORE')">恢复显示</a-button>
              <a-button v-else size="small" danger @click="openHide(record)">隐藏</a-button>
              <a-button type="link" size="small" @click="router.push('/works/' + record.workId)">查看作品</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="hideOpen" title="隐藏评论" @ok="doHide">
      <a-textarea v-model:value="hideReason" :rows="3" placeholder="隐藏原因（会通知评论作者）" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { listAdminComments, moderateComment } from '@/api/adminConsole'

const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const status = ref('HIDDEN')
const page = ref(1)
const total = ref(0)
const hiddenCount = ref(0)
const hideOpen = ref(false)
const hideReason = ref('')
const current = ref<any>(null)

const columns = [
  { title: 'ID', dataIndex: 'id', width: 80 },
  { title: '评论内容', dataIndex: 'content', ellipsis: true },
  { title: '评论人', dataIndex: 'userId', width: 190 },
  { title: '所属作品', dataIndex: 'workId', width: 190 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '时间', dataIndex: 'createTime', width: 190 },
  { title: '操作', dataIndex: 'action', width: 210 },
]

function openHide(record: any) {
  current.value = record
  hideReason.value = ''
  hideOpen.value = true
}

async function doHide() {
  const res: any = await moderateComment({ id: current.value.id, action: 'HIDE', reason: hideReason.value })
  if (res.data.code === 0) {
    message.success('已隐藏并通知作者')
    hideOpen.value = false
    await load()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

async function moderate(record: any, action: 'HIDE' | 'RESTORE') {
  const res: any = await moderateComment({ id: record.id, action })
  if (res.data.code === 0) {
    message.success(action === 'RESTORE' ? '已恢复显示' : '已隐藏')
    await load()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

async function load() {
  loading.value = true
  try {
    const res: any = await listAdminComments({ status: status.value || undefined, current: page.value, size: 10 })
    if (res.data.code === 0) {
      list.value = res.data.data?.records || []
      total.value = Number(res.data.data?.total || 0)
      hiddenCount.value = Number(res.data.data?.hiddenCount || 0)
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
.page-title span { color: var(--text-3); font-size: 11px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); }
.content { max-width: 460px; }
</style>
