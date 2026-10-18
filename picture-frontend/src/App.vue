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
  </a-config-provider>
</template>

<script setup lang="ts">
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import { antdTheme } from '@/config/antdTheme'
import CommandPalette from '@/components/layout/CommandPalette.vue'
import { onBeforeUnmount, onMounted, ref } from 'vue'
import Lenis from 'lenis'

dayjs.locale('zh-cn')

/** 鼠标跟随光晕：rAF 缓动追随（0.08 lerp），触屏与 reduced-motion 不启用 */
const glowEl = ref<HTMLElement | null>(null)
let glowRaf = 0

/** Lenis 平滑滚动（motionsites 手感的核心）：wheel 归一化 + lerp 追随；reduced-motion 不启用 */
let lenis: any = null
let lenisRaf = 0
onMounted(() => {
  const reduced = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!reduced) {
    lenis = new Lenis({ lerp: 0.11, smoothWheel: true })
    const lenisLoop = (t: number) => { lenis.raf(t); lenisRaf = requestAnimationFrame(lenisLoop) }
    lenisRaf = requestAnimationFrame(lenisLoop)
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
})
onBeforeUnmount(() => {
  ;(window as any)._glowCleanup?.()
  if (lenis) { lenis.destroy(); cancelAnimationFrame(lenisRaf) }
})
</script>

<style>
/* 自托管衬线字体：随构建打包，不依赖外网（Google Fonts 国内不可达） */
@import '@fontsource/noto-serif-sc/400.css';
@import '@fontsource/noto-serif-sc/600.css';
@import '@/styles/tokens.css';
@import '@/styles/app.css';
</style>
