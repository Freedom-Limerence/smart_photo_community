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

      <button v-if="featured && mode !== 'community'" class="featured" @click="openWork(featured)">
        <img :src="coverOf(featured)" :alt="featured.title" />
        <span class="featured-shade" />
        <span class="featured-label">FEATURED STORY <span>01</span></span>
        <span class="featured-caption"><strong>{{ featured.title }}</strong><small>{{ featured.author?.userName || '摄影师' }} · 浏览作品 ↗</small></span>
      </button>
      <div v-else class="opening-visual"><span>CURATED BY LIGHT</span><strong>光 / 影 / 之间</strong><span>让照片成为对话的开始</span></div>
    </section>

    <section id="works-feed" class="feed container">
      <div class="feed-heading">
        <div><span class="overline">THE COMMUNITY EDIT</span><h2>{{ mode === 'community' ? '社区作品' : '最新发现' }}</h2><p>从城市漫游到自然光景，看看大家正在记录什么。</p></div>
        <div class="feed-tabs" role="group" aria-label="作品排序">
          <button :class="{ active: sort === 'latest' }" @click="sort = 'latest'">最新</button>
          <button :class="{ active: sort === 'hot' }" @click="sort = 'hot'">热门</button>
          <button :class="{ active: feed === 'following' }" @click="toggleFollowing">关注</button>
        </div>
      </div>

      <div v-if="loading && !works.length" class="loading-grid">
        <div v-for="n in 6" :key="n" class="skeleton" />
      </div>
      <MasonryGrid v-else-if="displayWorks.length" :items="displayWorks" gap="28px">
        <template #default="{ item }"><WorkCard :work="item" @open="openWork" /></template>
      </MasonryGrid>
      <div v-else class="empty-feed"><strong>社区还在等待第一组作品</strong><p>分享你眼里的世界，开启新的对话。</p><a-button type="primary" @click="router.push('/me/create-work')">发布作品</a-button></div>

      <div v-if="works.length && works.length < total" class="feed-more"><button :disabled="loading" @click="loadMore">{{ loading ? '正在加载…' : '继续浏览作品 ↓' }}</button></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
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
.discover-page { padding-bottom: 110px; background: #fafaf8; }
.opening { display: grid; grid-template-columns: minmax(310px, 37%) minmax(0, 1fr); gap: clamp(36px, 5vw, 84px); min-height: 580px; padding-top: 62px; padding-bottom: 72px; }
.opening-copy { display: flex; flex-direction: column; align-items: flex-start; padding: 26px 0 8px; }
.overline { color: #8c908a; font-size: 10px; font-weight: 600; letter-spacing: .22em; }
.opening h1 { white-space: pre-line; margin: 58px 0 25px; font-family: 'Noto Serif SC', 'Songti SC', 'STSong', serif; font-size: clamp(40px, 4.1vw, 66px); font-weight: 400; letter-spacing: -.055em; line-height: 1.34; color: #222520; }
.opening-copy > p { max-width: 360px; margin: 0; color: #777d75; line-height: 2; font-size: 14px; }
.opening-actions { display: flex; align-items: center; gap: 24px; margin-top: 34px; }
.opening-actions :deep(.ant-btn-primary) { height: 42px; padding: 0 24px; background: #35443a; box-shadow: none; }
.plain-link { padding: 8px 0; border: 0; border-bottom: 1px solid #9da59b; background: none; color: #3c4a3f; cursor: pointer; font-size: 13px; }
.opening-foot { display: flex; justify-content: space-between; gap: 16px; width: 100%; margin-top: auto; padding-top: 45px; color: #a1a49e; font-size: 10px; letter-spacing: .12em; }
.featured { display: block; position: relative; min-width: 0; height: 510px; padding: 0; border: 0; overflow: hidden; background: #d8d9d5; cursor: pointer; text-align: left; }
.featured > img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform .8s; }
.featured:hover > img { transform: scale(1.035); }
.featured-shade { position: absolute; inset: 35% 0 0; background: linear-gradient(transparent, rgba(0,0,0,.54)); }
.featured-label { position: absolute; top: 24px; left: 28px; right: 28px; display: flex; justify-content: space-between; color: white; font-size: 10px; letter-spacing: .17em; }
.featured-caption { position: absolute; bottom: 28px; left: 30px; right: 30px; display: flex; flex-direction: column; gap: 8px; color: white; }
.featured-caption strong { font-family: 'Noto Serif SC', 'Songti SC', serif; font-size: 29px; font-weight: 400; }
.featured-caption small { font-size: 12px; color: rgba(255,255,255,.82); }
.opening-visual { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 36px; height: 510px; background: #e8e9e4; color: #707a70; letter-spacing: .2em; font-size: 10px; }
.opening-visual strong { color: #58675d; font-family: 'Noto Serif SC', 'Songti SC', serif; font-size: clamp(32px, 4vw, 60px); font-weight: 400; letter-spacing: .3em; }
.feed { padding-top: 35px; }
.feed-heading { display: flex; justify-content: space-between; align-items: end; gap: 22px; padding-bottom: 30px; border-bottom: 1px solid #e7e8e3; margin-bottom: 30px; }
.feed-heading h2 { margin: 8px 0 3px; color: #252a25; font-family: 'Noto Serif SC', 'Songti SC', serif; font-size: 32px; font-weight: 400; }
.feed-heading p { margin: 0; color: #969994; font-size: 13px; }
.feed-tabs { display: flex; gap: 6px; }
.feed-tabs button { padding: 8px 15px; background: none; border: none; color: #9a9e98; cursor: pointer; font-size: 13px; }
.feed-tabs button.active { color: #263a2e; border-bottom: 1px solid #263a2e; }
.loading-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 28px; }
.loading-grid > div { height: 340px; }
.empty-feed { padding: 90px 20px; text-align: center; color: #686f67; }
.empty-feed strong { color: #283229; font-family: 'Noto Serif SC', 'Songti SC', serif; font-size: 24px; font-weight: 400; }
.empty-feed p { margin: 10px 0 24px; }
.feed-more { display: flex; justify-content: center; padding-top: 55px; }
.feed-more button { padding: 12px 25px; border: 1px solid #c8cec6; background: transparent; color: #425047; cursor: pointer; }
@media (max-width: 900px) { .opening { grid-template-columns: 1fr; padding-top: 36px; gap: 36px; } .opening h1 { margin-top: 36px; } .opening-foot { margin-top: 20px; } .featured, .opening-visual { height: 430px; } }
@media (max-width: 600px) { .opening { padding-bottom: 45px; } .opening h1 { font-size: 42px; } .featured, .opening-visual { height: 350px; } .feed-heading { align-items: start; flex-direction: column; } .loading-grid { grid-template-columns: repeat(2,1fr); } }
</style>
