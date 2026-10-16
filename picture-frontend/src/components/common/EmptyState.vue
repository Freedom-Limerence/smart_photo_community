<template>
  <div class="empty-state" :class="[`size-${size}`, { 'is-dashed': dashed }]">
    <span class="empty-illus" aria-hidden="true">
      <slot name="icon"><component :is="icon || PictureOutlined" /></slot>
    </span>
    <p v-if="title" class="empty-title">{{ title }}</p>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.default" class="empty-action"><slot /></div>
  </div>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { PictureOutlined } from '@ant-design/icons-vue'

/**
 * 全站统一空态：图标 + 标题 + 说明 + 操作。
 * 只负责表现，不承载业务逻辑；未传 title 时退化为「图标 + 说明」。
 */
withDefaults(
  defineProps<{
    title?: string
    description?: string
    size?: 'sm' | 'md' | 'lg'
    dashed?: boolean
    icon?: Component
  }>(),
  { title: '', description: '', size: 'md', dashed: false },
)
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-8) var(--space-5);
  border-radius: var(--radius-lg);
  text-align: center;
}

.empty-state.is-dashed {
  border: 1px dashed var(--border-strong);
  background: transparent;
}

.empty-illus {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--surface-2);
  color: var(--text-3);
  font-size: 26px;
  line-height: 1;
}

.empty-title {
  margin: 0;
  color: var(--text-1);
  font-family: var(--font-display);
  font-size: var(--font-5);
  font-weight: 400;
}

.empty-desc {
  max-width: 46ch;
  margin: 0;
  color: var(--text-3);
  font-size: var(--font-2);
  line-height: 1.8;
}

.empty-action { margin-top: var(--space-2); }

/* 尺寸档：侧栏 / 弹层等窄容器用 sm，整页空态用 lg */
.empty-state.size-sm { gap: var(--space-2); padding: var(--space-5) var(--space-3); }
.empty-state.size-sm .empty-illus { width: 44px; height: 44px; font-size: 18px; }
.empty-state.size-sm .empty-desc { font-size: var(--font-1); }

.empty-state.size-lg { gap: var(--space-4); }
.empty-state.size-lg .empty-illus { width: 76px; height: 76px; font-size: 30px; }
.empty-state.size-lg .empty-title { font-size: var(--font-6); }
</style>
