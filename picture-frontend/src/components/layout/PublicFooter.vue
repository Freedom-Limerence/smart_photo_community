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

      <nav class="footer-links" aria-label="页脚导航">
        <router-link to="/community">作品社区</router-link>
        <router-link to="/exhibitions">专题展览</router-link>
        <router-link to="/search">探索作品</router-link>
        <router-link to="/me/gallery">我的图库</router-link>
      </nav>

      <p class="footer-copy">© {{ year }} 光影集 · 仅用于学习与毕业设计演示</p>
    </div>
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

/* 桌面端：品牌 / 导航两栏 + 通栏版权条；超宽屏下仍然贴住版心 */
.footer-inner {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1fr);
  align-items: center;
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

.footer-links {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3) var(--space-6);
  justify-content: flex-end;
}

.footer-links a {
  position: relative;
  color: var(--text-2);
  font-size: var(--font-2);
  transition: color var(--dur-fast) var(--ease);
}

.footer-links a::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -4px;
  height: 1px;
  background: var(--accent);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform var(--dur) var(--ease);
}

.footer-links a:hover { color: var(--accent-hover); }
.footer-links a:hover::after { transform: scaleX(1); }

.footer-copy {
  grid-column: 1 / -1;
  margin: 0;
  padding-top: var(--space-4);
  border-top: 1px solid var(--border);
  color: var(--text-3);
  font-size: var(--font-1);
}

@media (max-width: 768px) {
  .footer-inner { grid-template-columns: 1fr; gap: var(--space-4); padding-top: var(--space-6); }
  .footer-links { justify-content: flex-start; }
}
</style>
