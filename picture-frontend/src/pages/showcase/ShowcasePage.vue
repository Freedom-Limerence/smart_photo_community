<template>
  <div class="showcase-page">
    <!-- 第一幕：宣言（滚动显影） -->
    <section class="sc-hero container">
      <span class="sc-eyebrow">ABOUT · SHOWCASE</span>
      <h1 class="sc-title" data-scrub-words>一间永不打烊的画廊，是这样建成的。</h1>
      <p class="sc-sub">光影集 —— 毕业设计项目「智能摄影云社区平台」的门面与设计系统展示。这里陈列它的审美、它的令牌、它的构件，和它讲过的每一次光影。</p>
    </section>

    <!-- 第二幕：项目数字（计数滚动） -->
    <section class="sc-stats container">
      <div v-for="s in stats" :key="s.label" v-reveal="s.delay" class="sc-stat">
        <strong><span :ref="(el) => setStatRef(i0of(s), el)">{{ s.value }}</span>{{ s.suffix }}</strong>
        <span>{{ s.label }}</span>
      </div>
    </section>

    <!-- 第三幕：令牌标本墙（clip 揭示 = 本页标志性瞬间） -->
    <section class="sc-section container">
      <header class="sc-head" v-reveal>
        <span class="sc-no">N°01</span>
        <h2>色彩<em>令牌</em></h2>
        <p>全部界面颜色来自设计令牌，散色只减不增（门禁棘轮基线 hex=90）。</p>
      </header>
      <div class="swatch-grid">
        <div v-for="(c, i) in colorTokens" :key="c.name" v-reveal:clip="(i % 4) * 70" class="swatch">
          <div class="swatch-chip" :style="{ background: c.value, borderColor: c.ring }" />
          <strong>{{ c.name }}</strong>
          <code>{{ c.value }}</code>
        </div>
      </div>
    </section>

    <!-- 第四幕：字体与间距 -->
    <section class="sc-section container">
      <header class="sc-head" v-reveal>
        <span class="sc-no">N°02</span>
        <h2>排版<em>尺度</em></h2>
        <p>衬线大字与眉标的极端尺度差，就是层次本身。</p>
      </header>
      <div class="type-specimens">
        <div v-for="t in typeSpecimens" :key="t.label" v-reveal="t.delay" class="type-row">
          <span class="type-label">{{ t.label }} · {{ t.size }}</span>
          <span class="type-demo" :style="{ font: t.font, fontSize: t.px, letterSpacing: t.ls }">{{ t.text }}</span>
        </div>
      </div>
    </section>

    <!-- 第五幕：构件目录 -->
    <section class="sc-section container">
      <header class="sc-head" v-reveal>
        <span class="sc-no">N°03</span>
        <h2>构件<em>目录</em></h2>
        <p>被门面与工具页复用的基础构件，全部走令牌。</p>
      </header>
      <div class="comp-grid">
        <div v-for="(g, i) in components" :key="g.title" v-reveal:clip="(i % 3) * 80" class="comp-card">
          <h3>{{ g.title }}</h3>
          <div class="comp-demo" v-html="g.demo" />
          <p>{{ g.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 第六幕：动效语言（现场演示区） -->
    <section class="sc-section container">
      <header class="sc-head" v-reveal>
        <span class="sc-no">N°04</span>
        <h2>动效<em>语言</em></h2>
        <p>GSAP + Lenis，只动 transform / opacity / filter。下面是活的标本——把鼠标放上去。</p>
      </header>
      <div class="motion-grid">
        <div class="motion-card" data-magnetic>
          <strong>磁吸</strong>
          <span>data-magnetic · 光标靠近时被吸过去</span>
        </div>
        <div class="motion-card" v-tilt="8">
          <strong>3D 倾斜</strong>
          <span>v-tilt · 卡片跟随鼠标透视</span>
        </div>
        <div class="motion-card" data-scrub-fade>
          <strong>滚动显影</strong>
          <span>data-scrub-fade · 你正在滚动触发它</span>
        </div>
      </div>
    </section>

    <!-- 第七幕：技术栈 + 收尾 -->
    <section class="sc-end container">
      <h2 data-scrub-words>用工程的方式，守住审美。</h2>
      <div class="stack-row" v-reveal>
        <span v-for="t in stack" :key="t" class="stack-chip">{{ t }}</span>
      </div>
      <p class="sc-foot">每页收尾三绿：verify（类型 + 设计门禁）· test:smoke（Playwright）· build。散色棘轮 hex 265 → 90。</p>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, type ComponentPublicInstance } from 'vue'

/** 数字滚动（IntersectionObserver 一次性触发，reduced-motion 直显最终值） */
const stats = [
  { value: 65, suffix: '+', label: '次提交（两轮改造）', delay: 0 },
  { value: 12, suffix: '', label: '幅真实摄影作品在线', delay: 70 },
  { value: 9, suffix: '', label: '组动效记忆点', delay: 140 },
  { value: 90, suffix: '', label: '散色棘轮基线（自 265 压下）', delay: 210 },
]
const countRefs = ref<HTMLElement[]>([])
const i0of = (s: { value: number; suffix?: string; label?: string; delay?: number }) => stats.indexOf(s as any)
function setStatRef(i: number, el: Element | ComponentPublicInstance | null) {
  if (el) countRefs.value[i] = el as HTMLElement
}
let counted = false
let io: IntersectionObserver | null = null

function runCount(el: HTMLElement, target: number) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    el.textContent = String(target)
    return
  }
  const start = performance.now()
  const dur = 1100
  const step = (now: number) => {
    const p = Math.min(1, (now - start) / dur)
    el.textContent = String(Math.round(target * (1 - Math.pow(1 - p, 3))))
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

onMounted(() => {
  readTokens()
  io = new IntersectionObserver((entries) => {
    if (counted) return
    if (entries.some((e) => e.isIntersecting)) {
      counted = true
      stats.forEach((s, i) => {
        const el = countRefs.value[i]
        if (el) runCount(el, s.value)
      })
      io?.disconnect()
    }
  }, { threshold: 0.3 })
  const section = document.querySelector('.sc-stats')
  if (section && io) io.observe(section)
})
onBeforeUnmount(() => io?.disconnect())

/** 色卡标本：动态读 tokens 的 computed 值——单一数据源，令牌改了标本自动跟 */
const COLOR_VARS = [
  { name: '画廊底色', cssVar: '--bg' },
  { name: '表面', cssVar: '--surface' },
  { name: '表面·二层', cssVar: '--surface-2' },
  { name: '主文字', cssVar: '--text-1' },
  { name: '鼠尾草', cssVar: '--accent' },
  { name: '鼠尾草·亮', cssVar: '--accent-hover' },
  { name: '强调墨', cssVar: '--accent-ink' },
  { name: '媒体亮字', cssVar: '--text-media' },
  { name: '暖沙', cssVar: '--warning' },
  { name: '柔砖', cssVar: '--danger' },
  { name: '后台·松绿', cssVar: '--admin-sider-bg, --ink-2' },
  { name: '次级文字', cssVar: '--text-2' },
]
const colorTokens = ref(
  COLOR_VARS.map((c) => ({ name: c.name, value: '', ring: 'rgba(128,128,128,.18)' })),
)
function readTokens() {
  const cs = getComputedStyle(document.documentElement)
  colorTokens.value.forEach((t, i) => {
    let v = cs.getPropertyValue(COLOR_VARS[i].cssVar).trim()
    if (!v && COLOR_VARS[i].cssVar.includes(',')) {
      for (const vName of COLOR_VARS[i].cssVar.split(',').map((x) => x.trim())) {
        v = cs.getPropertyValue(vName).trim()
        if (v) break
      }
    }
    if (v) colorTokens.value[i].value = v
  })
}

const typeSpecimens = [
  { label: 'Display', size: 'clamp 84px', font: '400 clamp(40px,6vw,84px)/1.1 var(--font-display)', px: 'clamp(40px,6vw,84px)', ls: '-.03em', text: '在光影里遇见彼此', delay: 0 },
  { label: 'Headline', size: '56px', font: '400 56px/1.15 var(--font-display)', px: '56px', ls: '-.02em', text: '让作品之间发生对话', delay: 60 },
  { label: 'Body', size: '15px', font: '400 15px/1.7 var(--font-sans)', px: '15px', ls: '0', text: '从城市漫游到自然光景，看看大家正在记录什么。', delay: 120 },
  { label: 'Eyebrow', size: '12px', font: '600 12px/1 var(--font-sans)', px: '12px', ls: '.2em', text: 'N°01 — PHOTOGRAPHY COMMUNITY', delay: 180 },
]

const components = [
  {
    title: '作品卡片',
    desc: '两质感体系：暗场印样（画廊）/ 白卡（后台）。hover 层分离——图片下沉、标题浮起。',
    demo: '<div class="dm-card"><div class="dm-thumb"></div><b>雾起时，山把轮廓交还给光</b><span>林间纪事</span></div>',
  },
  {
    title: '按钮',
    desc: '主按钮（鼠尾草实底）与幽灵按钮（描边），CTA 带磁吸。',
    demo: '<div class="dm-btns"><span class="dm-primary">进入社区</span><span class="dm-ghost">了解更多</span></div>',
  },
  {
    title: '标签与徽记',
    desc: '审核态、匹配来源、标签词共用一套胶囊语言。',
    demo: '<div class="dm-tags"><span class="dm-tag on">已过审</span><span class="dm-tag">语义匹配</span><span class="dm-tag">EXIF</span></div>',
  },
  {
    title: '数据看板',
    desc: '衬线大数字 + 同比箭头 + sparkline；计数滚动进场。',
    demo: '<div class="dm-kpi"><b>1,284</b><span>累计作品 ↑ 12%</span></div>',
  },
  {
    title: '空态',
    desc: '圆形图标容器 + 衬线短句 + 一个明确的后续动作。',
    demo: '<div class="dm-empty"><i></i><b>还没有作品</b><span>发布第一组影像</span></div>',
  },
  {
    title: '表格（后台）',
    desc: '紧凑密度 + 吸顶表头 + 行悬停反馈，亮色豁免区的主力构件。',
    demo: '<div class="dm-table"><span>ID</span><span>标题</span><span>状态</span><span>…</span></div>',
  },
]

const stack = ['Vue 3.5', 'Vite', 'Pinia', 'GSAP + Lenis', 'Ant Design Vue 4.2', 'Spring Boot 3.5', 'MyBatis-Plus', 'Sa-Token', 'MySQL', 'Redis', '腾讯云 COS + 数据万象', 'Spring AI Alibaba']
</script>

<style scoped>
.showcase-page { padding-bottom: var(--space-8); }

/* 第一幕 */
.sc-hero { padding-top: clamp(72px, 10vw, 150px); padding-bottom: clamp(40px, 5vw, 80px); }
.sc-eyebrow { display: inline-block; margin-bottom: clamp(14px, 2vw, 24px); color: var(--text-3); font-size: 12px; font-weight: 600; letter-spacing: .22em; }
.sc-title {
  margin: 0; max-width: 18ch;
  font: 400 clamp(38px, 5.5vw, 84px)/1.12 var(--font-display);
  letter-spacing: -.03em; color: var(--text-1); text-wrap: balance;
}
.sc-sub { margin: clamp(18px, 2.4vw, 30px) 0 0; max-width: 52ch; color: var(--text-2); font-size: var(--font-3); line-height: 1.8; }

/* 第二幕：数字 */
.sc-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-4); padding: clamp(24px, 3vw, 48px) var(--gutter); }
.sc-stat { padding: clamp(18px, 2vw, 30px); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); }
.sc-stat strong { display: block; font: 400 clamp(34px, 3.6vw, 56px)/1 var(--font-display); color: var(--text-1); font-variant-numeric: tabular-nums; }
.sc-stat > span { display: block; margin-top: 10px; color: var(--text-3); font-size: var(--font-1); }

/* 通用章节头 */
.sc-section { padding-top: clamp(56px, 7vw, 110px); }
.sc-head { margin-bottom: clamp(24px, 3vw, 44px); }
.sc-no { display: inline-block; margin-bottom: 12px; padding-left: 10px; border-left: 1px solid var(--border-strong); color: var(--text-3); font-family: var(--font-display); font-size: 13px; letter-spacing: .22em; writing-mode: vertical-rl; }
.sc-head h2 { margin: 0 0 10px; font: 400 clamp(30px, 3.6vw, 56px)/1.2 var(--font-display); letter-spacing: -.02em; color: var(--text-1); }
.sc-head h2 em { font-style: normal; color: var(--accent-hover); }
.sc-head p { margin: 0; color: var(--text-2); max-width: 56ch; }

/* 色卡墙 */
.swatch-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: var(--space-4); }
.swatch strong { display: block; margin-top: 10px; font-size: var(--font-2); font-weight: 500; color: var(--text-1); }
.swatch code { display: block; margin-top: 3px; color: var(--text-3); font-size: 12px; }
.swatch-chip { height: clamp(64px, 7vw, 96px); border-radius: var(--radius); border: 1px solid var(--border); }

/* 排版标本 */
.type-specimens { display: flex; flex-direction: column; gap: clamp(16px, 2vw, 28px); border-top: 1px solid var(--border); padding-top: clamp(20px, 2.4vw, 36px); }
.type-row { display: grid; grid-template-columns: 150px minmax(0, 1fr); gap: var(--space-4); align-items: baseline; padding-bottom: clamp(14px, 1.6vw, 24px); border-bottom: 1px solid var(--border); }
.type-label { color: var(--text-3); font-size: var(--font-1); letter-spacing: .08em; }
.type-demo { color: var(--text-1); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 构件目录 */
.comp-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-4); }
.comp-card { padding: clamp(18px, 2vw, 28px); background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); }
.comp-card h3 { margin: 0 0 12px; font: 400 19px/1.3 var(--font-display); color: var(--text-1); }
.comp-card p { margin: 12px 0 0; color: var(--text-3); font-size: var(--font-1); line-height: 1.7; }
.comp-demo { min-height: 84px; display: grid; place-content: center; }

/* 构件标本（内联演示样式） */
.comp-demo :deep(.dm-card) { width: 150px; margin: 0 auto; padding: 8px; background: var(--surface-2); border: 1px solid var(--border); border-radius: 8px; }
.comp-demo :deep(.dm-thumb) { height: 64px; border-radius: 6px; background: linear-gradient(135deg, var(--accent-soft), var(--surface-3)); }
.comp-demo :deep(.dm-card b) { display: block; margin-top: 8px; font-size: 12px; color: var(--text-1); font-weight: 500; }
.comp-demo :deep(.dm-card span) { font-size: 12px; color: var(--text-3); }
.comp-demo :deep(.dm-btns) { display: flex; gap: 8px; justify-content: center; }
.comp-demo :deep(.dm-primary), .comp-demo :deep(.dm-ghost) { padding: 7px 16px; border-radius: 999px; font-size: 12px; }
.comp-demo :deep(.dm-primary) { background: var(--accent); color: var(--accent-contrast); }
.comp-demo :deep(.dm-ghost) { border: 1px solid var(--border-strong); color: var(--text-2); }
.comp-demo :deep(.dm-tags) { display: flex; gap: 6px; justify-content: center; flex-wrap: wrap; }
.comp-demo :deep(.dm-tag) { padding: 4px 10px; border-radius: 999px; font-size: 12px; color: var(--text-3); border: 1px solid var(--border-strong); }
.comp-demo :deep(.dm-tag.on) { background: var(--accent-soft); color: var(--accent-ink); border-color: transparent; }
.comp-demo :deep(.dm-kpi) { text-align: center; }
.comp-demo :deep(.dm-kpi b) { display: block; font: 400 30px/1 var(--font-display); color: var(--text-1); font-variant-numeric: tabular-nums; }
.comp-demo :deep(.dm-kpi span) { font-size: 12px; color: var(--text-3); }
.comp-demo :deep(.dm-empty) { text-align: center; }
.comp-demo :deep(.dm-empty i) { display: inline-grid; place-items: center; width: 36px; height: 36px; border-radius: 50%; background: var(--accent-soft); }
.comp-demo :deep(.dm-empty b) { display: block; margin-top: 6px; font-size: 12px; font-weight: 500; color: var(--text-1); }
.comp-demo :deep(.dm-empty span) { font-size: 12px; color: var(--accent-ink); }
.comp-demo :deep(.dm-table) { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1px; background: var(--border); border: 1px solid var(--border); border-radius: 6px; overflow: hidden; }
.comp-demo :deep(.dm-table span) { background: var(--surface-2); padding: 6px 8px; font-size: 12px; color: var(--text-2); }

/* 动效语言 */
.motion-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: var(--space-4); }
.motion-card {
  padding: clamp(22px, 2.6vw, 40px);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  will-change: transform;
}
.motion-card strong { display: block; font: 400 20px/1.3 var(--font-display); color: var(--text-1); }
.motion-card span { display: block; margin-top: 8px; color: var(--text-3); font-size: var(--font-1); }

/* 收尾 */
.sc-end { padding-top: clamp(64px, 8vw, 130px); text-align: center; }
.sc-end h2 { margin: 0; font: 400 clamp(30px, 4vw, 60px)/1.2 var(--font-display); letter-spacing: -.02em; color: var(--text-1); }
.stack-row { display: flex; flex-wrap: wrap; gap: 10px; justify-content: center; margin-top: clamp(20px, 2.6vw, 36px); }
.stack-chip { padding: 7px 16px; border: 1px solid var(--border-strong); border-radius: 999px; color: var(--text-2); font-size: var(--font-1); }
.sc-foot { margin: clamp(20px, 2.6vw, 36px) 0 0; color: var(--text-3); font-size: var(--font-1); }

@media (max-width: 900px) {
  .swatch-grid, .comp-grid, .motion-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .sc-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .type-row { grid-template-columns: 1fr; gap: 6px; }
  .type-demo { white-space: normal; }
}
@media (max-width: 600px) {
  .swatch-grid, .comp-grid, .motion-grid, .sc-stats { grid-template-columns: 1fr; }
}
</style>
