import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import {
  deleteChatSession, listChatMessages, listChatSessions, streamChat,
  type ChatReference, type ChatToolCall,
} from '@/api/aiChat'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  references?: ChatReference[]
  toolCalls?: ChatToolCall[]
  provider?: string
  streaming?: boolean
  interrupted?: boolean
  error?: string
}

const OPEN_KEY = 'dsh.aiChat.open'

export const useAiChatStore = defineStore('aiChat', () => {
  const open = ref(localStorage.getItem(OPEN_KEY) === '1')
  const sending = ref(false)
  const sessions = ref<any[]>([])
  const activeSessionId = ref<string | undefined>(undefined)
  const messages = ref<ChatMessage[]>([])
  const provider = ref<string>('')
  const providerAvailable = ref(false)
  let controller: AbortController | null = null

  const hasMessages = computed(() => messages.value.length > 0)

  function toggle(next?: boolean) {
    open.value = next === undefined ? !open.value : next
    localStorage.setItem(OPEN_KEY, open.value ? '1' : '0')
  }

  function uid() {
    return Math.random().toString(36).slice(2)
  }

  function newSession() {
    activeSessionId.value = undefined
    messages.value = []
  }

  function safeParse(text: string) {
    try {
      return JSON.parse(text)
    } catch {
      return []
    }
  }

  async function loadSessions() {
    const res: any = await listChatSessions({ current: 1, size: 20 })
    if (res.data.code === 0) sessions.value = res.data.data || []
  }

  async function loadMessages(sessionId: string) {
    const res: any = await listChatMessages({ sessionId, current: 1, size: 50 })
    if (res.data.code !== 0) return
    activeSessionId.value = sessionId
    messages.value = (res.data.data || []).map((m: any) => ({
      id: String(m.id),
      role: m.role,
      content: m.content || '',
      references: m.referencesJson ? safeParse(m.referencesJson) : [],
      toolCalls: m.toolCallsJson ? safeParse(m.toolCallsJson) : [],
      provider: m.provider,
    }))
  }

  async function removeSession(sessionId: string) {
    await deleteChatSession(sessionId)
    sessions.value = sessions.value.filter((s) => String(s.id) !== sessionId)
    if (activeSessionId.value === sessionId) newSession()
  }

  function stop() {
    controller?.abort()
    controller = null
    sending.value = false
    const last = messages.value[messages.value.length - 1]
    if (last?.streaming) {
      last.streaming = false
      last.interrupted = true
    }
  }

  /**
   * 发送消息并消费 SSE 流。
   *
   * 注意：这里所有写入都必须走 messages.value[idx]（响应式代理），
   * 直接修改 push 之前的原始对象不会触发 Vue 更新。
   */
  async function send(text: string, context?: { pictureId?: string; workId?: string }) {
    const loginUserStore = useLoginUserStore()
    const content = text.trim()
    if (!content || sending.value) return
    if (!loginUserStore.loginUser?.id) {
      messages.value.push({
        id: uid(), role: 'assistant',
        content: '请先登录后再使用摄影助手：点击右上角「登录 / 注册」。',
      })
      return
    }

    messages.value.push({ id: uid(), role: 'user', content })
    messages.value.push({
      id: uid(), role: 'assistant', content: '', streaming: true, references: [], toolCalls: [],
    })
    const idx = messages.value.length - 1
    sending.value = true
    controller = new AbortController()

    try {
      await streamChat(
        { sessionId: activeSessionId.value, message: content, context },
        {
          onMeta: (meta) => {
            if (meta.sessionId) activeSessionId.value = meta.sessionId
            provider.value = meta.provider || ''
            providerAvailable.value = !!meta.providerAvailable
            messages.value[idx].provider = meta.provider
          },
          onTool: (tool) => {
            const list = messages.value[idx].toolCalls || []
            messages.value[idx].toolCalls = [...list, tool]
          },
          onDelta: (piece) => {
            messages.value[idx].content = (messages.value[idx].content || '') + piece
          },
          onReferences: (refs) => {
            messages.value[idx].references = refs
          },
          onError: (err) => {
            messages.value[idx].error = err.message || '助手处理失败'
          },
        },
        controller.signal,
      )
    } catch (e: any) {
      if (e?.name === 'AbortError') {
        messages.value[idx].interrupted = true
      } else {
        messages.value[idx].error = e?.message || '连接助手失败'
      }
    } finally {
      messages.value[idx].streaming = false
      if (!messages.value[idx].content && !messages.value[idx].error) {
        messages.value[idx].content = '（没有收到回答，请稍后重试）'
      }
      sending.value = false
      controller = null
      void loadSessions()
    }
  }

  return {
    open, sending, sessions, activeSessionId, messages, provider, providerAvailable, hasMessages,
    toggle, send, stop, newSession, loadSessions, loadMessages, removeSession,
  }
})
