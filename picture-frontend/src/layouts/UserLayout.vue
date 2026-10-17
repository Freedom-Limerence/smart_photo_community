<template>
  <div class="user-layout">
    <PublicHeader />
    <div class="personal-shell container">
      <div class="personal-heading">
        <a-avatar :src="loginUser.userAvatar" :size="88" class="personal-avatar">{{ (loginUser.userName || '摄').slice(0, 1) }}</a-avatar>
        <div class="personal-id">
          <span>MY SPACE / PHOTOGRAPHER</span>
          <h1>{{ loginUser.userName || '我的摄影空间' }}</h1>
          <p>{{ loginUser.userProfile || '记录生活中值得记住的光。' }}</p>
        </div>
      </div>
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
.user-layout { display: flex; flex-direction: column; min-height: 100vh; background: var(--bg); }
.personal-shell { flex: 1; }

/* 个人中心头部：衬线大标题 + 眉标，超宽屏下与版心同宽 */
.personal-heading { display: flex; align-items: center; gap: var(--space-5); padding: var(--space-8) 0 var(--space-6); }
.personal-avatar { flex: 0 0 auto; font-family: var(--font-display); font-size: 34px; }
.personal-id { min-width: 0; }
.personal-heading span { color: var(--text-3); font-size: var(--font-1); font-weight: 600; letter-spacing: .22em; }
.personal-heading h1 {
  margin: var(--space-3) 0 var(--space-1);
  font-family: var(--font-display);
  font-size: var(--font-8);
  font-weight: 400;
  letter-spacing: -.02em;
  line-height: 1.2;
}
.personal-heading p { margin: 0; color: var(--text-2); font-size: var(--font-2); }

/* 导航：滚动时吸附在页头下方，长页面也能随手切换 */
.personal-nav {
  position: sticky;
  top: var(--header-h);
  z-index: 20;
  display: flex;
  gap: clamp(var(--space-4), 2.4vw, var(--space-7));
  overflow-x: auto;
  border-bottom: 1px solid var(--border);
  background: color-mix(in srgb, var(--bg) 88%, transparent);
  backdrop-filter: saturate(180%) blur(12px);
  scrollbar-width: none;
}
.personal-nav::-webkit-scrollbar { display: none; }

.personal-nav a {
  position: relative;
  flex: 0 0 auto;
  padding: var(--space-3) 1px calc(var(--space-3) + 2px);
  color: var(--text-3);
  font-size: var(--font-3);
  transition: color var(--dur-fast) var(--ease);
}
.personal-nav a::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: var(--accent-ink);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform var(--dur) var(--ease);
}
.personal-nav a:hover { color: var(--text-1); }
.personal-nav a.active { color: var(--text-1); font-weight: 600; }
.personal-nav a.active::after { transform: scaleX(1); }

.personal-content { min-height: 62vh; padding: var(--space-6) 0 var(--space-8); }

@media (max-width: 700px) {
  .personal-heading { padding: var(--space-6) 0 var(--space-5); gap: var(--space-4); }
  .personal-avatar { font-size: 24px; }
  :deep(.personal-avatar.ant-avatar-88) { width: 64px !important; height: 64px !important; line-height: 64px !important; }
  .personal-content { padding-top: var(--space-5); }
  /* 窄屏导航横向滚动：左右渐隐提示可滚动（4.2 #2） */
  .personal-nav {
    mask-image: linear-gradient(90deg, transparent 0, #000 var(--space-4), #000 calc(100% - var(--space-4)), transparent 100%);
    -webkit-mask-image: linear-gradient(90deg, transparent 0, #000 var(--space-4), #000 calc(100% - var(--space-4)), transparent 100%);
  }
}
</style>
