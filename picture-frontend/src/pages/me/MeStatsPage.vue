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

    <!-- 成就等级与徽章：由真实数据推导（作品/获赞/收藏/粉丝） -->
    <div v-if="achievement.level" class="ach surface-card">
      <div class="ach-head">
        <div class="ach-level">
          <span class="lv">Lv.{{ achievement.level }}</span>
          <div>
            <strong>{{ achievement.levelName }}</strong>
            <em>成就值 {{ achievement.score }} · 距下一级还差 {{ Math.max(0, (achievement.nextLevelScore || 0) - (achievement.score || 0)) }}</em>
          </div>
        </div>
        <a-button type="text" @click="router.push('/wall')">去大屏看看 →</a-button>
      </div>
      <div class="ach-bar"><i :style="{ width: (achievement.progress || 0) + '%' }" /></div>
      <div class="ach-badges">
        <span v-for="b in achievement.badges || []" :key="b.name" class="badge" :class="{ owned: b.owned }" :title="b.description">
          {{ b.owned ? '★' : '☆' }} {{ b.name }}
        </span>
      </div>
    </div>

    <a-spin :spinning="loading">
      <div class="stat-grid">
        <div class="stat surface-card"><span>作品数</span><strong>{{ data.workCount ?? 0 }}</strong></div>
        <div class="stat surface-card"><span>总浏览</span><strong>{{ data.viewCount ?? 0 }}</strong></div>
        <div class="stat surface-card"><span>总点赞</span><strong>{{ data.likeCount ?? 0 }}</strong></div>
        <div class="stat surface-card"><span>总收藏</span><strong>{{ data.favoriteCount ?? 0 }}</strong></div>
      </div>

      <a-card :bordered="false" class="surface-card ranking-card" title="表现最好的作品">
        <a-table :data-source="data.ranking || []" :columns="columns" :pagination="false" row-key="id" size="middle">
          <template #emptyText>
            <EmptyState size="sm" title="还没有发布作品" description="发布作品后，这里会出现热度排行。" />
          </template>
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
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getAchievement, getMyWorkStats } from '@/api/notification'
import EmptyState from '@/components/common/EmptyState.vue'

const router = useRouter()
const data = ref<Record<string, any>>({})
const achievement = ref<Record<string, any>>({})
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
  const ach: any = await getAchievement()
  if (ach.data.code === 0) achievement.value = ach.data.data || {}

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

/* 四个核心指标：等宽卡片，数字用衬线体做出对比 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(200px, 100%), 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}
.stat {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-5);
  text-align: center;
  transition: border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease);
}
.stat:hover { border-color: var(--border-strong); box-shadow: var(--shadow-2); }
.stat span { color: var(--text-3); font-size: var(--font-1); letter-spacing: .06em; }
.stat strong { color: var(--text-1); font-family: var(--font-display); font-size: var(--font-7); font-weight: 400; line-height: 1.15; }

.ranking-card :deep(.ant-card-head) { border-bottom: 1px solid var(--border); }

.ach { padding: var(--space-5); margin-bottom: var(--space-5); }
.ach-head { display: flex; align-items: center; justify-content: space-between; gap: var(--space-4); margin-bottom: var(--space-4); }
.ach-level { display: flex; align-items: center; gap: var(--space-4); }
.ach-level .lv { display: grid; place-items: center; width: 46px; height: 46px; border-radius: var(--radius); background: var(--accent-soft); color: var(--accent-ink); font-weight: 600; }
.ach-level strong { display: block; font-size: var(--font-4); }
.ach-level em { color: var(--text-3); font-size: var(--font-1); font-style: normal; }
.ach-bar { height: 6px; background: var(--surface-2); border-radius: var(--radius-pill); overflow: hidden; margin-bottom: var(--space-4); }
.ach-bar i { display: block; height: 100%; background: var(--accent); transition: width var(--dur-slow) var(--ease); }
.ach-badges { display: flex; flex-wrap: wrap; gap: var(--space-2); }
.badge { padding: 4px 10px; border-radius: var(--radius-pill); background: var(--surface-2); color: var(--text-3); font-size: var(--font-1); }
.badge.owned { background: var(--accent-soft); color: var(--accent-ink); }

.cover { width: 56px; height: 40px; object-fit: cover; border-radius: var(--radius-sm); }
.bar-wrap { height: 6px; background: var(--surface-2); border-radius: var(--radius-pill); overflow: hidden; }
.bar { height: 100%; background: var(--accent); border-radius: var(--radius-pill); }
</style>
