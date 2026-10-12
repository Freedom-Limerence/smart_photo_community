<template>
  <div id="globalHeader">
    <router-link to="/" class="brand"><img class="logo" src="../assets/logo.png" alt="智能摄影云社区" /><span class="brand-copy"><strong>光影集</strong><small>智能摄影云社区</small></span></router-link>
    <a-menu v-model:selectedKeys="current" class="main-nav" mode="horizontal" :items="items" @click="doMenuClick" />
    <div class="header-actions">
      <a-button class="upload-button" type="primary" @click="router.push('/add_picture')"><template #icon><CloudUploadOutlined /></template>上传照片</a-button>
      <a-dropdown v-if="isLoggedIn" placement="bottomRight" trigger="click">
        <button class="user-trigger" type="button" aria-label="打开用户中心"><a-avatar :src="loginUserStore.loginUser.userAvatar" :size="34">{{ avatarText }}</a-avatar><span class="user-name">{{ loginUserStore.loginUser.userName || '摄影师' }}</span><DownOutlined /></button>
        <template #overlay>
          <a-menu class="user-menu">
            <div class="menu-user-summary"><a-avatar :src="loginUserStore.loginUser.userAvatar" :size="42">{{ avatarText }}</a-avatar><div><strong>{{ loginUserStore.loginUser.userName || '摄影师' }}</strong><span>{{ loginUserStore.loginUser.userProfile || '记录光影，也认识同好' }}</span></div></div>
            <a-menu-divider /><a-menu-item key="profile" @click="go('/user/profile')"><UserOutlined />个人主页</a-menu-item><a-menu-item key="edit-profile" @click="go('/user/profile?mode=edit')"><EditOutlined />编辑资料</a-menu-item><a-menu-item key="password" @click="go('/user/password')"><LockOutlined />修改密码</a-menu-item><a-menu-item key="settings" @click="go('/user/settings')"><SettingOutlined />账号设置</a-menu-item><a-menu-divider /><a-menu-item key="logout" danger @click="doLogout"><LogoutOutlined />退出登录</a-menu-item>
          </a-menu>
        </template>
      </a-dropdown>
      <a-button v-else type="primary" ghost @click="router.push('/user/login')">登录 / 注册</a-button>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { computed, h, ref } from 'vue'
import type { MenuProps } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import { AppstoreOutlined, CloudUploadOutlined, DownOutlined, EditOutlined, LockOutlined, LogoutOutlined, RobotOutlined, SettingOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { userLogoutUsingPost } from '@/api/userController.ts'
const router=useRouter();const loginUserStore=useLoginUserStore();const current=ref<string[]>([]);const isLoggedIn=computed(()=>Boolean(loginUserStore.loginUser.id));const avatarText=computed(()=> (loginUserStore.loginUser.userName||'摄').slice(0,1))
const originItems:MenuProps['items']=[{key:'/',icon:()=>h(AppstoreOutlined),label:'发现'},{key:'/community',icon:()=>h(TeamOutlined),label:'作品社区'},{key:'/exhibitions',icon:()=>h(AppstoreOutlined),label:'专题展览'},{key:'/ai-workbench',icon:()=>h(RobotOutlined),label:'AI 工具'}]
const items=computed(()=>{const visible=[...(originItems||[])];if(loginUserStore.loginUser.userRole==='admin')visible.push({key:'/admin/pictureManage',label:'管理'});return visible})
router.afterEach(to=>{current.value=[to.path==='/'?'/':`/${to.path.split('/')[1]}`]});function doMenuClick({key}:{key:string}){if(key?.startsWith('/'))router.push(key)}function go(path:string){router.push(path)}
async function doLogout(){const res=await userLogoutUsingPost();if(res.data.code===0){loginUserStore.setLoginUser({userName:'未登录'});message.success('已退出登录');await router.push('/user/login')}else message.error(`退出登录失败，${res.data.message}`)}
</script>
<style scoped>
#globalHeader{display:flex;align-items:center;height:100%;gap:28px}.brand{display:flex;align-items:center;flex:0 0 auto;color:#20322e}.logo{width:38px;height:38px;object-fit:contain}.brand-copy{display:grid;gap:1px;margin-left:10px;line-height:1.2}.brand-copy strong{font-size:17px;letter-spacing:1px}.brand-copy small{color:#8a978f;font-size:10px}.main-nav{flex:1;min-width:0;border-bottom:0;background:transparent}.main-nav :deep(.ant-menu-item){padding-inline:14px;color:#64736b}.main-nav :deep(.ant-menu-item-selected){color:#356149;font-weight:600}.header-actions{display:flex;align-items:center;gap:14px;flex:0 0 auto}.upload-button{border-radius:6px;box-shadow:none}.user-trigger{display:flex;align-items:center;gap:7px;border:0;background:transparent;color:#46584e;cursor:pointer;padding:3px}.user-name{max-width:100px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.user-menu{width:260px;padding:6px}.menu-user-summary{display:flex;align-items:center;gap:10px;padding:8px}.menu-user-summary>div{display:grid;gap:3px;min-width:0}.menu-user-summary strong,.menu-user-summary span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.menu-user-summary strong{color:#304c3a}.menu-user-summary span{color:#8a978f;font-size:12px}@media(max-width:900px){.brand-copy small,.user-name{display:none}.main-nav :deep(.ant-menu-item){padding-inline:8px}}@media(max-width:640px){#globalHeader{gap:10px}.brand-copy,.main-nav{display:none}.header-actions{margin-left:auto}.upload-button{padding-inline:10px;font-size:0}.upload-button :deep(.ant-btn-icon){margin-inline-end:0}}
</style>
