<template>
  <button
    type="button"
    :class="{ on: liked }"
    :disabled="disabled || pending"
    :aria-pressed="liked"
    :aria-label="liked ? '取消点赞' : '点赞'"
    @click="emit('toggle')"
  >
    <span class="lb-icon" :class="{ pop: popping }">
      <LikeFilled v-if="liked" />
      <LikeOutlined v-else />
      <span v-if="bursting" class="lb-burst" aria-hidden="true"></span>
    </span>
    <span class="lb-count">{{ display }}</span>
  </button>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { LikeFilled, LikeOutlined } from '@ant-design/icons-vue'
import { useCountUp } from '@/composables/useCountUp'

/**
 * 点赞按钮（P2-2 微爆点）：点赞瞬间图标弹性放大 + 8 点粒子径向扩散 + 计数 320ms 滚动。
 * 取消点赞：图标收缩回落、无粒子。prefers-reduced-motion：只做颜色变化，计数直跳。
 * 视觉样式（尺寸/配色）由父级 class 控制（如 .quick-btn / .act），本组件只负责行为与动效。
 */
const props = withDefaults(
  defineProps<{ liked: boolean; count?: number; pending?: boolean; disabled?: boolean }>(),
  { count: 0, pending: false, disabled: false },
)
const emit = defineEmits<{ toggle: [] }>()

const display = useCountUp(() => props.count)
const popping = ref(false)
const bursting = ref(false)
let popTimer = 0
let burstTimer = 0

watch(
  () => props.liked,
  (to, from) => {
    if (!to || from) return
    popping.value = true
    bursting.value = true
    window.clearTimeout(popTimer)
    window.clearTimeout(burstTimer)
    popTimer = window.setTimeout(() => (popping.value = false), 220)
    burstTimer = window.setTimeout(() => (bursting.value = false), 400)
  },
)
</script>

<style scoped>
.lb-icon {
  position: relative;
  display: inline-flex;
  align-items: center;
}
.lb-icon.pop :deep(.anticon) {
  animation: lb-pop 200ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes lb-pop {
  0% { transform: scale(1); }
  45% { transform: scale(1.25); }
  100% { transform: scale(1); }
}
/* 取消点赞：收缩回落 */
.lb-icon:not(.pop) :deep(.anticon) {
  transition: transform var(--dur-fast) var(--ease);
}

/* 8 点粒子：单元素 box-shadow 定位 8 个点，transform 缩放实现径向扩散 */
.lb-burst {
  position: absolute;
  left: 50%;
  top: 50%;
  width: 4px;
  height: 4px;
  margin: -2px 0 0 -2px;
  border-radius: 50%;
  pointer-events: none;
  background: transparent;
  box-shadow:
    0 -14px 0 var(--accent),
    10px -10px 0 var(--accent),
    14px 0 0 var(--accent),
    10px 10px 0 var(--accent),
    0 14px 0 var(--accent),
    -10px 10px 0 var(--accent),
    -14px 0 0 var(--accent),
    -10px -10px 0 var(--accent);
  animation: lb-burst 320ms var(--ease) forwards;
}
@keyframes lb-burst {
  from { transform: scale(0.2); opacity: 0.9; }
  to { transform: scale(1.3); opacity: 0; }
}

@media (prefers-reduced-motion: reduce) {
  .lb-icon.pop :deep(.anticon),
  .lb-burst {
    animation: none;
  }
  .lb-burst {
    display: none;
  }
}
</style>
