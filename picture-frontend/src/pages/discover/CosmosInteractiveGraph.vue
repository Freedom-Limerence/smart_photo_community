<template>
  <div class="graph-shell">
    <div ref="mountEl" class="graph-canvas" data-lenis-prevent role="img" aria-label="影像星图。初始显示题材光点，放大显现照片，点击照片进入详情。可拖动画布或节点。" />
    <div class="graph-controls" aria-label="图谱视野控制">
      <button type="button" aria-label="放大图谱" @click="zoomBy(1.25)"><svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14"/></svg></button>
      <span class="zoom-value">{{ zoomPercent }}%</span>
      <button type="button" aria-label="缩小图谱" @click="zoomBy(.8)"><svg viewBox="0 0 24 24"><path d="M5 12h14"/></svg></button>
      <span class="control-divider"></span>
      <button type="button" title="缩小并返回题材星图" aria-label="返回题材星图" @click="fit"><svg viewBox="0 0 24 24"><path d="M9 4H4v5m11-5h5v5M4 15v5h5m11-5v5h-5"/></svg></button>
      <button type="button" class="text-control" @click="resetLayout">重新布局</button>
    </div>
    <div class="graph-legend"><i></i>{{ zoomRatio < 1.3 ? '题材星点' : '影像已展开' }}</div>
    <p class="graph-hint">{{ zoomRatio < 1.3 ? '滚轮放大，发现星点间的影像 · 点击题材靠近' : '点击照片进入详情 · 缩小返回题材星图' }}</p>
    <p class="graph-announcement" role="status">{{ announcement }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import ForceGraph, { type LinkObject, type NodeObject } from 'force-graph'
import { forceCollide, forceY } from 'd3-force'

interface Hub { tag: string; count: number }
interface Work { workId: string; title: string; author: string; cover: string; heat: number; tags: string[] }
interface Edge { hub: string; leaf: string }
interface PhotoNode extends NodeObject { id: string; kind: 'hub' | 'work'; hub?: Hub; work?: Work; width: number; height: number }
interface PhotoLink extends LinkObject<PhotoNode> { source: string | PhotoNode; target: string | PhotoNode }
const props = defineProps<{ hubs: Hub[]; works: Work[]; edges: Edge[]; selectedTag: string | null; selectedWork: string | null; neighborsOnly: boolean }>()
const emit = defineEmits<{ selectHub: [tag: string]; openWork: [work: Work]; clear: [] }>()
const mountEl = ref<HTMLElement | null>(null)
const zoomPercent = ref(100)
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
const topicPairs = computed(() => {
  const tagsByWork = new Map<string, string[]>()
  for (const edge of props.edges) {
    if (!tagsByWork.has(edge.leaf)) tagsByWork.set(edge.leaf, [])
    tagsByWork.get(edge.leaf)!.push(edge.hub)
  }
  const pairs = new Map<string, [string, string]>()
  for (const tags of tagsByWork.values()) {
    for (let i = 0; i < tags.length; i++) for (let j = i + 1; j < tags.length; j++) {
      const pair: [string, string] = [tags[i]!, tags[j]!].sort() as [string, string]
      pairs.set(JSON.stringify(pair), pair)
    }
  }
  return [...pairs.values()]
})
let graph: ForceGraph<PhotoNode, PhotoLink> | null = null
let observer: ResizeObserver | null = null
let hovered: string | null = null
let firstFit = true
let disposed = false
const zoomRatio = ref(1)
let overviewScale = .9
function photoReveal(scale = graph?.zoom() || overviewScale) {
  const t = Math.max(0, Math.min(1, (scale / overviewScale - 1.25) / .65))
  return t * t * (3 - 2 * t)
}
const images = new Map<string, HTMLImageElement>()
const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const duration = reducedMotion ? 0 : 500
const idOf = (v: string | number | PhotoNode | undefined) => typeof v === 'object' ? v.id : String(v ?? '')
function visible(node: PhotoNode) { return !props.neighborsOnly || !activeId.value || !!neighbors.value.get(activeId.value)?.has(node.id) }
function isActiveLink(link: PhotoLink) {
  const active = hovered || activeId.value
  return !!active && (idOf(link.source) === active || idOf(link.target) === active)
}
function redraw() {
  graph?.nodeCanvasObject(paintNode)
  graph?.linkColor(link => `rgba(166,197,176,${photoReveal() * (isActiveLink(link) ? .8 : (hovered || activeId.value) ? .12 : .3)})`)
  graph?.linkWidth(link => isActiveLink(link) ? 1.7 : .8)
}
function paintNode(node: PhotoNode, ctx: CanvasRenderingContext2D, scale: number) {
  const x = node.x || 0, y = node.y || 0, active = hovered || activeId.value
  const related = active ? neighbors.value.get(active) : null
  const selected = node.id === activeId.value, hot = node.id === hovered || selected
  const dim = !!active && !related?.has(node.id)
  ctx.save()
  ctx.globalAlpha = dim ? .19 : 1
  if (node.kind === 'hub') {
    // Keep a star-sized marker and readable label at every zoom level.
    const radius = (hot ? 5 : 3.5) / scale
    ctx.beginPath(); ctx.arc(x, y, radius, 0, Math.PI * 2)
    ctx.fillStyle = hot ? 'rgb(235,243,224)' : 'rgb(172,198,181)'; ctx.fill()
    ctx.beginPath(); ctx.arc(x, y, (hot ? 15 : 10) / scale, 0, Math.PI * 2)
    ctx.strokeStyle = hot ? 'rgba(193,220,200,.7)' : 'rgba(145,170,153,.28)'
    ctx.lineWidth = 1 / scale; ctx.stroke()
    ctx.textAlign = 'center'; ctx.textBaseline = 'top'
    ctx.fillStyle = 'rgb(226,235,225)'; ctx.font = `500 ${18 / scale}px "Noto Serif SC", serif`
    ctx.fillText(node.hub?.tag || '', x, y + 22 / scale)
    ctx.fillStyle = 'rgb(163,184,169)'; ctx.font = `${12 / scale}px sans-serif`
    ctx.fillText((node.hub?.count || 0) + ' 幅', x, y + 49 / scale)
  } else {
    const reveal = photoReveal(scale)
    if (!reveal) { ctx.restore(); return }
    ctx.globalAlpha *= reveal
    const w = node.width, h = node.height
    ctx.shadowColor = 'rgba(0,0,0,.48)'; ctx.shadowBlur = 16; ctx.shadowOffsetY = 8
    ctx.fillStyle = 'rgb(27,32,29)'
    ctx.beginPath(); ctx.roundRect(x - w / 2 - 4, y - h / 2 - 4, w + 8, h + 8, 5); ctx.fill()
    ctx.shadowBlur = 0; ctx.shadowOffsetY = 0
    const image = images.get(node.work?.cover || '')
    ctx.save()
    ctx.beginPath(); ctx.roundRect(x - w / 2, y - h / 2, w, h, 3); ctx.clip()
    if (image?.complete && image.naturalWidth) {
      const ratio = Math.max(w / image.naturalWidth, h / image.naturalHeight)
      const sw = w / ratio, sh = h / ratio
      ctx.drawImage(image, (image.naturalWidth - sw) / 2, (image.naturalHeight - sh) / 2, sw, sh, x - w / 2, y - h / 2, w, h)
    } else {
      ctx.fillStyle = 'rgb(54,68,58)'; ctx.fillRect(x - w / 2, y - h / 2, w, h)
      ctx.fillStyle = 'rgb(191,207,196)'; ctx.font = '12px sans-serif'; ctx.textAlign = 'center'; ctx.fillText('影像待呈现', x, y)
    }
    ctx.restore()
    if (hot) {
      ctx.strokeStyle = 'rgb(193,220,200)'; ctx.lineWidth = 2 / scale
      ctx.strokeRect(x - w / 2 - 5, y - h / 2 - 5, w + 10, h + 10)
    }
    if (photoReveal(scale) > .8 || hot) {
      const title = node.work?.title || '未命名作品'
      ctx.font = '12px sans-serif'; ctx.textAlign = 'center'; ctx.textBaseline = 'top'
      ctx.fillStyle = 'rgb(215,224,216)'
      ctx.fillText(title.length > 12 ? title.slice(0, 11) + '…' : title, x, y + h / 2 + 12, w + 30)
    }
  }
  ctx.restore()
}
function paintPointer(node: PhotoNode, color: string, ctx: CanvasRenderingContext2D) {
  if (node.kind === 'work' && photoReveal() < .55) return
  ctx.fillStyle = color
  if (node.kind === 'hub') { ctx.beginPath(); ctx.arc(node.x || 0, node.y || 0, 20 / (graph?.zoom() || 1), 0, Math.PI * 2); ctx.fill() }
  else ctx.fillRect((node.x || 0) - node.width / 2 - 5, (node.y || 0) - node.height / 2 - 5, node.width + 10, node.height + 10)
}
function photoDimensions(image?: HTMLImageElement) {
  if (!image?.complete || !image.naturalWidth || !image.naturalHeight) return { width: 126, height: 98 }
  return image.naturalWidth / image.naturalHeight < .95
    ? { width: 104, height: 133 }
    : { width: 146, height: 102 }
}
function updateData() {
  if (!graph) return
  const hubs: PhotoNode[] = props.hubs.map((hub, i) => {
    const angle = (i - 1) * Math.PI * 2 / Math.max(1, props.hubs.length - 1) - Math.PI / 2
    const x = i === 0 ? -70 : Math.cos(angle) * 470, y = i === 0 ? 0 : Math.sin(angle) * 175
    return { id: 'tag:' + hub.tag, kind: 'hub', hub, x, y, fx: x, fy: y, width: i === 0 ? 108 : 90, height: 90 }
  })
  const hubMap = new Map(hubs.map(n => [n.hub!.tag, n]))
  const works: PhotoNode[] = props.works.map((work, i) => {
    const connected = props.edges.filter(e => e.leaf === work.workId).map(e => hubMap.get(e.hub)).filter((n): n is PhotoNode => !!n)
    const x = connected.reduce((sum, n) => sum + n.x!, 0) / Math.max(1, connected.length)
    const y = connected.reduce((sum, n) => sum + n.y!, 0) / Math.max(1, connected.length)
    return { id: 'work:' + work.workId, kind: 'work', work, x: x + Math.cos(i * 2.4) * 120, y: y + Math.sin(i * 2.4) * 120, ...photoDimensions(images.get(work.cover)) }
  })
  for (const work of props.works) {
    if (!work.cover || images.has(work.cover)) continue
    const image = new Image()
    image.onload = () => {
      if (disposed) return
      for (const node of graph?.graphData().nodes ?? []) {
        if (node.work?.cover === work.cover) Object.assign(node, photoDimensions(image))
      }
      redraw()
    }
    image.onerror = () => { if (!disposed) redraw() }
    images.set(work.cover, image); image.src = work.cover
  }
  firstFit = true
  graph.graphData({ nodes: [...hubs, ...works], links: props.edges.map(e => ({ source: 'tag:' + e.hub, target: 'work:' + e.leaf })) })
  graph.d3Force('charge')?.strength(-180)
  graph.d3Force('link')?.distance(140).strength(.25)
  graph.d3Force('vertical', forceY<PhotoNode>(0).strength(.12))
  graph.d3Force('collide', forceCollide<PhotoNode>(n => n.kind === 'hub' ? n.width / 2 + 17 : 86).iterations(3))
  graph.d3ReheatSimulation()
}
function fit() {
  if (!graph || !mountEl.value) return
  const hubs = graph.graphData().nodes.filter(n => n.kind === 'hub')
  const xs = hubs.map(n => n.x || 0), ys = hubs.map(n => n.y || 0)
  const left = Math.min(0, ...xs), right = Math.max(0, ...xs)
  const top = Math.min(0, ...ys), bottom = Math.max(0, ...ys)
  overviewScale = Math.max(.25, Math.min(1, (mountEl.value.clientWidth - 200) / Math.max(1, right - left), (mountEl.value.clientHeight - 240) / Math.max(1, bottom - top)))
  emit('clear')
  graph.centerAt((left + right) / 2, (top + bottom) / 2, duration)
  graph.zoom(overviewScale, duration)
}
function paintTopicLinks(ctx: CanvasRenderingContext2D, scale: number) {
  const alpha = 1 - photoReveal(scale)
  if (alpha <= 0 || !graph) return
  const nodes = new Map(graph.graphData().nodes.map(n => [n.id, n]))
  ctx.save(); ctx.strokeStyle = `rgba(143,173,152,${alpha * .24})`; ctx.lineWidth = .7 / scale
  for (const [source, target] of topicPairs.value) {
    const a = nodes.get('tag:' + source), b = nodes.get('tag:' + target)
    if (!a || !b || !visible(a) || !visible(b)) continue
    ctx.beginPath(); ctx.moveTo(a.x || 0, a.y || 0); ctx.lineTo(b.x || 0, b.y || 0); ctx.stroke()
  }
  ctx.restore()
}
function resetLayout() { emit('clear'); updateData(); announcement.value = '已恢复星图布局' }
function zoomBy(factor: number) { if (graph) graph.zoom(Math.max(.3, Math.min(3.5, graph.zoom() * factor)), duration / 2) }
function focusSelection() {
  if (activeId.value) firstFit = false
  redraw()
  graph?.nodeVisibility(visible).linkVisibility(link => {
    if (!props.neighborsOnly || !activeId.value) return true
    return idOf(link.source) === activeId.value || idOf(link.target) === activeId.value
  })
  const node = graph?.graphData().nodes.find(n => n.id === activeId.value)
  if (node && graph) {
    graph.centerAt(node.x, node.y, duration)
    graph.zoom(overviewScale * 1.95, duration)
  }
}
onMounted(() => {
  if (!mountEl.value) return
  graph = new ForceGraph<PhotoNode, PhotoLink>(mountEl.value)
    .width(mountEl.value.clientWidth).height(mountEl.value.clientHeight).backgroundColor('rgba(0,0,0,0)')
    .nodeCanvasObject(paintNode).nodePointerAreaPaint(paintPointer).nodeLabel(() => '')
    .linkCurvature(.06).onRenderFramePre(paintTopicLinks)
    .onNodeHover(node => { hovered = node?.id || null; if (mountEl.value) mountEl.value.style.cursor = node ? 'pointer' : 'grab'; redraw() })
    .onNodeClick(node => {
      if (node.hub) { emit('selectHub', node.hub.tag); graph?.centerAt(node.x, node.y, duration); graph?.zoom(overviewScale * 1.95, duration) }
      else if (node.work && photoReveal() >= .55) emit('openWork', node.work)
    })
    .onNodeDragEnd(node => { node.fx = node.x; node.fy = node.y; announcement.value = '已移动并固定「' + (node.hub?.tag || node.work?.title) + '」' })
    .onBackgroundClick(() => emit('clear'))
    .onZoom(({ k, x, y }) => {
      zoomRatio.value = k / overviewScale
      if (hovered?.startsWith('work:') && photoReveal(k) < .55) hovered = null
      redraw()
      zoomPercent.value = Math.round(k / overviewScale * 100)
      if (mountEl.value) { mountEl.value.dataset.zoom = String(k); mountEl.value.dataset.level = photoReveal(k) > .55 ? 'photos' : 'topics'; mountEl.value.dataset.pan = Math.round(x) + ',' + Math.round(y) }
    })
    .onEngineStop(() => { if (firstFit) { firstFit = false; if (activeId.value) focusSelection(); else fit() } })
    .warmupTicks(120).cooldownTicks(0).minZoom(.3).maxZoom(3.5)
  graph.zoom(overviewScale, 0)
  updateData(); redraw()
  observer = new ResizeObserver(() => { if (mountEl.value && graph) graph.width(mountEl.value.clientWidth).height(mountEl.value.clientHeight) })
  observer.observe(mountEl.value)
})
watch(() => [props.hubs, props.works, props.edges], updateData)
defineExpose({ focusSelection })
watch(activeId, focusSelection)
watch(() => props.neighborsOnly, focusSelection)
onBeforeUnmount(() => { disposed = true; observer?.disconnect(); graph?._destructor(); graph = null; images.clear() })
</script>

<style scoped>
.graph-shell, .graph-canvas { width: 100%; height: 100%; }
.graph-shell { position: relative; }
.graph-canvas { cursor: grab; }
.graph-canvas:active { cursor: grabbing; }
.graph-controls { position: absolute; right: 12px; bottom: 8px; display: flex; align-items: center; gap: 3px; padding: 5px; border: 1px solid rgba(153,174,159,.25); border-radius: 10px; background: var(--bg); }
.graph-controls button { display: grid; place-items: center; width: 34px; height: 34px; border: 0; border-radius: 5px; color: var(--accent-ink); background: none; cursor: pointer; }
.graph-controls button:hover { background: rgba(142,168,150,.15); color: var(--text-1); }
.graph-controls button:focus-visible { outline: 2px solid var(--accent-hover); }
.graph-controls svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-width: 1.5; }
.graph-controls .text-control { width: auto; padding: 0 12px; font-size: 12px; }
.zoom-value { width: 42px; text-align: center; color: var(--accent-ink); font-size: 12px; font-variant-numeric: tabular-nums; }
.control-divider { height: 20px; width: 1px; background: rgba(153,174,159,.22); margin: 0 4px; }
.graph-legend { position: absolute; left: 12px; bottom: 25px; display: flex; gap: 9px; align-items: center; color: var(--accent-ink); font-size: 12px; pointer-events: none; }
.graph-legend i { width: 11px; height: 11px; border: 1px solid var(--accent); border-radius: 50%; }
.graph-legend b { width: 12px; height: 9px; background: var(--accent); margin-left: 14px; }
.graph-legend span { width: 24px; height: 1px; background: var(--accent); margin-left: 14px; }
.graph-hint { position: absolute; left: 50%; bottom: 18px; transform: translateX(-50%); color: var(--text-3); font-size: 12px; white-space: nowrap; pointer-events: none; }
.graph-announcement { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
</style>

