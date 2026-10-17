<template>
  <div class="review-page">
    <div class="page-title"><div><span>OPERATIONS / CONTENT</span><h1>内容审核</h1><p>审核图片，处理违规作品并保留操作记录。</p></div><a-button :loading="loading" @click="load">刷新</a-button></div>
    <div class="review-panel">
      <a-tabs v-model:activeKey="type" @change="switchType"><a-tab-pane key="picture" tab="图片审核" /><a-tab-pane key="work" tab="作品治理" /></a-tabs>
      <div class="toolbar">
        <a-select v-if="type === 'picture'" v-model:value="pictureStatus" style="width:160px" @change="reload"><a-select-option value="all">全部状态</a-select-option><a-select-option value="0">待审核</a-select-option><a-select-option value="1">已通过</a-select-option><a-select-option value="2">已拒绝</a-select-option></a-select>
        <a-select v-else v-model:value="workStatus" style="width:160px" @change="reload"><a-select-option value="all">全部作品</a-select-option><a-select-option value="PUBLISHED">已发布</a-select-option><a-select-option value="BANNED">已封禁</a-select-option><a-select-option value="OFFLINE">已下线</a-select-option></a-select>
        <a-input-search v-if="type === 'work'" v-model:value="keyword" placeholder="搜索作品标题" style="width:240px" @search="reload" />
        <span class="selected">已选择 {{ selectedKeys.length }} 项</span>
        <a-space v-if="selectedKeys.length">
          <a-button v-if="type === 'picture'" type="primary" :loading="submitting" @click="moderate('PASS')">批量通过</a-button>
          <a-button v-if="type === 'picture'" danger :loading="submitting" @click="openReason('REJECT')">批量拒绝</a-button>
          <a-button v-if="type === 'work'" danger :loading="submitting" @click="openReason('BAN')">批量封禁</a-button>
          <a-button v-if="type === 'work'" :loading="submitting" @click="moderate('RESTORE')">批量恢复</a-button>
          <a-button v-if="type === 'work'" type="primary" :loading="submitting" @click="moderate('PASS')">批量审核通过</a-button>
          <a-button v-if="type === 'work'" danger :loading="submitting" @click="openReason('REJECT')">批量审核拒绝</a-button>
        </a-space>
      </div>
      <a-table :columns="type === 'picture' ? pictureColumns : workColumns" :data-source="records" :loading="loading" row-key="id" :row-selection="rowSelection" :pagination="pagination" :scroll="{ x: 800 }" @change="onPageChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'preview'"><img :src="record.thumbnailUrl || record.url" class="preview" alt="" /></template>
          <template v-else-if="column.key === 'pictureStatus'"><a-tag :color="pictureColor(record.reviewStatus)">{{ pictureLabel(record.reviewStatus) }}</a-tag></template>
          <template v-else-if="column.key === 'workReview'"><a-tag :color="record.reviewStatus === 1 ? 'green' : record.reviewStatus === 2 ? 'red' : 'orange'">{{ record.reviewStatus === 1 ? '已通过' : record.reviewStatus === 2 ? '已拒绝' : '待审核' }}</a-tag></template>
                    <template v-else-if="column.key === 'workStatus'"><a-tag :color="record.status === 'BANNED' ? 'red' : record.status === 'PUBLISHED' ? 'green' : 'default'">{{ workLabel(record.status) }}</a-tag></template>
          <template v-else-if="column.key === 'date'">{{ formatDate(record.createTime) }}</template>
          <template v-else-if="column.key === 'actions'"><a-space><a-button v-if="type === 'picture' && record.reviewStatus !== 1" size="small" type="link" @click="moderate('PASS', [record.id])">通过</a-button><a-button v-if="type === 'picture' && record.reviewStatus !== 2" size="small" type="link" danger @click="openReason('REJECT', [record.id])">拒绝</a-button><a-button v-if="type === 'work' && record.status === 'PUBLISHED'" size="small" type="link" danger @click="openReason('BAN', [record.id])">封禁</a-button><a-button v-if="type === 'work' && record.status === 'BANNED'" size="small" type="link" @click="moderate('RESTORE', [record.id])">恢复</a-button></a-space></template>
        </template>
      </a-table>
    </div>
    <a-modal v-model:open="reasonOpen" :title="pendingAction === 'BAN' ? '封禁作品' : '拒绝图片'" ok-text="确认处理" :confirm-loading="submitting" @ok="confirmReason"><p>请填写处理原因。操作会写入管理日志。</p><a-textarea v-model:value="reason" :rows="3" maxlength="200" placeholder="请输入原因" /></a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listAdminPictures, listAdminWorks, moderateContent } from '@/api/adminConsole'
type Action = 'PASS' | 'REJECT' | 'BAN' | 'RESTORE'
const type = ref<'picture' | 'work'>('picture'); const pictureStatus = ref('0'); const workStatus = ref('PUBLISHED'); const keyword = ref('')
const loading = ref(false); const submitting = ref(false); const records = ref<any[]>([]); const selectedKeys = ref<(string | number)[]>([]); const page = ref(1); const total = ref(0)
const reasonOpen = ref(false); const reason = ref(''); const pendingAction = ref<Action>('REJECT'); const pendingIds = ref<(string | number)[]>([])
const pictureColumns = [{ title: '预览', key: 'preview', width: 90 }, { title: '图片名称', dataIndex: 'name', ellipsis: true }, { title: '分类', dataIndex: 'category', width: 115 }, { title: '上传者 ID', dataIndex: 'userId', width: 190 }, { title: '状态', key: 'pictureStatus', width: 100 }, { title: '上传时间', key: 'date', width: 150 }, { title: '操作', key: 'actions', width: 130 }]
const workColumns = [{ title: '作品标题', dataIndex: 'title', ellipsis: true }, { title: '审核', key: 'workReview', width: 96 }, { title: '作者 ID', dataIndex: 'authorId', width: 200 }, { title: '可见范围', dataIndex: 'visibility', width: 120 }, { title: '状态', key: 'workStatus', width: 100 }, { title: '发布时间', key: 'date', width: 150 }, { title: '操作', key: 'actions', width: 100 }]
const pagination = computed(() => ({ current: page.value, pageSize: 20, total: total.value, showSizeChanger: false, showTotal: (n: number) => `共 ${n} 项` }))
const rowSelection = computed(() => ({ selectedRowKeys: selectedKeys.value, onChange: (keys: (string | number)[]) => { selectedKeys.value = keys } }))
function pictureColor(n: number) { return n === 0 ? 'orange' : n === 1 ? 'green' : 'red' }
function pictureLabel(n: number) { return n === 0 ? '待审核' : n === 1 ? '已通过' : '已拒绝' }
function workLabel(s: string) { return ({ PUBLISHED: '已发布', BANNED: '已封禁', OFFLINE: '已下线', DRAFT: '草稿' } as Record<string,string>)[s] || s }
function formatDate(value: string) { return value ? new Date(value).toLocaleDateString('zh-CN') : '' }
function switchType() { page.value = 1; selectedKeys.value = []; void load() }
function reload() { page.value = 1; selectedKeys.value = []; void load() }
function onPageChange(p: any) { page.value = p.current; selectedKeys.value = []; void load() }
async function load() { loading.value = true; try { const res: any = type.value === 'picture' ? await listAdminPictures({ current: page.value, size: 20, reviewStatus: pictureStatus.value === 'all' ? undefined : Number(pictureStatus.value) }) : await listAdminWorks({ current: page.value, size: 20, status: workStatus.value === 'all' ? undefined : workStatus.value, keyword: keyword.value || undefined }); if (res.data.code !== 0) throw new Error(res.data.message); records.value = res.data.data?.records || []; total.value = Number(res.data.data?.total || 0) } catch (e: any) { message.error(e?.message || '加载失败') } finally { loading.value = false } }
function openReason(action: Action, ids = selectedKeys.value) { pendingAction.value = action; pendingIds.value = [...ids]; reason.value = ''; reasonOpen.value = true }
async function confirmReason() { if (!reason.value.trim()) return message.warning('请填写处理原因'); await moderate(pendingAction.value, pendingIds.value, reason.value.trim()); reasonOpen.value = false }
async function moderate(action: Action, ids = selectedKeys.value, reasonText?: string) { if (!ids.length) return; submitting.value = true; try { const res: any = await moderateContent({ type: type.value, action, ids, reason: reasonText }); if (res.data.code !== 0) throw new Error(res.data.message); message.success(`已处理 ${res.data.data} 项`); selectedKeys.value = []; await load() } catch (e: any) { message.error(e?.message || '处理失败') } finally { submitting.value = false } }
onMounted(load)
</script>

<style scoped>
.page-title { display:flex; align-items:end; justify-content:space-between; margin-bottom:24px; }.page-title span { color:var(--text-3); font-size:11px; letter-spacing:.14em; }.page-title h1 { margin:5px 0; font-size:26px; color:var(--text-1); }.page-title p { margin:0; color:var(--text-3); font-size:13px; }
.review-panel { padding:18px 22px 22px; background:#fff; border:1px solid #e7ebe7; border-radius:8px; }.toolbar { display:flex; align-items:center; gap:12px; flex-wrap:wrap; margin:4px 0 18px; }.selected { margin-left:auto; color:#98a299; font-size:12px; }.preview { width:54px; height:44px; object-fit:cover; background:#e8ebe8; border-radius:3px; }
</style>
