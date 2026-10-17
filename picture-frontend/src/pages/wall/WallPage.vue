<template>
  <!-- 沉浸式主题闭合：深色大屏里的 antd 组件跟随暗色算法（P0-8） -->
  <a-config-provider :theme="immersiveTheme">
  <div class="wall" data-theme="immersive">
    <div class="wall-stage">
      <transition name="fade" mode="out-in">
        <!-- Ken Burns：每次切换重新挂载，动画随 key 重启；a/b 交替方向 -->
        <img v-if="current" :key="current.id" :src="coverOf(current)" :alt="current.title"
             class="wall-img" :class="index % 2 ? 'kb-b' : 'kb-a'" />
      </transition>
      <div class="wall-mask" />

      <header class="wall-top">
        <div class="brand"><span class="mark">光</span><strong>光影集 · 今日影像</strong></div>
        <div class="wall-meta">
          <span>{{ today }}</span>
          <span class="dot">·</span>
          <span>{{ current ? (current.author?.userName || '佚名') : '' }}</span>
        </div>
        <a-space>
          <span class="wall-counter" aria-live="polite">{{ index + 1 }} / {{ list.length }}<i>·</i>已播 {{ laps }} 轮</span>
          <a-button type="text" class="wall-btn" @click="togglePlay">
            <PauseOutlined v-if="playing" /><PlayCircleOutlined v-else /> {{ playing ? '暂停' : '播放' }}
          </a-button>
          <a-button type="text" class="wall-btn" @click="toggleFullscreen"><FullscreenOutlined /> 全屏</a-button>
          <a-button type="text" class="wall-btn" @click="router.push('/works/' + current?.id)">查看作品 →</a-button>
        </a-space>
      </header>

      <div v-if="current" class="wall-caption">
        <p class="eyebrow">TODAY'S PICK</p>
        <h1>{{ current.title || '未命名作品' }}</h1>
        <p v-if="current.description" class="desc">{{ current.description }}</p>
        <div class="stats">
          <span><LikeOutlined /> {{ current.likeCount ?? 0 }}</span>
          <span><StarOutlined /> {{ current.favoriteCount ?? 0 }}</span>
          <span><EyeOutlined /> {{ current.viewCount ?? 0 }}</span>
        </div>
      </div>

      <button class="wall-nav prev" type="button" aria-label="上一张" @click="step(-1)"><LeftOutlined /></button>
      <button class="wall-nav next" type="button" aria-label="下一张" @click="step(1)"><RightOutlined /></button>

      <!-- 迷你进度环组：当前张的环随播放填充，任意环可点击跳转 -->
      <div class="wall-rings" role="group" aria-label="影像导航">
        <button v-for="(item, i) in list" :key="item.id" type="button"
                class="ring-btn" :class="{ active: i === index }"
                :aria-label="'跳到第 ' + (i + 1) + ' 张'" :aria-current="i === index ? 'true' : undefined"
                @click="go(i)">
          <svg viewBox="0 0 20 20" width="22" height="22" aria-hidden="true">
            <circle class="track" cx="10" cy="10" r="8" />
            <circle v-if="i === index" class="fill" cx="10" cy="10" r="8"
                    :stroke-dasharray="RING_C" :stroke-dashoffset="RING_C * (1 - progress / 100)" />
            <circle class="core" cx="10" cy="10" r="2.2" />
          </svg>
        </button>
      </div>
    </div>
  </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { theme as antdTheme } from 'ant-design-vue'

/** 沉浸式暗色主题：与 data-theme="immersive" 的 CSS 变量同源（P0-8） */
const immersiveTheme = {
  algorithm: antdTheme.darkAlgorithm,
  token: { colorPrimary: '#8FA394', colorBgBase: '#0B0B0C', borderRadius: 10, fontSize: 14 },
}
import {
  EyeOutlined, FullscreenOutlined, LeftOutlined, LikeOutlined, PauseOutlined,
  PlayCircleOutlined, RightOutlined, StarOutlined,
} from '@ant-design/icons-vue'
import { listWorks } from '@/api/photoCommunity'

const router = useRouter()
const list = ref<any[]>([])
const index = ref(0)
const playing = ref(true)
const progress = ref(0)
const laps = ref(0)
/** P2-4：12s 一张，Ken Burns 与进度环共用此时长 */
const DURATION = 12000
const RING_C = 2 * Math.PI * 8
const reducedMotion =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
    : false
let raf = 0
let lastTick = 0

const current = computed(() => list.value[index.value])
const today = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

function coverOf(work: any) {
  const pic = work?.pictures?.[0]
  return work?.coverUrl || pic?.url || pic?.thumbnailUrl || ''
}

/** 预加载后 2 张，长时间循环不掉链 */
function preloadAround() {
  for (const d of [1, 2]) {
    const w = list.value[(index.value + d) % list.value.length]
    const src = w && coverOf(w)
    if (src) {
      const img = new Image()
      img.src = src
    }
  }
}

function step(delta: number) {
  if (!list.value.length) return
  const next = index.value + delta
  if (delta > 0 && next >= list.value.length) laps.value += 1
  index.value = (next + list.value.length) % list.value.length
  progress.value = 0
}

function go(i: number) {
  index.value = i
  progress.value = 0
}

function togglePlay() {
  playing.value = !playing.value
}

function toggleFullscreen() {
  if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {})
  else document.querySelector('.wall')?.requestFullscreen?.().catch(() => {})
}

/** rAF 驱动进度：暂停时冻结，到 100 自动切下一张 */
function loop(now: number) {
  const dt = now - lastTick
  lastTick = now
  if (playing.value && list.value.length > 1) {
    progress.value += (dt / DURATION) * 100
    if (progress.value >= 100) {
      progress.value = 0
      step(1)
    }
  }
  raf = requestAnimationFrame(loop)
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'ArrowLeft') step(-1)
  if (e.key === 'ArrowRight') step(1)
  if (e.key === ' ') { e.preventDefault(); togglePlay() }
}

onMounted(async () => {
  const res: any = await listWorks({ current: 1, size: 12, sort: 'hot' })
  const records = res.data.code === 0 ? (res.data.data?.records || []) : []
  list.value = records.filter((w: any) => coverOf(w))
  // 每日轮换：按日期偏移起始位置，让每天的“今日精选”不同
  const dayOfYear = Math.floor((Date.now() - new Date(new Date().getFullYear(), 0, 0).getTime()) / 86400000)
  index.value = list.value.length ? dayOfYear % list.value.length : 0
  preloadAround()
  lastTick = performance.now()
  raf = requestAnimationFrame(loop)
  window.addEventListener('keydown', onKey)
})

watch(index, preloadAround)

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  window.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.wall { position: relative; min-height: calc(100vh - var(--header-h)); background: #08080a; color: var(--text-2); overflow: hidden; }
.wall-stage { position: relative; min-height: calc(100vh - var(--header-h)); }
.wall-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
/* Ken Burns：12s 极缓推近 + 2% 漂移，a/b 交替方向（P2-4） */
.wall-img.kb-a { animation: kb-a 12s linear forwards; }
.wall-img.kb-b { animation: kb-b 12s linear forwards; }
@keyframes kb-a { from { transform: scale(1) translate(0, 0); } to { transform: scale(1.06) translate(0.8%, -0.8%); } }
@keyframes kb-b { from { transform: scale(1.06) translate(-0.8%, 0.8%); } to { transform: scale(1) translate(0, 0); } }
.wall-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(8,8,10,.72) 0%, rgba(8,8,10,.18) 38%, rgba(8,8,10,.86) 100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 900ms ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.wall-top { position: relative; z-index: 2; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 22px 34px; }
.brand { display: inline-flex; align-items: center; gap: 10px; }
.mark { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; background: var(--accent); color: #fff; font-weight: 600; }
.brand strong { font-size: 15px; letter-spacing: .04em; }
.wall-meta { display: inline-flex; gap: 8px; color: rgba(242,242,239,.66); font-size: 13px; }
.wall-meta .dot { opacity: .5; }
.wall-counter { color: rgba(242,242,239,.86); font-size: 13px; font-variant-numeric: tabular-nums; }
.wall-counter i { font-style: normal; margin: 0 8px; opacity: .4; }
.wall-btn { color: rgba(242,242,239,.86); }
.wall-btn:hover { color: #fff; background: rgba(255,255,255,.08); }

.wall-caption { position: absolute; left: 34px; bottom: 96px; z-index: 2; max-width: 620px; }
.wall-caption .eyebrow { margin: 0 0 8px; font-size: 12px; letter-spacing: .22em; color: rgba(242,242,239,.6); }
.wall-caption h1 { margin: 0 0 10px; font-size: clamp(28px, 4vw, 46px); font-weight: 500; line-height: 1.15; }
.wall-caption .desc { margin: 0 0 14px; color: rgba(242,242,239,.78); line-height: 1.8; }
.wall-caption .stats { display: flex; gap: 18px; color: rgba(242,242,239,.72); font-size: 13px; }
.wall-caption .stats span { display: inline-flex; align-items: center; gap: 6px; }

.wall-nav { position: absolute; top: 50%; transform: translateY(-50%); z-index: 3; display: grid; place-items: center; width: 46px; height: 46px; border: 1px solid rgba(255,255,255,.18); border-radius: 50%; background: rgba(12,12,14,.5); color: var(--text-2); font-size: 16px; cursor: pointer; transition: background var(--dur-fast) var(--ease); }
.wall-nav:hover { background: rgba(12,12,14,.86); }
.wall-nav.prev { left: 22px; }
.wall-nav.next { right: 22px; }

/* 迷你进度环组：当前环实时填充，其余为暗核；可点击跳转（P2-4） */
.wall-rings { position: absolute; left: 50%; bottom: 24px; transform: translateX(-50%); z-index: 3; display: flex; gap: 10px; }
.ring-btn { padding: 0; border: none; background: none; cursor: pointer; opacity: .45; transition: opacity var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease); }
.ring-btn:hover { opacity: .8; }
.ring-btn.active { opacity: 1; transform: scale(1.12); }
.ring-btn svg { display: block; transform: rotate(-90deg); }
.ring-btn .track { fill: none; stroke: rgba(255,255,255,.22); stroke-width: 1.5; }
.ring-btn .fill { fill: none; stroke: var(--accent); stroke-width: 1.5; stroke-linecap: round; transition: stroke-dashoffset 180ms linear; }
.ring-btn .core { fill: rgba(255,255,255,.4); }
.ring-btn.active .core { fill: var(--text-2); }

@media (prefers-reduced-motion: reduce) {
  .wall-img.kb-a, .wall-img.kb-b { animation: none; }
}
@media (max-width: 768px) {
  .wall-top { padding: 16px; flex-wrap: wrap; }
  .wall-meta { display: none; }
  .wall-caption { left: 18px; right: 18px; bottom: 80px; }
  .wall-nav { display: none; }
}
</style>
