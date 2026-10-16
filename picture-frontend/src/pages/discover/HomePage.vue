<template>
  <div class="discover-page">
    <section class="opening container">
      <div class="opening-copy">
        <span class="overline">GUANGYING · PHOTOGRAPHY COMMUNITY</span>
        <h1>{{ mode === 'community' ? '一张照片，\n一种看世界的方式。' : '在这里，\n看见更多视角。' }}</h1>
        <p>来自摄影师的真实创作。慢一点浏览，留意光线、色彩与每一个值得停驻的瞬间。</p>
        <div class="opening-actions">
          <a-button type="primary" size="large" @click="router.push('/me/create-work')">发布作品</a-button>
          <button class="plain-link" @click="scrollToFeed">探索作品 <span aria-hidden="true">↘</span></button>
        </div>
        <div class="opening-foot"><span>01 / DISCOVER</span><span>{{ total ? `${total} 件公开作品` : '摄影爱好者的创作社区' }}</span></div>
      </div>

      <button v-if="featured && mode !== 'community'" class="featured" type="button" @click="openWork(featured)">
        <img :src="coverOf(featured)" :alt="featured.title" />
        <span class="featured-shade" aria-hidden="true" />
        <span class="featured-label">FEATURED STORY <span>01</span></span>
        <span class="featured-caption"><strong>{{ featured.title }}</strong><small>{{ featured.author?.userName || '摄影师' }} · 浏览作品 ↗</small></span>
      </button>
      <div v-else class="opening-visual"><span>CURATED BY LIGHT</span><strong>光 / 影 / 之间</strong><span>让照片成为对话的开始</span></div>
    </section>

    <section id="works-feed" class="feed container">
      <div class="feed-heading">
        <div><span class="overline">THE COMMUNITY EDIT</span><h2>{{ mode === 'community' ? '社区作品' : '最新发现' }}</h2><p>从城市漫游到自然光景，看看大家正在记录什么。</p></div>
        <div class="feed-tabs" role="group" aria-label="作品排序与范围">
          <button type="button" :class="{ active: sort === 'latest' }" :aria-pressed="sort === 'latest'" @click="sort = 'latest'">最新</button>
          <button type="button" :class="{ active: sort === 'hot' }" :aria-pressed="sort === 'hot'" @click="sort = 'hot'">热门</button>
          <button type="button" :class="{ active: feed === 'following' }" :aria-pressed="feed === 'following'" @click="toggleFollowing">关注</button>
        </div>
      </div>

      <CardSkeleton v-if="loading && !works.length" variant="masonry" :count="8" ratio="72%" />
      <MasonryGrid v-else-if="displayWorks.length" :items="displayWorks">
        <template #default="{ item }"><WorkCard :work="item" @open="openWork" /></template>
      </MasonryGrid>
      <EmptyState v-else title="社区还在等待第一组作品" description="分享你眼里的世界，开启新的对话。">
        <a-button type="primary" @click="router.push('/me/create-work')">发布作品</a-button>
      </EmptyState>

      <div v-if="works.length && works.length < total" class="feed-more">
        <button type="button" :disabled="loading" @click="loadMore">{{ loading ? '正在加载…' : '继续浏览作品 ↓' }}</button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listWorks } from '@/api/photoCommunity'

const props = withDefaults(defineProps<{ mode?: 'home' | 'community' }>(), { mode: 'home' })
const router = useRouter()
const works = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const loading = ref(false)
const sort = ref<'latest' | 'hot'>('latest')
const feed = ref<'all' | 'following'>('all')
/** 关注流：只看我关注的作者（未登录时会退回普通流） */
function toggleFollowing() { feed.value = feed.value === 'following' ? 'all' : 'following'; }
let fetchVersion = 0
const featured = computed(() => works.value.find((w) => coverOf(w)))
const displayWorks = computed(() => {
  const source = props.mode === 'home' && featured.value ? works.value.filter((w) => w.id !== featured.value.id) : [...works.value]
  return source
})
function coverOf(work: any) { return work?.coverUrl || work?.pictures?.[0]?.url || work?.thumbnailUrl || '' }
function openWork(work: any) { if (work?.id) router.push('/works/' + work.id) }
function scrollToFeed() { document.querySelector('#works-feed')?.scrollIntoView({ behavior: 'smooth' }) }
async function fetchWorks() {
  const version = ++fetchVersion
  loading.value = true
  try {
    const res: any = await listWorks({ current: current.value, size: 16, sort: sort.value, feed: feed.value })
    if (version !== fetchVersion) return
    if (res.data.code !== 0) throw new Error(res.data.message || '加载失败')
    const records = res.data.data?.records || []
    works.value = current.value === 1 ? records : [...works.value, ...records]
    total.value = Number(res.data.data?.total || 0)
  } catch (error: any) { if (version === fetchVersion) message.error(error?.message || '作品暂时无法加载') }
  finally { if (version === fetchVersion) loading.value = false }
}
async function loadMore() { current.value += 1; await fetchWorks() }
onMounted(fetchWorks)
watch([sort, feed], () => { current.value = 1; works.value = []; void fetchWorks() })
</script>

<style scoped>
.discover-page { padding-bottom: var(--space-8); }

/* ------------------------------------------------------------
   首屏：左文案 + 右主视觉，随视口放宽版心与间距
   ------------------------------------------------------------ */
.opening {
  display: grid;
  grid-template-columns: minmax(320px, 39%) minmax(0, 1fr);
  align-items: stretch;
  gap: clamp(var(--space-6), 5vw, 96px);
  min-height: 560px;
  padding-top: clamp(var(--space-7), 5vw, 88px);
  padding-bottom: var(--space-8);
}

.opening-copy { display: flex; flex-direction: column; align-items: flex-start; padding: var(--space-5) 0 var(--space-1); }
.overline { color: var(--text-3); font-size: 10px; font-weight: 600; letter-spacing: .22em; }
.opening h1 {
  white-space: pre-line;
  margin: clamp(var(--space-5), 4vw, 56px) 0 var(--space-5);
  color: var(--text-1);
  font-family: var(--font-display);
  font-size: clamp(38px, 4vw, 64px);
  font-weight: 400;
  letter-spacing: -.035em;
  line-height: 1.3;
}
.opening-copy > p { max-width: 38ch; margin: 0; color: var(--text-2); font-size: var(--font-3); line-height: 2; }

.opening-actions { display: flex; align-items: center; gap: var(--space-5); margin-top: var(--space-6); }
.opening-actions :deep(.ant-btn-primary) {
  height: 46px;
  padding: 0 26px;
  background: var(--accent-ink);
  box-shadow: none;
}
.opening-actions :deep(.ant-btn-primary:hover) { background: var(--accent-hover); }

.plain-link {
  padding: var(--space-2) 0;
  border: 0;
  border-bottom: 1px solid var(--border-strong);
  background: none;
  color: var(--text-2);
  cursor: pointer;
  font-size: var(--font-2);
  transition: color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease);
}
.plain-link:hover { color: var(--accent-ink); border-color: var(--accent); }

.opening-foot {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  width: 100%;
  margin-top: auto;
  padding-top: var(--space-5);
  border-top: 1px solid var(--border);
  color: var(--text-3);
  font-size: 10px;
  letter-spacing: .12em;
}

/* 主视觉：圆角 + 阴影，悬停轻微推近 */
.featured {
  position: relative;
  display: block;
  min-width: 0;
  height: clamp(420px, 40vw, 560px);
  padding: 0;
  border: 0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--surface-3);
  box-shadow: var(--shadow-2);
  cursor: pointer;
  text-align: left;
  transition: box-shadow var(--dur) var(--ease), transform var(--dur) var(--ease);
}
.featured:hover { box-shadow: var(--shadow-3); transform: translateY(-2px); }
.featured:focus-visible { outline: 2px solid var(--accent); outline-offset: 3px; }
.featured > img { display: block; width: 100%; height: 100%; object-fit: cover; transition: transform 800ms var(--ease); }
.featured:hover > img { transform: scale(1.035); }

.featured-shade { position: absolute; inset: 38% 0 0; background: linear-gradient(transparent, rgba(0, 0, 0, .58)); }
.featured-label {
  position: absolute;
  top: var(--space-5);
  left: var(--space-5);
  right: var(--space-5);
  display: flex;
  justify-content: space-between;
  color: #fff;
  font-size: 10px;
  letter-spacing: .17em;
}
.featured-caption {
  position: absolute;
  bottom: var(--space-5);
  left: var(--space-5);
  right: var(--space-5);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  color: #fff;
}
.featured-caption strong { font-family: var(--font-display); font-size: clamp(22px, 2vw, 30px); font-weight: 400; line-height: 1.3; }
.featured-caption small { color: rgba(255, 255, 255, .84); font-size: var(--font-1); }

/* 无主视觉时（社区模式）的编辑式占位 */
.opening-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-6);
  height: clamp(420px, 40vw, 560px);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(120% 120% at 20% 0%, var(--accent-soft) 0%, transparent 58%),
    var(--surface-2);
  box-shadow: inset 0 0 0 1px var(--border);
  color: var(--text-3);
  font-size: 10px;
  letter-spacing: .2em;
}
.opening-visual strong {
  color: var(--accent-ink);
  font-family: var(--font-display);
  font-size: clamp(30px, 3.6vw, 56px);
  font-weight: 400;
  letter-spacing: .28em;
}

/* ------------------------------------------------------------
   作品流
   ------------------------------------------------------------ */
.feed { padding-top: var(--space-5); }

.feed-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border);
}
.feed-heading h2 { margin: var(--space-2) 0 var(--space-1); color: var(--text-1); font-family: var(--font-display); font-size: var(--font-7); font-weight: 400; }
.feed-heading p { margin: 0; color: var(--text-3); font-size: var(--font-2); }

.feed-tabs {
  display: inline-flex;
  gap: 2px;
  padding: 3px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
}
.feed-tabs button {
  padding: 6px 16px;
  border: 0;
  border-radius: var(--radius-pill);
  background: none;
  color: var(--text-3);
  cursor: pointer;
  font-size: var(--font-2);
  transition: background var(--dur-fast) var(--ease), color var(--dur-fast) var(--ease);
}
.feed-tabs button:hover { color: var(--text-1); background: var(--surface-2); }
.feed-tabs button.active { background: var(--accent-soft); color: var(--accent-ink); font-weight: 600; }

.feed-more { display: flex; justify-content: center; padding-top: var(--space-7); }
.feed-more button {
  padding: var(--space-3) var(--space-6);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-2);
  cursor: pointer;
  font-size: var(--font-2);
  transition: all var(--dur-fast) var(--ease);
}
.feed-more button:hover:not(:disabled) { border-color: var(--accent); color: var(--accent-ink); background: var(--accent-soft); }
.feed-more button:disabled { cursor: default; opacity: .7; }

@media (max-width: 900px) {
  .opening { grid-template-columns: 1fr; min-height: 0; padding-top: var(--space-7); gap: var(--space-6); }
  .opening h1 { margin-top: var(--space-5); }
  .opening-foot { margin-top: var(--space-5); }
}

@media (max-width: 640px) {
  .opening { padding-bottom: var(--space-6); }
  .opening h1 { font-size: 34px; }
  .opening-actions { flex-wrap: wrap; gap: var(--space-3); }
  .featured, .opening-visual { height: 320px; }
  .feed-heading { flex-direction: column; align-items: flex-start; }
  .feed-tabs { width: 100%; justify-content: space-between; }
  .feed-tabs button { flex: 1 1 auto; padding: 6px 10px; }
}
</style>
