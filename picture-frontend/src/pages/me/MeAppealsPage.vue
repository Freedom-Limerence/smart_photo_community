<template>
  <div class="me-page">
    <div class="section-head">
      <div>
        <p class="eyebrow">MY APPEALS</p>
        <h2>我的申诉</h2>
        <p>未通过审核的图片可以申诉；这里的每一条都会由人工复核</p>
      </div>
      <a-button :loading="loading" @click="load">刷新</a-button>
    </div>

    <a-spin :spinning="loading">
      <CardSkeleton v-if="loading && !list.length" variant="list" :count="4" />
      <ul v-else-if="list.length" class="appeal-list">
        <li v-for="item in list" :key="item.id" class="appeal-item">
          <div class="thumb-wrap" @click="open(item)">
            <img v-if="item.thumbnailUrl" :src="item.thumbnailUrl" alt="" loading="lazy" />
            <span v-else class="thumb-empty"><PictureOutlined /></span>
          </div>

          <div class="appeal-body">
            <div class="appeal-head">
              <a class="name" @click="open(item)">{{ item.pictureName || ('图片 #' + item.pictureId) }}</a>
              <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
            </div>

            <p class="reason"><span class="label">申诉理由</span>{{ item.reason }}</p>
            <p v-if="item.reviewMessage" class="reason muted"><span class="label">原审核意见</span>{{ item.reviewMessage }}</p>

            <div v-if="item.status !== 'PENDING'" class="result" :class="item.status.toLowerCase()">
              <span class="label">复核结论</span>
              <span>{{ item.handleResult || (item.status === 'ACCEPTED' ? '复核通过，图片已重新上架' : '经复核未通过') }}</span>
            </div>
            <div v-else class="pending-tip">
              <a-spin size="small" /> 正在等待人工复核，结果会通过站内通知告诉你
            </div>

            <div class="appeal-foot">
              <span class="muted">提交于 {{ formatTime(item.createTime) }}</span>
              <span v-if="item.handleTime" class="muted">· 处理于 {{ formatTime(item.handleTime) }}</span>
              <a-button v-if="item.status === 'REJECTED'" type="link" size="small" @click="open(item)">再次申诉 →</a-button>
            </div>
          </div>
        </li>
      </ul>

      <EmptyState v-else title="还没有申诉记录"
                  description="如果你的图片被判定为未通过审核，可以在图片详情页发起申诉。">
        <a-button type="primary" @click="router.push('/me/gallery')">去我的图库看看</a-button>
      </EmptyState>
    </a-spin>

    <div v-if="total > list.length" class="load-more">
      <a-button :loading="loadingMore" @click="loadMore">加载更多</a-button>
    </div>
  </div>
</template>


<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { PictureOutlined } from '@ant-design/icons-vue'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listMyAppeals } from '@/api/notification'

const router = useRouter()
const list = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const current = ref(1)

const statusMap: Record<string, string> = { PENDING: '待处理', ACCEPTED: '已受理', REJECTED: '已驳回' }
function statusText(s: string) { return statusMap[s] || s }
function statusColor(s: string) { return s === 'PENDING' ? 'orange' : s === 'ACCEPTED' ? 'green' : 'default' }

function open(item: any) {
  if (item.pictureId) router.push('/picture/' + item.pictureId)
}

function formatTime(value?: string) {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '-' : d.toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })
}

async function load() {
  loading.value = true
  try {
    const res: any = await listMyAppeals({ current: current.value, size: 10 })
    if (res.data.code === 0) {
      const records = res.data.data?.records || []
      list.value = current.value === 1 ? records : [...list.value, ...records]
      total.value = Number(res.data.data?.total || records.length)
    }
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  current.value += 1
  loadingMore.value = true
  try {
    await load()
  } finally {
    loadingMore.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.me-page { display: flex; flex-direction: column; }

.appeal-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: var(--space-4); }

.appeal-item {
  display: flex;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  transition: border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease);
}
.appeal-item:hover { border-color: var(--border-strong); box-shadow: var(--shadow-1); }

.thumb-wrap { flex: 0 0 auto; width: 96px; height: 72px; border-radius: var(--radius); overflow: hidden; background: var(--surface-2); cursor: pointer; }
.thumb-wrap img { width: 100%; height: 100%; object-fit: cover; display: block; }
.thumb-empty { display: grid; place-items: center; width: 100%; height: 100%; color: var(--text-3); }

.appeal-body { flex: 1 1 auto; min-width: 0; display: flex; flex-direction: column; gap: var(--space-2); }
.appeal-head { display: flex; align-items: center; gap: var(--space-3); }
.appeal-head .name { font-size: var(--font-4); font-weight: 500; cursor: pointer; }
.appeal-head .name:hover { color: var(--accent-hover); }

.reason { margin: 0; font-size: var(--font-2); color: var(--text-2); }
.label { display: inline-block; min-width: 76px; color: var(--text-3); font-size: var(--font-1); }

.result { display: flex; align-items: center; gap: 6px; padding: var(--space-2) var(--space-3); border-radius: var(--radius); font-size: var(--font-2); }
.result.accepted { background: var(--accent-soft); color: var(--accent-ink); }
.result.rejected { background: var(--danger-soft); color: var(--danger); }

.pending-tip { display: flex; align-items: center; gap: var(--space-2); color: var(--text-3); font-size: var(--font-2); }

.appeal-foot { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); font-size: var(--font-1); }

.muted { color: var(--text-3); }
.load-more { display: flex; justify-content: center; margin-top: var(--space-5); }

@media (max-width: 640px) {
  .appeal-item { flex-direction: column; }
  .thumb-wrap { width: 100%; height: 160px; }
  .label { min-width: 68px; }
}
</style>
