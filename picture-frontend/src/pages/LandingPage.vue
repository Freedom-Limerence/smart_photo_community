<template>
  <div class="landing">
    <!-- 极光 + 星尘：入口页的氛围层 -->
    <div class="landing-aurora" aria-hidden="true"></div>
    <HomeParticles />

    <!-- 顶部最小导航：只留品牌与进入动作 -->
    <header class="landing-top">
      <a class="landing-brand" href="/" @click.prevent>
        <span class="mark">光</span>
        <span class="word">光影集<em>PHOTOGRAPHY COMMUNITY</em></span>
      </a>
      <nav class="landing-nav">
        <button type="button" class="ghost" @click="enterGuest">以游客身份进入 <ArrowRightOutlined /></button>
        <a-button type="primary" @click="goLogin">登录 / 注册</a-button>
      </nav>
    </header>

    <!-- 电影感首屏 -->
    <section class="landing-hero">
      <p class="landing-manifesto" v-reveal>
        我们为热爱摄影的人，<br />
        <em><span class="nb">建造</span>一间永不打烊的画廊</em>。
      </p>
      <p class="landing-sub" v-reveal="120">
        保存原图与拍摄信息，把一组影像发布成作品，<br />让它被看见、被讨论、被策展。
      </p>
      <div class="landing-cta" v-reveal="200">
        <a-button type="primary" size="large" class="enter-btn" @click="enterGuest">
          进入社区 <ArrowRightOutlined />
        </a-button>
        <a-button size="large" class="ghost-btn" @click="goLogin">登录后发布作品</a-button>
      </div>
      <span class="scroll-cue" aria-hidden="true">向下滚动</span>
    </section>

    <!-- 滚动字幕：轻动效，同时交代内容边界 -->
    <div class="landing-marquee" aria-hidden="true">
      <div class="marquee-track">
        <span v-for="n in 2" :key="n">
          风光 · 人像 · 街拍 · 建筑 · 夜景 · 静物 · 纪实 · 策展 ·
          风光 · 人像 · 街拍 · 建筑 · 夜景 · 静物 · 纪实 · 策展 ·
        </span>
      </div>
    </div>

    <!-- 精选作品：进来先看到照片 -->
    <section class="landing-featured">
      <header class="featured-head">
        <h2>正在被看见的<em>影像</em></h2>
        <button type="button" class="ghost" @click="enterGuest">查看全部 <ArrowRightOutlined /></button>
      </header>
      <div class="featured-grid">
        <article v-for="(w, i) in featured" :key="w.id" class="feat" :class="i === 0 ? 'wide' : ''"
                 v-reveal="i * 90" @click="enterGuest">
          <img :src="thumbOf(w)" :alt="w.title || ''" loading="lazy" />
          <div class="feat-cap"><strong>{{ w.title || '未命名作品' }}</strong><span>{{ w.author?.userName || '摄影师' }}</span></div>
        </article>
      </div>
    </section>

    <!-- 平台能力：用编号列表，不用等大卡片 -->
    <section class="landing-capabilities">
      <h2>这里能做的<em>四件事</em></h2>
      <ul class="cap-list">
        <li v-for="(c, i) in capabilities" :key="c.title" v-reveal="i * 80">
          <span class="cap-no">0{{ i + 1 }}</span>
          <div><strong>{{ c.title }}</strong><p>{{ c.desc }}</p></div>
        </li>
      </ul>
    </section>

    <!-- 收尾 CTA -->
    <section class="landing-end">
      <h2>把照片放进来，<em>让它开始说话</em>。</h2>
      <div class="landing-cta">
        <a-button type="primary" size="large" @click="enterGuest">以游客身份进入 <ArrowRightOutlined /></a-button>
        <a-button size="large" class="ghost-btn" @click="goLogin">注册并发布作品</a-button>
      </div>
      <p class="landing-foot">© {{ year }} 光影集 · 摄影创作社区</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import HomeParticles from '@/components/home/HomeParticles.vue'
import { listWorks } from '@/api/photoCommunity'

const router = useRouter()
const featured = ref<any[]>([])
const year = new Date().getFullYear()

const capabilities = [
  { title: '摄影资产管理', desc: '原图、EXIF、标签与 AI 编目；私有空间与团队空间分开管，容量配额一目了然。' },
  { title: '作品与社区', desc: '把一张或一组图片发布成作品；点赞、收藏、评论与关注，构成完整的交流链路。' },
  { title: '专题与征稿', desc: '发起征稿启事，摄影师投稿，策展人评选采纳，作品自动进入专题作品墙。' },
  { title: '内容审核与治理', desc: '可解释的机审规则 + 人工复核 + 审核流水 + 申诉与举报，全流程可追溯。' },
]

function thumbOf(w: any) {
  const p = w?.pictures?.[0]
  return w?.thumbnailUrl || p?.thumbnailUrl || w?.coverUrl || p?.url || ''
}

function enterGuest() { router.push('/discover') }
function goLogin() { router.push({ path: '/user/login', query: { redirect: '/discover' } }) }

onMounted(async () => {
  try {
    const res: any = await listWorks({ current: 1, size: 5, sort: 'hot' })
    featured.value = res.data.code === 0 ? (res.data.data?.records || []).filter((w: any) => thumbOf(w)) : []
  } catch { featured.value = [] }
})
</script>

<style scoped>
.landing { position: relative; min-height: 100vh; background: var(--bg); color: var(--text-1); overflow-x: hidden; }
.landing-aurora { position: absolute; inset: -20% -10% auto -10%; height: 120vh; pointer-events: none;
  background: radial-gradient(60% 50% at 50% 0%, color-mix(in srgb, var(--accent) 22%, transparent), transparent 70%); filter: blur(40px); }

.landing-top { position: relative; z-index: 3; display: flex; align-items: center; justify-content: space-between;
  gap: 24px; padding: 22px clamp(20px, 4vw, 56px); }
.landing-brand { display: inline-flex; align-items: center; gap: 10px; color: inherit; }
.landing-brand .mark { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 50%;
  border: 1px solid var(--border-strong); font-family: var(--font-display); }
.landing-brand .word { display: flex; flex-direction: column; font-family: var(--font-display); font-size: 15px; letter-spacing: .04em; }
.landing-brand em { font-style: normal; font-size: 8px; letter-spacing: .22em; color: var(--text-3); }
.landing-nav { display: flex; align-items: center; gap: 10px; }

.landing-hero { position: relative; z-index: 2; display: flex; flex-direction: column; align-items: flex-start;
  justify-content: center; gap: 22px; min-height: calc(100svh - 96px); padding: 0 clamp(20px, 6vw, 96px) 64px; }
.landing-manifesto { margin: 0; font-family: var(--font-display); font-weight: 400; letter-spacing: -.02em;
  font-size: clamp(34px, 6.2vw, 92px); line-height: 1.18; text-wrap: balance; max-width: 18ch; }
.landing-manifesto em { font-style: normal; color: var(--accent-hover); }
.landing-manifesto .nb { white-space: nowrap; }
.landing-sub { margin: 0; max-width: 46ch; color: var(--text-2); font-size: clamp(14px, 1.2vw, 17px); line-height: 1.9; }
.landing-cta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.enter-btn { height: 48px; padding: 0 26px; border-radius: 999px; }
.ghost-btn { height: 48px; padding: 0 22px; border-radius: 999px; background: transparent; }
.ghost { display: inline-flex; align-items: center; gap: 6px; border: 0; background: none; color: var(--text-2);
  font-size: 13px; cursor: pointer; padding: 8px 4px; border-radius: 8px; transition: color .22s var(--ease); }
.ghost:hover { color: var(--text-1); }
.ghost:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
.scroll-cue { position: absolute; left: clamp(20px, 6vw, 96px); bottom: 22px; font-size: 11px; letter-spacing: .18em; color: var(--text-3); }
.scroll-cue::after { content: ''; display: block; width: 1px; height: 26px; margin-top: 8px;
  background: linear-gradient(to bottom, var(--border-strong), transparent); }

.landing-marquee { border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); overflow: hidden; padding: 14px 0; }
.marquee-track { display: flex; width: max-content; animation: slide 34s linear infinite; }
.marquee-track span { padding-right: 32px; font-family: var(--font-display); font-size: clamp(16px, 1.6vw, 22px);
  color: var(--text-3); letter-spacing: .12em; white-space: nowrap; }
@keyframes slide { from { transform: translateX(0) } to { transform: translateX(-50%) } }
@media (prefers-reduced-motion: reduce) { .marquee-track { animation: none } }

.landing-featured, .landing-capabilities, .landing-end { position: relative; z-index: 2; padding: clamp(56px, 8vw, 120px) clamp(20px, 6vw, 96px); }
.featured-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: 28px; }
.landing-featured h2, .landing-capabilities h2, .landing-end h2 { margin: 0; font-family: var(--font-display); font-weight: 400;
  font-size: clamp(26px, 3.4vw, 52px); letter-spacing: -.02em; text-wrap: balance; }
.landing-featured h2 em, .landing-capabilities h2 em, .landing-end h2 em { font-style: normal; color: var(--accent-hover); }

.featured-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 3px; }
.feat { position: relative; grid-column: span 2; overflow: hidden; background: var(--surface-2); cursor: pointer; }
.feat.wide { grid-column: span 3; }
.feat img { display: block; width: 100%; height: clamp(160px, 21vw, 320px); object-fit: cover; transition: transform .7s var(--ease), filter .4s; }
.feat:hover img { transform: scale(1.03); filter: brightness(.9); }
.feat-cap { position: absolute; left: 0; right: 0; bottom: 0; display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
  padding: 30px 14px 12px; background: linear-gradient(to top, rgba(11,11,12,.82), transparent); opacity: 0; transform: translateY(6px);
  transition: opacity .3s var(--ease), transform .3s var(--ease); }
.feat:hover .feat-cap, .feat.wide .feat-cap { opacity: 1; transform: none; }
.feat-cap strong { font-size: 14px; font-weight: 500; }
.feat-cap span { font-size: 11px; color: var(--text-3); }

.cap-list { list-style: none; margin: 32px 0 0; padding: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0; }
.cap-list li { display: flex; gap: 18px; padding: 26px 0; border-top: 1px solid var(--border); }
.cap-no { color: var(--accent-hover); font-size: 12px; letter-spacing: .12em; padding-top: 4px; }
.cap-list strong { display: block; font-size: 17px; font-weight: 500; margin-bottom: 6px; }
.cap-list p { margin: 0; color: var(--text-2); font-size: 13.5px; line-height: 1.85; max-width: 40ch; }

.landing-end { text-align: center; padding-bottom: 64px; }
.landing-end .landing-cta { justify-content: center; margin-top: 26px; }
.landing-foot { margin: 56px 0 0; color: var(--text-3); font-size: 11px; letter-spacing: .16em; }

@media (max-width: 900px) {
  .landing-nav .ghost { display: none; }
  .featured-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .feat, .feat.wide { grid-column: span 1; }
  .cap-list { grid-template-columns: 1fr; }
  .scroll-cue { display: none; }
}
</style>
