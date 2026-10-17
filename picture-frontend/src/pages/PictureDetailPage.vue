<template>
  <div id="pictureDetailPage" class="container">
    <a-row :gutter="[16, 16]">
      <!-- 图片预览 + 工具条 -->
      <a-col :sm="24" :md="15" :xl="17">
        <a-card :bordered="false" class="panel">
          <template #title>
            <span class="card-title">{{ picture.name || '图片预览' }}</span>
          </template>
          <template #extra>
            <a-space :size="4" wrap>
              <a-button size="small" :loading="downloading" @click="download"><DownloadOutlined /> 下载原图</a-button>
              <a-button size="small" @click="findSimilar"><PictureOutlined /> 找相似</a-button>
              <a-button size="small" @click="share"><ShareAltOutlined /> 分享</a-button>
              <a-button v-if="isOwner" size="small" @click="openEdit"><EditOutlined /> 编辑</a-button>
              <a-button v-if="isOwner" size="small" danger @click="removePicture"><DeleteOutlined /> 删除</a-button>
              <a-button v-if="!isOwner" size="small" @click="reportOpen = true"><WarningOutlined /> 举报</a-button>
            </a-space>
          </template>

          <div class="preview-wrap">
            <a-image :src="picture.url" :fallback="picture.thumbnailUrl" class="preview-img" />
          </div>

          <!-- 参数面板（可展开/收起） -->
          <div class="params">
            <div class="params-head" @click="showParams = !showParams">
              <span class="eyebrow">IMAGE NOTES / 图片参数</span>
              <a-button type="text" size="small">
                {{ showParams ? '收起' : '展开' }} <DownOutlined v-if="!showParams" /><UpOutlined v-else />
              </a-button>
            </div>
            <a-descriptions v-show="showParams" :column="2" size="small" bordered class="params-body">
              <a-descriptions-item label="格式">{{ picture.picFormat ? String(picture.picFormat).toUpperCase() : '-' }}</a-descriptions-item>
              <a-descriptions-item label="尺寸">{{ sizeText }}</a-descriptions-item>
              <a-descriptions-item label="宽高比">{{ picture.picScale ?? '-' }}</a-descriptions-item>
              <a-descriptions-item label="主色">
                <span v-if="picture.picColor" class="color-dot" :style="{ background: picture.picColor }" />{{ picture.picColor ?? '-' }}
              </a-descriptions-item>
              <a-descriptions-item label="体积">{{ picture.picSize ? Math.round(Number(picture.picSize) / 1024) + ' KB' : '-' }}</a-descriptions-item>
              <a-descriptions-item label="上传时间">{{ fmt(picture.createTime) }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.shootTime" label="拍摄时间">{{ fmt(picture.shootTime) }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.shootLocation" label="拍摄地点">{{ picture.shootLocation }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.camera" label="相机">{{ picture.camera }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.lens" label="镜头">{{ picture.lens }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.exposureTime" label="快门">{{ picture.exposureTime }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.aperture" label="光圈">{{ picture.aperture }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.iso" label="ISO">{{ picture.iso }}</a-descriptions-item>
              <a-descriptions-item v-if="picture.focalLength" label="焦距">{{ picture.focalLength }}</a-descriptions-item>
            </a-descriptions>
          </div>
        </a-card>
      </a-col>

      <!-- 信息区 -->
      <a-col :sm="24" :md="9" :xl="7">
        <a-alert v-if="isOwner && reviewStatus === 0" type="warning" show-icon class="review-alert"
                 message="这张图片正在等待审核"
                 description="审核通过后才会出现在公共图库、被其他人看到。在此之前只有你自己能在这里看到它。" />
        <a-alert v-else-if="isOwner && reviewStatus === 2" type="error" show-icon class="review-alert"
                 message="这张图片未通过审核">
          <template #description>
            <div>{{ picture.reviewMessage || '可以修改分类与标签后重新提交审核。' }}</div>
            <a-button type="link" size="small" @click="appealOpen = true">认为审核有误？提交申诉 →</a-button>
          </template>
        </a-alert>

        <a-card :bordered="false" class="panel">
          <template #title><span class="card-title">作者</span></template>
          <div class="author-row">
            <router-link :to="'/photographers/' + picture.userId" class="author-main">
              <a-avatar :size="40" :src="picture.user?.userAvatar">{{ (picture.user?.userName || '光').slice(0, 1) }}</a-avatar>
              <div>
                <strong>{{ picture.user?.userName || '未知作者' }}</strong>
                <span class="muted">{{ picture.category || '未分类' }}</span>
              </div>
            </router-link>
            <a-button v-if="!isOwner" size="small" type="primary" @click="router.push('/photographers/' + picture.userId)">
              进入主页
            </a-button>
          </div>
        </a-card>

        <a-card :bordered="false" class="panel">
          <template #title><span class="card-title">图片信息</span></template>
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="审核状态"><a-tag :color="reviewColor">{{ reviewText }}</a-tag></a-descriptions-item>
            <a-descriptions-item label="名称">{{ picture.name ?? '未命名' }}</a-descriptions-item>
            <a-descriptions-item label="简介">{{ picture.introduction || '-' }}</a-descriptions-item>
            <a-descriptions-item label="分类">{{ picture.category ?? '默认' }}</a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture.tags || []" :key="tag">{{ tag }}</a-tag>
              <span v-if="!(picture.tags || []).length" class="muted">-</span>
            </a-descriptions-item>
          </a-descriptions>

          <div class="info-actions">
            <a-button size="small" @click="share">复制链接</a-button>
            <a-button size="small" @click="copyUrl">复制图片地址</a-button>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 互跳：这张图被哪些作品使用过 -->
    <a-card v-if="relatedWorks.length" :bordered="false" class="panel">
      <template #title><span class="card-title">被以下作品使用（{{ relatedWorks.length }}）</span></template>
      <div class="related-works">
        <WorkCard v-for="item in relatedWorks" :key="item.id" :work="item" ratio="66%" @open="router.push('/works/' + item.id)" />
      </div>
    </a-card>

    <!-- 编辑弹窗 -->
    <a-modal v-model:open="editOpen" title="编辑图片信息" :confirm-loading="saving" ok-text="保存" @ok="saveEdit">
      <a-form layout="vertical">
        <a-form-item label="名称"><a-input v-model:value="form.name" placeholder="给这张图片起个名字" /></a-form-item>
        <a-form-item label="简介"><a-textarea v-model:value="form.introduction" :rows="3" placeholder="拍摄思路、地点、故事" /></a-form-item>
        <a-form-item label="分类"><a-input v-model:value="form.category" placeholder="例如：风光 / 人像 / 街拍" /></a-form-item>
        <a-form-item label="标签">
          <a-select v-model:value="form.tags" mode="tags" style="width: 100%" placeholder="回车添加标签" />
        </a-form-item>
      </a-form>
      <p class="muted">修改后审核状态会重置为「待审核」，需要重新过审。</p>
    </a-modal>

    <!-- 申诉弹窗 -->
    <a-modal v-model:open="appealOpen" title="提交申诉" @ok="submitAppealAction">
      <p class="muted">请说明这张图片应当通过审核的理由（例如原创证明、分辨率说明、内容无违规）。</p>
      <a-textarea v-model:value="appealReason" :rows="4" placeholder="申诉理由" />
    </a-modal>

    <!-- 举报弹窗 -->
    <a-modal v-model:open="reportOpen" title="举报该图片" @ok="doReport">
      <a-select v-model:value="reportReason" style="width: 100%; margin-bottom: 12px">
        <a-select-option value="内容违规">内容违规</a-select-option>
        <a-select-option value="侵权盗用">侵权盗用</a-select-option>
        <a-select-option value="色情低俗">色情低俗</a-select-option>
        <a-select-option value="广告营销">广告营销</a-select-option>
        <a-select-option value="其他">其他</a-select-option>
      </a-select>
      <a-textarea v-model:value="reportDetail" :rows="3" placeholder="补充说明（可选）" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  DeleteOutlined, DownOutlined, DownloadOutlined, EditOutlined, PictureOutlined,
  ShareAltOutlined, UpOutlined, WarningOutlined,
} from '@ant-design/icons-vue'
import { deletePictureUsingPost, editPictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController'
import { authorizeDownload } from '@/api/photoCommunity'
import { submitAppeal, submitReport } from '@/api/notification'

import WorkCard from '@/components/common/WorkCard.vue'
import { listWorksByPicture } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUser = useLoginUserStore()

const picture = ref<any>({})
const relatedWorks = ref<any[]>([])

async function loadRelatedWorks() {
  if (!pictureId.value) return
  const res: any = await listWorksByPicture(pictureId.value)
  relatedWorks.value = res.data.code === 0 ? (res.data.data || []) : []
}
const loading = ref(false)
const showParams = ref(true)
const downloading = ref(false)
const saving = ref(false)
const appealOpen = ref(false)
const appealReason = ref('')
const reportOpen = ref(false)
const reportReason = ref('内容违规')
const reportDetail = ref('')
const editOpen = ref(false)
const form = ref<any>({ name: '', introduction: '', category: '', tags: [] })

const pictureId = computed(() => String(route.params.id ?? ''))
const isOwner = computed(() => String(picture.value?.userId ?? '') !== '' && String(picture.value?.userId) === String(loginUser.loginUser?.id ?? ''))
const reviewStatus = computed(() => Number(picture.value?.reviewStatus ?? 1))
const reviewText = computed(() => ({ 0: '待审核', 1: '已通过', 2: '未通过' } as Record<number, string>)[reviewStatus.value] || '未知')
const reviewColor = computed(() => ({ 0: 'orange', 1: 'green', 2: 'red' } as Record<number, string>)[reviewStatus.value] || 'default')
const sizeText = computed(() => (picture.value?.picWidth && picture.value?.picHeight)
  ? picture.value.picWidth + ' × ' + picture.value.picHeight : '-')

function fmt(value?: string) {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '-' : d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

async function load() {
  if (!pictureId.value) return
  loading.value = true
  try {
    const res: any = await getPictureVoByIdUsingGet({ id: pictureId.value })
    if (res.data.code === 0) {
      picture.value = res.data.data || {}
      await loadRelatedWorks()
    } else {
      message.error(res.data.message || '图片不存在或无权查看')
    }
  } finally {
    loading.value = false
  }
}

async function download() {
  if (!picture.value?.id) return
  downloading.value = true
  try {
    const requestId = 'web-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8)
    const res: any = await authorizeDownload({ pictureId: picture.value.id, requestId })
    if (res.data.code !== 0) return message.error(res.data.message || '下载授权失败')
    message.success('已授权下载')
    window.open('http://localhost:8123' + res.data.data.downloadPath, '_blank')
  } catch {
    message.error('下载失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

function findSimilar() {
  router.push('/ai/similar?pictureId=' + pictureId.value)
}

async function share() {
  try {
    await navigator.clipboard.writeText(location.href)
    message.success('链接已复制')
  } catch {
    message.warning('复制失败，请手动复制地址栏链接')
  }
}

async function copyUrl() {
  if (!picture.value?.url) return
  try {
    await navigator.clipboard.writeText(picture.value.url)
    message.success('图片地址已复制')
  } catch {
    message.warning('复制失败')
  }
}

function openEdit() {
  form.value = {
    name: picture.value.name || '',
    introduction: picture.value.introduction || '',
    category: picture.value.category || '',
    tags: [...(picture.value.tags || [])],
  }
  editOpen.value = true
}

async function saveEdit() {
  saving.value = true
  try {
    const res: any = await editPictureUsingPost({
      id: picture.value.id,
      name: form.value.name,
      introduction: form.value.introduction,
      category: form.value.category,
      tags: form.value.tags,
    })
    if (res.data.code === 0) {
      message.success('已保存，图片将重新进入审核')
      editOpen.value = false
      await load()
    } else {
      message.error(res.data.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

function removePicture() {
  Modal.confirm({
    title: '删除这张图片',
    content: '删除后无法恢复，图片将从图库与所有空间中移除。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      const res: any = await deletePictureUsingPost({ id: picture.value.id })
      if (res.data.code === 0) {
        message.success('已删除')
        router.push('/me/gallery')
      } else {
        message.error(res.data.message || '删除失败')
      }
    },
  })
}

async function submitAppealAction() {
  if (!appealReason.value.trim()) return message.warning('请填写申诉理由')
  const res: any = await submitAppeal({ pictureId: picture.value.id, reason: appealReason.value })
  if (res.data.code === 0) {
    message.success('申诉已提交，我们会尽快复核')
    appealOpen.value = false
    appealReason.value = ''
  } else {
    message.error(res.data.message || '提交失败')
  }
}


async function doReport() {
  const res: any = await submitReport({
    targetType: 'picture',
    targetId: pictureId.value,
    reason: reportReason.value,
    detail: reportDetail.value,
  })
  if (res.data.code === 0) {
    message.success('举报已提交，我们会尽快处理')
    reportOpen.value = false
    reportDetail.value = ''
  } else {
    message.error(res.data.message || '提交失败')
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
#pictureDetailPage { padding: var(--space-5) var(--space-5) var(--space-8); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); margin-bottom: var(--space-4); }
.card-title { font-size: var(--font-3); }
.preview-wrap { display: grid; place-items: center; background: var(--surface-2); border-radius: var(--radius); padding: var(--space-4); }
.preview-img { max-height: 620px; object-fit: contain; }
.params { margin-top: var(--space-4); }
.params-head { display: flex; align-items: center; justify-content: space-between; cursor: pointer; padding: 6px 0; }
.params-head .eyebrow { color: var(--text-3); font-size: 12px; letter-spacing: .18em; }
.params-body { margin-top: 8px; }
.color-dot { display: inline-block; width: 12px; height: 12px; border-radius: 3px; margin-right: 6px; border: 1px solid var(--border-strong); vertical-align: -1px; }
.author-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.author-main { display: flex; align-items: center; gap: 10px; }
.author-main strong { display: block; }
.author-main span { font-size: 12px; }
.info-actions { display: flex; gap: 8px; margin-top: var(--space-3); }
.review-alert { margin-bottom: var(--space-4); }
.related-works { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: var(--space-4); }
.muted { color: var(--text-3); }
</style>
