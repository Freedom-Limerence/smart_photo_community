import request from '@/request'

export interface NotificationItem {
  id: string
  type: 'LIKE' | 'FAVORITE' | 'COMMENT' | 'FOLLOW' | 'SYSTEM' | string
  title: string
  content?: string
  senderId?: string
  targetType?: string
  targetId?: string
  isRead: number
  createTime: string
}

export const listNotifications = (params: { current?: number; size?: number; onlyUnread?: boolean } = {}) =>
  request('/api/notification/list', { method: 'GET', params })

export const getUnreadCount = () => request('/api/notification/unread/count', { method: 'GET' })

export const markNotificationsRead = (data: { ids?: (string | number)[]; all?: boolean }) =>
  request('/api/notification/read', { method: 'POST', data })

/* ===== 举报 ===== */
export const submitReport = (data: { targetType: string; targetId: string | number; reason: string; detail?: string }) =>
  request('/api/report/submit', { method: 'POST', data })

/* ===== 公告 ===== */
export const getLatestAnnouncements = (limit = 3) =>
  request('/api/announcement/latest', { method: 'GET', params: { limit } })

/* ===== 作者数据 ===== */
export const getMyWorkStats = () => request('/api/work/stats/mine', { method: 'GET' })

/* ===== 社交关系 ===== */
export const getSocialStats = (userId: string | number) =>
  request('/api/social/stats', { method: 'GET', params: { userId } })

/* ===== 审核申诉 ===== */
export const submitAppeal = (data: { pictureId: string | number; reason: string }) => request('/api/appeal/submit', { method: 'POST', data })
export const listMyAppeals = (params: { current?: number; size?: number } = {}) => request('/api/appeal/mine', { method: 'GET', params })
export const listAdminAppeals = (params: { status?: string; current?: number; size?: number }) => request('/api/admin/console/appeals', { method: 'GET', params })
export const handleAppeal = (data: { id: string | number; action: 'ACCEPT' | 'REJECT'; result?: string }) => request('/api/admin/console/appeals/handle', { method: 'POST', data })

/* ===== 用户成就 ===== */
export const getAchievement = (userId?: string | number) => request('/api/user/achievement', { method: 'GET', params: userId ? { userId } : {} })
