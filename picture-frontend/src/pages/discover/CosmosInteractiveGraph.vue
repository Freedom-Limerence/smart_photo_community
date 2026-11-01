<template>
  <div class="graph-shell">
    <div ref="mountEl" class="graph-canvas" data-lenis-prevent role="img" aria-label="影像星图。拖拽旋转视角，滚轮缩放，点击星点查看影像。" />
    <div class="graph-controls" aria-label="图谱视野控制">
      <button type="button" aria-label="推近星图" @click="zoomBy(0.8)"><svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg></button>
      <span class="zoom-value">{{ zoomPercent }}%</span>
      <button type="button" aria-label="拉远星图" @click="zoomBy(1.25)"><svg viewBox="0 0 24 24"><path d="M5 12h14"/></svg></button>
      <span class="control-divider"></span>
      <button type="button" title="回到全景" aria-label="回到全景" @click="fit"><svg viewBox="0 0 24 24"><path d="M9 4H4v5m11-5h5v5M4 15v5h5m11-5v5h-5"/></svg></button>
      <button type="button" class="text-control" @click="resetLayout">重新布局</button>
    </div>
    <div class="graph-legend"><i></i>{{ revealRatio < 0.4 ? '题材星座' : '影像已浮现' }}</div>
    <p class="graph-hint" :class="{ faded: interacted }">{{ hint }}</p>
    <p class="graph-announcement" role="status">{{ announcement }}</p>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import ForceGraph3D from '3d-force-graph'
import * as THREE from 'three'
import { CSS2DObject, CSS2DRenderer } from 'three/addons/renderers/CSS2DRenderer.js'

interface Hub { tag: string; count: number }
interface Work { workId: string; title: string; author: string; cover: string; heat: number; tags: string[]; hub?: string }
interface Edge { hub: string; leaf: string }
interface GraphNode {
  id: string
  kind: 'hub' | 'work'
  hub?: Hub
  work?: Work
  x?: number
  y?: number
  z?: number
  fx?: number
  fy?: number
  fz?: number
  __threeObj?: THREE.Group
}

const props = defineProps<{ hubs: Hub[]; works: Work[]; edges: Edge[]; selectedTag: string | null; selectedWork: string | null; neighborsOnly: boolean }>()
const emit = defineEmits<{ selectHub: [tag: string]; openWork: [work: Work]; interacted: []; clear: [] }>()

const mountEl = ref<HTMLElement | null>(null)
const zoomPercent = ref(100)
const revealRatio = ref(0)
const announcement = ref('')
const interacted = ref(false)
const hint = ref('拖拽旋转宇宙 · 滚轮推近，看清这片星空')
watch(revealRatio, (v) => {
  hint.value = v < 0.25
    ? '滚轮推近，看清这片星空'
    : v < 0.85
      ? '题材已现身 · 继续推近走进一个星团'
      : '点击影像卡查看详情 · 拉远回到星座'
})
const activeId = ref<string | null>(null)
watch(() => [props.selectedTag, props.selectedWork], ([tag, work]) => {
  activeId.value = tag ? 'tag:' + tag : work ? 'work:' + work : null
}, { immediate: true })

const neighbors = ref(new Map<string, Set<string>>())
watch(() => props.edges, (edges) => {
  const m = new Map<string, Set<string>>()
  for (const edge of edges) {
    const a = 'tag:' + edge.hub, b = 'work:' + edge.leaf
    if (!m.has(a)) m.set(a, new Set([a]))
    if (!m.has(b)) m.set(b, new Set([b]))
    m.get(a)!.add(b); m.get(b)!.add(a)
  }
  neighbors.value = m
}, { immediate: true, deep: true })

let graph: any = null
let disposed = false
let firstFit = true
let hoveredId: string | null = null
let reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

/**
 * 三级 LOD（17 的层级要求）：先光点 → 放大见标签 → 再放大见图片
 * L1 远景：纯光点星座（无文字）
 * L2 中景：题材标签浮现
 * L3 近景：拍立得（缩略图+标题）浮现
 */
const DIST_FAR = 780, DIST_NEAR = 330
function smooth01(t: number) { const x = Math.max(0, Math.min(1, t)); return x * x * (3 - 2 * x) }
function lodLevels() {
  if (!graph) return { label: 0, photo: 0, d: 9999 }
  const cam = graph.cameraPosition()
  const d = Math.hypot(cam.x, cam.y, cam.z)
  const t = Math.max(0, Math.min(1, (DIST_FAR - d) / (DIST_FAR - DIST_NEAR)))
  return {
    label: smooth01((t - 0.15) / 0.4),    // L2：中景标签（fit 全景≈1）
    photo: smooth01((t - 0.72) / 0.28),   // L3：近景拍立得（只在继续推近后）
    d,
  }
}
// 兼容旧调用点
function photoReveal() { return lodLevels().photo }

const textures: THREE.Texture[] = []
const hubMeta = new Map<string, { glow: THREE.Sprite; card: HTMLElement; phase: number }>()
const leafMeta = new Map<string, { glow: THREE.Sprite; polaroid: THREE.Sprite; polaroidMat: THREE.SpriteMaterial; phase: number }>()
let thumbLoader: THREE.TextureLoader | null = null
let css2d: CSS2DRenderer | null = null

function glowTexture(color: string): THREE.Texture {
  const c = document.createElement('canvas')
  c.width = c.height = 128
  const ctx = c.getContext('2d')!
  const g = ctx.createRadialGradient(64, 64, 4, 64, 64, 64)
  g.addColorStop(0, 'rgba(255,255,255,.95)')
  g.addColorStop(0.25, color)
  g.addColorStop(1, 'rgba(0,0,0,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, 128, 128)
  const tex = new THREE.CanvasTexture(c)
  textures.push(tex)
  return tex
}

const HUB_PALETTE = [0x8fd8f0, 0xf0c987, 0xb8a7e8, 0xf0a7b8, 0xa7e8c9, 0xf0d8a7, 0xc9b8f0, 0xa7d8c9]
function hubColor(i: number) { return '#' + HUB_PALETTE[i % HUB_PALETTE.length].toString(16).padStart(6, '0') }
function escapeHtml(s: string) { return s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c] as string)) }

/** Hub：Three 光晕 + CSS2D 文字卡（常显） */
function makeHubObject(node: GraphNode, onHub: (tag: string) => void): THREE.Group {
  const hub = node.hub!
  const i = props.hubs.findIndex((h) => h.tag === hub.tag)
  const color = hubColor(i)
  const group = new THREE.Group()
  const glow = new THREE.Sprite(new THREE.SpriteMaterial({
    map: glowTexture(color), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0.95,
  }))
  const s = 15 + Math.min(13, hub.count * 3)
  glow.scale.set(s, s, 1)
  group.add(glow)

  const card = document.createElement('div')
  card.className = 'cg-hub-card'
  card.innerHTML = `<b>${escapeHtml(hub.tag)}</b><span>${hub.count} 幅</span>`
  card.addEventListener('click', (ev) => { ev.stopPropagation(); markInteracted(); onHub(hub.tag) })
  for (const evt of ['pointerdown', 'pointerup', 'mousedown', 'mouseup', 'touchstart', 'touchend']) {
    card.addEventListener(evt, (ev) => ev.stopPropagation())
  }
  const cssObj = new CSS2DObject(card)
  cssObj.position.set(0, s * 0.5 + 2.2, 0)
  group.add(cssObj)
  hubMeta.set(node.id, { glow, card, phase: i * 1.7 })
  return group
}

/** Leaf：小光晕 + 拍立得 Sprite（canvas 纹理：缩略图 + 标题常显） */
function makeLeafObject(node: GraphNode): THREE.Group {
  const work = node.work!
  const hubIdx = Math.max(0, props.hubs.findIndex((h) => h.tag === work.hub))
  const color = '#' + HUB_PALETTE[hubIdx % HUB_PALETTE.length].toString(16).padStart(6, '0')
  const group = new THREE.Group()
  const glow = new THREE.Sprite(new THREE.SpriteMaterial({
    map: glowTexture(color), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0.8,
  }))
  glow.scale.set(8, 8, 1)
  group.add(glow)

  const cardCanvas = document.createElement('canvas')
  cardCanvas.width = 220; cardCanvas.height = 210
  const cctx = cardCanvas.getContext('2d')!
  const title = work.title && work.title.length > 14 ? work.title.slice(0, 13) + '…' : (work.title || '未命名作品')
  function paintCard(img?: HTMLImageElement) {
    cctx.clearRect(0, 0, 220, 210)
    cctx.fillStyle = 'rgba(12,15,12,.94)'
    cctx.beginPath(); cctx.roundRect(4, 4, 212, 202, 10); cctx.fill()
    cctx.strokeStyle = hoveredId === node.id ? 'rgba(193,220,200,.9)' : 'rgba(163,184,170,.35)'
    cctx.lineWidth = 2; cctx.stroke()
    cctx.save()
    cctx.beginPath(); cctx.roundRect(14, 14, 192, 138, 6); cctx.clip()
    if (img && img.naturalWidth) {
      const r = Math.max(192 / img.naturalWidth, 138 / img.naturalHeight)
      const sw = 192 / r, sh = 138 / r
      cctx.drawImage(img, (img.naturalWidth - sw) / 2, (img.naturalHeight - sh) / 2, sw, sh, 14, 14, 192, 138)
    } else {
      cctx.fillStyle = 'rgba(54,68,58,.9)'; cctx.fillRect(14, 14, 192, 138)
      cctx.fillStyle = 'rgb(191,207,196)'; cctx.font = '20px sans-serif'; cctx.textAlign = 'center'
      cctx.fillText('影像待呈现', 110, 90)
    }
    cctx.restore()
    cctx.fillStyle = 'rgb(215,224,216)'
    cctx.font = '500 19px "Noto Serif SC", serif'
    cctx.textAlign = 'center'
    cctx.fillText(title, 110, 182)
    cardTexture.needsUpdate = true
  }
  const cardTexture = new THREE.CanvasTexture(cardCanvas)
  textures.push(cardTexture)
  const polaroid = new THREE.Sprite(new THREE.SpriteMaterial({ map: cardTexture, transparent: true, depthWrite: false }))
  polaroid.scale.set(13, 12.4, 1)
  group.add(polaroid)
  if (work.cover) {
    const img = new Image()
    let retried = false
    img.onload = () => { if (!disposed) paintCard(img) }
    img.onerror = () => {
      if (disposed) return
      if (!retried) { retried = true; window.setTimeout(() => { img.src = work.cover + '?r=1' }, 1500) }
      else paintCard()
    }
    img.src = work.cover   // /cos 同源反代无跨域污染
  }
  paintCard()
  leafMeta.set(node.id, { glow, polaroid, polaroidMat: polaroid.material as THREE.SpriteMaterial, phase: Math.random() * Math.PI * 2 })
  return group
}

function nodeThreeObject(node: GraphNode) {
  return node.kind === 'hub'
    ? makeHubObject(node, (tag) => { emit('selectHub', tag); focusSelection() })
    : makeLeafObject(node)
}

/** 星尘背景：三层视差微星 + 三片星云雾 */
function addStardust(scene: THREE.Scene) {
  const layers = [
    { count: 160, rMin: 500, rMax: 900, size: 2.2, opacity: 0.5 },
    { count: 110, rMin: 320, rMax: 520, size: 3.2, opacity: 0.65 },
    { count: 60, rMin: 180, rMax: 330, size: 4.5, opacity: 0.8 },
  ]
  for (const layer of layers) {
    const pos = new Float32Array(layer.count * 3)
    for (let i = 0; i < layer.count; i++) {
      const v = new THREE.Vector3().randomDirection().multiplyScalar(layer.rMin + Math.random() * (layer.rMax - layer.rMin))
      pos.set([v.x, v.y, v.z], i * 3)
    }
    const geo = new THREE.BufferGeometry()
    geo.setAttribute('position', new THREE.BufferAttribute(pos, 3))
    const dustTex = glowTexture('rgba(207,224,216,.9)')
    scene.add(new THREE.Points(geo, new THREE.PointsMaterial({ map: dustTex, color: 0xcfe0d8, size: layer.size * 2.2, sizeAttenuation: true, transparent: true, opacity: layer.opacity, depthWrite: false, blending: THREE.AdditiveBlending })))
  }
  const nebulaColors = ['rgba(96,140,220,.16)', 'rgba(190,140,220,.12)', 'rgba(120,200,170,.13)']
  nebulaColors.forEach((c, i) => {
    const c2 = document.createElement('canvas')
    c2.width = c2.height = 256
    const ctx = c2.getContext('2d')!
    const g = ctx.createRadialGradient(128, 128, 10, 128, 128, 128)
    g.addColorStop(0, c)
    g.addColorStop(1, 'rgba(0,0,0,0)')
    ctx.fillStyle = g
    ctx.fillRect(0, 0, 256, 256)
    const tex = new THREE.CanvasTexture(c2)
    textures.push(tex)
    const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex, transparent: true, depthWrite: false }))
    const a = i * 2.2
    sp.position.set(Math.cos(a) * 260, (i - 1) * 90, Math.sin(a) * 260)
    sp.scale.set(560, 560, 1)
    scene.add(sp)
  })
}

function updateData() {
  if (!graph) return
  hubMeta.clear(); leafMeta.clear()
  // Hub：确定性球环布局 + 坐标钉死（构图可控，刷新一致、永远在视野内）
  const hubs: GraphNode[] = props.hubs.map((hub, i) => {
    const a = (i / Math.max(1, props.hubs.length)) * Math.PI * 2 - Math.PI / 2
    const r = 130
    const x = Math.cos(a) * r, y = (i % 3 - 1) * 42, z = Math.sin(a) * r
    return { id: 'tag:' + hub.tag, kind: 'hub' as const, hub, x, y, z, fx: x, fy: y, fz: z }
  })
  // Leaf：环绕所属 Hub 的确定性轨道（钉死）
  const hubById = new Map(hubs.map((nn) => [nn.id, nn]))
  const ringCount = new Map<string, number>()
  const works: GraphNode[] = props.works.map((work) => {
    const hubNode = hubById.get('tag:' + (work.hub ?? props.hubs[0]?.tag ?? ''))
    const k = hubNode ? (ringCount.get(hubNode.id) ?? 0) : 0
    if (hubNode) ringCount.set(hubNode.id, k + 1)
    const a = k * 2.39996
    const rr = 62
    const node: GraphNode = { id: 'work:' + work.workId, kind: 'work' as const, work }
    node.x = (hubNode?.x ?? 0) + Math.cos(a) * rr
    node.y = (hubNode?.y ?? 0) + Math.sin(a * 1.7) * 22
    node.z = (hubNode?.z ?? 0) + Math.sin(a) * rr
    node.fx = node.x; node.fy = node.y; node.fz = node.z
    return node
  })
  firstFit = true
  graph.graphData({
    nodes: [...hubs, ...works],
    links: props.edges.map((e) => ({ source: 'tag:' + e.hub, target: 'work:' + e.leaf })),
  })
  graph.d3Force('charge')?.strength(-300)
  graph.d3Force('link')?.distance(85).strength(0.35)
}

function fit() {
  if (!graph) return
  const hubs = graph.graphData().nodes.filter((nn: GraphNode) => nn.kind === 'hub')
  const box = new THREE.Box3().setFromPoints(hubs.map((nn: any) => new THREE.Vector3(nn.x ?? 0, nn.y ?? 0, nn.z ?? 0)))
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3()).length()
  const d = Math.max(340, size * 1.15)
  graph.cameraPosition(
    { x: center.x + d * 0.4, y: center.y + d * 0.24, z: center.z + d * 0.9 },
    center, reducedMotion ? 0 : 1100,
  )
}
function resetLayout() { emit('clear'); announcement.value = '已恢复星图布局' }
function zoomBy(factor: number) {
  if (!graph) return
  const cam = graph.cameraPosition()
  const f = Math.max(0.25, Math.min(4, factor))
  graph.cameraPosition(
    { x: cam.x * f, y: cam.y * f, z: cam.z * f },
    { x: 0, y: 0, z: 0 }, reducedMotion ? 0 : 450,
  )
}
function focusSelection() {
  if (!activeId.value || !graph) return
  const node: GraphNode | undefined = graph.graphData().nodes.find((nn: GraphNode) => nn.id === activeId.value)
  if (!node) return
  const d = 270
  graph.cameraPosition(
    { x: (node.x ?? 0) + d * 0.3, y: (node.y ?? 0) + d * 0.22, z: (node.z ?? 0) + d * 0.85 },
    { x: node.x ?? 0, y: node.y ?? 0, z: node.z ?? 0 }, reducedMotion ? 0 : 1100,
  )
}

/** 每帧：呼吸 + LOD + 高亮 + CSS2D 位置保底同步 */
let raf = 0
function tick() {
  if (disposed || !graph) return
  const t = performance.now() / 1000
  const levels = lodLevels()
  revealRatio.value = levels.label
  // CSS2D 层保底在 canvas 之上
  if (css2d) {
    if (css2d.domElement.style.zIndex !== '5') css2d.domElement.style.zIndex = '5'
    if (css2d.domElement.style.pointerEvents !== 'none') css2d.domElement.style.pointerEvents = 'none'
  }
  const active = hoveredId || activeId.value
  const nodes: GraphNode[] = graph.graphData().nodes ?? []
  for (const node of nodes) {
    const obj = node.__threeObj
    if (!obj) continue
    const isHub = node.kind === 'hub'
    const meta = isHub ? hubMeta.get(node.id) : leafMeta.get(node.id)
    if (!meta) continue
    const hot = hoveredId === node.id || node.id === activeId.value
    const dim = !!active && !neighbors.value.get(active)?.has(node.id)
    const pulse = 1 + Math.sin(t * 1.35 + meta.phase) * (reducedMotion ? 0 : (isHub ? 0.1 : 0.16))
    if (isHub) {
      const m = meta as { glow: THREE.Sprite; card: HTMLElement; phase: number }
      const base = 15 + Math.min(13, (node.hub?.count ?? 0) * 3)
      m.glow.scale.setScalar(base * pulse * (dim ? 0.55 : hot ? 1.3 : 1))
      ;(m.glow.material as THREE.SpriteMaterial).opacity = dim ? 0.3 : 0.95
      // L2：题材标签按级浮现
      const cardOpacity = levels.label * (dim ? 0.25 : hot ? 1 : 0.92)
      m.card.style.opacity = cardOpacity.toFixed(2)
      m.card.style.visibility = cardOpacity > 0.05 ? 'visible' : 'hidden'
      m.card.classList.toggle('hot', hot)
      m.card.classList.toggle('dim', dim)
    } else {
      const m = meta as { glow: THREE.Sprite; polaroid: THREE.Sprite; polaroidMat: THREE.SpriteMaterial }
      // L1：leaf 只是微光点
      m.glow.scale.setScalar(7 * pulse * (0.7 + levels.photo * 0.5) * (dim ? 0.25 : hot ? 1.6 : 1))
      ;(m.glow.material as THREE.SpriteMaterial).opacity = (0.35 + levels.photo * 0.45) * (dim ? 0.15 : 1)
      // L3：拍立得按级浮现
      const base = 13 * (0.75 + levels.photo * 0.4) * (dim ? 0.4 : hot ? 1.15 : 1)
      m.polaroid.scale.set(base, base * 0.95, 1)
      m.polaroidMat.opacity = levels.photo * (dim ? 0.2 : 1)
      m.polaroid.visible = levels.photo > 0.06
    }
  }
  raf = requestAnimationFrame(tick)
}

function markInteracted() { if (!interacted.value) { interacted.value = true; emit('interacted') } }

onMounted(() => {
  if (!mountEl.value) return
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  css2d = new CSS2DRenderer()
  css2d.setSize(mountEl.value.clientWidth, mountEl.value.clientHeight)
  css2d.domElement.style.position = 'absolute'
  css2d.domElement.style.top = '0'
  css2d.domElement.style.zIndex = '5'
  css2d.domElement.style.pointerEvents = 'none'
  mountEl.value.appendChild(css2d.domElement)

  graph = new (ForceGraph3D as any)(mountEl.value, { extraRenderers: [css2d] })
    .width(mountEl.value.clientWidth).height(mountEl.value.clientHeight)
    .backgroundColor('rgba(0,0,0,0)')
    .showNavInfo(false)
    .nodeThreeObject(nodeThreeObject)
    .nodeLabel(() => '')
    .linkColor((link: any) => {
      const active = hoveredId || activeId.value
      return !!active && (String((link as any).source?.id ?? (link as any).source) === active || String((link as any).target?.id ?? (link as any).target) === active)
        ? 'rgba(193,220,200,.9)' : 'rgba(143,173,152,.4)'
    })
    .linkWidth(0.7)
    .linkOpacity(0.6)
    .linkDirectionalParticles(3)
    .linkDirectionalParticleSpeed(0.006)
    .linkDirectionalParticleWidth(1.6)
    .enableNodeDrag(true)   // 拖节点 = 摆放星星；拖空白 = 旋转视角
    .onNodeDragEnd((node: GraphNode) => {
      node.fx = node.x; node.fy = node.y; node.fz = node.z
      announcement.value = '已将「' + (node.hub?.tag || node.work?.title || '星点') + '」安放至此'
      markInteracted()
    })
    .onNodeHover((node: GraphNode | null) => {
      hoveredId = node?.id ?? null
      if (mountEl.value) mountEl.value.style.cursor = node ? 'pointer' : 'grab'
    })
    .onNodeClick((node: GraphNode) => {
      markInteracted()
      if (node.hub) { emit('selectHub', node.hub.tag); focusSelection() }
      else if (node.work) emit('openWork', node.work)
    })
    .onEngineStop(() => {
      if (firstFit) {
        firstFit = false
        // 等入场叙事（1.7s）播完再飞入全景：让用户先看到 L1 纯光点宇宙
        window.setTimeout(() => fit(), reducedMotion ? 0 : 1800)
      }
    })
    .warmupTicks(160).cooldownTicks(0)
  graph.d3Force('charge')?.strength(-300)
  graph.d3Force('link')?.distance(85).strength(0.35)
  // 入场转场：相机从远处拉近
  graph.cameraPosition({ x: 1150, y: 540, z: 1650 }, { x: 0, y: 0, z: 0 }, 0)
  addStardust(graph.scene())
  updateData()
  raf = requestAnimationFrame(tick)
  const obs = new ResizeObserver(() => {
    if (!mountEl.value || !graph) return
    graph.width(mountEl.value.clientWidth).height(mountEl.value.clientHeight)
    css2d?.setSize(mountEl.value.clientWidth, mountEl.value.clientHeight)
  })
  obs.observe(mountEl.value)
  observers.push(obs)
})
const observers: ResizeObserver[] = []

watch(() => [props.hubs, props.works, props.edges], updateData)
watch(activeId, focusSelection)
watch(() => props.neighborsOnly, focusSelection)

onBeforeUnmount(() => {
  disposed = true
  cancelAnimationFrame(raf)
  observers.forEach((o) => o.disconnect())
  graph?._destructor()
  graph = null
  textures.forEach((t) => t.dispose())
  textures.length = 0
  hubMeta.clear(); leafMeta.clear()
  css2d?.domElement.remove()
})
defineExpose({ focusSelection })
</script>

<style scoped>
.graph-shell, .graph-canvas { width: 100%; height: 100%; }
.graph-shell { position: relative; }
.graph-canvas { cursor: grab; }
.graph-canvas:active { cursor: grabbing; }
.graph-controls { position: absolute; right: 12px; bottom: 8px; display: flex; align-items: center; gap: 3px; padding: 5px; border: 1px solid rgba(153,174,159,.25); border-radius: 10px; background: var(--bg); z-index: 2; }
.graph-controls button { display: grid; place-items: center; width: 34px; height: 34px; border: 0; border-radius: 5px; color: var(--accent-ink); background: none; cursor: pointer; }
.graph-controls button:hover { background: rgba(142,168,150,.15); color: var(--text-1); }
.graph-controls button:focus-visible { outline: 2px solid var(--accent-hover); }
.graph-controls svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.5; }
.graph-controls .text-control { width: auto; padding: 0 12px; font-size: 12px; }
.zoom-value { width: 42px; text-align: center; color: var(--accent-ink); font-size: 12px; font-variant-numeric: tabular-nums; }
.control-divider { height: 20px; width: 1px; background: rgba(153,174,159,.22); margin: 0 4px; }
.graph-legend { position: absolute; left: 12px; bottom: 25px; display: flex; gap: 9px; align-items: center; color: var(--accent-ink); font-size: 12px; pointer-events: none; z-index: 2; }
.graph-legend i { width: 11px; height: 11px; border: 1px solid var(--accent); border-radius: 50%; }
.graph-hint { position: absolute; left: 50%; bottom: 18px; transform: translateX(-50%); color: var(--text-3); font-size: 12px; white-space: nowrap; pointer-events: none; z-index: 2; transition: opacity .6s; }
.graph-hint.faded { opacity: .55; }
.graph-announcement { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
</style>

<!-- CSS2D 卡片样式：动态插入 DOM 不带 scoped 属性，需全局样式 -->
<style>
.cg-hub-card {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  padding: 7px 16px; border-radius: 10px;
  background: rgba(8, 12, 9, .72); border: 1px solid rgba(163, 184, 170, .3);
  backdrop-filter: blur(6px);
  pointer-events: auto; cursor: pointer; user-select: none;
  transition: opacity .3s, box-shadow .3s;
}
.cg-hub-card b { color: rgb(226, 235, 225); font: 600 15px/1.2 "Noto Serif SC", serif; letter-spacing: .06em; white-space: nowrap; }
.cg-hub-card span { color: rgb(163, 184, 169); font-size: 12px; }
.cg-hub-card.hot { border-color: rgba(193, 220, 200, .8); box-shadow: 0 0 26px rgba(140, 190, 160, .35); }
.cg-hub-card.dim { opacity: .45; }
</style>
