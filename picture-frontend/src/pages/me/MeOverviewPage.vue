<template>
  <div class="me-page">
    <section class="me-hero surface-card">
      <a-avatar :src="user.userAvatar" :size="76">{{ avatarText }}</a-avatar>
      <div class="me-hero-main">
        <h1>{{ user.userName || '摄影师' }}</h1>
        <p class="muted">{{ user.userProfile || '还没有填写简介，去「账号设置」补充一句吧' }}</p>
      </div>
      <a-space>
        <a-button @click="router.push('/me/gallery')">管理我的图库</a-button>
        <a-button type="primary" @click="router.push('/me/upload')">上传图片</a-button>
      </a-space>
    </section>

    <a-row :gutter="16" class="me-stats">
      <a-col :xs="8"><div class="stat-card surface-card"><strong>{{ stats.pictures }}</strong><span>图库图片</span></div></a-col>
      <a-col :xs="8"><div class="stat-card surface-card"><strong>{{ stats.works }}</strong><span>已发布作品</span></div></a-col>
      <a-col :xs="8"><div class="stat-card surface-card"><strong>{{ stats.spaces }}</strong><span>我的空间</span></div></a-col>
    </a-row>

    <div class="section-head">
      <div><p class="eyebrow">MY WORKS</p><h2>最近的公开作品</h2></div>
      <a-button type="text" @click="router.push('/me/works')">全部作品 →</a-button>
    </div>

    <a-spin :spinning="loading">
      <MasonryGrid v-if="works.length" :items="works">
        <template #default="{ item }">
          <WorkCard :work="item" @open="(w:any) => router.push('/works/' + w.id)" />
        </template>
      </MasonryGrid>
      <a-empty v-else-if="!loading" description="还没有发布作品">
        <a-button type="primary" @click="router.push('/community/create')">发布第一组作品</a-button>
      </a-empty>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { listWorks, listWorksByAuthor } from '@/api/photoCommunity'
import { listPictureVoByPageUsingPost } from '@/api/pictureController'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const user = computed(() => loginUserStore.loginUser || ({} as any))
const avatarText = computed(() => (user.value.userName || '光').slice(0, 1))
const works = ref<any[]>([])
const loading = ref(false)
const stats = ref({ pictures: 0, works: 0, spaces: 0 })

async function load() {
  if (!user.value.id) return
  loading.value = true
  try {
    const [wRes, pRes, sRes]: any[] = await Promise.all([
      listWorksByAuthor({ authorId: user.value.id, current: 1, size: 8 }),
      listPictureVoByPageUsingPost({ current: 1, pageSize: 1, userId: user.value.id }),
      listSpaceVoByPageUsingPost({ current: 1, pageSize: 1, userId: user.value.id }),
    ])
    if (wRes.data.code === 0) works.value = wRes.data.data?.records ?? []
    stats.value = {
      works: Number(wRes.data.data?.total ?? works.value.length),
      pictures: Number(pRes.data.data?.total ?? 0),
      spaces: Number(sRes.data.data?.total ?? 0),
    }
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.me-page { display: flex; flex-direction: column; gap: var(--space-5); }
.me-hero { display: flex; align-items: center; gap: var(--space-5); padding: var(--space-5); }
.me-hero-main { flex: 1 1 auto; }
.me-hero-main h1 { margin: 0 0 4px; font-size: var(--font-6); }
.me-hero-main p { margin: 0; font-size: var(--font-2); }
.me-stats { margin-top: var(--space-2); }
.stat-card { display: flex; flex-direction: column; gap: 2px; padding: var(--space-4); text-align: center; }
.stat-card strong { font-size: var(--font-6); }
.stat-card span { color: var(--text-3); font-size: var(--font-1); }
</style>
