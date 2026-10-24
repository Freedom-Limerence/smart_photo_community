<template>
  <div class="ranking-page">
    <div class="aurora" aria-hidden="true"></div>
    <section class="rank-hero container">
      <p class="eyebrow">LEADERBOARD</p>
      <h1>作品<em>热度榜</em></h1>
      <p class="rank-sub">按社区互动热度实时排序——点赞、收藏与浏览共同决定座次。</p>
    </section>

    <section class="rank-body container">
      <CardSkeleton v-if="loading && !works.length" variant="masonry" :count="6" ratio="72%" />
      <template v-else>
        <!-- 前三名领奖台 -->
        <div v-if="works.length >= 3" class="podium">
          <button v-for="(w, i) in works.slice(0, 3)" :key="w.id" type="button" class="podium-card"
                  :class="'p' + (i + 1)" @click="openWork(w)">
            <span class="p-medal">{{ ['①', '②', '③'][i] }}</span>
            <span class="p-cover"><img :src="coverOf(w)" :alt="w.title || ''" loading="lazy" /></span>
            <span class="p-title">{{ w.title || '未命名作品' }}</span>
            <span class="p-author">{{ w.author?.userName || '摄影师' }}</span>
          </button>
        </div>

        <!-- 4 名以后列表 -->
        <ol v-if="works.length > 3" class="rank-list">
          <li v-for="(w, i) in works.slice(3)" :key="w.id" class="rank-row" @click="openWork(w)">
            <span class="r-no">{{ String(i + 4).padStart(2, '0') }}</span>
            <span class="r-cover"><img :src="coverOf(w)" :alt="w.title || ''" loading="lazy" /></span>
            <span class="r-title">{{ w.title || '未命名作品' }}<em>{{ w.author?.userName || '摄影师' }}</em></span>
            <span class="r-heat"><FireOutlined /> {{ heatOf(w) }}</span>
          </li>
        </ol>
        <EmptyState v-if="!works.length" title="榜单虚位以待" description="发布作品、收获互动，登上热度榜。">
          <a-button type="primary" @click="router.push('/me/create-work')">发布作品</a-button>
        </EmptyState>
      </template>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { FireOutlined } from '@ant-design/icons-vue'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listWorks } from '@/api/photoCommunity'

const router = useRouter()
const works = ref<any[]>([])
const loading = ref(false)

function coverOf(w: any) {
  const p = w.pictures?.[0]
  return w.coverUrl || p?.thumbnailUrl || p?.url || ''
}
function heatOf(w: any) {
  return (w.likeCount || 0) + (w.favoriteCount || 0)
}
function openWork(w: any) {
  if (w?.id) router.push('/works/' + w.id)
}

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await listWorks({ current: 1, size: 20, sort: 'hot' })
    if (res.data.code === 0) works.value = res.data.data?.records || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.ranking-page { position: relative; min-height: 100vh; padding-bottom: 60px; }
.aurora { position: absolute; inset: 0 0 auto 0; height: 60vh; background: radial-gradient(60% 50% at 50% 0%, rgba(64, 100, 210, .16), transparent 70%); pointer-events: none; }
.rank-hero { position: relative; z-index: 1; padding-top: clamp(30px, 5vw, 70px); }
.rank-hero h1 { margin: 8px 0 10px; font-family: var(--font-display); font-weight: 400; font-size: clamp(30px, 4vw, 56px); letter-spacing: -.02em; color: var(--text-1); }
.rank-hero h1 em { font-style: normal; color: var(--accent-hover); }
.rank-sub { margin: 0; color: var(--text-3); max-width: 56ch; }
.rank-body { position: relative; z-index: 1; margin-top: clamp(24px, 3vw, 44px); }

/* 领奖台 */
.podium { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: clamp(12px, 2vw, 24px); align-items: end; }
.podium-card { position: relative; display: flex; flex-direction: column; gap: 8px; padding: 0 0 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--surface-1, rgba(255,255,255,.03)); cursor: pointer; text-align: left; overflow: hidden; transition: transform .35s var(--ease), border-color .25s; }
.podium-card:hover { transform: translateY(-4px); border-color: rgba(163,182,168,.5); }
.podium-card.p1 { order: 2; }
.podium-card.p2 { order: 1; }
.podium-card.p3 { order: 3; }
.p-medal { position: absolute; top: 10px; left: 10px; z-index: 2; width: 34px; height: 34px; display: grid; place-items: center; border-radius: 50%; font-size: 16px; font-family: var(--font-display); }
.p1 .p-medal { background: rgba(255, 200, 90, .92); color: rgb(58, 44, 5); }
.p2 .p-medal { background: rgba(200, 205, 215, .9); color: rgb(51, 54, 60); }
.p3 .p-medal { background: rgba(205, 140, 90, .9); color: rgb(61, 36, 16); }
.p-cover { height: clamp(140px, 22vw, 240px); overflow: hidden; }
.p-cover img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .6s var(--ease); }
.podium-card:hover .p-cover img { transform: scale(1.05); }
.p-title { padding: 0 14px; color: var(--text-1); font-size: 15px; font-family: var(--font-display); }
.p-author { padding: 0 14px; color: var(--text-3); font-size: 12px; }

/* 4 名以后 */
.rank-list { margin-top: clamp(16px, 2.4vw, 28px); display: flex; flex-direction: column; }
.rank-row { display: grid; grid-template-columns: 56px 84px 1fr auto; align-items: center; gap: 16px; padding: 12px 16px; border-bottom: 1px solid var(--border); cursor: pointer; transition: background .2s; }
.rank-row:hover { background: rgba(163, 182, 168, .07); }
.r-no { font-family: var(--font-display); font-size: 20px; color: var(--text-3); }
.r-cover { width: 84px; height: 56px; overflow: hidden; border-radius: 6px; }
.r-cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.r-title { display: flex; flex-direction: column; gap: 2px; color: var(--text-1); font-size: 14.5px; min-width: 0; }
.r-title em { font-style: normal; color: var(--text-3); font-size: 12px; }
.r-heat { display: inline-flex; align-items: center; gap: 6px; color: var(--accent-hover); font-size: 13px; font-variant-numeric: tabular-nums; }

@media (max-width: 720px) {
  .podium { grid-template-columns: 1fr; }
  .podium-card { order: initial !important; }
  .rank-row { grid-template-columns: 40px 64px 1fr auto; gap: 10px; }
}
</style>
