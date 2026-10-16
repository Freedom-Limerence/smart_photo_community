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
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CalendarOutlined, EyeOutlined, PictureOutlined } from '@ant-design/icons-vue'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { getExhibitionDetail } from '@/api/photoCommunity'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const exhibition = ref<any>(null)
const works = ref<any[]>([])
const captions = ref<Record<string, string>>({})

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

onMounted(load)
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

@media (max-width: 768px) {
  .cover, .cover-inner { min-height: 300px; }
  .cover-inner h1 { font-size: var(--font-7); }
}
</style>
