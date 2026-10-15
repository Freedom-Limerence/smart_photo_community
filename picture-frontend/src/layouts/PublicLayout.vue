<template>
  <div class="public-layout">
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
.public-layout { display: flex; flex-direction: column; min-height: 100vh; background: var(--bg); }
.public-main { flex: 1 1 auto; min-height: calc(100vh - var(--header-h) - 200px); }
</style>
