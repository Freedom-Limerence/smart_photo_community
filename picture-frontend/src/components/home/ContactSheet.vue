<template>
  <section class="contact-sheet" @keydown="onKeydown" tabindex="0" ref="sheetRef">
    <!-- 主作品：占据页面实体空间 -->
    <div class="sheet-stage">
      <transition name="reveal" mode="out-in">
        <img v-if="active" :key="active.id" class="sheet-hero" :src="heroOf(active)" :alt="active.title || ''"
             @click="emit('open', active)" />
      </transition>

      <!-- 悬浮在画面边缘的标题 / 作者 / 收藏 -->
      <div class="sheet-overlay">
        <h1 class="sheet-title">{{ active?.title || '等待第一组作品' }}</h1>
        <p class="sheet-by">{{ active?.author?.userName || '摄影师' }}</p>
      </div>

      <button class="sheet-fav" type="button" :aria-pressed="favorited" @click.stop="toggleFavorite">
        <StarFilled v-if="favorited" /><StarOutlined v-else />
        <span>{{ favorited ? '已收藏' : '收藏' }}</span>
      </button>

      <div class="sheet-counter">
        <b>{{ pad(index + 1) }}</b><i>/</i>{{ pad(works.length) }}
      </div>

      <div class="sheet-nav">
        <button type="button" aria-label="上一张" @click="step(-1)"><LeftOutlined /></button>
        <button type="button" aria-label="下一张" @click="step(1)"><RightOutlined /></button>
      </div>
    </div>

    <!-- 连续影像索引：浏览位置始终可见 -->
    <ol class="sheet-strip">
      <li v-for="(item, i) in works" :key="item.id"
          :class="{ on: i === index, near: Math.abs(i - index) === 1 }">
        <button type="button" :aria-current="i === index" @click="go(i)">
          <img :src="thumbOf(item)" alt="" loading="lazy" />
          <em>{{ pad(i + 1) }}</em>
        </button>
      </li>
    </ol>
  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { LeftOutlined, RightOutlined, StarFilled, StarOutlined } from '@ant-design/icons-vue'
import { favoriteWork, unfavoriteWork } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'

const props = defineProps<{ works: any[] }>()
const emit = defineEmits<{ open: [work: any] }>()

const loginUser = useLoginUserStore()
const index = ref(0)
const favorited = ref(false)
const sheetRef = ref<HTMLElement | null>(null)

const active = computed(() => props.works[index.value] || null)

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
  if (e.key === 'ArrowLeft') step(-1)
  if (e.key === 'ArrowRight') step(1)
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

let timer: number | null = null
onMounted(() => {
  timer = window.setInterval(() => {
    if (document.hidden) return
    const el = sheetRef.value
    if (el && el.matches(':hover')) return
    step(1)
  }, 7000)
})
onBeforeUnmount(() => { if (timer !== null) window.clearInterval(timer) })
</script>

<style scoped>
.contact-sheet { background: #F5F6F4; padding: 0 0 4px; outline: none; }
.sheet-stage { position: relative; overflow: hidden; background: #202621; }
.sheet-hero { display: block; width: 100%; height: min(78vh, 760px); object-fit: cover; cursor: pointer; }
.sheet-overlay { position: absolute; left: clamp(20px, 4vw, 64px); bottom: clamp(18px, 3vw, 44px); right: clamp(20px, 4vw, 64px); pointer-events: none; }
.sheet-title { margin: 0 0 6px; font-size: clamp(24px, 3.4vw, 52px); line-height: 1.06; letter-spacing: -0.02em; color: #FFFFFF; text-shadow: 0 2px 24px rgba(0, 0, 0, .45); max-width: 22ch; }
.sheet-by { margin: 0; color: rgba(255, 255, 255, .82); font-size: 13px; letter-spacing: .04em; }
.sheet-fav { position: absolute; top: 20px; right: 20px; display: inline-flex; align-items: center; gap: 6px; padding: 8px 14px; border: 1px solid rgba(255, 255, 255, .28); border-radius: 999px; background: rgba(32, 38, 33, .42); backdrop-filter: blur(10px); color: #fff; font-size: 13px; cursor: pointer; transition: background .22s cubic-bezier(.16, 1, .3, 1), border-color .22s; }
.sheet-fav:hover { background: rgba(32, 38, 33, .68); border-color: #7C8B7F; }
.sheet-counter { position: absolute; bottom: 22px; right: clamp(20px, 4vw, 64px); display: inline-flex; align-items: baseline; gap: 4px; color: rgba(255, 255, 255, .74); font-size: 13px; font-variant-numeric: tabular-nums; }
.sheet-counter b { color: #fff; font-size: 20px; font-weight: 500; }
.sheet-counter i { font-style: normal; opacity: .5; margin: 0 2px; }
.sheet-nav { position: absolute; right: clamp(20px, 4vw, 64px); bottom: 48px; display: flex; gap: 8px; }
.sheet-nav button { width: 38px; height: 38px; display: grid; place-items: center; border: 1px solid rgba(255, 255, 255, .26); border-radius: 50%; background: rgba(32, 38, 33, .38); color: #fff; cursor: pointer; transition: background .22s cubic-bezier(.16, 1, .3, 1), transform .22s; }
.sheet-nav button:hover { background: rgba(124, 139, 127, .85); transform: translateY(-1px); }

.sheet-strip { display: flex; gap: 2px; margin: 2px 0 0; padding: 0; list-style: none; overflow-x: auto; background: #F5F6F4; scrollbar-width: thin; }
.sheet-strip li { flex: 0 0 auto; }
.sheet-strip button { position: relative; display: block; padding: 0; border: 0; background: none; cursor: pointer; opacity: .38; transition: opacity .3s cubic-bezier(.16, 1, .3, 1); }
.sheet-strip li.near button { opacity: .62; }
.sheet-strip li.on button { opacity: 1; }
.sheet-strip img { display: block; width: clamp(84px, 11vw, 148px); height: clamp(56px, 7vw, 96px); object-fit: cover; }
.sheet-strip em { position: absolute; left: 6px; bottom: 4px; font-style: normal; font-size: 10px; color: #fff; letter-spacing: .06em; text-shadow: 0 1px 6px rgba(0, 0, 0, .6); }
.sheet-strip li.on em { color: #E9EFE9; }

.reveal-enter-active { transition: clip-path .72s cubic-bezier(.16, 1, .3, 1), filter .72s cubic-bezier(.16, 1, .3, 1); }
.reveal-leave-active { transition: opacity .28s ease; position: absolute; inset: 0; }
.reveal-enter-from { clip-path: inset(0 0 100% 0); filter: blur(14px); }
.reveal-leave-to { opacity: 0; }

.contact-sheet:focus-visible { outline: 2px solid #7C8B7F; outline-offset: 2px; }
.contact-sheet ::selection { background: #7C8B7F; color: #fff; }

@media (max-width: 768px) {
  .sheet-hero { height: 62vh; }
  .sheet-title { font-size: 24px; }
  .sheet-nav { display: none; }
}
</style>
