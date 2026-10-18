<template>
  <section class="contact-sheet" ref="sheetRef" tabindex="0" role="group"
           aria-roledescription="轮播"
           aria-label="首页作品接触印样：聚焦后可用左右方向键切换，Enter 打开当前作品"
           @keydown="onKeydown">
    <!-- 主作品：占据页面实体空间 -->
    <div class="sheet-stage" :class="{ 'is-empty': !active }">
      <transition name="reveal" mode="out-in">
        <img v-if="active" :key="active.id" class="sheet-hero" :src="heroOf(active)" :alt="active.title || ''"
             @click="emit('open', active)" />
        <!-- 加载 / 空态：暗房显影语言，与印样同一人格 -->
        <div v-else class="sheet-develop" aria-hidden="true">
          <span class="develop-line"></span>
          <strong>{{ loading ? '正在显影' : '等待第一组作品' }}</strong>
        </div>
      </transition>

      <!-- 悬浮在画面边缘的标题 / 作者 / 收藏 -->
      <div class="sheet-overlay">
        <h1 class="sheet-title" :key="active?.id || 'empty'" aria-label="active?.title || (loading ? '正在显影' : '等待第一组作品')">
          <span v-for="(ch, i) in titleChars" :key="i" class="kt" :style="{ animationDelay: i * 28 + 'ms' }" aria-hidden="true">{{ ch }}</span>
        </h1>
        <p class="sheet-by">{{ active?.author?.userName || (loading ? '' : '摄影师') }}</p>
      </div>

      <button v-if="active" class="sheet-fav" type="button" :aria-pressed="favorited" @click.stop="toggleFavorite">
        <StarFilled v-if="favorited" /><StarOutlined v-else />
        <span>{{ favorited ? '已收藏' : '收藏' }}</span>
      </button>

      <div v-if="works.length" class="sheet-counter">
        <b>{{ pad(index + 1) }}</b><i>/</i>{{ pad(works.length) }}
      </div>

      <div v-if="works.length > 1" class="sheet-nav">
        <button type="button" aria-label="上一张" @click="step(-1)"><LeftOutlined /></button>
        <button type="button" :aria-label="playing ? '暂停轮播' : '播放轮播'" :aria-pressed="!playing" @click="playing = !playing">
          <PauseOutlined v-if="playing" /><CaretRightOutlined v-else />
        </button>
        <button type="button" aria-label="下一张" @click="step(1)"><RightOutlined /></button>
      </div>
    </div>

    <!-- 连续影像索引：浏览位置始终可见 -->
    <ol v-if="works.length" class="sheet-strip">
      <li v-for="(item, i) in works" :key="item.id"
          :class="{ on: i === index, near: Math.abs(i - index) === 1 }">
        <button type="button" :aria-current="i === index ? 'true' : undefined" @click="go(i)">
          <img :src="thumbOf(item)" alt="" loading="lazy" />
          <em>{{ pad(i + 1) }}</em>
        </button>
      </li>
    </ol>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { CaretRightOutlined, LeftOutlined, PauseOutlined, RightOutlined, StarFilled, StarOutlined } from '@ant-design/icons-vue'
import { favoriteWork, unfavoriteWork } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'

const props = withDefaults(defineProps<{ works: any[]; loading?: boolean }>(), { loading: false })
const emit = defineEmits<{ open: [work: any] }>()

const loginUser = useLoginUserStore()
const index = ref(0)
const favorited = ref(false)
const sheetRef = ref<HTMLElement | null>(null)

const active = computed(() => props.works[index.value] || null)

/** 标题逐字拆分（motionsites 式 kinetic type）：换片即重放 */
const titleChars = computed(() =>
  String(active.value?.title || (props.loading ? '正在显影' : '等待第一组作品')).split(''),
)

function pad(n: number) { return String(n).padStart(2, '0') }
function heroOf(work: any) {
  const pic = work?.pictures?.[0]
  return work?.coverUrl || pic?.url || pic?.thumbnailUrl || ''
}
function thumbOf(work: any) {
  const pic = work?.pictures?.[0]
  return pic?.thumbnailUrl || work?.coverUrl || pic?.url || ''
}

function step(delta: number) {
  const total = props.works.length
  if (total < 2) return
  index.value = (index.value + delta + total) % total
  favorited.value = !!props.works[index.value]?.favorited
}

function go(i: number) {
  index.value = i
  favorited.value = !!props.works[i]?.favorited
}

function onKeydown(e: KeyboardEvent) {
  // 只在 section 自身聚焦时响应：不劫持索引条按钮等子控件的 Enter / 方向键
  if (e.target !== sheetRef.value) return
  if (e.key === 'ArrowLeft') { e.preventDefault(); step(-1) }
  if (e.key === 'ArrowRight') { e.preventDefault(); step(1) }
  if (e.key === 'Enter' && active.value) emit('open', active.value)
}

async function toggleFavorite() {
  if (!loginUser.loginUser?.id) return message.warning('登录后可以收藏作品')
  if (!active.value) return
  const next = !favorited.value
  const res: any = next ? await favoriteWork({ workId: active.value.id }) : await unfavoriteWork({ workId: active.value.id })
  if (res.data.code === 0) {
    favorited.value = next
    message.success(next ? '已收藏' : '已取消收藏')
  } else {
    message.error(res.data.message || '操作失败')
  }
}

/* 数据到位后同步首张作品的收藏态，避免显示与真实状态不符 */
watch(() => props.works.length > 0, (has) => {
  if (has) favorited.value = !!props.works[index.value]?.favorited
}, { immediate: true })

/* 自动轮播：保留，但可暂停；触屏/键盘聚焦时暂停；reduced-motion 默认不播放 */
const playing = ref(true)
let timer: number | null = null

function stopTimer() {
  if (timer !== null) { window.clearInterval(timer); timer = null }
}

function syncTimer() {
  stopTimer()
  if (!playing.value || props.works.length < 2) return
  timer = window.setInterval(() => {
    if (document.hidden) return
    const el = sheetRef.value
    if (!el) return
    if (el.matches(':hover')) return
    if (el.contains(document.activeElement)) return // 键盘用户正在阅读时暂停
    step(1)
  }, 7000)
}

onMounted(() => {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) playing.value = false
  syncTimer()
})
watch([playing, () => props.works.length], syncTimer)
onBeforeUnmount(stopTimer)
</script>

<style scoped>
.contact-sheet { background: var(--surface-print); padding: 0 0 4px; outline: none; }
.sheet-stage { position: relative; overflow: hidden; background: var(--stage); }
.sheet-stage.is-empty { min-height: min(46vh, 420px); }
.sheet-hero { display: block; width: 100%; height: min(78vh, 760px); object-fit: cover; cursor: pointer; animation: hero-drift 8s var(--ease, ease) forwards; }
@keyframes hero-drift { from { transform: scale(1.04); } to { transform: scale(1); } }

/* 显影占位：暗场 + 缓慢扫过的高光，与印样的「显影」动效同语言 */
.sheet-develop { display: grid; place-content: center; justify-items: center; gap: 18px; width: 100%; height: min(78vh, 760px); }
.sheet-stage.is-empty .sheet-develop { height: min(46vh, 420px); }
.sheet-develop strong { color: rgba(255, 255, 255, .5); font-family: var(--font-display); font-size: 15px; letter-spacing: .3em; }
.develop-line { width: clamp(120px, 16vw, 220px); height: 1px; background: linear-gradient(to right, transparent, rgba(233, 239, 233, .55), transparent); animation: develop-sweep 2.4s var(--ease, ease) infinite; }
@keyframes develop-sweep { 0% { transform: translateX(-60%); opacity: .2; } 50% { opacity: 1; } 100% { transform: translateX(60%); opacity: .2; } }

.sheet-overlay { position: absolute; left: clamp(20px, 4vw, 64px); bottom: clamp(18px, 3vw, 44px); right: clamp(20px, 4vw, 64px); pointer-events: none; }
.sheet-title { margin: 0 0 6px; font-family: var(--font-display); font-weight: 400; font-size: clamp(24px, 3.4vw, 52px); line-height: 1.06; letter-spacing: -.02em; color: #FFFFFF; text-shadow: 0 2px 24px rgba(0, 0, 0, .45); max-width: 22ch; }
/* 逐字升起：blur + 上浮 + 微旋转，28ms 级联（reduced-motion 由 .kt 全局兜底） */
.sheet-title .kt { display: inline-block; white-space: pre; animation: kt-in .62s cubic-bezier(.16, 1, .3, 1) both; }
@keyframes kt-in { from { opacity: 0; transform: translateY(16px) rotate(2.5deg); filter: blur(6px); } to { opacity: 1; transform: none; filter: blur(0); } }
.sheet-by { margin: 0; color: rgba(255, 255, 255, .82); font-size: 13px; letter-spacing: .04em; }
.sheet-fav { position: absolute; top: 20px; right: 20px; display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border: 1px solid rgba(255, 255, 255, .28); border-radius: 999px; background: rgba(32, 38, 33, .42); backdrop-filter: blur(10px); color: #fff; font-size: 13px; cursor: pointer; transition: background .22s cubic-bezier(.16, 1, .3, 1), border-color .22s; }
.sheet-fav:hover { background: rgba(32, 38, 33, .68); border-color: var(--accent); }
.sheet-counter { position: absolute; bottom: 22px; right: clamp(20px, 4vw, 64px); display: inline-flex; align-items: baseline; gap: 4px; color: rgba(255, 255, 255, .74); font-size: 13px; font-variant-numeric: tabular-nums; }
.sheet-counter b { color: #fff; font-size: 20px; font-weight: 500; }
.sheet-counter i { font-style: normal; opacity: .5; margin: 0 2px; }
.sheet-nav { position: absolute; right: clamp(20px, 4vw, 64px); bottom: 48px; display: flex; gap: 8px; }
.sheet-nav button { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid rgba(255, 255, 255, .26); border-radius: 50%; background: rgba(32, 38, 33, .38); color: #fff; cursor: pointer; transition: background .22s cubic-bezier(.16, 1, .3, 1), transform .22s; }
.sheet-nav button:hover { background: rgba(124, 139, 127, .85); transform: translateY(-1px); }
.sheet-nav button[aria-pressed='true'] { background: rgba(124, 139, 127, .85); border-color: var(--accent); }

.sheet-strip { display: flex; gap: 2px; margin: 2px 0 0; padding: 0; list-style: none; overflow-x: auto; background: var(--surface-print); scrollbar-width: thin; }
.sheet-strip li { flex: 0 0 auto; }
.sheet-strip button { position: relative; display: block; padding: 0; border: 0; background: none; cursor: pointer; opacity: .38; transition: opacity .3s cubic-bezier(.16, 1, .3, 1); }
.sheet-strip li.near button { opacity: .62; }
.sheet-strip li.on button { opacity: 1; }
.sheet-strip img { display: block; width: clamp(84px, 11vw, 148px); height: clamp(56px, 7vw, 96px); object-fit: cover; }
.sheet-strip em { position: absolute; left: 6px; bottom: 4px; font-style: normal; font-size: 10px; color: #fff; letter-spacing: .06em; text-shadow: 0 1px 6px rgba(0, 0, 0, .6); }
.sheet-strip li.on em { color: var(--accent-soft); }

.reveal-enter-active { transition: clip-path .72s cubic-bezier(.16, 1, .3, 1), filter .72s cubic-bezier(.16, 1, .3, 1); }
.reveal-leave-active { transition: opacity .28s ease; position: absolute; inset: 0; }
.reveal-enter-from { clip-path: inset(0 0 100% 0); filter: blur(14px); }
.reveal-leave-to { opacity: 0; }

.contact-sheet:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
.contact-sheet ::selection { background: var(--accent); color: #fff; }

@media (max-width: 768px) {
  .sheet-hero, .sheet-develop { height: 62vh; }
  .sheet-stage.is-empty .sheet-develop { height: 42vh; }
  .sheet-title { font-size: 24px; }
  .sheet-nav { display: none; }
}

@media (prefers-reduced-motion: reduce) {
  .develop-line { animation: none; }
  .sheet-hero { animation: none; }
  .sheet-title .kt { animation: none; }
}
</style>
