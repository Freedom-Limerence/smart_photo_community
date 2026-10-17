<template>
  <div class="space-page container">
    <a-spin :spinning="loading">
      <!-- 空间头部 -->
      <header class="space-head">
        <div class="head-main">
          <p class="eyebrow">SPACE / TEAM LIBRARY</p>
          <h1>
            {{ space.spaceName || '空间' }}
            <a-tag :color="space.spaceType === 1 ? 'blue' : 'green'">{{ typeText }}</a-tag>
            <a-tag v-if="myRole" color="purple">{{ roleText(myRole) }}</a-tag>
          </h1>
          <div class="head-stats">
            <span><PictureOutlined /> {{ total }} 张图片</span>
            <span><TeamOutlined /> {{ members.length }} 位成员</span>
            <span><CloudOutlined /> {{ formatSize(space.totalSize) }} / {{ formatSize(space.maxSize) }}</span>
          </div>
          <a-progress class="quota" :percent="quotaPercent" :status="quotaPercent > 90 ? 'exception' : 'normal'"
                      :stroke-color="quotaPercent > 90 ? '#c4564a' : undefined" />
        </div>
        <div class="head-actions">
          <a-button v-if="canUpload" type="primary" @click="router.push('/add_picture?spaceId=' + spaceId)">
            <UploadOutlined /> 上传图片
          </a-button>
          <a-button @click="router.push('/space_analyze?spaceId=' + spaceId)">
            <BarChartOutlined /> 空间分析
          </a-button>
          <a-button v-if="canManage" @click="router.push('/spaceUserManage/' + spaceId)">
            <TeamOutlined /> 成员与邀请码
          </a-button>
        </div>
      </header>

      <!-- 内容 Tabs -->
      <a-tabs v-model:activeKey="tab" class="space-tabs">
        <!-- ========== 图片 ========== -->
        <a-tab-pane key="pictures" :tab="'图片（' + total + '）'">
          <div class="toolbar">
            <a-input-search v-model:value="searchParams.searchText" placeholder="搜索图片名称 / 简介" style="width: 260px"
                            allow-clear @search="reload" />
            <a-select v-model:value="searchParams.category" placeholder="全部分类" style="width: 150px" allow-clear @change="reload" />
            <a-select v-model:value="sortBy" style="width: 140px">
              <a-select-option value="latest">最新上传</a-select-option>
              <a-select-option value="name">按名称</a-select-option>
            </a-select>
            <a-button :loading="loading" @click="fetchData">刷新</a-button>
            <div class="toolbar-right">
              <a-button :type="selectMode ? 'default' : 'primary'" @click="toggleSelectMode">
                {{ selectMode ? '退出管理' : '批量管理' }}
              </a-button>
            </div>
          </div>

          <div v-if="selectMode" class="batch-bar">
            <a-checkbox :checked="allSelected" :indeterminate="picked.length > 0 && !allSelected" @change="toggleAll">
              全选本页（{{ dataList.length }}）
            </a-checkbox>
            <span class="muted">已选 {{ picked.length }} 张</span>
            <a-space>
              <a-button size="small" :disabled="!picked.length" @click="copySelected">复制地址</a-button>
              <a-button size="small" :disabled="!picked.length" :loading="working" @click="batchMove()">移出空间</a-button>
              <a-button size="small" danger :disabled="!picked.length" :loading="working" @click="batchDelete">批量删除</a-button>
            </a-space>
          </div>

          <a-spin :spinning="loading">
            <div v-if="dataList.length" class="pic-grid">
              <div v-for="item in dataList" :key="item.id" class="pic-card"
                   :class="{ picked: picked.includes(String(item.id)) }">
                <label v-if="selectMode" class="pick-box">
                  <input type="checkbox" :checked="picked.includes(String(item.id))" @change="togglePick(String(item.id))" />
                </label>
                <img :src="item.thumbnailUrl || item.url" :alt="item.name" loading="lazy" @click="openPicture(item)" />
                <div class="pic-body">
                  <strong class="ellipsis">{{ item.name || '未命名' }}</strong>
                  <span class="muted">{{ item.category || '未分类' }}</span>
                </div>
                <div class="pic-ops">
                  <a-button type="text" size="small" @click="openPicture(item)">查看</a-button>
                  <a-button v-if="canEdit" type="text" size="small" @click="openPicture(item)">编辑</a-button>
                  <a-button v-if="canDelete" type="text" size="small" danger @click="removeOne(item)">删除</a-button>
                </div>
              </div>
            </div>
            <a-empty v-else description="这个空间还没有图片">
              <a-button v-if="canUpload" type="primary" @click="router.push('/add_picture?spaceId=' + spaceId)">上传第一张</a-button>
            </a-empty>
          </a-spin>

          <div class="pager">
            <a-pagination v-model:current="searchParams.current" v-model:pageSize="searchParams.pageSize"
                          :total="total" :show-size-changer="true" @change="onPageChange" />
          </div>
        </a-tab-pane>

        <!-- ========== 成员 ========== -->
        <a-tab-pane key="members" :tab="'成员（' + members.length + '）'">
          <div class="toolbar">
            <a-button v-if="canManage" type="primary" @click="router.push('/spaceUserManage/' + spaceId)">
              <UserAddOutlined /> 添加成员 / 生成邀请码
            </a-button>
            <a-button :loading="membersLoading" @click="loadMembers">刷新</a-button>
          </div>
          <ul v-if="members.length" class="member-list">
            <li v-for="member in members" :key="member.id">
              <a-avatar :src="member.userAvatar" :size="40">{{ (member.userName || '成').slice(0, 1) }}</a-avatar>
              <div class="member-main">
                <strong>{{ member.userName || '成员' }}</strong>
                <span class="muted">加入于 {{ fmt(member.createTime) }}</span>
              </div>
              <a-select v-if="canManage" :value="member.spaceRole" style="width: 130px" size="small"
                        :options="roleOptions" @change="(value: string) => changeRole(member, value)" />
              <a-tag v-else>{{ roleText(member.spaceRole) }}</a-tag>
              <a-button v-if="canManage" type="text" size="small" danger @click="removeMember(member)">移出</a-button>
            </li>
          </ul>
          <a-empty v-else description="还没有其他成员">
            <a-button v-if="canManage" type="primary" @click="router.push('/spaceUserManage/' + spaceId)">邀请同事加入</a-button>
          </a-empty>
        </a-tab-pane>
      </a-tabs>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  BarChartOutlined, CloudOutlined, PictureOutlined, TeamOutlined, UploadOutlined, UserAddOutlined,
} from '@ant-design/icons-vue'
import { getSpaceVoByIdUsingGet } from '@/api/spaceController'
import { editSpaceUserUsingPost, deleteSpaceUserUsingPost, listSpaceUserUsingPost } from '@/api/spaceUserController'
import { deletePictureUsingPost, editPictureByBatchUsingPost, listPictureVoByPageUsingPost } from '@/api/pictureController'
import { formatSize } from '@/utils'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loginUser = useLoginUserStore()

const space = ref<any>({})
const dataList = ref<any[]>([])
const members = ref<any[]>([])
const loading = ref(false)
const membersLoading = ref(false)
const working = ref(false)
const total = ref(0)
const tab = ref('pictures')
const sortBy = ref('latest')
const selectMode = ref(false)
const picked = ref<string[]>([])
const searchParams = ref<any>({ current: 1, pageSize: 12, searchText: '', category: undefined })
const myRole = ref('')

const spaceId = computed(() => String(route.params.id ?? ''))
const typeText = computed(() => (space.value?.spaceType === 1 ? '团队空间' : '私有空间'))
const quotaPercent = computed(() => {
  const max = Number(space.value?.maxSize ?? 0)
  if (!max) return 0
  return Number(Math.min(100, ((Number(space.value?.totalSize ?? 0) * 100) / max)).toFixed(1))
})
const roleOptions = [
  { label: '查看者', value: 'viewer' },
  { label: '编辑者', value: 'editor' },
  { label: '管理员', value: 'admin' },
]

function roleText(role?: string) {
  return ({ viewer: '查看者', editor: '编辑者', admin: '管理员' } as Record<string, string>)[role || ''] || role || '成员'
}
function fmt(value?: string) {
  if (!value) return '-'
  const d = new Date(value)
  return Number.isNaN(d.getTime()) ? '-' : d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}
function openPicture(item: any) {
  router.push('/picture/' + item.id)
}

const canUpload = computed(() => ['admin', 'editor'].includes(myRole.value) || space.value?.userId === loginUser.loginUser?.id)
const canEdit = computed(() => canUpload.value)
const canDelete = computed(() => canUpload.value)
const canManage = computed(() => myRole.value === 'admin' || space.value?.userId === loginUser.loginUser?.id)
const allSelected = computed(() => dataList.value.length > 0 && picked.value.length === dataList.value.length)

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
  picked.value = allSelected.value ? [] : dataList.value.map((i) => String(i.id))
}

async function loadSpace() {
  const res: any = await getSpaceVoByIdUsingGet({ id: spaceId.value })
  if (res.data.code === 0) {
    space.value = res.data.data || {}
    myRole.value = res.data.data?.spaceRole || (res.data.data?.userId === loginUser.loginUser?.id ? 'admin' : '')
  } else {
    message.error(res.data.message || '空间不存在或无权访问')
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await listPictureVoByPageUsingPost({
      spaceId: spaceId.value,
      current: searchParams.value.current,
      pageSize: searchParams.value.pageSize,
      searchText: searchParams.value.searchText || undefined,
      category: searchParams.value.category || undefined,
    })
    if (res.data.code === 0) {
      const records = res.data.data?.records || []
      dataList.value = sortBy.value === 'name'
        ? [...records].sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'zh-CN'))
        : records
      total.value = Number(res.data.data?.total ?? records.length)
    } else {
      message.error(res.data.message || '加载图片失败')
    }
    picked.value = []
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  if (!spaceId.value) return
  membersLoading.value = true
  try {
    const res: any = await listSpaceUserUsingPost({ spaceId: spaceId.value })
    members.value = res.data.code === 0 ? (res.data.data || []) : []
  } finally {
    membersLoading.value = false
  }
}

function reload() {
  searchParams.value.current = 1
  void fetchData()
}
function onPageChange() {
  void fetchData()
}

async function batchDelete() {
  Modal.confirm({
    title: '批量删除图片',
    content: '将删除所选 ' + picked.value.length + ' 张图片，删除后无法恢复。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      working.value = true
      try {
        let done = 0
        for (const id of picked.value) {
          const res: any = await deletePictureUsingPost({ id })
          if (res.data.code === 0) done++
        }
        message.success('已删除 ' + done + ' 张图片')
        picked.value = []
        await fetchData()
      } finally {
        working.value = false
      }
    },
  })
}

async function batchMove() {
  working.value = true
  try {
    const res: any = await editPictureByBatchUsingPost({
      pictureIdList: picked.value,
      spaceId: undefined,
    })
    if (res.data.code === 0) {
      message.success('已移出空间 ' + picked.value.length + ' 张')
      picked.value = []
      await fetchData()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } finally {
    working.value = false
  }
}

async function copySelected() {
  const urls = dataList.value
    .filter((i) => picked.value.includes(String(i.id)))
    .map((i) => i.url)
    .filter(Boolean)
  try {
    await navigator.clipboard.writeText(urls.join('\n'))
    message.success('已复制 ' + urls.length + ' 个图片地址')
  } catch {
    message.warning('复制失败')
  }
}

function removeOne(item: any) {
  Modal.confirm({
    title: '删除图片',
    content: '删除后无法恢复。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      const res: any = await deletePictureUsingPost({ id: item.id })
      if (res.data.code === 0) {
        message.success('已删除')
        await fetchData()
      } else {
        message.error(res.data.message || '删除失败')
      }
    },
  })
}

async function changeRole(member: any, role: string) {
  const res: any = await editSpaceUserUsingPost({ id: member.id, spaceRole: role })
  if (res.data.code === 0) {
    member.spaceRole = role
    message.success('已将 ' + (member.userName || '成员') + ' 调整为' + roleText(role))
  } else {
    message.error(res.data.message || '调整失败')
  }
}

function removeMember(member: any) {
  Modal.confirm({
    title: '移出成员',
    content: '确定把 ' + (member.userName || '该成员') + ' 移出空间吗？',
    okText: '确认移出',
    okType: 'danger',
    async onOk() {
      const res: any = await deleteSpaceUserUsingPost({ id: member.id })
      if (res.data.code === 0) {
        message.success('已移出')
        await loadMembers()
      } else {
        message.error(res.data.message || '操作失败')
      }
    },
  })
}

async function init() {
  await loadSpace()
  await fetchData()
  await loadMembers()
}

onMounted(init)
watch(() => route.params.id, init)
watch(sortBy, () => {
  if (sortBy.value === 'name') {
    dataList.value = [...dataList.value].sort((a, b) => String(a.name || '').localeCompare(String(b.name || ''), 'zh-CN'))
  } else {
    void fetchData()
  }
})
</script>

<style scoped>
.space-page { padding: var(--space-6) var(--space-5) var(--space-8); }
.space-head { display: flex; align-items: flex-start; justify-content: space-between; gap: var(--space-5); flex-wrap: wrap; margin-bottom: var(--space-4); }
.head-main { flex: 1 1 420px; min-width: 0; }
.head-main .eyebrow { margin: 0 0 6px; color: var(--text-3); font-size: 12px; letter-spacing: .2em; }
.head-main h1 { margin: 0 0 10px; font-size: var(--font-7); display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.head-stats { display: flex; flex-wrap: wrap; gap: 18px; color: var(--text-2); font-size: var(--font-2); margin-bottom: 8px; }
.head-stats span { display: inline-flex; align-items: center; gap: 6px; }
.quota { max-width: 420px; }
.head-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.space-tabs { margin-top: var(--space-3); }
.toolbar { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: var(--space-4); }
.toolbar-right { margin-left: auto; }
.batch-bar { display: flex; align-items: center; gap: 16px; padding: 10px 14px; margin-bottom: var(--space-3); border-radius: var(--radius); background: var(--accent-soft); }
.pic-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: var(--space-4); }
.pic-card { position: relative; border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; background: var(--surface); transition: all var(--dur-fast) var(--ease); }
.pic-card:hover { border-color: var(--accent); transform: translateY(-2px); }
.pic-card.picked { outline: 2px solid var(--accent); outline-offset: 2px; }
.pic-card img { width: 100%; height: 140px; object-fit: cover; display: block; cursor: pointer; }
.pic-body { padding: 8px 10px; display: flex; flex-direction: column; gap: 2px; }
.pic-body strong { font-size: var(--font-2); }
.pic-body span { font-size: 12px; }
.pic-ops { display: flex; gap: 2px; padding: 0 6px 8px; }
.pick-box { position: absolute; top: 8px; left: 8px; z-index: 2; padding: 2px 4px; border-radius: 4px; background: rgba(255, 255, 255, .9); }
.pager { display: flex; justify-content: flex-end; margin-top: var(--space-5); }
.member-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.member-list li { display: flex; align-items: center; gap: 14px; padding: 12px 14px; border: 1px solid var(--border); border-radius: var(--radius); }
.member-main { flex: 1 1 auto; min-width: 0; }
.member-main strong { display: block; }
.member-main span { font-size: 12px; }
.muted { color: var(--text-3); }
</style>
