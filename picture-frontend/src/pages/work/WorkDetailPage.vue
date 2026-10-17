<template>
  <!-- 沉浸式主题闭合：深色看图界面里的 antd 组件跟随暗色算法，不再出现白色裂开 -->
  <a-config-provider :theme="immersiveTheme">
  <div class="work-page" data-theme="immersive">
    <a-spin :spinning="loading">
      <div v-if="work" class="work-layout">
        <!-- ============ 主区：沉浸式看图 ============ -->
        <section class="viewer">
          <div class="stage" :class="{ fullscreen }">
            <img v-if="activePicture" :src="activePicture.url || activePicture.thumbnailUrl" :alt="work.title"
                 class="stage-img" @click="toggleFullscreen" />
            <button class="nav prev" type="button" :disabled="pictures.length < 2" aria-label="上一张" @click="step(-1)">
              <LeftOutlined />
            </button>
            <button class="nav next" type="button" :disabled="pictures.length < 2" aria-label="下一张" @click="step(1)">
              <RightOutlined />
            </button>
            <div class="stage-bar">
              <a-button type="text" class="stage-btn" @click="openOriginal">
                <PictureOutlined /> 查看原图
              </a-button>
              <a-button type="text" class="stage-btn" @click="toggleFullscreen">
                <FullscreenOutlined /> {{ fullscreen ? '退出全屏' : '全屏看图' }}
              </a-button>
              <span class="stage-meta">
                <EyeOutlined /> {{ work.viewCount ?? 0 }} 次浏览
                <em v-if="pictures.length > 1">· {{ activeIndex + 1 }} / {{ pictures.length }}</em>
                <em class="hint">← → 切换</em>
              </span>
            </div>
          </div>

          <div v-if="pictures.length > 1" class="filmstrip">
            <button v-for="(pic, i) in pictures" :key="pic.id" type="button" class="strip"
                    :class="{ active: i === activeIndex }" @click="activeIndex = i">
              <img :src="pic.thumbnailUrl || pic.url" alt="" loading="lazy" />
            </button>
          </div>
        </section>

        <!-- ============ 副区：故事与互动 ============ -->
        <aside class="story">
          <header class="author">
            <router-link class="author-main" :to="'/photographers/' + work.authorId">
              <a-avatar :src="work.author?.userAvatar" :size="44">{{ authorText }}</a-avatar>
              <div>
                <strong>{{ work.author?.userName || '摄影师' }}</strong>
                <span class="muted">{{ formatDate(work.createTime) }} 发布</span>
              </div>
            </router-link>
            <a-space>
              <a-button v-if="!isSelf" :type="followed ? 'default' : 'primary'" size="small" @click="toggleFollow">
                {{ followed ? '已关注' : '+ 关注' }}
              </a-button>
              <a-dropdown v-if="!isSelf">
                <a-button type="text" size="small"><MoreOutlined /></a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="report" @click="reportOpen = true">举报该作品</a-menu-item>
                    <a-menu-item key="copy" @click="copyLink">复制链接</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </header>

          <h1 class="title">{{ work.title || '未命名作品' }}</h1>
          <p v-if="work.description" class="description">{{ work.description }}</p>

          <dl class="notes">
            <div v-if="meta.picWidth"><dt>画幅</dt><dd>{{ meta.picWidth }} × {{ meta.picHeight }}</dd></div>
            <div v-if="meta.picFormat"><dt>格式</dt><dd>{{ String(meta.picFormat).toUpperCase() }}</dd></div>
            <div v-if="meta.picColor"><dt>主色</dt><dd><i class="dot" :style="{ background: meta.picColor }" />{{ meta.picColor }}</dd></div>
            <div v-if="work.shootTime"><dt>拍摄</dt><dd>{{ formatDate(work.shootTime) }}</dd></div>
            <div v-if="work.shootLocation"><dt>地点</dt><dd>{{ work.shootLocation }}</dd></div>
            <div v-if="work.copyrightNotice"><dt>版权</dt><dd>{{ work.copyrightNotice }}</dd></div>
          </dl>

          <div class="actions">
            <LikeButton class="act" :liked="liked" :count="work.likeCount ?? 0" @toggle="toggleLike" />
            <button class="act" :class="{ on: favorited }" type="button" @click="toggleFavorite">
              <StarFilled v-if="favorited" /><StarOutlined v-else /> {{ work.favoriteCount ?? 0 }}
            </button>
            <button class="act" type="button" @click="scrollToComments">
              <MessageOutlined /> {{ comments.length || work.commentCount || 0 }}
            </button>
            <a-button size="small" class="download" :loading="downloading" @click="download">下载原图</a-button>
          </div>

          <!-- ============ 评论区（二级回复 + 点赞） ============ -->
          <section id="work-comments" class="comments">
            <div class="section-title">
              <span class="eyebrow">CONVERSATION</span>
              <h2>关于这组作品 <small>{{ comments.length }}</small></h2>
            </div>

            <div class="composer">
              <a-textarea v-model:value="commentDraft" :auto-size="{ minRows: 2, maxRows: 4 }"
                          :placeholder="replyTo ? ('回复 @' + replyTo.userName + '：') : '写下你的想法…'" />
              <div class="composer-bar">
                <a-button v-if="replyTo" type="text" size="small" @click="replyTo = null">取消回复</a-button>
                <a-button type="primary" size="small" :loading="commenting" :disabled="!commentDraft.trim()" @click="submitComment">
                  发表
                </a-button>
              </div>
            </div>

            <ul v-if="commentTree.length" class="comment-list">
              <li v-for="node in commentTree" :key="node.id" class="comment">
                <a-avatar :src="node.user?.userAvatar" :size="32">{{ (node.user?.userName || '光').slice(0, 1) }}</a-avatar>
                <div class="comment-main">
                  <div class="comment-head">
                    <strong>{{ node.user?.userName || '用户' }}</strong>
                    <span class="muted">{{ formatTime(node.createTime) }}</span>
                  </div>
                  <p class="comment-text">{{ node.content }}</p>
                  <div class="comment-ops">
                    <button type="button" :class="{ on: node.liked }" @click="toggleCommentLike(node)">
                      <LikeOutlined /> {{ node.likeCount ?? 0 }}
                    </button>
                    <button type="button" @click="startReply(node)">回复</button>
                  </div>

                  <ul v-if="node.children?.length" class="reply-list">
                    <li v-for="child in node.children" :key="child.id" class="reply">
                      <a-avatar :src="child.user?.userAvatar" :size="24">{{ (child.user?.userName || '光').slice(0, 1) }}</a-avatar>
                      <div class="comment-main">
                        <div class="comment-head">
                          <strong>{{ child.user?.userName || '用户' }}</strong>
                          <span class="muted">{{ formatTime(child.createTime) }}</span>
                        </div>
                        <p class="comment-text">{{ child.content }}</p>
                        <div class="comment-ops">
                          <button type="button" :class="{ on: child.liked }" @click="toggleCommentLike(child)">
                            <LikeOutlined /> {{ child.likeCount ?? 0 }}
                          </button>
                          <button type="button" @click="startReply(node)">回复</button>
                        </div>
                      </div>
                    </li>
                  </ul>
                </div>
              </li>
            </ul>
            <a-empty v-else description="还没有评论，来说点什么" />
          </section>

          <!-- ============ 相关推荐 ============ -->
          <section v-if="related.length" class="related">
            <div class="section-title">
              <span class="eyebrow">MORE LIKE THIS</span>
              <h2>你可能也喜欢</h2>
            </div>
            <div class="related-grid">
              <WorkCard v-for="item in related" :key="item.id" :work="item" ratio="70%" @open="openRelated" />
            </div>
          </section>
        </aside>
      </div>

      <a-empty v-else-if="!loading" description="作品暂不可见">
        <a-button type="primary" @click="router.push('/')">返回发现</a-button>
      </a-empty>
    </a-spin>

    <!-- 举报 -->
    <a-modal v-model:open="reportOpen" title="举报该作品" @ok="doReport">
      <a-select v-model:value="reportReason" style="width: 100%; margin-bottom: 12px">
        <a-select-option value="内容违规">内容违规</a-select-option>
        <a-select-option value="侵权盗用">侵权盗用</a-select-option>
        <a-select-option value="色情低俗">色情低俗</a-select-option>
        <a-select-option value="广告营销">广告营销</a-select-option>
        <a-select-option value="其他">其他</a-select-option>
      </a-select>
      <a-textarea v-model:value="reportDetail" :rows="3" placeholder="补充说明（可选）" />
    </a-modal>
  </div>
  </a-config-provider>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, theme as antdTheme } from 'ant-design-vue'
import {
  EyeOutlined, FullscreenOutlined, LeftOutlined, LikeFilled, LikeOutlined, MessageOutlined,
  MoreOutlined, PictureOutlined, RightOutlined, StarFilled, StarOutlined,
} from '@ant-design/icons-vue'
import WorkCard from '@/components/common/WorkCard.vue'
import LikeButton from '@/components/common/LikeButton.vue'
import {
  authorizeDownload, commentWork, favoriteWork, followUser, getWorkDetail, likeComment, likeWork,
  listComments, listRelatedWorks, unfavoriteWork, unfollowUser, unlikeComment, unlikeWork,
} from '@/api/photoCommunity'
import { submitReport } from '@/api/notification'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

/** 沉浸式暗色主题：与 data-theme="immersive" 的 CSS 变量同源（P0-8） */
const immersiveTheme = {
  algorithm: antdTheme.darkAlgorithm,
  token: { colorPrimary: '#8FA394', colorBgBase: '#0B0B0C', borderRadius: 10, fontSize: 14 },
}

const loading = ref(true)
const work = ref<any>(null)
const comments = ref<any[]>([])
const related = ref<any[]>([])
const activeIndex = ref(0)
const fullscreen = ref(false)
const liked = ref(false)
const favorited = ref(false)
const followed = ref(false)
const downloading = ref(false)
const commentDraft = ref('')
const commenting = ref(false)
const replyTo = ref<any>(null)
const reportOpen = ref(false)
const reportReason = ref('内容违规')
const reportDetail = ref('')

const workId = computed(() => String(route.params.id))
const pictures = computed(() => (work.value?.pictures?.length ? work.value.pictures : []))
const activePicture = computed(() => pictures.value[activeIndex.value] || pictures.value[0] || null)
const meta = computed(() => activePicture.value || {})
const authorText = computed(() => (work.value?.author?.userName || '光').slice(0, 1))
const isSelf = computed(() => String(work.value?.authorId) === String(loginUserStore.loginUser?.id))

/** 评论二级结构：parentId 分组（后端一直支持回复） */
const commentTree = computed(() => {
  const roots = comments.value.filter((c) => !c.parentId)
  const byParent: Record<string, any[]> = {}
  for (const c of comments.value) {
    if (c.parentId) {
      const key = String(c.parentId)
      byParent[key] = byParent[key] || []
      byParent[key].push(c)
    }
  }
  return roots.map((root) => ({ ...root, children: byParent[String(root.id)] || [] }))
})

function formatDate(value?: string) {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '-' : d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' })
}

function formatTime(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

function step(delta: number) {
  const total = pictures.value.length
  if (total < 2) return
  activeIndex.value = (activeIndex.value + delta + total) % total
}

function toggleFullscreen() {
  fullscreen.value = !fullscreen.value
  const el: any = document.querySelector('.work-page')
  if (fullscreen.value) el?.requestFullscreen?.().catch(() => {})
  else document.exitFullscreen?.().catch(() => {})
}

function scrollToComments() {
  document.querySelector('#work-comments')?.scrollIntoView({ behavior: 'smooth' })
}

function openOriginal() {
  const pic = activePicture.value
  if (pic?.id) router.push('/picture/' + pic.id)
}

function openRelated(item: any) {
  router.push('/works/' + item.id)
}

/** 键盘翻页 */
function onKeydown(e: KeyboardEvent) {
  if ((e.target as HTMLElement)?.tagName === 'TEXTAREA' || (e.target as HTMLElement)?.tagName === 'INPUT') return
  if (e.key === 'ArrowLeft') step(-1)
  if (e.key === 'ArrowRight') step(1)
}

async function load() {
  loading.value = true
  try {
    const res: any = await getWorkDetail(workId.value)
    work.value = res.data.code === 0 ? res.data.data : null
    if (!work.value) return
    liked.value = !!work.value.liked
    favorited.value = !!work.value.favorited
    followed.value = !!work.value.followed
    const [cRes, rRes]: any[] = await Promise.all([
      listComments(workId.value),
      listRelatedWorks(workId.value, 6),
    ])
    comments.value = cRes.data.code === 0 ? (cRes.data.data || []) : []
    related.value = rRes.data.code === 0 ? (rRes.data.data || []) : []
  } finally {
    loading.value = false
  }
}

async function reloadComments() {
  const res: any = await listComments(workId.value)
  if (res.data.code === 0) comments.value = res.data.data || []
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
  const next = !followed.value
  const res: any = next ? await followUser({ userId: work.value.authorId }) : await unfollowUser({ userId: work.value.authorId })
  if (res.data.code === 0) {
    followed.value = next
    message.success(next ? '已关注该作者' : '已取消关注')
  } else message.error(res.data.message)
}

function startReply(node: any) {
  replyTo.value = node
  const box = document.querySelector('.composer textarea') as HTMLTextAreaElement | null
  box?.focus()
}

async function submitComment() {
  if (!commentDraft.value.trim()) return
  commenting.value = true
  try {
    const res: any = await commentWork({
      workId: workId.value,
      content: commentDraft.value.trim(),
      parentId: replyTo.value?.id,
    })
    if (res.data.code === 0) {
      commentDraft.value = ''
      replyTo.value = null
      await reloadComments()
      work.value.commentCount = (work.value.commentCount || 0) + 1
    } else message.error(res.data.message)
  } finally {
    commenting.value = false
  }
}

async function toggleCommentLike(node: any) {
  const next = !node.liked
  const res: any = next ? await likeComment({ workId: node.id }) : await unlikeComment({ workId: node.id })
  if (res.data.code === 0) {
    node.liked = next
    node.likeCount = Math.max(0, (node.likeCount || 0) + (next ? 1 : -1))
  } else message.error(res.data.message)
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(location.href)
    message.success('链接已复制')
  } catch {
    message.warning('复制失败，请手动复制地址栏链接')
  }
}

async function doReport() {
  const res: any = await submitReport({
    targetType: 'work',
    targetId: workId.value,
    reason: reportReason.value,
    detail: reportDetail.value,
  })
  if (res.data.code === 0) {
    message.success('举报已提交，我们会尽快处理')
    reportOpen.value = false
    reportDetail.value = ''
  } else message.error(res.data.message || '提交失败')
}

async function download() {
  const pic = activePicture.value
  if (!pic?.id) return message.warning('该作品没有可下载的原图')
  downloading.value = true
  try {
    const requestId = 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
    const res: any = await authorizeDownload({ pictureId: pic.id, workId: workId.value, requestId })
    if (res.data.code !== 0) return message.error(res.data.message || '下载授权失败')
    message.success('已授权下载')
    window.open('http://localhost:8123' + res.data.data.downloadPath, '_blank')
  } finally {
    downloading.value = false
  }
}

onMounted(() => {
  void load()
  window.addEventListener('keydown', onKeydown)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.work-page { min-height: calc(100vh - var(--header-h)); background: var(--bg); color: var(--text-1); }
.work-layout { display: grid; grid-template-columns: minmax(0, 1fr) 460px; gap: 0; }

/* 主区 */
.viewer { padding: var(--space-5); display: flex; flex-direction: column; gap: var(--space-4); }
.stage { position: relative; flex: 1 1 auto; display: grid; place-items: center; min-height: 62vh; background: #0c0c0e; border-radius: var(--radius-lg); overflow: hidden; }
.stage.fullscreen { border-radius: 0; min-height: 100vh; }
.stage-img { max-width: 100%; max-height: calc(100vh - 220px); object-fit: contain; cursor: zoom-in; }
.nav { position: absolute; top: 50%; transform: translateY(-50%); display: grid; place-items: center; width: 44px; height: 44px; border: 1px solid rgba(255,255,255,.16); border-radius: 50%; background: rgba(20,20,22,.6); color: #EDEDEA; font-size: 16px; cursor: pointer; transition: background var(--dur-fast) var(--ease); }
.nav:hover:not(:disabled) { background: rgba(20,20,22,.92); }
.nav:disabled { opacity: .28; cursor: default; }
.nav.prev { left: 18px; }
.nav.next { right: 18px; }
.stage-bar { position: absolute; left: 16px; right: 16px; bottom: 14px; display: flex; align-items: center; justify-content: space-between; color: rgba(237,237,234,.86); font-size: var(--font-1); }
.stage-btn { color: #EDEDEA; }
.stage-meta { display: inline-flex; align-items: center; gap: 6px; }
.stage-meta em { font-style: normal; }
.stage-meta .hint { opacity: .5; margin-left: 8px; }
.filmstrip { display: flex; gap: 10px; overflow-x: auto; padding-bottom: 4px; }
.strip { flex: 0 0 auto; width: 74px; height: 58px; padding: 0; border: 1px solid var(--border); border-radius: var(--radius-sm); overflow: hidden; background: var(--surface-2); cursor: pointer; opacity: .55; transition: color var(--dur-fast) var(--ease), background-color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease); }
.strip img { width: 100%; height: 100%; object-fit: cover; display: block; }
.strip.active { opacity: 1; border-color: var(--accent); }

/* 副区 */
.story { padding: var(--space-5) var(--space-5) var(--space-7); border-left: 1px solid var(--border); background: var(--surface); }
.author { display: flex; align-items: center; justify-content: space-between; gap: var(--space-3); margin-bottom: var(--space-4); }
.author-main { display: flex; align-items: center; gap: var(--space-3); }
.author-main strong { display: block; font-size: var(--font-3); }
.author-main span { font-size: var(--font-1); }
.title { margin: 0 0 var(--space-2); font-size: var(--font-6); line-height: 1.3; }
.description { margin: 0 0 var(--space-4); color: var(--text-2); font-size: var(--font-2); line-height: 1.8; white-space: pre-wrap; }
.notes { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px var(--space-4); margin: 0 0 var(--space-4); padding: var(--space-4); background: var(--surface-2); border-radius: var(--radius); }
.notes div { display: flex; flex-direction: column; gap: 2px; }
.notes dt { color: var(--text-3); font-size: 12px; }
.notes dd { margin: 0; font-size: var(--font-2); display: flex; align-items: center; gap: 6px; }
.dot { width: 12px; height: 12px; border-radius: 3px; border: 1px solid var(--border-strong); }
.actions { display: flex; align-items: center; gap: var(--space-3); padding-bottom: var(--space-5); border-bottom: 1px solid var(--border); }
.act { display: inline-flex; align-items: center; gap: 6px; padding: 7px 12px; border: 1px solid var(--border); border-radius: var(--radius-pill); background: transparent; color: var(--text-2); font-size: var(--font-2); cursor: pointer; transition: color var(--dur-fast) var(--ease), background-color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease); }
.act:hover { border-color: var(--accent); color: var(--text-1); }
.act.on { background: var(--accent-soft); border-color: var(--accent); color: var(--accent-ink); }
.download { margin-left: auto; }

.section-title { display: flex; align-items: baseline; gap: 10px; margin: var(--space-5) 0 var(--space-3); }
.eyebrow { color: var(--text-3); font-size: 10px; letter-spacing: .18em; }
.section-title h2 { margin: 0; font-size: var(--font-4); }
.section-title small { color: var(--text-3); font-weight: 400; }
.composer { display: flex; flex-direction: column; gap: 8px; align-items: flex-end; margin-bottom: var(--space-4); }
.composer-bar { display: flex; gap: 8px; }
.comment-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--space-4); }
.comment, .reply { display: flex; gap: 10px; }
.comment-main { flex: 1 1 auto; min-width: 0; }
.comment-head { display: flex; align-items: baseline; gap: 8px; }
.comment-head strong { font-size: var(--font-2); }
.comment-head span { font-size: 12px; }
.comment-text { margin: 4px 0 6px; font-size: var(--font-2); color: var(--text-2); line-height: 1.7; white-space: pre-wrap; }
.comment-ops { display: flex; gap: 14px; }
.comment-ops button { border: none; background: none; padding: 0; color: var(--text-3); font-size: 12px; cursor: pointer; }
.comment-ops button:hover { color: var(--text-1); }
.comment-ops button.on { color: var(--accent-hover); }
.reply-list { list-style: none; margin: var(--space-3) 0 0; padding: var(--space-3) 0 0 var(--space-3); border-left: 2px solid var(--border); display: flex; flex-direction: column; gap: var(--space-3); }
.related-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--space-3); }

@media (max-width: 1024px) {
  .work-layout { grid-template-columns: 1fr; }
  .story { border-left: none; border-top: 1px solid var(--border); }
  .stage-img { max-height: 56vh; }
  .related-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
