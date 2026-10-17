<template>
  <div class="auto-page">
    <div class="page-title">
      <div>
        <span>OPERATIONS / AUTO REVIEW</span>
        <h1>自动审核（机审）</h1>
        <p>规则命中的高危内容才转人工；低风险内容机审直接放行，并可一键清理积压</p>
      </div>
      <a-space>
        <a-button :loading="loading" @click="load">刷新</a-button>
        <a-button type="primary" :loading="running" @click="runNow">一键机审积压</a-button>
      </a-space>
    </div>

    <div class="metrics">
      <div class="metric"><span>待人工审核</span><strong>{{ stats.pending ?? 0 }}</strong></div>
      <div class="metric"><span>已通过</span><strong>{{ stats.passed ?? 0 }}</strong></div>
      <div class="metric"><span>机审通过累计</span><strong>{{ stats.autoPassed ?? 0 }}</strong></div>
      <div class="metric"><span>已拒绝</span><strong>{{ stats.rejected ?? 0 }}</strong></div>
    </div>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="10">
        <a-card :bordered="false" class="panel" title="审核策略">
          <a-form layout="vertical">
            <a-form-item label="启用机审">
              <a-switch v-model:checked="form.enabled" />
              <span class="muted tip">关闭后所有上传都转人工</span>
            </a-form-item>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="最小宽度(px)"><a-input-number v-model:value="form.minWidth" :min="1" style="width:100%" /></a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="最小高度(px)"><a-input-number v-model:value="form.minHeight" :min="1" style="width:100%" /></a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="作者历史样本下限"><a-input-number v-model:value="form.minHistory" :min="0" style="width:100%" /></a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="历史通过率下限"><a-input-number v-model:value="form.minPassRate" :min="0" :max="1" :step="0.05" style="width:100%" /></a-form-item>
              </a-col>
            </a-row>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="突发窗口(分钟)"><a-input-number v-model:value="form.burstWindowMinutes" :min="1" style="width:100%" /></a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="窗口内上限(张)"><a-input-number v-model:value="form.burstLimit" :min="1" style="width:100%" /></a-form-item>
              </a-col>
            </a-row>
            <a-form-item label="风险词（逗号分隔）">
              <a-textarea v-model:value="form.bannedWords" :rows="3" />
            </a-form-item>
            <a-button type="primary" block :loading="saving" @click="save">保存策略</a-button>
          </a-form>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="14">
        <a-card :bordered="false" class="panel" title="最近一次机审明细">
          <template #extra><span class="muted">{{ lastRun.scanned ? ('扫描 ' + lastRun.scanned + ' 张 · 通过 ' + lastRun.autoPassed + ' · 转人工 ' + lastRun.toManual) : '尚未运行' }}</span></template>
          <a-table :data-source="lastRun.details || []" :columns="columns" :pagination="{ pageSize: 8 }" row-key="pictureId" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'status'">
                <a-tag :color="record.status === 1 ? 'green' : 'orange'">{{ record.status === 1 ? '机审通过' : '转人工' }}</a-tag>
              </template>
              <template v-else-if="column.dataIndex === 'rule'">
                <code>{{ record.rule }}</code>
              </template>
            </template>
          </a-table>
          <a-empty v-if="!(lastRun.details || []).length" :image="false" description="点右上「一键机审积压」开始处理待审图片" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getAutoReviewConfig, runAutoReview, saveAutoReviewConfig } from '@/api/adminConsole'

const loading = ref(false)
const saving = ref(false)
const running = ref(false)
const stats = ref<Record<string, any>>({})
const lastRun = ref<Record<string, any>>({})
const form = reactive({
  enabled: true,
  minWidth: 300,
  minHeight: 300,
  minHistory: 3,
  minPassRate: 0.8,
  burstWindowMinutes: 10,
  burstLimit: 15,
  bannedWords: '',
})

const columns = [
  { title: '图片', dataIndex: 'name', ellipsis: true },
  { title: '判定', dataIndex: 'status', width: 110 },
  { title: '命中规则', dataIndex: 'rule', width: 150 },
  { title: '理由', dataIndex: 'message', ellipsis: true },
]

async function load() {
  loading.value = true
  try {
    const res: any = await getAutoReviewConfig()
    if (res.data.code !== 0) {
      message.error(res.data.message || '加载失败')
      return
    }
    const data = res.data.data || {}
    stats.value = data
    form.enabled = String(data['autoReview.enabled']) === 'true'
    form.minWidth = Number(data['autoReview.minWidth'] ?? 300)
    form.minHeight = Number(data['autoReview.minHeight'] ?? 300)
    form.minHistory = Number(data['autoReview.minHistory'] ?? 3)
    form.minPassRate = Number(data['autoReview.minPassRate'] ?? 0.8)
    form.burstWindowMinutes = Number(data['autoReview.burstWindowMinutes'] ?? 10)
    form.burstLimit = Number(data['autoReview.burstLimit'] ?? 15)
    form.bannedWords = (data.bannedWordList || []).join(',')
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    const res: any = await saveAutoReviewConfig({
      'autoReview.enabled': form.enabled ? 'true' : 'false',
      'autoReview.minWidth': form.minWidth,
      'autoReview.minHeight': form.minHeight,
      'autoReview.minHistory': form.minHistory,
      'autoReview.minPassRate': form.minPassRate,
      'autoReview.burstWindowMinutes': form.burstWindowMinutes,
      'autoReview.burstLimit': form.burstLimit,
      'autoReview.bannedWords': form.bannedWords,
    })
    if (res.data.code === 0) {
      message.success('策略已保存并立即生效')
      await load()
    } else {
      message.error(res.data.message || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

async function runNow() {
  running.value = true
  try {
    const res: any = await runAutoReview(500)
    if (res.data.code === 0) {
      lastRun.value = res.data.data || {}
      message.success('机审完成：通过 ' + lastRun.value.autoPassed + ' 张，转人工 ' + lastRun.value.toManual + ' 张')
      await load()
    } else {
      message.error(res.data.message || '运行失败')
    }
  } finally {
    running.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.page-title { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title span { color: var(--text-3); font-size: 12px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.metric { display: flex; flex-direction: column; gap: 2px; padding: 14px 16px; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); }
.metric span { color: var(--text-3); font-size: var(--font-1); }
.metric strong { font-size: var(--font-6); }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); }
.tip { margin-left: 10px; font-size: 12px; }
</style>
