import request from '@/request'

export function listWorks(params: { current?: number; size?: number }) {
  return request('/api/work/page', { method: 'GET', params })
}

export function saveWork(data: any) {
  return request('/api/work/save', { method: 'POST', data })
}

export function publishWork(params: { workId: number | string; publish?: boolean }) {
  return request('/api/work/publish', { method: 'POST', params })
}

export function likeWork(data: { workId: number | string }) {
  return request('/api/work/like', { method: 'POST', data })
}

export function favoriteWork(data: { workId: number | string }) {
  return request('/api/work/favorite', { method: 'POST', data })
}

export function unfavoriteWork(data: { workId: number | string }) {
  return request('/api/work/unfavorite', { method: 'POST', data })
}

export function commentWork(data: { workId: number | string; content: string; parentId?: number | string }) {
  return request('/api/work/comment', { method: 'POST', data })
}

export function searchAi(query: string, topK = 20) {
  return request('/api/ai/search', { method: 'GET', params: { query, topK } })
}

export function catalogPicture(pictureId: number | string) {
  return request('/api/ai/catalog', { method: 'POST', params: { pictureId } })
}

export function getAiMeta(pictureId: number | string) {
  return request('/api/ai/meta', { method: 'GET', params: { pictureId } })
}

export function getTodayQuota() {
  return request('/api/access/quota/today', { method: 'GET' })
}

export function authorizeDownload(data: { pictureId: number | string; workId?: number | string; requestId: string }) {
  return request('/api/access/download/authorize', { method: 'POST', data })
}

export function downloadAuthorizedFile(token: string) {
  return request<Blob>('/api/access/download/file', {
    method: 'GET',
    params: { token },
    responseType: 'blob',
  })
}

export function createInvite(data: any) {
  return request('/api/access/invite/create', { method: 'POST', data })
}

export function redeemInvite(data: { code: string }) {
  return request('/api/access/invite/redeem', { method: 'POST', data })
}

export function listMyTeamSpaces() {
  return request('/api/spaceUser/list/my', { method: 'POST' })
}

export function listInvites(spaceId: number) {
  return request('/api/access/invite/list', { method: 'GET', params: { spaceId } })
}

export function listExhibitions(data: { current?: number; pageSize?: number; searchText?: string; theme?: string }) {
  return request('/api/exhibition/list/page', { method: 'POST', data })
}

export function getExhibition(id: number) {
  return request('/api/exhibition/get/vo', { method: 'GET', params: { id } })
}

export function listExhibitionPictures(exhibitionId: number) {
  return request('/api/exhibition/picture/list', { method: 'GET', params: { exhibitionId } })
}

export function listExhibitionWorks(exhibitionId: number) {
  return request('/api/exhibition/work/list', { method: 'GET', params: { exhibitionId } })
}
