<template>
  <div class="follows-page container">
    <div class="heading"><button class="back" @click="router.back()">← 返回</button><span>PHOTOGRAPHER COMMUNITY</span><h1>{{ isFollowers ? '关注你的人' : '正在关注' }}</h1><p>在这里发现与你分享同样热爱的摄影师。</p></div>
    <div class="switcher"><router-link :to="basePath + '/following'" :class="{ active: !isFollowers }">关注 <small>{{ stats.following }}</small></router-link><router-link :to="basePath + '/followers'" :class="{ active: isFollowers }">粉丝 <small>{{ stats.followers }}</small></router-link></div>
    <a-spin :spinning="loading"><div v-if="people.length" class="people-grid"><router-link v-for="person in people" :key="person.id" class="person" :to="'/photographers/' + person.id"><a-avatar :src="person.userAvatar" :size="56">{{ (person.userName || '摄').slice(0, 1) }}</a-avatar><div><strong>{{ person.userName || '摄影师' }}</strong><p>{{ person.userProfile || '记录光与影' }}</p></div><span>↗</span></router-link></div><div v-else-if="!loading" class="empty">{{ isFollowers ? '还没有粉丝' : '还没有关注摄影师' }}</div></a-spin>
    <a-button v-if="people.length < total" class="load-more" :loading="loading" @click="loadMore">查看更多</a-button>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getFollowStats, listFollowers, listFollowing } from '@/api/photoCommunity'
const route = useRoute(); const router = useRouter(); const loginUser = useLoginUserStore()
const userId = computed(() => String(route.params.id || loginUser.loginUser?.id || ''))
const isFollowers = computed(() => route.path.endsWith('/followers'))
const basePath = computed(() => route.path.startsWith('/me/') ? '/me' : '/photographers/' + userId.value)
const people = ref<any[]>([]); const stats = ref({ following: 0, followers: 0 }); const loading = ref(false); const current = ref(1); const total = ref(0)
async function load(reset = true) {
  if (!userId.value) return
  if (reset) { current.value = 1; people.value = [] }
  loading.value = true
  try { const [sr, lr]: any[] = await Promise.all([getFollowStats(userId.value), (isFollowers.value ? listFollowers : listFollowing)(userId.value, current.value)]); if (sr.data.code === 0) stats.value = { following: Number(sr.data.data.following), followers: Number(sr.data.data.followers) }; if (lr.data.code === 0) { people.value = reset ? lr.data.data?.records || [] : [...people.value, ...(lr.data.data?.records || [])]; total.value = Number(lr.data.data?.total || 0) } } finally { loading.value = false }
}
async function loadMore() { current.value += 1; await load(false) }
watch([userId, isFollowers], () => void load(), { immediate: true })
</script>

<style scoped>
.follows-page { min-height: 70vh; padding-top: 58px; padding-bottom: 100px; }
.heading { position: relative; padding-bottom: 34px; }
.heading > span { display: block; color: #9a9e97; font-size: 10px; letter-spacing: .2em; }
.heading h1 { margin: 12px 0 6px; font-family: 'Noto Serif SC','Songti SC',serif; font-size: 40px; font-weight: 400; }
.heading p { color: #8e958d; }
.back { margin-bottom: 30px; padding: 0; background: none; border: 0; color: #687168; cursor: pointer; }
.switcher { display: flex; gap: 36px; margin: 0 0 32px; border-bottom: 1px solid #e3e6df; }
.switcher a { display: inline-flex; gap: 8px; padding: 10px 0 15px; color: #999e97; font-size: 14px; }
.switcher a.active { border-bottom: 2px solid #36483b; color: #2a382d; }
.switcher small { font-size: 11px; }
.people-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 14px; }
.person { display: flex; align-items: center; gap: 15px; min-width: 0; padding: 19px; background: white; border: 1px solid #e9ebe7; border-radius: 5px; }
.person > div { flex: 1; min-width: 0; }.person strong { color: #29352b; font-size: 14px; }.person p { margin: 3px 0 0; color: #9ba199; font-size: 12px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.person > span { color: #a5aaa4; }
.person:hover { border-color: #b5c0b6; }
.empty { padding: 80px 20px; text-align: center; color: #9ca29b; }
.load-more { display: block; margin: 28px auto; }
@media (max-width: 650px) { .people-grid { grid-template-columns: 1fr; } }
</style>
