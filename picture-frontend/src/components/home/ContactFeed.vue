<template>
  <div class="contact-feed">
    <article v-for="(item, i) in works" :key="item.id" v-reveal:clip="(i % 6) * 60" v-tilt="5" class="frame" :class="frameClass(i)">
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
function frameClass(i:number) { return ['third','third','third','lead','portrait','panorama'][i%6] }
function imageOf(w:any) { return w.coverUrl || w.pictures?.[0]?.url || w.thumbnailUrl || w.pictures?.[0]?.thumbnailUrl || '' }
</script>
<style scoped>
.contact-feed { display:grid; grid-template-columns:repeat(6,minmax(0,1fr)); column-gap:24px; row-gap:40px; }
.frame { min-width:0; position:relative; }
/* 卡内光斑：跟随鼠标的柔光（v-tilt 写入 --glow-x/y），motionsites 式 hover 魔法 */
.frame::after {
  content:''; position:absolute; inset:0; z-index:1; pointer-events:none;
  opacity:0; transition:opacity var(--dur) var(--ease);
  background: radial-gradient(340px circle at var(--glow-x,50%) var(--glow-y,50%), rgba(143,163,148,.10), transparent 65%);
}
.frame:hover::after { opacity:1; }.lead { grid-column:span 4; }.portrait { grid-column:span 2; }.third { grid-column:span 2; }.panorama { grid-column:span 6; }
.frame-image { position:relative; display:block; overflow:hidden; width:100%; padding:0; border:0; background:var(--surface-3); cursor:pointer; aspect-ratio:1.5; }
.lead .frame-image,.portrait .frame-image { height:clamp(270px,32vw,480px); aspect-ratio:auto; }.panorama .frame-image { aspect-ratio:2.75; }
.frame-image img { width:100%; height:100%; object-fit:cover; display:block; filter:saturate(.82); transition:transform 700ms var(--ease), filter 500ms var(--ease); }
/* 滚动视差（Chrome 渐进增强，不支持则静止）：图片随滚动轻微漂移，画面活起来 */
@supports (animation-timeline: view()) {
  .frame-image img { animation: frame-parallax linear both; animation-timeline: view(); animation-range: entry 0% cover 55%; }
  @keyframes frame-parallax { from { transform: translateY(-4%) scale(1.06); } to { transform: translateY(0) scale(1.02); } }
}
.frame-image:hover img { transform:scale(1.035); filter:saturate(1.05) brightness(.55); }
.frame-action { position:absolute; left:16px; bottom:16px; display:flex; align-items:center; gap:24px; padding:11px 15px; background:var(--surface); color:var(--text-1); font-size:12px; opacity:0; transform:translateY(6px); transition:opacity var(--dur) var(--ease),transform var(--dur) var(--ease); }
.frame-image:hover .frame-action,.frame-image:focus-visible .frame-action { opacity:1; transform:none; }
.frame-count { position:absolute; right:12px; top:12px; padding:4px 8px; background:rgba(15,20,17,.75); color:white; font-size:12px; }
.frame-caption {
  position:absolute; left:0; right:0; bottom:0; z-index:2;
  display:flex; justify-content:space-between; align-items:end; gap:12px;
  padding:44px 20px 18px; margin:0;
  background:linear-gradient(to top, rgba(10,10,12,.88), transparent);
  opacity:0; transform:translateY(14px);
  transition:opacity var(--dur) var(--ease), transform var(--dur) var(--ease);
  pointer-events:none;
}
.frame:hover .frame-caption, .frame:focus-within .frame-caption { opacity:1; transform:none; }
.frame-caption h3 { font-family:var(--font-display); font-size:clamp(18px,1.8vw,26px); font-weight:400; margin:0 0 4px; overflow-wrap:anywhere; color:var(--text-media); }
.frame-caption span { font-size:12px; color:color-mix(in srgb, var(--text-media) 50%, transparent,.66); }
/* 编辑感编号：衬线淡色大字，压在卡片右下 */
.frame-no { font-family:var(--font-display); font-size:clamp(20px,2vw,30px); line-height:1; color:color-mix(in srgb, var(--text-media) 50%, transparent,.5); font-variant-numeric:tabular-nums; }
@media(max-width:768px) { .contact-feed { grid-template-columns:repeat(2,minmax(0,1fr)); gap:28px 14px; }.lead,.panorama { grid-column:span 2; }.portrait,.third { grid-column:span 1; }.lead .frame-image { height:auto; aspect-ratio:1.35; }.portrait .frame-image,.third .frame-image { height:auto; aspect-ratio:.8; }.panorama .frame-image { aspect-ratio:1.6; }.frame-caption h3 { font-size:14px; }.frame-no { display:none; } }
@media(hover:none) { .frame-action { opacity:1; transform:none; padding:7px 10px; left:10px; bottom:10px; }.frame-action .anticon { display:none; }
  .frame-caption { opacity:1; transform:none; } }
</style>
