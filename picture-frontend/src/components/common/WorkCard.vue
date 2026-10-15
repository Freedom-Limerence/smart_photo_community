<template>
  <article class="work-card hover-lift" @click="$emit('open', work)">
    <div class="cover">
      <LazyImage :src="cover" :ratio="ratio" :alt="work.title" />

      <!-- 悬浮快捷互动：列表页即可点赞/收藏，无需进入详情 -->
      <div class="quick-actions" @click.stop>
        <button class="quick-btn" :class="{ on: st.liked }" type="button" :disabled="st.pending" @click="store.toggleLike(work)">
          <LikeFilled v-if="st.liked" /><LikeOutlined v-else />
          <span>{{ st.likeCount }}</span>
        </button>
        <button class="quick-btn" :class="{ on: st.favorited }" type="button" :disabled="st.pending" @click="store.toggleFavorite(work)">
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
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
}

.cover { position: relative; background: var(--surface-2); }

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

.work-card:hover .quick-actions { opacity: 1; transform: none; }

.quick-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid rgba(255, 255, 255, .22);
  border-radius: var(--radius-pill);
  background: rgba(20, 20, 22, .62);
  color: #F2F2EF;
  font-size: 12px;
  cursor: pointer;
  backdrop-filter: blur(6px);
  transition: background var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}

.quick-btn:hover { background: rgba(20, 20, 22, .85); transform: translateY(-1px); }
.quick-btn.on { background: var(--accent); border-color: var(--accent); color: #fff; }
.quick-btn:disabled { opacity: .6; cursor: default; }

.badge {
  position: absolute; top: 10px; left: 10px; padding: 2px 8px;
  border-radius: var(--radius-pill); background: rgba(28, 28, 26, .68); color: #fff; font-size: 11px;
}

.body { padding: 12px 14px 14px; }
.title { margin: 0 0 10px; font-size: var(--font-3); font-weight: 500; line-height: 1.5; }

.meta { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.author { display: inline-flex; align-items: center; gap: 6px; min-width: 0; color: var(--text-2); font-size: var(--font-1); }
.author:hover { color: var(--accent-hover); }

.follow-btn {
  flex: 0 0 auto; padding: 2px 10px; border: 1px solid var(--border-strong); border-radius: var(--radius-pill);
  background: transparent; color: var(--text-2); font-size: 11px; cursor: pointer;
  transition: all var(--dur-fast) var(--ease);
}
.follow-btn:hover { border-color: var(--accent); color: var(--accent-hover); }
.follow-btn.on { background: var(--accent-soft); border-color: var(--accent); color: #4F5F53; }

.stats { display: inline-flex; gap: 10px; color: var(--text-3); font-size: var(--font-1); }
.stats span { display: inline-flex; align-items: center; gap: 3px; }
</style>
