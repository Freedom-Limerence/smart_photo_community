<template>
  <div class="masonry" :style="{ '--masonry-gap': gap }">
    <div v-for="(item, index) in items" :key="keyOf(item, index)" class="masonry-item">
      <slot :item="item" :index="index" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ items: any[]; gap?: string; keyField?: string }>(), {
  gap: '20px',
  keyField: 'id',
})

function keyOf(item: any, index: number) {
  const field = 'id'
  return (item && (item[field] ?? item.workId ?? item.pictureId)) ?? index
}
</script>

<style scoped>
.masonry {
  column-gap: var(--masonry-gap);
  column-count: 4;
}

@media (max-width: 1280px) { .masonry { column-count: 3; } }
@media (max-width: 900px) { .masonry { column-count: 2; } }
@media (max-width: 560px) { .masonry { column-count: 1; } }

.masonry-item {
  break-inside: avoid;
  margin-bottom: var(--masonry-gap);
}
</style>
