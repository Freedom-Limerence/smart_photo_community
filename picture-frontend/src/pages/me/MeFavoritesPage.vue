<template>
  <div class="me-page">
    <div class="section-head">
      <div><p class="eyebrow">MY FAVORITES</p><h2>我的收藏</h2><p>你收藏过的摄影作品</p></div>
    </div>
    <a-empty description="收藏列表接口将随「迭代 2」交付">
      <p class="muted tip">现在可以在作品详情页收藏，收藏记录已入库（work_favorite 表）；列表接口与通知中心一起开发。</p>
      <a-button type="primary" @click="router.push('/community')">去社区发现作品</a-button>
    </a-empty>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<style scoped>
.tip { max-width: 460px; margin: 0 auto var(--space-3); }
</style>
