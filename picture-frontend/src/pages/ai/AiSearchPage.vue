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
        <a-col v-for="(item, i) in results" :key="item.pictureId" v-reveal:clip="(i % 3) * 80" :xs="24" :sm="12" :lg="8">
          <a-card class="result-card ai-scan" :style="{ '--scan-delay': (i * 0.12) + 's' }" size="small" hoverable>
            <img v-if="item.thumbnailUrl || item.url" :src="item.thumbnailUrl || item.url" alt="" class="thumb" />
            <div class="result-body">
              <strong>{{ item.name || ('图片 #' + item.pictureId) }}</strong>

              <!-- 匹配置信条：分值归一化到本组最高分 -->
              <div class="conf">
                <div class="conf-bar"><i :style="{ width: confPct(item) + '%' }" /></div>
                <span class="conf-val">{{ confPct(item) }}%</span>
              </div>

              <!-- 命中字段：按类型着色的徽章 -->
              <div class="hit-row">
                <span v-for="f in item.matchedFields" :key="f" class="hit-field" :class="'f-' + f">{{ fieldLabel(f) }}</span>
                <span v-if="!item.matchedFields?.length" class="muted">—</span>
              </div>

              <!-- 命中标签：可点 chips，点击即以该标签重新检索 -->
              <div class="hit-row">
                <button v-for="t in item.matchedTags" :key="t" type="button" class="hit-tag"
                        :title="'用「' + t + '」再次检索'" @click.stop="rerunWith(t)"># {{ t }}</button>
                <span v-if="!item.matchedTags?.length" class="muted">—</span>
              </div>

              <div class="access-line">
                <SafetyOutlined /> {{ item.accessReason }}
                <a-button type="link" size="small" @click="router.push('/ai/catalog?pictureId=' + item.pictureId)">查看元数据</a-button>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
      <a-empty v-else class="mt16" :description="searched ? '没有符合条件的图片（可能被权限过滤）' : '输入摄影语义开始检索'" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SafetyOutlined } from '@ant-design/icons-vue'
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

/** 匹配置信度：以本组最高分为 100% */
const maxScore = computed(() => Math.max(1, ...results.value.map((i: any) => i.score || 0)))
function confPct(item: any) {
  return Math.max(5, Math.round(((item.score || 0) / maxScore.value) * 100))
}
/** 点击命中标签：以该标签重新检索 */
function rerunWith(tag: string) {
  query.value = tag
  doSearch()
}

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
.muted { color: var(--text-3); }

/* 匹配置信条 */
.conf { display: flex; align-items: center; gap: 10px; }
.conf-bar { flex: 1; height: 6px; border-radius: 4px; background: var(--surface-2, rgba(0,0,0,.08)); overflow: hidden; }
.conf-bar i { display: block; height: 100%; border-radius: 4px; background: linear-gradient(90deg, var(--accent), var(--accent-hover)); transition: width .7s var(--ease); }
.conf-val { font-size: 12px; color: var(--accent-hover); font-variant-numeric: tabular-nums; min-width: 38px; text-align: right; }

/* 命中字段徽章（按字段类型着色） */
.hit-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.hit-field { font-size: 12px; padding: 3px 9px; border-radius: 999px; letter-spacing: .04em; }
.hit-field.f-name { background: rgba(70, 110, 235, .14); color: rgb(111, 146, 232); }
.hit-field.f-introduction { background: rgba(80, 170, 110, .14); color: rgb(102, 179, 130); }
.hit-field.f-category { background: rgba(140, 90, 230, .14); color: rgb(161, 132, 224); }
.hit-field.f-tags, .hit-field.f-subjectTags, .hit-field.f-sceneTags, .hit-field.f-lightTags, .hit-field.f-compositionTags { background: rgba(220, 140, 60, .14); color: rgb(217, 154, 85); }
.hit-field.f-camera { background: rgba(110, 120, 130, .16); color: rgb(143, 160, 173); }

/* 命中标签 chips：可点 */
.hit-tag { font-size: 12px; padding: 3px 10px; border-radius: 999px; border: 1px dashed var(--border-strong, rgba(163,182,168,.4)); background: transparent; color: var(--accent-hover); cursor: pointer; transition: background .2s, border-color .2s; }
.hit-tag:hover { background: rgba(163, 182, 168, .12); border-color: var(--accent-hover); }

.access-line { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--text-3); }

/* AI 命中扫光：结果卡揭示后一道高亮扫过（每卡错峰，一次即止） */
.ai-scan { position: relative; overflow: hidden; }
.ai-scan::after {
  content: ''; position: absolute; inset: 0; pointer-events: none;
  background: linear-gradient(105deg, transparent 42%, color-mix(in srgb, var(--accent) 22%, transparent) 50%, transparent 58%);
  transform: translateX(-120%);
  animation: ai-sweep 1.3s var(--ease) forwards; animation-delay: var(--scan-delay, 0s);
}
@keyframes ai-sweep { to { transform: translateX(120%); } }
@media (prefers-reduced-motion: reduce) { .ai-scan::after { animation: none; } }
</style>
