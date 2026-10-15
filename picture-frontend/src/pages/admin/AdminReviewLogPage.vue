<template>
  <div class="log-page">
    <div class="page-title">
      <div>
        <span>OPERATIONS / AUDIT TRAIL</span>
        <h1>审核流水</h1>
        <p>每一次审核动作的完整记录：谁在何时审了什么、改前改后、依据是什么</p>
      </div>
      <a-space>
        <a-select v-model:value="targetType" style="width: 140px" @change="reload">
          <a-select-option value="">全部类型</a-select-option>
          <a-select-option value="picture">图片</a-select-option>
          <a-select-option value="work">作品</a-select-option>
          <a-select-option value="comment">评论</a-select-option>
        </a-select>
        <a-input v-model:value="targetId" placeholder="目标 ID（可选）" allow-clear style="width: 160px" @press-enter="reload" />
        <a-button :loading="loading" @click="load">刷新</a-button>
      </a-space>
    </div>

    <a-card :bordered="false" class="panel">
      <a-table :data-source="list" :columns="columns" :loading="loading" row-key="id" size="middle"
               :pagination="{ current: page, pageSize: 20, total, onChange: onChange }">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'target'">
            <a-tag>{{ typeText(record.targetType) }}</a-tag>
            <code>#{{ record.targetId }}</code>
          </template>
          <template v-else-if="column.dataIndex === 'action'">
            <a-tag :color="actionColor(record.action)">{{ actionText(record.action) }}</a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'change'">
            <span class="muted">{{ fmt(record.fromStatus) }}</span>
            <span class="arrow">→</span>
            <strong>{{ fmt(record.toStatus) }}</strong>
          </template>
          <template v-else-if="column.dataIndex === 'operatorName'">
            <a-tag :color="record.operatorType === 'SYSTEM' ? 'blue' : 'default'">{{ record.operatorName }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { listReviewLogs } from '@/api/adminConsole'

const list = ref<any[]>([])
const loading = ref(false)
const targetType = ref('')
const targetId = ref('')
const page = ref(1)
const total = ref(0)

const columns = [
  { title: '时间', dataIndex: 'createTime', width: 190 },
  { title: '对象', dataIndex: 'target', width: 160 },
  { title: '动作', dataIndex: 'action', width: 140 },
  { title: '状态变化', dataIndex: 'change', width: 170 },
  { title: '操作人', dataIndex: 'operatorName', width: 160 },
  { title: '依据 / 理由', dataIndex: 'reason', ellipsis: true },
]

const typeMap: Record<string, string> = { picture: '图片', work: '作品', comment: '评论' }
const actionMap: Record<string, string> = {
  AUTO_PASS: '机审通过', MANUAL_PASS: '人工通过', REJECT: '拒绝', BAN: '封禁', RESTORE: '恢复',
  APPEAL_ACCEPT: '申诉受理', AUTO_HIDE: '机审隐藏', HIDE: '隐藏', UNHIDE: '恢复显示', SUBMIT_REVIEW: '提交审核',
}
function typeText(t: string) { return typeMap[t] || t }
function actionText(a: string) { return actionMap[a] || a }
function actionColor(a: string) {
  if (a.includes('PASS')) return 'green'
  if (a.includes('REJECT') || a.includes('HIDE') || a === 'BAN') return 'red'
  return 'default'
}
function fmt(v: any) {
  if (v === null || v === undefined || v === '') return '—'
  return ({ 0: '待审', 1: '通过', 2: '拒绝', VISIBLE: '正常', HIDDEN: '隐藏' } as any)[v] ?? String(v)
}

async function load() {
  loading.value = true
  try {
    const res: any = await listReviewLogs({
      targetType: targetType.value || undefined,
      targetId: targetId.value ? targetId.value.trim() : undefined,
      current: page.value,
      size: 20,
    })
    if (res.data.code === 0) {
      list.value = res.data.data?.records || []
      total.value = Number(res.data.data?.total || 0)
    } else {
      message.error(res.data.message || '加载失败')
    }
  } finally {
    loading.value = false
  }
}

function reload() { page.value = 1; void load() }
function onChange(p: any) { page.value = p.current; void load() }

onMounted(load)
</script>

<style scoped>
.page-title { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title span { color: var(--text-3); font-size: 11px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); }
.arrow { margin: 0 6px; color: var(--text-3); }
</style>
