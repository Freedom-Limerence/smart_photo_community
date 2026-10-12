<template>
  <div class="profile-page">
    <section class="profile-hero"><div class="profile-avatar"><a-avatar :src="loginUserStore.loginUser.userAvatar" :size="92">{{ avatarText }}</a-avatar></div><div class="profile-heading"><p class="eyebrow">PHOTOGRAPHER PROFILE</p><h1>{{ loginUserStore.loginUser.userName || '摄影师' }}</h1><p>{{ loginUserStore.loginUser.userProfile || '还没有个人简介，写一句话介绍你的摄影方向。' }}</p><div class="profile-actions"><a-button type="primary" @click="editing = true">编辑资料</a-button><a-button @click="router.push('/community')">查看社区作品</a-button></div></div></section>
    <a-card v-if="editing" title="编辑资料" class="form-card"><a-form layout="vertical" :model="form" @finish="save"><a-form-item label="昵称" name="userName" :rules="[{required:true,message:'请输入昵称'}]" ><a-input v-model:value="form.userName" maxlength="32" show-count /></a-form-item><a-form-item label="头像地址"><a-input v-model:value="form.userAvatar" placeholder="可填写公开图片地址" /></a-form-item><a-form-item label="个人简介"><a-textarea v-model:value="form.userProfile" :rows="4" maxlength="200" show-count placeholder="例如：喜欢记录城市夜色和日常人像。" /></a-form-item><div class="form-actions"><a-button @click="cancelEdit">取消</a-button><a-button type="primary" html-type="submit" :loading="saving">保存资料</a-button></div></a-form></a-card>
    <a-card v-else title="账号资料" class="info-card"><a-descriptions :column="1"><a-descriptions-item label="账号">{{ loginUserStore.loginUser.userAccount || '当前登录账号' }}</a-descriptions-item><a-descriptions-item label="昵称">{{ loginUserStore.loginUser.userName || '摄影师' }}</a-descriptions-item><a-descriptions-item label="个人简介">{{ loginUserStore.loginUser.userProfile || '未填写' }}</a-descriptions-item></a-descriptions></a-card>
  </div>
</template>
<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { updateMyProfileUsingPost } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
const router=useRouter();const route=useRoute();const loginUserStore=useLoginUserStore();const saving=ref(false);const editing=ref(route.query.mode==='edit');const avatarText=computed(()=> (loginUserStore.loginUser.userName||'摄').slice(0,1));const form=reactive({userName:'',userAvatar:'',userProfile:''})
function sync(){form.userName=loginUserStore.loginUser.userName||'';form.userAvatar=loginUserStore.loginUser.userAvatar||'';form.userProfile=loginUserStore.loginUser.userProfile||''}sync();watch(()=>route.query.mode,value=>{editing.value=value==='edit';if(editing.value)sync()})
function cancelEdit(){editing.value=false;router.replace('/user/profile')}
async function save(){saving.value=true;try{const res:any=await updateMyProfileUsingPost({...form});if(res.data.code===0){await loginUserStore.fetchLoginUser();message.success('资料已更新');editing.value=false;router.replace('/user/profile')}else message.error(res.data.message)}catch{message.error('资料保存失败，请稍后重试')}finally{saving.value=false}}
</script>
<style scoped>
.profile-page{max-width:860px;margin:0 auto}.profile-hero{display:flex;gap:24px;align-items:center;padding:30px;background:#eaf2ec;border:1px solid #dce9df;border-radius:10px}.profile-avatar{display:grid;place-items:center;width:110px;height:110px;border-radius:50%;background:#d5e7d8}.profile-heading h1{margin:4px 0 8px;color:#20322e;font-size:34px}.profile-heading>p:not(.eyebrow){max-width:520px;margin:0;color:#718078;line-height:1.7}.eyebrow{margin:0;color:#668473;font-size:11px;letter-spacing:2px;font-weight:600}.profile-actions{display:flex;gap:10px;margin-top:18px}.form-card,.info-card{margin-top:18px;border-color:#e1e9e2}.form-actions{display:flex;justify-content:flex-end;gap:10px}@media(max-width:600px){.profile-hero{display:block;text-align:center}.profile-avatar{margin:0 auto 18px}.profile-actions{justify-content:center;flex-wrap:wrap}}
</style>
