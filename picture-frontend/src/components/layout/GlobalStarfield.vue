<template>
  <!-- 全局深空背景层：底色渐变 + 三层星 + 流星 + 星云光晕（fixed，全站生效） -->
  <div class="global-starfield" aria-hidden="true">
    <canvas ref="canvasRef" class="sf-canvas" />
  </div>
  <!-- 顶层暗角包裹（视线聚焦） -->
  <div class="global-vignette" aria-hidden="true" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

const canvasRef = ref<HTMLCanvasElement | null>(null)
let raf = 0
let cleanup: (() => void) | null = null

interface Star {
  x: number
  y: number
  r: number
  base: number
  phase: number
  speed: number
  vx: number
  vy: number
  glow: number
  layer: 1 | 2 | 3
}

interface Meteor {
  x: number
  y: number
  vx: number
  vy: number
  len: number
  life: number
}

function init(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  let w = 0
  let h = 0
  const dpr = Math.min(2, window.devicePixelRatio || 1)

  function resize() {
    w = window.innerWidth
    h = window.innerHeight
    canvas.width = w * dpr
    canvas.height = h * dpr
    canvas.style.width = w + 'px'
    canvas.style.height = h + 'px'
    ctx!.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  resize()
  window.addEventListener('resize', resize)

  // 三层星：远(静止极暗) / 中(闪烁) / 近(发光漂浮)
  const stars: Star[] = []
  const spawn = (n: number, layer: Star['layer']) => {
    for (let i = 0; i < n; i++) {
      stars.push({
        x: Math.random() * w,
        y: Math.random() * h,
        r: layer === 1 ? 0.4 + Math.random() * 0.5 : layer === 2 ? 0.8 + Math.random() * 0.6 : 1.4 + Math.random() * 0.9,
        base: layer === 1 ? 0.25 + Math.random() * 0.3 : layer === 2 ? 0.4 + Math.random() * 0.4 : 0.7 + Math.random() * 0.3,
        phase: Math.random() * Math.PI * 2,
        speed: layer === 2 ? 0.8 + Math.random() * 1.6 : layer === 3 ? 0.4 + Math.random() * 0.6 : 0.05 + Math.random() * 0.15,
        vx: layer === 3 ? (Math.random() - 0.5) * 0.05 : (Math.random() - 0.5) * 0.008,
        vy: layer === 3 ? (Math.random() - 0.5) * 0.04 : 0,
        glow: layer === 3 ? 6 + Math.random() * 10 : 0,
        layer,
      })
    }
  }
  spawn(140, 1)
  spawn(60, 2)
  spawn(22, 3)

  // 流星：5-8s 随机一颗，左上/右上斜下
  const neb1 = document.querySelector('.nebula-1') as HTMLElement | null
  const neb2 = document.querySelector('.nebula-2') as HTMLElement | null
  const meteors: Meteor[] = []
  let nextMeteor = performance.now() + 2500
  function spawnMeteor() {
    const fromLeft = Math.random() > 0.5
    const sx = fromLeft ? Math.random() * w * 0.35 : w * (0.65 + Math.random() * 0.35)
    const sy = Math.random() * h * 0.25
    const dir = fromLeft ? 1 : -1
    const speed = 9 + Math.random() * 7
    meteors.push({
      x: sx,
      y: sy,
      vx: dir * speed * 0.82,
      vy: speed * 0.57,
      len: 130 + Math.random() * 110,
      life: 1,
    })
    nextMeteor = performance.now() + 5000 + Math.random() * 3000
  }

  let last = performance.now()
  function frame(now: number) {
    const dt = Math.min(0.05, (now - last) / 1000)
    last = now
    const t = now / 1000

    // 深空底色（上深蓝 → 下更暗）
    const g = ctx!.createLinearGradient(0, 0, 0, h)
    g.addColorStop(0, 'rgb(5, 8, 20)')
    g.addColorStop(0.55, 'rgb(7, 10, 26)')
    g.addColorStop(1, 'rgb(6, 9, 19)')
    ctx!.fillStyle = g
    ctx!.fillRect(0, 0, w, h)

    // 星云光晕：随滚动上移，覆盖第二/三屏
    const sy = window.scrollY || window.pageYOffset || 0
    const blobs = [
      { x: w * 0.26, y: h * 1.45 - sy * 0.35, r: Math.max(w, h) * 0.36, c: '64, 100, 210' },
      { x: w * 0.76, y: h * 2.3 - sy * 0.3, r: Math.max(w, h) * 0.32, c: '124, 66, 208' },
    ]
    for (const b of blobs) {
      if (b.y + b.r < 0 || b.y - b.r > h) continue
      const ng = ctx!.createRadialGradient(b.x, b.y, 0, b.x, b.y, b.r)
      ng.addColorStop(0, `rgba(${b.c}, 0.8)`)
      ng.addColorStop(0.55, `rgba(${b.c}, 0.32)`)
      ng.addColorStop(1, `rgba(${b.c}, 0)`)
      ctx!.fillStyle = ng
      ctx!.fillRect(b.x - b.r, b.y - b.r, b.r * 2, b.r * 2)
    }

    // 星
    for (const s of stars) {
      if (s.layer === 3) {
        s.x += s.vx
        s.y += s.vy
        if (s.x < -5) s.x = w + 5
        if (s.x > w + 5) s.x = -5
        if (s.y < -5) s.y = h + 5
        if (s.y > h + 5) s.y = -5
      } else if (s.layer === 1 && s.speed > 0) {
        s.y += s.speed * dt
        if (s.y > h + 3) s.y = -3
      }
      const tw = s.layer >= 2 ? s.base * (0.62 + 0.38 * Math.sin(t * s.speed + s.phase)) : s.base
      ctx!.beginPath()
      if (s.glow > 0) {
        ctx!.shadowBlur = s.glow
        ctx!.shadowColor = 'rgba(190, 210, 255, 0.9)'
      }
      ctx!.fillStyle = `rgba(214, 226, 244, ${tw.toFixed(3)})`
      ctx!.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx!.fill()
      ctx!.shadowBlur = 0
    }

    // 流星
    if (now > nextMeteor && meteors.length < 2) spawnMeteor()
    for (let i = meteors.length - 1; i >= 0; i--) {
      const m = meteors[i]
      m.x += m.vx
      m.y += m.vy
      if (m.y > h + 60 || m.x < -260 || m.x > w + 260) {
        meteors.splice(i, 1)
        continue
      }
      const tailX = m.x - m.vx * (m.len / 12)
      const tailY = m.y - m.vy * (m.len / 12)
      const grad = ctx!.createLinearGradient(m.x, m.y, tailX, tailY)
      grad.addColorStop(0, 'rgba(255, 255, 255, 0.95)')
      grad.addColorStop(0.35, 'rgba(178, 208, 255, 0.55)')
      grad.addColorStop(1, 'rgba(140, 170, 255, 0)')
      ctx!.strokeStyle = grad
      ctx!.lineWidth = 1.6
      ctx!.lineCap = 'round'
      ctx!.beginPath()
      ctx!.moveTo(m.x, m.y)
      ctx!.lineTo(tailX, tailY)
      ctx!.stroke()
      // 头部亮点
      ctx!.beginPath()
      ctx!.shadowBlur = 12
      ctx!.shadowColor = 'rgba(255, 255, 255, 0.9)'
      ctx!.fillStyle = 'rgba(255, 255, 255, 0.9)'
      ctx!.arc(m.x, m.y, 1.4, 0, Math.PI * 2)
      ctx!.fill()
      ctx!.shadowBlur = 0
    }

    raf = requestAnimationFrame(frame)
  }
  raf = requestAnimationFrame(frame)

  function onVis() {
    if (document.hidden && raf) { cancelAnimationFrame(raf); raf = 0 }
    else if (!document.hidden && !raf) { last = performance.now(); raf = requestAnimationFrame(frame) }
  }
  document.addEventListener('visibilitychange', onVis)

  cleanup = () => {
    if (raf) cancelAnimationFrame(raf)
    window.removeEventListener('resize', resize)
    document.removeEventListener('visibilitychange', onVis)
  }
}

onMounted(() => {
  if (canvasRef.value && !window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    init(canvasRef.value)
  } else if (canvasRef.value) {
    // reduced-motion：静态星空单帧
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) {
      canvasRef.value.width = window.innerWidth
      canvasRef.value.height = window.innerHeight
      ctx.fillStyle = 'rgb(5, 8, 20)'
      ctx.fillRect(0, 0, canvasRef.value.width, canvasRef.value.height)
      for (let i = 0; i < 200; i++) {
        ctx.fillStyle = `rgba(214,226,244,${0.2 + Math.random() * 0.5})`
        ctx.beginPath()
        ctx.arc(Math.random() * canvasRef.value.width, Math.random() * canvasRef.value.height, Math.random() * 1.4, 0, Math.PI * 2)
        ctx.fill()
      }
    }
  }
})

onBeforeUnmount(() => cleanup?.())
</script>

<style scoped>
.global-starfield { position: fixed; inset: 0; z-index: 0; pointer-events: none; overflow: hidden; }
.sf-canvas { position: absolute; inset: 0; display: block; }

/* 顶层暗角包裹：聚焦视线 */
.global-vignette {
  position: fixed; inset: 0; z-index: 999; pointer-events: none;
  background: radial-gradient(ellipse 120% 105% at 50% 45%, transparent 58%, rgba(0, 0, 0, .38) 100%);
}
@media (prefers-reduced-motion: reduce) {
  .nebula { filter: blur(60px); }
}
</style>
