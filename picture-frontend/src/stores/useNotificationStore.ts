import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUnreadCount, type NotificationItem } from '@/api/notification'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const POLL_MS = 60_000

/** 未读通知数（顶栏红点）：登录后每 60 秒轮询一次 */
export const useNotificationStore = defineStore('notification', () => {
  const unread = ref(0)
  const latest = ref<NotificationItem[]>([])
  let timer: number | null = null

  async function refresh() {
    const loginUserStore = useLoginUserStore()
    if (!loginUserStore.loginUser?.id) {
      unread.value = 0
      return
    }
    try {
      const res: any = await getUnreadCount()
      if (res.data.code === 0) unread.value = Number(res.data.data?.count || 0)
    } catch {
      // 未登录或网络异常时静默
    }
  }

  function start() {
    stop()
    void refresh()
    timer = window.setInterval(refresh, POLL_MS)
  }

  function stop() {
    if (timer !== null) {
      window.clearInterval(timer)
      timer = null
    }
  }

  function setUnread(value: number) {
    unread.value = Math.max(0, value)
  }

  return { unread, latest, refresh, start, stop, setUnread }
})
