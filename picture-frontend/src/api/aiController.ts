import request from '@/request'

/** 生成摄影编目（阶段 2.3） */
export function catalogPicture(pictureId: string | number) {
  return request('/api/ai/catalog', { method: 'POST', params: { pictureId } })
}

/** EXIF 提取任务（阶段 2.2） */
export function startExifTask(pictureId: string | number) {
  return request('/api/ai/task/exif', { method: 'POST', params: { pictureId } })
}

/** 我的编目任务列表（阶段 2.1） */
export function listMyAiTasks(params: { current?: number; size?: number; status?: string }) {
  return request('/api/ai/task/page/my', { method: 'GET', params })
}

/** 重试失败任务（阶段 2.1） */
export function retryAiTask(id: string | number) {
  return request('/api/ai/task/retry', { method: 'POST', params: { id } })
}

/** 取消任务（阶段 2.1） */
export function cancelAiTask(id: string | number) {
  return request('/api/ai/task/cancel', { method: 'POST', params: { id } })
}

/** 查询摄影元数据（阶段 2.2） */
export function getAiMeta(pictureId: string | number) {
  return request('/api/ai/meta', { method: 'GET', params: { pictureId } })
}

/** 人工确认元数据（阶段 2.3） */
export function confirmAiMeta(data: {
  pictureId: string | number
  subjectTags?: string
  sceneTags?: string
  lightTags?: string
  compositionTags?: string
  shootLocation?: string
}) {
  return request('/api/ai/meta/confirm', { method: 'POST', data })
}

/** 权限感知检索：带命中依据（阶段 2.4） */
export function searchWithEvidence(params: { query: string; topK?: number }) {
  return request('/api/ai/search/evidence', { method: 'GET', params })
}

/** 相似分组与辅助选片（阶段 2.5） */
export function similarGroups(params: { pictureId: string | number; timeWindowSeconds?: number; threshold?: number }) {
  return request('/api/ai/similar/group', { method: 'GET', params })
}
