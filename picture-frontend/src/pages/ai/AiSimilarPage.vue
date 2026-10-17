<template>
  <div class="ai-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">SIMILAR SHOTS &amp; CULLING</p>
        <h1>相似分组与辅助选片</h1>
        <p>按拍摄时间窗口、尺寸、主色、标签和相机信息分组，只给建议，不会自动删除任何原图。</p>
      </div>
      <a-space>
        <a-button @click="router.push('/ai/catalog')">摄影编目</a-button>
        <a-button @click="router.push('/ai/search')">语义检索</a-button>
      </a-space>
    </section>

    <a-card size="small" title="① 选择一张基准照片">
      <a-space wrap>
        <a-input v-model:value="pictureId" placeholder="基准图片 ID" style="width: 260px" allow-clear />
        <span class="field-label">时间窗口（秒）</span>
        <a-input-number v-model:value="timeWindowSeconds" :min="10" :max="86400" :step="60" />
        <span class="field-label">相似阈值</span>
        <a-input-number v-model:value="threshold" :min="0.1" :max="0.99" :step="0.05" />
        <a-button type="primary" :loading="loading" @click="doGroup">开始分组</a-button>
      </a-space>
    </a-card>

    <a-alert v-if="note" class="mt12" type="info" show-icon :message="note"
             :description="'候选 ' + candidateCount + ' 张，阈值 ' + threshold + '，时间窗口 ' + timeWindowSeconds + ' 秒'" />

    <a-spin :spinning="loading">
      <a-card v-for="group in groups" :key="group.groupId" class="mt16" size="small" :title="group.groupId + ' · ' + group.reason">
        <a-row :gutter="16">
          <a-col v-for="pic in group.pictures" :key="pic.id" :xs="24" :sm="12" :lg="6">
            <a-card class="shot-card" size="small" :class="{ recommended: String(pic.id) === String(group.recommendPictureId) }">
              <img v-if="pic.thumbnailUrl || pic.url" :src="pic.thumbnailUrl || pic.url" class="thumb" alt="" />
              <div class="shot-body">
                <strong>{{ pic.name || ('#' + pic.id) }}</strong>
                <span class="muted">{{ pic.picWidth }}×{{ pic.picHeight }} · {{ Math.round((pic.picSize || 0) / 1024) }} KB</span>
                <span class="muted">主色 {{ pic.picColor || '未知' }}</span>
                <a-tag v-if="String(pic.id) === String(group.recommendPictureId)" color="green">建议保留</a-tag>
                <a-button type="link" size="small" @click="router.push('/ai/catalog?pictureId=' + pic.id)">元数据</a-button>
              </div>
            </a-card>
          </a-col>
        </a-row>
        <a-typography-paragraph class="mt12" type="secondary">{{ group.recommendReason }}</a-typography-paragraph>
      </a-card>
      <a-empty v-if="!loading && !groups.length" class="mt16" :description="searched ? '没有找到相似度达标的分组' : '输入基准图片 ID 开始分组'" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { similarGroups } from '@/api/aiController'

const router = useRouter()
const pictureId = ref('')
const timeWindowSeconds = ref(600)
const threshold = ref(0.6)
const loading = ref(false)
const searched = ref(false)
const groups = ref<any[]>([])
const note = ref('')
const candidateCount = ref(0)

async function doGroup() {
  if (!pictureId.value) return message.warning('请输入基准图片 ID')
  loading.value = true
  searched.value = true
  try {
    const res: any = await similarGroups({
      pictureId: pictureId.value,
      timeWindowSeconds: timeWindowSeconds.value,
      threshold: threshold.value,
    })
    if (res.data.code === 0) {
      groups.value = res.data.data.groups || []
      note.value = res.data.data.note || ''
      candidateCount.value = res.data.data.candidateCount || 0
    } else message.error(res.data.message)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.ai-page { max-width: 1180px; margin: 0 auto; }
.page-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 22px; }
.eyebrow { margin: 0 0 8px; color: #668473; font-size: 12px; letter-spacing: .16em; }
h1 { margin: 0 0 6px; font-size: 26px; }
.field-label { color: #6b7d73; font-size: 13px; }
.mt12 { margin-top: 12px; }
.mt16 { margin-top: 16px; }
.shot-card { margin-bottom: 12px; }
.shot-card.recommended { border-color: #52a37a; box-shadow: 0 0 0 2px rgba(82, 163, 122, .15); }
.thumb { width: 100%; height: 120px; object-fit: cover; border-radius: 6px; }
.shot-body { display: flex; flex-direction: column; gap: 4px; margin-top: 8px; font-size: 12px; }
.muted { color: #8a9a90; }
</style>
