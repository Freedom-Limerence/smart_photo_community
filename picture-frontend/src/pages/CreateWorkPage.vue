<template>
  <div class="create-work-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">PUBLISH A PHOTO WORK</p>
        <h1>把图片整理成一组作品</h1>
        <p>作品是社区交流的基本单位。先填写标题和图片 ID，保存后即可发布到作品流。</p>
      </div>
      <a-button @click="router.push('/community')">返回作品社区</a-button>
    </section>
    <a-row :gutter="22">
      <a-col :xs="24" :lg="15">
        <a-form layout="vertical" :model="form" @finish="submit">
          <section class="form-panel">
            <a-form-item label="作品标题" name="title" :rules="[{ required: true, message: '请输入作品标题' }]">
              <a-input v-model:value="form.title" size="large" maxlength="128" show-count placeholder="例如：雨后城市的绿色时刻" />
            </a-form-item>
            <a-form-item label="作品说明" name="description">
              <a-textarea v-model:value="form.description" :rows="5" maxlength="2000" show-count placeholder="记录拍摄动机、地点或你想和其他摄影师交流的内容" />
            </a-form-item>
            <a-row :gutter="14">
              <a-col :xs="24" :sm="12"><a-form-item label="拍摄地点"><a-input v-model:value="form.shootLocation" placeholder="例如：杭州西湖" /></a-form-item></a-col>
              <a-col :xs="24" :sm="12"><a-form-item label="版权说明"><a-input v-model:value="form.copyrightNotice" placeholder="例如：© 我的名字" /></a-form-item></a-col>
            </a-row>
            <a-form-item label="关联图片 ID" name="pictureIds" :rules="[{ required: true, message: '至少填写一个图片 ID' }]">
              <a-input v-model:value="pictureIdsText" size="large" placeholder="填写图片 ID，多个 ID 用英文逗号分隔" />
              <template #extra>图片 ID 可以从图片详情地址中查看，例如 `/picture/12` 对应 ID 12。</template>
            </a-form-item>
          </section>
          <section class="form-panel options-panel">
            <div class="panel-title"><h2>发布设置</h2><span>可以先保存草稿，确认后再公开</span></div>
            <a-row :gutter="14">
              <a-col :xs="24" :sm="12">
                <a-form-item label="可见范围">
                  <a-select v-model:value="form.visibility" size="large">
                    <a-select-option value="PUBLIC">公开到作品社区</a-select-option>
                    <a-select-option value="PRIVATE">仅自己可见</a-select-option>
                    <a-select-option value="FOLLOWER_ONLY">仅关注者可见</a-select-option>
                    <a-select-option value="SPACE_ONLY">团队空间可见</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :xs="24" :sm="12">
                <a-form-item label="下载策略">
                  <a-select v-model:value="form.downloadPolicy" size="large">
                    <a-select-option value="QUOTA_DOWNLOAD">使用每日下载配额</a-select-option>
                    <a-select-option value="TEAM_ONLY">仅团队成员可下载</a-select-option>
                    <a-select-option value="OWNER_ONLY">仅作者可下载</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item v-if="form.visibility === 'SPACE_ONLY'" label="团队空间" name="spaceId" :rules="[{ required: true, message: '请选择团队空间' }]">
              <a-select v-model:value="form.spaceId" size="large" placeholder="选择作品所属团队空间" :loading="spacesLoading">
                <a-select-option v-for="space in teamSpaces" :key="space.spaceId" :value="space.spaceId">{{ space.space?.spaceName || `团队空间 ${space.spaceId}` }}</a-select-option>
              </a-select>
              <template #extra><span v-if="!spacesLoading && !teamSpaces.length">暂无可用团队空间，请先创建或加入团队空间。</span></template>
            </a-form-item>
          </section>
          <div class="submit-row"><a-button @click="saveDraft" :loading="saving">保存草稿</a-button><a-button type="primary" html-type="submit" :loading="publishing">保存并发布</a-button></div>
        </a-form>
      </a-col>
      <a-col :xs="24" :lg="9"><aside class="guide-panel"><PictureOutlined /><h2>发布前检查</h2><ul><li>图片必须属于当前登录账号。</li><li>至少关联一张已经上传的图片。</li><li>公开作品会出现在社区最新作品流。</li><li>受限作品仍然可以保存为草稿。</li></ul></aside></a-col>
    </a-row>
  </div>
</template>
<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PictureOutlined } from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { listMyTeamSpaces, publishWork, saveWork } from '@/api/photoCommunity'
const router=useRouter();const route=useRoute();const saving=ref(false);const publishing=ref(false);const spacesLoading=ref(false);const teamSpaces=ref<any[]>([]);const pictureIdsText=ref(String(route.query.pictureId||''));const form=reactive<any>({title:'',description:'',shootLocation:'',copyrightNotice:'',visibility:'PUBLIC',downloadPolicy:'QUOTA_DOWNLOAD',spaceId:undefined})
function payload(){const pictureIds=pictureIdsText.value.split(',').map(item=>Number(item.trim())).filter(item=>Number.isInteger(item)&&item>0);return {...form,pictureIds}}
async function save(publish=false){const data=payload();if(!data.pictureIds.length){message.warning('请填写至少一个有效的图片 ID');return}if(!data.title.trim()){message.warning('请填写作品标题');return}if(data.visibility==='SPACE_ONLY'&&!data.spaceId){message.warning('团队可见作品必须选择团队空间');return}publish?publishing.value=true:saving.value=true;try{const res:any=await saveWork(data);if(res.data.code!==0){message.error(res.data.message);return}const workId=Number(res.data.data);if(publish){const publishRes:any=await publishWork({workId,publish:true});if(publishRes.data.code!==0){message.error('作品已保存，但发布失败：'+publishRes.data.message);return}message.success('作品已发布');router.push('/community')}else{message.success('作品草稿已保存')}}catch{message.error('保存作品失败，请检查登录状态和图片 ID')}finally{saving.value=false;publishing.value=false}}
function saveDraft(){save(false)}function submit(){save(true)}
onMounted(async()=>{if(route.query.title)form.title=String(route.query.title);spacesLoading.value=true;try{const res:any=await listMyTeamSpaces();if(res.data.code===0)teamSpaces.value=(res.data.data||[]).filter((item:any)=>item.space?.spaceType===1)}finally{spacesLoading.value=false}})
</script>
<style scoped>
.create-work-page{max-width:1120px;margin:0 auto}.page-heading{display:flex;justify-content:space-between;gap:20px;align-items:start;margin-bottom:28px}.eyebrow{margin:0 0 10px;color:#668473;font-size:11px;font-weight:600;letter-spacing:2px}.page-heading h1{margin:0 0 8px;color:#20322e;font-size:36px}.page-heading p:not(.eyebrow){margin:0;color:#748078;line-height:1.7}.form-panel,.guide-panel{padding:24px;background:#fff;border:1px solid #e1e9e2;border-radius:8px;margin-bottom:16px}.form-panel :deep(.ant-form-item:last-child){margin-bottom:0}.options-panel{padding-bottom:8px}.panel-title{display:flex;gap:12px;align-items:baseline;margin-bottom:18px}.panel-title h2{margin:0;color:#304c3a;font-size:18px}.panel-title span{color:#8a978f;font-size:12px}.submit-row{display:flex;justify-content:flex-end;gap:12px;margin-top:20px}.guide-panel{background:#edf5ef;color:#557061}.guide-panel>svg{font-size:28px;color:#6d9b79}.guide-panel h2{margin:18px 0 10px;color:#355443;font-size:18px}.guide-panel ul{padding-left:18px;margin:0;color:#748078;font-size:13px;line-height:2}@media(max-width:650px){.page-heading{display:block}.page-heading .ant-btn{margin-top:14px}.submit-row{justify-content:stretch}.submit-row .ant-btn{flex:1}}
</style>
