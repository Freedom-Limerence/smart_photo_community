<template>
  <div class="admin-layout">
    <aside class="admin-sider" :class="{ collapsed }">
      <div class="admin-brand">
        <span class="admin-mark">光</span>
        <div v-if="!collapsed" class="admin-brand-text">
          <strong>光影集 · 管理后台</strong>
          <em>内容治理与数据看板</em>
        </div>
      </div>
      <a-menu class="admin-menu" mode="inline" theme="dark" :items="menuItems" :selected-keys="selectedKeys"
              @click="onMenuClick" />
      <div v-if="!collapsed" class="admin-sider-foot">
        <a-button type="text" block @click="router.push('/')">返回前台</a-button>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <a-button type="text" class="collapse-btn" @click="collapsed = !collapsed">
          <MenuFoldOutlined v-if="!collapsed" />
          <MenuUnfoldOutlined v-else />
        </a-button>
        <a-breadcrumb class="admin-breadcrumb">
          <a-breadcrumb-item>管理后台</a-breadcrumb-item>
          <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="admin-header-right">
          <a-tag color="green">{{ roleLabel }}</a-tag>
          <a-avatar :src="loginUser.userAvatar" :size="32">{{ avatarText }}</a-avatar>
          <span class="admin-user-name">{{ loginUser.userName }}</span>
        </div>
      </header>
      <section class="admin-content">
        <router-view v-slot="{ Component, route }">
          <transition name="page" mode="out-in">
            <component :is="Component" :key="route.path" />
          </transition>
        </router-view>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  AuditOutlined, DashboardOutlined, FileTextOutlined, MenuFoldOutlined, MenuUnfoldOutlined,
  FundProjectionScreenOutlined, PictureOutlined, SafetyCertificateOutlined, TeamOutlined, UserOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useAdminPermissionStore } from '@/stores/useAdminPermissionStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const collapsed = ref(false)
const loginUser = computed(() => loginUserStore.loginUser || ({} as any))
const avatarText = computed(() => (loginUser.value.userName || 'A').slice(0, 1))

/** 后台角色标签：显示真实角色（管理员 / 策展人 / 已封禁） */
const roleLabel = computed(
  () =>
    ({
      admin: '管理员',
      curator: '策展人',
      banned: '已封禁',
    } as Record<string, string>)[adminPermissionStore.roleCode] || '普通用户',
)

const adminPermissionStore = useAdminPermissionStore()

/** 菜单按 RBAC 权限过滤：策展人看不到用户封禁/角色权限/系统监控 */
const menuItems = computed(() =>
  adminPermissionStore.visibleMenu.map((item) => ({ key: item.key, icon: () => h(item.icon), label: item.label })),
)

const selectedKeys = computed(() => {
  const match = menuItems.value
    .filter((i) => route.path === i.key || route.path.startsWith(i.key + '/'))
    .sort((a, b) => b.key.length - a.key.length)[0]
  return match ? [match.key] : ['/admin/dashboard']
})

const currentTitle = computed(() => menuItems.value.find((i) => i.key === selectedKeys.value[0])?.label ?? '')

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}
</script>

<style scoped>
.admin-layout { display: flex; min-height: 100vh; background: var(--bg); }

/* 侧栏：整屏吸附，自身可滚动；收起时只剩图标列 */
.admin-sider {
  display: flex;
  flex-direction: column;
  width: var(--sider-w);
  flex: 0 0 auto;
  position: sticky;
  top: 0;
  align-self: flex-start;
  height: 100vh;
  overflow-y: auto;
  background: var(--ink-2);
  transition: width var(--dur) var(--ease);
}
.admin-sider.collapsed { width: 76px; }

.admin-brand { display: flex; align-items: center; gap: var(--space-3); padding: var(--space-5) var(--space-4) var(--space-4); }
.admin-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: var(--radius);
  background: var(--accent);
  color: var(--accent-contrast);
  font-weight: 600;
}
.admin-brand-text strong { display: block; color: var(--ink-text); font-size: var(--font-3); }
.admin-brand-text em { color: var(--ink-text-dim); font-size: 11px; font-style: normal; }

.admin-menu { flex: 1 1 auto; background: transparent; border-inline-end: none !important; }
.admin-sider-foot { padding: var(--space-4); border-top: 1px solid rgba(255, 255, 255, .08); }

.admin-main { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; }

.admin-header {
  position: sticky;
  top: 0;
  z-index: var(--z-header);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  height: var(--header-h);
  padding: 0 var(--space-5);
  background: color-mix(in srgb, var(--surface) 92%, transparent);
  backdrop-filter: saturate(180%) blur(12px);
  border-bottom: 1px solid var(--border);
}

.admin-breadcrumb { flex: 1 1 auto; }
.admin-header-right { display: flex; align-items: center; gap: var(--space-3); }
.admin-user-name { color: var(--text-2); font-size: var(--font-2); }

/* 内容区：超宽屏下限制最大宽度并居中，表格不再被拉到屏幕两端 */
.admin-content { flex: 1 1 auto; padding: var(--space-6) var(--gutter) var(--space-8); }
.admin-content > :deep(*) { max-width: var(--content-max); margin-inline: auto; }

@media (max-width: 992px) {
  .admin-sider { position: static; height: auto; max-height: none; }
  .admin-content { padding: var(--space-5) var(--gutter) var(--space-6); }
  .admin-user-name { display: none; }
}
</style>
