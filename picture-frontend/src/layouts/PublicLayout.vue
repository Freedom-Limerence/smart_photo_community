<template>
  <div class="public-layout" :class="{ 'is-immersive': isWorkDetail }">
    <!-- 键盘跳转：Tab 第一站直达内容区（P0-7） -->
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <!-- 摄影演示模式提示条：仅开发环境 ?demo=photography 时显示 -->
    <PhotographyPreviewBar />
    <PublicHeader v-if="!isWorkDetail && !isBare" />
    <AnnouncementBar v-if="!isWorkDetail && !isBare" />
    <main id="main-content" class="public-main">
      <!-- 过渡只挂一次（4.1 #8）：页面级过渡统一由 App.vue 承担，布局内不再叠加 -->
      <router-view />
    </main>
    <PublicFooter v-if="!isWorkDetail && !isBare" />
    <!-- 全局悬浮 AI 助手：仅前台挂载，管理端不挂 -->
    <AiChatDock v-if="!isWorkDetail && !isBare" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PublicHeader from '@/components/layout/PublicHeader.vue'
import PublicFooter from '@/components/layout/PublicFooter.vue'
import AnnouncementBar from '@/components/layout/AnnouncementBar.vue'
import PhotographyPreviewBar from '@/components/layout/PhotographyPreviewBar.vue'
import AiChatDock from '@/components/chat/AiChatDock.vue'
const route = useRoute()
const isWorkDetail = computed(() => route.name === 'work-detail')
const isBare = computed(() => !!route.meta.bare)
</script>

<style scoped>
.public-layout {
  position: relative;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: var(--bg);
}

/* 顶部极淡的环境光晕：超大屏下首屏不再是一条空白带 */
.public-layout::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 460px;
  pointer-events: none;
  background:
    radial-gradient(120% 100% at 12% 0%, rgba(124, 139, 127, .10) 0%, rgba(124, 139, 127, 0) 62%),
    radial-gradient(90% 80% at 92% 6%, rgba(194, 168, 120, .08) 0%, rgba(194, 168, 120, 0) 60%);
}

.public-layout.is-immersive::before { display: none; }

.public-main {
  position: relative;
  z-index: 1;
  flex: 1 1 auto;
  min-height: calc(100vh - var(--header-h) - 220px);
}

/* 跳转链接：默认视觉隐藏，键盘聚焦时出现在左上角（P0-7） */
.skip-link {
  position: absolute;
  top: -48px;
  left: 12px;
  z-index: 100;
  padding: 10px 16px;
  border-radius: var(--radius);
  background: var(--accent-ink);
  color: var(--accent-contrast);
  text-decoration: none;
  font-size: 13px;
  transition: top var(--dur-fast) var(--ease);
}
.skip-link:focus-visible { top: 12px; }
</style>
