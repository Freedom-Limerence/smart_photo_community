<template>
  <div class="me-page">
    <div class="section-head">
      <div>
        <p class="eyebrow">MY WORKS</p>
        <h2>我的作品 <small>{{ sorted.length }} 件</small></h2>
        <p>已发布到社区的摄影作品</p>
      </div>
      <a-space>
        <a-button :type="selectMode ? 'default' : 'primary'" @click="toggleSelectMode">
          {{ selectMode ? '退出管理' : '批量管理' }}
        </a-button>
        <a-button type="primary" @click="router.push('/community/create')">发布新作品</a-button>
      </a-space>
    </div>

    <!-- 工具条：状态筛选 + 排序 + 视图 -->
    <div class="toolbar">
      <a-radio-group v-model:value="status" button-style="solid" size="small">
        <a-radio-button v-for="item in statusOptions" :key="item.value" :value="item.value">
          {{ item.label }} ({{ countOf(item.value) }})
        </a-radio-button>
      </a-radio-group>
      <div class="toolbar-right">
        <a-select v-model:value="sort" size="small" style="width: 132px">
          <a-select-option value="latest">最新发布</a-select-option>
          <a-select-option value="hot">最多点赞</a-select-option>
          <a-select-option value="view">最多浏览</a-select-option>
          <a-select-option value="comment">最多评论</a-select-option>
        </a-select>
        <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
          <a-radio-button value="grid">网格</a-radio-button>
          <a-radio-button value="list">列表</a-radio-button>
        </a-radio-group>
        <a-button size="small" :loading="loading" @click="load">刷新</a-button>
      </div>
    </div>

    <!-- 批量操作条 -->
    <div v-if="selectMode" class="batch-bar">
      <a-checkbox :checked="allSelected" :indeterminate="picked.length > 0 && !allSelected" @change="toggleAll">
        全选本页（{{ sorted.length }}）
      </a-checkbox>
      <span class="muted">已选 {{ picked.length }} 件</span>
      <a-space>
        <a-button size="small" :disabled="!picked.length" @click="copySelected">复制链接</a-button>
        <a-button size="small" danger :disabled="!picked.length" :loading="deleting" @click="removeSelected">
          批量删除
        </a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <MasonryGrid v-if="sorted.length && viewMode === 'grid'" :items="sorted">
        <template #default="{ item }">
          <div class="work-wrap" :class="{ picked: picked.includes(String(item.id)) }">
            <label v-if="selectMode" class="pick-box">
              <input type="checkbox" :checked="picked.includes(String(item.id))" @change="togglePick(String(item.id))" />
            </label>
            <WorkCard :work="item" @open="onCardClick" />
            <div v-if="selectMode" class="card-actions">
              <a-button size="small" type="text" @click="openWork(item)">查看</a-button>
              <a-button size="small" type="text" @click="copyLink(item)">复制链接</a-button>
              <a-button size="small" type="text" danger @click="removeOne(item)">删除</a-button>
            </div>
            <div v-else class="card-badges">
              <a-tag v-if="statusOf(item) !== 'PUBLISHED'" :color="statusColor(statusOf(item))">
                {{ statusLabel(statusOf(item)) }}
              </a-tag>
              <a-tag v-if="item.reviewStatus === 0" color="orange">待审核</a-tag>
              <a-tag v-else-if="item.reviewStatus === 2" color="red">未过审</a-tag>
            </div>
          </div>
        </template>
      </MasonryGrid>

      <ul v-else-if="sorted.length" class="work-list">
        <li v-for="item in sorted" :key="item.id">
          <a-checkbox v-if="selectMode" :checked="picked.includes(String(item.id))" @change="togglePick(String(item.id))" />
          <img :src="thumbOf(item)" alt="" loading="lazy" @click="openWork(item)" />
          <div class="work-list-main" @click="openWork(item)">
            <strong>{{ item.title || '未命名作品' }}</strong>
            <span class="muted">{{ fmt(item.createTime) }}</span>
            <div class="work-list-stats">
              <span>♥ {{ item.likeCount ?? 0 }}</span>
              <span>★ {{ item.favoriteCount ?? 0 }}</span>
              <span>💬 {{ item.commentCount ?? 0 }}</span>
              <span>👁 {{ item.viewCount ?? 0 }}</span>
            </div>
          </div>
          <a-tag :color="statusColor(statusOf(item))">{{ statusLabel(statusOf(item)) }}</a-tag>
          <a-space>
            <a-button size="small" @click="openWork(item)">查看</a-button>
            <a-button size="small" @click="copyLink(item)">复制链接</a-button>
            <a-button size="small" danger @click="removeOne(item)">删除</a-button>
          </a-space>
        </li>
      </ul>

      <a-empty v-else-if="!loading" :description="status === 'ALL' ? '还没有发布作品' : '该状态下没有作品'">
        <a-button type="primary" @click="router.push('/community/create')">去发布作品</a-button>
      </a-empty>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { deleteWorks, listWorksByAuthor } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const list = ref<any[]>([])
const loading = ref(false)
const deleting = ref(false)
const status = ref('ALL')
const sort = ref('latest')
const viewMode = ref<'grid' | 'list'>('grid')
const selectMode = ref(false)
const picked = ref<string[]>([])
const userId = computed(() => loginUserStore.loginUser?.id)

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已下线', value: 'OFFLINE' },
]

function statusOf(work: any) { return work?.status || 'DRAFT' }
function statusLabel(value: string) {
  return ({ PUBLISHED: '已发布', DRAFT: '草稿', OFFLINE: '已下线', BANNED: '已封禁' } as Record<string, string>)[value] || value
}
function statusColor(value: string) {
  return ({ PUBLISHED: 'green', DRAFT: 'default', OFFLINE: 'orange', BANNED: 'red' } as Record<string, string>)[value] || 'default'
}
function countOf(value: string) {
  return value === 'ALL' ? list.value.length : list.value.filter((w) => statusOf(w) === value).length
}
function thumbOf(work: any) {
  const pic = work?.pictures?.[0]
  return work?.coverUrl || pic?.thumbnailUrl || pic?.url || ''
}
function fmt(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

const sorted = computed(() => {
  const base = status.value === 'ALL' ? [...list.value] : list.value.filter((w) => statusOf(w) === status.value)
  const num = (v: any) => Number(v ?? 0)
  if (sort.value === 'hot') return base.sort((a, b) => num(b.likeCount) - num(a.likeCount))
  if (sort.value === 'view') return base.sort((a, b) => num(b.viewCount) - num(a.viewCount))
  if (sort.value === 'comment') return base.sort((a, b) => num(b.commentCount) - num(a.commentCount))
  return base.sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime())
})

const allSelected = computed(() => sorted.value.length > 0 && picked.value.length === sorted.value.length)

function toggleSelectMode() {
  selectMode.value = !selectMode.value
  picked.value = []
}

function togglePick(id: string) {
  const index = picked.value.indexOf(id)
  if (index >= 0) picked.value.splice(index, 1)
  else picked.value.push(id)
}

function toggleAll() {
  picked.value = allSelected.value ? [] : sorted.value.map((w) => String(w.id))
}

function onCardClick(work: any) {
  if (selectMode.value) togglePick(String(work.id))
  else openWork(work)
}

function openWork(work: any) {
  router.push('/works/' + work.id)
}

async function copyLink(work: any) {
  await copyText(location.origin + '/works/' + work.id)
}

async function copySelected() {
  await copyText(picked.value.map((id) => location.origin + '/works/' + id).join('\n'))
}

async function copyText(text: string) {
  try {
    await navigator.clipboard.writeText(text)
    message.success('已复制到剪贴板')
  } catch {
    message.warning('复制失败，请手动复制')
  }
}

function removeOne(work: any) {
  Modal.confirm({
    title: '删除作品',
    content: '删除后作品将从社区移除（图片仍在云图库中）。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      await doDelete([String(work.id)])
    },
  })
}

function removeSelected() {
  Modal.confirm({
    title: '批量删除作品',
    content: '将删除所选 ' + picked.value.length + ' 件作品，删除后无法在社区展示。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      await doDelete([...picked.value])
    },
  })
}

async function doDelete(ids: string[]) {
  deleting.value = true
  try {
    const res: any = await deleteWorks(ids)
    if (res.data.code === 0) {
      const removed = Number(res.data.data || 0)
      message.success('已删除 ' + removed + ' 件作品' + (removed < ids.length ? '（部分作品无权删除）' : ''))
      picked.value = []
      await load()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } finally {
    deleting.value = false
  }
}

async function load() {
  if (!userId.value) return
  loading.value = true
  try {
    const res: any = await listWorksByAuthor({ authorId: userId.value, current: 1, size: 60 })
    list.value = res.data.code === 0 ? (res.data.data?.records ?? []) : []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.me-page { display: flex; flex-direction: column; }
.section-head h2 small { color: var(--text-3); font-weight: 400; font-size: var(--font-3); }
.toolbar { display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap; margin: var(--space-4) 0; }
.toolbar-right { display: flex; align-items: center; gap: 8px; }
.batch-bar { display: flex; align-items: center; gap: 16px; padding: 10px 14px; margin-bottom: var(--space-3); border-radius: var(--radius); background: var(--accent-soft); }
.work-wrap { position: relative; }
.work-wrap.picked { outline: 2px solid var(--accent); outline-offset: 3px; border-radius: var(--radius); }
.pick-box { position: absolute; top: 8px; left: 8px; z-index: 2; padding: 2px 4px; border-radius: 4px; background: rgba(255, 255, 255, .9); }
.card-badges { display: flex; gap: 4px; margin-top: 6px; }
.card-actions { display: flex; gap: 4px; margin-top: 6px; }
.work-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.work-list li { display: flex; align-items: center; gap: 14px; padding: 10px; border: 1px solid var(--border); border-radius: var(--radius); }
.work-list img { width: 120px; height: 80px; object-fit: cover; border-radius: var(--radius-sm); cursor: pointer; }
.work-list-main { flex: 1 1 auto; min-width: 0; cursor: pointer; }
.work-list-main strong { display: block; }
.work-list-stats { display: flex; gap: 12px; margin-top: 4px; color: var(--text-3); font-size: 12px; }
.muted { color: var(--text-3); }
</style>
