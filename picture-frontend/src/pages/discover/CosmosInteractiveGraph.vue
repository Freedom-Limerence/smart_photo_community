<template>
  <div class="graph-shell">
    <div ref="mountEl" class="graph-canvas" data-lenis-prevent role="img" aria-label="影像星图。初始显示题材光点，放大显现照片，点击照片进入详情。拖拽旋转视角，滚轮缩放。" />
    <div class="graph-controls" aria-label="图谱视野控制">
      <button type="button" aria-label="推近星图" @click="zoomBy(0.8)"><svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg></button>
      <span class="zoom-value">{{ zoomPercent }}%</span>
      <button type="button" aria-label="拉远星图" @click="zoomBy(1.25)"><svg viewBox="0 0 24 24"><path d="M5 12h14"/></svg></button>
      <span class="control-divider"></span>
      <button type="button" title="回到全景" aria-label="回到全景" @click="fit"><svg viewBox="0 0 24 24"><path d="M9 4H4v5m11-5h5v5M4 15v5h5m11-5v5h-5"/></svg></button>
      <button type="button" class="text-control" @click="resetLayout">重新布局</button>
    </div>
    <div class="graph-legend"><i></i>{{ revealRatio < 0.4 ? '题材星点' : '影像已展开' }}</div>
    <p class="graph-hint">{{ revealRatio < 0.4 ? '滚轮推近星团，展开其中的影像 · 点击题材星点进入标签页' : '点击影像进入灯箱 · 拉远返回题材星座' }}</p>
    <p class="graph-announcement" role="status">{{ announcement }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import ForceGraph3D from '3d-force-graph'
import * as THREE from 'three'

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
  __threeObj?: THREE.Group
}

const props = defineProps<{ hubs: Hub[]; works: Work[]; edges: Edge[]; selectedTag: string | null; selectedWork: string | null; neighborsOnly: boolean }>()
const emit = defineEmits<{ selectHub: [tag: string]; openWork: [work: Work]; clear: [] }>()

const mountEl = ref<HTMLElement | null>(null)
const zoomPercent = ref(100)
const revealRatio = ref(0)
const announcement = ref('')
const activeId = computed(() => props.selectedTag ? 'tag:' + props.selectedTag : props.selectedWork ? 'work:' + props.selectedWork : null)
const neighbors = computed(() => {
  const map = new Map<string, Set<string>>()
  for (const edge of props.edges) {
    const a = 'tag:' + edge.hub, b = 'work:' + edge.leaf
    if (!map.has(a)) map.set(a, new Set([a]))
    if (!map.has(b)) map.set(b, new Set([b]))
    map.get(a)!.add(b); map.get(b)!.add(a)
  }
  return map
})

let graph: any = null
let disposed = false
let firstFit = true
let hoveredId: string | null = null
let reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches

/** LOD：相机距离 → leaf 展开度（0=题材星座，1=作品展开） */
const DIST_FAR = 620, DIST_NEAR = 200
function photoReveal() {
  if (!graph) return 0
  const cam = graph.cameraPosition()
  const d = Math.hypot(cam.x, cam.y, cam.z)
  const t = Math.max(0, Math.min(1, (DIST_FAR - d) / (DIST_FAR - DIST_NEAR)))
  return t * t * (3 - 2 * t)
}

// ---- 程序化纹理（本地 canvas，无跨域）----
const textures: THREE.Texture[] = []
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
function labelTexture(title: string, sub: string): THREE.Texture {
  const c = document.createElement('canvas')
  c.width = 512; c.height = 160
  const ctx = c.getContext('2d')!
  ctx.textAlign = 'center'
  ctx.shadowColor = 'rgba(0,0,0,.9)'; ctx.shadowBlur = 10
  ctx.fillStyle = 'rgb(226,235,225)'
  ctx.font = '600 56px "Noto Serif SC", serif'
  ctx.fillText(title, 256, 66)
  ctx.fillStyle = 'rgb(163,184,169)'
  ctx.font = '30px sans-serif'
  ctx.fillText(sub, 256, 128)
  const tex = new THREE.CanvasTexture(c)
  textures.push(tex)
  return tex
}

const HUB_PALETTE = [0x8fd8f0, 0xf0c987, 0xb8a7e8, 0xf0a7b8, 0xa7e8c9, 0xf0d8a7, 0xc9b8f0, 0xa7d8c9]
const hubMeta = new Map<string, any>()
const leafMeta = new Map<string, any>()
let thumbLoader: THREE.TextureLoader | null = null

function makeHubObject(node: GraphNode): THREE.Group {
  const hub = node.hub!
  const i = props.hubs.findIndex((h) => h.tag === hub.tag)
  const color = '#' + HUB_PALETTE[i % HUB_PALETTE.length].toString(16).padStart(6, '0')
  const group = new THREE.Group()
  const glow = new THREE.Sprite(new THREE.SpriteMaterial({
    map: glowTexture(color), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0.95,
  }))
  const s = 16 + Math.min(14, hub.count * 3)
  glow.scale.set(s, s, 1)
  group.add(glow)
  const label = new THREE.Sprite(new THREE.SpriteMaterial({
    map: labelTexture(hub.tag, hub.count + ' 幅'), transparent: true, depthWrite: false, opacity: 0.92,
  }))
  label.scale.set(22, 7, 1)
  label.position.set(0, s * 0.62 + 2.4, 0)
  group.add(label)
  hubMeta.set(node.id, { color: new THREE.Color(color), glow, label, phase: i * 1.7 })
  return group
}

function makeLeafObject(node: GraphNode): THREE.Group {
  const work = node.work!
  const hubColor = hubMeta.get('tag:' + work.hub)?.color ?? new THREE.Color(0x8fd8f0)
  const color = '#' + hubColor.getHexString()
  const group = new THREE.Group()
  const glow = new THREE.Sprite(new THREE.SpriteMaterial({
    map: glowTexture(color), transparent: true, depthWrite: false, blending: THREE.AdditiveBlending, opacity: 0.8,
  }))
  glow.scale.set(9, 9, 1)
  group.add(glow)
  const meta = { color: hubColor, glow, photo: null as THREE.Mesh | null, phase: Math.random() * Math.PI * 2 }
  // 缩略图面片：/cos 同源反代无跨域污染
  if (work.cover) {
    const loader = thumbLoader ??= new THREE.TextureLoader()
    loader.load(work.cover, (tex) => {
      if (disposed || !node.__threeObj) { tex.dispose(); return }
      textures.push(tex)
      const ratio = tex.image.width / tex.image.height
      const w = ratio < 0.95 ? 7 : 10.5, h = w / ratio
      const mesh = new THREE.Mesh(
        new THREE.PlaneGeometry(w, h),
        new THREE.MeshBasicMaterial({ map: tex, transparent: true, opacity: 0, side: THREE.DoubleSide }),
      )
      meta.photo = mesh
      node.__threeObj.add(mesh)
    }, undefined, () => { /* 加载失败保持光点 */ })
  }
  leafMeta.set(node.id, meta)
  return group
}

function nodeThreeObject(node: GraphNode) {
  return node.kind === 'hub' ? makeHubObject(node) : makeLeafObject(node)
}

function isActiveLink(link: any) {
  const active = hoveredId || activeId.value
  return !!active && (String(link.source?.id ?? link.source) === active || String(link.target?.id ?? link.target) === active)
}

function updateData() {
  if (!graph) return
  hubMeta.clear(); leafMeta.clear()
  const hubs: GraphNode[] = props.hubs.map((hub) => ({ id: 'tag:' + hub.tag, kind: 'hub' as const, hub }))
  const works: GraphNode[] = props.works.map((work) => ({ id: 'work:' + work.workId, kind: 'work' as const, work }))
  firstFit = true
  graph.graphData({
    nodes: [...hubs, ...works],
    links: props.edges.map((e) => ({ source: 'tag:' + e.hub, target: 'work:' + e.leaf })),
  })
  graph.d3Force('charge')?.strength(-320)
  graph.d3Force('link')?.distance(90).strength(0.4)
}

function fit() {
  if (!graph) return
  const hubs = graph.graphData().nodes.filter((nn: any) => nn.kind === 'hub')
  const box = new THREE.Box3().setFromPoints(hubs.map((nn: any) => new THREE.Vector3(nn.x ?? 0, nn.y ?? 0, nn.z ?? 0)))
  const center = box.getCenter(new THREE.Vector3())
  const size = box.getSize(new THREE.Vector3()).length()
  emit('clear')
  const d = Math.max(320, size * 1.15)
  graph.cameraPosition(
    { x: center.x + d * 0.4, y: center.y + d * 0.24, z: center.z + d * 0.9 },
    center, reducedMotion ? 0 : 900,
  )
}
function resetLayout() { emit('clear'); updateData(); announcement.value = '已恢复星图布局' }
function zoomBy(factor: number) {
  if (!graph) return
  const cam = graph.cameraPosition()
  const f = Math.max(0.25, Math.min(4, factor))
  graph.cameraPosition(
    { x: cam.x * f, y: cam.y * f, z: cam.z * f },
    { x: 0, y: 0, z: 0 }, reducedMotion ? 0 : 400,
  )
}
function focusSelection() {
  if (!activeId.value || !graph) return
  const node: GraphNode | undefined = graph.graphData().nodes.find((nn: GraphNode) => nn.id === activeId.value)
  if (!node) return
  const d = 260
  graph.cameraPosition(
    { x: (node.x ?? 0) + d * 0.3, y: (node.y ?? 0) + d * 0.22, z: (node.z ?? 0) + d * 0.85 },
    { x: node.x ?? 0, y: node.y ?? 0, z: node.z ?? 0 }, reducedMotion ? 0 : 900,
  )
}

/** 每帧：呼吸 + LOD 渐显 + 高亮压暗 */
let raf = 0
function tick() {
  if (disposed || !graph) return
  const t = performance.now() / 1000
  const reveal = photoReveal()
  revealRatio.value = reveal
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
      const base = 16 + Math.min(14, (node.hub?.count ?? 0) * 3)
      meta.glow.scale.setScalar(base * pulse * (dim ? 0.55 : hot ? 1.3 : 1))
      ;(meta.glow.material as THREE.SpriteMaterial).opacity = dim ? 0.3 : 0.95
      ;(meta.label.material as THREE.SpriteMaterial).opacity = dim ? 0.25 : 0.92
    } else {
      meta.glow.scale.setScalar(9 * pulse * (0.55 + reveal * 0.55) * (dim ? 0.35 : hot ? 1.45 : 1))
      ;(meta.glow.material as THREE.SpriteMaterial).opacity = (0.22 + reveal * 0.65) * (dim ? 0.25 : 1)
    }
    if (!isHub && meta.photo) {
      const m = meta.photo.material as THREE.MeshBasicMaterial
      m.opacity = reveal * (dim ? 0.2 : 1)
      meta.photo.visible = reveal > 0.1
      if (meta.photo.visible) meta.photo.lookAt(graph.cameraPosition().x, graph.cameraPosition().y, graph.cameraPosition().z)
    }
  }
  raf = requestAnimationFrame(tick)
}

onMounted(() => {
  if (!mountEl.value) return
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  graph = new (ForceGraph3D as any)(mountEl.value)
    .width(mountEl.value.clientWidth).height(mountEl.value.clientHeight)
    .backgroundColor('rgba(0,0,0,0)')
    .showNavInfo(false)
    .nodeThreeObject(nodeThreeObject)
    .nodeLabel(() => '')
    .linkColor((link: any) => isActiveLink(link) ? 'rgba(193,220,200,.9)' : 'rgba(143,173,152,.35)')
    .linkWidth((link: any) => isActiveLink(link) ? 1.8 : 0.6)
    .linkOpacity(0.55)
    .linkDirectionalParticles(3)
    .linkDirectionalParticleSpeed(0.006)
    .linkDirectionalParticleWidth(1.5)
    .enableNodeDrag(false)   // 拖拽 = 旋转视角（17 的核心要求）
    .onNodeHover((node: GraphNode | null) => {
      hoveredId = node?.id ?? null
      if (mountEl.value) mountEl.value.style.cursor = node ? 'pointer' : 'grab'
    })
    .onNodeClick((node: GraphNode) => {
      if (node.hub) { emit('selectHub', node.hub.tag); focusSelection() }
      else if (node.work) emit('openWork', node.work)
    })
    .onBackgroundClick(() => emit('clear'))
    .onEngineStop(() => { if (firstFit) { firstFit = false; fit() } })
    .warmupTicks(160).cooldownTicks(0)
  graph.d3Force('charge')?.strength(-320)
  graph.d3Force('link')?.distance(90).strength(0.4)
  // 入场转场：相机从远处拉近
  graph.cameraPosition({ x: 1100, y: 520, z: 1600 }, { x: 0, y: 0, z: 0 }, 0)
  updateData()
  raf = requestAnimationFrame(tick)
  const obs = new ResizeObserver(() => { if (mountEl.value && graph) graph.width(mountEl.value.clientWidth).height(mountEl.value.clientHeight) })
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
.graph-hint { position: absolute; left: 50%; bottom: 18px; transform: translateX(-50%); color: var(--text-3); font-size: 12px; white-space: nowrap; pointer-events: none; z-index: 2; }
.graph-announcement { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
</style>
