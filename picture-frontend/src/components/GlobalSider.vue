<template>
  <a-layout-sider v-if="loginUserStore.loginUser.id" id="globalSider" width="224" breakpoint="lg" collapsed-width="0">
    <div class="sider-inner">
      <p class="sider-label">社区创作</p>
      <a-menu v-model:selectedKeys="current" mode="inline" :items="communityItems" @click="doMenuClick" />
      <p class="sider-label section-gap">个人中心</p>
      <a-menu v-model:selectedKeys="current" mode="inline" :items="centerItems" @click="doMenuClick" />
      <a-collapse v-model:activeKey="activeKeys" ghost class="asset-collapse"><a-collapse-panel key="assets" header="图库后台"><a-menu v-model:selectedKeys="current" mode="inline" :items="assetItems" @click="doMenuClick" /></a-collapse-panel></a-collapse>
      <template v-if="teamSpaceList.length"><p class="sider-label section-gap">团队空间</p><a-menu v-model:selectedKeys="current" mode="inline" :items="teamItems" @click="doMenuClick" /></template>
      <div class="sider-tip"><strong>先整理，再分享</strong><span>用 AI 整理资产，再把值得分享的作品带到社区。</span></div>
    </div>
  </a-layout-sider>
</template>
<script lang="ts" setup>
import { computed, h, onMounted, ref } from 'vue'
import { FolderOpenOutlined, PlusOutlined, SafetyOutlined, TeamOutlined, UserOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router';import { useLoginUserStore } from '@/stores/useLoginUserStore.ts';import { SPACE_TYPE_ENUM } from '@/constants/space.ts';import { listMyTeamSpaceUsingPost } from '@/api/spaceUserController.ts'
const loginUserStore=useLoginUserStore();const router=useRouter();const current=ref<string[]>([]);const activeKeys=ref<string[]>([]);const teamSpaceList=ref<API.SpaceUserVO[]>([])
const communityItems=[{key:'/community',icon:()=>h(TeamOutlined),label:'作品社区'},{key:'/exhibitions',icon:()=>h(SafetyOutlined),label:'专题展览'},{key:'/ai-workbench',icon:()=>h(SafetyOutlined),label:'AI 工作台'}];const centerItems=[{key:'/user/profile',icon:()=>h(UserOutlined),label:'个人主页'},{key:'/access-center',icon:()=>h(TeamOutlined),label:'配额与团队'}];const assetItems=[{key:'/my_space',icon:()=>h(UserOutlined),label:'我的摄影资产'},{key:'/add_picture',icon:()=>h(PlusOutlined),label:'上传照片'},{key:'/search_picture',icon:()=>h(FolderOpenOutlined),label:'图片检索'},{key:`/add_space?type=${SPACE_TYPE_ENUM.TEAM}`,icon:()=>h(TeamOutlined),label:'创建团队空间'}];const teamItems=computed(()=>teamSpaceList.value.map((spaceUser:any)=>({key:`/space/${spaceUser.spaceId}`,label:spaceUser.space?.spaceName||`团队空间 ${spaceUser.spaceId}`})))
onMounted(async()=>{if(!loginUserStore.loginUser.id)return;const res=await listMyTeamSpaceUsingPost();if(res.data.code===0&&res.data.data)teamSpaceList.value=res.data.data});router.afterEach(to=>{current.value=[to.path]});function doMenuClick({key}:{key:string}){router.push(key)}
</script>
<style scoped>
#globalSider{background:#fff}.sider-inner{min-height:calc(100vh - 122px);padding:22px 12px;display:flex;flex-direction:column}.sider-label{margin:0 12px 9px;color:#9aa59e;font-size:11px;letter-spacing:1px;text-transform:uppercase}.section-gap{margin-top:24px}.asset-collapse{margin-top:10px}.asset-collapse :deep(.ant-collapse-header){padding:8px 12px;color:#718078;font-size:13px}.asset-collapse :deep(.ant-collapse-content-box){padding:0}.asset-collapse :deep(.ant-menu){background:transparent}.sider-inner :deep(.ant-menu){border-inline-end:0;background:transparent}.sider-inner :deep(.ant-menu-item){height:40px;line-height:40px;border-radius:7px;color:#607068}.sider-inner :deep(.ant-menu-item-selected){color:#356149;background:#edf5ef;font-weight:600}.sider-tip{margin:auto 8px 0;padding:14px;display:grid;gap:5px;background:#f2f7f3;border:1px solid #dfeae1;border-radius:8px;color:#557061;font-size:12px;line-height:1.6}.sider-tip strong{color:#355443}@media(max-width:991px){.sider-inner{min-height:auto}}
</style>
