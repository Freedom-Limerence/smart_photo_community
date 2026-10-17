<template>
  <article class="work-card hover-lift" tabindex="0" @click="$emit('open', work)" @keydown.enter="$emit('open', work)">
    <div class="cover">
      <LazyImage :src="cover" :preview="work.coverThumbnailUrl || work.thumbnailUrl || work.pictures?.[0]?.thumbnailUrl" :ratio="ratio" :alt="work.title" />
      <span class="cover-veil" aria-hidden="true" />

      <!-- 悬浮快捷互动：列表页即可点赞/收藏，无需进入详情 -->
      <div class="quick-actions" @click.stop>
        <LikeButton class="quick-btn" :liked="st.liked" :count="st.likeCount" :pending="st.pending"
                    @toggle="store.toggleLike(work)" />
        <button class="quick-btn" :class="{ on: st.favorited }" type="button" :disabled="st.pending"
                :aria-pressed="st.favorited" :aria-label="st.favorited ? '取消收藏' : '收藏'" @click="store.toggleFavorite(work)">
          <StarFilled v-if="st.favorited" /><StarOutlined v-else />
          <span>{{ st.favoriteCount }}</span>
        </button>
      </div>

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
        <button v-if="canFollow" class="follow-btn" type="button" :class="{ on: followed }" @click.stop="doFollow">
          {{ followed ? '已关注' : '+ 关注' }}
        </button>
        <div v-else class="stats">
          <span><MessageOutlined /> {{ work.commentCount ?? 0 }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  LikeFilled, LikeOutlined, MessageOutlined, StarFilled, StarOutlined,
} from '@ant-design/icons-vue'
import LazyImage from './LazyImage.vue'
import LikeButton from './LikeButton.vue'
import { useWorkInteractionStore } from '@/stores/useWorkInteractionStore'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { followUser, unfollowUser } from '@/api/photoCommunity'

const props = withDefaults(defineProps<{ work: any; ratio?: string }>(), { ratio: '66%' })
defineEmits<{ (e: 'open', work: any): void }>()

const store = useWorkInteractionStore()
const loginUserStore = useLoginUserStore()
const followPending = ref(false)
// 关注状态由服务端返回（fillPictures 中的 work.followed），避免刷新后丢失
const followed = ref(!!props.work?.followed)
watch(() => props.work?.followed, (v) => { followed.value = !!v })

const st = computed(() => store.ensure(props.work))

const cover = computed(() => {
  const w = props.work || {}
  return w.coverUrl || w.pictures?.[0]?.thumbnailUrl || w.pictures?.[0]?.url || ''
})

const author = computed(() => props.work?.author)
const authorId = computed(() => props.work?.authorId || props.work?.author?.id)
const authorText = computed(() => (author.value?.userName || '光').slice(0, 1))
const canFollow = computed(() => !!loginUserStore.loginUser?.id && !!authorId.value && String(authorId.value) !== String(loginUserStore.loginUser.id))

const visibilityText = computed(() => {
  const map: Record<string, string> = { FOLLOWER_ONLY: '关注可见', SPACE_ONLY: '团队可见', PRIVATE: '仅自己' }
  return map[props.work?.visibility] || ''
})

async function doFollow() {
  if (followPending.value) return
  followPending.value = true
  const next = !followed.value
  try {
    const res: any = next
      ? await followUser({ userId: authorId.value })
      : await unfollowUser({ userId: authorId.value })
    if (res.data.code === 0) {
      followed.value = next
      message.success(next ? '已关注该作者' : '已取消关注')
    } else {
      message.error(res.data.message)
    }
  } finally {
    followPending.value = false
  }
}
</script>

<style scoped>
.work-card {
  display: flex;
  flex-direction: column;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
}

.work-card:focus-visible { outline: 2px solid var(--accent); outline-offset: 3px; }

.cover { position: relative; overflow: hidden; background: var(--surface-2); }

/* 悬停时封面轻微推近，配合卡片上浮形成层次 */
.cover :deep(.lazy-img.is-loaded) {
  transition: transform var(--dur-slow) var(--ease), opacity var(--dur-slow) var(--ease);
}

.work-card:hover .cover :deep(.lazy-img.is-loaded) { transform: scale(1.028); }

/* 快捷操作背后的压暗层：只在悬停时出现，平时保持画面干净 */
.cover-veil {
  position: absolute;
  inset: 40% 0 0;
  background: linear-gradient(to top, rgba(20, 20, 22, .48), rgba(20, 20, 22, 0));
  opacity: 0;
  transition: opacity var(--dur) var(--ease);
  pointer-events: none;
}

.work-card:hover .cover-veil { opacity: 1; }

.quick-actions {
  position: absolute;
  right: 10px;
  bottom: 10px;
  display: flex;
  gap: 6px;
  opacity: 0;
  transform: translateY(6px);
  transition: opacity var(--dur) var(--ease), transform var(--dur) var(--ease);
}

.work-card:hover .quick-actions,
.work-card:focus-within .quick-actions { opacity: 1; transform: none; }

.quick-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: var(--radius-pill);
  background: rgba(20, 20, 22, .62);
  color: var(--surface-2);
  font-size: 12px;
  cursor: pointer;
  backdrop-filter: blur(6px);
  transition: background var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}

.quick-btn:hover { background: rgba(20, 20, 22, .85); transform: translateY(-1px); }
.quick-btn.on { background: var(--accent); border-color: var(--accent); color: #fff; }
.quick-btn:disabled { opacity: .6; cursor: default; }

.badge {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 9px;
  border-radius: var(--radius-pill);
  background: rgba(28, 28, 26, .62);
  color: #fff;
  font-size: 12px;
  letter-spacing: .02em;
  backdrop-filter: blur(6px);
}

.body { display: flex; flex: 1 1 auto; flex-direction: column; padding: var(--space-3) var(--space-4) var(--space-4); }

.title {
  margin: 0 0 var(--space-3);
  color: var(--text-1);
  font-size: var(--font-3);
  font-weight: 500;
  line-height: 1.5;
  transition: color var(--dur-fast) var(--ease);
}

.work-card:hover .title { color: var(--accent-ink); }

.meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  margin-top: auto;
  padding-top: var(--space-3);
  border-top: 1px solid var(--border);
}

.author { display: inline-flex; align-items: center; gap: 6px; min-width: 0; color: var(--text-2); font-size: var(--font-1); }
.author:hover { color: var(--accent-hover); }

.follow-btn {
  flex: 0 0 auto;
  padding: 2px 10px;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-pill);
  background: transparent;
  color: var(--text-2);
  font-size: 12px;
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease), background-color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}
.follow-btn:hover { border-color: var(--accent); color: var(--accent-hover); }
.follow-btn.on { background: var(--accent-soft); border-color: var(--accent); color: var(--accent-ink); }

.stats { display: inline-flex; gap: 10px; color: var(--text-3); font-size: var(--font-1); }
.stats span { display: inline-flex; align-items: center; gap: 3px; }
</style>
