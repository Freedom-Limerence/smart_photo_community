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
      <div class="footer-links">
        <router-link to="/community">作品社区</router-link>
        <router-link to="/exhibitions">专题展览</router-link>
        <router-link to="/search">探索作品</router-link>
        <router-link to="/me/gallery">我的图库</router-link>
      </div>
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

.footer-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--space-6);
}

.footer-brand { display: flex; align-items: center; gap: var(--space-3); }
.brand-mark {
  display: grid; place-items: center; width: 32px; height: 32px;
  border-radius: var(--radius); background: var(--accent); color: var(--accent-contrast); font-weight: 600;
}
.footer-brand strong { display: block; font-size: var(--font-3); }
.footer-brand p { margin: 2px 0 0; color: var(--text-3); font-size: var(--font-1); }

.footer-links { display: flex; gap: var(--space-5); }
.footer-links a { color: var(--text-2); font-size: var(--font-2); transition: color var(--dur-fast) var(--ease); }
.footer-links a:hover { color: var(--accent-hover); }

.footer-copy { width: 100%; margin: 0; color: var(--text-3); font-size: var(--font-1); }
</style>
