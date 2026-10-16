<template>
  <div class="fav-page">
    <div class="section-head">
      <div>
        <p class="eyebrow">MY FAVORITES</p>
        <h2>我的收藏</h2>
        <p>用收藏夹把喜欢的作品归类整理</p>
      </div>
      <a-space>
        <a-button :type="selectMode ? 'default' : 'primary'" @click="toggleSelectMode">
          {{ selectMode ? '退出管理' : '批量管理' }}
        </a-button>
        <a-button @click="openFolderForm()">新建收藏夹</a-button>
      </a-space>
    </div>

    <div class="fav-layout">
      <!-- 收藏夹侧栏 -->
      <aside class="folders">
        <a-spin :spinning="foldersLoading">
          <ul class="folder-list">
            <li :class="{ on: activeFolder === 'ALL' }" @click="selectFolder('ALL')">
              <span class="folder-name">全部收藏</span>
              <em>{{ total }}</em>
            </li>
            <li :class="{ on: activeFolder === 'NONE' }" @click="selectFolder('NONE')">
              <span class="folder-name">未分组</span>
              <em>{{ ungroupedCount }}</em>
            </li>
            <li v-for="folder in folders" :key="folder.id"
                :class="{ on: activeFolder === String(folder.id) }" @click="selectFolder(String(folder.id))">
              <span class="folder-name" :title="folder.description || folder.name">{{ folder.name }}</span>
              <em>{{ folder.workCount ?? 0 }}</em>
              <span class="folder-ops">
                <a @click.stop="openFolderForm(folder)">改名</a>
                <a class="danger" @click.stop="removeFolder(folder)">删除</a>
              </span>
            </li>
          </ul>
          <a-empty v-if="!foldersLoading && !folders.length" description="还没有收藏夹" :image-style="{ height: '48px' }">
            <a-button size="small" type="primary" @click="openFolderForm()">新建收藏夹</a-button>
          </a-empty>
        </a-spin>
      </aside>

      <!-- 收藏内容 -->
      <section class="fav-main">
        <div v-if="selectMode" class="batch-bar">
          <a-checkbox :checked="allSelected" :indeterminate="picked.length > 0 && !allSelected" @change="toggleAll">
            全选本页（{{ works.length }}）
          </a-checkbox>
          <span class="muted">已选 {{ picked.length }} 件</span>
          <a-space>
            <a-dropdown :disabled="!picked.length">
              <a-button size="small" :disabled="!picked.length">移动到收藏夹 <DownOutlined /></a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item key="none" @click="moveTo(null)">移出分组（未分组）</a-menu-item>
                  <a-menu-item v-for="folder in folders" :key="folder.id" @click="moveTo(String(folder.id))">
                    {{ folder.name }}
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button size="small" danger :disabled="!picked.length" :loading="working" @click="cancelSelected">
              批量取消收藏
            </a-button>
          </a-space>
        </div>

        <a-spin :spinning="loading">
          <MasonryGrid v-if="works.length" :items="works">
            <template #default="{ item }">
              <div class="work-wrap" :class="{ picked: picked.includes(String(item.id)) }">
                <label v-if="selectMode" class="pick-box">
                  <input type="checkbox" :checked="picked.includes(String(item.id))" @change="togglePick(String(item.id))" />
                </label>
                <WorkCard :work="item" @open="onCardClick" />
                <div class="card-actions">
                  <a-button size="small" type="text" @click="openWork(item)">查看</a-button>
                  <a-button size="small" type="text" @click="copyLink(item)">复制链接</a-button>
                  <a-dropdown>
                    <a-button size="small" type="text">移动到</a-button>
                    <template #overlay>
                      <a-menu>
                        <a-menu-item key="none" @click="moveOne(item, null)">未分组</a-menu-item>
                        <a-menu-item v-for="folder in folders" :key="folder.id" @click="moveOne(item, String(folder.id))">
                          {{ folder.name }}
                        </a-menu-item>
                      </a-menu>
                    </template>
                  </a-dropdown>
                  <a-button size="small" type="text" danger @click="cancelOne(item)">取消收藏</a-button>
                </div>
              </div>
            </template>
          </MasonryGrid>

          <a-empty v-else-if="!loading" :description="emptyText">
            <a-button type="primary" @click="router.push('/community')">去社区逛逛</a-button>
          </a-empty>
        </a-spin>
      </section>
    </div>

    <!-- 收藏夹表单 -->
    <a-modal v-model:open="folderFormOpen" :title="folderForm.id ? '重命名收藏夹' : '新建收藏夹'" @ok="saveFolder">
      <a-form layout="vertical">
        <a-form-item label="名称" required>
          <a-input v-model:value="folderForm.name" placeholder="例如：城市夜景、人像灵感" :maxlength="64" />
        </a-form-item>
        <a-form-item label="简介">
          <a-textarea v-model:value="folderForm.description" :rows="2" placeholder="这个收藏夹用来收集什么？" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined } from '@ant-design/icons-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import {
  addFavoriteFolder, cancelFavorites, deleteFavoriteFolder, listFavoriteFolders, listFavoriteWorks,
  moveFavorites, updateFavoriteFolder,
} from '@/api/photoCommunity'

const router = useRouter()
const folders = ref<any[]>([])
const foldersLoading = ref(false)
const activeFolder = ref('ALL')
const works = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const ungroupedCount = ref(0)
const selectMode = ref(false)
const picked = ref<string[]>([])
const working = ref(false)
const folderFormOpen = ref(false)
const folderForm = ref<any>({ id: undefined, name: '', description: '' })

const emptyText = computed(() => (activeFolder.value === 'ALL' ? '还没有收藏任何作品' : '这个收藏夹还是空的'))
const allSelected = computed(() => works.value.length > 0 && picked.value.length === works.value.length)

async function loadFolders() {
  foldersLoading.value = true
  try {
    const res: any = await listFavoriteFolders()
    if (res.data.code === 0) {
      folders.value = res.data.data?.folders || []
    }
  } finally {
    foldersLoading.value = false
  }
}

async function loadWorks() {
  loading.value = true
  try {
    const folderId = activeFolder.value === 'NONE' ? null : activeFolder.value === 'ALL' ? undefined : activeFolder.value
    const res: any = await listFavoriteWorks({ folderId, current: 1, size: 60 })
    if (res.data.code === 0) {
      works.value = res.data.data?.records || []
      ungroupedCount.value = Number(res.data.data?.ungroupedCount || 0)
      total.value = activeFolder.value === 'ALL'
        ? folders.value.reduce((sum, f) => sum + Number(f.workCount || 0), 0) + ungroupedCount.value
        : Number(res.data.data?.total || 0)
    }
  } finally {
    loading.value = false
  }
}

async function selectFolder(key: string) {
  activeFolder.value = key
  picked.value = []
  await loadWorks()
}

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
  picked.value = allSelected.value ? [] : works.value.map((w) => String(w.id))
}

function onCardClick(work: any) {
  if (selectMode.value) togglePick(String(work.id))
  else openWork(work)
}

function openWork(work: any) {
  router.push('/works/' + work.id)
}

async function copyLink(work: any) {
  try {
    await navigator.clipboard.writeText(location.origin + '/works/' + work.id)
    message.success('已复制链接')
  } catch {
    message.warning('复制失败')
  }
}

function openFolderForm(folder?: any) {
  folderForm.value = folder
    ? { id: folder.id, name: folder.name, description: folder.description }
    : { id: undefined, name: '', description: '' }
  folderFormOpen.value = true
}

async function saveFolder() {
  if (!folderForm.value.name?.trim()) return message.warning('请填写收藏夹名称')
  const res: any = folderForm.value.id
    ? await updateFavoriteFolder({ id: folderForm.value.id, name: folderForm.value.name, description: folderForm.value.description })
    : await addFavoriteFolder({ name: folderForm.value.name, description: folderForm.value.description })
  if (res.data.code === 0) {
    message.success(folderForm.value.id ? '已保存' : '收藏夹已创建')
    folderFormOpen.value = false
    await loadFolders()
    if (activeFolder.value !== 'ALL' && activeFolder.value !== 'NONE') await loadWorks()
  } else {
    message.error(res.data.message || '保存失败')
  }
}

function removeFolder(folder: any) {
  Modal.confirm({
    title: '删除收藏夹「' + folder.name + '」',
    content: '夹内作品会回到「未分组」，收藏关系不会丢失。',
    okText: '确认删除',
    okType: 'danger',
    async onOk() {
      const res: any = await deleteFavoriteFolder(folder.id)
      if (res.data.code === 0) {
        message.success('已删除')
        if (activeFolder.value === String(folder.id)) activeFolder.value = 'ALL'
        await loadFolders()
        await loadWorks()
      } else {
        message.error(res.data.message || '删除失败')
      }
    },
  })
}

async function moveTo(folderId: string | null) {
  if (!picked.value.length) return
  working.value = true
  try {
    const res: any = await moveFavorites({ workIds: [...picked.value], folderId })
    if (res.data.code === 0) {
      message.success('已移动 ' + picked.value.length + ' 件作品')
      picked.value = []
      await loadFolders()
      await loadWorks()
    } else {
      message.error(res.data.message || '移动失败')
    }
  } finally {
    working.value = false
  }
}

async function moveOne(work: any, folderId: string | null) {
  const res: any = await moveFavorites({ workIds: [String(work.id)], folderId })
  if (res.data.code === 0) {
    message.success('已移动')
    await loadFolders()
    await loadWorks()
  } else {
    message.error(res.data.message || '移动失败')
  }
}

async function cancelSelected() {
  working.value = true
  try {
    const res: any = await cancelFavorites([...picked.value])
    if (res.data.code === 0) {
      message.success('已取消收藏 ' + Number(res.data.data || picked.value.length) + ' 件')
      picked.value = []
      await loadFolders()
      await loadWorks()
    } else {
      message.error(res.data.message || '操作失败')
    }
  } finally {
    working.value = false
  }
}

function cancelOne(work: any) {
  Modal.confirm({
    title: '取消收藏',
    content: '取消后该作品将从收藏中移除。',
    okText: '确认取消',
    okType: 'danger',
    async onOk() {
      const res: any = await cancelFavorites([String(work.id)])
      if (res.data.code === 0) {
        message.success('已取消收藏')
        await loadFolders()
        await loadWorks()
      } else {
        message.error(res.data.message || '操作失败')
      }
    },
  })
}

onMounted(async () => {
  await loadFolders()
  await loadWorks()
})
</script>

<style scoped>
.fav-layout { display: grid; grid-template-columns: 220px minmax(0, 1fr); gap: var(--space-5); margin-top: var(--space-4); }
.folders { border-right: 1px solid var(--border); padding-right: var(--space-4); }
.folder-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 4px; }
.folder-list li { position: relative; display: flex; align-items: center; justify-content: space-between; gap: 8px; padding: 8px 10px; border-radius: var(--radius-sm); cursor: pointer; transition: background var(--dur-fast) var(--ease); }
.folder-list li:hover { background: var(--surface-2); }
.folder-list li.on { background: var(--accent-soft); }
.folder-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--font-2); }
.folder-list em { font-style: normal; color: var(--text-3); font-size: 12px; }
.folder-ops { display: none; gap: 6px; font-size: 12px; }
.folder-list li:hover .folder-ops { display: inline-flex; }
.folder-ops a { color: var(--text-2); }
.folder-ops a.danger { color: var(--danger, #c4564a); }
.batch-bar { display: flex; align-items: center; gap: 16px; padding: 10px 14px; margin-bottom: var(--space-3); border-radius: var(--radius); background: var(--accent-soft); }
.work-wrap { position: relative; }
.work-wrap.picked { outline: 2px solid var(--accent); outline-offset: 3px; border-radius: var(--radius); }
.pick-box { position: absolute; top: 8px; left: 8px; z-index: 2; padding: 2px 4px; border-radius: 4px; background: rgba(255, 255, 255, .9); }
.card-actions { display: flex; flex-wrap: wrap; gap: 2px; margin-top: 6px; }
.muted { color: var(--text-3); }

@media (max-width: 900px) {
  .fav-layout { grid-template-columns: 1fr; }
  .folders { border-right: none; border-bottom: 1px solid var(--border); padding: 0 0 var(--space-3); }
}
</style>
