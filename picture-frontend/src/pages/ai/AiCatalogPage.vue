<template>
  <div class="ai-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">AI PHOTOGRAPHY WORKBENCH</p>
        <h1>摄影编目与元数据确认</h1>
        <p>为一张图片生成摄影元数据，人工确认后重新运行不会覆盖你的修改。</p>
      </div>
      <a-space>
        <a-button @click="router.push('/ai/search')">语义检索</a-button>
        <a-button @click="router.push('/ai/similar')">相似选片</a-button>
      </a-space>
    </section>

    <a-row :gutter="20">
      <a-col :xs="24" :lg="9">
        <a-card title="① 选择图片并创建任务" size="small">
          <a-input v-model:value="pictureId" placeholder="输入图片 ID（字符串，避免精度丢失）" allow-clear />
          <a-space class="mt12" wrap>
            <a-button type="primary" :loading="cataloging" @click="doCatalog">生成摄影编目</a-button>
            <a-button :loading="exifing" @click="doExif">提取 EXIF</a-button>
            <a-button :loading="loadingMeta" @click="loadMeta">查看元数据</a-button>
          </a-space>
          <a-alert v-if="lastTask" class="mt12" :type="lastTask.status === 'FAILED' ? 'error' : 'success'" show-icon
                   :message="'任务 ' + lastTask.taskType + ' → ' + lastTask.status"
                   :description="lastTask.errorMessage || ('provider: ' + lastTask.provider)" />
        </a-card>

        <a-card class="mt16" title="② 摄影元数据（人工复核）" size="small">
          <a-spin :spinning="loadingMeta">
            <template v-if="meta">
              <a-descriptions bordered :column="1" size="small">
                <a-descriptions-item label="相机">{{ (meta.cameraMake || '未识别') + ' ' + (meta.cameraModel || '') }}</a-descriptions-item>
                <a-descriptions-item label="镜头">{{ meta.lensModel || '未识别' }}</a-descriptions-item>
                <a-descriptions-item label="拍摄时间">{{ meta.shootTime ? new Date(meta.shootTime).toLocaleString('zh-CN') : '未识别' }}</a-descriptions-item>
                <a-descriptions-item label="方向 / 质量分">{{ (meta.orientation || '未识别') + ' / ' + (meta.qualityScore ?? '-') }}</a-descriptions-item>
                <a-descriptions-item label="EXIF 可用">{{ exifAvailable ? '是（上传时解析）' : '否（对象存储已剥离）' }}</a-descriptions-item>
                <a-descriptions-item label="确认状态">
                  <a-tag :color="meta.confirmed ? 'green' : 'orange'">{{ meta.confirmed ? '人工已确认' : '待人工确认' }}</a-tag>
                </a-descriptions-item>
              </a-descriptions>
              <a-form layout="vertical" class="mt12">
                <a-form-item label="主体标签"><a-input v-model:value="form.subjectTags" /></a-form-item>
                <a-form-item label="场景标签"><a-input v-model:value="form.sceneTags" /></a-form-item>
                <a-form-item label="光线标签"><a-input v-model:value="form.lightTags" /></a-form-item>
                <a-form-item label="构图标签"><a-input v-model:value="form.compositionTags" /></a-form-item>
                <a-button type="primary" :loading="confirming" @click="doConfirm">确认这些字段</a-button>
              </a-form>
            </template>
            <a-empty v-else description="输入图片 ID 后查看或生成元数据" />
          </a-spin>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="15">
        <a-card title="③ 我的编目任务" size="small">
          <template #extra>
            <a-select v-model:value="statusFilter" style="width: 130px" :options="statusOptions" @change="loadTasks" />
            <a-button type="link" @click="loadTasks">刷新</a-button>
          </template>
          <a-table :data-source="tasks" :columns="columns" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'action'">
                <a-button v-if="record.status === 'FAILED'" type="link" size="small" @click="doRetry(record)">重试</a-button>
                <a-button v-else-if="record.status === 'QUEUED' || record.status === 'RUNNING'" type="link" size="small"
                          danger @click="doCancel(record)">取消</a-button>
                <span v-else>—</span>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!tasks.length" description="还没有任务，先在上面创建一次编目" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import {
  cancelAiTask, catalogPicture, confirmAiMeta, getAiMeta, listMyAiTasks, retryAiTask, startExifTask,
} from '@/api/aiController'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const pictureId = ref<string>('')
const meta = ref<any>(null)
const tasks = ref<any[]>([])
const lastTask = ref<any>(null)
const cataloging = ref(false)
const exifing = ref(false)
const loadingMeta = ref(false)
const confirming = ref(false)
const statusFilter = ref<string | undefined>(undefined)
const form = reactive({ subjectTags: '', sceneTags: '', lightTags: '', compositionTags: '' })

const statusOptions = [
  { value: undefined, label: '全部状态' },
  { value: 'SUCCEEDED', label: '成功' },
  { value: 'FAILED', label: '失败' },
  { value: 'CANCELLED', label: '已取消' },
]
const columns = [
  { title: '任务类型', dataIndex: 'taskType' },
  { title: '状态', dataIndex: 'status' },
  { title: '重试', dataIndex: 'retryCount', width: 70 },
  { title: '失败原因', dataIndex: 'errorMessage', ellipsis: true },
  { title: '操作', dataIndex: 'action', width: 90 },
]

const exifAvailable = ref(false)

function statusColor(status: string) {
  return ({ SUCCEEDED: 'green', FAILED: 'red', CANCELLED: 'default', RUNNING: 'blue', QUEUED: 'orange', RETRYING: 'purple' } as any)[status] || 'default'
}

async function loadTasks() {
  const res: any = await listMyAiTasks({ current: 1, size: 20, status: statusFilter.value })
  if (res.data.code === 0) tasks.value = res.data.data || []
}

async function loadMeta() {
  if (!pictureId.value) return message.warning('请先填写图片 ID')
  loadingMeta.value = true
  try {
    const res: any = await getAiMeta(pictureId.value)
    meta.value = res.data.code === 0 ? res.data.data : null
    if (meta.value) {
      form.subjectTags = meta.value.subjectTags || ''
      form.sceneTags = meta.value.sceneTags || ''
      form.lightTags = meta.value.lightTags || ''
      form.compositionTags = meta.value.compositionTags || ''
      exifAvailable.value = !!(meta.value.exifJson && meta.value.exifJson.includes('"available":true'))
    }
  } finally {
    loadingMeta.value = false
  }
}

async function doCatalog() {
  if (!pictureId.value) return message.warning('请先填写图片 ID')
  cataloging.value = true
  try {
    const res: any = await catalogPicture(pictureId.value)
    if (res.data.code === 0) {
      lastTask.value = res.data.data
      message.success('编目任务已完成')
      await Promise.all([loadMeta(), loadTasks()])
    } else message.error(res.data.message)
  } finally {
    cataloging.value = false
  }
}

async function doExif() {
  if (!pictureId.value) return message.warning('请先填写图片 ID')
  exifing.value = true
  try {
    const res: any = await startExifTask(pictureId.value)
    if (res.data.code === 0) {
      lastTask.value = res.data.data
      message.success('EXIF 任务已完成')
      await Promise.all([loadMeta(), loadTasks()])
    } else message.error(res.data.message)
  } finally {
    exifing.value = false
  }
}

async function doConfirm() {
  if (!pictureId.value) return message.warning('请先填写图片 ID')
  confirming.value = true
  try {
    const res: any = await confirmAiMeta({ pictureId: pictureId.value, ...form })
    if (res.data.code === 0) {
      meta.value = res.data.data
      message.success('已确认，重新运行任务不会覆盖这些字段')
    } else message.error(res.data.message)
  } finally {
    confirming.value = false
  }
}

async function doRetry(record: any) {
  const res: any = await retryAiTask(record.id)
  if (res.data.code === 0) { message.success('已重试'); await loadTasks() } else message.error(res.data.message)
}

async function doCancel(record: any) {
  const res: any = await cancelAiTask(record.id)
  if (res.data.code === 0) { message.success('已取消'); await loadTasks() } else message.error(res.data.message)
}

onMounted(async () => {
  const q = router.currentRoute.value.query.pictureId
  if (q) pictureId.value = String(q)
  // 任务与元数据都属于登录用户：未登录时不请求接口，避免被全局拦截器强制跳转
  await loginUserStore.fetchLoginUser()
  if (!loginUserStore.loginUser?.id) return
  await loadTasks()
  if (pictureId.value) await loadMeta()
})
</script>

<style scoped>
.ai-page { max-width: 1180px; margin: 0 auto; }
.page-heading { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 22px; }
.eyebrow { margin: 0 0 8px; color: #668473; font-size: 12px; letter-spacing: .16em; }
h1 { margin: 0 0 6px; font-size: 26px; }
.mt12 { margin-top: 12px; }
.mt16 { margin-top: 16px; }
</style>
