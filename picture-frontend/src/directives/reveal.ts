import type { Directive } from 'vue'

const reduced =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
    : false

/**
 * v-reveal（视觉冲击力专项）：进入视口时上浮显现。
 * v-reveal 或 v-reveal="延迟毫秒"（做级联入场）。
 * prefers-reduced-motion 下直接显示；不支持 IO 的环境直接显示。
 */
export const vReveal: Directive<HTMLElement, number | undefined> = {
  mounted(el, binding) {
    const delay = Math.min(600, Math.max(0, Number(binding.value) || 0))
    if (reduced || typeof IntersectionObserver === 'undefined') {
      el.classList.add('is-revealed')
      return
    }
    el.classList.add('will-reveal')
    if (delay) el.style.setProperty('--reveal-delay', delay + 'ms')
    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            el.classList.add('is-revealed')
            io.disconnect()
          }
        }
      },
      { rootMargin: '0px 0px -8% 0px', threshold: 0.05 },
    )
    io.observe(el)
  },
}
