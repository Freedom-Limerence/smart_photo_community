<template>
  <header class="page-head" :class="[`align-${align}`, `size-${size}`]">
    <div class="page-head-main">
      <p v-if="eyebrow" class="page-head-eyebrow">{{ eyebrow }}</p>
      <h1 class="page-head-title">{{ title }}</h1>
      <p v-if="description" class="page-head-desc">{{ description }}</p>
      <div v-if="$slots.default" class="page-head-body"><slot /></div>
    </div>
    <div v-if="$slots.actions" class="page-head-actions"><slot name="actions" /></div>
  </header>
</template>

<script setup lang="ts">
/**
 * 全站统一页面标题区：眉标 / 标题 / 说明 / 右侧操作。
 * 只决定排版与间距，标题内容仍由各页面传入。
 */
withDefaults(
  defineProps<{
    eyebrow?: string
    title?: string
    description?: string
    align?: 'left' | 'center'
    size?: 'md' | 'lg'
  }>(),
  { eyebrow: '', title: '', description: '', align: 'left', size: 'md' },
)
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--border);
}

.page-head-main { min-width: 0; }

.page-head-eyebrow {
  margin: 0 0 var(--space-2);
  color: var(--text-3);
  font-size: var(--font-1);
  font-weight: 600;
  letter-spacing: .2em;
  text-transform: uppercase;
}

.page-head-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: var(--font-7);
  font-weight: 400;
  letter-spacing: -.02em;
  line-height: 1.25;
}

.page-head-desc { margin: var(--space-2) 0 0; color: var(--text-2); font-size: var(--font-3); }
.page-head-body { margin-top: var(--space-4); }
.page-head-actions { display: flex; align-items: center; gap: var(--space-3); flex: 0 0 auto; padding-bottom: 2px; }

.size-lg { padding-bottom: var(--space-6); }
.size-lg .page-head-title { font-size: var(--font-8); }

.align-center { flex-direction: column; align-items: center; text-align: center; }
.align-center .page-head-main { display: flex; flex-direction: column; align-items: center; }
.align-center .page-head-body { width: 100%; }

@media (max-width: 640px) {
  .page-head { flex-direction: column; align-items: stretch; gap: var(--space-4); }
  .page-head-actions { padding-bottom: 0; }
}
</style>
