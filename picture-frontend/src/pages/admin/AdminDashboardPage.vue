<template>
  <div class="dashboard">
    <div class="page-title"><div><span>OPERATIONS / OVERVIEW</span><h1>数据看板</h1><p>平台内容与社区运营概况</p></div><a-button :loading="loading" @click="load">刷新数据</a-button></div>

    <!-- KPI 卡：衬线大数字 + 600ms 滚动 + 同比箭头 + 近 7 日 sparkline（P2-6） -->
    <div class="metrics">
      <div v-for="item in metrics" :key="item.key" class="metric surface-card">
        <span class="kpi-label">{{ item.label }}</span>
        <strong class="kpi-num">{{ kpiDisplay[item.key]?.value ?? overview[item.key] ?? '—' }}</strong>
        <div class="kpi-foot">
          <small class="kpi-caption">
            <template v-if="deltas[item.key]">
              <i class="delta" :class="deltas[item.key]!.dir">{{ deltas[item.key]!.dir === 'up' ? '↑' : '↓' }}{{ deltas[item.key]!.pct }}%</i>
            </template>
            {{ item.caption }}
          </small>
          <svg v-if="sparks[item.key]" class="spark" viewBox="0 0 60 20" width="60" height="20" aria-hidden="true">
            <polyline :points="sparks[item.key]" fill="none" stroke="#5F6E63" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <!-- 趋势图：迁移到 ECharts（与空间分析统一语言），hover tooltip + 可点击图例 + 面积渐变 -->
      <section class="panel trend surface-card">
        <div class="panel-heading"><div><h2>近 7 日增长</h2><p>每日新增用户、图片与作品</p></div></div>
        <v-chart class="trend-chart" :option="trendOption" autoresize :loading="loading" />
      </section>
      <section class="panel attention surface-card">
        <div class="panel-heading"><div><h2>待处理事项</h2><p>需要管理员关注的内容</p></div></div><router-link to="/admin/review"><span>待审核图片</span><strong>{{ overview.pendingPictures ?? 0 }}</strong><b>→</b></router-link><router-link to="/admin/review"><span>已封禁作品</span><strong>{{ overview.bannedWorks ?? 0 }}</strong><b>→</b></router-link><router-link to="/admin/users"><span>已封禁账号</span><strong>{{ overview.bannedUsers ?? 0 }}</strong><b>→</b></router-link></section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import VChart from 'vue-echarts'
import 'echarts'
import * as echarts from 'echarts'
import { getAdminOverview, getAdminTrend } from '@/api/adminConsole'
import { useCountUp } from '@/composables/useCountUp'

const loading = ref(false); const overview = ref<Record<string, number>>({}); const trend = ref<any[]>([])
const metrics = [
  { key: 'users', label: '总用户', caption: '今日新增', series: 'users' },
  { key: 'pictures', label: '图库图片', caption: '站内资产总量', series: 'pictures' },
  { key: 'works', label: '公开作品', caption: '社区可见作品', series: 'works' },
  { key: 'newUsersToday', label: '今日新用户', caption: '截至当前', series: 'users' },
  { key: 'newPicturesToday', label: '今日新图片', caption: '截至当前', series: 'pictures' },
  { key: 'pendingPictures', label: '待审核', caption: '等待处理的图片', series: null },
] as const

/** KPI 数字滚动（600ms，useCountUp 复用自点赞计数；reduced-motion 直跳） */
const kpiDisplay: Record<string, ReturnType<typeof useCountUp>> = {}
for (const m of metrics) {
  kpiDisplay[m.key] = useCountUp(computed(() => Number(overview.value[m.key]) || 0), 600)
}

/** 同比箭头：今日 vs 昨日（对应 series 的日增量） */
const deltas = computed<Record<string, { dir: 'up' | 'down'; pct: number } | undefined>>(() => {
  const out: Record<string, { dir: 'up' | 'down'; pct: number } | undefined> = {}
  const t = trend.value
  if (t.length < 2) return out
  for (const m of metrics) {
    if (!m.series) continue
    const today = Number(t[t.length - 1][m.series] || 0)
    const yest = Number(t[t.length - 2][m.series] || 0)
    if (!yest && !today) { out[m.key] = undefined; continue }
    out[m.key] = { dir: today >= yest ? 'up' : 'down', pct: yest ? Math.min(999, Math.round(Math.abs(today - yest) / yest * 100)) : 100 }
  }
  return out
})

/** 60×20 sparkline：近 7 日折线（缺失 series 自动省略） */
const sparks = computed<Record<string, string | undefined>>(() => {
  const out: Record<string, string | undefined> = {}
  const t = trend.value
  if (t.length < 2) return out
  for (const m of metrics) {
    if (!m.series) continue
    const series = t.map((d) => Number(d[m.series] || 0))
    const max = Math.max(1, ...series)
    out[m.key] = series
      .map((v, i) => `${(i / (series.length - 1)) * 60},${20 - (v / max) * 17 - 1.5}`)
      .join(' ')
  }
  return out
})

/** 趋势图：三系面积线，配色 --accent-strong / --accent / --warning（accent-soft 过浅不做线条） */
const SERIES_COLORS: Record<string, string> = { users: '#5F6E63', pictures: '#7C8B7F', works: '#C2A878' }
function areaColor(hex: string) {
  return new echarts.graphic.LinearGradient(0, 0, 0, 1, [
    { offset: 0, color: hex + '3D' },
    { offset: 1, color: hex + '08' },
  ])
}
const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { bottom: 0, icon: 'roundRect', itemWidth: 12, itemHeight: 4, textStyle: { color: '#5A5A56', fontSize: 12 } },
  grid: { left: 8, right: 12, top: 20, bottom: 36, containLabel: true },
  xAxis: {
    type: 'category',
    data: trend.value.map((d) => String(d.date).slice(5)),
    axisLine: { lineStyle: { color: '#E6E6E1' } },
    axisTick: { show: false },
    axisLabel: { color: '#8A8A84', fontSize: 11 },
  },
  yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#F1F1EE' } }, axisLabel: { color: '#8A8A84', fontSize: 11 } },
  series: (['users', 'pictures', 'works'] as const).map((key) => ({
    name: key === 'users' ? '用户' : key === 'pictures' ? '图片' : '作品',
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 5,
    data: trend.value.map((d) => Number(d[key] || 0)),
    lineStyle: { width: 2, color: SERIES_COLORS[key] },
    itemStyle: { color: SERIES_COLORS[key] },
    areaStyle: { color: areaColor(SERIES_COLORS[key]) },
  })),
}))

async function load() {
  loading.value = true
  try {
    const [o, t]: any[] = await Promise.all([getAdminOverview(), getAdminTrend()])
    if (o.data.code !== 0 || t.data.code !== 0) throw new Error(o.data.message || t.data.message)
    overview.value = o.data.data || {}
    trend.value = t.data.data || []
  } catch (e: any) {
    message.error(e?.message || '数据加载失败')
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
<style scoped>
.page-title { display:flex; align-items:end; justify-content:space-between; margin-bottom:24px; }
.page-title span { color:var(--text-3); font-size:12px; letter-spacing:.14em; }
.page-title h1 { margin:5px 0; font-size:26px; color:var(--text-1); }
.page-title p { margin:0; color:var(--text-3); font-size:13px; }

/* KPI：1920 六卡一屏；1024 降两列；640 单列（P2-6 验收） */
.metrics { display:grid; grid-template-columns:repeat(6,minmax(0,1fr)); gap:16px; margin-bottom:20px; }
.metric { display:flex; flex-direction:column; padding:20px; min-height:132px; }
.kpi-label { color:var(--text-2); font-size:13px; }
.kpi-num { margin:8px 0 4px; font-family:var(--font-display); font-weight:400; font-size:34px; line-height:1.1; color:var(--text-1); font-variant-numeric:tabular-nums; }
.kpi-foot { margin-top:auto; display:flex; align-items:end; justify-content:space-between; gap:8px; }
.kpi-caption { color:var(--text-3); font-size:12px; }
.delta { font-style:normal; font-size:11px; margin-right:5px; font-variant-numeric:tabular-nums; }
.delta.up { color:var(--accent-hover); }
.delta.down { color:var(--danger); }
.spark { flex:0 0 auto; opacity:.85; }

.dashboard-grid { display:grid; grid-template-columns:minmax(0,1.7fr) minmax(280px,1fr); gap:20px; }
.panel { min-width:0; padding:25px; }
.panel-heading { display:flex; justify-content:space-between; gap:12px; margin-bottom:12px; }
.panel h2 { margin:0 0 3px; color:var(--text-1); font-size:17px; }
.panel p { margin:0; color:var(--text-3); font-size:12px; }
.trend-chart { height:260px; width:100%; }
.attention>a { display:flex; align-items:center; gap:12px; padding:20px 0; border-bottom:1px solid var(--border); font-size:13px; color:var(--text-2); }
.attention>a span { flex:1; }
.attention>a strong { color:var(--accent-ink); font-size:18px; }
.attention>a b { font-weight:400; }
.attention>a:hover { color:var(--accent-ink); }

@media (max-width:1440px) { .metrics { grid-template-columns:repeat(3,minmax(0,1fr)); } }
@media (max-width:1024px) { .dashboard-grid { grid-template-columns:1fr; } }
@media (max-width:640px) { .metrics { grid-template-columns:repeat(2,minmax(0,1fr)); } }
</style>
