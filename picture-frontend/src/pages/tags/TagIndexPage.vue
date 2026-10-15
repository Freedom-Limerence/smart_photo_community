<template>
  <div class="tag-index">
    <header class="tag-head">
      <div class="container">
        <p class="eyebrow">EXPLORE BY TAG</p>
        <h1>标签广场</h1>
        <p class="muted">按主题浏览站内公开作品，点击任意标签进入聚合页</p>
        <a-input-search v-model:value="filter" placeholder="筛选标签" allow-clear class="tag-search" />
      </div>
    </header>

    <div class="container">
      <a-spin :spinning="loading">
        <section v-if="filteredTags.length" class="tag-cloud">
          <router-link v-for="tag in filteredTags" :key="tag" class="tag-item" :to="'/tags/' + encodeURIComponent(tag)">
            # {{ tag }}
          </router-link>
        </section>
        <a-empty v-else-if="!loading" description="还没有可用标签" />
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listPictureTagCategoryUsingGet } from '@/api/pictureController'

const tags = ref<string[]>([])
const loading = ref(false)
const filter = ref('')

const filteredTags = computed(() => {
  const key = filter.value.trim().toLowerCase()
  return key ? tags.value.filter((t) => t.toLowerCase().includes(key)) : tags.value
})

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await listPictureTagCategoryUsingGet()
    if (res.data.code === 0) tags.value = res.data.data?.tags || []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.tag-index { padding-bottom: var(--space-8); }
.tag-head { padding: var(--space-7) 0 var(--space-6); background: var(--surface); border-bottom: 1px solid var(--border); margin-bottom: var(--space-6); text-align: center; }
.tag-head .eyebrow { margin: 0 0 6px; color: var(--text-3); font-size: var(--font-1); letter-spacing: .2em; }
.tag-head h1 { margin: 0 0 8px; font-size: var(--font-7); }
.tag-head p { margin: 0 0 var(--space-4); }
.tag-search { max-width: 320px; margin: 0 auto; }
.tag-cloud { display: flex; flex-wrap: wrap; gap: var(--space-3); }
.tag-item {
  padding: 10px 18px; border: 1px solid var(--border); border-radius: var(--radius-pill);
  background: var(--surface); color: var(--text-2); font-size: var(--font-3);
  transition: all var(--dur-fast) var(--ease);
}
.tag-item:hover { border-color: var(--accent); color: var(--accent-hover); box-shadow: var(--shadow-2); transform: translateY(-2px); }
</style>
