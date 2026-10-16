<template>
  <div class="photographer-page">
    <div class="container profile-block">
      <a-avatar :src="user.userAvatar" :size="104" class="profile-avatar">{{ avatarText }}</a-avatar>

      <div class="profile-main">
        <span class="profile-eyebrow">PHOTOGRAPHER / COMMUNITY</span>
        <h1>{{ user.userName || '摄影师' }}</h1>
        <p class="bio">{{ user.userProfile || '这位摄影师还没有填写简介' }}</p>
        <div class="stats">
          <span><strong>{{ totalWorks }}</strong> 作品</span>
          <span><strong>{{ totalLikes }}</strong> 获赞</span>
          <router-link :to="'/photographers/' + userId + '/following'"><strong>{{ stats.following }}</strong> 关注</router-link>
          <router-link :to="'/photographers/' + userId + '/followers'"><strong>{{ stats.followers }}</strong> 粉丝</router-link>
        </div>
      </div>

      <div class="profile-actions">
        <a-button v-if="!isSelf" :type="followed ? 'default' : 'primary'" size="large" @click="toggleFollow">
          {{ followed ? '已关注' : '+ 关注' }}
        </a-button>
        <a-button v-else size="large" @click="router.push('/me/profile')">编辑资料</a-button>
        <a-button size="large" @click="shareProfile">分享主页</a-button>
        <a-dropdown v-if="!isSelf">
          <a-button size="large"><MoreOutlined /></a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="share" @click="shareProfile">复制主页链接</a-menu-item>
              <a-menu-item key="report" @click="reportOpen = true">举报该用户</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </div>

    <div class="container works-section">
      <div class="works-head">
        <div><span>PORTFOLIO</span><h2>作品集 <small>{{ totalWorks }} 件</small></h2></div>
        <div class="works-tools">
          <a-radio-group v-model:value="sort" button-style="solid" size="small">
            <a-radio-button value="latest">最新</a-radio-button>
            <a-radio-button value="hot">最热</a-radio-button>
            <a-radio-button value="view">最多浏览</a-radio-button>
          </a-radio-group>
          <a-button size="small" :loading="loading" @click="load">刷新</a-button>
        </div>
      </div>

      <a-spin :spinning="loading">
        <MasonryGrid v-if="sortedWorks.length" :items="sortedWorks">
          <template #default="{ item }">
            <WorkCard :work="item" :ratio="ratioOf(item)" @open="openWork" />
          </template>
        </MasonryGrid>
        <a-empty v-else-if="!loading" description="TA 还没有公开作品">
          <a-button v-if="isSelf" type="primary" @click="router.push('/community/create')">去发布作品</a-button>
        </a-empty>
      </a-spin>
    </div>

    <a-modal v-model:open="reportOpen" title="举报该用户" @ok="doReport">
      <a-select v-model:value="reportReason" style="width: 100%; margin-bottom: 12px">
        <a-select-option value="侵权盗用">侵权盗用</a-select-option>
        <a-select-option value="恶意刷屏">恶意刷屏</a-select-option>
        <a-select-option value="内容违规">内容违规</a-select-option>
        <a-select-option value="其他">其他</a-select-option>
      </a-select>
      <a-textarea v-model:value="reportDetail" :rows="3" placeholder="补充说明（可选）" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { MoreOutlined } from '@ant-design/icons-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { followUser, getFollowStats, listWorksByAuthor, unfollowUser } from '@/api/photoCommunity'
import { submitReport } from '@/api/notification'
import { getUserVoByIdUsingGet } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUser = useLoginUserStore()
const user = ref<any>({})
const works = ref<any[]>([])
const loading = ref(false)
const followed = ref(false)
const totalWorks = ref(0)
const stats = ref({ following: 0, followers: 0 })
const sort = ref<'latest' | 'hot' | 'view'>('latest')
const reportOpen = ref(false)
const reportReason = ref('侵权盗用')
const reportDetail = ref('')

const userId = computed(() => String(route.params.id ?? ''))
const avatarText = computed(() => (user.value?.userName || '光').slice(0, 1))
const isSelf = computed(() => String(loginUser.loginUser?.id ?? '') === userId.value)
const totalLikes = computed(() => works.value.reduce((sum, w) => sum + Number(w.likeCount || 0), 0))

const sortedWorks = computed(() => {
  const list = [...works.value]
  const num = (v: any) => Number(v ?? 0)
  if (sort.value === 'hot') return list.sort((a, b) => num(b.likeCount) - num(a.likeCount))
  if (sort.value === 'view') return list.sort((a, b) => num(b.viewCount) - num(a.viewCount))
  return list.sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime())
})

function ratioOf(work: any) {
  const pic = work?.pictures?.[0]
  if (!pic?.picWidth || !pic?.picHeight) return '66%'
  return Math.round((pic.picHeight / pic.picWidth) * 100) + '%'
}

function openWork(work: any) {
  router.push('/works/' + work.id)
}

async function load() {
  if (!userId.value) return
  loading.value = true
  try {
    const [userRes, worksRes, statsRes]: any[] = await Promise.all([
      getUserVoByIdUsingGet({ id: userId.value }),
      listWorksByAuthor({ authorId: userId.value, current: 1, size: 60 }),
      getFollowStats(userId.value),
    ])
    if (userRes.data.code === 0) user.value = userRes.data.data || {}
    if (worksRes.data.code === 0) {
      works.value = worksRes.data.data?.records || []
      totalWorks.value = Number(worksRes.data.data?.total ?? works.value.length)
    }
    if (statsRes.data.code === 0) {
      stats.value = {
        following: Number(statsRes.data.data?.followingCount ?? 0),
        followers: Number(statsRes.data.data?.followerCount ?? 0),
      }
      followed.value = !!statsRes.data.data?.followed
    }
  } catch {
    /* 保持空态 */
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  const next = !followed.value
  const res: any = next ? await followUser({ userId: userId.value }) : await unfollowUser({ userId: userId.value })
  if (res.data.code === 0) {
    followed.value = next
    stats.value.followers = Math.max(0, stats.value.followers + (next ? 1 : -1))
    message.success(next ? '已关注' : '已取消关注')
  } else {
    message.error(res.data.message || '操作失败')
  }
}

async function shareProfile() {
  try {
    await navigator.clipboard.writeText(location.origin + '/photographers/' + userId.value)
    message.success('主页链接已复制')
  } catch {
    message.warning('复制失败，请手动复制地址栏链接')
  }
}

async function doReport() {
  const res: any = await submitReport({
    targetType: 'user',
    targetId: userId.value,
    reason: reportReason.value,
    detail: reportDetail.value,
  })
  if (res.data.code === 0) {
    message.success('举报已提交，我们会尽快核实')
    reportOpen.value = false
    reportDetail.value = ''
  } else {
    message.error(res.data.message || '提交失败')
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.profile-block { display: flex; align-items: center; gap: var(--space-5); padding: var(--space-6) var(--space-5); flex-wrap: wrap; }
.profile-main { flex: 1 1 320px; min-width: 0; }
.profile-eyebrow { color: var(--text-3); font-size: 11px; letter-spacing: .2em; }
.profile-main h1 { margin: 6px 0 8px; font-size: var(--font-7); }
.bio { margin: 0 0 12px; color: var(--text-2); }
.stats { display: flex; flex-wrap: wrap; gap: 22px; color: var(--text-2); font-size: var(--font-2); }
.stats strong { color: var(--text-1); font-size: var(--font-4); }
.profile-actions { display: flex; align-items: center; gap: 10px; }
.works-section { padding: var(--space-5) var(--space-5) var(--space-8); }
.works-head { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; flex-wrap: wrap; margin-bottom: var(--space-4); }
.works-head span { color: var(--text-3); font-size: 11px; letter-spacing: .18em; }
.works-head h2 { margin: 4px 0 0; font-size: var(--font-6); }
.works-head h2 small { color: var(--text-3); font-size: var(--font-3); font-weight: 400; }
.works-tools { display: flex; align-items: center; gap: 8px; }
</style>
