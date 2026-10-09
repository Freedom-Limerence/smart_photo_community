<template>
  <div class="community-page">
    <section class="community-hero">
      <div>
        <p class="eyebrow">SMART PHOTOGRAPHY COMMUNITY</p>
        <h1>把每一张照片，整理成可交流的作品</h1>
        <p class="hero-copy">从云端资产管理开始，用 AI 完成摄影编目，再把作品交给社区和专题展览。</p>
        <a-space>
          <a-button type="primary" size="large" @click="router.push('/ai-workbench')">打开 AI 工作台</a-button>
          <a-button size="large" @click="router.push('/add_picture')">上传照片</a-button>
        </a-space>
      </div>
      <div class="hero-stats">
        <div><strong>{{ total }}</strong><span>公开作品</span></div>
        <div><strong>AI</strong><span>摄影编目</span></div>
        <div><strong>{{ quotaLabel }}</strong><span>今日下载配额</span></div>
      </div>
    </section>

    <section class="feed-head">
      <div><h2>摄影作品流</h2><span>按发布时间浏览社区作品</span></div>
      <a-button @click="loadWorks">刷新</a-button>
    </section>
    <a-spin :spinning="loading">
      <a-row :gutter="[20, 20]">
        <a-col v-for="work in works" :key="work.id" :xs="24" :sm="12" :lg="8">
          <a-card class="work-card" :bordered="false">
            <div class="work-cover" :style="{ backgroundImage: work.thumbnailUrl || work.coverUrl ? `url(${work.thumbnailUrl || work.coverUrl})` : 'linear-gradient(135deg, #20322e, #8fa895)' }">
              <span>{{ work.visibility === 'PUBLIC' ? '公开作品' : '受限作品' }}</span>
            </div>
            <h3>{{ work.title }}</h3>
            <p>{{ work.description || '作者还没有添加作品说明' }}</p>
            <a-space>
              <a-button type="text" @click="doLike(work.id)">赞 {{ work.likeCount || 0 }}</a-button>
              <a-button type="text" @click="doFavorite(work.id)">{{ favoriteIds.has(work.id) ? '已收藏' : '收藏' }} {{ work.favoriteCount || 0 }}</a-button>
              <a-button type="text" @click="openComment(work)">评论 {{ work.commentCount || 0 }}</a-button>
            </a-space>
          </a-card>
        </a-col>
      </a-row>
      <a-empty v-if="!loading && works.length === 0" description="还没有公开作品，先上传一组照片吧" />
    </a-spin>

    <a-modal v-model:open="commentOpen" title="发表评论" ok-text="发布" @ok="submitComment">
      <a-textarea v-model:value="commentText" :rows="4" maxlength="1000" show-count placeholder="分享你的观感" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { commentWork, favoriteWork, getTodayQuota, likeWork, listWorks, unfavoriteWork } from '@/api/photoCommunity'

const router = useRouter()
const works = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const quota = ref<any>()
const selectedWork = ref<any>()
const commentOpen = ref(false)
const commentText = ref('')
const favoriteIds = ref<Set<number>>(new Set())
const quotaLabel = computed(() => quota.value ? `${Math.max(0, quota.value.quotaLimit - quota.value.usedCount)}/${quota.value.quotaLimit}` : '--')

async function loadWorks() {
  loading.value = true
  const res: any = await listWorks({ current: 1, size: 12 })
  if (res.data.code === 0) { works.value = res.data.data?.records || []; total.value = res.data.data?.total || 0 }
  loading.value = false
}
async function doLike(workId: number) { const res: any = await likeWork({ workId }); if (res.data.code === 0) { message.success('已点赞'); await loadWorks() } }
async function doFavorite(workId: number) {
  const isFav = favoriteIds.value.has(workId)
  const res: any = isFav ? await unfavoriteWork({ workId }) : await favoriteWork({ workId })
  if (res.data.code === 0) { if (isFav) favoriteIds.value.delete(workId); else favoriteIds.value.add(workId); message.success(isFav ? '已取消收藏' : '已收藏'); await loadWorks() }
}
function openComment(work: any) { selectedWork.value = work; commentText.value = ''; commentOpen.value = true }
async function submitComment() { if (!commentText.value.trim()) return; const res: any = await commentWork({ workId: selectedWork.value.id, content: commentText.value }); if (res.data.code === 0) { message.success('评论已发布'); commentOpen.value = false; await loadWorks() } }
onMounted(async () => { await Promise.all([loadWorks(), getTodayQuota().then((res: any) => { if (res.data.code === 0) quota.value = res.data.data })]) })
</script>

<style scoped>
.community-page { max-width: 1180px; margin: 0 auto; }
.community-hero { display: flex; justify-content: space-between; gap: 40px; padding: 34px 0 44px; border-bottom: 1px solid #e6ebe7; }
.eyebrow { color: #5a836d; letter-spacing: 2px; font-size: 12px; margin: 0 0 12px; }
h1 { font-size: 40px; line-height: 1.15; max-width: 620px; margin: 0 0 16px; color: #20322e; }
.hero-copy { max-width: 560px; color: #65736c; font-size: 16px; margin-bottom: 24px; }
.hero-stats { display: flex; gap: 28px; align-items: end; padding-bottom: 10px; }
.hero-stats div { display: grid; gap: 5px; min-width: 88px; }
.hero-stats strong { color: #20322e; font-size: 25px; }
.hero-stats span, .feed-head span { color: #7a8780; font-size: 13px; }
.feed-head { display: flex; justify-content: space-between; align-items: end; margin: 30px 0 18px; }
.feed-head h2 { margin: 0 0 4px; color: #20322e; }
.work-card { border: 1px solid #e5ebe7; box-shadow: none; }
.work-cover { height: 160px; display: flex; align-items: flex-start; justify-content: flex-end; padding: 12px; background-size: cover; }
.work-cover span { background: rgba(255,255,255,.85); color: #355443; padding: 4px 8px; font-size: 12px; }
.work-card h3 { margin: 16px 0 6px; }
.work-card p { min-height: 44px; color: #748078; }
@media (max-width: 760px) { .community-hero { display: block; } h1 { font-size: 32px; } .hero-stats { margin-top: 28px; } }
</style>
