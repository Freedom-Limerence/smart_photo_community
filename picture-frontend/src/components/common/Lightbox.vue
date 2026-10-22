<template>
  <Teleport to="body">
    <transition name="lb-fade">
      <div v-if="open" class="lightbox" role="dialog" aria-modal="true" :aria-label="'作品灯箱 ' + (index + 1) + '/' + works.length" @click.self="close">
        <button type="button" class="lb-close" aria-label="关闭灯箱" @click="close"><CloseOutlined /></button>

        <transition name="lb-slide" mode="out-in">
          <img v-if="current" :key="index" class="lb-img" :src="srcOf(current)" :alt="current.title || '作品'" />
        </transition>

        <button v-if="works.length > 1" type="button" class="lb-arrow lb-prev" aria-label="上一幅" @click.stop="step(-1)"><LeftOutlined /></button>
        <button v-if="works.length > 1" type="button" class="lb-arrow lb-next" aria-label="下一幅" @click.stop="step(1)"><RightOutlined /></button>

        <footer v-if="current" class="lb-meta">
          <div class="lb-copy">
            <strong>{{ current.title || '未命名作品' }}</strong>
            <span class="lb-sub">
              {{ current.author?.userName || '摄影师' }}
              <template v-if="exifText"> · {{ exifText }}</template>
            </span>
          </div>
          <div class="lb-right">
            <span class="lb-count">{{ index + 1 }} / {{ works.length }}</span>
            <button v-if="current.id" type="button" class="lb-detail" @click="emit('detail', current)">
              查看作品详情 <ArrowRightOutlined />
            </button>
          </div>
        </footer>
      </div>
    </transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, watch } from 'vue'
import { ArrowRightOutlined, CloseOutlined, LeftOutlined, RightOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  works: any[]
  index: number
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [v: boolean]
  'update:index': [i: number]
  detail: [work: any]
}>()

const current = computed(() => props.works[props.index] || null)

function srcOf(w: any) {
  const p = w?.pictures?.[0]
  return p?.url || w?.coverUrl || p?.thumbnailUrl || ''
}

const exifText = computed(() => {
  const p = current.value?.pictures?.[0]
  const bits: string[] = []
  if (p?.shotTime || p?.createTime) bits.push(String(p.shotTime || p.createTime).slice(0, 10))
  if (p?.picColor) bits.push('主色 ' + p.picColor)
  return bits.join(' · ')
})

function close() { emit('update:open', false) }
function step(d: number) {
  const n = props.works.length
  if (!n) return
  emit('update:index', (props.index + d + n) % n)
}

function onKey(e: KeyboardEvent) {
  if (!props.open) return
  if (e.key === 'Escape') close()
  else if (e.key === 'ArrowLeft') step(-1)
  else if (e.key === 'ArrowRight') step(1)
}

onMounted(() => window.addEventListener('keydown', onKey))
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))

// 打开时锁滚动
watch(() => props.open, (v) => {
  document.body.style.overflow = v ? 'hidden' : ''
})
onBeforeUnmount(() => { document.body.style.overflow = '' })
</script>

<style scoped>
.lightbox {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(4, 5, 8, .95);
  display: grid; grid-template-rows: 1fr auto;
}
.lb-img { grid-row: 1; place-self: center; max-width: 92vw; max-height: 82vh; object-fit: contain; box-shadow: 0 30px 90px rgba(0,0,0,.7); }
.lb-close { position: absolute; top: 20px; right: 24px; z-index: 2; width: 42px; height: 42px; border-radius: 50%; border: 1px solid rgba(255,255,255,.2); background: rgba(15,17,20,.6); color: var(--text-1); cursor: pointer; font-size: 15px; transition: border-color .2s; }
.lb-close:hover { border-color: rgba(163,182,168,.6); }
.lb-arrow { position: absolute; top: 50%; transform: translateY(-50%); z-index: 2; width: 48px; height: 48px; border-radius: 50%; border: 1px solid rgba(255,255,255,.18); background: rgba(15,17,20,.55); color: var(--text-1); cursor: pointer; font-size: 15px; transition: border-color .2s, background .2s; }
.lb-arrow:hover { border-color: rgba(163,182,168,.6); background: rgba(25,28,25,.75); }
.lb-prev { left: 22px; }
.lb-next { right: 22px; }
.lb-meta { grid-row: 2; display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 18px clamp(20px, 5vw, 60px) 26px; border-top: 1px solid rgba(255,255,255,.08); }
.lb-copy { display: flex; flex-direction: column; gap: 4px; min-width: 0; }
.lb-copy strong { color: var(--text-1); font-family: var(--font-display, Georgia, serif); font-weight: 400; font-size: 19px; }
.lb-sub { color: var(--text-3); font-size: 12px; letter-spacing: .05em; }
.lb-right { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.lb-count { color: var(--text-3); font-size: 13px; font-variant-numeric: tabular-nums; }
.lb-detail { display: inline-flex; align-items: center; gap: 8px; padding: 9px 18px; border-radius: 999px; border: 1px solid rgba(255,255,255,.16); background: rgba(15,17,20,.6); color: var(--text-1); cursor: pointer; font-size: 12.5px; letter-spacing: .05em; transition: border-color .2s; }
.lb-detail:hover { border-color: rgba(163,182,168,.6); }

.lb-fade-enter-active, .lb-fade-leave-active { transition: opacity .3s ease; }
.lb-fade-enter-from, .lb-fade-leave-to { opacity: 0; }
.lb-slide-enter-active, .lb-slide-leave-active { transition: opacity .22s ease, transform .22s ease; }
.lb-slide-enter-from { opacity: 0; transform: scale(.985); }
.lb-slide-leave-to { opacity: 0; }

@media (max-width: 640px) {
  .lb-img { max-width: 96vw; max-height: 70vh; }
  .lb-arrow { width: 40px; height: 40px; }
}
</style>
