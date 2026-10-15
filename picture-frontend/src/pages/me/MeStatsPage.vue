<template>
  <div class="me-page">
    <div class="section-head">
      <div>
        <p class="eyebrow">MY PERFORMANCE</p>
        <h2>作品数据表现</h2>
        <p>你发布作品的浏览、点赞、收藏与评论汇总</p>
      </div>
      <a-button @click="load">刷新</a-button>
    </div>

    <a-spin :spinning="loading">
      <a-row :gutter="16" class="stats-row">
        <a-col :xs="12" :md="6"><div class="stat surface-card"><span>作品数</span><strong>{{ data.workCount ?? 0 }}</strong></div></a-col>
        <a-col :xs="12" :md="6"><div class="stat surface-card"><span>总浏览</span><strong>{{ data.viewCount ?? 0 }}</strong></div></a-col>
        <a-col :xs="12" :md="6"><div class="stat surface-card"><span>总点赞</span><strong>{{ data.likeCount ?? 0 }}</strong></div></a-col>
        <a-col :xs="12" :md="6"><div class="stat surface-card"><span>总收藏</span><strong>{{ data.favoriteCount ?? 0 }}</strong></div></a-col>
      </a-row>

      <a-card :bordered="false" class="surface-card" title="表现最好的作品">
        <a-table :data-source="data.ranking || []" :columns="columns" :pagination="false" row-key="id" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'title'">
              <a-space>
                <img v-if="record.coverUrl" :src="record.coverUrl" class="cover" alt="" />
                <a @click="router.push('/works/' + record.id)">{{ record.title }}</a>
              </a-space>
            </template>
            <template v-else-if="column.dataIndex === 'bar'">
              <div class="bar-wrap"><div class="bar" :style="{ width: barWidth(record) }" /></div>
            </template>
          </template>
        </a-table>
        <a-empty v-if="!(data.ranking || []).length && !loading" description="还没有发布作品" />
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyWorkStats } from '@/api/notification'

const router = useRouter()
const data = ref<Record<string, any>>({})
const loading = ref(false)

const columns = [
  { title: '作品', dataIndex: 'title', ellipsis: true },
  { title: '浏览', dataIndex: 'viewCount', width: 90 },
  { title: '点赞', dataIndex: 'likeCount', width: 90 },
  { title: '收藏', dataIndex: 'favoriteCount', width: 90 },
  { title: '评论', dataIndex: 'commentCount', width: 90 },
  { title: '热度', dataIndex: 'bar', width: 160 },
]

const maxScore = computed(() => {
  const rows = data.value.ranking || []
  return Math.max(1, ...rows.map((r: any) => score(r)))
})

function score(record: any) {
  return Number(record.viewCount || 0) + Number(record.likeCount || 0) * 3 + Number(record.favoriteCount || 0) * 5
}

function barWidth(record: any) {
  return Math.max(4, Math.round((score(record) / maxScore.value) * 100)) + '%'
}

async function load() {
  loading.value = true
  try {
    const res: any = await getMyWorkStats()
    if (res.data.code === 0) data.value = res.data.data || {}
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.me-page { display: flex; flex-direction: column; }
.stats-row { margin-bottom: var(--space-5); }
.stat { display: flex; flex-direction: column; gap: 4px; padding: var(--space-5); text-align: center; }
.stat span { color: var(--text-3); font-size: var(--font-1); }
.stat strong { font-size: var(--font-7); }
.cover { width: 56px; height: 40px; object-fit: cover; border-radius: var(--radius-sm); }
.bar-wrap { height: 6px; background: var(--surface-2); border-radius: 3px; overflow: hidden; }
.bar { height: 100%; background: var(--accent); border-radius: 3px; }
</style>
