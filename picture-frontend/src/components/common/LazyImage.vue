<template>
  <img v-if="loaded" ref="el" :src="src" :alt="alt" class="lazy-img is-loaded" loading="lazy" decoding="async" />
  <div v-else-if="failed" ref="el" class="lazy-img is-failed" :style="{ paddingBottom: ratio }" role="img" :aria-label="alt">
    <span class="fail-text">图片暂不可用</span>
  </div>
  <div v-else ref="el" class="lazy-img skeleton placeholder" :style="{ paddingBottom: ratio }" />
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{ src?: string; alt?: string; ratio?: string }>(), {
  alt: '',
  ratio: '66%',
})

const loaded = ref(false)
const failed = ref(false)
const el = ref<HTMLElement | null>(null)
let observer: IntersectionObserver | null = null

function load() {
  if (!props.src) return
  const img = new Image()
  img.decoding = 'async'
  img.onload = () => {
    loaded.value = true
    failed.value = false
  }
  img.onerror = () => {
    loaded.value = false
    failed.value = true
  }
  img.src = props.src
}

/** 进入视口前只渲染骨架，避免首屏同时发起大量图片请求 */
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
  () => props.src,
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
