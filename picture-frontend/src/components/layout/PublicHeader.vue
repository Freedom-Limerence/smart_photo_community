<template>
  <header class="public-header">
    <div class="header-inner">
      <router-link to="/" class="brand">
        <span class="brand-mark">光</span>
        <span class="brand-text">
          <strong>光影集</strong>
          <em>PHOTOGRAPHY COMMUNITY</em>
        </span>
      </router-link>

      <nav class="nav">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item"
                     :class="{ active: isActive(item) }">
          {{ item.label }}
        </router-link>
      </nav>

      <div class="header-actions">
        <a-input-search v-model:value="keyword" class="header-search" placeholder="搜索作品 / 摄影师 / 专题"
                        allow-clear @search="doSearch" />
        <a-button type="primary" class="upload-btn" @click="go('/me/create-work')">
          <template #icon><CloudUploadOutlined /></template>
          发布作品
        </a-button>

        <a-dropdown v-if="loginUser.id" placement="bottomRight">
          <button class="user-trigger" type="button">
            <a-avatar :src="loginUser.userAvatar" :size="34">{{ avatarText }}</a-avatar>
          </button>
          <template #overlay>
            <a-menu class="user-menu">
              <div class="menu-summary">
                <a-avatar :src="loginUser.userAvatar" :size="40">{{ avatarText }}</a-avatar>
                <div>
                  <strong>{{ loginUser.userName || '摄影师' }}</strong>
                  <span>{{ loginUser.userProfile || '记录光与影' }}</span>
                </div>
              </div>
              <a-menu-divider />
              <a-menu-item key="home" @click="go('/me')"><UserOutlined />个人中心</a-menu-item>
              <a-menu-item key="following" @click="go('/me/following')"><UserOutlined />关注与粉丝</a-menu-item>
              <a-menu-item key="gallery" @click="go('/me/gallery')"><PictureOutlined />我的图库</a-menu-item>
              <a-menu-item key="settings" @click="go('/me/settings')"><SettingOutlined />账号设置</a-menu-item>
              <a-menu-item v-if="loginUser.userRole === 'admin'" key="admin" @click="go('/admin')">
                <DashboardOutlined />管理后台
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item key="logout" danger @click="doLogout"><LogoutOutlined />退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>

        <a-button v-else type="primary" ghost @click="go('/user/login')">登录 / 注册</a-button>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import {
  CloudUploadOutlined, DashboardOutlined, LogoutOutlined, PictureOutlined,
  SettingOutlined, UserOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { userLogoutUsingPost } from '@/api/userController'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const keyword = ref('')

const loginUser = computed(() => loginUserStore.loginUser || ({} as any))
const avatarText = computed(() => (loginUser.value.userName || '光').slice(0, 1))

const navItems = [
  { label: '发现', path: '/' },
  { label: '作品社区', path: '/community' },
  { label: '专题展览', path: '/exhibitions' },
]

function isActive(item: { path: string }) {
  if (item.path === '/') return route.path === '/'
  return route.path.startsWith(item.path)
}

function go(path: string) {
  router.push(path)
}

function doSearch(value: string) {
  if (!value?.trim()) return
  router.push({ path: '/search', query: { q: value.trim() } })
}

async function doLogout() {
  const res: any = await userLogoutUsingPost()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' } as any)
    message.success('已退出登录')
    router.push('/')
  } else {
    message.error(res.data.message || '退出失败')
  }
}
</script>

<style scoped>
.public-header {
  position: sticky;
  top: 0;
  z-index: 50;
  height: var(--header-h);
  background: rgba(255, 255, 255, .88);
  backdrop-filter: saturate(180%) blur(14px);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  height: var(--header-h);
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 var(--space-6);
}

.brand { display: flex; align-items: center; gap: var(--space-3); flex: 0 0 auto; }

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #3b4d3f;
  color: var(--accent-contrast);
  font-size: var(--font-4);
  font-weight: 600;
}

.brand-text { display: flex; flex-direction: column; line-height: 1.15; }
.brand-text strong { font-size: var(--font-4); letter-spacing: .02em; }
.brand-text em { color: var(--text-3); font-size: 11px; font-style: normal; }

.nav { display: flex; align-items: center; gap: var(--space-1); flex: 1 1 auto; }

.nav-item {
  position: relative;
  padding: 8px 14px;
  border-radius: var(--radius-pill);
  color: var(--text-2);
  font-size: var(--font-3);
  transition: color var(--dur-fast) var(--ease), background var(--dur-fast) var(--ease);
}

.nav-item:hover { color: var(--text-1); background: var(--surface-2); }
.nav-item.active { color: #314737; background: none; font-weight: 600; }

.header-actions { display: flex; align-items: center; gap: var(--space-3); flex: 0 0 auto; }
.header-search { width: 220px; }
.upload-btn { display: inline-flex; align-items: center; }

.user-trigger {
  display: grid;
  place-items: center;
  padding: 2px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  cursor: pointer;
  transition: box-shadow var(--dur-fast) var(--ease);
}

.user-trigger:hover { box-shadow: var(--shadow-2); }

.menu-summary { display: flex; align-items: center; gap: var(--space-3); padding: 10px 12px 6px; min-width: 200px; }
.menu-summary strong { display: block; font-size: var(--font-3); }
.menu-summary span { color: var(--text-3); font-size: var(--font-1); }

@media (max-width: 1024px) {
  .header-search { display: none; }
}

@media (max-width: 768px) {
  .public-header { height: auto; }
  .header-inner {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    grid-template-areas: 'brand actions' 'nav nav';
    gap: 0 12px;
    height: auto;
    padding: 8px var(--space-4) 0;
  }
  .brand { grid-area: brand; }
  .brand-text em { display: none; }
  .nav { grid-area: nav; justify-content: center; min-width: 0; overflow-x: auto; white-space: nowrap; }
  .nav-item { flex: 0 0 auto; padding: 10px 14px; font-size: 12px; }
  .header-actions { grid-area: actions; }
  .upload-btn { width: 36px; height: 36px; padding: 0; justify-content: center; font-size: 0; }
  .upload-btn :deep(.anticon) { font-size: 16px; }
}
</style>
