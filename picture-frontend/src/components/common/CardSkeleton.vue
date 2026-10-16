<template>
  <div class="sk-root" :class="[`variant-${variant}`]" :style="{ '--sk-gap': gap }" aria-hidden="true">
    <div v-for="n in count" :key="n" class="sk-card">
      <div class="skeleton sk-cover" :style="{ paddingBottom: ratio }" />
      <div v-if="showMeta" class="sk-meta">
        <div class="skeleton sk-line" style="width: 74%" />
        <div class="skeleton sk-line sk-line-sm" style="width: 42%" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 全站统一加载骨架：栅格列数、圆角、间距与真实卡片一致，
 * 加载完成后不发生列数 / 位置跳变（降低 CLS）。
 * grid：等宽卡片栅格；masonry：与 MasonryGrid 同列数的瀑布流；list：横向列表行。
 */
withDefaults(
  defineProps<{
    count?: number
    ratio?: string
    variant?: 'grid' | 'masonry' | 'list'
    showMeta?: boolean
    gap?: string
  }>(),
  { count: 8, ratio: '66%', variant: 'grid', showMeta: true, gap: 'var(--grid-gap)' },
)
</script>

<style scoped>
.sk-card {
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
}

.sk-cover { width: 100%; height: 0; border-radius: 0; }

.sk-meta { display: flex; flex-direction: column; gap: var(--space-2); padding: var(--space-4); }

.sk-line { height: 12px; border-radius: var(--radius-pill); }
.sk-line-sm { height: 10px; }

/* 等宽栅格：与 .card-grid 完全对齐 */
.variant-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(var(--grid-min), 100%), 1fr));
  gap: var(--sk-gap);
}

/* 瀑布流：列数与断点跟 MasonryGrid 保持同步 */
.variant-masonry { column-gap: var(--sk-gap); column-count: 4; }
.variant-masonry .sk-card { break-inside: avoid; margin-bottom: var(--sk-gap); }

@media (min-width: 1680px) { .variant-masonry { column-count: 5; } }
@media (max-width: 1280px) { .variant-masonry { column-count: 3; } }
@media (max-width: 900px) { .variant-masonry { column-count: 2; } }
@media (max-width: 560px) { .variant-masonry { column-count: 1; } }

/* 列表行：与页面内 li 行结构对齐 */
.variant-list { display: flex; flex-direction: column; gap: var(--space-3); }
.variant-list .sk-card { display: flex; align-items: center; gap: var(--space-4); padding: var(--space-4); }
.variant-list .sk-cover { flex: 0 0 auto; width: 112px; height: 76px; padding-bottom: 0; border-radius: var(--radius); }
.variant-list .sk-meta { flex: 1 1 auto; padding: 0; }
</style>
