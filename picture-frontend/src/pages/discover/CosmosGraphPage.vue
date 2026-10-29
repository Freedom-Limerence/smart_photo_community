<template>
  <div class="cosmos-graph-page">
    <div class="cg-canvas-wrap">
      <canvas ref="canvasRef" class="cg-canvas" />
      <!-- hover 浮标：节点标题与作者 -->
      <div v-if="hoverText" class="cg-tooltip" :style="{ left: hoverPos.x + 14 + 'px', top: hoverPos.y - 10 + 'px' }">
        <strong>{{ hoverText }}</strong>
      </div>
      <!-- 操作提示 -->
      <div class="cg-hints" aria-hidden="true">
        <span>拖拽 · 旋转宇宙</span><span>滚轮 · 缩放</span><span>点击光点 · 看作品</span>
      </div>
    </div>

    <header class="cg-head">
      <p class="eyebrow">LIGHT CONSTELLATION</p>
      <h1>光影<em>星图</em></h1>
      <p class="cg-sub">大星是题材，小星是作品。<br />{{ hubsCount }} 个题材星团 · {{ leaves }} 幅作品 · {{ edgeCount }} 条光桥。<br /><span style="color: var(--accent-hover)">推近星团，展开其中的作品。</span></p>
    </header>

    <a-spin v-if="loading" class="cg-loading" size="large" />
    <a-empty v-else-if="!works.length" class="cg-loading" description="星空还没有作品——先去发布一组吧" />

    <Lightbox :works="works" v-model:open="lightboxOpen" v-model:index="lightboxIdx" @detail="openDetail" />
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import Lightbox from '@/components/common/Lightbox.vue'
import { createGraphUniverse, type GraphData, type GraphHandle } from '@/three/photoGraph'
import { getCosmosGraph } from '@/api/photoCommunity'

const router = useRouter()
const canvasRef = ref<HTMLCanvasElement | null>(null)
const loading = ref(true)
const works = ref<any[]>([])
const edgeCount = ref(0)
const lightboxOpen = ref(false)
const lightboxIdx = ref(0)
const hoverText = ref<string | null>(null)
const hoverPos = ref({ x: 0, y: 0 })
const hubsCount = ref(0)
const leaves = computed(() => works.value.length)

let handle: GraphHandle | null = null

function openDetail(w: any) {
  if (w?.id) router.push('/works/' + w.id)
}

onMounted(async () => {
  try {
      const res: any = await getCosmosGraph(300)
    if (res.data.code === 0 && res.data.data?.hubs?.length) {
      const gd = res.data.data
      works.value = (gd.leaves || []).map((lf: any) => ({ ...lf, id: lf.workId }))
      hubsCount.value = (gd.hubs || []).length
      edgeCount.value = (gd.edges || []).length
      const data: GraphData = { hubs: gd.hubs || [], leaves: gd.leaves || [], edges: gd.edges || [] }
      if (canvasRef.value) {
        handle = createGraphUniverse(canvasRef.value, data, {
          onPickLeaf: (leafIndex) => {
            lightboxIdx.value = leafIndex
            lightboxOpen.value = true
          },
          onOpenHub: (tag) => router.push('/tags/' + encodeURIComponent(tag)),
          onHover: (text, screen) => {
            hoverText.value = text
            hoverPos.value = screen
          },
        })
      }
    }
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => handle?.dispose())
</script>

<style scoped>
.cosmos-graph-page { position: relative; min-height: 100vh; overflow: hidden; background: rgb(2, 2, 4); }
.cg-canvas-wrap { position: fixed; inset: 0; }
.cg-canvas { position: absolute; inset: 0; width: 100%; height: 100%; display: block; touch-action: none; cursor: grab; }
.cg-canvas:active { cursor: grabbing; }

.cg-head { position: absolute; top: clamp(60px, 10vh, 110px); left: clamp(20px, 5vw, 64px); z-index: 2; pointer-events: none; }
.eyebrow { margin: 0 0 6px; color: var(--accent-hover); font-size: 12px; letter-spacing: .2em; }
.cg-head h1 { margin: 0 0 10px; color: var(--text-1); font-family: var(--font-display); font-weight: 400; font-size: clamp(30px, 4.4vw, 58px); letter-spacing: -.02em; }
.cg-head h1 em { font-style: normal; color: var(--accent-hover); }
.cg-sub { margin: 0; color: var(--text-3); font-size: 13.5px; line-height: 1.8; }

.cg-tooltip { position: absolute; z-index: 3; pointer-events: none; padding: 10px 14px; border-radius: 10px; background: rgba(8, 10, 8, .82); border: 1px solid rgba(163, 182, 168, .3); backdrop-filter: blur(8px); max-width: 260px; }
.cg-tooltip strong { display: block; color: var(--text-1); font-family: var(--font-display); font-size: 14.5px; font-weight: 400; }
.cg-tooltip span { display: block; margin-top: 3px; color: var(--text-3); font-size: 12px; }

.cg-hints { position: absolute; bottom: 22px; left: 50%; transform: translateX(-50%); z-index: 2; display: flex; gap: 22px; pointer-events: none; }
.cg-hints span { color: var(--text-3); font-size: 12px; letter-spacing: .1em; }

.cg-loading { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); z-index: 2; }

@media (max-width: 640px) {
  .cg-sub { display: none; }
  .cg-hints { gap: 12px; }
}
</style>
