<template>
  <div class="admin-cosmos">
    <a-page-header class="cosmos-header" title="星环画廊管理" sub-title="着陆页 3D 星环展示的 12 幅作品（hot 前 12 件已发布作品）">
      <template #extra>
        <a-button type="primary" @click="openAdd">
          <PlusOutlined /> 新增作品
        </a-button>
        <a-button @click="load" :loading="loading">刷新</a-button>
      </template>
    </a-page-header>

    <a-alert
      class="tip"
      type="info"
      show-icon
      message="星环成员 = 按热度排序的前 12 件已发布作品"
      description="更换图片后新图立即生效于着陆页 3D 星环与全屏背景；下架的作品会自动退出星环。"
    />

    <a-spin :spinning="loading">
      <div class="ring-grid">
        <article v-for="(w, i) in ringWorks" :key="w.id" class="ring-card">
          <div class="rank">{{ i + 1 }}</div>
          <div class="cover">
            <img v-if="coverOf(w)" :src="coverOf(w)" :alt="w.title" />
            <div v-else class="no-cover"><PictureOutlined /></div>
          </div>
          <div class="meta">
            <strong :title="w.title">{{ w.title || '未命名作品' }}</strong>
            <span>{{ w.author?.userName || '摄影师' }} · {{ w.pictures?.length || 0 }} 图</span>
          </div>
          <div class="ops">
            <a-upload
              :show-upload-list="false"
              :before-upload="(file: any) => { replaceFile = file; replaceTarget = w; replaceOpen = true; return false }"
            >
              <a-button size="small"><SwapOutlined /> 更换图片</a-button>
            </a-upload>
            <a-popconfirm title="下架后该作品将退出星环，确定？" @confirm="offlineWork(w)">
              <a-button size="small" danger><StopOutlined /> 下架</a-button>
            </a-popconfirm>
          </div>
        </article>
      </div>
      <a-empty v-if="!loading && !ringWorks.length" description="还没有已发布作品——先新增一件" />
    </a-spin>

    <!-- 更换图片确认 -->
    <a-modal v-model:open="replaceOpen" title="更换星环图片" :confirm-loading="busy" @ok="doReplace">
      <p v-if="replaceTarget">将为「{{ replaceTarget.title }}」更换展示图片（原图保留，作品改指向新图）。</p>
      <p v-if="replaceFile" class="pick-name">已选择：{{ replaceFile.name }}</p>
    </a-modal>

    <!-- 新增作品 -->
    <a-modal v-model:open="addOpen" title="新增星环作品" :confirm-loading="busy" @ok="doAdd">
      <a-form layout="vertical">
        <a-form-item label="标题" required>
          <a-input v-model:value="addForm.title" placeholder="例如：晨光与窗" :maxlength="64" show-count />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea v-model:value="addForm.description" placeholder="一句话介绍这张照片" :rows="2" />
        </a-form-item>
        <a-form-item label="图片" required>
          <input type="file" accept="image/*" @change="(e: any) => (addFile = e.target.files?.[0] ?? null)" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { PictureOutlined, PlusOutlined, StopOutlined, SwapOutlined } from '@ant-design/icons-vue'
import { listWorks, saveWork, publishWork } from '@/api/photoCommunity'
import { uploadPictureUsingPost } from '@/api/pictureController'

const loading = ref(false)
const busy = ref(false)
const ringWorks = ref<any[]>([])

const replaceOpen = ref(false)
const replaceTarget = ref<any>(null)
const replaceFile = ref<File | null>(null)

const addOpen = ref(false)
const addFile = ref<File | null>(null)
const addForm = ref({ title: '', description: '' })

function coverOf(w: any) {
  const p = w.pictures?.[0]
  return w.coverUrl || p?.thumbnailUrl || p?.url || ''
}

async function load() {
  loading.value = true
  try {
    const res: any = await listWorks({ current: 1, size: 12, sort: 'hot' })
    if (res.data.code === 0) ringWorks.value = res.data.data?.records || []
  } finally {
    loading.value = false
  }
}


async function doReplace() {
  if (!replaceTarget.value || !replaceFile.value) return
  busy.value = true
  try {
    const w = replaceTarget.value
    let picId: any = null
    try {
      const res: any = await uploadPictureUsingPost({ picName: w.title }, {}, replaceFile.value)
      if (res.data.code !== 0) throw new Error(res.data.message || '上传失败')
      picId = res.data.data?.id
    } catch (e: any) {
      message.error(e?.message?.slice(0, 60) || '图片上传失败')
      return
    }
    const res: any = await saveWork({
      id: w.id,
      title: w.title,
      description: w.description || '',
      visibility: w.visibility || 'PUBLIC',
      downloadPolicy: w.downloadPolicy || 'QUOTA_DOWNLOAD',
      pictureIds: [picId],
    })
    if (res.data.code === 0) {
      message.success('已更换星环图片')
      replaceOpen.value = false
      replaceFile.value = null
      await load()
    } else {
      message.error(res.data.message || '更换失败')
    }
  } finally {
    busy.value = false
  }
}

function openAdd() {
  addForm.value = { title: '', description: '' }
  addFile.value = null
  addOpen.value = true
}

async function doAdd() {
  if (!addForm.value.title.trim() || !addFile.value) { message.warning('标题和图片必填'); return }
  busy.value = true
  try {
    let picId: any = null
    try {
      const res: any = await uploadPictureUsingPost({ picName: addForm.value.title }, {}, addFile.value)
      if (res.data.code !== 0) throw new Error(res.data.message || '上传失败')
      picId = res.data.data?.id
    } catch (e: any) {
      message.error(e?.message?.slice(0, 60) || '图片上传失败')
      return
    }
    const res: any = await saveWork({
      title: addForm.value.title,
      description: addForm.value.description || '',
      visibility: 'PUBLIC',
      downloadPolicy: 'QUOTA_DOWNLOAD',
      pictureIds: [picId],
    })
    if (res.data.code === 0) {
      const pub: any = await publishWork({ workId: res.data.data, publish: true })
      if (pub.data.code === 0) {
        message.success('已发布并进入星环')
        addOpen.value = false
        await load()
      } else {
        message.error('发布失败')
      }
    } else {
      message.error(res.data.message || '保存失败')
    }
  } finally {
    busy.value = false
  }
}

async function offlineWork(w: any) {
  const res: any = await publishWork({ workId: w.id, publish: false })
  if (res.data.code === 0) {
    message.success('已下架')
    await load()
  } else {
    message.error('下架失败')
  }
}

onMounted(load)
</script>

<style scoped>
.admin-cosmos { padding: 8px 4px 32px; }
.cosmos-header { padding-left: 0; }
.tip { margin-bottom: 20px; }
.ring-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; }
.ring-card { position: relative; overflow: hidden; background: #fff; border: 1px solid var(--border, #e6e6e1); border-radius: 10px; }
.rank { position: absolute; top: 10px; left: 10px; z-index: 2; width: 26px; height: 26px; display: grid; place-items: center; border-radius: 50%; background: rgba(15, 20, 17, .72); color: #dfe7e0; font-size: 12px; font-family: Georgia, serif; }
.cover { height: 150px; background: #f1f1ee; }
.cover img { width: 100%; height: 100%; object-fit: cover; display: block; }
.no-cover { display: grid; place-items: center; height: 100%; color: #b6b6b0; font-size: 26px; }
.meta { padding: 12px 14px 8px; display: flex; flex-direction: column; gap: 3px; }
.meta strong { font-size: 14px; color: #1c1c1a; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meta span { font-size: 12px; color: #8e8e88; }
.ops { display: flex; gap: 8px; padding: 8px 14px 14px; }
.pick-name { color: #4f5f53; font-size: 13px; }
@media (max-width: 1100px) { .ring-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .ring-grid { grid-template-columns: 1fr; } }
</style>
