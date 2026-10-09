<template>
  <div class="ai-page">
    <div class="page-heading"><div><p class="eyebrow">AI PHOTOGRAPHY WORKBENCH</p><h1>让照片先被理解，再被发现</h1><p>对已有云图库资产生成摄影元数据，人工确认后用于自然语言检索。</p></div><a-button @click="router.push('/')">返回作品流</a-button></div>
    <a-card :bordered="false" class="search-panel"><a-input-search v-model:value="query" size="large" enter-button="智能检索" placeholder="例如：夜晚的城市风景、横构图、自然光" @search="doSearch" /><div class="search-hint">当前使用关键词 + 摄影元数据混合检索，结果会过滤未公开内容。</div></a-card>
    <a-row :gutter="24" class="workspace"><a-col :xs="24" :lg="10"><a-card title="选择图片进行编目" :bordered="false"><a-input-number v-model:value="pictureId" :min="1" style="width: 100%" placeholder="输入图片 ID" /><a-button type="primary" block style="margin-top: 12px" :loading="cataloging" @click="catalog">生成摄影编目</a-button><a-alert v-if="task" style="margin-top: 16px" :message="`任务状态：${task.status}`" :description="task.resultJson || task.errorMessage" type="info" show-icon /></a-card></a-col><a-col :xs="24" :lg="14"><a-card title="检索结果" :bordered="false"><a-list :data-source="results" :locale="{ emptyText: '输入摄影语义开始检索' }"><template #renderItem="{ item }"><a-list-item><a-list-item-meta :title="`图片 #${item}" description="已通过权限过滤的摄影资产" /><a-button @click="loadMeta(item)">查看元数据</a-button></a-list-item></template></a-list></a-card></a-col></a-row>
    <a-drawer v-model:open="metaOpen" title="摄影元数据" placement="right"><a-descriptions v-if="meta" bordered :column="1"><a-descriptions-item label="方向">{{ meta.orientation }}</a-descriptions-item><a-descriptions-item label="主体">{{ meta.subjectTags }}</a-descriptions-item><a-descriptions-item label="场景">{{ meta.sceneTags }}</a-descriptions-item><a-descriptions-item label="光线">{{ meta.lightTags }}</a-descriptions-item><a-descriptions-item label="构图">{{ meta.compositionTags }}</a-descriptions-item><a-descriptions-item label="质量分">{{ meta.qualityScore }}</a-descriptions-item><a-descriptions-item label="确认状态">{{ meta.confirmed ? '人工已确认' : '待确认' }}</a-descriptions-item></a-descriptions></a-drawer>
  </div>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { catalogPicture, getAiMeta, searchAi } from '@/api/photoCommunity'
const router = useRouter(); const query = ref(''); const results = ref<number[]>([]); const pictureId = ref<number>(); const task = ref<any>(); const cataloging = ref(false); const meta = ref<any>(); const metaOpen = ref(false)
async function doSearch() { if (!query.value.trim()) return; const res: any = await searchAi(query.value); if (res.data.code === 0) results.value = res.data.data || []; else message.error(res.data.message) }
async function catalog() { if (!pictureId.value) return; cataloging.value = true; const res: any = await catalogPicture(pictureId.value); if (res.data.code === 0) { task.value = res.data.data; message.success('摄影编目已生成') } else message.error(res.data.message); cataloging.value = false }
async function loadMeta(id: number) { const res: any = await getAiMeta(id); if (res.data.code === 0) { meta.value = res.data.data; metaOpen.value = true } }
</script>
<style scoped>.ai-page{max-width:1180px;margin:0 auto}.page-heading{display:flex;justify-content:space-between;align-items:flex-start}.eyebrow{color:#5a836d;letter-spacing:2px;font-size:12px;margin:0 0 10px}.page-heading h1{color:#20322e;margin:0 0 10px}.page-heading p{color:#748078}.search-panel{margin:28px 0;border:1px solid #e5ebe7;box-shadow:none}.search-hint{color:#829087;font-size:12px;margin-top:10px}.workspace :deep(.ant-card){border:1px solid #e5ebe7;box-shadow:none;min-height:280px}</style>
