<template>
  <div class="contact-feed">
    <button v-for="(item, i) in works" :key="item.id" type="button" class="frame"
            :class="frameClass(i)" @click="emit('open', item)">
      <img :src="thumbOf(item)" :alt="item.title || ''" loading="lazy" />
      <span class="frame-no">{{ pad(i + 1) }}</span>
      <div class="frame-meta" aria-hidden="true">
        <strong>{{ item.title || '未命名作品' }}</strong>
        <em>{{ item.author?.userName || '摄影师' }}</em>
      </div>
    </button>
  </div>
</template>

<script setup lang="ts">
const props = defineProps<{ works: any[] }>()
const emit = defineEmits<{ open: [work: any] }>()

function pad(n: number) { return String(n).padStart(2, '0') }

/** 接触印样的节奏：两幅对开 → 三联 → 一幅全景，循环。用主次尺度避免退回等大图库。 */
function frameClass(i: number) {
  const slot = i % 6
  if (slot === 5) return 'wide'
  if (slot <= 1) return 'half'
  return 'third'
}

function thumbOf(work: any) {
  const pic = work?.pictures?.[0]
  return work?.thumbnailUrl || pic?.thumbnailUrl || work?.coverUrl || pic?.url || ''
}
</script>

<style scoped>
.contact-feed {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 2px;
  background: var(--print-line);
  border-top: 1px solid var(--print-line);
  border-bottom: 1px solid var(--print-line);
}

.frame { position: relative; overflow: hidden; width: 100%; padding: 0; border: 0; background: var(--stage); cursor: pointer; font: inherit; text-align: left; }
.frame.half { grid-column: span 3; }
.frame.third { grid-column: span 2; }
.frame.wide { grid-column: span 6; }

.frame img { display: block; width: 100%; object-fit: cover; transition: transform .62s cubic-bezier(.16, 1, .3, 1), filter .4s ease; }
.frame.half img { height: clamp(200px, 27vw, 420px); }
.frame.third img { height: clamp(180px, 22vw, 340px); }
.frame.wide img { height: clamp(150px, 17vw, 280px); }

.frame:hover img, .frame:focus-visible img { transform: scale(1.028); filter: brightness(.88); }

.frame-no {
  position: absolute; top: 8px; left: 10px;
  font-size: 10px; letter-spacing: .1em; font-variant-numeric: tabular-nums;
  color: rgba(245, 246, 244, .82); text-shadow: 0 1px 8px rgba(0, 0, 0, .55);
}

.frame-meta {
  position: absolute; left: 0; right: 0; bottom: 0;
  display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap;
  padding: 26px 12px 10px;
  background: linear-gradient(to top, rgba(20, 24, 21, .78), rgba(20, 24, 21, 0));
  color: #F5F6F4; opacity: 0; transform: translateY(6px);
  transition: opacity .3s cubic-bezier(.16, 1, .3, 1), transform .3s cubic-bezier(.16, 1, .3, 1);
  pointer-events: none;
}
/* 键盘聚焦与悬停同样显出标题与作者，触屏上 wide 帧常显 */
.frame:hover .frame-meta, .frame:focus-visible .frame-meta, .frame.wide .frame-meta { opacity: 1; transform: none; }
.frame-meta strong { font-size: 13px; font-weight: 500; letter-spacing: -.01em; }
.frame-meta em { font-style: normal; font-size: 11px; color: rgba(245, 246, 244, .72); }
.frame.half .frame-meta strong, .frame.wide .frame-meta strong { font-size: 15px; }

.frame:focus-visible { outline: 2px solid var(--accent); outline-offset: -3px; }
.contact-feed ::selection { background: var(--accent); color: #fff; }

@media (max-width: 900px) {
  .contact-feed { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .frame.half, .frame.third { grid-column: span 1; }
  .frame.wide { grid-column: span 2; }
  .frame img, .frame.half img, .frame.third img, .frame.wide img { height: 46vw; }
}
</style>
