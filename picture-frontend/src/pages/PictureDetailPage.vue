<template>
  <div id="pictureDetailPage">
    <a-row :gutter="[16, 16]">
      <!-- 图片预览 -->
      <a-col :sm="24" :md="16" :xl="18">
        <a-card title="图片预览">
          <a-image :src="picture.url" style="max-height: 600px; object-fit: contain" />
        </a-card>
      </a-col>
      <!-- 图片信息区域 -->
      <a-col :sm="24" :md="8" :xl="6">
        <a-alert v-if="isOwner && reviewStatus === 0" type="warning" show-icon class="review-alert"
                 message="这张图片正在等待审核"
                 description="审核通过后才会出现在公共图库、被其他人看到。在此之前只有你自己能在这里看到它。" />
        <a-alert v-else-if="isOwner && reviewStatus === 2" type="error" show-icon class="review-alert"
                 :message="'这张图片未通过审核'"
                 :description="picture.reviewMessage || '可以修改分类与标签后重新提交审核。'" />
        <a-card title="图片信息">
          <a-descriptions :column="1">
            <a-descriptions-item label="审核状态">
              <a-tag :color="reviewColor">{{ reviewText }}</a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="作者">
              <a-space>
                <a-avatar :size="24" :src="picture.user?.userAvatar" />
                <div>{{ picture.user?.userName }}</div>
              </a-space>
            </a-descriptions-item>
            <a-descriptions-item label="名称">
              {{ picture.name ?? '未命名' }}
            </a-descriptions-item>
            <a-descriptions-item label="简介">
              {{ picture.introduction ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="分类">
              {{ picture.category ?? '默认' }}
            </a-descriptions-item>
            <a-descriptions-item label="标签">
              <a-tag v-for="tag in picture.tags" :key="tag">
                {{ tag }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="格式">
              {{ picture.picFormat ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽度">
              {{ picture.picWidth ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="高度">
              {{ picture.picHeight ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="宽高比">
              {{ picture.picScale ?? '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="大小">
              {{ formatSize(picture.picSize) }}
            </a-descriptions-item>
            <a-descriptions-item label="主色调">
              <a-space>
                {{ picture.picColor ?? '-' }}
                <div
                  v-if="picture.picColor"
                  :style="{
                    width: '16px',
                    height: '16px',
                    backgroundColor: toHexColor(picture.picColor),
                  }"
                />
              </a-space>
            </a-descriptions-item>
          </a-descriptions>
          <!-- 图片操作 -->
          <a-space wrap>
            <a-button type="primary" @click="doDownload">
              免费下载
              <template #icon>
                <DownloadOutlined />
              </template>
            </a-button>
            <a-button :icon="h(ShareAltOutlined)" type="primary" ghost @click="doShare">
              分享
            </a-button>
            <a-button v-if="canEdit" :icon="h(EditOutlined)" type="default" @click="doEdit">
              编辑
            </a-button>
            <a-button v-if="canDelete" :icon="h(DeleteOutlined)" danger @click="doDelete">
              删除
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
    <ShareModal ref="shareModalRef" :link="shareLink" />
  </div>
</template>

<script setup lang="ts">
import { computed, h, onMounted, ref } from 'vue'
import { deletePictureUsingPost, getPictureVoByIdUsingGet } from '@/api/pictureController.ts'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import {
  DeleteOutlined,
  DownloadOutlined,
  EditOutlined,
  ShareAltOutlined,
} from '@ant-design/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import { saveAs } from 'file-saver'
import { formatSize, toHexColor, parseId } from '@/utils'
import { authorizeDownload, downloadAuthorizedFile } from '@/api/photoCommunity'
import ShareModal from '@/components/ShareModal.vue'
import { SPACE_PERMISSION_ENUM } from '@/constants/space.ts'

interface Props {
  id: string | number
}

const props = defineProps<Props>()
const route = useRoute()
const picture = ref<API.PictureVO>({})
const loginUserStore = useLoginUserStore()

/** 审核状态：作者需要看到自己图片的审核进度 */
const reviewStatus = computed(() => (picture.value as any).reviewStatus)
const isOwner = computed(() => String(picture.value.userId) === String(loginUserStore.loginUser?.id))
const reviewText = computed(() => (({ 0: "待审核", 1: "已通过", 2: "已拒绝" } as any)[reviewStatus.value as any] ?? "未知"))
const reviewColor = computed(() => (({ 0: "orange", 1: "green", 2: "red" } as any)[reviewStatus.value as any] ?? "default"))
const pictureId = computed(() => parseId(props.id))

// 通用权限检查函数
function createPermissionChecker(permission: string) {
  return computed(() => {
    return (picture.value.permissionList ?? []).includes(permission)
  })
}

// 定义权限检查
const canEdit = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_EDIT)
const canDelete = createPermissionChecker(SPACE_PERMISSION_ENUM.PICTURE_DELETE)

// 获取图片详情
const fetchPictureDetail = async () => {
  try {
    if (!pictureId.value) {
      message.error('图片编号无效')
      return
    }
    const res = await getPictureVoByIdUsingGet({ id: pictureId.value })
    if (res.data.code === 0 && res.data.data) {
      picture.value = res.data.data
    } else {
      message.error('获取图片详情失败，' + res.data.message)
    }
  } catch (e: any) {
    message.error('获取图片详情失败：' + e.message)
  }
}

onMounted(() => {
  fetchPictureDetail()
})

const router = useRouter()

// 编辑
const doEdit = () => {
  router.push({
    path: '/add_picture',
    query: {
      id: picture.value.id,
      spaceId: picture.value.spaceId,
    },
  })
}

// 删除数据
const doDelete = async () => {
  const id = picture.value.id
  if (!id) {
    return
  }
  const res = await deletePictureUsingPost({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
  } else {
    message.error('删除失败')
  }
}

// 通过短期授权令牌下载，避免把 COS 原图地址直接暴露给浏览器。
const doDownload = async () => {
  if (!pictureId.value) return
  try {
    const requestId = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`
    const workId = parseId(route.query.workId)
    const authorizeRes: any = await authorizeDownload({ pictureId: pictureId.value, workId, requestId })
    if (authorizeRes.data.code !== 0 || !authorizeRes.data.data?.downloadToken) {
      message.error(authorizeRes.data.message || '暂无下载权限')
      return
    }
    const fileRes: any = await downloadAuthorizedFile(authorizeRes.data.data.downloadToken)
    const blob = fileRes.data instanceof Blob ? fileRes.data : new Blob([fileRes.data])
    saveAs(blob, picture.value.name || `picture-${pictureId.value}`)
    message.success('下载已开始')
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '下载失败，请稍后重试')
  }
}

// ----- 分享操作 ----
const shareModalRef = ref()
// 分享链接
const shareLink = ref('')
// 分享
const doShare = () => {
  shareLink.value = `${window.location.protocol}//${window.location.host}/picture/${picture.value.id}`
  if (shareModalRef.value) {
    shareModalRef.value.openModal()
  }
}
</script>

<style scoped>
#pictureDetailPage {
  margin-bottom: 16px;
}
</style>

<style scoped>
.review-alert { margin-bottom: 12px; border-radius: var(--radius); }
</style>