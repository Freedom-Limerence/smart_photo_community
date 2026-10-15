<template>
  <div class="notify-page container">
    <div class="section-head">
      <div>
        <p class="eyebrow">NOTIFICATIONS</p>
        <h2>通知中心</h2>
        <p>点赞、收藏、评论、关注与平台公告</p>
      </div>
      <a-space>
        <a-radio-group v-model:value="onlyUnread" button-style="solid" size="small" @change="reload">
          <a-radio-button :value="false">全部</a-radio-button>
          <a-radio-button :value="true">未读</a-radio-button>
        </a-radio-group>
        <a-button size="small" :disabled="!store.unread" @click="readAll">全部标为已读</a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <ul v-if="list.length" class="notify-list">
        <li v-for="item in list" :key="item.id" :class="{ unread: !item.isRead }" @click="openItem(item)">
          <div class="type-tag" :class="item.type.toLowerCase()">{{ typeText(item.type) }}</div>
          <div class="notify-body">
            <strong>{{ item.title }}</strong>
            <p v-if="item.content">{{ item.content }}</p>
          </div>
          <span class="muted time">{{ timeText(item.createTime) }}</span>
        </li>
      </ul>
      <a-empty v-else-if="!loading" description="还没有通知" />
    </a-spin>

    <div v-if="total > list.length" class="load-more">
      <a-button :loading="loadingMore" @click="loadMore">加载更多</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listNotifications, markNotificationsRead, type NotificationItem } from '@/api/notification'
import { useNotificationStore } from '@/stores/useNotificationStore'

const router = useRouter()
const store = useNotificationStore()
const list = ref<NotificationItem[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const onlyUnread = ref(false)
const current = ref(1)
const total = ref(0)

const typeMap: Record<string, string> = { LIKE: '点赞', FAVORITE: '收藏', COMMENT: '评论', FOLLOW: '关注', SYSTEM: '公告' }
function typeText(type: string) { return typeMap[type] || '通知' }

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
  void load()
}

async function loadMore() {
  current.value += 1
  loadingMore.value = true
  try {
    await load()
  } finally {
    loadingMore.value = false
  }
}

async function readAll() {
  await markNotificationsRead({ all: true })
  list.value = list.value.map((i) => ({ ...i, isRead: 1 }))
  store.setUnread(0)
}

async function openItem(item: NotificationItem) {
  if (!item.isRead) {
    await markNotificationsRead({ ids: [item.id] })
    item.isRead = 1
    store.setUnread(store.unread - 1)
  }
  if (item.targetType === 'work' && item.targetId) router.push('/works/' + item.targetId)
  else if (item.targetType === 'user' && item.targetId) router.push('/photographers/' + item.targetId)
}

function timeText(value: string) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

onMounted(load)
</script>

<style scoped>
.notify-page { padding: var(--space-6) 0 var(--space-8); }
.notify-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.notify-list li { display: flex; align-items: flex-start; gap: 14px; padding: 16px 18px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); cursor: pointer; transition: all var(--dur-fast) var(--ease); }
.notify-list li:hover { border-color: var(--accent); box-shadow: var(--shadow-2); }
.notify-list li.unread { background: var(--accent-soft); }
.type-tag { flex: 0 0 auto; padding: 3px 10px; border-radius: var(--radius-pill); background: var(--surface-3); color: var(--text-2); font-size: 11px; }
.type-tag.system { background: #EFF1F4; color: #5A6472; }
.notify-body { flex: 1 1 auto; min-width: 0; }
.notify-body strong { font-size: var(--font-3); }
.notify-body p { margin: 4px 0 0; color: var(--text-2); font-size: var(--font-2); }
.time { flex: 0 0 auto; font-size: var(--font-1); }
.load-more { display: flex; justify-content: center; margin-top: var(--space-5); }
</style>
