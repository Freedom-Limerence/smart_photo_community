<template>
  <section ref="rootEl" class="scrolly container">
    <div class="scrolly-left">
      <span class="scrolly-no" aria-hidden="true">N°02</span>
      <div class="scrolly-sticky">
        <h2>翻阅<em>这一周</em>的社群</h2>
        <p>慢慢往下滚，作品会像展签一样逐张亮起。</p>
        <div class="scrolly-progress" aria-hidden="true"><i :style="{ height: progress + '%' }" /></div>
        <span class="scrolly-count" aria-live="polite">{{ current + 1 }} / {{ works.length }}</span>
      </div>
    </div>
    <div class="scrolly-right">
      <figure v-for="(w, i) in works" :key="w.id" class="scrolly-item" :class="{ on: i === current }">
        <img :src="coverOf(w)" :alt="w.title || '摄影作品'" loading="lazy" decoding="async" />
        <figcaption>
          <strong>{{ w.title || '未命名作品' }}</strong>
          <span>{{ w.author?.userName || '摄影师' }}</span>
        </figcaption>
      </figure>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 滚动叙事（motionsites 式 scrollytelling）：左侧章节信息吸顶，
 * 右侧作品随滚动逐张切换，进度条与计数实时联动。
 */
const props = defineProps<{ works: any[] }>()
const rootEl = ref<HTMLElement | null>(null)
const current = ref(0)
const progress = ref(0)

function coverOf(w: any) {
  const pic = w?.pictures?.[0]
  return w?.coverUrl || pic?.url || pic?.thumbnailUrl || ''
}

let raf = 0
let ticking = false
function onScroll() {
  if (ticking) return
  ticking = true
  raf = requestAnimationFrame(() => {
    ticking = false
    const root = rootEl.value
    if (!root) return
    const items = Array.from(root.querySelectorAll('.scrolly-item'))
    const mid = window.innerHeight / 2
    let idx = 0
    items.forEach((el, i) => {
      if (el.getBoundingClientRect().top <= mid) idx = i
    })
    current.value = idx
    progress.value = items.length ? Math.round(((idx + 1) / items.length) * 100) : 0
  })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})
onBeforeUnmount(() => {
  window.removeEventListener('scroll', onScroll)
  if (raf) cancelAnimationFrame(raf)
})
</script>

<style scoped>
.scrolly {
  display: grid;
  grid-template-columns: minmax(0, .9fr) minmax(0, 1.1fr);
  gap: clamp(24px, 4vw, 72px);
  padding-top: clamp(56px, 7vw, 120px);
  padding-bottom: var(--space-8);
}
.scrolly-left { min-width: 0; }
.scrolly-no {
  display: inline-block;
  margin-bottom: clamp(16px, 2vw, 32px);
  padding-left: 10px;
  border-left: 1px solid var(--border-strong);
  color: var(--text-3);
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: .22em;
  writing-mode: vertical-rl;
}
.scrolly-sticky { position: sticky; top: calc(var(--header-h) + 40px); }
.scrolly-sticky h2 {
  margin: 0;
  font: 400 clamp(34px, 4.2vw, 64px)/1.12 var(--font-display);
  letter-spacing: -.02em;
  color: var(--text-1);
  text-wrap: balance;
}
.scrolly-sticky h2 em { font-style: normal; color: var(--accent-hover); }
.scrolly-sticky p { margin: var(--space-3) 0 0; color: var(--text-2); max-width: 34ch; }
.scrolly-progress {
  width: 2px; height: 120px;
  margin: var(--space-6) 0 var(--space-2);
  background: var(--border);
  border-radius: 2px;
  overflow: hidden;
}
.scrolly-progress i { display: block; width: 100%; background: var(--accent); transition: height .3s var(--ease); }
.scrolly-count { color: var(--text-3); font-size: var(--font-2); font-variant-numeric: tabular-nums; letter-spacing: .08em; }

.scrolly-right { display: flex; flex-direction: column; gap: clamp(32px, 5vw, 88px); }
.scrolly-item {
  margin: 0;
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: var(--surface-2);
  opacity: .38;
  transform: scale(.965);
  transition: opacity var(--dur-slow) var(--ease), transform var(--dur-slow) var(--ease);
}
.scrolly-item.on { opacity: 1; transform: none; }
.scrolly-item img { display: block; width: 100%; height: min(72vh, 640px); object-fit: cover; }
.scrolly-item figcaption {
  position: absolute; left: 0; right: 0; bottom: 0;
  display: flex; align-items: baseline; justify-content: space-between; gap: 16px;
  padding: 40px 22px 18px;
  background: linear-gradient(to top, rgba(10, 10, 12, .82), transparent);
}
.scrolly-item figcaption strong { font-family: var(--font-display); font-weight: 400; font-size: clamp(17px, 1.7vw, 24px); color: var(--text-media); }
.scrolly-item figcaption span { color: color-mix(in srgb, var(--text-media) 66%, transparent, .6); font-size: 12px; }

@media (max-width: 860px) {
  .scrolly { grid-template-columns: 1fr; }
  .scrolly-sticky { position: static; }
  .scrolly-progress { display: none; }
  .scrolly-item img { height: 56vw; }
}
@media (prefers-reduced-motion: reduce) {
  .scrolly-item { opacity: 1; transform: none; transition: none; }
}
</style>
