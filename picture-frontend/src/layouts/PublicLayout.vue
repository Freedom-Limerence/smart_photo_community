<template>
  <div class="public-layout" :class="{ 'is-immersive': isWorkDetail }">
    <PublicHeader v-if="!isWorkDetail" />
    <AnnouncementBar v-if="!isWorkDetail" />
    <main class="public-main">
      <router-view v-slot="{ Component, route }">
        <transition name="page" mode="out-in">
          <component :is="Component" :key="route.path" />
        </transition>
      </router-view>
    </main>
    <PublicFooter v-if="!isWorkDetail" />
    <!-- 全局悬浮 AI 助手：仅前台挂载，管理端不挂 -->
    <AiChatDock v-if="!isWorkDetail" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PublicHeader from '@/components/layout/PublicHeader.vue'
import PublicFooter from '@/components/layout/PublicFooter.vue'
import AnnouncementBar from '@/components/layout/AnnouncementBar.vue'
import AiChatDock from '@/components/chat/AiChatDock.vue'
const route = useRoute()
const isWorkDetail = computed(() => route.name === 'work-detail')
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
</style>
