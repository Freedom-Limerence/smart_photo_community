<template>
  <div class="work-detail">
    <div v-if="loading" class="detail-loading"><div class="skeleton" /></div>
    <template v-else-if="work">
      <section class="viewer" aria-label="作品图片浏览">
        <div class="viewer-backdrop" :style="{ backgroundImage: `url('${activePicture?.url || activePicture?.thumbnailUrl || ''}')` }" />
        <div class="viewer-frame">
          <img v-if="activePicture?.url || activePicture?.thumbnailUrl" :src="activePicture.url || activePicture.thumbnailUrl" :alt="work.title" @click="toggleFullscreen" />
          <div v-else class="no-image">影像暂不可用</div>
        </div>
        <div class="viewer-top viewer-reveal">
          <button class="icon-button" aria-label="返回" @click="router.back()"><ArrowLeftOutlined /></button>
          <router-link class="viewer-author" :to="'/photographers/' + work.authorId"><a-avatar :src="work.author?.userAvatar" :size="30">{{ authorText }}</a-avatar><span>{{ work.author?.userName || '摄影师' }}</span></router-link>
          <button class="icon-button top-close" aria-label="关闭作品" @click="router.push('/')"><CloseOutlined /></button>
        </div>
        <button v-if="pictures.length > 1" class="viewer-arrow left viewer-reveal" aria-label="上一张" @click="step(-1)"><LeftOutlined /></button>
        <button v-if="pictures.length > 1" class="viewer-arrow right viewer-reveal" aria-label="下一张" @click="step(1)"><RightOutlined /></button>
        <div class="viewer-bottom viewer-reveal">
          <span class="viewer-index">{{ String(activeIndex + 1).padStart(2, '0') }} <i>/</i> {{ String(pictures.length).padStart(2, '0') }}</span>
          <div class="viewer-actions">
            <button :class="{ on: liked }" @click="toggleLike" aria-label="喜欢作品"><HeartFilled v-if="liked" /><HeartOutlined v-else /> <span>{{ work.likeCount || 0 }}</span></button>
            <button :class="{ on: favorited }" @click="toggleFavorite" aria-label="收藏作品"><StarFilled v-if="favorited" /><StarOutlined v-else /></button>
            <button @click="scrollToDetails" aria-label="查看评论"><MessageOutlined /></button>
            <button @click="toggleFullscreen" :aria-label="fullscreen ? '退出全屏' : '全屏看图'"><FullscreenExitOutlined v-if="fullscreen" /><FullscreenOutlined v-else /></button>
          </div>
        </div>
      </section>

      <div v-if="pictures.length > 1" class="filmstrip"><button v-for="(pic, i) in pictures" :key="pic.id" :class="{ active: i === activeIndex }" :aria-label="`查看第 ${i + 1} 张`" @click="activeIndex = i"><img :src="pic.thumbnailUrl || pic.url" alt="" /></button></div>

      <section id="work-story" class="story">
        <div class="story-heading"><span class="eyebrow">A PHOTOGRAPHIC STORY</span><h1>{{ work.title }}</h1><p>{{ formatDate(work.createTime) }} <span v-if="work.shootLocation"> · {{ work.shootLocation }}</span></p></div>
        <div class="story-content">
          <div class="story-main"><p v-if="work.description" class="description">{{ work.description }}</p><p v-else class="description muted">作者还没有写下这组照片背后的故事。</p><div class="story-author"><router-link :to="'/photographers/' + work.authorId"><a-avatar :src="work.author?.userAvatar" :size="46">{{ authorText }}</a-avatar><span><strong>{{ work.author?.userName || '摄影师' }}</strong><small>查看摄影师主页 ↗</small></span></router-link><a-button v-if="!isSelf" size="small" @click="toggleFollow">{{ followed ? '已关注' : '+ 关注' }}</a-button></div></div>
          <div class="story-details"><span class="eyebrow">IMAGE NOTES</span><dl><div v-if="meta.picWidth"><dt>画幅</dt><dd>{{ meta.picWidth }} × {{ meta.picHeight }}</dd></div><div v-if="meta.picFormat"><dt>格式</dt><dd>{{ String(meta.picFormat).toUpperCase() }}</dd></div><div v-if="work.shootTime"><dt>拍摄于</dt><dd>{{ formatDate(work.shootTime) }}</dd></div><div v-if="work.copyrightNotice"><dt>版权</dt><dd>{{ work.copyrightNotice }}</dd></div></dl><a-button :loading="downloading" @click="download">下载原图 ↗</a-button></div>
        </div>
        <div class="comments"><div class="comments-title"><span class="eyebrow">CONVERSATION</span><h2>关于这组作品 <small>{{ comments.length }}</small></h2></div><div class="comment-composer"><a-textarea v-model:value="commentDraft" :auto-size="{ minRows: 2, maxRows: 4 }" maxlength="1000" placeholder="分享你看到的、感受到的…" /><a-button type="primary" :loading="commenting" :disabled="!commentDraft.trim()" @click="submitComment">发表评论</a-button></div><ul><li v-for="c in comments" :key="c.id"><a-avatar :src="c.user?.userAvatar" :size="34">{{ (c.user?.userName || '光').slice(0, 1) }}</a-avatar><div><strong>{{ c.user?.userName || '用户' }}</strong><time>{{ formatDate(c.createTime) }}</time><p>{{ c.content }}</p></div></li></ul><p v-if="!comments.length" class="no-comments">还没有评论，来留下第一句吧。</p></div>
      </section>
    </template>
    <div v-else class="not-found"><h1>作品暂不可见</h1><p>作品可能已下线，或你没有查看权限。</p><a-button @click="router.push('/')">返回发现</a-button></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowLeftOutlined, CloseOutlined, FullscreenExitOutlined, FullscreenOutlined, HeartFilled, HeartOutlined, LeftOutlined, MessageOutlined, RightOutlined, StarFilled, StarOutlined } from '@ant-design/icons-vue'
import { authorizeDownload, commentWork, favoriteWork, followUser, getWorkDetail, likeWork, listComments, unfavoriteWork, unfollowUser, unlikeWork } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { API_BASE_URL } from '@/request'

const route = useRoute(); const router = useRouter(); const loginUserStore = useLoginUserStore()
const loading = ref(true); const work = ref<any>(null); const comments = ref<any[]>([]); const activeIndex = ref(0)
const fullscreen = ref(false); const commentDraft = ref(''); const commenting = ref(false); const downloading = ref(false)
const liked = ref(false); const favorited = ref(false); const followed = ref(false)
const workId = computed(() => String(route.params.id))
const pictures = computed<any[]>(() => work.value?.pictures?.length ? work.value.pictures : [])
const activePicture = computed<any>(() => pictures.value[activeIndex.value] || { url: work.value?.coverUrl })
const meta = computed<any>(() => activePicture.value || {})
const authorText = computed(() => (work.value?.author?.userName || '光').slice(0, 1))
const isSelf = computed(() => String(work.value?.authorId) === String(loginUserStore.loginUser?.id))
function formatDate(value?: string) { if (!value) return ''; const d = new Date(value); return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) }
function step(delta: number) { if (pictures.value.length > 1) activeIndex.value = (activeIndex.value + delta + pictures.value.length) % pictures.value.length }
function onKeydown(e: KeyboardEvent) { if (e.key === 'ArrowLeft') step(-1); if (e.key === 'ArrowRight') step(1) }
function onFullscreenChange() { fullscreen.value = !!document.fullscreenElement }
function toggleFullscreen() { if (document.fullscreenElement) void document.exitFullscreen(); else void document.querySelector('.viewer')?.requestFullscreen() }
function scrollToDetails() { document.querySelector('#work-story')?.scrollIntoView({ behavior: 'smooth' }) }
async function load() { loading.value = true; activeIndex.value = 0; try { const res: any = await getWorkDetail(workId.value); work.value = res.data.code === 0 ? res.data.data : null; liked.value = Boolean(work.value?.liked); favorited.value = Boolean(work.value?.favorited); followed.value = Boolean(work.value?.followed); if (work.value) { const cr: any = await listComments(workId.value); comments.value = cr.data.code === 0 ? cr.data.data || [] : [] } } catch { work.value = null } finally { loading.value = false } }
async function toggleLike() { try { const res: any = liked.value ? await unlikeWork({ workId: workId.value }) : await likeWork({ workId: workId.value }); if (res.data.code !== 0) throw new Error(res.data.message); liked.value = !liked.value; work.value.likeCount = Math.max(0, Number(work.value.likeCount || 0) + (liked.value ? 1 : -1)) } catch (e: any) { message.error(e?.message || '请先登录') } }
async function toggleFavorite() { try { const res: any = favorited.value ? await unfavoriteWork({ workId: workId.value }) : await favoriteWork({ workId: workId.value }); if (res.data.code !== 0) throw new Error(res.data.message); favorited.value = !favorited.value; work.value.favoriteCount = Math.max(0, Number(work.value.favoriteCount || 0) + (favorited.value ? 1 : -1)) } catch (e: any) { message.error(e?.message || '请先登录') } }
async function toggleFollow() { try { const res: any = followed.value ? await unfollowUser({ userId: work.value.authorId }) : await followUser({ userId: work.value.authorId }); if (res.data.code !== 0) throw new Error(res.data.message); followed.value = !followed.value } catch (e: any) { message.error(e?.message || '请先登录') } }
async function submitComment() { if (!commentDraft.value.trim()) return; commenting.value = true; try { const res: any = await commentWork({ workId: workId.value, content: commentDraft.value.trim() }); if (res.data.code !== 0) throw new Error(res.data.message); commentDraft.value = ''; const cr: any = await listComments(workId.value); comments.value = cr.data.code === 0 ? cr.data.data || [] : []; work.value.commentCount = Number(work.value.commentCount || 0) + 1 } catch (e: any) { message.error(e?.message || '评论失败') } finally { commenting.value = false } }
async function download() { if (!activePicture.value?.id) return message.warning('没有可下载的原图'); downloading.value = true; try { const res: any = await authorizeDownload({ pictureId: activePicture.value.id, workId: workId.value, requestId: `web-${Date.now()}` }); if (res.data.code !== 0) throw new Error(res.data.message); window.open(new URL(res.data.data.downloadPath, API_BASE_URL).toString(), '_blank') } catch (e: any) { message.error(e?.message || '下载失败') } finally { downloading.value = false } }
onMounted(() => { void load(); window.addEventListener('keydown', onKeydown); document.addEventListener('fullscreenchange', onFullscreenChange) })
onBeforeUnmount(() => { window.removeEventListener('keydown', onKeydown); document.removeEventListener('fullscreenchange', onFullscreenChange) })
watch(() => route.params.id, load)
</script>

<style scoped>
.work-detail { min-height: 100vh; background: #f9f9f7; }
.viewer { position: relative; height: 100vh; min-height: 550px; background: #0b0c0b; color: #fff; overflow: hidden; }
.viewer-backdrop { position:absolute; inset:-60px; background-size:cover; background-position:center; opacity:.12; filter:blur(55px) brightness(.6); }
.viewer-frame { position:relative; display: grid; place-items: center; width: 100%; height: 100%; padding: 42px 8vw; }
.viewer-frame img { display: block; width: min(74vw, 1200px); height: min(82vh, 900px); max-width: 100%; max-height: 100%; object-fit: contain; cursor: zoom-in; }
.no-image { color: #8d908d; letter-spacing: .2em; font-size: 12px; }
.viewer-top { position: absolute; left: 28px; right: 28px; top: 24px; display: flex; align-items: center; gap: 16px; }
.viewer-author { display: flex; align-items: center; gap: 9px; color: rgba(255,255,255,.86); font-size: 12px; }
.top-close { margin-left: auto; }
.icon-button, .viewer-arrow, .viewer-actions button { display: grid; place-items: center; padding: 0; border: 0; background: rgba(15,17,16,.45); color: #fff; cursor: pointer; }
.icon-button { width: 36px; height: 36px; border-radius: 50%; }
.viewer-arrow { position: absolute; top: 50%; width: 45px; height: 45px; border-radius: 50%; transform: translateY(-50%); }
.viewer-arrow.left { left: 24px; } .viewer-arrow.right { right: 24px; }
.viewer-bottom { position: absolute; left: 34px; right: 34px; bottom: 27px; display: flex; justify-content: space-between; align-items: center; }
.viewer-index { font-size: 12px; letter-spacing: .16em; } .viewer-index i { color: #8e928d; font-style: normal; padding: 0 5px; }
.viewer-actions { display: flex; gap: 10px; }
.viewer-actions button { display: inline-flex; gap: 6px; width: auto; min-width: 34px; height: 34px; padding: 0 8px; border-radius: 50%; font-size: 13px; }
.viewer-actions button.on { color: #eabdb7; }
.viewer-reveal { opacity: 0; transition: opacity .24s; }
.viewer:hover .viewer-reveal, .viewer:focus-within .viewer-reveal { opacity: 1; }
.filmstrip { display: flex; justify-content: center; gap: 8px; padding: 18px; background: #151615; overflow-x: auto; }
.filmstrip button { width: 56px; height: 48px; padding: 0; border: 2px solid transparent; background: #333; cursor: pointer; opacity: .54; }
.filmstrip button.active { opacity: 1; border-color: #fff; }
.filmstrip img { width: 100%; height: 100%; object-fit: cover; }
.story { max-width: 1080px; margin: auto; padding: 90px 30px 120px; }
.eyebrow { color: #969b94; font-size: 10px; font-weight: 600; letter-spacing: .2em; }
.story-heading { padding-bottom: 45px; border-bottom: 1px solid #e4e6e1; }
.story-heading h1 { margin: 14px 0 10px; font-family: 'Noto Serif SC','Songti SC',serif; font-size: clamp(34px,4vw,54px); font-weight: 400; color: #262b26; }
.story-heading p { color: #929791; font-size: 12px; }
.story-content { display: grid; grid-template-columns: minmax(0, 1fr) 240px; gap: 90px; padding: 45px 0 70px; }
.description { margin: 0 0 70px; color: #525b52; font-size: 16px; line-height: 2.15; white-space: pre-wrap; }
.story-author { display: flex; align-items: center; justify-content: space-between; max-width: 430px; }
.story-author a { display: flex; align-items: center; gap: 11px; }
.story-author a span { display: flex; flex-direction: column; gap: 1px; }
.story-author strong { font-size: 13px; color: #323a32; } .story-author small { font-size: 11px; color: #9ca29c; }
.story-details dl { margin: 20px 0 28px; }
.story-details dl div { display: flex; justify-content: space-between; gap: 15px; padding: 10px 0; border-bottom: 1px solid #e8eae5; font-size: 11px; }
.story-details dt { color: #a0a49d; } .story-details dd { margin: 0; color: #4d574f; text-align: right; }
.comments { border-top: 1px solid #e4e6e1; padding-top: 60px; max-width: 720px; }
.comments-title h2 { margin: 7px 0 25px; font-family: 'Noto Serif SC','Songti SC',serif; font-size: 27px; font-weight: 400; }
.comments-title small { color: #aab0a9; font: 13px sans-serif; }
.comment-composer { display: flex; align-items: end; gap: 10px; margin-bottom: 40px; }
.comment-composer :deep(.ant-input) { background: transparent; }
.comments ul { margin: 0; padding: 0; list-style: none; }
.comments li { display: flex; gap: 12px; margin: 0 0 27px; }
.comments li > div { flex: 1; } .comments li strong { color: #333b34; font-size: 12px; } .comments li time { margin-left: 12px; color: #a4a9a2; font-size: 11px; } .comments li p { margin: 6px 0 0; color: #687168; line-height: 1.8; font-size: 13px; }
.no-comments { color: #a0a69f; font-size: 13px; }
.detail-loading { height: 100vh; padding: 20px; background: #0b0c0b; } .detail-loading > div { height: 100%; }
.not-found { display: grid; place-content: center; min-height: 100vh; text-align: center; }
@media (hover: none) { .viewer-reveal { opacity: 1; } }
@media (max-width: 700px) { .viewer-frame { padding: 40px 0; } .viewer-top { left: 14px; right: 14px; top: 12px; } .viewer-bottom { left: 15px; right: 15px; bottom: 12px; } .viewer-arrow.left { left: 8px; } .viewer-arrow.right { right: 8px; } .story { padding: 65px 20px 90px; } .story-content { grid-template-columns: 1fr; gap: 35px; } .description { margin-bottom: 35px; } .comment-composer { flex-direction: column; align-items: stretch; } }
</style>
