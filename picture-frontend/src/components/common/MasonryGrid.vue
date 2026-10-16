<template>
  <div class="masonry" :style="{ '--masonry-gap': gap }">
    <div v-for="(item, index) in items" :key="keyOf(item, index)" class="masonry-item">
      <slot :item="item" :index="index" />
    </div>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{ items: any[]; gap?: string; keyField?: string }>(), {
  gap: 'var(--grid-gap)',
  keyField: 'id',
})

function keyOf(item: any, index: number) {
  return (item && (item[props.keyField] ?? item.id ?? item.workId ?? item.pictureId)) ?? index
}
</script>

<style scoped>
/* 列数与间距全部走令牌：超宽屏自动加到 5 列，小屏收敛到 1 列 */
.masonry {
  column-gap: var(--masonry-gap, var(--grid-gap));
  column-count: 4;
}

@media (min-width: 1680px) { .masonry { column-count: 5; } }
@media (max-width: 1280px) { .masonry { column-count: 3; } }
@media (max-width: 900px) { .masonry { column-count: 2; } }
@media (max-width: 560px) { .masonry { column-count: 1; } }

.masonry-item {
  break-inside: avoid;
  margin-bottom: var(--masonry-gap, var(--grid-gap));
}
</style>
