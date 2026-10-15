<template>
  <div class="me-page">
    <div class="section-head">
      <div>
        <p class="eyebrow">MY FAVORITES</p>
        <h2>我的收藏</h2>
        <p>你收藏与点赞过的摄影作品</p>
      </div>
      <a-radio-group v-model:value="tab" button-style="solid" size="small">
        <a-radio-button value="favorites">收藏 {{ favorites.length }}</a-radio-button>
        <a-radio-button value="likes">喜欢 {{ likes.length }}</a-radio-button>
      </a-radio-group>
    </div>

    <a-spin :spinning="loading">
      <MasonryGrid v-if="list.length" :items="list">
        <template #default="{ item }">
          <WorkCard :work="item" @open="(w:any) => router.push('/works/' + w.id)" />
        </template>
      </MasonryGrid>
      <a-empty v-else-if="!loading" :description="tab === 'favorites' ? '还没有收藏任何作品' : '还没有点赞任何作品'">
        <a-button type="primary" @click="router.push('/community')">去社区发现作品</a-button>
      </a-empty>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { listMyFavorites, listMyLikes } from '@/api/photoCommunity'

const router = useRouter()
const tab = ref<'favorites' | 'likes'>('favorites')
const favorites = ref<any[]>([])
const likes = ref<any[]>([])
const loading = ref(false)

const list = computed(() => (tab.value === 'favorites' ? favorites.value : likes.value))

async function load() {
  loading.value = true
  try {
    const [favRes, likeRes]: any[] = await Promise.all([listMyFavorites(60), listMyLikes(60)])
    if (favRes.data.code === 0) favorites.value = favRes.data.data || []
    if (likeRes.data.code === 0) likes.value = likeRes.data.data || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.me-page { display: flex; flex-direction: column; }
</style>
