<template>
  <Teleport to="body">
    <Transition name="cmd">
      <div v-if="open" class="cmd-backdrop" @mousedown.self="close">
        <div class="cmd-panel" role="dialog" aria-modal="true" aria-label="命令面板">
          <div class="cmd-input-row">
            <SearchOutlined class="cmd-search-icon" />
            <input
              ref="inputEl"
              v-model="query"
              class="cmd-input"
              type="text"
              role="combobox"
              aria-expanded="true"
              aria-controls="cmd-list"
              aria-activedescendant="cmd-opt-active"
              placeholder="搜索作品 / 摄影师，或输入页名跳转…"
              @keydown="onKeydown"
            />
            <button type="button" class="cmd-esc" aria-label="关闭" @click="close">Esc</button>
          </div>

          <div id="cmd-list" class="cmd-list" role="listbox" aria-label="命令列表">
            <p v-if="!flat.length" class="cmd-empty">
              {{ query ? '没有匹配的结果' : '输入以搜索，或选择下面的快捷动作' }}
            </p>

            <template v-for="group in groups" :key="group.key">
              <p class="cmd-group">{{ group.label }}</p>
              <component
                :is="item.tag"
                v-for="item in group.items"
                :id="item.id"
                :key="item.key"
                class="cmd-item"
                role="option"
                :aria-selected="item.flatIndex === active"
                :class="{ active: item.flatIndex === active }"
                @mousemove="active = item.flatIndex"
                @click="run(item)"
              >
                <img v-if="item.thumb" :src="item.thumb" alt="" class="cmd-thumb" />
                <component :is="item.icon" v-else-if="item.icon" class="cmd-item-icon" />
                <span class="cmd-item-label" v-html="item.html"></span>
                <span v-if="item.sub" class="cmd-item-sub">{{ item.sub }}</span>
              </component>
            </template>
          </div>

          <footer class="cmd-foot">
            <span><b>↑↓</b> 选择</span>
            <span><b>Enter</b> 跳转</span>
            <span><b>Esc</b> 关闭</span>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  AppstoreOutlined, CameraOutlined, CompassOutlined, FileImageOutlined, PictureOutlined,
  SearchOutlined, SettingOutlined, StarOutlined, TeamOutlined, ThunderboltOutlined, UserOutlined,
} from '@ant-design/icons-vue'
import { searchWorksByKeyword } from '@/api/photoCommunity'
import { listUserVoByPageUsingPost } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

/**
 * 命令面板（P2-3）：Ctrl/⌘+K 全局唤起；输入即搜（作品/摄影师/页面），分组呈现；
 * ↑↓ 选择、Enter 跳转、Esc 关闭；空输入展示最近浏览与快捷动作。
 * 顶栏搜索框聚焦时也唤起同一面板（一个入口两种用法）。
 */
const router = useRouter()
const login = useLoginUserStore()
const open = ref(false)
const query = ref('')
const inputEl = ref<HTMLInputElement | null>(null)
const active = ref(0)
const workHits = ref<any[]>([])
const userHits = ref<any[]>([])
let searchTimer = 0
let seq = 0

const RECENTS_KEY = 'photo_recent_works'

const PAGES = [
  { label: '发现 · 首页', path: '/', icon: CompassOutlined },
  { label: '作品社区', path: '/community', icon: AppstoreOutlined },
  { label: '专题展览', path: '/exhibitions', icon: StarOutlined },
  { label: '今日影像 · 大屏', path: '/wall', icon: ThunderboltOutlined },
  { label: '标签广场', path: '/tags', icon: FileImageOutlined },
  { label: '我的图库', path: '/me/works', icon: CameraOutlined },
  { label: '个人设置', path: '/me/settings', icon: SettingOutlined },
]

const ACTIONS = [
  { label: '发布作品', path: '/me/create-work', icon: CameraOutlined },
  { label: '以图搜图', path: '/search_picture', icon: PictureOutlined },
  { label: '语义检索', path: '/search', icon: SearchOutlined },
]

function recents(): { id: string; title: string; cover?: string }[] {
  try { return JSON.parse(localStorage.getItem(RECENTS_KEY) || '[]') } catch { return [] }
}

const isAdmin = computed(() => login.loginUser?.userRole === 'admin')

function esc(s: string) {
  return s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c] as string))
}
function hi(label: string) {
  const q = query.value.trim()
  if (!q) return esc(label)
  const i = label.toLowerCase().indexOf(q.toLowerCase())
  if (i < 0) return esc(label)
  return esc(label.slice(0, i)) + '<mark>' + esc(label.slice(i, i + q.length)) + '</mark>' + esc(label.slice(i + q.length))
}

interface FlatItem {
  id: string; key: string; group: string; label: string; html: string; sub?: string
  icon?: any; thumb?: string; tag: string; run: () => void; flatIndex: number
}

const groups = computed(() => {
  const out: { key: string; label: string; items: any[] }[] = []
  const q = query.value.trim()
  let flat = 0
  const push = (key: string, label: string, items: any[]) => {
    if (!items.length) return
    for (const it of items) it.flatIndex = flat++
    out.push({ key, label, items })
  }

  if (q) {
    push('work', '作品', workHits.value.map((w, i) => ({
      key: `w-${w.id}-${i}`, id: `cmd-opt-${flat}`, tag: 'button', group: 'work',
      label: w.title || '未命名作品', html: hi(w.title || '未命名作品'), sub: w.author?.userName || '',
      thumb: w.coverUrl || w.pictures?.[0]?.thumbnailUrl || w.pictures?.[0]?.url || '',
      run: () => router.push('/works/' + w.id),
    })))
    push('user', '摄影师', userHits.value.map((u, i) => ({
      key: `u-${u.id}-${i}`, id: `cmd-opt-${flat}`, tag: 'button', group: 'user',
      label: u.userName || '摄影师', html: hi(u.userName || ''), sub: u.userProfile || '',
      icon: UserOutlined,
      run: () => router.push('/user/' + u.id),
    })))
  } else if (recents().length) {
    push('recent', '最近浏览', recents().slice(0, 6).map((w, i) => ({
      key: `r-${w.id}-${i}`, id: `cmd-opt-${flat}`, tag: 'button', group: 'recent',
      label: w.title || '未命名作品', html: hi(w.title || '未命名作品'),
      thumb: w.cover || '', run: () => router.push('/works/' + w.id),
    })))
  }

  push('page', '页面', PAGES.filter((p) => !q || p.label.toLowerCase().includes(q.toLowerCase())).map((p, i) => ({
    key: `p-${i}`, id: `cmd-opt-${flat}`, tag: 'button', group: 'page',
    label: p.label, html: hi(p.label), icon: p.icon, run: () => router.push(p.path),
  })))

  const actions = [...ACTIONS, ...(isAdmin.value ? [{ label: '管理后台', path: '/admin', icon: TeamOutlined }] : [])]
  push('action', '操作', actions.filter((a) => !q || a.label.toLowerCase().includes(q.toLowerCase())).map((a, i) => ({
    key: `a-${i}`, id: `cmd-opt-${flat}`, tag: 'button', group: 'action',
    label: a.label, html: hi(a.label), icon: a.icon, run: () => router.push(a.path),
  })))

  return out
})

const flat = computed(() => groups.value.flatMap((g) => g.items))

watch(flat, (items) => {
  if (active.value >= items.length) active.value = Math.max(0, items.length - 1)
})
watch(query, () => {
  active.value = 0
  scheduleSearch()
})

function scheduleSearch() {
  window.clearTimeout(searchTimer)
  const q = query.value.trim()
  if (!q) {
    workHits.value = []
    userHits.value = []
    return
  }
  searchTimer = window.setTimeout(async () => {
    const ticket = ++seq
    try {
      const res: any = await searchWorksByKeyword({ keyword: q, current: 1, size: 6 })
      if (ticket === seq) workHits.value = res.data.code === 0 ? (res.data.data?.records || []) : []
    } catch { if (ticket === seq) workHits.value = [] }
    try {
      // 用户列表接口需要登录态，匿名调用会触发 401 重定向——未登录只搜作品
      if (login.loginUser?.id) {
        const res2: any = await listUserVoByPageUsingPost({ userName: q, current: 1, pageSize: 4 } as any)
        if (ticket === seq) userHits.value = res2.data.code === 0 ? (res2.data.data?.records || []) : []
      }
    } catch { if (ticket === seq) userHits.value = [] }
  }, 250)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'ArrowDown') { e.preventDefault(); active.value = Math.min(flat.value.length - 1, active.value + 1); scrollActive() }
  else if (e.key === 'ArrowUp') { e.preventDefault(); active.value = Math.max(0, active.value - 1); scrollActive() }
  else if (e.key === 'Enter') { e.preventDefault(); const item = flat.value[active.value]; if (item) run(item) }
  else if (e.key === 'Escape') { e.preventDefault(); close() }
}

function scrollActive() {
  void nextTick(() => {
    document.getElementById(`cmd-opt-${flat.value[active.value]?.id}`)?.scrollIntoView({ block: 'nearest' })
  })
}

function run(item: FlatItem) {
  close()
  void item.run()
}

function openPalette() {
  open.value = true
  active.value = 0
  void nextTick(() => inputEl.value?.focus())
}
function close() {
  open.value = false
  query.value = ''
}

function onGlobalKey(e: KeyboardEvent) {
  if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
    e.preventDefault()
    open.value ? close() : openPalette()
  }
}
function onOpenEvent() { openPalette() }

onMounted(() => {
  window.addEventListener('keydown', onGlobalKey)
  window.addEventListener('photo:palette-open', onOpenEvent)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKey)
  window.removeEventListener('photo:palette-open', onOpenEvent)
})

defineExpose({ openPalette })
</script>

<style scoped>
.cmd-backdrop {
  position: fixed;
  inset: 0;
  z-index: 70;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 18vh 16px 16px;
  background: rgba(10, 10, 12, .44);
  backdrop-filter: blur(3px);
}
.cmd-panel {
  width: min(640px, 100%);
  max-height: min(560px, 72vh);
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-3);
  overflow: hidden;
}
.cmd-input-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border);
}
.cmd-search-icon { color: var(--text-3); font-size: var(--icon-md); }
.cmd-input {
  flex: 1;
  border: none;
  outline: none;
  background: none;
  color: var(--text-1);
  font: inherit;
  font-size: var(--font-4);
}
.cmd-input::placeholder { color: var(--text-3); }
.cmd-esc {
  padding: 2px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-2);
  color: var(--text-3);
  font-size: var(--font-1);
  cursor: pointer;
}
.cmd-list { flex: 1 1 auto; overflow-y: auto; padding: 8px 8px 12px; }
.cmd-group {
  margin: 10px 8px 4px;
  color: var(--text-3);
  font-size: var(--font-1);
  letter-spacing: .12em;
}
.cmd-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 10px;
  border: none;
  border-radius: var(--radius);
  background: none;
  color: var(--text-1);
  font: inherit;
  font-size: var(--font-3);
  text-align: left;
  cursor: pointer;
}
.cmd-item.active { background: var(--accent-soft); }
.cmd-item-icon { color: var(--text-3); font-size: var(--icon-md); flex: 0 0 auto; }
.cmd-item.active .cmd-item-icon { color: var(--accent-ink); }
.cmd-thumb {
  width: 34px;
  height: 26px;
  flex: 0 0 auto;
  object-fit: cover;
  border-radius: var(--radius-xs);
  background: var(--surface-2);
}
.cmd-item-label { flex: 1 1 auto; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cmd-item-label :deep(mark) { background: none; color: var(--accent-ink); font-weight: 600; }
.cmd-item-sub {
  flex: 0 0 auto;
  max-width: 40%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-3);
  font-size: var(--font-2);
}
.cmd-empty { padding: 28px 0; text-align: center; color: var(--text-3); font-size: var(--font-3); }
.cmd-foot {
  display: flex;
  gap: 16px;
  padding: 8px 16px;
  border-top: 1px solid var(--border);
  color: var(--text-3);
  font-size: var(--font-1);
}
.cmd-foot b { font-weight: 500; color: var(--text-2); }

.cmd-enter-active, .cmd-leave-active { transition: opacity var(--dur-fast) var(--ease); }
.cmd-enter-active .cmd-panel, .cmd-leave-active .cmd-panel { transition: transform var(--dur) var(--ease), opacity var(--dur) var(--ease); }
.cmd-enter-from, .cmd-leave-to { opacity: 0; }
.cmd-enter-from .cmd-panel, .cmd-leave-to .cmd-panel { transform: translateY(-8px) scale(.985); opacity: 0; }
@media (prefers-reduced-motion: reduce) {
  .cmd-enter-active, .cmd-leave-active, .cmd-enter-active .cmd-panel, .cmd-leave-active .cmd-panel { transition: none; }
}
</style>
