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

    <!-- 今日下载配额环（FR-ACCESS 可视化） -->
    <div class="quota-card" :class="quotaState" v-if="quota">
      <svg class="ring" viewBox="0 0 120 120">
        <circle class="ring-bg" cx="60" cy="60" r="50" />
        <circle class="ring-fg" cx="60" cy="60" r="50" :stroke-dasharray="ringCircum" :stroke-dashoffset="ringOffset" />
      </svg>
      <div class="ring-center"><strong>{{ quota.usedCount }}</strong><span>/ {{ quota.quotaLimit ?? '∞' }}</span></div>
      <div class="quota-copy">
        <span class="quota-eyebrow">DOWNLOAD QUOTA / TODAY</span>
        <h3>今日下载配额</h3>
        <p>{{ quotaMsg }}</p>
      </div>
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
import { getFollowStats, listWorksByAuthor, getTodayQuota } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
const router = useRouter(); const store = useLoginUserStore(); const user = computed(() => store.loginUser || ({} as any))
const works = ref<any[]>([]); const totalWorks = ref(0); const follows = ref({ followers: 0, following: 0 }); const loading = ref(false)
async function load() { if (!user.value.id) return; loading.value = true; try { const [w,s]: any[] = await Promise.all([listWorksByAuthor({ authorId: user.value.id, current: 1, size: 8 }), getFollowStats(user.value.id)]); if (w.data.code === 0) { works.value = w.data.data?.records || []; totalWorks.value = Number(w.data.data?.total || 0) } if (s.data.code === 0) follows.value = { followers: Number(s.data.data.followers), following: Number(s.data.data.following) } } finally { loading.value = false } }
// 今日下载配额（受控下载可视化）
const quota = ref<{ usedCount: number; quotaLimit: number | null } | null>(null)
async function loadQuota() {
  try {
    const res: any = await getTodayQuota()
    if (res.data.code === 0 && res.data.data) {
      const q = res.data.data
      quota.value = { usedCount: q.usedCount ?? 0, quotaLimit: q.quotaLimit ?? null }
    }
  } catch { /* 配额规则未配置时静默 */ }
}
const RING = 2 * Math.PI * 50
const ringCircum = RING.toFixed(1)
const ratio = computed(() => {
  if (!quota.value || !quota.value.quotaLimit) return 0
  return Math.min(1, quota.value.usedCount / quota.value.quotaLimit)
})
const ringOffset = computed(() => (RING * (1 - ratio.value)).toFixed(1))
const quotaState = computed(() => {
  const r = ratio.value
  return r >= 0.95 ? 'danger' : r >= 0.7 ? 'warn' : 'ok'
})
const quotaMsg = computed(() => {
  const r = ratio.value
  if (!quota.value?.quotaLimit) return '当前未启用配额限制，可自由下载。'
  if (r >= 0.95) return '配额即将用尽——明日 0 点自动重置。'
  if (r >= 0.7) return '已接近当日上限，请合理安排下载。'
  return '受控下载保障创作者权益，配额每日 0 点重置。'
})

onMounted(() => { load(); loadQuota() })
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

/* 配额环卡 */
.quota-card {
  display: flex; align-items: center; gap: var(--space-5);
  margin-top: var(--space-4); padding: var(--space-5) var(--space-6);
  background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg);
}
.ring { width: 96px; height: 96px; flex-shrink: 0; transform: rotate(-90deg); }
.ring-bg { fill: none; stroke: var(--surface-2, rgba(0,0,0,.06)); stroke-width: 9; }
.ring-fg { fill: none; stroke: var(--accent); stroke-width: 9; stroke-linecap: round; transition: stroke-dashoffset .8s var(--ease), stroke .3s; }
.quota-card.warn .ring-fg { stroke: rgb(217, 154, 43); }
.quota-card.danger .ring-fg { stroke: #d9564b; }
.ring-center { position: absolute; }
.quota-card { position: relative; }
.ring-center { width: 96px; display: grid; place-items: center; position: absolute; left: calc(var(--space-5) + var(--space-6)); top: 50%; transform: translateY(-50%); pointer-events: none; }
.ring-center strong { font-family: var(--font-display); font-size: 22px; color: var(--text-1); line-height: 1; }
.ring-center span { font-size: 12px; color: var(--text-3); }
.quota-copy { display: flex; flex-direction: column; gap: 4px; }
.quota-eyebrow { color: var(--text-3); font-size: 12px; letter-spacing: .18em; }
.quota-copy h3 { margin: 0; color: var(--text-1); font-family: var(--font-display); font-size: var(--font-5); font-weight: 400; }
.quota-copy p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.quota-card.danger .quota-copy p { color: rgb(217, 86, 75); }

@media (max-width: 640px) {
  .profile-bar { flex-wrap: wrap; gap: var(--space-4); }
  .asset-link { flex-direction: column; align-items: flex-start; }
}
</style>
