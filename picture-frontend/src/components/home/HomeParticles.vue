<template>
  <canvas ref="canvasEl" class="particles" aria-hidden="true"></canvas>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * 星尘粒子（Active Theory 式氛围）：canvas 2D，60 颗柔光点缓慢漂浮 + 闪烁。
 * 触屏 / reduced-motion 不启动；rAF 单循环，离开页面即清理。
 */
const canvasEl = ref<HTMLCanvasElement | null>(null)
let raf = 0

onMounted(() => {
  const canvas = canvasEl.value
  if (!canvas) return
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  let w = 0
  let h = 0

  const resize = () => {
    const r = canvas.parentElement?.getBoundingClientRect()
    w = r?.width || window.innerWidth
    h = r?.height || 400
    canvas.width = w * dpr
    canvas.height = h * dpr
    canvas.style.width = w + 'px'
    canvas.style.height = h + 'px'
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  resize()
  window.addEventListener('resize', resize)

  const COUNT = 60
  const dots = Array.from({ length: COUNT }, () => ({
    x: Math.random() * w,
    y: Math.random() * h,
    r: 0.8 + Math.random() * 1.8,
    vx: (Math.random() - 0.5) * 0.12,
    vy: -0.05 - Math.random() * 0.12,
    tw: Math.random() * Math.PI * 2,
    ts: 0.4 + Math.random() * 0.8,
  }))

  const loop = () => {
    ctx.clearRect(0, 0, w, h)
    for (const d of dots) {
      d.x += d.vx
      d.y += d.vy
      d.tw += 0.016 * d.ts
      if (d.y < -8) { d.y = h + 8; d.x = Math.random() * w }
      if (d.x < -8) d.x = w + 8
      if (d.x > w + 8) d.x = -8
      const a = 0.16 + 0.22 * (0.5 + 0.5 * Math.sin(d.tw))
      ctx.beginPath()
      ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(163, 182, 168, ${a.toFixed(3)})`
      ctx.fill()
    }
    raf = requestAnimationFrame(loop)
  }
  loop()

  ;(canvas as any)._cleanup = () => {
    window.removeEventListener('resize', resize)
    cancelAnimationFrame(raf)
  }
})

onBeforeUnmount(() => (canvasEl.value as any)?._cleanup?.())
</script>

<style scoped>
.particles { position: absolute; inset: 0; z-index: 0; pointer-events: none; }
</style>
