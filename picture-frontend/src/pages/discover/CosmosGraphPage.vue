<template>
  <section class="constellation-page" aria-label="影像星座">
    <header class="constellation-top">
      <router-link class="constellation-brand" to="/discover">光影集 <span>/</span> 影像星座</router-link>
      <nav class="constellation-nav" aria-label="星座导航">
        <router-link to="/discover">← 返回发现</router-link>
        <router-link to="/community">浏览所有作品</router-link>
      </nav>
      <span v-if="!loading && !error && hubs.length" class="constellation-count">
        {{ hubs.length }} 个题材 <span>/</span> {{ leaves.length }} 幅作品
      </span>
    </header>

    <div class="constellation-scene">
      <div v-if="loading" class="scene-status" role="status" aria-live="polite">
        <span class="status-mark" aria-hidden="true"></span>
        <p>正在寻找影像之间的线索…</p>
      </div>
      <div v-else-if="error" class="scene-status" role="alert">
        <h1>星座暂时无法呈现</h1>
        <p>{{ error }}</p>
        <button type="button" @click="loadGraph">重新加载</button>
      </div>
      <div v-else-if="!hubs.length" class="scene-status">
        <h1>第一组影像，正在等待被发现</h1>
        <p>公开作品有了题材标签后，属于它们的星座会出现在这里。</p>
        <router-link to="/discover">去发现作品 ↗</router-link>
      </div>

      <div v-else class="constellation-overview">
          <div class="overview-intro">
            <h1>影像星图<span>从一个星点，走近一幅影像。</span></h1>
            <p>{{ hubs.length }} 个题材 · {{ leaves.length }} 幅作品 · {{ edges.length }} 条真实关联</p>
          </div>
          <div class="graph-search">
            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg>
            <input v-model="searchQuery" aria-label="搜索题材、作品或摄影师" placeholder="寻找题材、作品或摄影师" @keydown.escape="searchQuery = ''" />
            <div v-if="searchQuery.trim()" class="search-results">
              <button v-for="result in searchResults" :key="result.id" type="button" @click="chooseSearchResult(result)">
                <span>{{ result.label }}</span><small>{{ result.kind === 'hub' ? '题材' : '作品' }}</small>
              </button>
              <p v-if="!searchResults.length">没有找到相关影像，试试其他关键词。</p>
            </div>
          </div>

          <div class="constellation-field" aria-label="题材星座">
            <CosmosInteractiveGraph ref="graphRef" :hubs="hubs" :works="leaves" :edges="edges" :selected-tag="inspectedHub" :selected-work="inspectedWork?.workId || null" :neighbors-only="false" @select-hub="inspectHub" @open-work="openWork" @clear="clearInspection" />
          </div>

          <button v-if="inspectedWork" class="selected-work-link" type="button" @click="openWork(inspectedWork)">查看「{{ inspectedWork.title }}」详情</button>
          <div class="theme-index">
            <span class="theme-index-label">探索题材</span>
            <button v-for="hub in hubs" :key="hub.tag" type="button" :aria-pressed="inspectedHub === hub.tag" :class="{ active: inspectedHub === hub.tag }" @click="inspectHub(hub.tag)">
              {{ hub.tag }} <span>{{ hub.count }}</span>
            </button>
          </div>
        </div>
    </div>

  </section>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCosmosGraph } from '@/api/photoCommunity'
import CosmosInteractiveGraph from './CosmosInteractiveGraph.vue'
import { isPreviewResource, loadPhotographyPreview } from '@/utils/photographyPreview'

interface Hub { tag: string; count: number }
interface Work { workId: string; title: string; author: string; cover: string; heat: number; tags: string[] }
interface Edge { hub: string; leaf: string }
interface Graph { hubs: Hub[]; leaves: Work[]; edges: Edge[] }

const graphRef = ref<InstanceType<typeof CosmosInteractiveGraph> | null>(null)
const route = useRoute()
const router = useRouter()
const hubs = ref<Hub[]>([])
const leaves = ref<Work[]>([])
const edges = ref<Edge[]>([])
const loading = ref(true)
const error = ref('')
const inspectedHub = ref<string | null>(null)
const inspectedWork = ref<Work | null>(null)
const searchQuery = ref('')
let requestVersion = 0

interface SearchResult { id: string; kind: 'hub' | 'work'; label: string; work?: Work }
const searchResults = computed<SearchResult[]>(() => {
  const q = searchQuery.value.trim().toLocaleLowerCase()
  if (!q) return []
  return [
    ...hubs.value.filter(h => h.tag.toLocaleLowerCase().includes(q)).map(h => ({ id: 'tag:' + h.tag, kind: 'hub' as const, label: h.tag })),
    ...leaves.value.filter(w => (w.title + ' ' + w.author).toLocaleLowerCase().includes(q)).map(w => ({ id: 'work:' + w.workId, kind: 'work' as const, label: w.title || '未命名作品', work: w })),
  ].slice(0, 8)
})
function chooseSearchResult(result: SearchResult) { if (result.work) inspectWork(result.work); else inspectHub(result.label); searchQuery.value = '' }
function inspectHub(tag: string) {
  if (inspectedHub.value === tag) graphRef.value?.focusSelection()
  inspectedHub.value = tag || null; inspectedWork.value = null
}
function inspectWork(work: Work) {
  if (inspectedWork.value?.workId === work.workId) graphRef.value?.focusSelection()
  inspectedWork.value = work; inspectedHub.value = null
}
function clearInspection() { inspectedHub.value = null; inspectedWork.value = null }
function openWork(work: Work) {
  void router.push({ path: '/works/' + encodeURIComponent(work.workId), query: isPreviewResource(work.workId) ? { demo: 'photography' } : {} })
}
function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') { clearInspection(); searchQuery.value = '' }
}

async function loadGraph() {
  const version = ++requestVersion
  loading.value = true
  error.value = ''
  try {
    const preview = await loadPhotographyPreview()
    let graph: Graph
    if (preview) graph = preview.demoCosmosGraph
    else {
      const response: any = await getCosmosGraph(300)
      if (response.data.code !== 0) throw new Error(response.data.message || '作品暂时无法加载。')
      graph = response.data.data
    }
    if (version !== requestVersion) return
    leaves.value = Array.isArray(graph?.leaves) ? graph.leaves.filter((work) => work?.workId) : []
    const leafIds = new Set(leaves.value.map((work) => work.workId))
    const incomingHubs = Array.isArray(graph?.hubs) ? graph.hubs : []
    const knownTags = new Set(incomingHubs.map((hub) => hub.tag))
    const edgeMap = new Map<string, Edge>()
    const addEdge = (hub: string, leaf: string) => {
      if (knownTags.has(hub) && leafIds.has(leaf)) edgeMap.set(`${hub}:${leaf}`, { hub, leaf })
    }
    if (Array.isArray(graph?.edges)) {
      for (const edge of graph.edges) if (edge?.hub && edge?.leaf) addEdge(edge.hub, edge.leaf)
    }
    // 旧服务只返回一图一边；作品标签仍可还原真实的跨题材关系。
    for (const work of leaves.value) {
      if (Array.isArray(work.tags)) for (const tag of work.tags) addEdge(tag, work.workId)
    }
    edges.value = [...edgeMap.values()]
    const connectedTags = new Set(edges.value.map((edge) => edge.hub))
    hubs.value = incomingHubs.filter((hub) => hub?.tag && connectedTags.has(hub.tag))
    const requestedTheme = typeof route.query.theme === 'string' ? route.query.theme : null
    inspectedHub.value = hubs.value.some((hub) => hub.tag === requestedTheme) ? requestedTheme : null
  } catch (cause: any) {
    if (version === requestVersion) error.value = cause?.message || '请检查网络连接后重试。'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

watch(() => route.query.demo, () => { void loadGraph() })
watch(() => route.query.theme, (theme) => {
  inspectedHub.value = typeof theme === 'string' && hubs.value.some((hub) => hub.tag === theme) ? theme : null
})
onMounted(() => { void loadGraph(); window.addEventListener('keydown', onKeydown) })
onBeforeUnmount(() => { requestVersion++; window.removeEventListener('keydown', onKeydown) })
</script>

<style scoped>

.constellation-page { position: relative; min-height: 100svh; background: var(--bg); color: var(--text-1); }
.constellation-top { display: flex; align-items: center; justify-content: space-between; height: 64px; padding: 0 36px; border-bottom: 1px solid rgba(165,185,172,.12); }
.constellation-brand { color: var(--text-1); font-size: 15px; letter-spacing: .08em; font-weight: 600; text-decoration: none; }
.constellation-brand span { margin: 0 14px; color: var(--accent); }
.constellation-nav { display: flex; gap: 30px; }
.constellation-nav a { color: var(--accent-ink); font-size: 12px; text-decoration: none; }
.constellation-nav a:hover { color: var(--text-1); }
.constellation-count { color: var(--text-3); font-size: 12px; }
.constellation-count span { margin: 0 12px; }
.constellation-page button { font: inherit; }
.constellation-page button:focus-visible, .constellation-page a:focus-visible, .constellation-page input:focus-visible { outline: 2px solid var(--accent-hover); outline-offset: 4px; }
.constellation-scene { position: relative; min-height: max(640px, calc(100svh - 64px)); background: radial-gradient(ellipse at 48% 42%, color-mix(in srgb, var(--accent) 11%, var(--bg)), var(--bg) 68%); }
.constellation-overview { position: relative; height: max(640px, calc(100svh - 64px)); overflow: hidden; }
.overview-intro { position: absolute; z-index: 2; top: 28px; left: 36px; pointer-events: none; }
.overview-intro h1 { display: flex; align-items: baseline; gap: 23px; margin: 0; font: 400 34px/1.3 var(--font-display, serif); }
.overview-intro h1 span { color: var(--accent-ink); font: 400 13px/1.4 var(--font-sans, sans-serif); }
.overview-intro p { margin: 10px 0 0; color: var(--text-3); font-size: 12px; }
.constellation-field { position: absolute; inset: 108px 24px 80px; }
.graph-search { position: absolute; z-index: 6; top: 30px; right: 36px; width: 310px; }
.graph-search > svg { position: absolute; top: 13px; left: 13px; width: 17px; height: 17px; stroke: var(--accent-ink); fill: none; stroke-width: 1.5; pointer-events: none; }
.graph-search input { width: 100%; height: 44px; padding: 0 14px 0 40px; border: 1px solid rgba(163,184,170,.23); border-radius: 8px; color: var(--text-1); background: color-mix(in srgb, var(--bg) 94%, var(--accent)); font-size: 13px; caret-color: var(--accent-hover); }
.graph-search input::placeholder { color: var(--text-3); }
.search-results { margin-top: 7px; padding: 6px; border: 1px solid rgba(163,184,170,.25); border-radius: 8px; background: var(--bg); box-shadow: 0 14px 35px rgba(0,0,0,.45); }
.search-results button { display: flex; justify-content: space-between; align-items: center; gap: 16px; width: 100%; padding: 12px; border: 0; border-radius: 5px; color: var(--text-1); background: none; text-align: left; cursor: pointer; }
.search-results button:hover { background: rgba(154,180,161,.13); }
.search-results small { color: var(--accent-ink); font-size: 12px; }
.search-results p { padding: 14px; font-size: 13px; color: var(--accent-ink); }
.selected-work-link { position: absolute; z-index: 3; top: 88px; right: 36px; padding: 8px 0; border: 0; border-bottom: 1px solid var(--accent); background: var(--bg); color: var(--accent-ink); font-size: 13px; cursor: pointer; }
.theme-index { position: absolute; z-index: 3; bottom: 19px; left: 36px; right: 36px; display: flex; gap: 7px; align-items: center; overflow-x: auto; scrollbar-width: thin; scrollbar-color: var(--accent) transparent; }
.theme-index-label { color: var(--text-3); font-size: 12px; white-space: nowrap; margin-right: 12px; }
.theme-index button { display: inline-flex; align-items: center; gap: 9px; flex: 0 0 auto; padding: 9px 14px; border: 1px solid rgba(164,188,173,.2); border-radius: 24px; color: var(--accent-ink); background: rgba(24,32,27,.6); font-size: 12px; cursor: pointer; }
.theme-index button span { color: var(--text-3); }
.theme-index button:hover, .theme-index button.active { background: rgba(120,150,131,.2); border-color: var(--accent); color: var(--text-1); }
.scene-status { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 28px; text-align: center; }
.scene-status h1 { margin: 0 0 12px; font: 400 clamp(30px, 4vw, 54px)/1.3 var(--font-display, serif); }
.scene-status p { max-width: 38ch; color: var(--accent-ink); font-size: 14px; line-height: 1.7; }
.scene-status button, .scene-status a { margin-top: 14px; padding: 10px 18px; border: 1px solid var(--accent); background: none; color: var(--text-1); font-size: 12px; text-decoration: none; cursor: pointer; }
.status-mark { width: 14px; height: 14px; border: 1px solid var(--accent-hover); border-radius: 50%; animation: status-pulse 1.8s ease-in-out infinite; }
@keyframes status-pulse { 50% { opacity: .35; transform: scale(1.6); } }
.scene-change-enter-active, .scene-change-leave-active { transition: opacity .4s ease, transform .6s cubic-bezier(.2,.8,.2,1); }
.scene-change-enter-from { opacity: 0; transform: translateY(18px); }
.scene-change-leave-to { opacity: 0; transform: translateY(-12px); }


@media (prefers-reduced-motion: reduce) { * { transition: none !important; animation: none !important; } }
</style>
