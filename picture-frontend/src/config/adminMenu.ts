import type { Component } from 'vue'
import {
  AuditOutlined, DashboardOutlined, FileTextOutlined, FlagOutlined,
  CommentOutlined, FileProtectOutlined, HistoryOutlined, ThunderboltOutlined, TrophyOutlined, FundProjectionScreenOutlined, NotificationOutlined,
  PictureOutlined, SafetyCertificateOutlined, TeamOutlined, UserOutlined,
} from '@ant-design/icons-vue'

export interface AdminMenuItem {
  key: string
  label: string
  icon: Component
  /** 访问所需权限点（与后端 sys_permission.permCode 一致） */
  permission: string
  description: string
  /** 侧栏分组（4.3：概览 / 内容治理 / 用户与空间 / 系统） */
  group: 'overview' | 'content' | 'userspace' | 'system'
}

/**
 * 管理后台菜单 = 权限的唯一前端声明处。
 * 菜单渲染、路由守卫、无权限时的落地页都从这里取，避免三处各写一遍。
 */
export const ADMIN_MENU: AdminMenuItem[] = [
  { key: '/admin/dashboard', label: '数据看板', icon: DashboardOutlined, permission: 'stats:view', description: '平台总量、今日新增与增长趋势', group: 'overview' },
  { key: '/admin/review', label: '内容审核', icon: AuditOutlined, permission: 'content:review', description: '待审图片与作品队列', group: 'content' },
  { key: '/admin/users', label: '用户与权限', icon: TeamOutlined, permission: 'user:manage', description: '用户资料维护与封禁', group: 'userspace' },
  { key: '/admin/pictures', label: '图片管理', icon: PictureOutlined, permission: 'picture:manage', description: '站内图片资产管理', group: 'userspace' },
  { key: '/admin/spaces', label: '空间管理', icon: UserOutlined, permission: 'space:manage', description: '团队与私有空间治理', group: 'userspace' },
  { key: '/admin/monitor', label: '系统监控', icon: FundProjectionScreenOutlined, permission: 'system:monitor', description: '接口调用量、耗时与错误率', group: 'system' },
  { key: '/admin/rbac', label: '角色权限', icon: SafetyCertificateOutlined, permission: 'rbac:manage', description: '角色权限矩阵与角色分配', group: 'system' },
  { key: '/admin/auto-review', label: '自动审核', icon: ThunderboltOutlined, permission: 'content:review', description: '机审策略与一键处理待审积压', group: 'content' },
  { key: '/admin/appeal', label: '申诉处理', icon: FileProtectOutlined, permission: 'report:manage', description: '作者申诉的复核与重新上架', group: 'content' },
  { key: '/admin/comments', label: '评论管理', icon: CommentOutlined, permission: 'content:review', description: '评论机审结果复核与隐藏/恢复', group: 'content' },
  { key: '/admin/review-logs', label: '审核流水', icon: HistoryOutlined, permission: 'content:review', description: '每一次审核动作的完整记录', group: 'content' },
  { key: '/admin/exhibitions', label: '征稿管理', icon: TrophyOutlined, permission: 'exhibition:manage', description: '发起征稿启事、评选用户投稿', group: 'content' },
  { key: '/admin/report', label: '举报处理', icon: FlagOutlined, permission: 'report:manage', description: '用户举报的受理与处置', group: 'content' },
  { key: '/admin/announcement', label: '公告管理', icon: NotificationOutlined, permission: 'announcement:manage', description: '发布平台公告并推送全体用户', group: 'system' },
  { key: '/admin/logs', label: '操作日志', icon: FileTextOutlined, permission: 'log:view', description: '后台操作审计记录', group: 'system' },
]

/** 根据路径找到对应菜单项（用于守卫判断该路径需要什么权限） */
export function findMenuByPath(path: string): AdminMenuItem | undefined {
  return ADMIN_MENU.filter((item) => path === item.key || path.startsWith(item.key + '/'))
    .sort((a, b) => b.key.length - a.key.length)[0]
}
