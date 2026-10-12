<template>
  <div id="basicLayout">
    <a-layout class="app-shell">
      <a-layout-header class="header"><GlobalHeader /></a-layout-header>
      <a-layout class="body-layout">
        <GlobalSider class="sider" />
        <a-layout-content class="content"><router-view /></a-layout-content>
      </a-layout>
      <a-layout-footer class="footer"><span>智能摄影云社区</span><span>摄影资产管理 · AI 编目 · 作品交流</span></a-layout-footer>
    </a-layout>
  </div>
</template>
<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalSider from '@/components/GlobalSider.vue'
</script>
<style scoped>
#basicLayout,.app-shell { min-height:100vh; background:#f5f7f5; color:#20322e; }
.header { position:sticky; z-index:20; top:0; height:68px; padding:0 28px; line-height:68px; background:rgba(255,255,255,.96); border-bottom:1px solid #e6ebe7; box-shadow:0 2px 14px rgba(28,54,42,.04); }
.body-layout { flex:1; min-height:calc(100vh - 122px); background:transparent; }
.sider { flex:0 0 auto; background:#fff; border-right:1px solid #e6ebe7; }
.content { min-width:0; padding:30px clamp(18px,4vw,54px) 44px; background:#f5f7f5; }
.footer { display:flex; justify-content:center; gap:16px; padding:18px 24px; color:#829087; font-size:12px; background:#fff; border-top:1px solid #e6ebe7; }
@media (max-width:768px) { .header{padding:0 14px}.content{padding:20px 14px 32px}.footer{flex-direction:column;align-items:center;gap:4px} }
</style>
