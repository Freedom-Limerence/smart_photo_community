import type { Directive } from 'vue'

const reduced =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia('(prefers-reduced-motion: reduce)').matches
    : false

/**
 * v-tilt（动效升级包）：卡片随鼠标 3D 倾斜 + 光斑内聚。
 * prefers-reduced-motion / 触屏环境自动禁用。
 */
export const vTilt: Directive<HTMLElement, number | undefined> = {
  mounted(el, binding) {
    if (reduced || !window.matchMedia('(hover: hover)').matches) return
    const max = Math.min(10, Math.max(2, Number(binding.value) || 6))
    let raf = 0
    let tx = 0, ty = 0, cx = 0, cy = 0

    const apply = () => {
      cx += (tx - cx) * 0.12
      cy += (ty - cy) * 0.12
      el.style.transform = `perspective(900px) rotateX(${cy.toFixed(2)}deg) rotateY(${cx.toFixed(2)}deg) translateZ(0)`
      if (Math.abs(tx - cx) > 0.05 || Math.abs(ty - cy) > 0.05) raf = requestAnimationFrame(apply)
      else raf = 0
    }
    const schedule = () => { if (!raf) raf = requestAnimationFrame(apply) }

    const onMove = (e: MouseEvent) => {
      const r = el.getBoundingClientRect()
      const px = (e.clientX - r.left) / r.width - 0.5
      const py = (e.clientY - r.top) / r.height - 0.5
      tx = px * max * 2
      ty = -py * max * 2
      el.style.setProperty('--glow-x', ((px + 0.5) * 100).toFixed(1) + '%')
      el.style.setProperty('--glow-y', ((py + 0.5) * 100).toFixed(1) + '%')
      schedule()
    }
    const onLeave = () => {
      tx = 0; ty = 0
      schedule()
    }

    el.style.willChange = 'transform'
    el.addEventListener('mousemove', onMove)
    el.addEventListener('mouseleave', onLeave)
    ;(el as any)._tiltCleanup = () => {
      el.removeEventListener('mousemove', onMove)
      el.removeEventListener('mouseleave', onLeave)
      if (raf) cancelAnimationFrame(raf)
      el.style.transform = ''
    }
  },
  unmounted(el) {
    ;(el as any)._tiltCleanup?.()
  },
}
