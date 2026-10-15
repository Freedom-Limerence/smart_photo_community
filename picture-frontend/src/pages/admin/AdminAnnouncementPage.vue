<template>
  <div class="ann-page">
    <div class="page-title">
      <div>
        <span>OPERATIONS / CONTENT</span>
        <h1>公告管理</h1>
        <p>发布平台公告；发布时会向全体用户推送站内通知，并展示在前台顶部</p>
      </div>
      <a-space>
        <a-button type="primary" @click="openEditor()">新建公告</a-button>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </a-space>
    </div>

    <a-card :bordered="false" class="panel">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" size="middle"
               :pagination="{ current: page, pageSize: 10, total, onChange: onChange }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'level'">
            <a-tag :color="levelColor(record.level)">{{ levelText(record.level) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'PUBLISHED' ? 'green' : record.status === 'DRAFT' ? 'default' : 'orange'">
              {{ statusText(record.status) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openEditor(record)">编辑</a-button>
              <a-button v-if="record.status !== 'PUBLISHED'" type="link" size="small" @click="changeStatus(record, 'PUBLISHED')">发布</a-button>
              <a-button v-else type="link" size="small" @click="changeStatus(record, 'OFFLINE')">下线</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="editorOpen" :title="form.id ? '编辑公告' : '新建公告'" width="640px" @ok="save">
      <a-form layout="vertical">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" placeholder="例如：光影集 1.0 上线公告" />
        </a-form-item>
        <a-form-item label="级别">
          <a-radio-group v-model:value="form.level">
            <a-radio-button value="INFO">普通</a-radio-button>
            <a-radio-button value="WARNING">提醒</a-radio-button>
            <a-radio-button value="IMPORTANT">重要</a-radio-button>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="内容" required>
          <a-textarea v-model:value="form.content" :rows="6" placeholder="公告正文" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { changeAnnouncementStatus, listAdminAnnouncements, saveAnnouncement } from '@/api/adminConsole'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const total = ref(0)
const editorOpen = ref(false)
const form = reactive<{ id?: string; title: string; content: string; level: string }>({ title: '', content: '', level: 'INFO' })

const columns = [
  { title: 'ID', dataIndex: 'id', width: 90 },
  { title: '标题', dataIndex: 'title', ellipsis: true },
  { title: '级别', dataIndex: 'level', width: 100 },
  { title: '状态', dataIndex: 'status', width: 100 },
  { title: '发布时间', dataIndex: 'publishTime', width: 190 },
  { title: '操作', dataIndex: 'action', width: 170 },
]

function levelText(l: string) { return ({ INFO: '普通', WARNING: '提醒', IMPORTANT: '重要' } as any)[l] || l }
function levelColor(l: string) { return ({ INFO: 'default', WARNING: 'orange', IMPORTANT: 'red' } as any)[l] || 'default' }
function statusText(s: string) { return ({ DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' } as any)[s] || s }

function openEditor(record?: any) {
  if (record) {
    form.id = record.id
    form.title = record.title
    form.content = record.content
    form.level = record.level
  } else {
    form.id = undefined
    form.title = ''
    form.content = ''
    form.level = 'INFO'
  }
  editorOpen.value = true
}

async function save() {
  if (!form.title.trim() || !form.content.trim()) {
    message.warning('标题与内容不能为空')
    return
  }
  const res: any = await saveAnnouncement({ ...form })
  if (res.data.code === 0) {
    message.success('已保存（草稿状态，需点「发布」才会推送）')
    editorOpen.value = false
    await load()
  } else {
    message.error(res.data.message || '保存失败')
  }
}

async function changeStatus(record: any, status: string) {
  const res: any = await changeAnnouncementStatus({ id: record.id, status, broadcast: status === 'PUBLISHED' })
  if (res.data.code === 0) {
    message.success(status === 'PUBLISHED' ? '已发布并推送给全体用户' : '已下线')
    await load()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

async function load() {
  loading.value = true
  try {
    const res: any = await listAdminAnnouncements({ current: page.value, size: 10 })
    if (res.data.code === 0) {
      list.value = res.data.data?.records || []
      total.value = Number(res.data.data?.total || 0)
    }
  } finally {
    loading.value = false
  }
}

function onChange(p: any) { page.value = p.current; void load() }

onMounted(load)
</script>

<style scoped>
.page-title { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title span { color: var(--text-3); font-size: 11px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); }
</style>
