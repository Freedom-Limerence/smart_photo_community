<template>
  <div class="work-detail" data-theme="immersive">
    <div v-if="loading" class="detail-loading">
      <div class="skeleton viewer-skeleton" />
      <div class="skeleton side-skeleton" />
    </div>

    <template v-else-if="work">
      <!-- 主区：沉浸式看图 -->
      <section class="viewer">
        <div class="viewer-stage">
          <img :src="activePicture?.url || activePicture?.thumbnailUrl" :alt="work.title" class="viewer-image" @click="toggleFullscreen" />
          <button class="viewer-nav prev" type="button" :disabled="pictures.length < 2" @click="step(-1)">
            <LeftOutlined />
          </button>
          <button class="viewer-nav next" type="button" :disabled="pictures.length < 2" @click="step(1)">
            <RightOutlined />
          </button>
          <div class="viewer-toolbar">
            <a-button type="text" class="tool-btn" @click="toggleFullscreen">
              <FullscreenOutlined /> {{ fullscreen ? '退出全屏' : '全屏看图' }}
            </a-button>
            <span class="viewer-counter">{{ activeIndex + 1 }} / {{ pictures.length }}</span>
          </div>
        </div>

        <div v-if="pictures.length > 1" class="viewer-strip">
          <button v-for="(pic, i) in pictures" :key="pic.id" class="strip-item" :class="{ active: i === activeIndex }"
                  type="button" @click="activeIndex = i">
            <img :src="pic.thumbnailUrl || pic.url" alt="" loading="lazy" />
          </button>
        </div>
      </section>

      <!-- 副区：信息与互动 -->
      <aside class="side">
        <header class="author-bar">
          <router-link class="author-link" :to="'/photographers/' + work.authorId">
            <a-avatar :src="work.author?.userAvatar" :size="44">{{ authorText }}</a-avatar>
            <div>
              <strong>{{ work.author?.userName || '摄影师' }}</strong>
              <span class="muted">{{ formatDate(work.createTime) }} 发布</span>
            </div>
          </router-link>
          <a-button v-if="!isSelf" :type="followed ? 'default' : 'primary'" size="small" @click="toggleFollow">
            {{ followed ? '已关注' : '关注' }}
          </a-button>
        </header>

        <h1 class="work-title">{{ work.title }}</h1>
        <p v-if="work.description" class="work-desc">{{ work.description }}</p>

        <dl class="params">
          <div v-if="meta.picWidth"><dt>尺寸</dt><dd>{{ meta.picWidth }} × {{ meta.picHeight }}</dd></div>
          <div v-if="meta.picFormat"><dt>格式</dt><dd>{{ String(meta.picFormat).toUpperCase() }}</dd></div>
          <div v-if="meta.picColor"><dt>主色</dt><dd><span class="color-dot" :style="{ background: meta.picColor }" />{{ meta.picColor }}</dd></div>
          <div v-if="work.shootTime"><dt>拍摄时间</dt><dd>{{ formatDate(work.shootTime) }}</dd></div>
          <div v-if="work.shootLocation"><dt>拍摄地点</dt><dd>{{ work.shootLocation }}</dd></div>
          <div v-if="work.copyrightNotice"><dt>版权</dt><dd>{{ work.copyrightNotice }}</dd></div>
          <div><dt>可见范围</dt><dd>{{ visibilityText }}</dd></div>
        </dl>

        <div class="actions">
          <button class="act" :class="{ on: liked }" type="button" @click="toggleLike">
            <LikeFilled v-if="liked" /><LikeOutlined v-else /> {{ work.likeCount ?? 0 }}
          </button>
          <button class="act" :class="{ on: favorited }" type="button" @click="toggleFavorite">
            <StarFilled v-if="favorited" /><StarOutlined v-else /> {{ work.favoriteCount ?? 0 }}
          </button>
          <button class="act" type="button" @click="showComments = !showComments">
            <MessageOutlined /> {{ comments.length || work.commentCount || 0 }}
          </button>
          <a-button class="download-btn" size="small" :loading="downloading" @click="download">下载原图</a-button>
        </div>

        <!-- 评论区 -->
        <section class="comments" :class="{ collapsed: !showComments }">
          <div class="comment-composer">
            <a-textarea v-model:value="commentDraft" :auto-size="{ minRows: 2, maxRows: 4 }" placeholder="写下你的想法…" />
            <a-button type="primary" size="small" :loading="commenting" :disabled="!commentDraft.trim()" @click="submitComment">
              发表评论
            </a-button>
          </div>
          <ul class="comment-list">
            <li v-for="c in comments" :key="c.id" class="comment-item">
              <a-avatar :src="c.user?.userAvatar" :size="30">{{ (c.user?.userName || '光').slice(0, 1) }}</a-avatar>
              <div class="comment-body">
                <div class="comment-head">
                  <strong>{{ c.user?.userName || '用户' }}</strong>
                  <span class="muted">{{ formatDate(c.createTime) }}</span>
                </div>
                <p>{{ c.content }}</p>
              </div>
            </li>
          </ul>
          <a-empty v-if="showComments && !comments.length" description="还没有评论，来说点什么" />
        </section>
      </aside>
    </template>

    <a-empty v-else description="作品不存在或你没有查看权限">
      <a-button type="primary" @click="router.push('/')">返回首页</a-button>
    </a-empty>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  FullscreenOutlined, LeftOutlined, LikeFilled, LikeOutlined, MessageOutlined,
  RightOutlined, StarFilled, StarOutlined,
} from '@ant-design/icons-vue'
import {
  commentWork, favoriteWork, followUser, listComments, unfavoriteWork, unlikeWork, likeWork, getWorkDetail,
} from '@/api/photoCommunity'
import { authorizeDownload } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const loading = ref(true)
const work = ref<any>(null)
const comments = ref<any[]>([])
const activeIndex = ref(0)
const fullscreen = ref(false)
const showComments = ref(true)
const commentDraft = ref('')
const commenting = ref(false)
const downloading = ref(false)
const liked = ref(false)
const favorited = ref(false)
const followed = ref(false)

const workId = computed(() => String(route.params.id))
const pictures = computed(() => work.value?.pictures?.length ? work.value.pictures : [])
const activePicture = computed(() => pictures.value[activeIndex.value] || work.value?.pictures?.[0] || { url: work.value?.coverUrl })
const meta = computed(() => activePicture.value || {})
const authorText = computed(() => (work.value?.author?.userName || '光').slice(0, 1))
const isSelf = computed(() => String(work.value?.authorId) === String(loginUserStore.loginUser?.id))
const visibilityText = computed(() => {
  const map: Record<string, string> = { PUBLIC: '公开', FOLLOWER_ONLY: '仅关注者', SPACE_ONLY: '仅团队成员', PRIVATE: '仅自己' }
  return map[work.value?.visibility] || '公开'
})

function formatDate(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function step(delta: number) {
  if (pictures.value.length < 2) return
  activeIndex.value = (activeIndex.value + delta + pictures.value.length) % pictures.value.length
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
  const el: any = document.querySelector('.work-detail')
  if (fullscreen.value) el?.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.().catch(() => {})
}

async function load() {
  loading.value = true
  try {
    const res: any = await getWorkDetail(workId.value)
    work.value = res.data.code === 0 ? res.data.data : null
    if (work.value) {
      const cRes: any = await listComments(workId.value)
      comments.value = cRes.data.code === 0 ? (cRes.data.data || []) : []
    }
  } finally {
    loading.value = false
  }
}

async function toggleLike() {
  const res: any = liked.value ? await unlikeWork({ workId: workId.value }) : await likeWork({ workId: workId.value })
  if (res.data.code === 0) {
    liked.value = !liked.value
    work.value.likeCount = Math.max(0, (work.value.likeCount || 0) + (liked.value ? 1 : -1))
  } else message.error(res.data.message)
}

async function toggleFavorite() {
  const res: any = favorited.value ? await unfavoriteWork({ workId: workId.value }) : await favoriteWork({ workId: workId.value })
  if (res.data.code === 0) {
    favorited.value = !favorited.value
    work.value.favoriteCount = Math.max(0, (work.value.favoriteCount || 0) + (favorited.value ? 1 : -1))
    message.success(favorited.value ? '已收藏' : '已取消收藏')
  } else message.error(res.data.message)
}

async function toggleFollow() {
  const res: any = await followUser({ userId: work.value.authorId })
  if (res.data.code === 0) {
    followed.value = true
    message.success('已关注该作者')
  } else message.error(res.data.message)
}

async function submitComment() {
  commenting.value = true
  try {
    const res: any = await commentWork({ workId: workId.value, content: commentDraft.value.trim() })
    if (res.data.code === 0) {
      commentDraft.value = ''
      const cRes: any = await listComments(workId.value)
      comments.value = cRes.data.code === 0 ? (cRes.data.data || []) : []
      work.value.commentCount = (work.value.commentCount || 0) + 1
    } else message.error(res.data.message)
  } finally {
    commenting.value = false
  }
}

async function download() {
  const pic = activePicture.value
  if (!pic?.id) return message.warning('该作品没有可下载的原图')
  downloading.value = true
  try {
    const requestId = 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
    const res: any = await authorizeDownload({ pictureId: pic.id, workId: workId.value, requestId })
    if (res.data.code !== 0) return message.error(res.data.message || '下载授权失败')
    const data = res.data.data
    message.success('已授权下载（剩余配额以访问控制中心为准）')
    window.open('http://localhost:8123' + data.downloadPath, '_blank')
  } finally {
    downloading.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.work-detail {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  min-height: calc(100vh - var(--header-h));
  background: var(--bg);
  color: var(--text-1);
}

.detail-loading { display: grid; grid-template-columns: minmax(0, 1fr) 400px; gap: var(--space-5); padding: var(--space-5); }
.viewer-skeleton { height: 70vh; }
.side-skeleton { height: 70vh; }

.viewer { display: flex; flex-direction: column; gap: var(--space-4); padding: var(--space-5); }
.viewer-stage { position: relative; flex: 1 1 auto; display: grid; place-items: center; background: #0E0E10; border-radius: var(--radius-lg); overflow: hidden; min-height: 60vh; }
.viewer-image { max-width: 100%; max-height: calc(100vh - 220px); object-fit: contain; cursor: zoom-in; }

.viewer-nav {
  position: absolute; top: 50%; transform: translateY(-50%);
  display: grid; place-items: center; width: 42px; height: 42px;
  border: 1px solid rgba(255, 255, 255, .16); border-radius: 50%;
  background: rgba(20, 20, 22, .6); color: #EDEDEA; font-size: 16px; cursor: pointer;
  transition: background var(--dur-fast) var(--ease);
}
.viewer-nav:hover:not(:disabled) { background: rgba(20, 20, 22, .9); }
.viewer-nav:disabled { opacity: .3; cursor: not-allowed; }
.viewer-nav.prev { left: 18px; }
.viewer-nav.next { right: 18px; }

.viewer-toolbar {
  position: absolute; left: 18px; right: 18px; bottom: 16px;
  display: flex; align-items: center; justify-content: space-between;
  color: rgba(237, 237, 234, .85); font-size: var(--font-1);
}
.tool-btn { color: #EDEDEA; }
.tool-btn:hover { color: #fff; background: rgba(255, 255, 255, .08); }

.viewer-strip { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 4px; }
.strip-item { flex: 0 0 auto; width: 72px; height: 56px; padding: 0; border: 1px solid var(--border); border-radius: var(--radius-sm); overflow: hidden; background: var(--surface-2); cursor: pointer; opacity: .6; transition: all var(--dur-fast) var(--ease); }
.strip-item img { width: 100%; height: 100%; object-fit: cover; display: block; }
.strip-item.active { opacity: 1; border-color: var(--accent); }

.side { padding: var(--space-5) var(--space-5) var(--space-7); border-left: 1px solid var(--border); background: var(--surface); overflow-y: auto; max-height: calc(100vh - var(--header-h)); }

.author-bar { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-4); }
.author-link { display: flex; align-items: center; gap: var(--space-3); }
.author-link strong { display: block; font-size: var(--font-3); }
.author-link span { font-size: var(--font-1); }

.work-title { margin: 0 0 var(--space-2); font-size: var(--font-6); line-height: 1.3; }
.work-desc { margin: 0 0 var(--space-5); color: var(--text-2); font-size: var(--font-2); line-height: 1.8; white-space: pre-wrap; }

.params { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px var(--space-4); margin: 0 0 var(--space-5); padding: var(--space-4); background: var(--surface-2); border-radius: var(--radius); }
.params div { display: flex; flex-direction: column; gap: 2px; }
.params dt { color: var(--text-3); font-size: 11px; }
.params dd { margin: 0; font-size: var(--font-2); display: flex; align-items: center; gap: 6px; }
.color-dot { width: 12px; height: 12px; border-radius: 3px; border: 1px solid var(--border-strong); }

.actions { display: flex; align-items: center; gap: var(--space-3); padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.act { display: inline-flex; align-items: center; gap: 6px; padding: 7px 12px; border: 1px solid var(--border); border-radius: var(--radius-pill); background: transparent; color: var(--text-2); font-size: var(--font-2); cursor: pointer; transition: all var(--dur-fast) var(--ease); }
.act:hover { border-color: var(--accent); color: var(--text-1); }
.act.on { background: var(--accent-soft); border-color: var(--accent); color: #4F5F53; }
.download-btn { margin-left: auto; }

.comments { margin-top: var(--space-5); }
.comments.collapsed .comment-list, .comments.collapsed .comment-composer { display: none; }
.comment-composer { display: flex; flex-direction: column; gap: 8px; align-items: flex-end; margin-bottom: var(--space-4); }
.comment-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--space-4); }
.comment-item { display: flex; gap: 10px; }
.comment-body { flex: 1 1 auto; min-width: 0; }
.comment-head { display: flex; align-items: baseline; gap: 8px; }
.comment-head strong { font-size: var(--font-2); }
.comment-head span { font-size: 11px; }
.comment-body p { margin: 4px 0 0; font-size: var(--font-2); color: var(--text-2); line-height: 1.7; }

@media (max-width: 1024px) {
  .work-detail { grid-template-columns: 1fr; }
  .side { border-left: none; border-top: 1px solid var(--border); max-height: none; }
  .viewer-image { max-height: 60vh; }
}
</style>
