<template>
  <!-- 沉浸式主题闭合：深色大屏里的 antd 组件跟随暗色算法（P0-8） -->
  <a-config-provider :theme="immersiveTheme">
  <div class="wall" data-theme="immersive">
    <div class="wall-stage">
      <transition name="fade" mode="out-in">
        <img v-if="current" :key="current.id" :src="coverOf(current)" :alt="current.title" class="wall-img" />
      </transition>
      <div class="wall-mask" />

      <header class="wall-top">
        <div class="brand"><span class="mark">光</span><strong>光影集 · 今日影像</strong></div>
        <div class="wall-meta">
          <span>{{ today }}</span>
          <span class="dot">·</span>
          <span>今日精选 {{ list.length }} 组</span>
          <span class="dot">·</span>
          <span>{{ current ? (current.author?.userName || '佚名') : '' }}</span>
        </div>
        <a-space>
          <a-button type="text" class="wall-btn" @click="togglePlay">
            <PauseOutlined v-if="playing" /><PlayCircleOutlined v-else /> {{ playing ? '暂停' : '播放' }}
          </a-button>
          <a-button type="text" class="wall-btn" @click="toggleFullscreen"><FullscreenOutlined /> 全屏</a-button>
          <a-button type="text" class="wall-btn" @click="router.push('/works/' + current?.id)">查看作品 →</a-button>
        </a-space>
      </header>

      <div v-if="current" class="wall-caption">
        <p class="eyebrow">TODAY'S PICK · {{ index + 1 }} / {{ list.length }}</p>
        <h1>{{ current.title || '未命名作品' }}</h1>
        <p v-if="current.description" class="desc">{{ current.description }}</p>
        <div class="stats">
          <span><LikeOutlined /> {{ current.likeCount ?? 0 }}</span>
          <span><StarOutlined /> {{ current.favoriteCount ?? 0 }}</span>
          <span><EyeOutlined /> {{ current.viewCount ?? 0 }}</span>
        </div>
      </div>

      <button class="wall-nav prev" type="button" @click="step(-1)"><LeftOutlined /></button>
      <button class="wall-nav next" type="button" @click="step(1)"><RightOutlined /></button>

      <div class="wall-progress"><i :style="{ width: progress + '%' }" /></div>
      <div class="wall-dots">
        <button v-for="(item, i) in list" :key="item.id" type="button" :class="{ active: i === index }" @click="go(i)" />
      </div>
    </div>
  </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
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
const DURATION = 7000
let timer: number | null = null
let tick: number | null = null

const current = computed(() => list.value[index.value])
const today = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }))

function coverOf(work: any) {
  const pic = work?.pictures?.[0]
  return work?.coverUrl || pic?.url || pic?.thumbnailUrl || ''
}

function step(delta: number) {
  if (!list.value.length) return
  index.value = (index.value + delta + list.value.length) % list.value.length
  progress.value = 0
}

function go(i: number) {
  index.value = i
  progress.value = 0
}

function togglePlay() {
  playing.value = !playing.value
  if (playing.value) startTimer()
  else stopTimer()
}

function toggleFullscreen() {
  if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {})
  else document.querySelector('.wall')?.requestFullscreen?.().catch(() => {})
}

function startTimer() {
  stopTimer()
  timer = window.setInterval(() => step(1), DURATION)
  tick = window.setInterval(() => {
    if (playing.value) progress.value = Math.min(100, progress.value + (100 / (DURATION / 200)))
  }, 200)
}

function stopTimer() {
  if (timer !== null) window.clearInterval(timer)
  if (tick !== null) window.clearInterval(tick)
  timer = null
  tick = null
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
  startTimer()
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => {
  stopTimer()
  window.removeEventListener('keydown', onKey)
})
</script>

<style scoped>
.wall { position: relative; min-height: calc(100vh - var(--header-h)); background: #08080a; color: #F2F2EF; overflow: hidden; }
.wall-stage { position: relative; min-height: calc(100vh - var(--header-h)); }
.wall-img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.wall-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(8,8,10,.72) 0%, rgba(8,8,10,.18) 38%, rgba(8,8,10,.86) 100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 900ms ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.wall-top { position: relative; z-index: 2; display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 22px 34px; }
.brand { display: inline-flex; align-items: center; gap: 10px; }
.mark { display: grid; place-items: center; width: 30px; height: 30px; border-radius: 8px; background: var(--accent); color: #fff; font-weight: 600; }
.brand strong { font-size: 15px; letter-spacing: .04em; }
.wall-meta { display: inline-flex; gap: 8px; color: rgba(242,242,239,.66); font-size: 13px; }
.wall-meta .dot { opacity: .5; }
.wall-btn { color: rgba(242,242,239,.86); }
.wall-btn:hover { color: #fff; background: rgba(255,255,255,.08); }

.wall-caption { position: absolute; left: 34px; bottom: 86px; z-index: 2; max-width: 620px; }
.wall-caption .eyebrow { margin: 0 0 8px; font-size: 12px; letter-spacing: .22em; color: rgba(242,242,239,.6); }
.wall-caption h1 { margin: 0 0 10px; font-size: clamp(28px, 4vw, 46px); font-weight: 500; line-height: 1.15; }
.wall-caption .desc { margin: 0 0 14px; color: rgba(242,242,239,.78); line-height: 1.8; }
.wall-caption .stats { display: flex; gap: 18px; color: rgba(242,242,239,.72); font-size: 13px; }
.wall-caption .stats span { display: inline-flex; align-items: center; gap: 6px; }

.wall-nav { position: absolute; top: 50%; transform: translateY(-50%); z-index: 3; display: grid; place-items: center; width: 46px; height: 46px; border: 1px solid rgba(255,255,255,.18); border-radius: 50%; background: rgba(12,12,14,.5); color: #F2F2EF; font-size: 16px; cursor: pointer; transition: background var(--dur-fast) var(--ease); }
.wall-nav:hover { background: rgba(12,12,14,.86); }
.wall-nav.prev { left: 22px; }
.wall-nav.next { right: 22px; }

.wall-progress { position: absolute; left: 0; right: 0; bottom: 0; height: 2px; background: rgba(255,255,255,.14); z-index: 3; }
.wall-progress i { display: block; height: 100%; background: var(--accent); }
.wall-dots { position: absolute; left: 50%; bottom: 26px; transform: translateX(-50%); z-index: 3; display: flex; gap: 8px; }
.wall-dots button { width: 8px; height: 8px; padding: 0; border: none; border-radius: 50%; background: rgba(255,255,255,.28); cursor: pointer; transition: all var(--dur-fast) var(--ease); }
.wall-dots button.active { background: #F2F2EF; transform: scale(1.35); }

@media (max-width: 768px) {
  .wall-top { padding: 16px; flex-wrap: wrap; }
  .wall-meta { display: none; }
  .wall-caption { left: 18px; right: 18px; bottom: 70px; }
  .wall-nav { display: none; }
}
</style>
