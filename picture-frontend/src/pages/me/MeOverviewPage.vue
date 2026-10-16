<template>
  <div class="me-overview">
    <div class="profile-bar surface-card">
      <a-avatar :src="user.userAvatar" :size="76">{{ (user.userName || '摄').slice(0,1) }}</a-avatar>
      <div class="profile-copy">
        <strong>{{ user.userName || '摄影师' }}</strong>
        <p>{{ user.userProfile || '添加个人简介，让更多人认识你的摄影。' }}</p>
      </div>
      <a-button @click="router.push('/me/profile')">编辑资料</a-button>
    </div>

    <div class="social-counts">
      <router-link to="/me/works"><strong>{{ totalWorks }}</strong><span>已发布作品</span></router-link>
      <router-link to="/me/following"><strong>{{ follows.following }}</strong><span>关注</span></router-link>
      <router-link to="/me/followers"><strong>{{ follows.followers }}</strong><span>粉丝</span></router-link>
    </div>

    <div class="section-head">
      <div><span class="eyebrow">YOUR CREATIVE JOURNEY</span><h2>你的公开作品</h2><p>每一组照片，都是你留给社区的一段观察。</p></div>
      <a-button type="primary" @click="router.push('/me/create-work')">发布新作品</a-button>
    </div>

    <a-spin :spinning="loading">
      <CardSkeleton v-if="loading && !works.length" variant="masonry" :count="4" ratio="72%" />
      <MasonryGrid v-else-if="works.length" :items="works">
        <template #default="{ item }"><WorkCard :work="item" @open="(w:any) => router.push('/works/' + w.id)" /></template>
      </MasonryGrid>
      <EmptyState v-else dashed title="你的作品墙还空着" description="上传照片并发布一组作品，让这里开始有光。">
        <a-button type="primary" @click="router.push('/me/upload')">先上传图片</a-button>
      </EmptyState>
    </a-spin>

    <div class="asset-link">
      <div><span>PRIVATE LIBRARY</span><h3>管理你的云图库</h3><p>整理图片、查看审核状态，为下一组创作做准备。</p></div>
      <a-button @click="router.push('/me/gallery')">进入图库 ↗</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { getFollowStats, listWorksByAuthor } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
const router = useRouter(); const store = useLoginUserStore(); const user = computed(() => store.loginUser || ({} as any))
const works = ref<any[]>([]); const totalWorks = ref(0); const follows = ref({ followers: 0, following: 0 }); const loading = ref(false)
async function load() { if (!user.value.id) return; loading.value = true; try { const [w,s]: any[] = await Promise.all([listWorksByAuthor({ authorId: user.value.id, current: 1, size: 8 }), getFollowStats(user.value.id)]); if (w.data.code === 0) { works.value = w.data.data?.records || []; totalWorks.value = Number(w.data.data?.total || 0) } if (s.data.code === 0) follows.value = { followers: Number(s.data.data.followers), following: Number(s.data.data.following) } } finally { loading.value = false } }
onMounted(load)
</script>
<style scoped>
/* 个人主页：与其它个人中心页面同宽（由 UserLayout 的版心统一约束） */
.me-overview { display: flex; flex-direction: column; }

.profile-bar {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  padding: var(--space-5);
}
.profile-copy { flex: 1; min-width: 0; }
.profile-copy strong { font-size: var(--font-5); font-weight: 500; }
.profile-copy p { margin: var(--space-1) 0 0; color: var(--text-3); font-size: var(--font-2); }

/* 三个数据入口做成卡片：整块可点，悬停有反馈 */
.social-counts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(220px, 100%), 1fr));
  gap: var(--space-4);
  margin-top: var(--space-4);
}
.social-counts a {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition: border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}
.social-counts a:hover { border-color: var(--border-strong); box-shadow: var(--shadow-2); transform: translateY(-2px); }
.social-counts strong { color: var(--text-1); font-family: var(--font-display); font-size: var(--font-7); font-weight: 400; line-height: 1.1; }
.social-counts span { color: var(--text-3); font-size: var(--font-1); letter-spacing: .06em; }

/* 图库入口：一条浅色 CTA 带，与页面留白形成节奏 */
.asset-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  margin-top: var(--space-8);
  padding: var(--space-6);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background:
    radial-gradient(120% 140% at 0% 0%, var(--accent-soft) 0%, transparent 60%),
    var(--surface-2);
}
.asset-link span { color: var(--text-3); font-size: 10px; letter-spacing: .18em; }
.asset-link h3 { margin: var(--space-2) 0 var(--space-1); color: var(--text-1); font-family: var(--font-display); font-size: var(--font-6); font-weight: 400; }
.asset-link p { margin: 0; color: var(--text-2); font-size: var(--font-2); }

@media (max-width: 640px) {
  .profile-bar { flex-wrap: wrap; gap: var(--space-4); }
  .asset-link { flex-direction: column; align-items: flex-start; }
}
</style>
