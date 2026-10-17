<template>
  <div class="dashboard">
    <div class="page-title"><div><span>OPERATIONS / OVERVIEW</span><h1>数据看板</h1><p>平台内容与社区运营概况</p></div><a-button :loading="loading" @click="load">刷新数据</a-button></div>
    <div class="metrics"><div v-for="item in metrics" :key="item.key" class="metric"><span>{{ item.label }}</span><strong>{{ overview[item.key] ?? '—' }}</strong><small>{{ item.caption }}</small></div></div>
    <div class="dashboard-grid">
      <section class="panel trend"><div class="panel-heading"><div><h2>近 7 日增长</h2><p>每日新增用户、图片与作品</p></div><div class="legend"><span>用户</span><span>图片</span><span>作品</span></div></div><div class="chart"><div v-for="day in trend" :key="day.date" class="day"><div class="bars"><i :style="barStyle(day.users)" /><i :style="barStyle(day.pictures)" /><i :style="barStyle(day.works)" /></div><span>{{ String(day.date).slice(5) }}</span></div></div></section>
      <section class="panel attention"><div class="panel-heading"><div><h2>待处理事项</h2><p>需要管理员关注的内容</p></div></div><router-link to="/admin/review"><span>待审核图片</span><strong>{{ overview.pendingPictures ?? 0 }}</strong><b>→</b></router-link><router-link to="/admin/review"><span>已封禁作品</span><strong>{{ overview.bannedWorks ?? 0 }}</strong><b>→</b></router-link><router-link to="/admin/users"><span>已封禁账号</span><strong>{{ overview.bannedUsers ?? 0 }}</strong><b>→</b></router-link></section>
    </div>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getAdminOverview, getAdminTrend } from '@/api/adminConsole'
const loading = ref(false); const overview = ref<Record<string, number>>({}); const trend = ref<any[]>([])
const metrics = [
  { key: 'users', label: '总用户', caption: '今日新增' }, { key: 'pictures', label: '图库图片', caption: '站内资产总量' },
  { key: 'works', label: '公开作品', caption: '社区可见作品' }, { key: 'newUsersToday', label: '今日新用户', caption: '截至当前' },
  { key: 'newPicturesToday', label: '今日新图片', caption: '截至当前' }, { key: 'pendingPictures', label: '待审核', caption: '等待处理的图片' },
]
const maxValue = computed(() => Math.max(1, ...trend.value.flatMap((d) => [Number(d.users), Number(d.pictures), Number(d.works)])))
function barStyle(value: number) { return { height: Math.max(3, Number(value || 0) / maxValue.value * 150) + 'px' } }
async function load() { loading.value = true; try { const [o, t]: any[] = await Promise.all([getAdminOverview(), getAdminTrend()]); if (o.data.code !== 0 || t.data.code !== 0) throw new Error(o.data.message || t.data.message); overview.value = o.data.data || {}; trend.value = t.data.data || [] } catch (e: any) { message.error(e?.message || '数据加载失败') } finally { loading.value = false } }
onMounted(load)
</script>
<style scoped>
.page-title { display:flex; align-items:end; justify-content:space-between; margin-bottom:24px; }.page-title span { color:var(--text-3); font-size:12px; letter-spacing:.14em; }.page-title h1 { margin:5px 0; font-size:26px; color:var(--text-1); }.page-title p { margin:0; color:var(--text-3); font-size:13px; }
.metrics { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px; margin-bottom:20px; }.metric { display:flex; flex-direction:column; padding:23px; min-height:128px; background:white; border:1px solid var(--border); border-radius:8px; }.metric span { color:#738178; font-size:13px; }.metric strong { margin:8px 0 3px; font-size:29px; line-height:1.1; color:#253b2d; }.metric small { color:#a4ada6; font-size:12px; }
.dashboard-grid { display:grid; grid-template-columns:minmax(0,1.7fr) minmax(280px,1fr); gap:20px; }.panel { min-width:0; background:white; border:1px solid var(--border); border-radius:8px; padding:25px; }.panel-heading { display:flex; justify-content:space-between; gap:12px; }.panel h2 { margin:0 0 3px; color:#2b352e; font-size:17px; }.panel p { margin:0; color:#a0a9a1; font-size:12px; }.legend { display:flex; gap:16px; font-size:12px; color:#819087; }.legend span:before { content:''; display:inline-block; width:8px; height:8px; margin-right:5px; border-radius:2px; background:#546f5b; }.legend span:nth-child(2):before { background:var(--border-strong); }.legend span:nth-child(3):before { background:var(--warning); }
.chart { display:flex; align-items:end; justify-content:space-around; height:210px; padding:20px 0 0; border-bottom:1px solid #e8ece8; }.day { display:flex; flex-direction:column; align-items:center; justify-content:end; height:100%; gap:9px; }.day .bars { display:flex; align-items:end; gap:3px; height:155px; }.bars i { display:block; width:10px; min-height:3px; background:#546f5b; border-radius:2px 2px 0 0; }.bars i:nth-child(2) { background:var(--border-strong); }.bars i:nth-child(3) { background:var(--warning); }.day>span { color:#9aa59c; font-size:10px; }
.attention>a { display:flex; align-items:center; gap:12px; padding:20px 0; border-bottom:1px solid var(--border); font-size:13px; color:#66756a; }.attention>a span { flex:1; }.attention>a strong { color:#344e3b; font-size:18px; }.attention>a b { font-weight:400; }.attention>a:hover { color:#2d4735; }
@media (max-width:900px) { .metrics { grid-template-columns:repeat(2,1fr); }.dashboard-grid { grid-template-columns:1fr; } } @media (max-width:550px) { .metrics { grid-template-columns:1fr; } }
</style>
