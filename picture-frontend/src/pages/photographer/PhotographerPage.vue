<template>
  <div class="photographer-page">
    <div class="cover" :style="coverStyle">
      <div class="cover-mask" />
    </div>

    <div class="container profile-block">
      <a-avatar :src="user.userAvatar" :size="104" class="profile-avatar">{{ avatarText }}</a-avatar>

      <div class="profile-main">
        <h1>{{ user.userName || '摄影师' }}</h1>
        <p class="bio">{{ user.userProfile || '这位摄影师还没有填写简介' }}</p>
        <div class="stats">
          <span><strong>{{ works.length }}</strong> 作品</span>
          <span><strong>—</strong> 关注</span>
          <span><strong>—</strong> 粉丝</span>
        </div>
      </div>

      <div class="profile-actions">
        <a-button :type="followed ? 'default' : 'primary'" size="large" @click="toggleFollow">
          {{ followed ? '已关注' : '+ 关注' }}
        </a-button>
        <a-button size="large" @click="message2">私信</a-button>
      </div>
    </div>

    <div class="container">
      <a-tabs v-model:activeKey="tab" class="profile-tabs">
        <a-tab-pane key="works" tab="作品">
          <a-spin :spinning="loading">
            <MasonryGrid v-if="works.length" :items="works">
              <template #default="{ item }">
                <WorkCard :work="item" :ratio="ratioOf(item)" @open="openWork" />
              </template>
            </MasonryGrid>
            <a-empty v-else-if="!loading" description="TA 还没有公开作品" />
          </a-spin>
        </a-tab-pane>
        <a-tab-pane key="favorites" tab="收藏">
          <a-empty description="收藏列表对访客不可见" />
        </a-tab-pane>
        <a-tab-pane key="likes" tab="喜欢">
          <a-empty description="喜欢列表对访客不可见" />
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { followUser, listWorksByAuthor } from '@/api/photoCommunity'
import { getUserVoByIdUsingGet } from '@/api/userController'

const route = useRoute()
const router = useRouter()
const user = ref<any>({})
const works = ref<any[]>([])
const loading = ref(false)
const followed = ref(false)
const tab = ref('works')

const userId = computed(() => String(route.params.id))
const avatarText = computed(() => (user.value?.userName || '光').slice(0, 1))
const coverStyle = computed(() => {
  const cover = works.value[0]?.coverUrl || works.value[0]?.pictures?.[0]?.url
  return cover
    ? { backgroundImage: 'url(' + cover + ')', backgroundSize: 'cover', backgroundPosition: 'center' }
    : { background: 'linear-gradient(120deg, #2A332E, #4A5A50)' }
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
  loading.value = true
  try {
    const [uRes, wRes]: any[] = await Promise.all([
      getUserVoByIdUsingGet({ id: userId.value }),
      listWorksByAuthor({ authorId: userId.value, current: 1, size: 30 }),
    ])
    if (uRes.data.code === 0) user.value = uRes.data.data || {}
    if (wRes.data.code === 0) works.value = wRes.data.data?.records ?? []
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  const res: any = await followUser({ userId: userId.value })
  if (res.data.code === 0) {
    followed.value = true
    message.success('已关注')
  } else message.error(res.data.message)
}

function message2() {
  message.info('私信功能将随「消息中心」一起交付（迭代 2）')
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
.photographer-page { padding-bottom: var(--space-8); }

.cover { position: relative; height: 260px; background: var(--surface-2); }
.cover-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(20, 24, 22, .15), rgba(20, 24, 22, .55)); }

.profile-block { position: relative; display: flex; align-items: flex-end; gap: var(--space-5); margin-top: -60px; padding-bottom: var(--space-5); }

.profile-avatar { border: 4px solid var(--surface); box-shadow: var(--shadow-2); }

.profile-main { flex: 1 1 auto; padding-bottom: 6px; }
.profile-main h1 { margin: 0 0 6px; font-size: var(--font-7); }
.bio { margin: 0 0 10px; color: var(--text-2); max-width: 560px; }
.stats { display: flex; gap: var(--space-5); color: var(--text-2); font-size: var(--font-2); }
.stats strong { color: var(--text-1); font-size: var(--font-4); margin-right: 4px; }

.profile-actions { display: flex; gap: var(--space-3); padding-bottom: 6px; }

.profile-tabs { margin-top: var(--space-4); }

@media (max-width: 768px) {
  .cover { height: 180px; }
  .profile-block { flex-direction: column; align-items: flex-start; gap: var(--space-4); margin-top: -40px; }
  .profile-actions { width: 100%; }
}
</style>
