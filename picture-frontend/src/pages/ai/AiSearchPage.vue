<template>
  <div class="ai-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">PERMISSION-AWARE SEARCH</p>
        <h1>用一句话找到你的照片</h1>
        <p>只检索你本人、可访问团队空间和审核通过的公开图片，并给出命中依据。</p>
      </div>
      <a-space>
        <a-button @click="router.push('/ai/catalog')">摄影编目</a-button>
        <a-button @click="router.push('/ai/similar')">相似选片</a-button>
      </a-space>
    </section>

    <a-card size="small">
      <a-input-search v-model:value="query" size="large" enter-button="开始检索" :loading="loading"
                      placeholder="例如：城市夜景、横构图、人物" @search="doSearch" />
      <div class="tips">
        <a-tag v-for="t in samples" :key="t" class="sample" @click="() => { query = t; doSearch() }">{{ t }}</a-tag>
      </div>
    </a-card>

    <a-alert v-if="filteredTip" class="mt12" type="info" show-icon :message="filteredTip" />

    <a-spin :spinning="loading">
      <a-row v-if="results.length" :gutter="16" class="mt16">
        <a-col v-for="item in results" :key="item.pictureId" :xs="24" :sm="12" :lg="8">
          <a-card class="result-card" size="small" hoverable>
            <img v-if="item.thumbnailUrl || item.url" :src="item.thumbnailUrl || item.url" alt="" class="thumb" />
            <div class="result-body">
              <strong>{{ item.name || ('图片 #' + item.pictureId) }}</strong>
              <div class="meta">
                <a-tag color="green">分值 {{ item.score }}</a-tag>
                <a-tag color="blue">{{ item.accessReason }}</a-tag>
              </div>
              <div class="meta">
                <span class="label">命中字段：</span>
                <a-tag v-for="f in item.matchedFields" :key="f">{{ fieldLabel(f) }}</a-tag>
                <span v-if="!item.matchedFields?.length" class="muted">—</span>
              </div>
              <div class="meta">
                <span class="label">命中标签：</span>
                <a-tag v-for="t in item.matchedTags" :key="t" color="orange">{{ t }}</a-tag>
                <span v-if="!item.matchedTags?.length" class="muted">—</span>
              </div>
              <a-button type="link" size="small" @click="router.push('/ai/catalog?pictureId=' + item.pictureId)">查看元数据</a-button>
            </div>
          </a-card>
        </a-col>
      </a-row>
      <a-empty v-else class="mt16" :description="searched ? '没有符合条件的图片（可能被权限过滤）' : '输入摄影语义开始检索'" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { searchWithEvidence } from '@/api/aiController'

const router = useRouter()
const query = ref('')
const topK = ref(20)
const loading = ref(false)
const searched = ref(false)
const results = ref<any[]>([])
const filteredTip = ref('')
const samples = ['城市夜景', '横构图', '人物', '自然光']

const fieldLabels: Record<string, string> = {
  name: '图片名称', introduction: '简介', category: '分类', tags: '标签',
  subjectTags: '主体标签', sceneTags: '场景标签', lightTags: '光线标签',
  compositionTags: '构图标签', camera: '相机信息',
}
function fieldLabel(f: string) { return fieldLabels[f] || f }

async function doSearch() {
  if (!query.value.trim()) return message.warning('请输入检索内容')
  loading.value = true
  searched.value = true
  filteredTip.value = ''
  try {
    const res: any = await searchWithEvidence({ query: query.value.trim(), topK: topK.value })
    if (res.data.code === 0) {
      results.value = res.data.data || []
      const filtered = results.value.find((i: any) => i.permissionFilteredCount)?.permissionFilteredCount
      if (filtered) filteredTip.value = '有 ' + filtered + ' 张命中图片因无权访问被过滤（权限过滤先于排序）'
    } else message.error(res.data.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-page { max-width: var(--container); margin: 0 auto; padding-inline: var(--gutter); }
.page-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 22px; }
.eyebrow { margin: 0 0 8px; color: var(--accent-hover); font-size: 12px; letter-spacing: .16em; }
h1 { margin: 0 0 6px; font-size: 26px; }
.tips { margin-top: 12px; display: flex; gap: 8px; flex-wrap: wrap; }
.sample { cursor: pointer; }
.mt12 { margin-top: 12px; }
.mt16 { margin-top: 16px; }
.result-card { margin-bottom: 16px; }
.thumb { width: 100%; height: 150px; object-fit: cover; border-radius: 6px; }
.result-body { margin-top: 10px; display: flex; flex-direction: column; gap: 6px; }
.meta { font-size: 12px; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.label { color: var(--text-3); }
.muted { color: #b6c2ba; }
</style>
