<template>
  <div class="discover-page" :class="'mode-' + mode">
    <!-- 顶部极光光雾：暗色画廊的"动态背景"层（缓慢漂移，纯氛围） -->
    <div class="aurora" aria-hidden="true"></div>

    <!-- 品牌宣言开场（Lusion 式）：三行大字陈述 + 粒子星尘 -->
    <!-- 品牌宣言只属于首页；社区页以内容开场，避免两个入口看到同一画面 -->
    <section v-if="mode === 'home'" class="manifesto container">
      <HomeParticles />
      <p class="manifesto-line" v-reveal>
        我们为热爱摄影的人，<br />
        <em><span class="nb">建造</span>一间永不打烊的画廊</em>。
      </p>
    </section>
    <!-- 每日精选：按日期确定性轮换，每天一位主角 -->
    <section v-if="mode === 'home' && dailyPick" class="daily-pick container" v-reveal>
      <transition name="backdropfade" appear>
        <img class="dp-cover" :src="dailyCover" :alt="dailyPick.title || '每日精选'" />
      </transition>
      <div class="dp-veil" aria-hidden="true"></div>
      <div class="dp-copy">
        <span class="dp-eyebrow">DAILY PICK / 每日精选</span>
        <h2>{{ dailyPick.title || '未命名作品' }}</h2>
        <p class="dp-meta">{{ dailyPick.author?.userName || '摄影师' }} · {{ todayText }}</p>
        <p class="dp-desc">{{ dailyPick.description || '今天，让它成为你的第一眼。' }}</p>
        <button type="button" class="dp-btn" @click="openWork(dailyPick)">
          欣赏今日之选 <ArrowRightOutlined />
        </button>
      </div>
    </section>
    <ContactSheet v-if="mode === 'home'" :works="featuredWorks" :loading="loading && !works.length" @open="openWork" />
    <section id="works-feed" class="feed container" style="position:relative; z-index:1;">
      <header v-reveal class="feed-heading">
        <div class="feed-intro">
          <span class="feed-no" aria-hidden="true">N°01</span>
          <div>
            <h1 v-if="mode === 'community'">每一种<em>视角</em>，都值得被看见。</h1>
            <h2 v-else>在光影里，<em>遇见</em>彼此。</h2>
            <p>{{ mode === 'community' ? (total ? total + ' 件公开作品 · ' : '') + '走进摄影师的世界，发现另一面。' : '从城市漫游到自然光景，看看大家正在记录什么。' }}</p>
          </div>
        </div>
        <div class="feed-tabs" role="group" aria-label="作品排序与范围">
          <button v-for="item in tabs" :key="item.value" type="button" :aria-pressed="activeTab === item.value"
                  :class="{ active: activeTab === item.value }" @click="selectTab(item.value)">{{ item.label }}</button>
        </div>
      </header>
      <div v-if="error" class="feed-error" role="alert"><span>{{ error }}</span><a-button @click="fetchWorks(current)">重新加载</a-button></div>
      <CardSkeleton v-if="loading && !works.length" variant="masonry" :count="6" ratio="72%" />
      <ContactFeed v-else-if="works.length" :works="works" @open="openWork" />
      <EmptyState v-else-if="!error" :title="activeTab === 'following' ? '关注喜欢的摄影师，让灵感在这里相遇' : '社区还在等待第一组作品'"
                  description="分享你眼里的世界，开启新的对话。">
        <a-button type="primary" @click="router.push('/me/create-work')">发布作品</a-button>
      </EmptyState>
      <div v-if="works.length && works.length < total" class="feed-more">
        <a-button size="large" :loading="loading" @click="fetchWorks(current + 1)">继续发现 <ArrowDownOutlined /></a-button>
      </div>
      <div v-else-if="works.length" class="feed-end"><span />每一次停留，都是新的发现<span /></div>
    </section>
    <Lightbox :works="works" v-model:open="lightboxOpen" v-model:index="lightboxIndex" @detail="openDetail" />
    <section v-reveal class="curation-link container">
      <div><h2>让作品之间，<em>发生对话</em>。</h2><p>从一组影像出发，走进不同的摄影主题。</p></div>
      <router-link to="/exhibitions">走进专题展览 <ArrowRightOutlined /></router-link>
    </section>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowDownOutlined, ArrowRightOutlined } from '@ant-design/icons-vue'
import ContactFeed from '@/components/home/ContactFeed.vue'
import HomeScrollytelling from '@/components/home/HomeScrollytelling.vue'
import HomeParticles from '@/components/home/HomeParticles.vue'
import ContactSheet from '@/components/home/ContactSheet.vue'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import Lightbox from '@/components/common/Lightbox.vue'
import { listWorks } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { loadPhotographyPreview } from '@/utils/photographyPreview'
withDefaults(defineProps<{ mode?: 'home' | 'community' }>(), { mode: 'home' })
const router = useRouter()
const login = useLoginUserStore()
const works = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const loading = ref(true)
const error = ref('')
const activeTab = ref('latest')
const tabs = [{value:'latest',label:'最新发现'}, {value:'hot',label:'热门作品'}, {value:'following',label:'我的关注'}]
const featuredWorks = computed(() => works.value.filter(w => w.coverUrl || w.pictures?.[0]?.url).slice(0,6))
const marqueeWords = ['人像', '街拍', '长曝光', '山与海', '人文纪事', '夜景', '胶片', '晨光', '几何', '自然光']
let fetchVersion = 0
/** 每日精选：按年中的第几天确定性轮换，每天一位主角 */
const dailyPick = computed(() => {
  const pool = (works.value.length ? works.value : featuredWorks.value).slice(0, 12)
  if (!pool.length) return null
  const day = Math.floor(Date.now() / 86400000)
  return pool[day % pool.length]
})
const dailyCover = computed(() => {
  const w = dailyPick.value
  if (!w) return ''
  const p = w.pictures?.[0]
  return p?.url || p?.thumbnailUrl || w.coverUrl || ''
})
const todayText = computed(() => new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }) + ' 精选')

/** 点卡片开灯箱（沉浸看图 + 键盘切换）；详情从灯箱进入 */
const lightboxOpen = ref(false)
const lightboxIndex = ref(0)
function openWork(work: any) {
  const i = works.value.findIndex((w) => w.id === work?.id)
  if (i >= 0) { lightboxIndex.value = i; lightboxOpen.value = true; return }
  if (work?.id) router.push('/works/' + work.id)
}
function openDetail(w: any) { lightboxOpen.value = false; if (w?.id) router.push('/works/' + w.id) }
function selectTab(value: string) {
  if(value === 'following' && !login.loginUser?.id) { message.info('登录后可以查看关注的摄影师'); router.push({path:'/user/login',query:{redirect:'/community'}}); return }
  if(activeTab.value === value) return
  activeTab.value = value
  void fetchWorks(1)
}
async function fetchWorks(page = 1) {
  const version = ++fetchVersion
  loading.value = true
  error.value = ''
  try {
    const preview = await loadPhotographyPreview()
    if(preview) { works.value = [...preview.demoWorks]; total.value = works.value.length; current.value=1; return }
    const res: any = await listWorks({current:page,size:16,sort:activeTab.value==='hot'?'hot':'latest',feed:activeTab.value==='following'?'following':'all'})
    if(version !== fetchVersion) return
    if(res.data.code !== 0) throw new Error(res.data.message || '加载失败')
    const records = res.data.data?.records || []
    works.value = page === 1 ? records : [...works.value,...records]
    total.value = Number(res.data.data?.total || 0)
    current.value = page
  } catch(e: any) { if(version === fetchVersion) error.value = e?.message || '作品暂时无法加载，请稍后重试。' }
  finally { if(version === fetchVersion) loading.value=false }
}
onMounted(() => fetchWorks())
</script>
<style scoped>
/* 社区页：没有大宣言，内容提前进场 */
.mode-community .feed { padding-top: clamp(24px, 4vw, 48px); }
.mode-community .aurora { opacity: .55; }

.discover-page { position:relative; padding-bottom:var(--space-8); }
/* 品牌宣言（Lusion 式开场）：大字陈述 + 星尘粒子在字后漂浮 */
.manifesto { position:relative; padding-top:clamp(14px,3vh,40px); padding-bottom:clamp(10px,2.2vh,26px); }
.manifesto-line {
  position:relative; z-index:1;
  margin:0; max-width:22ch;
  font:400 clamp(30px,4.2vw,66px)/1.28 var(--font-display);
  letter-spacing:-.02em;
  color:var(--text-1);
  text-wrap:balance;
}
.manifesto-line { text-wrap: balance; }
.manifesto-line .nb { white-space: nowrap; }
.manifesto-line em { font-style:normal; color:var(--accent-hover); }
/* 极光光雾：两只柔光缓慢呼吸漂移（motionsites 式动态背景，subtle 不抢戏） */
.aurora {
  position:absolute; top:-140px; left:0; right:0; height:560px;
  pointer-events:none; z-index:0; overflow:hidden;
  opacity:.85;
}
.aurora::before, .aurora::after {
  content:''; position:absolute; width:640px; height:640px; border-radius:50%;
  filter:blur(90px); will-change:transform;
}
.aurora::before {
  left:8%; top:-120px;
  background:radial-gradient(circle, rgba(143,163,148,.16), transparent 65%);
  animation: aurora-a 26s ease-in-out infinite alternate;
}
.aurora::after {
  right:6%; top:-180px;
  background:radial-gradient(circle, rgba(194,168,120,.10), transparent 65%);
  animation: aurora-b 32s ease-in-out infinite alternate;
}
@keyframes aurora-a { from { transform:translate(0,0) scale(1); } to { transform:translate(120px,60px) scale(1.15); } }
@keyframes aurora-b { from { transform:translate(0,0) scale(1.1); } to { transform:translate(-100px,40px) scale(.95); } }
@media (prefers-reduced-motion: reduce) { .aurora::before, .aurora::after { animation:none; } }
.feed { padding-top:clamp(36px,5vw,80px); }
.feed-intro h1, .feed-intro h2 { text-wrap: balance; }
.feed-heading { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:36px; }
/* 编辑式大字（视觉冲击力专项）：章节编号 + 超大衬线标题 + 强调字 */
.feed-intro { display:flex; align-items:flex-start; gap:clamp(16px,2vw,32px); min-width:0; }
.feed-no {
  flex:0 0 auto; margin-top:clamp(8px,1vw,18px);
  font:400 clamp(13px,1.2vw,16px)/1 var(--font-display);
  letter-spacing:.22em; color:var(--text-3);
  writing-mode:vertical-rl;
  padding-left:10px; border-left:1px solid var(--border-strong);
}
.feed-heading h1,.feed-heading h2 { margin:0; font:400 clamp(38px,5.5vw,84px)/1.12 var(--font-display); letter-spacing:-.03em; text-wrap:balance; }
.feed-heading em { font-style:normal; color:var(--accent-hover); }
.feed-heading p { color:var(--text-2); margin:14px 0 0; max-width:38ch; }
.feed-tabs { display:flex; flex-shrink:0; gap:22px; }
.feed-tabs button { border:0; border-bottom:2px solid transparent; padding:12px 0; background:none; color:var(--text-2); cursor:pointer; font:inherit; white-space:nowrap; }
.feed-tabs button.active { color:var(--accent-ink); border-color:var(--accent-ink); }
.feed-error { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:24px; padding:20px; background:var(--danger-soft); color:var(--text-1); border-radius:12px; }
.feed-more { text-align:center; padding-top:40px; }
.feed-end { display:flex; align-items:center; justify-content:center; gap:16px; margin-top:48px; color:var(--text-3); font-size:12px; }
.feed-end span { width:32px; height:1px; background:var(--border-strong); }
.curation-link { display:flex; justify-content:space-between; align-items:center; gap:24px; margin-top:64px; padding-top:40px; border-top:1px solid var(--border-strong); }
.curation-link h2 { font:400 clamp(30px,3.6vw,56px)/1.25 var(--font-display); letter-spacing:-.02em; margin:0; }
.curation-link h2 em { font-style:normal; color:var(--accent-hover); }
.curation-link p { color:var(--text-2); margin:10px 0 0; }
.curation-link a { display:flex; gap:24px; align-items:center; padding:14px 0; border-bottom:1px solid var(--text-1); white-space:nowrap; }
@media(max-width:768px) { .feed-heading { flex-direction:column; align-items:flex-start; gap:16px; }.feed-tabs { width:100%; gap:26px; border-bottom:1px solid var(--border); }.curation-link { align-items:flex-start; flex-direction:column; margin-top:40px; }.feed-heading p { font-size:13px; }.feed-no { display:none; } }
/* 无限跑马灯带（motionsites 式氛围件）：悬停减速 */
.marquee {
  margin-top: clamp(56px, 7vw, 110px);
  overflow: hidden;
  border-block: 1px solid var(--border);
  padding: clamp(14px, 1.6vw, 24px) 0;
  -webkit-mask-image: linear-gradient(90deg, transparent, rgba(0, 0, 0, 1) 8%, rgba(0, 0, 0, 1) 92%, transparent);
  mask-image: linear-gradient(90deg, transparent, rgba(0, 0, 0, 1) 8%, rgba(0, 0, 0, 1) 92%, transparent);
}
.marquee-track { display: flex; width: max-content; animation: marquee-roll 30s linear infinite; }
.marquee:hover .marquee-track { animation-play-state: paused; }
.marquee-seq { display: flex; align-items: center; flex: 0 0 auto; }
.marquee-seq em {
  font: 400 clamp(20px, 2.6vw, 38px)/1 var(--font-display);
  font-style: normal;
  color: var(--text-3);
  letter-spacing: .04em;
  white-space: nowrap;
}
.marquee-seq i { font-style: normal; color: var(--accent); font-size: .5em; margin: 0 clamp(18px, 2.4vw, 40px); transform: translateY(-4px); }
@keyframes marquee-roll { to { transform: translateX(-50%); } }
@media (prefers-reduced-motion: reduce) { .marquee-track { animation: none; } }

/* 每日精选 */
.daily-pick { position: relative; z-index: 1; margin-top: clamp(18px, 2.6vw, 34px); height: clamp(280px, 42vh, 460px); border-radius: 14px; overflow: hidden; border: 1px solid var(--border); }
.dp-cover { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.dp-veil { position: absolute; inset: 0; background: linear-gradient(100deg, rgba(4,5,8,.86) 0%, rgba(4,5,8,.55) 46%, rgba(4,5,8,.12) 100%); }
.dp-copy { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: center; gap: 10px; padding: clamp(20px, 4vw, 52px); max-width: 60%; }
.dp-eyebrow { color: var(--accent-hover); font-size: 12px; letter-spacing: .22em; }
.dp-copy h2 { margin: 0; color: var(--text-1); font-family: var(--font-display); font-weight: 400; font-size: clamp(22px, 3vw, 40px); letter-spacing: -.01em; text-shadow: 0 2px 20px rgba(0,0,0,.5); }
.dp-meta { margin: 0; color: var(--text-2); font-size: 13px; }
.dp-desc { margin: 0; color: var(--text-2); font-size: 14px; max-width: 46ch; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.dp-btn { margin-top: 8px; align-self: flex-start; display: inline-flex; align-items: center; gap: 8px; padding: 10px 22px; border-radius: 999px; border: 1px solid rgba(255,255,255,.2); background: rgba(10,12,10,.55); color: var(--text-1); cursor: pointer; font-size: 13.5px; letter-spacing: .05em; transition: border-color .25s, background .25s; }
.dp-btn:hover { border-color: rgba(163,182,168,.65); background: rgba(20,24,20,.7); }
@media (max-width: 720px) { .dp-copy { max-width: 100%; } }
</style>
