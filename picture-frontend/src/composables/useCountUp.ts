import { ref, watch, type Ref } from 'vue'

const reducedMotion =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
    : false

/**
 * 数字滚动（P2-2）：源值变化时以 easeOutCubic 在 duration 内滚动到新值。
 * source 可传 Ref 或 getter。prefers-reduced-motion 下直接跳到目标值。
 * 后台 KPI 可复用。
 */
export function useCountUp(source: Ref<number> | (() => number), duration = 320) {
  const read = () => Math.max(0, Number(typeof source === 'function' ? source() : source.value) || 0)
  const display = ref(read())
  let raf = 0

  watch(source, () => {
    const from = Number(display.value) || 0
    const target = read()
    if (reducedMotion || from === target) {
      display.value = target
      return
    }
    cancelAnimationFrame(raf)
    const start = performance.now()
    const tick = (now: number) => {
      const t = Math.min(1, (now - start) / duration)
      const eased = 1 - Math.pow(1 - t, 3)
      display.value = Math.round(from + (target - from) * eased)
      if (t < 1) raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)
  })

  return display
}
