<template>
  <div v-if="enabled" class="preview-bar" role="status">
    <span>摄影演示 <span class="preview-note">· 仅开发环境，示例内容</span></span>
    <button type="button" @click="leave">返回真实社区 <ArrowRightOutlined /></button>
  </div>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRightOutlined } from '@ant-design/icons-vue'
const route = useRoute()
const router = useRouter()
const enabled = computed(() => import.meta.env.DEV && route.query.demo === 'photography')
function leave() { window.location.assign(router.resolve('/').href) }
</script>
<style scoped>
.preview-bar { display:flex; justify-content:space-between; gap:12px; padding:8px var(--gutter); background:var(--accent-soft); color:var(--accent-ink); font-size:12px; }
.preview-bar button { border:0; padding:0; background:none; color:inherit; cursor:pointer; font:inherit; }
@media(max-width:460px) { .preview-note { display:none; } }
</style>
