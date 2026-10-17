<template>
  <div class="notify-page container">
    <div class="section-head">
      <div>
        <p class="eyebrow">NOTIFICATIONS</p>
        <h2>通知中心 <small v-if="store.unread">{{ store.unread }} 条未读</small></h2>
        <p>点赞、收藏、评论、关注与平台公告</p>
      </div>
      <a-space>
        <a-button size="small" @click="toggleSelectMode">{{ selectMode ? '退出管理' : '批量管理' }}</a-button>
        <a-button size="small" :disabled="!store.unread" @click="readAll">全部标为已读</a-button>
      </a-space>
    </div>

    <!-- 类型筛选 + 已读筛选 -->
    <div class="filter-bar">
      <a-radio-group v-model:value="type" button-style="solid" size="small">
        <a-radio-button v-for="item in typeOptions" :key="item.value" :value="item.value">
          {{ item.label }}<em v-if="countOf(item.value)">{{ countOf(item.value) }}</em>
        </a-radio-button>
      </a-radio-group>
      <a-radio-group v-model:value="onlyUnread" button-style="solid" size="small" @change="reload">
        <a-radio-button :value="false">全部</a-radio-button>
        <a-radio-button :value="true">未读</a-radio-button>
      </a-radio-group>
      <a-button size="small" :loading="loading" @click="reload">刷新</a-button>
    </div>

    <!-- 批量操作条 -->
    <div v-if="selectMode" class="batch-bar">
      <a-checkbox :checked="allSelected" :indeterminate="picked.length > 0 && !allSelected" @change="toggleAll">
        全选本页（{{ filtered.length }}）
      </a-checkbox>
      <span class="muted">已选 {{ picked.length }} 条</span>
      <a-space>
        <a-button size="small" :disabled="!picked.length" @click="readPicked">标为已读</a-button>
        <a-button size="small" danger :disabled="!picked.length" @click="deletePicked">删除</a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <ul v-if="filtered.length" class="notify-list">
        <li v-for="item in filtered" :key="item.id" :class="[{ unread: !item.isRead }, 'surface-card']">
          <a-checkbox v-if="selectMode" :checked="picked.includes(String(item.id))" @change="togglePick(String(item.id))" />
          <div class="type-tag" :class="String(item.type).toLowerCase()">{{ typeText(item.type) }}</div>
          <div class="notify-body" @click="openItem(item)">
            <strong>{{ item.title }}</strong>
            <p v-if="item.content">{{ item.content }}</p>
          </div>
          <span class="muted time">{{ timeText(item.createTime) }}</span>
          <div class="item-ops">
            <a-button v-if="!item.isRead" type="text" size="small" @click="readOne(item)">标为已读</a-button>
            <a-button type="text" size="small" @click="jump(item)">查看</a-button>
            <a-button type="text" size="small" danger @click="removeOne(item)">删除</a-button>
          </div>
        </li>
      </ul>
      <a-empty v-else-if="!loading" :description="type === 'ALL' ? '还没有通知' : '该类型下没有通知'">
        <a-button type="primary" @click="router.push('/community')">去社区逛逛</a-button>
      </a-empty>
    </a-spin>

    <div v-if="type === 'ALL' && total > list.length" class="load-more">
      <a-button :loading="loadingMore" @click="loadMore">加载更多</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { deleteNotifications, listNotifications, markNotificationsRead, type NotificationItem } from '@/api/notification'
import { useNotificationStore } from '@/stores/useNotificationStore'

const router = useRouter()
const store = useNotificationStore()
const list = ref<NotificationItem[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const onlyUnread = ref(false)
const type = ref('ALL')
const current = ref(1)
const total = ref(0)
const selectMode = ref(false)
const picked = ref<string[]>([])

const typeOptions = [
  { label: '全部', value: 'ALL' },
  { label: '点赞', value: 'LIKE' },
  { label: '收藏', value: 'FAVORITE' },
  { label: '评论', value: 'COMMENT' },
  { label: '关注', value: 'FOLLOW' },
  { label: '公告', value: 'SYSTEM' },
]
const typeMap: Record<string, string> = { LIKE: '点赞', FAVORITE: '收藏', COMMENT: '评论', FOLLOW: '关注', SYSTEM: '公告' }
function typeText(t: string) { return typeMap[t] || '通知' }
function countOf(value: string) {
  return value === 'ALL' ? list.value.length : list.value.filter((i) => i.type === value).length
}

const filtered = computed(() => (type.value === 'ALL' ? list.value : list.value.filter((i) => i.type === type.value)))
const allSelected = computed(() => filtered.value.length > 0 && picked.value.length === filtered.value.length)

function timeText(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const diff = Date.now() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  return d.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

async function load() {
  loading.value = true
  try {
    const res: any = await listNotifications({ current: current.value, size: 20, onlyUnread: onlyUnread.value })
    if (res.data.code === 0) {
      const records = res.data.data?.records || []
      list.value = current.value === 1 ? records : [...list.value, ...records]
      total.value = Number(res.data.data?.total || records.length)
    }
  } finally {
    loading.value = false
  }
}

function reload() {
  current.value = 1
  list.value = []
  picked.value = []
  void load()
}

async function loadMore() {
  loadingMore.value = true
  try {
    current.value += 1
    await load()
  } finally {
    loadingMore.value = false
  }
}

function toggleSelectMode() {
  selectMode.value = !selectMode.value
  picked.value = []
}

function togglePick(id: string) {
  const index = picked.value.indexOf(id)
  if (index >= 0) picked.value.splice(index, 1)
  else picked.value.push(id)
}

function toggleAll() {
  picked.value = allSelected.value ? [] : filtered.value.map((i) => String(i.id))
}

async function readAll() {
  const res: any = await markNotificationsRead({ all: true })
  if (res.data.code === 0) {
    message.success('已全部标为已读')
    await store.refresh()
    reload()
  } else {
    message.error(res.data.message || '操作失败')
  }
}

async function readOne(item: NotificationItem) {
  const res: any = await markNotificationsRead({ ids: [item.id] })
  if (res.data.code === 0) {
    item.isRead = 1
    await store.refresh()
  }
}

async function readPicked() {
  const res: any = await markNotificationsRead({ ids: [...picked.value] })
  if (res.data.code === 0) {
    message.success('已标为已读')
    picked.value = []
    await store.refresh()
    reload()
  }
}

async function doDelete(ids: string[]) {
  const res: any = await deleteNotifications(ids)
  if (res.data.code === 0) {
    message.success('已删除 ' + Number(res.data.data || ids.length) + ' 条通知')
    picked.value = []
    await store.refresh()
    reload()
  } else {
    message.error(res.data.message || '删除失败')
  }
}

function deletePicked() {
  Modal.confirm({
    title: '删除通知',
    content: '将删除所选 ' + picked.value.length + ' 条通知。',
    okText: '确认删除',
    okType: 'danger',
    onOk: () => doDelete([...picked.value]),
  })
}

function removeOne(item: NotificationItem) {
  Modal.confirm({
    title: '删除该通知',
    okText: '确认删除',
    okType: 'danger',
    onOk: () => doDelete([String(item.id)]),
  })
}

async function openItem(item: NotificationItem) {
  if (!item.isRead) await readOne(item)
  jump(item)
}

function jump(item: NotificationItem) {
  const target = item.targetType
  const id = item.targetId
  if (target === 'work' && id) {
    router.push('/works/' + id)
  } else if (target === 'picture' && id) {
    router.push('/picture/' + id)
  } else if (target === 'user' && id) {
    router.push('/photographers/' + id)
  } else if (target === 'exhibition' && id) {
    router.push('/exhibitions/' + id)
  } else {
    message.info('该通知没有关联的页面')
  }
}

onMounted(async () => {
  await store.refresh()
  await load()
})
</script>

<style scoped>
.section-head h2 small { font-size: var(--font-2); color: var(--accent-hover, #7C8B7F); font-weight: 400; margin-left: 8px; }
.filter-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin: var(--space-4) 0; }
.filter-bar em { font-style: normal; margin-left: 4px; opacity: .65; }
.batch-bar { display: flex; align-items: center; gap: 16px; padding: 10px 14px; margin-bottom: var(--space-3); border-radius: var(--radius); background: var(--accent-soft); }
.notify-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 8px; }
.notify-list li { display: flex; align-items: center; gap: 14px; padding: 14px 16px; transition: border-color var(--dur-fast) var(--ease), background-color var(--dur-fast) var(--ease); }
.notify-list li:hover { border-color: var(--accent); }
.notify-list li.unread { background: var(--accent-soft); }
.type-tag { flex: 0 0 auto; padding: 3px 10px; border-radius: var(--radius-pill); background: var(--surface-2); font-size: 12px; color: var(--text-2); }
.type-tag.like { background: #F6E7E7; color: #A2564F; }
.type-tag.comment { background: #E7EFF6; color: #4F6E8A; }
.type-tag.follow { background: #EDF1E7; color: #5F7A4F; }
.type-tag.system { background: #F1EDE7; color: #7A6B4F; }
.notify-body { flex: 1 1 auto; min-width: 0; cursor: pointer; }
.notify-body strong { font-size: var(--font-3); }
.notify-body p { margin: 4px 0 0; color: var(--text-2); font-size: var(--font-2); }
.time { flex: 0 0 auto; font-size: 12px; }
.item-ops { flex: 0 0 auto; display: flex; gap: 2px; opacity: 0; transition: opacity var(--dur-fast) var(--ease); }
.notify-list li:hover .item-ops { opacity: 1; }
.load-more { display: flex; justify-content: center; margin-top: var(--space-5); }
.muted { color: var(--text-3); }

@media (max-width: 768px) {
  .item-ops { opacity: 1; }
  .time { display: none; }
}
</style>
