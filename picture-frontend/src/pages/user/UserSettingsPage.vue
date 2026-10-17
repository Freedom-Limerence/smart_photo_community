<template>
  <div class="settings-page"><div class="heading"><span>ACCOUNT / SETTINGS</span><h2>账号设置</h2><p>管理公开资料和账号安全。</p></div><div class="setting-list"><div class="setting-row"><div><strong>个人资料</strong><p>昵称、头像与摄影简介</p></div><router-link to="/me/profile">编辑资料 ↗</router-link></div><div class="setting-row"><div><strong>登录密码</strong><p>定期更新密码，保护你的作品与图库</p></div><router-link to="/me/password">修改密码 ↗</router-link></div><div class="setting-row"><div><strong>登录账号</strong><p>{{ store.loginUser.userAccount || '当前账号' }}</p></div><span>不可修改</span></div><div class="setting-row"><div><strong>图片下载配额</strong><p>查看剩余次数与访问记录</p></div><router-link to="/access-center">查看详情 ↗</router-link></div></div></div>
</template>
<script setup lang="ts">
import { useLoginUserStore } from '@/stores/useLoginUserStore'
const store = useLoginUserStore()
</script>
<style scoped>
.settings-page{max-width:840px;margin:auto}.heading span{color:var(--text-3);font-size:10px;letter-spacing:.18em}.heading h2{margin:7px 0 4px;font:400 30px 'Noto Serif SC','Songti SC',serif}.heading p{margin:0 0 30px;color:var(--text-3);font-size:13px}.setting-list{background:white;border:1px solid #e9ebe7;border-radius:6px;padding:8px 30px}.setting-row{display:flex;justify-content:space-between;align-items:center;gap:20px;padding:23px 0;border-bottom:1px solid #ecefeb}.setting-row:last-child{border-bottom:0}.setting-row strong{color:#344036;font-size:14px;font-weight:500}.setting-row p{margin:4px 0 0;color:#9ba49b;font-size:12px}.setting-row a{flex:0 0 auto;color:#526a56;font-size:12px}.setting-row>span{color:#a6ada5;font-size:12px}@media(max-width:600px){.setting-list{padding:5px 18px}}
</style>
