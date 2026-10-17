<template>
  <div class="ex-detail">
    <a-spin :spinning="loading">
      <template v-if="exhibition">
        <!-- 顶部宽幅封面 -->
        <header class="cover" :style="coverStyle">
          <div class="cover-mask" />
          <div class="container cover-inner">
            <p class="eyebrow">EXHIBITION / 专题展览</p>
            <h1>{{ exhibition.title }}</h1>
            <p v-if="exhibition.theme" class="theme">「{{ exhibition.theme }}」</p>
            <div class="meta">
              <span><PictureOutlined /> 展品 {{ works.length }} 件</span>
              <span><EyeOutlined /> {{ exhibition.viewCount ?? 0 }} 次浏览</span>
              <span v-if="exhibition.startTime || exhibition.endTime"><CalendarOutlined /> {{ dateRange }}</span>
              <a-tag v-if="exhibition.status !== 1" color="orange">未发布（仅管理员可见）</a-tag>
            </div>
          </div>
        </header>

        <!-- 征稿横幅（B2 活动·征稿） -->
        <section v-if="call" class="container call-banner">
          <div class="call-main">
            <span class="call-tag">征稿中</span>
            <div>
              <h3>{{ call.title }}</h3>
              <p v-if="call.description">{{ call.description }}</p>
            </div>
          </div>
          <div class="call-side">
            <div class="countdown">
              <strong>{{ countdown }}</strong>
              <span>距截稿</span>
            </div>
            <div class="quota">已投 {{ submittedCount }} / {{ call.maxSubmissionsPerUser || 1 }}</div>
            <a-button type="primary" :disabled="remaining <= 0" @click="openSubmit">
              {{ remaining > 0 ? '立即投稿' : '已达投稿上限' }}
            </a-button>
          </div>
        </section>

        <!-- 策展语 -->
        <section class="container statement">
          <p class="eyebrow">CURATOR'S NOTE / 策展语</p>
          <p v-if="exhibition.description" class="statement-text">{{ exhibition.description }}</p>
          <p v-else class="muted">这个专题还没有写策展语。</p>
        </section>

        <!-- 展品作品墙 -->
        <section class="container">
          <div class="section-head">
            <div>
              <p class="eyebrow">WORKS</p>
              <h2>展品</h2>
              <p>共 {{ works.length }} 件作品，按策展顺序排列</p>
            </div>
            <a-button type="text" @click="router.push('/exhibitions')">← 返回专题列表</a-button>
          </div>

          <MasonryGrid v-if="works.length" :items="works">
            <template #default="{ item }">
              <div class="piece">
                <WorkCard :work="item" :ratio="ratioOf(item)" @open="openWork" />
                <p v-if="captions[String(item.id)]" class="caption">
                  <span class="quote">“</span>{{ captions[String(item.id)] }}
                </p>
              </div>
            </template>
          </MasonryGrid>

          <a-empty v-else description="该专题还没有展品">
            <a-button type="primary" @click="router.push('/exhibitions')">看看其他专题</a-button>
          </a-empty>
        </section>
      </template>

      <a-empty v-else-if="!loading" description="专题不存在、未发布或已下线">
        <a-button type="primary" @click="router.push('/exhibitions')">返回专题列表</a-button>
      </a-empty>
    </a-spin>

    <!-- 投稿弹窗（B2）：从自己的作品中选择 -->
    <a-modal v-model:open="submitOpen" title="投稿作品" width="880px" :confirm-loading="submitting"
             ok-text="提交投稿" cancel-text="取消" @ok="doSubmit">
      <div class="submit-layout">
        <div class="works-pane">
          <a-spin :spinning="worksLoading">
            <div v-if="myWorks.length" class="work-picks">
              <label v-for="item in myWorks" :key="item.id" class="pick" :class="{ on: picked.includes(String(item.id)) }">
                <input type="checkbox" :checked="picked.includes(String(item.id))" @change="togglePick(String(item.id))" />
                <img :src="thumbOf(item)" :alt="item.title" />
                <span class="pick-title">{{ item.title || '未命名作品' }}</span>
              </label>
            </div>
            <a-empty v-else description="你还没有已发布的作品，先去发布一组吧" />
          </a-spin>
        </div>
        <aside class="call-pane">
          <p class="eyebrow">投稿说明</p>
          <p class="call-desc">{{ call?.description || '按主题挑选你的作品投稿。' }}</p>
          <a-textarea v-model:value="submitCaption" :rows="3" placeholder="给策展人的一句话（可选）" />
          <p class="hint">已选 {{ picked.length }} 件 · 还可投 {{ remaining }} 件</p>
        </aside>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CalendarOutlined, EyeOutlined, PictureOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { getActiveCall, getExhibitionDetail, listWorks, submitWorks } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const exhibition = ref<any>(null)
const works = ref<any[]>([])
const captions = ref<Record<string, string>>({})
const loginUserStore = useLoginUserStore()
const call = ref<any>(null)
const submittedCount = ref(0)
const remaining = ref(0)
const submitOpen = ref(false)
const submitting = ref(false)
const worksLoading = ref(false)
const myWorks = ref<any[]>([])
const picked = ref<string[]>([])
const submitCaption = ref('')
const now = ref(Date.now())
let clock: number | null = null

const countdown = computed(() => {
  const deadline = call.value?.deadline
  if (!deadline) return '不限时'
  const diff = new Date(deadline).getTime() - now.value
  if (diff <= 0) return '已截稿'
  const day = Math.floor(diff / 86400000)
  const hour = Math.floor((diff % 86400000) / 3600000)
  const minute = Math.floor((diff % 3600000) / 60000)
  const second = Math.floor((diff % 60000) / 1000)
  if (day > 0) return day + ' 天 ' + hour + ' 时'
  const pad = (v: number) => String(v).padStart(2, '0')
  return pad(hour) + ':' + pad(minute) + ':' + pad(second)
})

function thumbOf(work: any) {
  const pic = work?.pictures?.[0]
  return work?.coverUrl || pic?.thumbnailUrl || pic?.url || ''
}

function togglePick(id: string) {
  const index = picked.value.indexOf(id)
  if (index >= 0) picked.value.splice(index, 1)
  else picked.value.push(id)
}

async function loadCall() {
  const res: any = await getActiveCall(exhibitionId.value)
  if (res.data.code === 0) {
    call.value = res.data.data?.call || null
    submittedCount.value = Number(res.data.data?.submittedCount || 0)
    remaining.value = Number(res.data.data?.remaining || 0)
  }
}

async function openSubmit() {
  if (remaining.value <= 0) return
  submitOpen.value = true
  picked.value = []
  submitCaption.value = ''
  worksLoading.value = true
  try {
    const res: any = await listWorks({ current: 1, size: 60, authorId: loginUserStore.loginUser?.id })
    myWorks.value = res.data.code === 0 ? (res.data.data?.records || []) : []
  } finally {
    worksLoading.value = false
  }
}

async function doSubmit() {
  if (!pickValid()) return
  submitting.value = true
  try {
    const res: any = await submitWorks({ callId: call.value.id, workIds: picked.value, caption: submitCaption.value })
    if (res.data.code === 0) {
      message.success('投稿成功，等待策展人评选')
      submitOpen.value = false
      await loadCall()
    } else {
      message.error(res.data.message || '投稿失败')
    }
  } finally {
    submitting.value = false
  }
}

function pickValid() {
  if (!picked.value.length) {
    message.warning('请至少选择一件作品')
    return false
  }
  if (picked.value.length > remaining.value) {
    message.warning('最多还能投 ' + remaining.value + ' 件')
    return false
  }
  return true
}


const exhibitionId = computed(() => String(route.params.id))

const coverStyle = computed(() => {
  const cover = exhibition.value?.coverUrl || works.value[0]?.coverUrl || works.value[0]?.pictures?.[0]?.url
  return cover
    ? { backgroundImage: 'url(' + cover + ')', backgroundSize: 'cover', backgroundPosition: 'center' }
    : { background: 'linear-gradient(120deg, #2A332E, #4A5A50)' }
})

const dateRange = computed(() => {
  const fmt = (v?: string) => {
    if (!v) return ''
    const d = new Date(v)
    return Number.isNaN(d.getTime()) ? '' : d.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
  }
  const start = fmt(exhibition.value?.startTime)
  const end = fmt(exhibition.value?.endTime)
  if (start && end) return start + ' — ' + end
  return start || end || ''
})

function ratioOf(work: any) {
  const pic = work?.pictures?.[0]
  if (!pic?.picWidth || !pic?.picHeight) return '66%'
  return Math.round((pic.picHeight / pic.picWidth) * 100) + '%'
}

function openWork(work: any) {
  if (work?.id) router.push('/works/' + work.id)
}

async function load() {
  loading.value = true
  try {
    const res: any = await getExhibitionDetail(exhibitionId.value)
    if (res.data.code === 0) {
      exhibition.value = res.data.data?.exhibition || null
      works.value = res.data.data?.works || []
      captions.value = res.data.data?.captions || {}
    } else {
      exhibition.value = null
    }
  } catch {
    exhibition.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
  loadCall()
  clock = window.setInterval(() => { now.value = Date.now() }, 1000)
})
watch(() => route.params.id, load)
</script>

<style scoped>
.ex-detail { padding-bottom: var(--space-8); }

.cover { position: relative; min-height: 380px; background: var(--surface-2); }
.cover-mask { position: absolute; inset: 0; background: linear-gradient(180deg, rgba(16,20,18,.42) 0%, rgba(16,20,18,.78) 100%); }
.cover-inner { position: relative; display: flex; flex-direction: column; justify-content: flex-end; min-height: 380px; padding-bottom: var(--space-6); color: #F6F6F3; }
.cover-inner .eyebrow { margin: 0 0 10px; font-size: var(--font-1); letter-spacing: .22em; color: rgba(246,246,243,.66); }
.cover-inner h1 { margin: 0 0 8px; font-size: var(--font-8); line-height: 1.15; }
.cover-inner .theme { margin: 0 0 16px; color: rgba(246,246,243,.82); font-size: var(--font-4); }
.meta { display: flex; flex-wrap: wrap; align-items: center; gap: 18px; color: rgba(246,246,243,.78); font-size: var(--font-2); }
.meta span { display: inline-flex; align-items: center; gap: 6px; }

.statement { padding: var(--space-6) var(--space-6) 0; }
.statement .eyebrow { margin: 0 0 10px; color: var(--text-3); font-size: 10px; letter-spacing: .2em; }
.statement-text { max-width: 820px; margin: 0; color: var(--text-2); font-size: var(--font-4); line-height: 2; white-space: pre-wrap; }

.section-head { margin-top: var(--space-7); }
.piece { display: flex; flex-direction: column; }
.caption { margin: 8px 2px 0; color: var(--text-2); font-size: var(--font-2); line-height: 1.7; }
.caption .quote { color: var(--accent); font-size: var(--font-5); margin-right: 2px; }


.call-banner { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-top: var(--space-5); padding: 18px 24px; border-radius: var(--radius-lg); background: linear-gradient(100deg, var(--accent-soft), var(--surface-2)); border: 1px solid var(--border); }
.call-main { display: flex; align-items: flex-start; gap: 14px; }
.call-tag { flex: 0 0 auto; padding: 3px 10px; border-radius: var(--radius-pill); background: var(--accent); color: #fff; font-size: 12px; }
.call-main h3 { margin: 0 0 4px; font-size: var(--font-4); }
.call-main p { margin: 0; color: var(--text-2); font-size: var(--font-2); }
.call-side { display: flex; align-items: center; gap: 18px; }
.countdown { display: flex; flex-direction: column; align-items: flex-end; }
.countdown strong { font-size: var(--font-4); font-variant-numeric: tabular-nums; }
.countdown span { color: var(--text-3); font-size: 12px; }
.quota { color: var(--text-2); font-size: var(--font-1); }
.submit-layout { display: grid; grid-template-columns: minmax(0, 1fr) 280px; gap: 20px; }
.work-picks { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; max-height: 420px; overflow-y: auto; padding-right: 4px; }
.pick { position: relative; display: flex; flex-direction: column; gap: 6px; padding: 6px; border: 1px solid var(--border); border-radius: var(--radius); cursor: pointer; transition: color var(--dur-fast) var(--ease), background-color var(--dur-fast) var(--ease), border-color var(--dur-fast) var(--ease), box-shadow var(--dur-fast) var(--ease), transform var(--dur-fast) var(--ease); }
.pick.on { border-color: var(--accent); background: var(--accent-soft); }
.pick img { width: 100%; height: 92px; object-fit: cover; border-radius: var(--radius-sm); }
.pick input { position: absolute; top: 10px; left: 10px; }
.pick-title { font-size: 12px; color: var(--text-2); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.call-pane { display: flex; flex-direction: column; gap: 10px; }
.call-pane .eyebrow { margin: 0; color: var(--text-3); font-size: 10px; letter-spacing: .18em; }
.call-desc { margin: 0; color: var(--text-2); font-size: var(--font-2); line-height: 1.8; max-height: 200px; overflow-y: auto; }
.call-pane .hint { margin: 0; color: var(--text-3); font-size: 12px; }
@media (max-width: 768px) {
  .cover, .cover-inner { min-height: 300px; }
  .cover-inner h1 { font-size: var(--font-7); }
}
</style>

onBeforeUnmount(() => { if (clock !== null) window.clearInterval(clock) })
