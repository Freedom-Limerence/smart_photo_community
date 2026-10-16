<template>
  <div class="search-page container">
    <div class="search-head">
      <p class="eyebrow">SEARCH</p>
      <h1>搜索{{ keyword ? '「' + keyword + '」' : '图片与作品' }}</h1>
      <a-input-search v-model:value="keyword" size="large" enter-button="搜索" allow-clear
                      placeholder="搜索图片语义，例如：风景 / 人像 / 夜景" @search="doSearch" />
      <div class="mode-switch">
        <a-radio-group v-model:value="mode" button-style="solid" size="small" @change="doSearch">
          <a-radio-button value="work">作品</a-radio-button>
          <a-radio-button value="picture">图片</a-radio-button>
        </a-radio-group>
        <span class="muted">作品 = 组图与文案的社区内容；图片 = 图库中的单张资产</span>
      </div>

      <div class="search-hint">
        <span>试试：</span>
        <button v-for="word in hotWords" :key="word" type="button" class="chip" @click="quickSearch(word)">{{ word }}</button>
      </div>
    </div>

    <!-- 搜索历史 -->
    <section v-if="history.length" class="history">
      <div class="history-head">
        <span class="eyebrow">RECENT</span>
        <div>
          <a-button type="text" size="small" @click="history = []; persistHistory()">清空记录</a-button>
        </div>
      </div>
      <div class="history-body">
        <span v-for="(word, i) in history" :key="word" class="history-chip">
          <button type="button" @click="quickSearch(word)">{{ word }}</button>
          <i @click="removeHistory(i)">×</i>
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
              :class="{ on: activeFields.includes(f) }" @click="toggleField(f)">
        {{ fieldLabel(f) }} <em>{{ fieldCount(f) }}</em>
      </button>
    </div>

    <a-spin v-if="mode === 'work'" :spinning="worksLoading">
      <div v-if="works.length" class="work-grid">
        <WorkCard v-for="item in works" :key="item.id" :work="item" ratio="66%" @open="router.push('/works/' + item.id)" />
      </div>
      <a-empty v-else-if="!worksLoading" description="没有找到匹配的作品">
        <a-button type="primary" @click="mode = 'picture'; doSearch()">改搜图片</a-button>
      </a-empty>
    </a-spin>

    <a-spin v-if="mode === 'picture'" :spinning="loading">
      <MasonryGrid v-if="filtered.length && viewMode === 'grid'" :items="filtered" key-field="pictureId">
        <template #default="{ item }">
          <article class="hit-card hover-lift" @click="openPicture(item)">
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
        <li v-for="item in filtered" :key="item.pictureId" @click="openPicture(item)">
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

      <a-empty v-else-if="!loading" :description="searched ? '没有找到匹配的内容' : '输入关键词开始搜索'">
        <template v-if="searched">
          <a-button type="primary" @click="keyword = ''; doSearch()">看看全部图片</a-button>
        </template>
      </a-empty>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { searchWorksByKeyword } from '@/api/photoCommunity'
import { searchWithEvidence } from '@/api/aiController'

const route = useRoute()
const router = useRouter()
const keyword = ref(String(route.query.q ?? ''))
const items = ref<any[]>([])
const loading = ref(false)
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
  try {
    const res: any = await searchWorksByKeyword({ keyword: word, current: 1, size: 24 })
    works.value = res.data.code === 0 ? (res.data.data?.records || []) : []
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
  try {
    const res: any = await searchWithEvidence({ query: word, topK: 24 })
    items.value = res.data.code === 0 ? (res.data.data || []) : []
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
.search-page { padding: var(--space-6) var(--space-5) var(--space-8); }
.search-head .eyebrow { margin: 0 0 6px; color: var(--text-3); font-size: 11px; letter-spacing: .2em; }
.search-head h1 { margin: 0 0 var(--space-4); font-size: var(--font-7); }
.search-hint { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-top: 12px; color: var(--text-3); font-size: var(--font-1); }
.chip { padding: 5px 12px; border: 1px solid var(--border); border-radius: var(--radius-pill); background: transparent; color: var(--text-2); font-size: var(--font-1); cursor: pointer; transition: all var(--dur-fast) var(--ease); }
.chip:hover { border-color: var(--accent); color: var(--text-1); }
.chip.on { background: var(--accent-soft); border-color: var(--accent); color: #4F5F53; }
.chip em { font-style: normal; opacity: .6; margin-left: 2px; }

.history { margin-top: var(--space-4); }
.history-head { display: flex; align-items: center; justify-content: space-between; }
.history-body { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.history-chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; border-radius: var(--radius-pill); background: var(--surface-2); font-size: var(--font-1); }
.history-chip button { border: none; background: none; padding: 0; color: var(--text-2); cursor: pointer; }
.history-chip i { cursor: pointer; color: var(--text-3); font-style: normal; }

.result-bar { display: flex; align-items: center; justify-content: space-between; margin: var(--space-5) 0 var(--space-3); }
.result-count strong { font-size: var(--font-4); }
.result-tools { display: flex; align-items: center; gap: 10px; }
.field-filter { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; margin-bottom: var(--space-4); }

.hit-list { list-style: none; margin: 0; padding: 0; display: flex; flex-direction: column; gap: 10px; }
.hit-list li { display: flex; align-items: center; gap: 14px; padding: 10px; border: 1px solid var(--border); border-radius: var(--radius); cursor: pointer; transition: all var(--dur-fast) var(--ease); }
.hit-list li:hover { border-color: var(--accent); background: var(--surface-2); }
.hit-list img { width: 110px; height: 74px; object-fit: cover; border-radius: var(--radius-sm); }
.hit-list div { flex: 1 1 auto; min-width: 0; }
.hit-tags { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 6px; }
.mode-switch { display: flex; align-items: center; gap: 12px; margin-top: 12px; flex-wrap: wrap; }
.work-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: var(--space-4); margin-top: var(--space-3); }
.muted { color: var(--text-3); }
</style>
