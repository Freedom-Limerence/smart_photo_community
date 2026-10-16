<template>
  <div class="follows-page container">
    <button class="back" type="button" @click="router.back()">← 返回</button>
    <PageHeader eyebrow="PHOTOGRAPHER COMMUNITY" :title="isFollowers ? '关注你的人' : '正在关注'"
                description="在这里发现与你分享同样热爱的摄影师。">
      <div class="switcher">
        <router-link :to="basePath + '/following'" :class="{ active: !isFollowers }">关注 <small>{{ stats.following }}</small></router-link>
        <router-link :to="basePath + '/followers'" :class="{ active: isFollowers }">粉丝 <small>{{ stats.followers }}</small></router-link>
      </div>
    </PageHeader>

    <a-spin :spinning="loading">
      <CardSkeleton v-if="loading && !people.length" variant="list" :count="6" />
      <div v-else-if="people.length" class="people-grid">
        <router-link v-for="person in people" :key="person.id" class="person" :to="'/photographers/' + person.id">
          <a-avatar :src="person.userAvatar" :size="56">{{ (person.userName || '摄').slice(0, 1) }}</a-avatar>
          <div><strong>{{ person.userName || '摄影师' }}</strong><p>{{ person.userProfile || '记录光与影' }}</p></div>
          <span class="person-go" aria-hidden="true">↗</span>
        </router-link>
      </div>
      <EmptyState v-else :title="isFollowers ? '还没有粉丝' : '还没有关注摄影师'"
                  :description="isFollowers ? '持续发布作品，让更多人认识你。' : '去社区看看，遇见值得关注的创作者。'">
        <a-button type="primary" @click="router.push('/community')">去社区逛逛</a-button>
      </EmptyState>
    </a-spin>

    <div v-if="people.length < total" class="load-more">
      <a-button :loading="loading" @click="loadMore">查看更多</a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
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
.follows-page { padding-top: var(--space-7); padding-bottom: var(--space-8); min-height: 60vh; }

.back {
  margin-bottom: var(--space-5);
  padding: 0;
  border: 0;
  background: none;
  color: var(--text-3);
  cursor: pointer;
  font-size: var(--font-2);
  transition: color var(--dur-fast) var(--ease);
}
.back:hover { color: var(--accent-ink); }

.switcher { display: flex; gap: var(--space-6); }
.switcher a {
  position: relative;
  display: inline-flex;
  gap: var(--space-2);
  padding: var(--space-2) 0 var(--space-3);
  color: var(--text-3);
  font-size: var(--font-3);
  transition: color var(--dur-fast) var(--ease);
}
.switcher a::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--accent-ink);
  transform: scaleX(0);
  transition: transform var(--dur) var(--ease);
}
.switcher a.active { color: var(--text-1); font-weight: 600; }
.switcher a.active::after { transform: scaleX(1); }
.switcher small { color: var(--text-3); font-size: var(--font-1); }

/* 自适应列数：宽屏一行 3~4 位，窄屏单列 */
.people-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(320px, 100%), 1fr));
  gap: var(--space-4);
  margin-top: var(--space-5);
}

.person {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-width: 0;
  padding: var(--space-4);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition: border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}
.person:hover { border-color: var(--border-strong); box-shadow: var(--shadow-2); transform: translateY(-2px); }
.person > div { flex: 1 1 auto; min-width: 0; }
.person strong { color: var(--text-1); font-size: var(--font-3); font-weight: 500; }
.person p { margin: 2px 0 0; color: var(--text-3); font-size: var(--font-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.person-go { flex: 0 0 auto; color: var(--text-3); transition: color var(--dur-fast) var(--ease); }
.person:hover .person-go { color: var(--accent); }

.load-more { display: flex; justify-content: center; margin-top: var(--space-6); }
</style>
