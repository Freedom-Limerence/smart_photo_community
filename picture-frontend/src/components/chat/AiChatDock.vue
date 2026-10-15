<template>
  <div class="ai-chat-dock">
    <!-- 展开态：右侧抽屉 -->
    <transition name="chat-panel">
      <section v-if="store.open" class="chat-panel" :class="{ fullscreen: isNarrow }">
        <header class="chat-header">
          <div class="chat-title">
            <span class="chat-mark">AI</span>
            <div>
              <strong>摄影助手</strong>
              <em>{{ providerLabel }}</em>
            </div>
          </div>
          <div class="chat-header-actions">
            <a-tooltip title="新会话"><a-button type="text" size="small" @click="store.newSession()"><PlusOutlined /></a-button></a-tooltip>
            <a-tooltip title="历史会话"><a-button type="text" size="small" @click="historyOpen = true"><HistoryOutlined /></a-button></a-tooltip>
            <a-tooltip title="收起"><a-button type="text" size="small" @click="store.toggle(false)"><DownOutlined /></a-button></a-tooltip>
          </div>
        </header>

        <div ref="listRef" class="chat-body">
          <div v-if="!store.hasMessages" class="chat-welcome">
            <h3>你好，我是光影集的摄影助手</h3>
            <p>我可以基于平台里真实的图片、摄影师和专题回答你，不会编造内容。</p>
            <div class="quick-list">
              <button v-for="q in quickQuestions" :key="q" class="quick-item" @click="ask(q)">{{ q }}</button>
            </div>
          </div>

          <ChatMessageItem v-for="msg in store.messages" :key="msg.id" :message="msg" @open-reference="onOpenReference" />

          <div ref="anchorRef" class="chat-anchor" />
        </div>

        <footer class="chat-composer">
          <a-textarea v-model:value="draft" :auto-size="{ minRows: 1, maxRows: 4 }" placeholder="问点什么…（Enter 发送 / Shift+Enter 换行）"
                      @press-enter="onEnter" />
          <div class="composer-actions">
            <span class="composer-hint">{{ store.sending ? '正在检索平台数据…' : '基于平台真实数据回答' }}</span>
            <a-button v-if="store.sending" size="small" @click="store.stop()">停止</a-button>
            <a-button v-else type="primary" size="small" :disabled="!draft.trim()" @click="ask()">发送</a-button>
          </div>
        </footer>
      </section>
    </transition>

    <!-- 收起态：悬浮按钮 -->
    <button v-if="!store.open" class="chat-launcher" type="button" @click="store.toggle(true)">
      <RobotOutlined />
      <span>问问摄影助手</span>
    </button>

    <!-- 历史会话 -->
    <a-drawer v-model:open="historyOpen" title="历史会话" placement="right" :width="320">
      <a-spin :spinning="loadingSessions">
        <a-list :data-source="store.sessions" size="small">
          <template #renderItem="{ item }">
            <a-list-item class="session-item" @click="openSession(String(item.id))">
              <div class="session-body">
                <strong class="ellipsis">{{ item.title || '未命名会话' }}</strong>
                <span class="muted">{{ formatTime(item.lastMessageAt) }} · {{ item.messageCount || 0 }} 条</span>
              </div>
              <a-button type="text" size="small" danger @click.stop="store.removeSession(String(item.id))">
                <DeleteOutlined />
              </a-button>
            </a-list-item>
          </template>
        </a-list>
        <a-empty v-if="!store.sessions.length" description="还没有历史会话" />
      </a-spin>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { DeleteOutlined, DownOutlined, HistoryOutlined, PlusOutlined, RobotOutlined } from '@ant-design/icons-vue'
import { useAiChatStore } from '@/stores/useAiChatStore'
import ChatMessageItem from './ChatMessageItem.vue'

const router = useRouter()
const store = useAiChatStore()
const draft = ref('')
const historyOpen = ref(false)
const loadingSessions = ref(false)
const listRef = ref<HTMLElement>()
const anchorRef = ref<HTMLElement>()
const windowWidth = ref(window.innerWidth)

const isNarrow = computed(() => windowWidth.value < 720)
const providerLabel = computed(() => {
  if (store.provider === 'SPRING_AI_DASHSCOPE') return '大模型驱动 · 基于平台检索'
  return '本地检索式回答 · 未配置大模型密钥'
})

const quickQuestions = [
  '帮我找几张适合做壁纸的深色系风景图',
  '平台上有哪些擅长拍人像的摄影师？',
  '最近有什么专题展览？',
  '平台现在有多少张图片？',
]

window.addEventListener('resize', () => { windowWidth.value = window.innerWidth })

function onEnter(e: KeyboardEvent) {
  if (e.shiftKey) return
  e.preventDefault()
  void ask()
}

async function ask(preset?: string) {
  const text = (preset ?? draft.value).trim()
  if (!text) return
  draft.value = ''
  await store.send(text)
}

async function openSession(id: string) {
  historyOpen.value = false
  await store.loadMessages(id)
}

function onOpenReference(ref: any) {
  if (ref?.url) {
    router.push(ref.url)
    if (isNarrow.value) store.toggle(false)
  }
}

function formatTime(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

watch(() => store.open, async (value) => {
  if (!value) return
  loadingSessions.value = true
  try {
    await store.loadSessions()
  } finally {
    loadingSessions.value = false
  }
})

watch(() => store.messages.map((m) => m.content.length).join(','), async () => {
  await nextTick()
  anchorRef.value?.scrollIntoView({ behavior: 'smooth', block: 'end' })
})
</script>

<style scoped>
.ai-chat-dock { position: fixed; right: 24px; bottom: 24px; z-index: 900; }

.chat-launcher {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--text-1);
  font-size: var(--font-3);
  box-shadow: var(--shadow-2);
  cursor: pointer;
  transition: transform var(--dur) var(--ease), box-shadow var(--dur) var(--ease);
}

.chat-launcher:hover { transform: translateY(-2px); box-shadow: var(--shadow-3); }

.chat-panel {
  display: flex;
  flex-direction: column;
  width: 400px;
  height: min(640px, calc(100vh - 120px));
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-3);
  overflow: hidden;
}

.chat-panel.fullscreen { position: fixed; inset: 0; width: 100%; height: 100%; border-radius: 0; }

.chat-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 14px; border-bottom: 1px solid var(--border); background: var(--surface);
}

.chat-title { display: flex; align-items: center; gap: 10px; }
.chat-mark {
  display: grid; place-items: center; width: 30px; height: 30px; border-radius: 8px;
  background: var(--accent-soft); color: #4F5F53; font-size: 12px; font-weight: 600;
}
.chat-title strong { display: block; font-size: var(--font-3); }
.chat-title em { color: var(--text-3); font-size: 11px; font-style: normal; }

.chat-body { flex: 1 1 auto; overflow-y: auto; padding: 16px 14px; background: var(--surface-2); }
.chat-anchor { height: 1px; }

.chat-welcome h3 { margin: 8px 0 6px; font-size: var(--font-4); }
.chat-welcome p { margin: 0 0 14px; color: var(--text-2); font-size: var(--font-2); }
.quick-list { display: flex; flex-direction: column; gap: 8px; }
.quick-item {
  padding: 10px 12px; text-align: left; font-size: var(--font-2);
  background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
  color: var(--text-2); cursor: pointer; transition: all var(--dur-fast) var(--ease);
}
.quick-item:hover { border-color: var(--accent); color: var(--text-1); box-shadow: var(--shadow-1); }

.chat-composer { border-top: 1px solid var(--border); padding: 10px 12px 12px; background: var(--surface); }
.composer-actions { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.composer-hint { color: var(--text-3); font-size: 11px; }

.session-item { display: flex; align-items: center; justify-content: space-between; cursor: pointer; }
.session-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
.session-body span { font-size: 11px; }

.chat-panel-enter-active, .chat-panel-leave-active { transition: opacity var(--dur) var(--ease), transform var(--dur) var(--ease); }
.chat-panel-enter-from, .chat-panel-leave-to { opacity: 0; transform: translateY(12px) scale(.98); }
</style>
