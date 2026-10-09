import request from '@/request'

export function listWorks(params: { current?: number; size?: number }) {
  return request('/api/work/page', { method: 'GET', params })
}

export function saveWork(data: any) {
  return request('/api/work/save', { method: 'POST', data })
}

export function publishWork(params: { workId: number; publish?: boolean }) {
  return request('/api/work/publish', { method: 'POST', params })
}

export function likeWork(data: { workId: number }) {
  return request('/api/work/like', { method: 'POST', data })
}

export function favoriteWork(data: { workId: number }) {
  return request('/api/work/favorite', { method: 'POST', data })
}

export function unfavoriteWork(data: { workId: number }) {
  return request('/api/work/unfavorite', { method: 'POST', data })
}

export function commentWork(data: { workId: number; content: string; parentId?: number }) {
  return request('/api/work/comment', { method: 'POST', data })
}

export function searchAi(query: string, topK = 20) {
  return request('/api/ai/search', { method: 'GET', params: { query, topK } })
}

export function catalogPicture(pictureId: number) {
  return request('/api/ai/catalog', { method: 'POST', params: { pictureId } })
}

export function getAiMeta(pictureId: number) {
  return request('/api/ai/meta', { method: 'GET', params: { pictureId } })
}

export function getTodayQuota() {
  return request('/api/access/quota/today', { method: 'GET' })
}

export function createInvite(data: any) {
  return request('/api/access/invite/create', { method: 'POST', data })
}

export function redeemInvite(data: { code: string }) {
  return request('/api/access/invite/redeem', { method: 'POST', data })
}
