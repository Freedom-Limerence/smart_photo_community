<template>
  <!-- 加载完成：真实图片（支持 srcset/sizes 响应式选择） -->
  <img
    v-if="loaded"
    ref="el"
    :src="src"
    :srcset="srcset"
    :sizes="sizes"
    :alt="alt"
    class="lazy-img is-loaded"
    loading="lazy"
    decoding="async"
  />
  <!-- 失败：占位 + 文案 -->
  <div v-else-if="failed" ref="el" class="lazy-img is-failed" :style="{ paddingBottom: ratio }" role="img" :aria-label="alt">
    <span class="fail-text">图片暂不可用</span>
  </div>
  <!-- 未加载：有 LQIP 缩略图则模糊递进（先见轮廓），否则骨架占位 -->
  <div v-else-if="preview" ref="el" class="lazy-img is-preview" :style="{ paddingBottom: ratio }">
    <img :src="preview" :alt="''" aria-hidden="true" class="preview-img" loading="lazy" decoding="async" />
  </div>
  <div v-else ref="el" class="lazy-img skeleton placeholder" :style="{ paddingBottom: ratio }" />
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    src?: string
    alt?: string
    ratio?: string
    /** LQIP 模糊递进：加载完成前先显示的缩略图（通常传 thumbnailUrl） */
    preview?: string
    /** 响应式候选源，如 "a.webp 1x, b@2x.webp 2x" */
    srcset?: string
    /** 配合 srcset 的 sizes 表达式 */
    sizes?: string
  }>(),
  { alt: '', ratio: '66%', preview: '', srcset: '', sizes: '' },
)

const loaded = ref(false)
const failed = ref(false)
const el = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

function load() {
  if (!props.src) return
  const img = new Image()
  img.decoding = 'async'
  if (props.srcset) img.srcset = props.srcset
  img.onload = () => {
    loaded.value = true
    failed.value = false
  }
  img.onerror = () => {
    // 有 LQIP 时保留模糊缩略图（比失败占位更有用），标记为已"加载完"阶段
    loaded.value = !!props.preview
    failed.value = !props.preview
  }
  img.src = props.src
}

/** 进入视口前只渲染骨架 / LQIP，避免首屏同时发起大量大图请求 */
async function observe() {
  observer?.disconnect()
  observer = null
  if (!props.src) return
  await nextTick()
  const target = el.value
  if (!target || typeof IntersectionObserver === 'undefined') {
    load()
    return
  }
  observer = new IntersectionObserver(
    (entries) => {
      if (entries.some((e) => e.isIntersecting)) {
        load()
        observer?.disconnect()
        observer = null
      }
    },
    { rootMargin: '320px' },
  )
  observer.observe(target)
}

onMounted(() => {
  void observe()
})

watch(
  () => [props.src, props.srcset],
  () => {
    loaded.value = false
    failed.value = false
    void observe()
  },
)

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
})
</script>

<style scoped>
.lazy-img { width: 100%; display: block; }
.placeholder { width: 100%; height: 0; }

/* LQIP 模糊递进：缩略图铺满占位区，轻微模糊 + 缓慢清晰 */
.is-preview { position: relative; height: 0; overflow: hidden; background: var(--surface-2); }
.preview-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  filter: blur(14px);
  transform: scale(1.04);
}

.is-failed {
  position: relative;
  height: 0;
  background: var(--surface-2);
  opacity: 1;
}

.fail-text {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: var(--text-3);
  font-size: var(--font-1);
}
</style>
