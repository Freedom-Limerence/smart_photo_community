<template>
  <div class="admin-soon">
    <div class="surface-card soon-card">
      <a-tag color="green">任务三</a-tag>
      <h1>{{ title }}</h1>
      <p>{{ desc }}</p>
      <a-divider />
      <p class="muted">该模块随「任务三：管理员端彻底分离」交付，依赖以下后端能力：</p>
      <ul>
        <li v-for="item in needs" :key="item">{{ item }}</li>
      </ul>
      <a-space>
        <a-button type="primary" @click="router.push('/admin/users')">先去用户与权限</a-button>
        <a-button @click="router.push('/')">返回前台</a-button>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const title = computed(() => {
  if (route.path.includes('dashboard')) return '数据看板'
  if (route.path.includes('review')) return '内容审核中心'
  return '系统监控'
})

const desc = computed(() => {
  if (route.path.includes('dashboard')) return '平台总用户、总图片、今日新增、活跃度趋势（ECharts）'
  if (route.path.includes('review')) return '图片与作品的独立审核工作台，支持多条件筛选与批量通过/拒绝'
  return '操作日志查询与接口调用统计（次数 / 耗时 / P95）'
})

const needs = computed(() => {
  if (route.path.includes('dashboard')) return ['GET /admin/stats/overview（总量与今日新增）', 'GET /admin/stats/trend?days=30（趋势）', 'GET /admin/stats/category（分类分布）']
  if (route.path.includes('review')) return ['作品审核状态 PENDING_REVIEW 与审核流', 'POST /admin/review/picture|work/page（多条件）', 'POST /admin/review/batch（批量通过/拒绝）']
  return ['复用 operation_log（已存在）', '接口调用统计聚合表 api_metric（按小时/接口）']
})
</script>

<style scoped>
.admin-soon { display: grid; place-items: center; min-height: 60vh; }
.soon-card { max-width: 640px; padding: var(--space-6); }
.soon-card h1 { margin: var(--space-3) 0 var(--space-2); font-size: var(--font-6); }
.soon-card p { margin: 0; color: var(--text-2); }
.soon-card ul { margin: var(--space-3) 0 var(--space-5); padding-left: 20px; color: var(--text-2); font-size: var(--font-2); }
</style>
