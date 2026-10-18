<template>
  <div class="tag-page">
    <header v-reveal class="tag-hero">
      <div class="container">
        <PageHeader eyebrow="TAG" :title="'# ' + tag"
                    :description="mode === 'work' ? '含该标签的作品（组图与文案）' : `共 ${total} 张带有该标签的公开图片`">
          <div class="mode-switch">
            <a-radio-group v-model:value="mode" button-style="solid" size="small" @change="onModeChange">
              <a-radio-button value="work">作品</a-radio-button>
              <a-radio-button value="picture">图片</a-radio-button>
            </a-radio-group>
            <span class="hint">作品 = 组图与文案的社区内容；图片 = 图库中的单张资产</span>
          </div>
        </PageHeader>
      </div>
    </header>

    <div class="container tag-body">
      <a-spin v-if="mode === 'work'" :spinning="worksLoading">
        <ErrorState v-if="worksError" :description="worksError" @retry="loadWorks()" />
        <CardSkeleton v-else-if="worksLoading && !works.length" variant="grid" :count="8" />
        <div v-else-if="works.length" class="card-grid">
          <WorkCard v-for="item in works" :key="item.id" :work="item" ratio="66%" @open="router.push('/works/' + item.id)" />
        </div>
        <EmptyState v-else-if="!worksError" title="还没有含该标签的作品" description="换个标签看看，或者按图片浏览同类内容。">
          <a-button type="primary" @click="mode = 'picture'; onModeChange()">看看图片</a-button>
        </EmptyState>
      </a-spin>

      <a-spin v-if="mode === 'picture'" :spinning="loading">
        <ErrorState v-if="error" :description="error" @retry="load()" />
        <CardSkeleton v-else-if="loading && !pictures.length" variant="masonry" :count="8" ratio="72%" />
        <MasonryGrid v-else-if="pictures.length" :items="pictures" key-field="id">
          <template #default="{ item }">
            <article class="pic-card media-card hover-lift" tabindex="0" @click="router.push('/picture/' + item.id)" @keydown.enter="router.push('/picture/' + item.id)">
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
        <EmptyState v-else-if="!error" title="还没有该标签下的公开图片" description="去社区逛逛，发现更多同主题创作。">
          <a-button type="primary" @click="router.push('/community')">去社区逛逛</a-button>
        </EmptyState>
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
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { listWorksByTag } from '@/api/photoCommunity'

const route = useRoute()
const router = useRouter()
const pictures = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const worksError = ref('')
const loadingMore = ref(false)
const current = ref(1)
const mode = ref<'work' | 'picture'>('work')
const works = ref<any[]>([])
const worksLoading = ref(false)

async function loadWorks() {
  if (!tag.value) return
  worksLoading.value = true
  worksError.value = ''
  try {
    const res: any = await listWorksByTag(tag.value, 24)
    works.value = res.data.code === 0 ? (res.data.data || []) : []
  } catch {
    worksError.value = '该标签的作品暂时无法加载，请稍后重试。'
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
  error.value = ''
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
  } catch {
    error.value = '该标签的内容暂时无法加载，请稍后重试。'
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

/* 标签页头：整条浅色带 + 统一标题区，超宽屏下同样贴住版心 */
.tag-hero {
  padding: var(--space-7) 0 0;
  margin-bottom: var(--space-6);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.tag-hero :deep(.page-head) { border-bottom: 0; padding-bottom: var(--space-6); }

.mode-switch { display: flex; align-items: center; gap: var(--space-3); flex-wrap: wrap; }
.mode-switch .hint { color: var(--text-3); font-size: var(--font-1); }

.tag-body { min-height: 40vh; }

.pic-card { cursor: pointer; }
.pic-card:focus-visible { outline: 2px solid var(--accent); outline-offset: 3px; }
.pic-body { display: flex; flex-direction: column; gap: var(--space-2); padding: var(--space-3) var(--space-4) var(--space-4); }
.pic-body strong { font-size: var(--font-2); font-weight: 500; }
.pic-tags { display: flex; flex-wrap: wrap; gap: var(--space-1); }
.tag-chip { cursor: pointer; }

.load-more { display: flex; justify-content: center; margin-top: var(--space-6); }
</style>
