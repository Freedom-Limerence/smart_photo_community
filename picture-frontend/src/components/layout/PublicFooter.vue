<template>
  <footer class="public-footer">
    <div class="footer-inner">
      <div class="footer-brand">
        <span class="brand-mark">光</span>
        <div>
          <strong>光影集 · 摄影创作社区</strong>
          <p>给热爱摄影的人，一个分享视角的地方。</p>
        </div>
      </div>

      <nav class="footer-col" aria-label="页脚导航">
        <p class="col-title">探索</p>
        <router-link to="/community">作品社区</router-link>
        <router-link to="/exhibitions">专题展览</router-link>
        <router-link to="/wall">今日影像</router-link>
        <router-link to="/search">语义检索</router-link>
      </nav>

      <nav class="footer-col" aria-label="创作入口">
        <p class="col-title">创作</p>
        <router-link to="/me/gallery">我的图库</router-link>
        <router-link to="/me/create-work">发布作品</router-link>
        <router-link to="/ai/workbench">AI 工作台</router-link>
        <router-link to="/tags">标签广场</router-link>
      </nav>

      <div class="footer-col footer-legal">
        <p class="col-title">说明与合规</p>
        <p class="legal-line">本站为学习与毕业设计演示，作品版权归原作者所有。</p>
        <div class="footer-meta">
          <span>© {{ year }} 光影集</span>
          <span class="theme-slot"><!-- 主题切换位（P2 深色体系就绪后接入） --></span>
        </div>
      </div>
    </div>

    <!-- 巨型品牌字：描边签名式收尾（获奖站标配） -->
    <div class="footer-giant" aria-hidden="true" data-scrub-fade>光影集</div>
  </footer>
</template>

<script setup lang="ts">
const year = new Date().getFullYear()
</script>

<style scoped>
.public-footer {
  margin-top: var(--space-8);
  background: var(--surface);
  border-top: 1px solid var(--border);
}

/* 桌面端：品牌 / 探索 / 创作 / 说明与合规 四列（4.1 #6）；移动端单列 */
.footer-inner {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, .7fr) minmax(0, .7fr) minmax(0, 1.2fr);
  align-items: start;
  gap: var(--space-5) var(--space-8);
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--space-7) var(--gutter) var(--space-6);
}

.footer-brand { display: flex; align-items: center; gap: var(--space-3); min-width: 0; }

.brand-mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius);
  background: var(--accent);
  color: var(--accent-contrast);
  font-weight: 600;
  flex: 0 0 auto;
}

.footer-brand strong { display: block; font-size: var(--font-3); }
.footer-brand p { margin: 2px 0 0; color: var(--text-3); font-size: var(--font-1); }

.footer-col { display: flex; flex-direction: column; gap: var(--space-2); min-width: 0; }
.col-title {
  margin: 0 0 var(--space-1);
  color: var(--text-3);
  font-size: var(--font-1);
  font-weight: 600;
  letter-spacing: .14em;
}
.footer-col a {
  position: relative;
  width: fit-content;
  color: var(--text-2);
  font-size: var(--font-2);
  transition: color var(--dur-fast) var(--ease);
}
.footer-col a:hover { color: var(--accent-hover); }

.legal-line { margin: 0; color: var(--text-3); font-size: var(--font-2); line-height: var(--lh-body); max-width: 34ch; }
.footer-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--border);
  color: var(--text-3);
  font-size: var(--font-1);
}

@media (max-width: 768px) {
  .footer-inner { grid-template-columns: 1fr; gap: var(--space-5); padding-top: var(--space-6); }
  .footer-brand { align-items: center; }
}
.footer-giant {
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 var(--gutter);
  font-family: var(--font-display);
  font-size: clamp(88px, 17vw, 300px);
  line-height: .82;
  text-align: center;
  letter-spacing: .04em;
  color: transparent;
  -webkit-text-stroke: 1px rgba(237, 237, 234, .13);
  user-select: none;
  transition: color .6s var(--ease, ease), -webkit-text-stroke-color .6s var(--ease, ease);
}
.footer-giant:hover { color: rgba(237, 237, 234, .05); -webkit-text-stroke-color: rgba(143, 163, 148, .3); }
</style>
