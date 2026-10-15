import { createRouter, createWebHistory } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { message } from 'ant-design-vue'
import { findMenuByPath } from '@/config/adminMenu'
import { useAdminPermissionStore } from '@/stores/useAdminPermissionStore'

// ---- 三套布局 ----
import PublicLayout from '@/layouts/PublicLayout.vue'
const UserLayout = () => import('@/layouts/UserLayout.vue')
const AdminLayout = () => import('@/layouts/AdminLayout.vue')

// ---- 发现与社区（公共布局）----
import DiscoverHomePage from '@/pages/discover/HomePage.vue'
const PhotoCommunityPage = () => import('@/pages/PhotoCommunityPage.vue')
const WorkDetailPage = () => import('@/pages/work/WorkDetailPage.vue')
const PhotographerPage = () => import('@/pages/photographer/PhotographerPage.vue')
const FollowListPage = () => import('@/pages/photographer/FollowListPage.vue')
const SearchPage = () => import('@/pages/search/SearchPage.vue')
const TagPage = () => import('@/pages/tags/TagPage.vue')
const TagIndexPage = () => import('@/pages/tags/TagIndexPage.vue')
const ExhibitionPage = () => import('@/pages/ExhibitionPage.vue')
const PictureDetailPage = () => import('@/pages/PictureDetailPage.vue')
const AccessCenterPage = () => import('@/pages/AccessCenterPage.vue')
const SpaceDetailPage = () => import('@/pages/SpaceDetailPage.vue')
const SpaceAnalyzePage = () => import('@/pages/SpaceAnalyzePage.vue')
const SearchPicturePage = () => import('@/pages/SearchPicturePage.vue')
const AddPictureBatchPage = () => import('@/pages/AddPictureBatchPage.vue')
const UserExchangeVipPage = () => import('@/pages/UserExchangeVipPage.vue')

// ---- AI 助手 ----
const AiCatalogPage = () => import('@/pages/ai/AiCatalogPage.vue')
const AiSearchPage = () => import('@/pages/ai/AiSearchPage.vue')
const AiSimilarPage = () => import('@/pages/ai/AiSimilarPage.vue')

// ---- 个人中心（UserLayout）----
const MeOverviewPage = () => import('@/pages/me/MeOverviewPage.vue')
const MeWorksPage = () => import('@/pages/me/MeWorksPage.vue')
const MeFavoritesPage = () => import('@/pages/me/MeFavoritesPage.vue')
const MeGalleryPage = () => import('@/pages/me/MeGalleryPage.vue')
const CreateWorkPage = () => import('@/pages/CreateWorkPage.vue')
const AddPicturePage = () => import('@/pages/AddPicturePage.vue')
const AddSpacePage = () => import('@/pages/AddSpacePage.vue')
const MySpacePage = () => import('@/pages/MySpacePage.vue')
const UserProfilePage = () => import('@/pages/user/UserProfilePage.vue')
const UserSettingsPage = () => import('@/pages/user/UserSettingsPage.vue')
const UserPasswordPage = () => import('@/pages/user/UserPasswordPage.vue')

// ---- 管理后台（AdminLayout）----
const UserManagePage = () => import('@/pages/admin/UserManagePage.vue')
const PictureManagePage = () => import('@/pages/admin/PictureManagePage.vue')
const SpaceManagePage = () => import('@/pages/admin/SpaceManagePage.vue')
const SpaceUserManagePage = () => import('@/pages/admin/SpaceUserManagePage.vue')
const AdminDashboardPage = () => import('@/pages/admin/AdminDashboardPage.vue')
const AdminReviewPage = () => import('@/pages/admin/AdminReviewPage.vue')
const AdminLogsPage = () => import('@/pages/admin/AdminLogsPage.vue')
const AdminRbacPage = () => import('@/pages/admin/AdminRbacPage.vue')
const AdminMonitorPage = () => import('@/pages/admin/AdminMonitorPage.vue')

// ---- 账号（无布局）----
const UserLoginPage = () => import('@/pages/user/UserLoginPage.vue')
const UserRegisterPage = () => import('@/pages/user/UserRegisterPage.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    /* ================= 前台（公共布局）================= */
    {
      path: '/',
      component: PublicLayout,
      children: [
        { path: '', name: 'discover', component: DiscoverHomePage, meta: { title: '发现' } },
        { path: 'community', name: 'community', component: PhotoCommunityPage, meta: { title: '作品社区' } },
        { path: 'works/:id', name: 'work-detail', component: WorkDetailPage, props: true, meta: { title: '作品详情' } },
        { path: 'photographers/:id', name: 'photographer', component: PhotographerPage, props: true, meta: { title: '摄影师主页' } },
        { path: 'photographers/:id/followers', component: FollowListPage, meta: { title: '粉丝' } },
        { path: 'photographers/:id/following', component: FollowListPage, meta: { title: '关注' } },
        { path: 'search', name: 'search', component: SearchPage, meta: { title: '搜索' } },
        { path: 'tags', name: 'tag-index', component: TagIndexPage, meta: { title: '标签广场' } },
        { path: 'tags/:tag', name: 'tag', component: TagPage, props: true, meta: { title: '标签' } },
        { path: 'exhibitions', name: 'exhibitions', component: ExhibitionPage, meta: { title: '专题展览' } },
        { path: 'picture/:id', name: 'picture-detail', component: PictureDetailPage, props: true, meta: { title: '图片详情' } },
        { path: 'access-center', name: 'access-center', component: AccessCenterPage, meta: { title: '访问控制中心' } },
        { path: 'ai/catalog', name: 'ai-catalog', component: AiCatalogPage, meta: { title: 'AI 摄影编目' } },
        { path: 'ai/search', name: 'ai-search', component: AiSearchPage, meta: { title: 'AI 语义检索' } },
        { path: 'ai/similar', name: 'ai-similar', component: AiSimilarPage, meta: { title: 'AI 相似选片' } },
        { path: 'ai-workbench', redirect: '/ai/catalog' },
        { path: 'space/:id', name: 'space-detail', component: SpaceDetailPage, props: true, meta: { title: '空间详情' } },
        { path: 'space_analyze', name: 'space-analyze', component: SpaceAnalyzePage, meta: { title: '空间分析' } },
        { path: 'search_picture', name: 'search-picture', component: SearchPicturePage, meta: { title: '以图搜图' } },
        { path: 'add_picture/batch', name: 'add-picture-batch', component: AddPictureBatchPage, meta: { title: '批量上传', requiresAuth: true } },
        { path: 'user_exchange_vip', name: 'exchange-vip', component: UserExchangeVipPage, meta: { title: '兑换会员' } },
      ],
    },

    /* ================= 个人中心（左侧个人菜单）================= */
    {
      path: '/me',
      component: UserLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'me-overview', component: MeOverviewPage, meta: { title: '我的主页' } },
        { path: 'works', name: 'me-works', component: MeWorksPage, meta: { title: '我的作品' } },
        { path: 'favorites', name: 'me-favorites', component: MeFavoritesPage, meta: { title: '我的收藏' } },
        { path: 'followers', component: FollowListPage, meta: { title: '我的粉丝' } },
        { path: 'following', component: FollowListPage, meta: { title: '我的关注' } },
        { path: 'gallery', name: 'me-gallery', component: MeGalleryPage, meta: { title: '我的图库' } },
        { path: 'upload', name: 'me-upload', component: AddPicturePage, meta: { title: '上传图片' } },
        { path: 'create-work', name: 'me-create-work', component: CreateWorkPage, meta: { title: '发布作品' } },
        { path: 'spaces', name: 'me-spaces', component: MySpacePage, meta: { title: '我的空间' } },
        { path: 'add-space', name: 'me-add-space', component: AddSpacePage, meta: { title: '创建空间' } },
        { path: 'profile', name: 'me-profile', component: UserProfilePage, meta: { title: '个人资料' } },
        { path: 'settings', name: 'me-settings', component: UserSettingsPage, meta: { title: '账号设置' } },
        { path: 'password', name: 'me-password', component: UserPasswordPage, meta: { title: '修改密码' } },
      ],
    },

    /* ================= 管理后台（独立外壳）================= */
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        { path: '', redirect: '/admin/dashboard' },
        { path: 'dashboard', component: AdminDashboardPage, meta: { title: '数据看板' } },
        { path: 'review', component: AdminReviewPage, meta: { title: '内容审核' } },
        { path: 'users', component: UserManagePage, meta: { title: '用户与权限' } },
        { path: 'pictures', component: PictureManagePage, meta: { title: '图片管理' } },
        { path: 'spaces', component: SpaceManagePage, meta: { title: '空间管理' } },
        { path: 'spaceUser/:id', component: SpaceUserManagePage, props: true, meta: { title: '空间成员' } },
        { path: 'monitor', component: AdminMonitorPage, meta: { title: '系统监控' } },
        { path: 'rbac', component: AdminRbacPage, meta: { title: '角色与权限' } },
        { path: 'logs', component: AdminLogsPage, meta: { title: '操作日志' } },
      ],
    },

    /* ================= 账号页（无布局）================= */
    { path: '/user/login', name: 'user-login', component: UserLoginPage, meta: { title: '登录' } },
    { path: '/user/register', name: 'user-register', component: UserRegisterPage, meta: { title: '注册' } },

    /* ================= 旧路径兼容跳转 ================= */
    { path: '/community/create', redirect: '/me/create-work' },
    { path: '/add_picture', redirect: '/me/upload' },
    { path: '/add_space', redirect: '/me/add-space' },
    { path: '/my_space', redirect: '/me/spaces' },
    { path: '/user/profile', redirect: '/me/profile' },
    { path: '/user/settings', redirect: '/me/settings' },
    { path: '/user/password', redirect: '/me/password' },
    { path: '/admin/userManage', redirect: '/admin/users' },
    { path: '/admin/pictureManage', redirect: '/admin/pictures' },
    { path: '/admin/spaceManage', redirect: '/admin/spaces' },
    { path: '/spaceUserManage/:id', redirect: (to) => '/admin/spaceUser/' + to.params.id },
    { path: '/about', redirect: '/' },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  const needsAuth = to.matched.some((r) => r.meta?.requiresAuth)
  const needsAdmin = to.matched.some((r) => r.meta?.requiresAdmin)
  if (!needsAuth && !needsAdmin) return true

  const loginUserStore = useLoginUserStore()
  if (!loginUserStore.loginUser?.id) {
    try {
      await loginUserStore.fetchLoginUser()
    } catch {
      // 未登录
    }
  }
  if (!loginUserStore.loginUser?.id) {
    return { path: '/user/login', query: { redirect: to.fullPath } }
  }

  // 后台：按 RBAC 权限点判断（admin / curator 等任意拥有后台权限的角色都可进入）
  if (needsAdmin) {
    const adminPermissionStore = useAdminPermissionStore()
    await adminPermissionStore.load()
    if (!adminPermissionStore.canEnterAdmin) {
      message.error('当前账号没有后台权限')
      return { path: '/' }
    }
    const required = findMenuByPath(to.path)?.permission
    if (required && !adminPermissionStore.has(required)) {
      message.error('缺少权限：' + required)
      return { path: adminPermissionStore.firstAccessiblePath() }
    }
  }
  return true
})

export default router
