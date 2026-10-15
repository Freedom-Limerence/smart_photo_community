<template>
  <div class="search-page container">
    <div class="search-head">
      <p class="eyebrow">SEARCH</p>
      <h1>搜索「{{ keyword || '全部' }}」</h1>
      <a-input-search v-model:value="keyword" size="large" enter-button="搜索" placeholder="搜索图片语义，例如：风景 / 人像 / 夜景"
                      @search="doSearch" />
    </div>

    <a-spin :spinning="loading">
      <MasonryGrid v-if="items.length" :items="items" key-field="pictureId">
        <template #default="{ item }">
          <article class="hit-card hover-lift" @click="router.push('/picture/' + item.pictureId)">
            <div class="hit-cover">
              <img :src="item.thumbnailUrl || item.url" alt="" loading="lazy" />
            </div>
            <div class="hit-body">
              <strong class="ellipsis">{{ item.name || ('图片 #' + item.pictureId) }}</strong>
              <div class="hit-tags">
                <a-tag color="green">{{ item.accessReason }}</a-tag>
                <a-tag v-for="f in item.matchedFields" :key="f">{{ fieldLabel(f) }}</a-tag>
              </div>
            </div>
          </article>
        </template>
      </MasonryGrid>
      <a-empty v-else-if="!loading" :description="searched ? '没有找到匹配的内容' : '输入关键词开始搜索'" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import { searchWithEvidence } from '@/api/aiController'

const route = useRoute()
const router = useRouter()
const keyword = ref(String(route.query.q ?? ''))
const items = ref<any[]>([])
const loading = ref(false)
const searched = ref(false)

const fieldLabels: Record<string, string> = {
  name: '名称', introduction: '简介', category: '分类', tags: '标签',
  subjectTags: '主体', sceneTags: '场景', lightTags: '光线', compositionTags: '构图', camera: '相机',
}
function fieldLabel(f: string) { return fieldLabels[f] || f }

async function doSearch() {
  if (!keyword.value.trim()) return
  loading.value = true
  searched.value = true
  try {
    const res: any = await searchWithEvidence({ query: keyword.value.trim(), topK: 30 })
    if (res.data.code === 0) items.value = res.data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (keyword.value) void doSearch()
})
</script>

<style scoped>
.search-page { padding: var(--space-6) 0 var(--space-8); }
.search-head { max-width: 640px; margin: 0 auto var(--space-6); text-align: center; }
.search-head .eyebrow { margin: 0 0 var(--space-2); color: var(--text-3); font-size: var(--font-1); letter-spacing: .18em; }
.search-head h1 { margin: 0 0 var(--space-4); font-size: var(--font-7); }
.hit-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; cursor: pointer; }
.hit-cover img { width: 100%; display: block; }
.hit-body { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 8px; }
.hit-tags { display: flex; flex-wrap: wrap; gap: 4px; }
</style>
