<template>
  <div class="password-page"><section class="page-heading"><p class="eyebrow">ACCOUNT SECURITY</p><h1>修改密码</h1><p>定期更新密码，保护你的摄影资产和社区账号。</p></section><a-card class="password-card"><a-form layout="vertical" :model="form" @finish="submit"><a-form-item label="当前密码" name="currentPassword" :rules="[{required:true,message:'请输入当前密码'}]"><a-input-password v-model:value="form.currentPassword" /></a-form-item><a-form-item label="新密码" name="newPassword" :rules="[{required:true,message:'请输入新密码'},{min:8,message:'新密码至少 8 位'}]"><a-input-password v-model:value="form.newPassword" /></a-form-item><a-form-item label="确认新密码" name="confirmPassword" :rules="[{required:true,message:'请再次输入新密码'}]"><a-input-password v-model:value="form.confirmPassword" /></a-form-item><div class="form-actions"><a-button @click="router.back()">取消</a-button><a-button type="primary" html-type="submit" :loading="saving">更新密码</a-button></div></a-form></a-card></div>
</template>
<script setup lang="ts">
import { reactive, ref } from 'vue';import { message } from 'ant-design-vue';import { useRouter } from 'vue-router';import { updateMyPasswordUsingPost } from '@/api/userController.ts'
const router=useRouter();const saving=ref(false);const form=reactive({currentPassword:'',newPassword:'',confirmPassword:''})
async function submit(){if(form.newPassword!==form.confirmPassword){message.warning('两次输入的新密码不一致');return}saving.value=true;try{const res:any=await updateMyPasswordUsingPost({...form});if(res.data.code===0){message.success('密码已更新，请重新登录');router.push('/user/login')}else message.error(res.data.message)}catch{message.error('密码修改失败，请稍后重试')}finally{saving.value=false}}
</script>
<style scoped>
.password-page{max-width:680px;margin:0 auto}.page-heading{margin:8px 0 24px}.eyebrow{margin:0 0 8px;color:#668473;font-size:11px;letter-spacing:2px;font-weight:600}.page-heading h1{margin:0 0 8px;color:#20322e;font-size:34px}.page-heading p:last-child{margin:0;color:#7b8981}.password-card{border-color:#e1e9e2}.form-actions{display:flex;justify-content:flex-end;gap:10px}
</style>
