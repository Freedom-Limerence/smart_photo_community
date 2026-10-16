<template>
  <div class="contact-feed">
    <article v-for="(item, i) in works" :key="item.id" class="frame" :class="frameClass(i)">
      <button type="button" class="frame-image" :aria-label="'欣赏作品：' + (item.title || '未命名作品')" @click="emit('open',item)">
        <img :src="imageOf(item)" :alt="item.title || '摄影作品'" loading="lazy" decoding="async" />
        <span class="frame-action">欣赏作品 <ArrowRightOutlined /></span>
        <span v-if="item.pictures?.length > 1" class="frame-count">{{ item.pictures.length }} 张</span>
      </button>
      <div class="frame-caption"><div><h3>{{ item.title || '未命名作品' }}</h3><span>{{ item.author?.userName || '摄影师' }}</span></div><span class="frame-no" aria-hidden="true">{{ String(i+1).padStart(2,'0') }}</span></div>
    </article>
  </div>
</template>
<script setup lang="ts">
import { ArrowRightOutlined } from '@ant-design/icons-vue'
defineProps<{works:any[]}>()
const emit = defineEmits<{open:[work:any]}>()
function frameClass(i:number) { return ['lead','portrait','third','third','third','panorama'][i%6] }
function imageOf(w:any) { return w.coverUrl || w.pictures?.[0]?.url || w.thumbnailUrl || w.pictures?.[0]?.thumbnailUrl || '' }
</script>
<style scoped>
.contact-feed { display:grid; grid-template-columns:repeat(6,minmax(0,1fr)); column-gap:24px; row-gap:40px; }
.frame { min-width:0; }.lead { grid-column:span 4; }.portrait { grid-column:span 2; }.third { grid-column:span 2; }.panorama { grid-column:span 6; }
.frame-image { position:relative; display:block; overflow:hidden; width:100%; padding:0; border:0; background:var(--surface-3); cursor:pointer; aspect-ratio:1.5; }
.lead .frame-image,.portrait .frame-image { height:clamp(270px,32vw,480px); aspect-ratio:auto; }.panorama .frame-image { aspect-ratio:2.75; }
.frame-image img { width:100%; height:100%; object-fit:cover; display:block; transition:transform 600ms var(--ease); }
.frame-image:hover img { transform:scale(1.025); }
.frame-action { position:absolute; left:16px; bottom:16px; display:flex; align-items:center; gap:24px; padding:11px 15px; background:var(--surface); color:var(--text-1); font-size:12px; opacity:0; transform:translateY(6px); transition:opacity var(--dur) var(--ease),transform var(--dur) var(--ease); }
.frame-image:hover .frame-action,.frame-image:focus-visible .frame-action { opacity:1; transform:none; }
.frame-count { position:absolute; right:12px; top:12px; padding:4px 8px; background:rgba(15,20,17,.75); color:white; font-size:11px; }
.frame-caption { display:flex; justify-content:space-between; gap:12px; margin-top:12px; }.frame-caption h3 { font-size:15px; font-weight:500; margin:0 0 4px; overflow-wrap:anywhere; }.frame-caption span { font-size:12px; color:var(--text-3); }.frame-no { font-variant-numeric:tabular-nums; }
@media(max-width:760px) { .contact-feed { grid-template-columns:repeat(2,minmax(0,1fr)); gap:28px 14px; }.lead,.panorama { grid-column:span 2; }.portrait,.third { grid-column:span 1; }.lead .frame-image { height:auto; aspect-ratio:1.35; }.portrait .frame-image,.third .frame-image { height:auto; aspect-ratio:.8; }.panorama .frame-image { aspect-ratio:1.6; }.frame-caption h3 { font-size:14px; }.frame-no { display:none; } }
@media(hover:none) { .frame-action { opacity:1; transform:none; padding:7px 10px; left:10px; bottom:10px; }.frame-action .anticon { display:none; } }
</style>
