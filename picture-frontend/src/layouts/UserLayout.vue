<template>
  <div class="user-layout">
    <PublicHeader />
    <div class="user-body">
      <aside class="user-sider">
        <div class="sider-card surface-card">
          <div class="sider-profile">
            <a-avatar :src="loginUser.userAvatar" :size="56">{{ avatarText }}</a-avatar>
            <strong>{{ loginUser.userName || '摄影师' }}</strong>
            <span class="muted">{{ loginUser.userProfile || '还没有填写简介' }}</span>
          </div>
          <a-menu class="sider-menu" mode="inline" :items="menuItems" :selected-keys="selectedKeys"
                  @click="onMenuClick" />
        </div>
      </aside>
      <section class="user-content">
        <router-view v-slot="{ Component, route }">
          <transition name="page" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </section>
    </div>
    <PublicFooter />
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AppstoreOutlined, FolderOpenOutlined, PictureOutlined, SettingOutlined, StarOutlined, UserOutlined,
} from '@ant-design/icons-vue'
import PublicHeader from '@/components/layout/PublicHeader.vue'
import PublicFooter from '@/components/layout/PublicFooter.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser || ({} as any))
const avatarText = computed(() => (loginUser.value.userName || '光').slice(0, 1))

const menuItems = [
  { key: '/me', icon: () => h(AppstoreOutlined), label: '我的主页' },
  { key: '/me/works', icon: () => h(PictureOutlined), label: '我的作品' },
  { key: '/me/favorites', icon: () => h(StarOutlined), label: '我的收藏' },
  { key: '/me/gallery', icon: () => h(FolderOpenOutlined), label: '我的图库' },
  { key: '/me/upload', icon: () => h(UserOutlined), label: '上传图片' },
  { key: '/me/settings', icon: () => h(SettingOutlined), label: '账号设置' },
]

const selectedKeys = computed(() => {
  const match = menuItems
    .filter((i) => route.path === i.key || route.path.startsWith(i.key + '/'))
    .sort((a, b) => b.key.length - a.key.length)[0]
  return match ? [match.key] : ['/me']
})

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}
</script>

<style scoped>
.user-layout { display: flex; flex-direction: column; min-height: 100vh; background: var(--bg); }

.user-body {
  display: flex;
  gap: var(--space-6);
  width: 100%;
  max-width: var(--container);
  margin: 0 auto;
  padding: var(--space-6);
}

.user-sider { flex: 0 0 var(--sider-w); }
.sider-card { position: sticky; top: calc(var(--header-h) + var(--space-5)); padding: var(--space-5) var(--space-3); }
.sider-profile { display: flex; flex-direction: column; align-items: center; gap: var(--space-2); padding: 0 var(--space-3) var(--space-4); text-align: center; }
.sider-profile strong { font-size: var(--font-4); }
.sider-profile span { font-size: var(--font-1); }
.sider-menu { border-inline-end: none !important; background: transparent; }

.user-content { flex: 1 1 auto; min-width: 0; }

@media (max-width: 900px) {
  .user-body { flex-direction: column; padding: var(--space-4); }
  .user-sider { flex: none; }
  .sider-card { position: static; }
}
</style>
