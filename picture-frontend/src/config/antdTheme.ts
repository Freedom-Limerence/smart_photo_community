import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'
import { theme as antdThemeApi } from 'ant-design-vue'

/**
 * Ant Design Vue 4.x 主题定制（等价于修改 less 变量，但无需 less 编译配置）
 * 目标：去掉默认的"后台蓝"，换成高级灰 + 莫兰迪鼠尾草绿
 *
 * P0-1 主题键名矫正（2026-10-17）：按《前端视觉改造方案.md》附录 A 把失效键
 * 换成 v4 真名 —— 修正前 105 个组件级键仅 15 个被读取（86% 静默失效），
 * 菜单选中态/表头/行 hover/输入聚焦环等全部走了默认灰蓝。
 * P0-2 补位：controlOutline / controlItemBgActive / controlItemBgHover 全局聚焦环与选中底。
 *
 * 注意：4.2.x 的组件级 token 类型定义较窄（不少键未收录），运行时是支持的，
 * 因此 components 部分显式断言为 any，避免类型检查报错。
 */
export const antdTheme: ThemeConfig = {
  // 暗色画廊（2026-10-17 晚）：全站 antd 组件跟随暗色算法，与 tokens.css 同源
  algorithm: antdThemeApi.darkAlgorithm,
  token: {
    colorPrimary: '#8FA394',
    colorInfo: '#8FA394',
    colorSuccess: '#8FA394',
    colorWarning: '#C2A878',
    colorError: '#B4746A',
    colorLink: '#A3B6A8',
    colorTextBase: '#EDEDEA',
    colorBgBase: '#0B0B0C',
    colorBgLayout: '#0B0B0C',
    colorBgContainer: '#141416',
    colorBorder: 'rgba(255, 255, 255, .12)',
    colorBorderSecondary: 'rgba(255, 255, 255, .08)',
    colorTextSecondary: '#A8A8A2',
    colorTextTertiary: '#82827E',
    colorTextQuaternary: '#6F6F69',
    colorFillSecondary: 'rgba(255, 255, 255, .06)',
    colorFillTertiary: 'rgba(255, 255, 255, .04)',
    colorFillQuaternary: 'rgba(255, 255, 255, .03)',
    controlOutline: 'rgba(143, 163, 148, .18)',
    controlOutlineWidth: 3,
    controlItemBgActive: 'rgba(143, 163, 148, .16)',
    controlItemBgHover: 'rgba(255, 255, 255, .06)',
    borderRadius: 10,
    borderRadiusLG: 16,
    borderRadiusSM: 6,
    borderRadiusXS: 4,
    controlHeight: 38,
    controlHeightLG: 44,
    controlHeightSM: 30,
    fontSize: 14,
    fontFamily: "'Inter', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', system-ui, -apple-system, sans-serif",
    lineHeight: 1.65,
    boxShadow: '0 4px 12px rgba(28,28,26,.06), 0 2px 6px rgba(28,28,26,.04)',
    boxShadowSecondary: '0 14px 36px rgba(28,28,26,.10), 0 4px 12px rgba(28,28,26,.06)',
    boxShadowTertiary: '0 1px 2px rgba(28,28,26,.04), 0 1px 3px rgba(28,28,26,.06)',
    // 页面留白与节奏：与 tokens.css 的 --space-* 同源
    padding: 16,
    paddingLG: 24,
    margin: 16,
    marginLG: 24,
    wireframe: false,
  },
  components: {
    Button: {
      fontWeight: 500,
      // v4 无 primaryShadow/defaultShadow：按钮阴影由全局 controlOutline 决定
    },
    Menu: {
      radiusItem: 8,                              // 原 itemBorderRadius（失效）
      colorItemBgSelected: '#E9EFE9',             // 原 itemSelectedBg（失效）
      colorItemTextSelected: '#4F5F53',           // 原 itemSelectedColor（失效）
      colorItemBgHover: '#F1F1EE',                // 原 itemHoverBg（失效）
      colorActiveBarBorderSize: 0,                // 原 activeBarBorderWidth（失效）
      colorItemBg: 'transparent',
      colorSubItemBg: 'transparent',
      colorItemTextSelectedHorizontal: '#4F5F53', // 原 horizontalItemSelectedColor（失效）
      menuItemHeight: 40,
      itemMarginInline: 8,
      darkItemBg: 'transparent',
      darkSubMenuItemBg: 'transparent',
      darkItemSelectedBg: 'rgba(124,139,127,.28)',
    },
    Layout: {
      colorBgHeader: '#FFFFFF',
      colorBgBody: '#F7F7F5',
      colorBgContainer: '#FFFFFF',
      colorBgLayout: '#F7F7F5',
      colorBgTrigger: '#F1F1EE',
    },
    Table: {
      tableHeaderBg: '#F7F7F5',                   // 原 headerBg（失效）
      tableHeaderTextColor: '#5A5A56',            // 原 headerColor（失效）
      tableRowHoverBg: '#FAFAF8',                 // 原 rowHoverBg（失效）
      tableBorderColor: '#EFEFEA',                // 原 borderColor（失效）
      tableHeaderCellSplitColor: 'transparent',
      tablePaddingVertical: 12,                   // 原 cellPaddingBlock（失效）
      tablePaddingHorizontal: 16,                 // 原 cellPaddingInline（失效）
    },
    Tabs: {
      // 墨条颜色取自全局 colorPrimary（#7C8B7F 已设），无需 inkBarColor
      tabsHoverColor: '#4F5F53',
      tabsHorizontalGutter: 28,
    },
    Tag: { tagDefaultBg: '#F1F1EE', tagDefaultColor: '#5A5A56' },
    Modal: {
      modalHeaderTitleFontSize: 18,               // 原 titleFontSize（失效）
      modalHeaderBg: 'transparent',
      modalContentBg: '#FFFFFF',
    },
    Drawer: { drawerFooterPaddingVertical: 16, paddingLG: 20 },
    Message: { contentBg: '#FFFFFF', borderRadiusLG: 12 },
    Notification: { notificationBg: '#FFFFFF', borderRadiusLG: 12 },
    Empty: { colorTextDescription: '#6F6F69' },
    Skeleton: { color: '#F1F1EE', colorGradientEnd: '#E9E9E4', skeletonBlockRadius: 10 },
    Alert: { alertPaddingHorizontal: 20, borderRadiusLG: 12 },
    Descriptions: { descriptionItemPaddingBottom: 12 },
    Radio: {
      radioCheckedColor: '#7C8B7F',
      radioButtonCheckedBg: '#4F5F53',            // 原 buttonSolidCheckedBg（失效）
      radioSolidCheckedColor: '#FFFFFF',
    },
    Segmented: { segmentedContainerPadding: 2 },  // 选中底色走全局 controlItemBgActive
    Pagination: { paginationItemBgActive: '#E9EFE9', borderRadius: 8 },
    Breadcrumb: {
      breadcrumbBaseColor: '#6F6F69',             // 原 itemColor（失效）
      breadcrumbLastItemColor: '#1C1C1A',
      breadcrumbSeparatorColor: '#C6C6BF',
    },
    Tooltip: { colorBgDefault: '#2A2A28' },       // 原 colorBgSpotlight（失效）
    Divider: { colorSplit: '#EFEFEA' },
    Progress: { progressRemainingColor: '#F1F1EE' }, // 主色走全局 colorPrimary
    Collapse: {
      collapseHeaderPadding: '10px 12px',         // 原 headerPadding（失效）
      collapseContentPaddingHorizontal: 12,
      collapsePanelBorderRadius: 10,
    },
    Card: {
      headerBg: 'transparent',
      paddingLG: 20,
      colorBorderSecondary: '#EFEFEA',
      boxShadowTertiary: 'none',
    },
    // Form：v4 无 labelColor 组件键，标签色走全局 colorTextSecondary
  } as any,
}
