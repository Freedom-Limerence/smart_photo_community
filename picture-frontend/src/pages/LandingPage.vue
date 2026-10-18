<template>
  <div class="landing">
    <!-- ① 动态背景层：真实作品 Ken Burns 交叉轮播 + 色调遮罩 -->
    <div class="hero-media" aria-hidden="true">
      <div v-for="(w, i) in heroSlides" :key="w.id" class="hero-slide"
           :style="{ backgroundImage: 'url(' + thumbOf(w) + ')', animationDuration: (5.4 * heroSlides.length) + 's', animationDelay: (5.4 * i) + 's' }" />
      <div class="hero-veil" />
    </div>

    <!-- ② 呼吸光晕 + 星尘 + 鼠标光斑 -->
    <div class="landing-aurora" aria-hidden="true" />
    <HomeParticles />
    <div v-if="!reduced" class="spotlight" aria-hidden="true" :style="{ '--mx': mx + 'px', '--my': my + 'px' }" />

    <!-- ③ 顶部最小导航 -->
    <header class="landing-top">
      <span class="landing-brand">
        <span class="mark">光</span>
        <span class="word">光影集<em>PHOTOGRAPHY COMMUNITY</em></span>
      </span>
      <nav class="landing-nav">
        <button type="button" class="ghost" @click="enterGuest">以游客身份进入 <ArrowRightOutlined /></button>
        <a-button type="primary" @click="goLogin">登录 / 注册</a-button>
      </nav>
    </header>

    <!-- ④ 动态首屏：逐行动力学宣言 + 滚动视差 -->
    <section class="landing-hero" :style="parallax">
      <p class="landing-manifesto">
        <span class="kline" style="--d: .05s"><span>我们为热爱摄影的人，</span></span>
        <span class="kline" style="--d: .28s"><span><em>建造</em>一间永不打烊的画廊。</span></span>
      </p>
      <p class="landing-sub" v-reveal="380">
        保存原图与拍摄信息，把一组影像发布成作品，<br />让它被看见、被讨论、被策展。
      </p>
      <div class="landing-cta" v-reveal="520">
        <a-button type="primary" size="large" class="enter-btn" @click="enterGuest">
          进入社区 <ArrowRightOutlined />
        </a-button>
        <a-button size="large" class="ghost-btn" @click="goLogin">登录后发布作品</a-button>
      </div>
      <div class="hero-index" v-reveal="640">
        <span><b>{{ total }}</b> 件作品</span>
        <span><b>{{ heroSlides.length }}</b> 幅正在显影</span>
        <span class="live"><i />LIVE</span>
      </div>
      <span class="scroll-cue" aria-hidden="true">向下滚动</span>
    </section>

    <!-- ⑤ 首屏滚动字幕（立即能看到的动效） -->
    <div class="landing-marquee" aria-hidden="true">
      <div class="marquee-track">
        <span v-for="n in 2" :key="n">
          风光 · 人像 · 街拍 · 建筑 · 夜景 · 静物 · 纪实 · 策展 ·
          风光 · 人像 · 街拍 · 建筑 · 夜景 · 静物 · 纪实 · 策展 ·
        </span>
      </div>
    </div>

    <!-- ⑥ 精选作品：clip 揭示 + 3D 倾斜 + 视差 -->
    <section class="landing-featured">
      <header class="featured-head">
        <h2>正在被看见的<em>影像</em></h2>
        <button type="button" class="ghost" @click="enterGuest">查看全部 <ArrowRightOutlined /></button>
      </header>
      <div class="featured-grid">
        <article v-for="(w, i) in featured" :key="w.id" class="feat" :class="i === 0 ? 'wide' : ''"
                 v-tilt="6" v-reveal:clip="(i % 4) * 90" @click="enterGuest">
          <img :src="thumbOf(w)" :alt="w.title || ''" loading="lazy" />
          <div class="feat-cap"><strong>{{ w.title || '未命名作品' }}</strong><span>{{ w.author?.userName || '摄影师' }}</span></div>
        </article>
      </div>
    </section>

    <!-- ⑦ 能力列表 + 收尾 CTA -->
    <section class="landing-capabilities">
      <h2>这里能做的<em>四件事</em></h2>
      <ul class="cap-list">
        <li v-for="(c, i) in capabilities" :key="c.title" v-reveal="i * 90">
          <span class="cap-no">0{{ i + 1 }}</span>
          <div><strong>{{ c.title }}</strong><p>{{ c.desc }}</p></div>
        </li>
      </ul>
    </section>

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
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
import HomeParticles from '@/components/home/HomeParticles.vue'
import { listWorks } from '@/api/photoCommunity'

const router = useRouter()
const featured = ref<any[]>([])
const allWorks = ref<any[]>([])
const total = ref(0)
const year = new Date().getFullYear()
const reduced = typeof window !== 'undefined' && typeof window.matchMedia === 'function'
  ? window.matchMedia('(prefers-reduced-motion: reduce)').matches : false

// 鼠标光斑 + 滚动视差
const mx = ref(-500)
const my = ref(-500)
const scrollY = ref(0)
let raf = 0

const heroSlides = computed(() => (allWorks.value.length ? allWorks.value : featured.value).slice(0, 5))
const parallax = computed(() => (reduced ? {} : {
  transform: 'translate3d(0,' + Math.min(scrollY.value * 0.12, 120) + 'px,0)',
  opacity: String(Math.max(0, 1 - scrollY.value / 720)),
}))

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

function onMove(e: MouseEvent) {
  if (raf) return
  raf = requestAnimationFrame(() => {
    mx.value = e.clientX
    my.value = e.clientY
    raf = 0
  })
}
function onScroll() { scrollY.value = window.scrollY || document.documentElement.scrollTop || 0 }

onMounted(async () => {
  try {
    const res: any = await listWorks({ current: 1, size: 8, sort: 'hot' })
    if (res.data.code === 0) {
      const recs = (res.data.data?.records || []).filter((w: any) => thumbOf(w))
      allWorks.value = recs
      featured.value = recs.slice(0, 5)
      total.value = Number(res.data.data?.total || recs.length)
    }
  } catch { /* 保持空态 */ }
  if (!reduced) {
    window.addEventListener('mousemove', onMove, { passive: true })
    window.addEventListener('scroll', onScroll, { passive: true })
  }
})
onBeforeUnmount(() => {
  window.removeEventListener('mousemove', onMove)
  window.removeEventListener('scroll', onScroll)
  if (raf) cancelAnimationFrame(raf)
})
</script>

<style scoped>
.landing { position: relative; min-height: 100vh; background: var(--bg); color: var(--text-1); overflow-x: hidden; }

/* ① 背景轮播 */
.hero-media { position: absolute; inset: 0 0 auto 0; height: 100svh; overflow: hidden; }
.hero-slide { position: absolute; inset: 0; background-size: cover; background-position: center;
  opacity: 0; transform: scale(1.04); animation-name: heroCycle; animation-timing-function: ease-in-out; animation-iteration-count: infinite; will-change: opacity, transform; }
@keyframes heroCycle {
  0%   { opacity: 0; transform: scale(1.03); }
  4%   { opacity: 1; transform: scale(1.04); }
  18%  { opacity: 1; transform: scale(1.09); }
  24%  { opacity: 0; transform: scale(1.1); }
  100% { opacity: 0; transform: scale(1.1); }
}
.hero-veil { position: absolute; inset: 0;
  background: linear-gradient(105deg, rgba(11,11,12,.94) 0%, rgba(11,11,12,.78) 42%, rgba(11,11,12,.42) 100%),
              linear-gradient(to top, rgba(11,11,12,.96), transparent 55%); }

/* ② 氛围层 */
.landing-aurora { position: absolute; left: -10%; right: -10%; top: -30%; height: 110vh; pointer-events: none;
  background: radial-gradient(60% 50% at 50% 0%, color-mix(in srgb, var(--accent) 24%, transparent), transparent 70%);
  filter: blur(50px); animation: auroraBreathe 14s ease-in-out infinite; }
@keyframes auroraBreathe { 0%,100% { opacity: .55; transform: scale(1) } 50% { opacity: .9; transform: scale(1.06) } }
.spotlight { position: absolute; inset: 0; pointer-events: none; z-index: 1;
  background: radial-gradient(340px circle at var(--mx) var(--my), color-mix(in srgb, var(--accent) 16%, transparent), transparent 70%);
  transition: background-position .1s linear; }

/* ③ 顶部 */
.landing-top { position: relative; z-index: 4; display: flex; align-items: center; justify-content: space-between;
  gap: 24px; padding: 22px clamp(20px, 4vw, 56px); }
.landing-brand { display: inline-flex; align-items: center; gap: 10px; }
.landing-brand .mark { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 50%;
  border: 1px solid var(--border-strong); font-family: var(--font-display); }
.landing-brand .word { display: flex; flex-direction: column; font-family: var(--font-display); font-size: 15px; letter-spacing: .04em; }
.landing-brand em { font-style: normal; font-size: 8px; letter-spacing: .22em; color: var(--text-3); }
.landing-nav { display: flex; align-items: center; gap: 10px; }

/* ④ 首屏 */
.landing-hero { position: relative; z-index: 3; display: flex; flex-direction: column; align-items: flex-start;
  justify-content: center; gap: 22px; min-height: calc(100svh - 96px); padding: 0 clamp(20px, 6vw, 96px) 96px;
  transition: opacity .1s linear; }
.landing-manifesto { margin: 0; font-family: var(--font-display); font-weight: 400; letter-spacing: -.02em;
  font-size: clamp(30px, 5.2vw, 78px); line-height: 1.2; max-width: 26ch; }
.landing-manifesto em { font-style: normal; color: var(--accent-hover); }
.kline { display: block; overflow: hidden; }
.kline > span { display: block; transform: translateY(105%) skewY(4deg); animation: klineUp .95s cubic-bezier(.16,1,.3,1) forwards; animation-delay: var(--d); }
@keyframes klineUp { to { transform: none; } }
.landing-sub { margin: 0; max-width: 46ch; color: var(--text-2); font-size: clamp(14px, 1.15vw, 17px); line-height: 1.9; }
.landing-cta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.enter-btn { height: 48px; padding: 0 26px; border-radius: 999px; }
.enter-btn:hover { transform: translateY(-2px); }
.ghost-btn { height: 48px; padding: 0 22px; border-radius: 999px; background: transparent; }
.ghost { display: inline-flex; align-items: center; gap: 6px; border: 0; background: none; color: var(--text-2);
  font-size: 13px; cursor: pointer; padding: 8px 4px; border-radius: 8px; transition: color .22s var(--ease); }
.ghost:hover { color: var(--text-1); }
.ghost:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
.hero-index { display: flex; align-items: center; gap: 22px; flex-wrap: wrap; color: var(--text-3); font-size: 12px; letter-spacing: .06em; }
.hero-index b { color: var(--text-1); font-weight: 500; font-family: var(--font-display); font-size: 18px; margin-right: 4px; }
.hero-index .live { display: inline-flex; align-items: center; gap: 6px; color: var(--accent-hover); }
.hero-index .live i { width: 6px; height: 6px; border-radius: 50%; background: var(--accent-hover); animation: pulse 2s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity: 1; transform: scale(1) } 50% { opacity: .35; transform: scale(.7) } }
.scroll-cue { position: absolute; left: clamp(20px, 6vw, 96px); bottom: 26px; font-size: 11px; letter-spacing: .18em; color: var(--text-3); }
.scroll-cue::after { content: ''; display: block; width: 1px; height: 30px; margin-top: 8px;
  background: linear-gradient(to bottom, var(--border-strong), transparent); animation: drip 2.4s ease-in-out infinite; }
@keyframes drip { 0%,100% { opacity: .3; transform: scaleY(.6) } 50% { opacity: 1; transform: scaleY(1) } }

/* ⑤ 滚动字幕 */
.landing-marquee { position: relative; z-index: 3; border-top: 1px solid var(--border); border-bottom: 1px solid var(--border); overflow: hidden; padding: 14px 0; background: color-mix(in srgb, var(--bg) 72%, transparent); backdrop-filter: blur(6px); }
.marquee-track { display: flex; width: max-content; animation: slide 34s linear infinite; }
.marquee-track span { padding-right: 32px; font-family: var(--font-display); font-size: clamp(16px, 1.6vw, 22px);
  color: var(--text-3); letter-spacing: .12em; white-space: nowrap; }
@keyframes slide { from { transform: translateX(0) } to { transform: translateX(-50%) } }

/* ⑥⑦ 区块 */
.landing-featured, .landing-capabilities, .landing-end { position: relative; z-index: 3; padding: clamp(56px, 8vw, 120px) clamp(20px, 6vw, 96px); }
.featured-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: 28px; }
.landing-featured h2, .landing-capabilities h2, .landing-end h2 { margin: 0; font-family: var(--font-display); font-weight: 400;
  font-size: clamp(26px, 3.4vw, 52px); letter-spacing: -.02em; text-wrap: balance; }
.landing-featured h2 em, .landing-capabilities h2 em, .landing-end h2 em { font-style: normal; color: var(--accent-hover); }
.featured-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 3px; }
.feat { position: relative; grid-column: span 2; overflow: hidden; background: var(--surface-2); cursor: pointer; transform-style: preserve-3d; }
.feat.wide { grid-column: span 3; }
.feat img { display: block; width: 100%; height: clamp(160px, 21vw, 320px); object-fit: cover; transition: transform .7s var(--ease), filter .4s; }
.feat:hover img { transform: scale(1.03); filter: brightness(.9); }
.feat-cap { position: absolute; left: 0; right: 0; bottom: 0; display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
  padding: 30px 14px 12px; background: linear-gradient(to top, rgba(11,11,12,.82), transparent); opacity: 0; transform: translateY(6px);
  transition: opacity .3s var(--ease), transform .3s var(--ease); }
.feat:hover .feat-cap, .feat.wide .feat-cap { opacity: 1; transform: none; }
.feat-cap strong { font-size: 14px; font-weight: 500; }
.feat-cap span { font-size: 11px; color: var(--text-3); }
.cap-list { list-style: none; margin: 32px 0 0; padding: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.cap-list li { display: flex; gap: 18px; padding: 26px 0; border-top: 1px solid var(--border); }
.cap-no { color: var(--accent-hover); font-size: 12px; letter-spacing: .12em; padding-top: 4px; }
.cap-list strong { display: block; font-size: 17px; font-weight: 500; margin-bottom: 6px; }
.cap-list p { margin: 0; color: var(--text-2); font-size: 13.5px; line-height: 1.85; max-width: 40ch; }
.landing-end { text-align: center; padding-bottom: 64px; }
.landing-end .landing-cta { justify-content: center; margin-top: 26px; }
.landing-foot { margin: 56px 0 0; color: var(--text-3); font-size: 11px; letter-spacing: .16em; }

@media (prefers-reduced-motion: reduce) {
  .hero-slide { animation: none; opacity: 1; }
  .hero-slide + .hero-slide { display: none; }
  .landing-aurora, .marquee-track, .scroll-cue::after, .hero-index .live i { animation: none; }
  .kline > span { transform: none; animation: none; }
}
@media (max-width: 900px) {
  .landing-nav .ghost { display: none; }
  .featured-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .feat, .feat.wide { grid-column: span 1; }
  .cap-list { grid-template-columns: 1fr; }
  .scroll-cue { display: none; }
  .landing-hero { min-height: auto; padding-top: 40px; }
}
</style>
