<template>
  <div class="settings-page"><section class="page-heading"><p class="eyebrow">ACCOUNT SETTINGS</p><h1>账号设置</h1><p>管理账号安全和摄影社区使用偏好。</p></section><a-card class="settings-card"><a-list item-layout="horizontal"><a-list-item><a-list-item-meta title="登录账号" :description="loginUserStore.loginUser.userAccount || '当前登录账号'" /><a-tag>不可修改</a-tag></a-list-item><a-list-item><a-list-item-meta title="账号安全" description="更新登录密码，保护图片资产" /><a-button @click="router.push('/user/password')">修改密码</a-button></a-list-item><a-list-item><a-list-item-meta title="每日下载配额" description="查看当前配额和团队邀请码" /><a-button @click="router.push('/access-center')">查看配额</a-button></a-list-item><a-list-item><a-list-item-meta title="个人资料" description="修改昵称、头像和个人简介" /><a-button type="link" @click="router.push('/user/profile?mode=edit')">编辑资料</a-button></a-list-item></a-list></a-card></div>
</template>
<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()
</script>
<style scoped>
.settings-page{max-width:760px;margin:0 auto}.page-heading{margin:8px 0 24px}.eyebrow{margin:0 0 8px;color:#668473;font-size:11px;letter-spacing:2px;font-weight:600}.page-heading h1{margin:0 0 8px;color:#20322e;font-size:34px}.page-heading p:last-child{margin:0;color:#7b8981}.settings-card{border-color:#e1e9e2}.settings-card :deep(.ant-list-item-meta-title){color:#304c3a}
</style>
