<template>
  <EmptyState :title="title || '内容加载失败'" :description="description || '网络可能不太顺畅，稍后再试一次。'" size="md">
    <template #icon><CloudOutlined /></template>
    <a-button type="primary" @click="emit('retry')">重新加载</a-button>
  </EmptyState>
</template>

<script setup lang="ts">
import { CloudOutlined } from '@ant-design/icons-vue'
import EmptyState from '@/components/common/EmptyState.vue'

/**
 * 全站统一错误态：与 EmptyState 同构（图标 + 标题 + 说明 + 操作），
 * 但图标/文案/动作表达"加载失败 + 重试"。三态契约（P0-9）的错误态出口。
 */
withDefaults(defineProps<{ title?: string; description?: string }>(), { title: '', description: '' })
const emit = defineEmits<{ retry: [] }>()
</script>
