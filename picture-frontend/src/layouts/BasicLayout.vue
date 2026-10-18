<template>
  <div id="basicLayout">
    <a-layout class="app-shell">
      <a-layout-header class="header"><GlobalHeader /></a-layout-header>
      <a-layout class="body-layout">
        <GlobalSider class="sider" />
        <a-layout-content class="content"><router-view /></a-layout-content>
      </a-layout>
      <a-layout-footer class="footer"><span>智能摄影云社区</span><span>摄影资产管理 · AI 编目 · 作品交流</span></a-layout-footer>
    </a-layout>
  </div>
</template>
<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'
</script>
<style scoped>
#basicLayout,
.app-shell { min-height: 100vh; background: var(--bg); color: var(--text-1); }

.header {
  position: sticky;
  z-index: var(--z-header);
  top: 0;
  height: var(--header-h);
  padding: 0 var(--gutter);
  line-height: var(--header-h);
  background: color-mix(in srgb, var(--surface) 90%, transparent);
  backdrop-filter: saturate(180%) blur(14px);
  border-bottom: 1px solid var(--border);
}

.body-layout { flex: 1; min-height: calc(100vh - var(--header-h) - 58px); background: transparent; }

.sider {
  flex: 0 0 auto;
  position: sticky;
  top: var(--header-h);
  align-self: flex-start;
  max-height: calc(100vh - var(--header-h));
  overflow-y: auto;
  background: var(--surface);
  border-right: 1px solid var(--border);
}

/* 内容区：超宽屏下限制阅读宽度并居中，避免正文被拉到 2000px 以上 */
.content {
  min-width: 0;
  padding: var(--space-6) clamp(var(--space-5), 3.2vw, var(--space-8)) var(--space-8);
  background: transparent;
}

.content > :deep(*) { max-width: var(--content-max); margin-inline: auto; }

.footer {
  display: flex;
  justify-content: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--gutter);
  color: var(--text-3);
  font-size: var(--font-1);
  background: var(--surface);
  border-top: 1px solid var(--border);
}

@media (max-width: 992px) {
  .sider { position: static; max-height: none; }
}

@media (max-width: 768px) {
  .content { padding: var(--space-5) var(--gutter) var(--space-6); }
  .footer { flex-direction: column; align-items: center; gap: var(--space-1); text-align: center; }
}
</style>
