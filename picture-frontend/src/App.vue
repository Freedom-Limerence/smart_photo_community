<template>
  <a-config-provider :locale="zhCN" :theme="antdTheme">
    <router-view v-slot="{ Component, route }">
      <transition name="page" mode="out-in">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
    <!-- 全局命令面板：Ctrl/⌘+K，或顶栏搜索框聚焦唤起（P2-3） -->
    <CommandPalette />
    <!-- 鼠标跟随光晕（motionsites 式氛围）：rAF 缓动追随光标 -->
    <div ref="glowEl" class="cursor-glow" aria-hidden="true"></div>
    <!-- 自定义光标：圆环跟随，可点元素上放大填充（触屏不启用） -->
    <div ref="ringEl" class="cursor-ring" aria-hidden="true"></div>
    <!-- 首次进站编排：三字显影 → 幕布拉开（sessionStorage 记忆，只播一次） -->
    <Transition name="curtain">
      <div v-if="introOn" class="intro-curtain" aria-hidden="true" @click="endIntro">
        <div class="intro-chars">
          <span v-for="(ch, i) in ['光', '影', '集']" :key="ch" :style="{ animationDelay: i * 300 + 'ms' }">{{ ch }}</span>
        </div>
      </div>
    </Transition>
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { antdTheme } from '@/config/antdTheme'
import CommandPalette from '@/components/layout/CommandPalette.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { initMotion, getLenis } from './motion'

dayjs.locale('zh-cn')

/** 鼠标跟随光晕：rAF 缓动追随（0.08 lerp），触屏与 reduced-motion 不启用 */
const glowEl = ref<HTMLElement | null>(null)
const ringEl = ref<HTMLElement | null>(null)
let glowRaf = 0

/** 首次进站编排：三字显影 → 幕布拉开（reduced-motion / 同 session 二次进入不播） */
const introOn = ref(false)
let introTimer = 0
function endIntro() {
  introOn.value = false
  clearTimeout(introTimer)
  try { sessionStorage.setItem('photo_intro_done', '1') } catch { /* ignore */ }
}


onMounted(() => {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  let played = false
  try { played = !!sessionStorage.getItem('photo_intro_done') } catch { /* ignore */ }
  if (!reduced && !played) {
    introOn.value = true
    introTimer = window.setTimeout(endIntro, 2300)
  }
  // 电影级动效系统（gsap ticker 驱动 Lenis，与 ScrollTrigger 帧同步）
  initMotion()
  if (!reduced) {
    // Lenis 由 motion/index.ts 的 gsap ticker 驱动
  }
  const el = glowEl.value
  if (!el) return
  if (reduced) return
  if (!window.matchMedia('(hover: hover)').matches) return
  let tx = window.innerWidth / 2, ty = window.innerHeight / 3
  let x = tx, y = ty, started = false
  const onMove = (e: MouseEvent) => {
    tx = e.clientX; ty = e.clientY
    if (!started) { started = true; document.body.classList.add('cursor-glow-on') }
  }
  const loop = () => {
    x += (tx - x) * 0.08
    y += (ty - y) * 0.08
    el.style.transform = `translate(${x.toFixed(1)}px, ${y.toFixed(1)}px)`
    glowRaf = requestAnimationFrame(loop)
  }
  window.addEventListener('mousemove', onMove, { passive: true })
  glowRaf = requestAnimationFrame(loop)
  ;(window as any)._glowCleanup = () => {
    window.removeEventListener('mousemove', onMove)
    cancelAnimationFrame(glowRaf)
    document.body.classList.remove('cursor-glow-on')
  }

  // 自定义光标环：位置快追随（0.22 lerp），悬停可点元素放大填充
  const ring = ringEl.value
  if (ring) {
    let rx = tx, ry = ty
    const ringLoop = () => {
      rx += (tx - rx) * 0.22
      ry += (ty - ry) * 0.22
      ring.style.transform = `translate(${rx.toFixed(1)}px, ${ry.toFixed(1)}px)`
      requestAnimationFrame(ringLoop)
    }
    requestAnimationFrame(ringLoop)
    const HOVER_SEL = 'a, button, [role=button], input, textarea, select, .frame-image, .hit-card'
    const onOver = (e: MouseEvent) => {
      const t = e.target as HTMLElement | null
      document.body.classList.toggle('cursor-hover', !!t?.closest?.(HOVER_SEL))
    }
    window.addEventListener('mouseover', onOver, { passive: true })
    ;(window as any)._ringCleanup = () => {
      window.removeEventListener('mouseover', onOver)
    }
  }
})
onBeforeUnmount(() => {
  ;(window as any)._glowCleanup?.()
  ;(window as any)._ringCleanup?.()
})
</script>

<style>
/* 自托管衬线字体：随构建打包，不依赖外网（Google Fonts 国内不可达） */
@import '@fontsource/noto-serif-sc/400.css';
@import '@fontsource/noto-serif-sc/600.css';
@import '@/styles/tokens.css';
@import '@/styles/app.css';
</style>
