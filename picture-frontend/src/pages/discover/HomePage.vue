<template>
  <div class="discover-page">
    <!-- Hero：高质量作品轮播 -->
    <section class="hero">
      <a-carousel :autoplay="heroItems.length > 1" :dots="true" effect="fade" class="hero-carousel">
        <div v-for="item in heroItems" :key="item.key" class="hero-slide">
          <img :src="item.cover" :alt="item.title" class="hero-image" />
          <div class="hero-mask" />
          <div class="hero-content container">
            <p class="hero-eyebrow">{{ item.eyebrow }}</p>
            <h1>{{ item.title }}</h1>
            <p class="hero-desc">{{ item.desc }}</p>
            <div class="hero-actions">
              <a-button type="primary" size="large" @click="openWork(item.work)">查看作品</a-button>
              <a-button size="large" ghost class="hero-ghost" @click="router.push('/community')">进入社区</a-button>
            </div>
          </div>
        </div>
        <div v-if="!heroItems.length" class="hero-slide hero-fallback">
          <div class="hero-content container">
            <p class="hero-eyebrow">PHOTOGRAPHY COMMUNITY</p>
            <h1>发现世界的另一种视角</h1>
            <p class="hero-desc">上传你的照片，让 AI 整理资产，再把值得分享的作品带到社区。</p>
            <div class="hero-actions">
              <a-button type="primary" size="large" @click="router.push('/me/upload')">上传第一组照片</a-button>
            </div>
          </div>
        </div>
      </a-carousel>
    </section>

    <!-- 瀑布流作品流 -->
    <section class="container">
      <div class="section-head">
        <div>
          <p class="eyebrow">DISCOVER</p>
          <h2>正在被看见的作品</h2>
          <p>来自社区的最新创作，按发布时间持续更新</p>
        </div>
        <a-space>
          <a-radio-group v-model:value="sort" button-style="solid" size="small" @change="reload">
            <a-radio-button value="latest">最新</a-radio-button>
            <a-radio-button value="hot">热门</a-radio-button>
          </a-radio-group>
          <a-button type="text" @click="router.push('/community')">查看全部 →</a-button>
        </a-space>
      </div>

      <a-spin :spinning="loading">
        <MasonryGrid v-if="works.length" :items="works">
          <template #default="{ item }">
            <WorkCard :work="item" :ratio="ratioOf(item)" @open="openWork" />
          </template>
        </MasonryGrid>

        <a-empty v-else-if="!loading" description="社区还没有公开作品">
          <a-button type="primary" @click="router.push('/me/upload')">上传第一组作品</a-button>
        </a-empty>
      </a-spin>

      <div v-if="works.length" class="load-more">
        <a-button :loading="loadingMore" @click="loadMore">加载更多作品</a-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { listWorks } from '@/api/photoCommunity'

const router = useRouter()
const works = ref<any[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const current = ref(1)
const sort = ref<'latest' | 'hot'>('latest')

const heroItems = computed(() =>
  works.value.slice(0, 3).map((w, i) => ({
    key: w.id,
    work: w,
    cover: coverOf(w),
    eyebrow: i === 0 ? 'FEATURED WORK' : 'EDITOR PICK',
    title: w.title || '未命名作品',
    desc: w.description || '来自社区摄影师的创作',
  })).filter((i) => !!i.cover),
)

function coverOf(work: any) {
  return work?.coverUrl || work?.pictures?.[0]?.url || work?.pictures?.[0]?.thumbnailUrl || ''
}

function ratioOf(work: any) {
  const pic = work?.pictures?.[0]
  if (!pic?.picWidth || !pic?.picHeight) return '66%'
  return Math.round((pic.picHeight / pic.picWidth) * 100) + '%'
}

function openWork(work: any) {
  if (work?.id) router.push('/works/' + work.id)
}

async function reload() {
  current.value = 1
  works.value = []
  await fetchWorks()
}

async function fetchWorks() {
  loading.value = true
  try {
    const res: any = await listWorks({ current: current.value, size: 12 })
    if (res.data.code === 0) {
      const records = res.data.data?.records ?? []
      works.value = current.value === 1 ? records : [...works.value, ...records]
    }
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  current.value += 1
  loadingMore.value = true
  try {
    await fetchWorks()
  } finally {
    loadingMore.value = false
  }
}

onMounted(fetchWorks)
</script>

<style scoped>
.discover-page { padding-bottom: var(--space-8); }

.hero { margin-bottom: var(--space-6); }
.hero-carousel { height: 460px; }
.hero-slide { position: relative; height: 460px; overflow: hidden; }
.hero-image { width: 100%; height: 100%; object-fit: cover; display: block; }
.hero-fallback { background: linear-gradient(135deg, #2A332E, #4A5A50); }

.hero-mask {
  position: absolute; inset: 0;
  background: linear-gradient(90deg, rgba(16, 20, 18, .78) 0%, rgba(16, 20, 18, .45) 45%, rgba(16, 20, 18, .12) 100%);
}

.hero-content { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: center; color: #F6F6F3; }
.hero-eyebrow { margin: 0 0 10px; font-size: var(--font-1); letter-spacing: .22em; color: rgba(246, 246, 243, .7); }
.hero-content h1 { margin: 0 0 12px; font-size: var(--font-8); line-height: 1.15; max-width: 560px; }
.hero-desc { margin: 0 0 22px; max-width: 480px; color: rgba(246, 246, 243, .82); }
.hero-actions { display: flex; gap: var(--space-3); }
.hero-ghost { color: #F6F6F3; border-color: rgba(246, 246, 243, .4); }

.load-more { display: flex; justify-content: center; margin-top: var(--space-6); }

@media (max-width: 768px) {
  .hero-carousel, .hero-slide { height: 340px; }
  .hero-content h1 { font-size: var(--font-7); }
}
</style>
