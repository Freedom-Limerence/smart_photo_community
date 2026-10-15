<template>
  <div class="me-page">
    <div class="section-head">
      <div><p class="eyebrow">MY WORKS</p><h2>我的作品</h2><p>已发布到社区的摄影作品</p></div>
      <a-button type="primary" @click="router.push('/community/create')">发布新作品</a-button>
    </div>
    <a-spin :spinning="loading">
      <MasonryGrid v-if="list.length" :items="list">
        <template #default="{ item }">
          <WorkCard :work="item" @open="(w:any) => router.push('/works/' + w.id)" />
        </template>
      </MasonryGrid>
      <a-empty v-else-if="!loading" description="还没有发布作品" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MasonryGrid from '@/components/common/MasonryGrid.vue'
import WorkCard from '@/components/common/WorkCard.vue'
import { listWorksByAuthor } from '@/api/photoCommunity'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const list = ref<any[]>([])
const loading = ref(false)
const userId = computed(() => loginUserStore.loginUser?.id)

onMounted(async () => {
  if (!userId.value) return
  loading.value = true
  try {
    const res: any = await listWorksByAuthor({ authorId: userId.value, current: 1, size: 30 })
    if (res.data.code === 0) list.value = res.data.data?.records ?? []
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.me-page { display: flex; flex-direction: column; }
</style>
