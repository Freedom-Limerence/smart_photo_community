<template>
  <div class="tag-page">
    <header class="tag-hero">
      <div class="container">
        <p class="eyebrow">TAG</p>
        <h1># {{ tag }}</h1>
        <p class="muted">{{ mode === 'work' ? '含该标签的作品（组图与文案）' : '共 ' + total + ' 张带有该标签的公开图片' }}</p>
        <div class="mode-switch">
          <a-radio-group v-model:value="mode" button-style="solid" size="small" @change="onModeChange">
            <a-radio-button value="work">作品</a-radio-button>
            <a-radio-button value="picture">图片</a-radio-button>
          </a-radio-group>
          <span class="hint">作品 = 组图与文案的社区内容；图片 = 图库中的单张资产</span>
        </div>
      </div>
    </header>

    <div class="container">
      <a-spin v-if="mode === 'work'" :spinning="worksLoading">
        <div v-if="works.length" class="work-grid">
          <WorkCard v-for="item in works" :key="item.id" :work="item" ratio="66%" @open="router.push('/works/' + item.id)" />
        </div>
        <a-empty v-else-if="!worksLoading" description="还没有含该标签的作品">
          <a-button type="primary" @click="mode = 'picture'; onModeChange()">看看图片</a-button>
        </a-empty>
      </a-spin>

      <a-spin v-if="mode === 'picture'" :spinning="loading">
        <MasonryGrid v-if="pictures.length" :items="pictures" key-field="id">
          <template #default="{ item }">
            <article class="pic-card hover-lift" @click="router.push('/picture/' + item.id)">
              <LazyImage :src="item.thumbnailUrl || item.url" :ratio="ratioOf(item)" :alt="item.name" />
              <div class="pic-body">
                <strong class="ellipsis">{{ item.name }}</strong>
                <div class="pic-tags">
                  <a-tag v-for="t in (item.tags || []).slice(0, 3)" :key="t" class="tag-chip" @click.stop="goTag(t)">{{ t }}</a-tag>
                </div>
              </div>
            </article>
          </template>
        </MasonryGrid>
        <a-empty v-else-if="!loading" description="还没有该标签下的公开图片">
          <a-button type="primary" @click="router.push('/community')">去社区逛逛</a-button>
        </a-empty>
      </a-spin>

      <div v-if="pictures.length < total" class="load-more">
        <a-button :loading="loadingMore" @click="loadMore">加载更多</a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LazyImage from '@/components/common/LazyImage.vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { listWorksByTag } from '@/api/photoCommunity'

const route = useRoute()
const router = useRouter()
const pictures = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const current = ref(1)
const mode = ref<'work' | 'picture'>('work')
const works = ref<any[]>([])
const worksLoading = ref(false)

async function loadWorks() {
  if (!tag.value) return
  worksLoading.value = true
  try {
    const res: any = await listWorksByTag(tag.value, 24)
    works.value = res.data.code === 0 ? (res.data.data || []) : []
  } finally {
    worksLoading.value = false
  }
}

function onModeChange() {
  if (mode.value === 'work') void loadWorks()
  else void load()
}

const tag = computed(() => String(route.params.tag ?? ''))

function ratioOf(pic: any) {
  if (!pic?.picWidth || !pic?.picHeight) return '66%'
  return Math.round((pic.picHeight / pic.picWidth) * 100) + '%'
}

function goTag(value: string) {
  router.push('/tags/' + encodeURIComponent(value))
}

async function load() {
  loading.value = true
  try {
    const res: any = await listPictureVoByPageUsingPost({
      current: current.value,
      pageSize: 20,
      tags: [tag.value],
      reviewStatus: 1,
    })
    if (res.data.code === 0) {
      const records = res.data.data?.records ?? []
      pictures.value = current.value === 1 ? records : [...pictures.value, ...records]
      total.value = Number(res.data.data?.total ?? records.length)
    }
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  current.value += 1
  loadingMore.value = true
  try {
    await load()
  } finally {
    loadingMore.value = false
  }
}

function reset() {
  current.value = 1
  pictures.value = []
  void load()
}

onMounted(() => {
  void loadWorks()
  void load()
})
watch(() => route.params.tag, reset)
</script>

<style scoped>
.tag-page { padding-bottom: var(--space-8); }
.tag-hero { padding: var(--space-7) 0 var(--space-5); background: var(--surface); border-bottom: 1px solid var(--border); margin-bottom: var(--space-6); }
.tag-hero .eyebrow { margin: 0 0 6px; color: var(--text-3); font-size: var(--font-1); letter-spacing: .2em; }
.tag-hero h1 { margin: 0 0 6px; font-size: var(--font-7); }
.tag-hero p { margin: 0; }
.pic-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; cursor: pointer; }
.pic-body { padding: 10px 12px 12px; display: flex; flex-direction: column; gap: 8px; }
.pic-body strong { font-size: var(--font-2); }
.pic-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.tag-chip { cursor: pointer; }
.mode-switch { display: flex; align-items: center; gap: 10px; margin-top: 10px; flex-wrap: wrap; }
.mode-switch .hint { color: var(--text-3); font-size: 12px; }
.work-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: var(--space-4); }
.load-more { display: flex; justify-content: center; margin-top: var(--space-6); }
</style>
