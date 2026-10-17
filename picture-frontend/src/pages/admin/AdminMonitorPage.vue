<template>
  <div class="monitor-page">
    <div class="page-title">
      <div>
        <span>SYSTEM / OBSERVABILITY</span>
        <h1>系统监控</h1>
        <p>接口调用量、耗时与错误率（按小时聚合，仅 system:monitor 权限可见）</p>
      </div>
      <a-space>
        <a-radio-group v-model:value="hours" button-style="solid" size="small" @change="load">
          <a-radio-button :value="1">1 小时</a-radio-button>
          <a-radio-button :value="24">24 小时</a-radio-button>
          <a-radio-button :value="168">7 天</a-radio-button>
        </a-radio-group>
        <a-button :loading="loading" @click="load">刷新</a-button>
      </a-space>
    </div>

    <a-spin :spinning="loading">
      <div class="metrics">
        <div class="metric surface-card"><span>总请求</span><strong>{{ data.totalRequests ?? 0 }}</strong><em>近 {{ hours }} 小时</em></div>
        <div class="metric surface-card"><span>平均耗时</span><strong>{{ data.avgCostMs ?? 0 }}<i>ms</i></strong><em>全部接口</em></div>
        <div class="metric surface-card"><span>最大耗时</span><strong>{{ data.maxCostMs ?? 0 }}<i>ms</i></strong><em>单次峰值</em></div>
        <div class="metric surface-card"><span>错误率</span><strong>{{ data.errorRate ?? 0 }}<i>%</i></strong><em>4xx / 5xx</em></div>
        <div class="metric surface-card"><span>覆盖接口</span><strong>{{ (data.summary || []).length }}</strong><em>Top 明细</em></div>
        <div class="metric surface-card"><span>待落库</span><strong>{{ data.pendingKeys ?? 0 }}</strong><em>内存缓冲</em></div>
      </div>

      <a-card :bordered="false" class="panel" title="调用量趋势">
        <div ref="chartRef" class="chart" />
      </a-card>

      <a-card :bordered="false" class="panel" title="接口明细（按调用量倒序）">
        <a-table :data-source="data.summary || []" :columns="columns" :pagination="{ pageSize: 12 }" row-key="uri" size="small">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'uri'">
              <span class="method" :class="record.method.toLowerCase()">{{ record.method }}</span>
              <code>{{ record.uri }}</code>
            </template>
            <template v-else-if="column.dataIndex === 'avg'">
              {{ avgOf(record) }} ms
            </template>
            <template v-else-if="column.dataIndex === 'errorCount'">
              <a-tag :color="Number(record.errorCount) > 0 ? 'red' : 'green'">{{ record.errorCount }}</a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'costBar'">
              <div class="bar-wrap"><div class="bar" :style="{ width: barWidth(record) }" /></div>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import { getAdminMetrics } from '@/api/adminConsole'

const loading = ref(false)
const hours = ref(24)
const data = ref<Record<string, any>>({})
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const columns = [
  { title: '接口', dataIndex: 'uri', ellipsis: true },
  { title: '调用次数', dataIndex: 'requestCount', width: 110, sorter: (a: any, b: any) => a.requestCount - b.requestCount },
  { title: '平均耗时', dataIndex: 'avg', width: 110 },
  { title: '最大耗时', dataIndex: 'maxCostMs', width: 110 },
  { title: '错误', dataIndex: 'errorCount', width: 90 },
  { title: '耗时分布', dataIndex: 'costBar', width: 160 },
]

function avgOf(record: any) {
  const count = Number(record.requestCount || 0)
  return count === 0 ? 0 : Math.round(Number(record.totalCostMs || 0) / count)
}

const maxAvg = computed(() => Math.max(1, ...(data.value.summary || []).map((r: any) => avgOf(r))))

function barWidth(record: any) {
  return Math.max(4, Math.round((avgOf(record) / maxAvg.value) * 100)) + '%'
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const hourly = data.value.hourly || []
  chart.setOption({
    grid: { left: 48, right: 24, top: 30, bottom: 40 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: hourly.map((h: any) => String(h.bucketHour).slice(5, 16).replace('T', ' ')),
      axisLine: { lineStyle: { color: '#E6E6E1' } },
      axisLabel: { color: 'var(--text-3)' },
    },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#F1F1EE' } }, axisLabel: { color: 'var(--text-3)' } },
    series: [
      {
        name: '调用次数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: hourly.map((h: any) => Number(h.requestCount || 0)),
        itemStyle: { color: 'var(--accent)' },
        areaStyle: { color: 'rgba(124,139,127,.14)' },
      },
      {
        name: '错误次数',
        type: 'line',
        smooth: true,
        symbolSize: 6,
        data: hourly.map((h: any) => Number(h.errorCount || 0)),
        itemStyle: { color: '#B4746A' },
      },
    ],
  })
}

async function load() {
  loading.value = true
  try {
    const res: any = await getAdminMetrics(hours.value)
    if (res.data.code !== 0) {
      message.error(res.data.message || '加载监控数据失败')
      return
    }
    data.value = res.data.data || {}
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function onResize() { chart?.resize() }

onMounted(() => {
  void load()
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
watch(hours, () => void load())
</script>

<style scoped>
.monitor-page { display: flex; flex-direction: column; }
.page-title { display: flex; align-items: flex-end; justify-content: space-between; margin-bottom: 20px; }
.page-title span { color: var(--text-3); font-size: 12px; letter-spacing: .18em; }
.page-title h1 { margin: 4px 0; font-size: var(--font-6); }
.page-title p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.metrics { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.metric { display: flex; flex-direction: column; gap: 2px; padding: 14px 16px; }
.metric span { color: var(--text-3); font-size: var(--font-1); }
.metric strong { font-size: var(--font-6); line-height: 1.2; }
.metric strong i { font-size: var(--font-2); font-style: normal; color: var(--text-3); margin-left: 2px; }
.metric em { color: var(--text-3); font-size: 12px; font-style: normal; }
.panel { border-radius: var(--radius-lg); box-shadow: var(--shadow-1); margin-bottom: 16px; }
.chart { height: 260px; }
.method { display: inline-block; min-width: 46px; margin-right: 8px; padding: 1px 6px; border-radius: 4px; font-size: 12px; text-align: center; background: var(--surface-3); color: var(--text-2); }
.method.post { background: #E9EFE9; color: var(--accent-ink); }
.method.get { background: #EFF1F4; color: #5A6472; }
.bar-wrap { height: 6px; background: var(--surface-2); border-radius: 3px; overflow: hidden; }
.bar { height: 100%; background: var(--accent); border-radius: 3px; }
@media (max-width: 1200px) { .metrics { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
</style>
