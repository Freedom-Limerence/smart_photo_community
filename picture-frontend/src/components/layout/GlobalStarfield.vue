<template>
  <!-- 全局深空背景层：底色渐变 + 银河带 + 三层星 + 流星 + 星云光晕（fixed，全站生效） -->
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

  // 银河带参数：斜跨屏幕的对角线带（星密 + 乳白光雾）
  const band = {
    x0: w * 0.85,
    y0: -h * 0.1,
    x1: w * 0.1,
    y1: h * 1.15,
  }
  const bandDist = (x: number, y: number) => {
    // 点到线段距离
    const dx = band.x1 - band.x0
    const dy = band.y1 - band.y0
    const l2 = dx * dx + dy * dy
    let t = ((x - band.x0) * dx + (y - band.y0) * dy) / l2
    t = Math.max(0, Math.min(1, t))
    const px = band.x0 + t * dx
    const py = band.y0 + t * dy
    return Math.hypot(x - px, y - py)
  }
  const bandWidth = Math.min(w, h) * 0.34

  const stars: Star[] = []
  const spawn = (n: number, layer: Star['layer'], inBand = false) => {
    for (let i = 0; i < n; i++) {
      let x = Math.random() * w
      let y = Math.random() * h
      if (inBand) {
        // 60% 的带内星：沿线段随机 + 法向高斯散布
        const t = Math.random()
        const px = band.x0 + (band.x1 - band.x0) * t
        const py = band.y0 + (band.y1 - band.y0) * t
        const spread = (Math.random() + Math.random() + Math.random() - 1.5) * bandWidth * 0.55
        const nx = -(band.y1 - band.y0) / Math.hypot(band.x1 - band.x0, band.y1 - band.y0)
        const ny = (band.x1 - band.x0) / Math.hypot(band.x1 - band.x0, band.y1 - band.y0)
        x = px + nx * spread
        y = py + ny * spread
      }
      stars.push({
        x,
        y,
        r: layer === 1 ? 0.5 + Math.random() * 0.6 : layer === 2 ? 0.9 + Math.random() * 0.8 : 1.5 + Math.random() * 1.1,
        base: layer === 1 ? 0.45 + Math.random() * 0.35 : layer === 2 ? 0.55 + Math.random() * 0.45 : 0.8 + Math.random() * 0.2,
        phase: Math.random() * Math.PI * 2,
        speed: layer === 2 ? 1.2 + Math.random() * 2.2 : layer === 3 ? 0.5 + Math.random() * 0.8 : 0.1 + Math.random() * 0.2,
        vx: layer === 3 ? (Math.random() - 0.5) * 0.06 : (Math.random() - 0.5) * 0.01,
        vy: layer === 3 ? (Math.random() - 0.5) * 0.05 : 0,
        glow: layer === 3 ? 12 + Math.random() * 16 : layer === 2 ? 4 + Math.random() * 5 : 0,
        layer,
      })
    }
  }
  // 总 340 颗：远 150（40% 银河带内）/ 中 110（50% 带内）/ 近 60（30% 带内）
  spawn(90, 1)
  spawn(60, 1, true)
  spawn(55, 2)
  spawn(55, 2, true)
  spawn(42, 3)
  spawn(18, 3, true)

  // 流星：3-5s 一颗
  const meteors: Meteor[] = []
  let nextMeteor = performance.now() + 1200
  function spawnMeteor() {
    const fromLeft = Math.random() > 0.5
    const sx = fromLeft ? Math.random() * w * 0.5 : w * (0.5 + Math.random() * 0.5)
    const sy = Math.random() * h * 0.65
    const dir = fromLeft ? 1 : -1
    const speed = 13 + Math.random() * 9
    meteors.push({
      x: sx,
      y: sy,
      vx: dir * speed * 0.82,
      vy: speed * 0.57,
      len: 220 + Math.random() * 140,
    })
    nextMeteor = performance.now() + 2500 + Math.random() * 2000
  }

  let last = performance.now()
  function frame(now: number) {
    const dt = Math.min(0.05, (now - last) / 1000)
    last = now
    const t = now / 1000

    // 深空蓝底色（比纯黑亮一档，可感知的蓝）
    const g = ctx!.createLinearGradient(0, 0, 0, h)
    g.addColorStop(0, 'rgb(2, 2, 4)')
    g.addColorStop(0.5, 'rgb(3, 3, 6)')
    g.addColorStop(1, 'rgb(1, 1, 3)')
    ctx!.fillStyle = g
    ctx!.fillRect(0, 0, w, h)

    // 银河带光雾（多层淡白蓝，沿对角线）
    ctx!.save()
    const bx = (band.x0 + band.x1) / 2
    const by = (band.y0 + band.y1) / 2
    const angle = Math.atan2(band.y1 - band.y0, band.x1 - band.x0)
    ctx!.translate(bx, by)
    ctx!.rotate(angle)
    const bw = Math.hypot(band.x1 - band.x0, band.y1 - band.y0) * 0.75
    const mg = ctx!.createLinearGradient(0, -bandWidth, 0, bandWidth)
    mg.addColorStop(0, 'rgba(150, 175, 235, 0)')
    mg.addColorStop(0.5, 'rgba(150, 175, 235, 0.14)')
    mg.addColorStop(1, 'rgba(150, 175, 235, 0)')
    ctx!.fillStyle = mg
    ctx!.fillRect(-bw / 2, -bandWidth, bw, bandWidth * 2)
    const mg2 = ctx!.createLinearGradient(0, -bandWidth * 0.3, 0, bandWidth * 0.3)
    mg2.addColorStop(0, 'rgba(205, 220, 250, 0)')
    mg2.addColorStop(0.5, 'rgba(205, 220, 250, 0.11)')
    mg2.addColorStop(1, 'rgba(205, 220, 250, 0)')
    ctx!.fillStyle = mg2
    ctx!.fillRect(-bw / 2, -bandWidth * 0.3, bw, bandWidth * 0.6)
    ctx!.restore()

    // 星云光晕：随滚动上移，覆盖第二/三屏
    const sy = window.scrollY || window.pageYOffset || 0
    const blobs = [
      { x: w * 0.24, y: h * 1.5 - sy * 0.35, r: Math.max(w, h) * 0.4, c: '70, 110, 235' },
      { x: w * 0.78, y: h * 2.35 - sy * 0.3, r: Math.max(w, h) * 0.36, c: '140, 80, 235' },
    ]
    for (const bl of blobs) {
      if (bl.y + bl.r < 0 || bl.y - bl.r > h) continue
      const ng = ctx!.createRadialGradient(bl.x, bl.y, 0, bl.x, bl.y, bl.r)
      ng.addColorStop(0, `rgba(${bl.c}, 0.34)`)
      ng.addColorStop(0.5, `rgba(${bl.c}, 0.15)`)
      ng.addColorStop(1, `rgba(${bl.c}, 0)`)
      ctx!.fillStyle = ng
      ctx!.fillRect(bl.x - bl.r, bl.y - bl.r, bl.r * 2, bl.r * 2)
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
      const tw = s.layer >= 2 ? s.base * (0.55 + 0.45 * Math.sin(t * s.speed + s.phase)) : s.base
      ctx!.beginPath()
      if (s.glow > 0) {
        ctx!.shadowBlur = s.glow
        ctx!.shadowColor = 'rgba(200, 218, 255, 0.95)'
      }
      ctx!.fillStyle = `rgba(222, 232, 250, ${tw.toFixed(3)})`
      ctx!.arc(s.x, s.y, s.r, 0, Math.PI * 2)
      ctx!.fill()
      ctx!.shadowBlur = 0
    }

    // 流星
    if (now > nextMeteor && meteors.length < 3) spawnMeteor()
    for (let i = meteors.length - 1; i >= 0; i--) {
      const m = meteors[i]
      m.x += m.vx
      m.y += m.vy
      if (m.y > h + 80 || m.x < -300 || m.x > w + 300) {
        meteors.splice(i, 1)
        continue
      }
      const tailX = m.x - m.vx * (m.len / 12)
      const tailY = m.y - m.vy * (m.len / 12)
      const grad = ctx!.createLinearGradient(m.x, m.y, tailX, tailY)
      grad.addColorStop(0, 'rgba(255, 255, 255, 1)')
      grad.addColorStop(0.3, 'rgba(190, 214, 255, 0.6)')
      grad.addColorStop(1, 'rgba(140, 170, 255, 0)')
      ctx!.strokeStyle = grad
      ctx!.lineWidth = 2.6
      ctx!.lineCap = 'round'
      ctx!.beginPath()
      ctx!.moveTo(m.x, m.y)
      ctx!.lineTo(tailX, tailY)
      ctx!.stroke()
      ctx!.beginPath()
      ctx!.shadowBlur = 14
      ctx!.shadowColor = 'rgba(255, 255, 255, 0.95)'
      ctx!.fillStyle = 'rgba(255, 255, 255, 0.95)'
      ctx!.arc(m.x, m.y, 1.6, 0, Math.PI * 2)
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
    const ctx = canvasRef.value.getContext('2d')
    if (ctx) {
      canvasRef.value.width = window.innerWidth
      canvasRef.value.height = window.innerHeight
      const g = ctx.createLinearGradient(0, 0, 0, canvasRef.value.height)
      g.addColorStop(0, 'rgb(2, 2, 4)')
      g.addColorStop(1, 'rgb(1, 1, 3)')
      ctx.fillStyle = g
      ctx.fillRect(0, 0, canvasRef.value.width, canvasRef.value.height)
      for (let i = 0; i < 320; i++) {
        ctx.fillStyle = `rgba(222,232,250,${0.3 + Math.random() * 0.6})`
        ctx.beginPath()
        ctx.arc(Math.random() * canvasRef.value.width, Math.random() * canvasRef.value.height, Math.random() * 1.5, 0, Math.PI * 2)
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
  background: radial-gradient(ellipse 125% 108% at 50% 45%, transparent 60%, rgba(0, 0, 0, .42) 100%);
}
</style>
