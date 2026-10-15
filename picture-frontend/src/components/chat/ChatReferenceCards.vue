<template>
  <div v-if="references.length" class="ref-grid">
    <button v-for="ref in references" :key="ref.type + ref.id" class="ref-card" :class="{ 'work-ref': ref.type === 'work' }" type="button" @click="$emit('open', ref)">
      <div class="ref-thumb">
        <img v-if="ref.thumbnailUrl" :src="ref.thumbnailUrl" alt="" loading="lazy" />
        <span v-else class="ref-icon">
          <TeamOutlined v-if="ref.type === 'photographer'" />
          <PictureOutlined v-else />
        </span>
      </div>
      <div class="ref-body">
        <strong class="ellipsis">{{ ref.title || ('#' + ref.id) }}</strong>
        <span v-if="ref.subtitle" class="muted ellipsis">{{ ref.subtitle }}</span>
        <span v-if="ref.reason" class="ref-reason clamp-2">{{ ref.reason }}</span>
      </div>
    </button>
  </div>
</template>

<script setup lang="ts">
import { PictureOutlined, TeamOutlined } from '@ant-design/icons-vue'
import type { ChatReference } from '@/api/aiChat'

defineProps<{ references: ChatReference[] }>()
defineEmits<{ (e: 'open', ref: ChatReference): void }>()
</script>

<style scoped>
.ref-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 8px; }

.ref-card {
  display: flex; gap: 8px; padding: 8px; text-align: left; cursor: pointer;
  background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius);
  transition: all var(--dur-fast) var(--ease);
}

.ref-card:hover { border-color: var(--accent); box-shadow: var(--shadow-2); transform: translateY(-1px); }

.ref-thumb { width: 48px; height: 48px; flex: 0 0 auto; border-radius: var(--radius-sm); overflow: hidden; background: var(--surface-3); }
.ref-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.ref-icon { display: grid; place-items: center; width: 100%; height: 100%; color: var(--text-3); }

.ref-body { display: flex; flex-direction: column; gap: 2px; min-width: 0; font-size: 11px; }
.ref-body strong { font-size: var(--font-2); }
.ref-reason { color: var(--text-3); line-height: 1.5; }
.work-ref { display:block; padding:0; overflow:hidden; border-radius:6px; }
.work-ref .ref-thumb { width:100%; height:112px; border-radius:0; }
.work-ref .ref-body { padding:9px 10px 11px; }
.work-ref .ref-reason { display:none; }
</style>
