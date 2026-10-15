<template>
  <div class="me-overview"><div class="profile-bar"><a-avatar :src="user.userAvatar" :size="76">{{ (user.userName || '摄').slice(0,1) }}</a-avatar><div class="profile-copy"><strong>{{ user.userName || '摄影师' }}</strong><p>{{ user.userProfile || '添加个人简介，让更多人认识你的摄影。' }}</p></div><a-button @click="router.push('/me/profile')">编辑资料</a-button></div>
    <div class="social-counts"><router-link to="/me/works"><strong>{{ totalWorks }}</strong><span>已发布作品</span></router-link><router-link to="/me/following"><strong>{{ follows.following }}</strong><span>关注</span></router-link><router-link to="/me/followers"><strong>{{ follows.followers }}</strong><span>粉丝</span></router-link></div>
    <div class="section-head"><div><span>YOUR CREATIVE JOURNEY</span><h2>你的公开作品</h2><p>每一组照片，都是你留给社区的一段观察。</p></div><a-button type="primary" @click="router.push('/me/create-work')">发布新作品</a-button></div>
    <a-spin :spinning="loading"><MasonryGrid v-if="works.length" :items="works" gap="24px"><template #default="{ item }"><WorkCard :work="item" @open="(w:any) => router.push('/works/' + w.id)" /></template></MasonryGrid><div v-else-if="!loading" class="empty-works"><p>你的作品墙还空着。上传照片并发布一组作品吧。</p><a-button @click="router.push('/me/upload')">先上传图片</a-button></div></a-spin>
    <div class="asset-link"><div><span>PRIVATE LIBRARY</span><h3>管理你的云图库</h3><p>整理图片、查看审核状态，为下一组创作做准备。</p></div><a-button @click="router.push('/me/gallery')">进入图库 ↗</a-button></div>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { getFollowStats, listWorksByAuthor } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
const router = useRouter(); const store = useLoginUserStore(); const user = computed(() => store.loginUser || ({} as any))
const works = ref<any[]>([]); const totalWorks = ref(0); const follows = ref({ followers: 0, following: 0 }); const loading = ref(false)
async function load() { if (!user.value.id) return; loading.value = true; try { const [w,s]: any[] = await Promise.all([listWorksByAuthor({ authorId: user.value.id, current: 1, size: 8 }), getFollowStats(user.value.id)]); if (w.data.code === 0) { works.value = w.data.data?.records || []; totalWorks.value = Number(w.data.data?.total || 0) } if (s.data.code === 0) follows.value = { followers: Number(s.data.data.followers), following: Number(s.data.data.following) } } finally { loading.value = false } }
onMounted(load)
</script>
<style scoped>
.me-overview { max-width:1100px; margin:auto; }.profile-bar { display:flex; align-items:center; gap:18px; padding:12px 0 30px; border-bottom:1px solid #e6e8e3; }.profile-copy { flex:1; min-width:0; }.profile-copy strong { font-size:19px; font-weight:500; }.profile-copy p { margin:3px 0 0; color:#8e978d; font-size:13px; }.social-counts { display:flex; gap:60px; padding:30px 0; }.social-counts a { display:flex; flex-direction:column; gap:2px; }.social-counts strong { color:#2b392d; font-size:23px; font-weight:500; }.social-counts span { color:#9ba29a; font-size:12px; }.section-head { display:flex; justify-content:space-between; align-items:end; margin:25px 0 28px; }.section-head span { color:#9aa199; letter-spacing:.18em; font-size:10px; }.section-head h2 { margin:7px 0 3px; font:400 29px 'Noto Serif SC','Songti SC',serif; }.section-head p { margin:0; color:#9aa199; font-size:12px; }.empty-works { padding:65px; text-align:center; border:1px dashed #d5ddd3; color:#8c978b; }.asset-link { display:flex; justify-content:space-between; align-items:center; gap:20px; margin-top:65px; padding:28px 35px; background:#e9eee8; border-radius:5px; }.asset-link span { font-size:10px; letter-spacing:.15em; color:#87968a; }.asset-link h3 { margin:5px 0; color:#344938; font:400 22px 'Noto Serif SC','Songti SC',serif; }.asset-link p { margin:0; color:#879689; font-size:12px; }@media(max-width:600px){.profile-bar{flex-wrap:wrap}.social-counts{gap:35px}.section-head{align-items:start;gap:20px;flex-direction:column}.asset-link{align-items:start;flex-direction:column}}
</style>
