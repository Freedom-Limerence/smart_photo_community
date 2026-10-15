import { createRouter, createWebHistory } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

// ---- 三套布局 ----
import PublicLayout from '@/layouts/PublicLayout.vue'
import UserLayout from '@/layouts/UserLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'

// ---- 发现与社区（公共布局）----
import DiscoverHomePage from '@/pages/discover/HomePage.vue'
import PhotoCommunityPage from '@/pages/PhotoCommunityPage.vue'
import WorkDetailPage from '@/pages/work/WorkDetailPage.vue'
import PhotographerPage from '@/pages/photographer/PhotographerPage.vue'
import SearchPage from '@/pages/search/SearchPage.vue'
import ExhibitionPage from '@/pages/ExhibitionPage.vue'
import PictureDetailPage from '@/pages/PictureDetailPage.vue'
import AccessCenterPage from '@/pages/AccessCenterPage.vue'
import SpaceDetailPage from '@/pages/SpaceDetailPage.vue'
import SpaceAnalyzePage from '@/pages/SpaceAnalyzePage.vue'
import SearchPicturePage from '@/pages/SearchPicturePage.vue'
import AddPictureBatchPage from '@/pages/AddPictureBatchPage.vue'
import UserExchangeVipPage from '@/pages/UserExchangeVipPage.vue'

// ---- AI 助手 ----
import AiCatalogPage from '@/pages/ai/AiCatalogPage.vue'
import AiSearchPage from '@/pages/ai/AiSearchPage.vue'
import AiSimilarPage from '@/pages/ai/AiSimilarPage.vue'

// ---- 个人中心（UserLayout）----
import MeOverviewPage from '@/pages/me/MeOverviewPage.vue'
import MeWorksPage from '@/pages/me/MeWorksPage.vue'
import MeFavoritesPage from '@/pages/me/MeFavoritesPage.vue'
import MeGalleryPage from '@/pages/me/MeGalleryPage.vue'
import CreateWorkPage from '@/pages/CreateWorkPage.vue'
import AddPicturePage from '@/pages/AddPicturePage.vue'
import AddSpacePage from '@/pages/AddSpacePage.vue'
import MySpacePage from '@/pages/MySpacePage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'
import UserSettingsPage from '@/pages/user/UserSettingsPage.vue'
import UserPasswordPage from '@/pages/user/UserPasswordPage.vue'

// ---- 管理后台（AdminLayout）----
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import PictureManagePage from '@/pages/admin/PictureManagePage.vue'
import SpaceManagePage from '@/pages/admin/SpaceManagePage.vue'
import SpaceUserManagePage from '@/pages/admin/SpaceUserManagePage.vue'
import AdminComingSoon from '@/pages/admin/AdminComingSoon.vue'

// ---- 账号（无布局）----
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'

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
        { path: 'search', name: 'search', component: SearchPage, meta: { title: '搜索' } },
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
        { path: 'dashboard', component: AdminComingSoon, meta: { title: '数据看板' } },
        { path: 'review', component: AdminComingSoon, meta: { title: '内容审核' } },
        { path: 'users', component: UserManagePage, meta: { title: '用户与权限' } },
        { path: 'pictures', component: PictureManagePage, meta: { title: '图片管理' } },
        { path: 'spaces', component: SpaceManagePage, meta: { title: '空间管理' } },
        { path: 'spaceUser/:id', component: SpaceUserManagePage, props: true, meta: { title: '空间成员' } },
        { path: 'logs', component: AdminComingSoon, meta: { title: '操作日志' } },
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
  if (needsAdmin && loginUserStore.loginUser.userRole !== 'admin') {
    return { path: '/' }
  }
  return true
})

export default router
