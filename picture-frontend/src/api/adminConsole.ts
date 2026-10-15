import request from '@/request'

export const getAdminOverview = () => request('/api/admin/console/overview', { method: 'GET' })
export const getAdminTrend = (days = 7) => request('/api/admin/console/trend', { method: 'GET', params: { days } })
export const listAdminWorks = (params: { current?: number; size?: number; status?: string; keyword?: string }) => request('/api/admin/console/works', { method: 'GET', params })
export const listAdminPictures = (params: { current?: number; size?: number; reviewStatus?: number }) => request('/api/admin/console/pictures', { method: 'GET', params })
export const moderateContent = (data: { type: 'picture' | 'work'; action: 'PASS' | 'REJECT' | 'BAN' | 'RESTORE'; ids: (string | number)[]; reason?: string }) => request('/api/admin/console/moderate', { method: 'POST', data })
export const banUser = (data: { id: string | number; banned: boolean }) => request('/api/user/ban', { method: 'POST', data })
export const listOperationLogs = (data: { current: number; pageSize: number }) => request('/api/log/list/page', { method: 'POST', data })

/* ===== RBAC 角色与权限 ===== */
export const getRbacMy = () => request('/api/admin/rbac/my', { method: 'GET' })
export const getRbacMatrix = () => request('/api/admin/rbac/matrix', { method: 'GET' })
export const updateRolePermissions = (data: { roleCode: string; permCodes: string[] }) => request('/api/admin/rbac/role/permissions', { method: 'POST', data })
export const assignUserRole = (data: { userId: string | number; roleCode: string }) => request('/api/admin/rbac/user/role', { method: 'POST', data })

/* ===== 系统监控 ===== */
export const getAdminMetrics = (hours = 24) => request('/api/admin/console/metrics', { method: 'GET', params: { hours } })
