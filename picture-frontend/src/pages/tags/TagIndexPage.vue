<template>
  <div class="tag-index">
    <header class="tag-head">
      <div class="container">
        <PageHeader eyebrow="EXPLORE BY TAG" title="标签广场" description="按主题浏览站内公开作品，点击任意标签进入聚合页" align="center">
          <a-input-search v-model:value="filter" placeholder="筛选标签" aria-label="筛选标签" allow-clear class="tag-search" />
        </PageHeader>
      </div>
    </header>

    <div class="container">
      <div class="tag-meta">
        <span class="eyebrow">ALL TAGS</span>
        <span class="muted">共 {{ filteredTags.length }} 个标签</span>
      </div>

      <a-spin :spinning="loading">
        <section v-if="loading && !tags.length" class="tag-cloud" aria-hidden="true">
          <span v-for="n in 24" :key="n" class="skeleton tag-skeleton" />
        </section>
        <section v-else-if="filteredTags.length" class="tag-cloud">
          <router-link v-for="tag in filteredTags" :key="tag" class="tag-item" :to="'/tags/' + encodeURIComponent(tag)">
            # {{ tag }}
          </router-link>
        </section>
        <EmptyState v-else :title="filter ? '没有匹配的标签' : '还没有可用标签'"
                    :description="filter ? '换个关键词试试。' : '待图片审核通过后，标签会自动出现在这里。'" />
      </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
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

.tag-head {
  padding: var(--space-7) 0 0;
  margin-bottom: var(--space-6);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.tag-head :deep(.page-head) { border-bottom: 0; padding-bottom: var(--space-6); }
.tag-search { max-width: 360px; margin: 0 auto; }

.tag-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-4);
}
.tag-meta .eyebrow { margin: 0; }

.tag-cloud { display: flex; flex-wrap: wrap; gap: var(--space-3); }

.tag-item {
  padding: 10px 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--text-2);
  font-size: var(--font-3);
  transition: all var(--dur-fast) var(--ease);
}
.tag-item:hover {
  border-color: var(--accent);
  color: var(--accent-ink);
  box-shadow: var(--shadow-2);
  transform: translateY(-2px);
}

.tag-skeleton { display: inline-block; width: 96px; height: 40px; border-radius: var(--radius-pill); }
</style>
