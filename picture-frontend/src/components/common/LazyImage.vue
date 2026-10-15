<template>
  <img v-if="loaded" :src="src" :alt="alt" class="lazy-img is-loaded" loading="lazy" />
  <div v-else class="lazy-img skeleton placeholder" :style="{ paddingBottom: ratio }" />
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = withDefaults(defineProps<{ src?: string; alt?: string; ratio?: string }>(), {
  alt: '',
  ratio: '66%',
})

const loaded = ref(false)
const el = ref<HTMLElement>()
let observer: IntersectionObserver | null = null

function load() {
  if (!props.src) return
  const img = new Image()
  img.onload = () => {
    loaded.value = true
  }
  img.src = props.src
}

onMounted(() => {
  if (!props.src) return
  // 若当前已进入视口则直接加载，否则等滚动到再加载（懒加载 + 渐显）
  observer = new IntersectionObserver((entries) => {
    if (entries.some((e) => e.isIntersecting)) {
      load()
      observer?.disconnect()
    }
  }, { rootMargin: '200px' })
  const target = el.value?.parentElement
  if (target) observer.observe(target)
  else load()
})

watch(() => props.src, () => {
  loaded.value = false
  load()
})

onBeforeUnmount(() => observer?.disconnect())
</script>

<style scoped>
.lazy-img { width: 100%; display: block; }
.placeholder { width: 100%; height: 0; }
</style>
