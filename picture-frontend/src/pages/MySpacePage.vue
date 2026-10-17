<template>
  <div id="mySpacePage" class="container" style="padding: 40px 20px 64px">
    <a-card class="redirect-card">
      <a-spin v-if="loading" size="large" />
      <a-result v-else-if="error" status="warning" title="个人空间暂时无法打开" sub-title="请确认服务已启动并重试。">
        <template #extra><a-button type="primary" @click="checkUserSpace">重新加载</a-button></template>
      </a-result>
      <a-result v-else status="info" title="正在查找你的个人空间" sub-title="找到后会自动进入空间。" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore.ts'
import { listSpaceVoByPageUsingPost } from '@/api/spaceController.ts'
import { message } from 'ant-design-vue'
import { onMounted, ref } from 'vue'
import { SPACE_TYPE_ENUM } from '@/constants/space.ts'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const loading = ref(true)
const error = ref(false)

// 检查用户是否有个人空间
const checkUserSpace = async () => {
  loading.value = true
  error.value = false
  // 用户未登录，则直接跳转到登录页面
  const loginUser = loginUserStore.loginUser
  if (!loginUser?.id) {
    router.replace('/user/login')
    return
  }
  // 如果用户已登录，会获取该用户已创建的空间
  try {
    const res = await listSpaceVoByPageUsingPost({
      userId: loginUser.id,
      current: 1,
      pageSize: 1,
      spaceType: SPACE_TYPE_ENUM.PRIVATE,
    })
    if (res.data.code === 0) {
      const records = res.data.data?.records ?? []
      if (records.length > 0) {
        router.replace(`/space/${records[0]?.id}`)
      } else {
        router.replace('/add_space')
        message.warn('请先创建空间')
      }
    } else {
      error.value = true
      message.error('加载我的空间失败，' + res.data.message)
    }
  } catch {
    error.value = true
    message.error('加载我的空间失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 在页面加载时检查用户空间
onMounted(() => {
  checkUserSpace()
})
</script>

<style scoped>
#mySpacePage { max-width: 720px; margin: 48px auto; }
.redirect-card { min-height: 260px; display: grid; place-items: center; border-color: var(--border); }
</style>
