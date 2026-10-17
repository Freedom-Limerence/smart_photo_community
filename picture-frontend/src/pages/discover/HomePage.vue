<template>
  <div class="discover-page">
    <ContactSheet v-if="mode === 'home'" :works="featuredWorks" :loading="loading && !works.length" @open="openWork" />
    <section id="works-feed" class="feed container">
      <header v-reveal class="feed-heading">
        <div class="feed-intro">
          <span class="feed-no" aria-hidden="true">N°01</span>
          <div>
            <h1 v-if="mode === 'community'">每一种<em>视角</em>，都值得被看见。</h1>
            <h2 v-else>在光影里，<em>遇见</em>彼此。</h2>
            <p>{{ mode === 'community' ? '走进摄影师的作品，发现世界的另一面。' : '从城市漫游到自然光景，看看大家正在记录什么。' }}</p>
          </div>
        </div>
        <div class="feed-tabs" role="group" aria-label="作品排序与范围">
          <button v-for="item in tabs" :key="item.value" type="button" :aria-pressed="activeTab === item.value"
                  :class="{ active: activeTab === item.value }" @click="selectTab(item.value)">{{ item.label }}</button>
        </div>
      </header>
      <div v-if="error" class="feed-error" role="alert"><span>{{ error }}</span><a-button @click="fetchWorks(current)">重新加载</a-button></div>
      <CardSkeleton v-if="loading && !works.length" variant="masonry" :count="6" ratio="72%" />
      <ContactFeed v-else-if="works.length" :works="works" @open="openWork" />
      <EmptyState v-else-if="!error" :title="activeTab === 'following' ? '关注喜欢的摄影师，让灵感在这里相遇' : '社区还在等待第一组作品'"
                  description="分享你眼里的世界，开启新的对话。">
        <a-button type="primary" @click="router.push('/me/create-work')">发布作品</a-button>
      </EmptyState>
      <div v-if="works.length && works.length < total" class="feed-more">
        <a-button size="large" :loading="loading" @click="fetchWorks(current + 1)">继续发现 <ArrowDownOutlined /></a-button>
      </div>
      <div v-else-if="works.length" class="feed-end"><span />每一次停留，都是新的发现<span /></div>
    </section>
    <section v-reveal class="curation-link container">
      <div><h2>让作品之间，<em>发生对话</em>。</h2><p>从一组影像出发，走进不同的摄影主题。</p></div>
      <router-link to="/exhibitions">走进专题展览 <ArrowRightOutlined /></router-link>
    </section>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowDownOutlined, ArrowRightOutlined } from '@ant-design/icons-vue'
import ContactFeed from '@/components/home/ContactFeed.vue'
import ContactSheet from '@/components/home/ContactSheet.vue'
import CardSkeleton from '@/components/common/CardSkeleton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { listWorks } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { loadPhotographyPreview } from '@/utils/photographyPreview'
withDefaults(defineProps<{ mode?: 'home' | 'community' }>(), { mode: 'home' })
const router = useRouter()
const login = useLoginUserStore()
const works = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const loading = ref(true)
const error = ref('')
const activeTab = ref('latest')
const tabs = [{value:'latest',label:'最新发现'}, {value:'hot',label:'热门作品'}, {value:'following',label:'我的关注'}]
const featuredWorks = computed(() => works.value.filter(w => w.coverUrl || w.pictures?.[0]?.url).slice(0,6))
let fetchVersion = 0
function openWork(work: any) { if(work?.id) router.push('/works/' + work.id) }
function selectTab(value: string) {
  if(value === 'following' && !login.loginUser?.id) { message.info('登录后可以查看关注的摄影师'); router.push({path:'/user/login',query:{redirect:'/community'}}); return }
  if(activeTab.value === value) return
  activeTab.value = value
  void fetchWorks(1)
}
async function fetchWorks(page = 1) {
  const version = ++fetchVersion
  loading.value = true
  error.value = ''
  try {
    const preview = await loadPhotographyPreview()
    if(preview) { works.value = [...preview.demoWorks]; total.value = works.value.length; current.value=1; return }
    const res: any = await listWorks({current:page,size:16,sort:activeTab.value==='hot'?'hot':'latest',feed:activeTab.value==='following'?'following':'all'})
    if(version !== fetchVersion) return
    if(res.data.code !== 0) throw new Error(res.data.message || '加载失败')
    const records = res.data.data?.records || []
    works.value = page === 1 ? records : [...works.value,...records]
    total.value = Number(res.data.data?.total || 0)
    current.value = page
  } catch(e: any) { if(version === fetchVersion) error.value = e?.message || '作品暂时无法加载，请稍后重试。' }
  finally { if(version === fetchVersion) loading.value=false }
}
onMounted(() => fetchWorks())
</script>
<style scoped>
.discover-page { padding-bottom:var(--space-8); }
.feed { padding-top:clamp(36px,5vw,80px); }
.feed-heading { display:flex; align-items:flex-end; justify-content:space-between; gap:24px; margin-bottom:36px; }
/* 编辑式大字（视觉冲击力专项）：章节编号 + 超大衬线标题 + 强调字 */
.feed-intro { display:flex; align-items:flex-start; gap:clamp(16px,2vw,32px); min-width:0; }
.feed-no {
  flex:0 0 auto; margin-top:clamp(8px,1vw,18px);
  font:400 clamp(13px,1.2vw,16px)/1 var(--font-display);
  letter-spacing:.22em; color:var(--text-3);
  writing-mode:vertical-rl;
  padding-left:10px; border-left:1px solid var(--border-strong);
}
.feed-heading h1,.feed-heading h2 { margin:0; font:400 clamp(38px,5.5vw,84px)/1.12 var(--font-display); letter-spacing:-.03em; text-wrap:balance; }
.feed-heading em { font-style:normal; color:var(--accent-hover); }
.feed-heading p { color:var(--text-2); margin:14px 0 0; max-width:38ch; }
.feed-tabs { display:flex; flex-shrink:0; gap:22px; }
.feed-tabs button { border:0; border-bottom:2px solid transparent; padding:12px 0; background:none; color:var(--text-2); cursor:pointer; font:inherit; white-space:nowrap; }
.feed-tabs button.active { color:var(--accent-ink); border-color:var(--accent-ink); }
.feed-error { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:24px; padding:20px; background:var(--danger-soft); color:var(--text-1); border-radius:12px; }
.feed-more { text-align:center; padding-top:40px; }
.feed-end { display:flex; align-items:center; justify-content:center; gap:16px; margin-top:48px; color:var(--text-3); font-size:12px; }
.feed-end span { width:32px; height:1px; background:var(--border-strong); }
.curation-link { display:flex; justify-content:space-between; align-items:center; gap:24px; margin-top:64px; padding-top:40px; border-top:1px solid var(--border-strong); }
.curation-link h2 { font:400 clamp(30px,3.6vw,56px)/1.25 var(--font-display); letter-spacing:-.02em; margin:0; }
.curation-link h2 em { font-style:normal; color:var(--accent-hover); }
.curation-link p { color:var(--text-2); margin:10px 0 0; }
.curation-link a { display:flex; gap:24px; align-items:center; padding:14px 0; border-bottom:1px solid var(--text-1); white-space:nowrap; }
@media(max-width:768px) { .feed-heading { flex-direction:column; align-items:flex-start; gap:16px; }.feed-tabs { width:100%; gap:26px; border-bottom:1px solid var(--border); }.curation-link { align-items:flex-start; flex-direction:column; margin-top:40px; }.feed-heading p { font-size:13px; }.feed-no { display:none; } }
</style>
