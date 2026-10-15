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
          <router-link :to="'/photographers/' + userId + '/following'"><strong>{{ stats.following }}</strong> 关注</router-link>
          <router-link :to="'/photographers/' + userId + '/followers'"><strong>{{ stats.followers }}</strong> 粉丝</router-link>
        </div>
      </div>

      <div class="profile-actions">
        <a-button v-if="!isSelf" :type="followed ? 'default' : 'primary'" size="large" @click="toggleFollow">
          {{ followed ? '已关注' : '+ 关注' }}
        </a-button>
        <a-button v-else size="large" @click="router.push('/me/profile')">编辑资料</a-button>
      </div>
    </div>

    <div class="container works-section">
      <div class="works-head"><span>PORTFOLIO</span><h2>作品集</h2></div>
          <a-spin :spinning="loading">
            <MasonryGrid v-if="works.length" :items="works">
              <template #default="{ item }">
                <WorkCard :work="item" :ratio="ratioOf(item)" @open="openWork" />
              </template>
            </MasonryGrid>
            <a-empty v-else-if="!loading" description="TA 还没有公开作品" />
          </a-spin>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { followUser, unfollowUser, getFollowStats, listWorksByAuthor } from '@/api/photoCommunity'
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

const userId = computed(() => String(route.params.id))
const isSelf = computed(() => String(loginUser.loginUser?.id) === userId.value)
const avatarText = computed(() => (user.value?.userName || '光').slice(0, 1))

function ratioOf(work: any) {
  const pic = work?.pictures?.[0]
  if (!pic?.picWidth || !pic?.picHeight) return '66%'
  return Math.round((pic.picHeight / pic.picWidth) * 100) + '%'
}

function openWork(work: any) {
  router.push('/works/' + work.id)
}

async function load() {
  loading.value = true
  try {
    const [uRes, wRes, sRes]: any[] = await Promise.all([
      getUserVoByIdUsingGet({ id: userId.value }),
      listWorksByAuthor({ authorId: userId.value, current: 1, size: 30 }),
      getFollowStats(userId.value),
    ])
    if (uRes.data.code === 0) user.value = uRes.data.data || {}
    if (wRes.data.code === 0) { works.value = wRes.data.data?.records ?? []; totalWorks.value = Number(wRes.data.data?.total || 0) }
    if (sRes.data.code === 0) { stats.value = { following: Number(sRes.data.data.following), followers: Number(sRes.data.data.followers) }; followed.value = !!sRes.data.data.followed }
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  const res: any = await (followed.value ? unfollowUser : followUser)({ userId: userId.value })
  if (res.data.code === 0) {
    followed.value = !followed.value
    stats.value.followers += followed.value ? 1 : -1
    message.success(followed.value ? '已关注' : '已取消关注')
  } else message.error(res.data.message)
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.photographer-page { padding-bottom: 100px; background:#fafaf8; min-height:100vh; }
.profile-block { display:flex; align-items:center; gap:30px; padding-top:76px; padding-bottom:65px; border-bottom:1px solid #e5e8e2; }
.profile-avatar { flex:0 0 auto; border:4px solid #fff; box-shadow:0 5px 20px rgba(37,50,40,.08); }
.profile-main { flex:1; min-width:0; }.profile-eyebrow { color:#9aa39a; font-size:10px; letter-spacing:.2em; }
.profile-main h1 { margin:9px 0 8px; font:400 clamp(30px,3.5vw,45px) 'Noto Serif SC','Songti SC',serif; color:#2b352c; }
.bio { margin:0 0 18px; color:#929b92; font-size:13px; max-width:540px; }
.stats { display:flex; gap:26px; color:#919a90; font-size:12px; }.stats strong { margin-right:5px; color:#3c4b3e; font-size:15px; }
.profile-actions { display:flex; gap:10px; }
.works-section { padding-top:43px; }.works-head { margin-bottom:28px; }.works-head span { color:#a1aaa0; font-size:10px; letter-spacing:.2em; }.works-head h2 { margin:7px 0 0; font:400 27px 'Noto Serif SC','Songti SC',serif; color:#2d392f; }
@media (max-width:700px) { .profile-block { flex-wrap:wrap; padding-top:45px; padding-bottom:35px; }.profile-actions { width:100%; } }
</style>
