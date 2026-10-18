import type { Directive } from 'vue'

const reduced =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
    : false

/**
 * v-reveal（视觉冲击力专项）：进入视口时显现。
 * v-reveal 或 v-reveal="延迟毫秒"（级联）；v-reveal:clip 走 clip-path 揭示变体。
 * 用 rAF 视口检查而非 IntersectionObserver——与 Lenis 平滑滚动等程序化滚动方案完全兼容
 * （IO 在 Lenis 拦截原生滚动的场景下时序不可靠，实测踩坑）。
 * prefers-reduced-motion 下直接显示。
 */
const watching = new Set<HTMLElement>()
let rafId = 0

function tick() {
  for (const el of watching) {
    const r = el.getBoundingClientRect()
    const vh = window.innerHeight || document.documentElement.clientHeight
    if (r.top < vh * 0.94 && r.bottom > 0 && r.width > 0) {
      el.classList.add('is-revealed')
      el.classList.remove('will-reveal', 'will-reveal-clip')
      watching.delete(el)
    }
  }
  if (watching.size) rafId = requestAnimationFrame(tick)
  else rafId = 0
}

function watch(el: HTMLElement) {
  watching.add(el)
  if (!rafId) rafId = requestAnimationFrame(tick)
}

export const vReveal: Directive<HTMLElement, number | undefined> = {
  mounted(el, binding) {
    const delay = Math.min(600, Math.max(0, Number(binding.value) || 0))
    if (reduced) {
      el.classList.add('is-revealed')
      return
    }
    el.classList.add('will-reveal')
    if (binding.arg === 'clip') el.classList.add('will-reveal-clip')
    if (delay) el.style.setProperty('--reveal-delay', delay + 'ms')
    watch(el)
  },
  unmounted(el) {
    watching.delete(el)
  },
}
