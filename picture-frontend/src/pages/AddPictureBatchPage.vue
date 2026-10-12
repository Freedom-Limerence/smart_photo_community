<template>
  <!-- @author 程序员鱼皮 <a href="https://www.codefather.cn">编程导航原创项目</a> -->
  <div id="addPictureBatchPage">
    <h2 style="margin-bottom: 16px">批量创建</h2>
    <!-- 图片信息表单 -->
    <a-form name="formData" layout="vertical" :model="formData" @finish="handleSubmit">
      <a-form-item name="searchText" label="关键词">
        <a-input v-model:value="formData.searchText" placeholder="请输入关键词" allow-clear />
      </a-form-item>
      <a-form-item name="count" label="抓取数量">
        <a-input-number
          v-model:value="formData.count"
          placeholder="请输入数量"
          style="min-width: 180px"
          :min="1"
          :max="30"
          allow-clear
        />
      </a-form-item>
      <a-form-item name="namePrefix" label="名称前缀">
        <a-input
          v-model:value="formData.namePrefix"
          placeholder="请输入名称前缀，会自动补充序号"
          allow-clear
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%" :loading="loading">
          执行任务
        </a-button>
      </a-form-item>
    </a-form>
    <a-alert
      v-if="batchResult"
      class="result-summary"
      type="info"
      show-icon
      :message="`处理完成：成功 ${batchResult.successCount || 0} 张，失败 ${batchResult.failureCount || 0} 张`"
    />
    <a-list v-if="batchResult?.items?.length" bordered :data-source="batchResult.items" class="result-list">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-space direction="vertical" size="small">
            <span>{{ item.name || item.sourceUrl }}</span>
            <a-tag :color="item.status === 'SUCCEEDED' ? 'success' : 'error'">
              {{ item.status === 'SUCCEEDED' ? `成功，图片 ID：${item.pictureId}` : '失败' }}
            </a-tag>
            <span v-if="item.errorMessage" class="error-text">{{ item.errorMessage }}</span>
          </a-space>
        </a-list-item>
      </template>
    </a-list>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  uploadPictureByBatchUsingPost,
} from '@/api/pictureController.ts'

const formData = reactive<API.PictureUploadByBatchRequest>({
  count: 10,
})
// 提交任务状态
const loading = ref(false)

const batchResult = ref<API.PictureBatchUploadResult>()

/**
 * 提交表单
 * @param values
 */
const handleSubmit = async (values: any) => {
  loading.value = true
  try {
    const res = await uploadPictureByBatchUsingPost({ ...formData })
  // 操作成功
    if (res.data.code === 0 && res.data.data) {
      batchResult.value = res.data.data
      message.success(`处理完成，成功 ${res.data.data.successCount || 0} 张`)
    } else message.error('创建失败，' + res.data.message)
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '批量处理失败，请稍后重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
#addPictureBatchPage {
  max-width: 720px;
  margin: 0 auto;
}
.result-summary { margin-top: 20px; }
.result-list { margin-top: 12px; }
.error-text { color: #c53d3d; }
</style>
