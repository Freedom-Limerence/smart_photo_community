<template>
  <div class="msg" :class="message.role">
    <div v-if="message.role === 'assistant'" class="avatar"><RobotOutlined /></div>
    <div class="bubble-wrap">
      <!-- 检索过程（可解释性） -->
      <div v-if="message.toolCalls?.length" class="tool-trace">
        <span v-for="(tool, i) in message.toolCalls" :key="i" class="tool-chip">
          <SearchOutlined />
          {{ toolLabel(tool) }}
        </span>
      </div>

      <div class="bubble" :class="{ 'is-error': !!message.error }">
        <template v-if="message.error">{{ message.error }}</template>
        <template v-else>
          <span class="text">{{ message.content }}</span>
          <span v-if="message.streaming" class="cursor">▍</span>
          <span v-if="message.interrupted" class="muted">（已停止）</span>
        </template>
      </div>

      <ChatReferenceCards v-if="message.references?.length" :references="message.references" @open="$emit('open-reference', $event)" />

      <div v-if="message.provider" class="provider muted">{{ providerLabel(message.provider) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RobotOutlined, SearchOutlined } from '@ant-design/icons-vue'
import ChatReferenceCards from './ChatReferenceCards.vue'
import type { ChatMessage } from '@/stores/useAiChatStore'

defineProps<{ message: ChatMessage }>()
defineEmits<{ (e: 'open-reference', ref: any): void }>()

function toolLabel(tool: any) {
  if (tool.tool === 'fallback') return '大模型不可用，已降级为本地回答'
  const names: Record<string, string> = {
    searchWorks: '检索社区作品',
    searchPictures: '检索平台图片',
    findPhotographers: '检索摄影师',
    listExhibitions: '检索专题',
    platformStats: '统计平台数据',
    recommendSimilar: '相似分组',
    getPictureDetail: '读取图片元数据',
  }
  const base = names[tool.tool] || tool.tool
  const hit = typeof tool.hitCount === 'number' ? ' · 命中 ' + tool.hitCount + ' 条' : ''
  const filtered = tool.filteredCount ? '（过滤 ' + tool.filteredCount + ' 条）' : ''
  return base + hit + filtered
}

function providerLabel(provider: string) {
  if (provider === 'SPRING_AI_DASHSCOPE') return '摄影助手 · 站内内容'
  if (provider.startsWith('LOCAL_TEMPLATE')) return '摄影助手 · 站内检索'
  return provider
}
</script>

<style scoped>
.msg { display: flex; gap: 8px; margin-bottom: 14px; }
.msg.user { flex-direction: row-reverse; }

.avatar {
  display: grid; place-items: center; width: 28px; height: 28px; flex: 0 0 auto;
  border-radius: 50%; background: var(--accent-soft); color: #4F5F53; font-size: 13px;
}

.bubble-wrap { display: flex; flex-direction: column; gap: 6px; max-width: 82%; }
.msg.user .bubble-wrap { align-items: flex-end; }

.bubble {
  padding: 10px 13px; border-radius: 14px; background: var(--surface);
  border: 1px solid var(--border); font-size: var(--font-2); line-height: 1.7;
  white-space: pre-wrap; word-break: break-word;
}

.msg.user .bubble { background: var(--accent); border-color: var(--accent); color: var(--accent-contrast); }
.bubble.is-error { background: var(--danger-soft); border-color: var(--danger); color: #8C4F45; }

.cursor { animation: blink 1s steps(2, start) infinite; color: var(--accent); }
@keyframes blink { to { visibility: hidden; } }

.tool-trace { display: flex; flex-wrap: wrap; gap: 6px; }
.tool-chip {
  display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px;
  border-radius: var(--radius-pill); background: var(--surface-3); color: var(--text-2); font-size: 12px;
}

.provider { font-size: 10px; }
</style>
