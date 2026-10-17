<template>
  <div class="search-page container">
    <PageHeader
      eyebrow="SEARCH"
      :title="keyword ? `搜索「${keyword}」` : '搜索图片与作品'"
      description="作品 = 组图与文案的社区内容；图片 = 图库中的单张资产"
    >
      <div class="search-tools">
        <a-input-search v-model:value="keyword" size="large" enter-button="搜索" allow-clear
                        aria-label="搜索作品或图片" placeholder="搜索图片语义，例如：风景 / 人像 / 夜景" @search="doSearch" />
        <div class="search-extras">
          <a-radio-group v-model:value="mode" button-style="solid" size="small" @change="doSearch">
            <a-radio-button value="work">作品</a-radio-button>
            <a-radio-button value="picture">图片</a-radio-button>
          </a-radio-group>
          <div class="search-hint">
            <span>试试：</span>
            <button v-for="word in hotWords" :key="word" type="button" class="chip" @click="quickSearch(word)">{{ word }}</button>
          </div>
        </div>
      </div>
    </PageHeader>

    <!-- 搜索历史 -->
    <section v-if="history.length" class="history">
      <div class="history-head">
        <span class="eyebrow">RECENT</span>
        <a-button type="text" size="small" @click="history = []; persistHistory()">清空记录</a-button>
      </div>
      <div class="history-body">
        <span v-for="(word, i) in history" :key="word" class="history-chip">
          <button type="button" @click="quickSearch(word)">{{ word }}</button>
          <i role="button" tabindex="0" aria-label="删除该条记录" @click="removeHistory(i)" @keydown.enter="removeHistory(i)">×</i>
        </span>
      </div>
    </section>

    <!-- 结果工具条 -->
    <div v-if="searched && mode === 'picture'" class="result-bar">
      <div class="result-count">
        共 <strong>{{ filtered.length }}</strong> 条结果
        <span v-if="activeFields.length" class="muted">（已按 {{ activeFields.length }} 个条件筛选）</span>
      </div>
      <div class="result-tools">
        <a-radio-group v-model:value="viewMode" button-style="solid" size="small">
          <a-radio-button value="grid">网格</a-radio-button>
          <a-radio-button value="list">列表</a-radio-button>
        </a-radio-group>
        <a-button size="small" :loading="loading" @click="doSearch">重新搜索</a-button>
      </div>
    </div>

    <!-- 匹配字段筛选（可点选，客户端过滤） -->
    <div v-if="searched && mode === 'picture' && availableFields.length" class="field-filter">
      <span class="eyebrow">按匹配来源筛选</span>
      <button v-for="f in availableFields" :key="f" type="button" class="chip"
              :class="{ on: activeFields.includes(f) }" :aria-pressed="activeFields.includes(f)" @click="toggleField(f)">
        {{ fieldLabel(f) }} <em>{{ fieldCount(f) }}</em>
      </button>
    </div>

    <a-spin v-if="mode === 'work'" :spinning="worksLoading">
      <ErrorState v-if="worksError" :description="worksError" @retry="searchWorks()" />
      <CardSkeleton v-else-if="worksLoading && !works.length" variant="grid" :count="8" />
      <div v-else-if="works.length" class="card-grid">
        <WorkCard v-for="item in works" :key="item.id" :work="item" ratio="66%" @open="router.push('/works/' + item.id)" />
      </div>
      <EmptyState v-else-if="!worksError" title="没有找到匹配的作品" description="换个关键词，或者直接按图片语义检索。">
        <a-button type="primary" @click="mode = 'picture'; doSearch()">改搜图片</a-button>
      </EmptyState>
    </a-spin>

    <a-spin v-if="mode === 'picture'" :spinning="loading">
      <ErrorState v-if="error" :description="error" @retry="doSearch()" />
      <CardSkeleton v-else-if="loading && !filtered.length" variant="grid" :count="8" />
      <MasonryGrid v-else-if="filtered.length && viewMode === 'grid'" :items="filtered" key-field="pictureId">
        <template #default="{ item }">
          <article class="hit-card hover-lift" tabindex="0" @click="openPicture(item)" @keydown.enter="openPicture(item)">
            <div class="hit-cover">
              <img :src="item.thumbnailUrl || item.url" alt="" loading="lazy" />
            </div>
            <div class="hit-body">
              <strong class="ellipsis">{{ item.name || ('图片 #' + item.pictureId) }}</strong>
              <div class="hit-tags">
                <a-tag color="green">{{ item.accessReason }}</a-tag>
                <a-tag v-for="f in item.matchedFields" :key="f">{{ fieldLabel(f) }}</a-tag>
              </div>
            </div>
          </article>
        </template>
      </MasonryGrid>

      <ul v-else-if="filtered.length" class="hit-list">
        <li v-for="item in filtered" :key="item.pictureId" tabindex="0" @click="openPicture(item)" @keydown.enter="openPicture(item)">
          <img :src="item.thumbnailUrl || item.url" alt="" loading="lazy" />
          <div>
            <strong>{{ item.name || ('图片 #' + item.pictureId) }}</strong>
            <div class="hit-tags">
              <a-tag color="green">{{ item.accessReason }}</a-tag>
              <a-tag v-for="f in item.matchedFields" :key="f">{{ fieldLabel(f) }}</a-tag>
            </div>
          </div>
          <a-button type="text" size="small">查看 →</a-button>
        </li>
      </ul>

      <EmptyState v-else-if="!loading && !error" :title="searched ? '没有找到匹配的内容' : '输入关键词开始搜索'"
                  :description="searched ? '可以换一个说法，或用更具体的画面描述。' : '支持自然语言描述画面，例如「清晨湖面的薄雾」。'">
        <a-button v-if="searched" type="primary" @click="keyword = ''; doSearch()">看看全部图片</a-button>
      </EmptyState>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import ErrorState from '@/components/common/ErrorState.vue'
import PageHeader from '@/components/common/PageHeader.vue'
import { searchWorksByKeyword } from '@/api/photoCommunity'
import { searchWithEvidence } from '@/api/aiController'

const route = useRoute()
const router = useRouter()
const keyword = ref(String(route.query.q ?? ''))
const items = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const worksError = ref('')
const searched = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')
const activeFields = ref<string[]>([])
const history = ref<string[]>([])
const mode = ref<'work' | 'picture'>('work')
const works = ref<any[]>([])
const worksLoading = ref(false)

async function searchWorks() {
  const word = keyword.value.trim()
  if (!word) { works.value = []; return }
  worksLoading.value = true
  worksError.value = ''
  try {
    const res: any = await searchWorksByKeyword({ keyword: word, current: 1, size: 24 })
    works.value = res.data.code === 0 ? (res.data.data?.records || []) : []
  } catch {
    worksError.value = '作品检索暂时不可用，请稍后重试。'
  } finally {
    worksLoading.value = false
  }
}

const hotWords = ['风景', '人像', '夜景', '街拍', '静物', '建筑', '日出', '雪景']
const HISTORY_KEY = 'photo_search_history'

const fieldLabels: Record<string, string> = {
  name: '名称', introduction: '简介', category: '分类', tags: '标签',
  subjectTags: '主体', sceneTags: '场景', lightTags: '光线', compositionTags: '构图', camera: '相机',
}
function fieldLabel(f: string) { return fieldLabels[f] || f }

const availableFields = computed(() => {
  const set = new Set<string>()
  for (const item of items.value) {
    for (const f of item.matchedFields || []) set.add(f)
  }
  return Array.from(set)
})

const filtered = computed(() => {
  if (!activeFields.value.length) return items.value
  return items.value.filter((item) => {
    const fields: string[] = item.matchedFields || []
    return activeFields.value.every((f) => fields.includes(f))
  })
})

function fieldCount(f: string) {
  return items.value.filter((item) => (item.matchedFields || []).includes(f)).length
}

function toggleField(f: string) {
  const index = activeFields.value.indexOf(f)
  if (index >= 0) activeFields.value.splice(index, 1)
  else activeFields.value.push(f)
}

function loadHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    history.value = raw ? JSON.parse(raw) : []
  } catch {
    history.value = []
  }
}

function persistHistory() {
  try {
    localStorage.setItem(HISTORY_KEY, JSON.stringify(history.value.slice(0, 10)))
  } catch {
    /* ignore */
  }
}

function pushHistory(word: string) {
  history.value = [word, ...history.value.filter((w) => w !== word)].slice(0, 10)
  persistHistory()
}

function removeHistory(index: number) {
  history.value.splice(index, 1)
  persistHistory()
}

function quickSearch(word: string) {
  keyword.value = word
  void doSearch()
}

function openPicture(item: any) {
  router.push('/picture/' + item.pictureId)
}

async function doSearch() {
  const word = keyword.value.trim()
  searched.value = true
  activeFields.value = []
  if (!word) {
    items.value = []
    return
  }
  pushHistory(word)
  if (mode.value === 'work') {
    await searchWorks()
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res: any = await searchWithEvidence({ query: word, topK: 24 })
    items.value = res.data.code === 0 ? (res.data.data || []) : []
  } catch {
    error.value = '检索暂时不可用，请稍后重试。'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadHistory()
  if (keyword.value) void doSearch()
})
</script>

<style scoped>
.search-page { padding-top: var(--space-6); padding-bottom: var(--space-8); }

/* 搜索区：控制在舒适行宽内，超宽屏不至于拉成一条 */
.search-tools { display: flex; flex-direction: column; gap: var(--space-4); max-width: 880px; }
.search-extras { display: flex; align-items: center; gap: var(--space-4); flex-wrap: wrap; }
.search-hint { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); color: var(--text-3); font-size: var(--font-1); }

.chip {
  padding: 5px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  color: var(--text-2);
  font-size: var(--font-1);
  cursor: pointer;
  transition: color var(--dur-fast) var(--ease), background-color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}
.chip:hover { border-color: var(--accent); color: var(--text-1); }
.chip.on { background: var(--accent-soft); border-color: var(--accent); color: var(--accent-ink); }
.chip em { margin-left: 2px; font-style: normal; opacity: .6; }

.history { margin-top: var(--space-5); }
.history-head { display: flex; align-items: center; justify-content: space-between; }
.history-head .eyebrow { margin: 0; }
.history-body { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-2); }
.history-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-pill);
  background: var(--surface);
  font-size: var(--font-1);
}
.history-chip button { padding: 0; border: none; background: none; color: var(--text-2); cursor: pointer; }
.history-chip i { color: var(--text-3); cursor: pointer; font-style: normal; }
.history-chip i:hover { color: var(--danger); }

.result-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin: var(--space-6) 0 var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--border);
}
.result-count strong { font-size: var(--font-4); }
.result-tools { display: flex; align-items: center; gap: var(--space-3); }

.field-filter { display: flex; flex-wrap: wrap; align-items: center; gap: var(--space-2); margin-bottom: var(--space-5); }
.field-filter .eyebrow { margin: 0 var(--space-2) 0 0; }

/* 图片结果卡：与作品卡同一套圆角 / 边框 / 阴影语言 */
.hit-card { background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-lg); overflow: hidden; cursor: pointer; }
.hit-card:focus-visible { outline: 2px solid var(--accent); outline-offset: 3px; }
.hit-cover { background: var(--surface-2); }
.hit-cover img { display: block; width: 100%; height: auto; }
.hit-body { display: flex; flex-direction: column; gap: var(--space-2); padding: var(--space-3) var(--space-4) var(--space-4); }
.hit-body strong { font-size: var(--font-2); font-weight: 500; }

.hit-list { display: flex; flex-direction: column; gap: var(--space-3); margin: 0; padding: 0; list-style: none; }
.hit-list li {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-3);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  background: var(--surface);
  cursor: pointer;
  transition: border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease);
}
.hit-list li:hover { border-color: var(--border-strong); box-shadow: var(--shadow-2); transform: translateY(-1px); }
.hit-list li:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px; }
.hit-list img { width: 120px; height: 80px; object-fit: cover; border-radius: var(--radius); }
.hit-list div { flex: 1 1 auto; min-width: 0; }
.hit-tags { display: flex; flex-wrap: wrap; gap: var(--space-1); margin-top: 6px; }

.muted { color: var(--text-3); }

@media (max-width: 640px) {
  .result-bar { flex-direction: column; align-items: flex-start; }
  .hit-list img { width: 88px; height: 62px; }
}
</style>
