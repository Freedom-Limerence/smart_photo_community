<template>
  <div class="admin-layout">
    <!-- 桌面侧栏：≥1280 展开 / 1024–1279 折叠为图标栏（4.3 响应式三态） -->
    <aside v-if="!isMobile" class="admin-sider" :class="{ collapsed }">
      <div class="admin-brand">
        <span class="admin-mark">光</span>
        <div v-if="!collapsed" class="admin-brand-text">
          <strong>光影集 · 管理后台</strong>
          <em>内容治理与数据看板</em>
        </div>
      </div>
      <a-menu class="admin-menu" mode="inline" theme="dark" :items="groupedMenuItems" :selected-keys="selectedKeys"
              @click="onMenuClick" />
      <div v-if="!collapsed" class="admin-sider-foot">
        <a-button type="text" block @click="router.push('/')">返回前台</a-button>
      </div>
    </aside>

    <!-- 移动端抽屉：<1024px 侧栏改为抽屉，顶栏汉堡唤起 -->
    <a-drawer v-model:open="drawerOpen" placement="left" :width="248" class="admin-sider-drawer" :closable="false"
              :body-style="{ padding: 0, background: 'var(--ink-2)' }" :header-style="{ display: 'none' }">
      <div class="admin-brand">
        <span class="admin-mark">光</span>
        <div class="admin-brand-text">
          <strong>光影集 · 管理后台</strong>
          <em>内容治理与数据看板</em>
        </div>
      </div>
      <a-menu class="admin-menu" mode="inline" theme="dark" :items="groupedMenuItems" :selected-keys="selectedKeys"
              @click="onMenuClick" />
      <div class="admin-sider-foot">
        <a-button type="text" block @click="router.push('/')">返回前台</a-button>
      </div>
    </a-drawer>

    <div class="admin-main">
      <header class="admin-header">
        <a-button type="text" class="collapse-btn" :aria-label="isMobile ? '打开菜单' : (collapsed ? '展开侧栏' : '收起侧栏')"
                  @click="onToggle">
          <MenuUnfoldOutlined v-if="isMobile || collapsed" />
          <MenuFoldOutlined v-else />
        </a-button>
        <a-breadcrumb class="admin-breadcrumb">
          <a-breadcrumb-item>管理后台</a-breadcrumb-item>
          <a-breadcrumb-item>{{ currentTitle }}</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="admin-header-right">
          <span class="role-tag" :data-role="adminPermissionStore.roleCode">{{ roleLabel }}</span>
          <a-avatar :src="loginUser.userAvatar" :size="32">{{ avatarText }}</a-avatar>
          <span class="admin-user-name">{{ loginUser.userName }}</span>
        </div>
      </header>
      <section class="admin-content">
        <!-- 过渡只挂一次（4.1 #8）：页面级过渡统一由 App.vue 承担 -->
        <router-view />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  MenuFoldOutlined, MenuUnfoldOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useAdminPermissionStore } from '@/stores/useAdminPermissionStore'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()
const adminPermissionStore = useAdminPermissionStore()

/** 响应式三态（4.3 #3）：≥1280 展开；1024–1279 默认折叠图标栏；<1024 抽屉 */
const isMobile = ref(false)
const collapsed = ref(false)
const drawerOpen = ref(false)

function measure() {
  const w = window.innerWidth
  isMobile.value = w < 1024
  collapsed.value = !isMobile.value && w < 1280
}
onMounted(() => {
  measure()
  window.addEventListener('resize', measure)
})
onBeforeUnmount(() => window.removeEventListener('resize', measure))

function onToggle() {
  if (isMobile.value) drawerOpen.value = true
  else collapsed.value = !collapsed.value
}

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

const GROUP_LABELS: Record<string, string> = {
  overview: '概览',
  content: '内容治理',
  userspace: '用户与空间',
  system: '系统',
}

/** 菜单按 RBAC 权限过滤 + 按 group 分组渲染（antd itemGroup，组标题走 --fs-eyebrow） */
const groupedMenuItems = computed(() => {
  const order = ['overview', 'content', 'userspace', 'system']
  const out: any[] = []
  for (const g of order) {
    const items = adminPermissionStore.visibleMenu.filter((item: any) => item.group === g)
    if (!items.length) continue
    out.push({
      type: 'group',
      label: GROUP_LABELS[g],
      children: items.map((item: any) => ({ key: item.key, icon: () => h(item.icon), label: item.label })),
    })
  }
  return out
})

const selectedKeys = computed(() => {
  const flat = groupedMenuItems.value.flatMap((g: any) => g.children || [])
  const match = flat
    .filter((i: any) => route.path === i.key || route.path.startsWith(i.key + '/'))
    .sort((a: any, b: any) => b.key.length - a.key.length)[0]
  return match ? [match.key] : ['/admin/dashboard']
})

const currentTitle = computed(() => {
  const flat = groupedMenuItems.value.flatMap((g: any) => g.children || [])
  return flat.find((i: any) => i.key === selectedKeys.value[0])?.label ?? ''
})

function onMenuClick({ key }: { key: string }) {
  drawerOpen.value = false
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
.admin-brand-text em { color: var(--ink-text-dim); font-size: 12px; font-style: normal; }

.admin-menu { flex: 1 1 auto; background: transparent; border-inline-end: none !important; }
/* 折叠图标栏：组标题竖排会挤成怪块，隐藏之 */
.admin-sider.collapsed :deep(.ant-menu-item-group-title) { display: none; }
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

/* 角色标签：antd 预设绿 → 令牌化（4.3：低饱和块 + 深绿字） */
.role-tag {
  padding: 1px 10px;
  border-radius: var(--radius-pill);
  font-size: var(--font-1);
  background: var(--accent-soft);
  color: var(--accent-ink);
}
.role-tag[data-role='banned'] { background: color-mix(in srgb, var(--danger) 12%, transparent); color: var(--danger); }

/* 内容区：超宽屏下限制最大宽度并居中，表格不再被拉到屏幕两端 */
.admin-content { flex: 1 1 auto; padding: var(--space-6) var(--gutter) var(--space-8); }
.admin-content > :deep(*) { max-width: var(--content-max); margin-inline: auto; }

@media (max-width: 1023px) {
  .admin-sider { display: none; }
  .admin-content { padding: var(--space-5) var(--gutter) var(--space-6); }
  .admin-user-name { display: none; }
}
</style>
