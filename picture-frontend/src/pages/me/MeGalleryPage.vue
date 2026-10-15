<template>
  <div class="me-page gallery-page">
    <div class="section-head">
      <div><p class="eyebrow">MY GALLERY</p><h2>我的图库</h2><p>批量管理你上传的图片资产（审核状态、分类、标签）</p></div>
      <a-space>
        <a-button @click="router.push('/add_picture/batch')">批量上传</a-button>
        <a-button type="primary" @click="router.push('/me/upload')">上传图片</a-button>
      </a-space>
    </div>

    <a-alert v-if="pendingCount > 0" type="warning" show-icon class="pending-alert">
      <template #message>有 <strong>{{ pendingCount }}</strong> 张图片待审核</template>
      <template #description>
        <span>审核通过后会自动出现在公共图库；你也可以先在「AI 编目」里为它们生成标签与场景描述。</span>
        <a-button type="link" size="small" @click="showPending">只看待审核 →</a-button>
      </template>
    </a-alert>

    <a-card :bordered="false" class="surface-card">
      <div class="filters">
        <a-input v-model:value="query.name" placeholder="按名称搜索" allow-clear style="width: 200px" @press-enter="reload" />
        <a-select v-model:value="query.reviewStatus" placeholder="审核状态" allow-clear style="width: 140px" @change="reload">
          <a-select-option :value="0">待审核</a-select-option>
          <a-select-option :value="1">已通过</a-select-option>
          <a-select-option :value="2">已拒绝</a-select-option>
        </a-select>
        <a-input v-model:value="query.category" placeholder="分类" allow-clear style="width: 140px" @press-enter="reload" />
        <a-button type="primary" @click="reload">查询</a-button>
        <a-button @click="reset">重置</a-button>
        <span class="muted">共 {{ total }} 张</span>
      </div>

      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" :pagination="pagination"
               @change="onTableChange" size="middle">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'cover'">
            <img :src="record.thumbnailUrl || record.url" class="row-cover" alt="" loading="lazy" />
          </template>
          <template v-else-if="column.dataIndex === 'reviewStatus'">
            <a-tag :color="reviewColor(record.reviewStatus)">{{ reviewText(record.reviewStatus) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'picSize'">
            {{ formatSize(record.picSize) }}
          </template>
          <template v-else-if="column.dataIndex === 'tags'">
            <a-tag v-for="t in parseTags(record.tags)" :key="t">{{ t }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="router.push('/picture/' + record.id)">查看</a-button>
              <a-button type="link" size="small" @click="router.push('/ai/catalog?pictureId=' + record.id)">AI 编目</a-button>
              <a-popconfirm title="确认删除这张图片？" @confirm="remove(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { deletePictureUsingPost, listPictureVoByPageUsingPost } from '@/api/pictureController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { formatSize } from '@/utils'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const pendingCount = ref(0)
const query = reactive<{ name?: string; reviewStatus?: number; category?: string; current: number; pageSize: number }>({
  current: 1,
  pageSize: 10,
})

const columns = [
  { title: '预览', dataIndex: 'cover', width: 96 },
  { title: '名称', dataIndex: 'name', ellipsis: true },
  { title: '分类', dataIndex: 'category', width: 110 },
  { title: '标签', dataIndex: 'tags', width: 180 },
  { title: '尺寸', dataIndex: 'picWidth', width: 130, customRender: ({ record }: any) => record.picWidth + '×' + record.picHeight },
  { title: '大小', dataIndex: 'picSize', width: 100 },
  { title: '审核', dataIndex: 'reviewStatus', width: 100 },
  { title: '操作', dataIndex: 'action', width: 210 },
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (t: number) => '共 ' + t + ' 张',
})

function reviewText(status: number) {
  return ({ 0: '待审核', 1: '已通过', 2: '已拒绝' } as any)[status] ?? '未知'
}
function reviewColor(status: number) {
  return ({ 0: 'orange', 1: 'green', 2: 'red' } as any)[status] ?? 'default'
}
function parseTags(tags: any) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  try {
    const parsed = JSON.parse(tags)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return String(tags).split(',').filter(Boolean)
  }
}

async function load() {
  if (!loginUserStore.loginUser?.id) return
  loading.value = true
  try {
    const res: any = await listPictureVoByPageUsingPost({
      current: pagination.current,
      pageSize: pagination.pageSize,
      userId: loginUserStore.loginUser.id,
      name: query.name,
      reviewStatus: query.reviewStatus,
      category: query.category,
    })
    if (res.data.code === 0) {
      list.value = res.data.data?.records ?? []
      total.value = Number(res.data.data?.total ?? 0)
      pagination.total = total.value
    }
  } finally {
    loading.value = false
  }
}

function reload() {
  pagination.current = 1
  void load()
}

function reset() {
  query.name = undefined
  query.reviewStatus = undefined
  query.category = undefined
  reload()
}

function onTableChange(pag: any) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void load()
}

async function remove(record: any) {
  const res: any = await deletePictureUsingPost({ id: record.id })
  if (res.data.code === 0) {
    message.success('已删除')
    void load()
  } else message.error(res.data.message)
}

/** 只看待审核 */
function showPending() {
  query.reviewStatus = 0
  reload()
}

/** 拉取待审核数量（不受当前筛选影响） */
async function loadPendingCount() {
  if (!loginUserStore.loginUser?.id) return
  const res: any = await listPictureVoByPageUsingPost({
    current: 1,
    pageSize: 1,
    userId: loginUserStore.loginUser.id,
    reviewStatus: 0,
  })
  if (res.data.code === 0) pendingCount.value = Number(res.data.data?.total || 0)
}

onMounted(() => {
  void load()
  void loadPendingCount()
})
</script>

<style scoped>
.gallery-page { display: flex; flex-direction: column; gap: var(--space-5); }
.pending-alert { border-radius: var(--radius-lg); }
.filters { display: flex; align-items: center; gap: var(--space-3); flex-wrap: wrap; margin-bottom: var(--space-4); }
.row-cover { width: 64px; height: 48px; object-fit: cover; border-radius: var(--radius-sm); display: block; }
</style>
