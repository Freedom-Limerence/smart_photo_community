<template>
  <article class="work-card hover-lift" @click="$emit('open', work)">
    <div class="cover">
      <LazyImage :src="cover" :ratio="ratio" :alt="work.title" />
      <span v-if="work.visibility && work.visibility !== 'PUBLIC'" class="badge">{{ visibilityText }}</span>
    </div>
    <div class="body">
      <h3 class="title clamp-2">{{ work.title || '未命名作品' }}</h3>
      <div class="meta">
        <router-link v-if="authorId" class="author" :to="'/photographers/' + authorId" @click.stop>
          <a-avatar :src="author?.userAvatar" :size="22">{{ authorText }}</a-avatar>
          <span class="ellipsis">{{ author?.userName || '摄影师' }}</span>
        </router-link>
        <span v-else class="author muted">匿名</span>
        <div class="stats">
          <span><LikeOutlined /> {{ work.likeCount ?? 0 }}</span>
          <span><MessageOutlined /> {{ work.commentCount ?? 0 }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { LikeOutlined, MessageOutlined } from '@ant-design/icons-vue'
import LazyImage from './LazyImage.vue'

const props = withDefaults(defineProps<{ work: any; ratio?: string }>(), { ratio: '66%' })
defineEmits<{ (e: 'open', work: any): void }>()

const cover = computed(() => {
  const w = props.work || {}
  return w.coverUrl || w.pictures?.[0]?.thumbnailUrl || w.pictures?.[0]?.url || ''
})

const author = computed(() => props.work?.author)
const authorId = computed(() => props.work?.authorId || props.work?.author?.id)
const authorText = computed(() => (author.value?.userName || '光').slice(0, 1))

const visibilityText = computed(() => {
  const map: Record<string, string> = { FOLLOWER_ONLY: '关注可见', SPACE_ONLY: '团队可见', PRIVATE: '仅自己' }
  return map[props.work?.visibility] || ''
})
</script>

<style scoped>
.work-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
}

.cover { position: relative; background: var(--surface-2); }

.badge {
  position: absolute; top: 10px; left: 10px; padding: 2px 8px;
  border-radius: var(--radius-pill); background: rgba(28, 28, 26, .68); color: #fff; font-size: 11px;
}

.body { padding: 12px 14px 14px; }
.title { margin: 0 0 10px; font-size: var(--font-3); font-weight: 500; line-height: 1.5; }

.meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.author { display: inline-flex; align-items: center; gap: 6px; min-width: 0; color: var(--text-2); font-size: var(--font-1); }
.author:hover { color: var(--accent-hover); }

.stats { display: inline-flex; gap: 10px; color: var(--text-3); font-size: var(--font-1); }
.stats span { display: inline-flex; align-items: center; gap: 3px; }
</style>
