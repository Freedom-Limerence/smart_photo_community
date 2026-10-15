<template>
  <div class="user-layout">
    <PublicHeader />
    <div class="personal-shell container">
      <div class="personal-heading"><span>MY SPACE / PHOTOGRAPHER</span><h1>{{ loginUser.userName || '我的摄影空间' }}</h1><p>{{ loginUser.userProfile || '记录生活中值得记住的光。' }}</p></div>
      <nav class="personal-nav" aria-label="个人中心导航">
        <router-link v-for="item in items" :key="item.path" :to="item.path" :class="{ active: selected(item.path) }">{{ item.label }}</router-link>
      </nav>
      <main class="personal-content"><router-view /></main>
    </div>
    <PublicFooter />
    <AiChatDock />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PublicHeader from '@/components/layout/PublicHeader.vue'
import PublicFooter from '@/components/layout/PublicFooter.vue'
import AiChatDock from '@/components/chat/AiChatDock.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
const route = useRoute(); const store = useLoginUserStore(); const loginUser = computed(() => store.loginUser || ({} as any))
const items = [
  { path: '/me', label: '主页' }, { path: '/me/works', label: '作品' }, { path: '/me/stats', label: '数据表现' }, { path: '/me/appeals', label: '申诉记录' }, { path: '/me/favorites', label: '收藏' },
  { path: '/me/following', label: '关注' }, { path: '/me/followers', label: '粉丝' },
  { path: '/me/gallery', label: '云图库' }, { path: '/me/settings', label: '设置' },
]
function selected(path: string) { return path === '/me' ? route.path === '/me' : route.path === path || route.path.startsWith(path + '/') || (path === '/me/settings' && ['/me/profile','/me/password'].includes(route.path)) }
</script>

<style scoped>
.user-layout { display: flex; flex-direction: column; min-height: 100vh; background: #fafaf8; }
.personal-shell { flex: 1; }
.personal-heading { padding: 55px 0 35px; }
.personal-heading span { color: #9da29a; font-size: 10px; letter-spacing: .2em; }
.personal-heading h1 { margin: 12px 0 7px; color: #283028; font-family: 'Noto Serif SC','Songti SC',serif; font-size: clamp(32px,4vw,48px); font-weight: 400; }
.personal-heading p { margin: 0; color: #929a91; font-size: 13px; }
.personal-nav { display: flex; gap: clamp(14px,3vw,42px); overflow-x: auto; border-bottom: 1px solid #e2e6e0; }
.personal-nav a { flex: 0 0 auto; padding: 0 1px 17px; color: #929890; font-size: 13px; }
.personal-nav a.active { color: #2d4032; border-bottom: 2px solid #344b39; }
.personal-content { min-height: 65vh; padding: 36px 0 100px; }
@media (max-width: 700px) { .personal-heading { padding: 34px 0 26px; } .personal-content { padding-top: 24px; } }
</style>
