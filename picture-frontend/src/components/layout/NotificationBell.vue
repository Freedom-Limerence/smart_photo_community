<template>
  <a-dropdown placement="bottomRight" trigger="click" @openChange="onOpen">
    <button class="bell" type="button" :title="unread > 0 ? unread + ' 条未读通知' : '通知'">
      <a-badge :count="unread" :overflow-count="99" size="small">
        <BellOutlined class="bell-icon" />
      </a-badge>
    </button>
    <template #overlay>
      <div class="bell-panel">
        <div class="bell-head">
          <strong>通知</strong>
          <a-button type="link" size="small" :disabled="!unread" @click="readAll">全部已读</a-button>
        </div>
        <a-spin :spinning="loading">
          <ul v-if="items.length" class="bell-list">
            <li v-for="item in items" :key="item.id" :class="{ unread: !item.isRead }" @click="openItem(item)">
              <span class="dot" :class="item.type.toLowerCase()" />
              <div class="bell-body">
                <strong class="ellipsis">{{ item.title }}</strong>
                <span class="muted">{{ timeText(item.createTime) }}</span>
              </div>
            </li>
          </ul>
          <a-empty v-else-if="!loading" :image="false" description="暂无通知" />
        </a-spin>
        <div class="bell-foot">
          <router-link to="/notifications" @click="close">查看全部通知 →</router-link>
        </div>
      </div>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BellOutlined } from '@ant-design/icons-vue'
import { listNotifications, markNotificationsRead, type NotificationItem } from '@/api/notification'
import { useNotificationStore } from '@/stores/useNotificationStore'

const router = useRouter()
const store = useNotificationStore()
const items = ref<NotificationItem[]>([])
const loading = ref(false)
const unread = computed(() => store.unread)

async function load() {
  loading.value = true
  try {
    const res: any = await listNotifications({ current: 1, size: 5 })
    if (res.data.code === 0) items.value = res.data.data?.records || []
  } finally {
    loading.value = false
  }
}

function onOpen(open: boolean) {
  if (open) void load()
}

async function readAll() {
  await markNotificationsRead({ all: true })
  items.value = items.value.map((i) => ({ ...i, isRead: 1 }))
  store.setUnread(0)
}

async function openItem(item: NotificationItem) {
  if (!item.isRead) {
    await markNotificationsRead({ ids: [item.id] })
    item.isRead = 1
    store.setUnread(unread.value - 1)
  }
  if (item.targetType === 'work' && item.targetId) router.push('/works/' + item.targetId)
  else if (item.targetType === 'user' && item.targetId) router.push('/photographers/' + item.targetId)
  else router.push('/notifications')
}

function close() {
  document.body.click()
}

function timeText(value: string) {
  if (!value) return ''
  const diff = Date.now() - new Date(value).getTime()
  const minute = Math.floor(diff / 60000)
  if (minute < 1) return '刚刚'
  if (minute < 60) return minute + ' 分钟前'
  const hour = Math.floor(minute / 60)
  if (hour < 24) return hour + ' 小时前'
  return Math.floor(hour / 24) + ' 天前'
}
</script>

<style scoped>
.bell { display: grid; place-items: center; width: 38px; height: 38px; border: 1px solid var(--border); border-radius: var(--radius-pill); background: var(--surface); cursor: pointer; transition: box-shadow var(--dur-fast) var(--ease); }
.bell:hover { box-shadow: var(--shadow-2); }
.bell-icon { font-size: 17px; color: var(--text-2); }
.bell-panel { width: 340px; padding: 6px 0 0; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); box-shadow: var(--shadow-3); }
.bell-head { display: flex; align-items: center; justify-content: space-between; padding: 6px 14px 8px; border-bottom: 1px solid var(--border); }
.bell-list { list-style: none; margin: 0; padding: 0; max-height: 320px; overflow-y: auto; }
.bell-list li { display: flex; gap: 10px; padding: 10px 14px; cursor: pointer; transition: background var(--dur-fast) var(--ease); }
.bell-list li:hover { background: var(--surface-2); }
.bell-list li.unread { background: var(--accent-soft); }
.dot { width: 8px; height: 8px; margin-top: 6px; border-radius: 50%; background: var(--text-3); flex: 0 0 auto; }
.dot.like { background: #B4746A; }
.dot.favorite { background: #C2A878; }
.dot.comment { background: #7E8AA0; }
.dot.follow { background: var(--accent); }
.bell-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.bell-body strong { font-size: var(--font-2); font-weight: 500; }
.bell-body span { font-size: 12px; }
.bell-foot { padding: 8px 14px 12px; text-align: center; border-top: 1px solid var(--border); font-size: var(--font-2); }
.bell-foot a { color: var(--accent-hover); }
</style>
