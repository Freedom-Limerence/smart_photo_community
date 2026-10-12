import {createRouter, createWebHistory} from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AddPicturePage from '@/pages/AddPicturePage.vue'
import PictureManagePage from '@/pages/admin/PictureManagePage.vue'
import PictureDetailPage from '@/pages/PictureDetailPage.vue'
import AddPictureBatchPage from '@/pages/AddPictureBatchPage.vue'
import SpaceManagePage from '@/pages/admin/SpaceManagePage.vue'
import AddSpacePage from '@/pages/AddSpacePage.vue'
import MySpacePage from '@/pages/MySpacePage.vue'
import SpaceDetailPage from '@/pages/SpaceDetailPage.vue'
import SearchPicturePage from '@/pages/SearchPicturePage.vue'
import SpaceAnalyzePage from '@/pages/SpaceAnalyzePage.vue'
import SpaceUserManagePage from '@/pages/admin/SpaceUserManagePage.vue'
import UserExchangeVipPage from '@/pages/UserExchangeVipPage.vue'
import PhotoCommunityPage from '@/pages/PhotoCommunityPage.vue'
import AiWorkbenchPage from '@/pages/AiWorkbenchPage.vue'
import AccessCenterPage from '@/pages/AccessCenterPage.vue'
import CreateWorkPage from '@/pages/CreateWorkPage.vue'
import ExhibitionPage from '@/pages/ExhibitionPage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'
import UserPasswordPage from '@/pages/user/UserPasswordPage.vue'
import UserSettingsPage from '@/pages/user/UserSettingsPage.vue'

// @author 程序员鱼皮 <a href="https://www.codefather.cn">编程导航原创项目</a>
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    { path: '/community', name: '摄影社区', component: PhotoCommunityPage },
    { path: '/ai-workbench', name: 'AI 摄影工作台', component: AiWorkbenchPage },
    { path: '/access-center', name: '访问控制中心', component: AccessCenterPage },
    { path: '/community/create', name: '创建摄影作品', component: CreateWorkPage },
    { path: '/exhibitions', name: '专题展览', component: ExhibitionPage },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    { path: '/user/profile', name: '个人主页', component: UserProfilePage },
    { path: '/user/password', name: '修改密码', component: UserPasswordPage },
    { path: '/user/settings', name: '账号设置', component: UserSettingsPage },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
    },
    {
      path: '/admin/pictureManage',
      name: '图片管理',
      component: PictureManagePage,
    },
    {
      path: '/admin/spaceManage',
      name: '空间管理',
      component: SpaceManagePage,
    },
    {
      path: '/spaceUserManage/:id',
      name: '空间成员管理',
      component: SpaceUserManagePage,
      props: true,
    },
    {
      path: '/add_picture',
      name: '创建图片',
      component: AddPicturePage,
    },
    {
      path: '/add_picture/batch',
      name: '批量创建图片',
      component: AddPictureBatchPage,
    },
    {
      path: '/picture/:id',
      name: '图片详情',
      component: PictureDetailPage,
      props: true,
    },
    {
      path: '/add_space',
      name: '创建空间',
      component: AddSpacePage,
    },
    {
      path: '/my_space',
      name: '我的空间',
      component: MySpacePage,
    },
    {
      path: '/space/:id',
      name: '空间详情',
      component: SpaceDetailPage,
      props: true,
    },
    {
      path: '/space_analyze',
      name: '空间分析',
      component: SpaceAnalyzePage,
    },
    {
      path: '/search_picture',
      name: '图片搜索',
      component: SearchPicturePage,
    },
    {
      path: '/user_exchange_vip',
      name: '用户兑换会员',
      component: UserExchangeVipPage,
    },
    {
      path: '/about',
      name: 'about',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/AboutView.vue'),
    },
  ],
})

export default router
