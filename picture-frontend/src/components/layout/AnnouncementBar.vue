<template>
  <transition name="bar">
    <div v-if="visible" class="ann-bar" :class="levelClass">
      <div class="container ann-inner">
        <span class="ann-tag">{{ levelText }}</span>
        <strong class="ann-title">{{ current?.title }}</strong>
        <span class="ann-content ellipsis">{{ current?.content }}</span>
        <button class="ann-close" type="button" @click="dismiss">×</button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getLatestAnnouncements } from '@/api/notification'

const current = ref<any>(null)
const visible = ref(false)

const levelText = computed(() => ({ INFO: '公告', WARNING: '提醒', IMPORTANT: '重要' } as any)[current.value?.level] || '公告')
const levelClass = computed(() => 'level-' + String(current.value?.level || 'INFO').toLowerCase())

function dismiss() {
  if (current.value?.id) localStorage.setItem('dsh.ann.dismissed', String(current.value.id))
  visible.value = false
}

onMounted(async () => {
  try {
    const res: any = await getLatestAnnouncements(3)
    if (res.data.code !== 0) return
    const list = res.data.data || []
    if (!list.length) return
    const first = list[0]
    if (localStorage.getItem('dsh.ann.dismissed') === String(first.id)) return
    current.value = first
    visible.value = true
  } catch {
    // 公告拉取失败不影响页面
  }
})
</script>

<style scoped>
.ann-bar { background: var(--surface-2); border-bottom: 1px solid var(--border); font-size: var(--font-2); }
.ann-bar.level-warning { background: #FBF6EC; }
.ann-bar.level-important { background: var(--danger-soft); }
.ann-inner { display: flex; align-items: center; gap: 10px; height: 40px; }
.ann-tag { flex: 0 0 auto; padding: 1px 8px; border-radius: var(--radius-pill); background: var(--surface); border: 1px solid var(--border); color: var(--text-2); font-size: 11px; }
.ann-title { flex: 0 0 auto; }
.ann-content { flex: 1 1 auto; color: var(--text-2); min-width: 0; }
.ann-close { flex: 0 0 auto; width: 22px; height: 22px; border: none; border-radius: 50%; background: transparent; color: var(--text-3); font-size: 16px; line-height: 1; cursor: pointer; }
.ann-close:hover { background: var(--surface-3); color: var(--text-1); }
.bar-enter-active, .bar-leave-active { transition: opacity var(--dur) var(--ease); }
.bar-enter-from, .bar-leave-to { opacity: 0; }
</style>
