import request from '@/request'

/** 与 request.ts 保持一致的后端地址 */
const API_BASE = 'http://localhost:8123'

export interface ChatReference {
  type: 'picture' | 'photographer' | 'exhibition' | string
  id: string
  title?: string
  subtitle?: string
  thumbnailUrl?: string
  reason?: string
  url?: string
}

export interface ChatToolCall {
  tool: string
  args?: Record<string, unknown>
  hitCount?: number
  filteredCount?: number
  summary?: string
  reason?: string
}

export interface ChatSendPayload {
  sessionId?: string | number
  message: string
  context?: { pictureId?: string | number; workId?: string | number }
}

export interface StreamHandlers {
  onMeta?: (meta: { sessionId?: string; provider?: string; providerAvailable?: boolean }) => void
  onTool?: (tool: ChatToolCall) => void
  onDelta?: (text: string) => void
  onReferences?: (refs: ChatReference[]) => void
  onDone?: (summary: Record<string, unknown>) => void
  onError?: (payload: { code?: number; message?: string }) => void
}

/** 非流式（降级/兼容） */
export function sendChat(data: ChatSendPayload) {
  return request('/api/ai/chat/send', { method: 'POST', data })
}

export function listChatSessions(params: { current?: number; size?: number } = {}) {
  return request('/api/ai/chat/sessions', { method: 'GET', params })
}

export function listChatMessages(params: { sessionId: string | number; current?: number; size?: number }) {
  return request('/api/ai/chat/messages', { method: 'GET', params })
}

export function deleteChatSession(id: string | number) {
  return request('/api/ai/chat/sessions/' + id, { method: 'DELETE' })
}

/**
 * SSE 流式对话：使用 fetch + ReadableStream 手动解析（EventSource 无法统一 Cookie 语义）
 */
export async function streamChat(payload: ChatSendPayload, handlers: StreamHandlers, signal?: AbortSignal) {
  const res = await fetch(API_BASE + '/api/ai/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(payload),
    signal,
  })
  if (!res.ok || !res.body) {
    throw new Error('连接助手失败（HTTP ' + res.status + '）')
  }
  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const blocks = buffer.split('\n\n')
    buffer = blocks.pop() ?? ''
    for (const block of blocks) {
      let event = 'message'
      const dataLines: string[] = []
      for (const line of block.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      if (!dataLines.length) continue
      let data: any
      try {
        data = JSON.parse(dataLines.join('\n'))
      } catch {
        continue
      }
      if (event === 'meta') handlers.onMeta?.(data)
      else if (event === 'tool') handlers.onTool?.(data)
      else if (event === 'delta') handlers.onDelta?.(data.text ?? '')
      else if (event === 'references') handlers.onReferences?.(data)
      else if (event === 'done') handlers.onDone?.(data)
      else if (event === 'error') handlers.onError?.(data)
    }
  }
}
