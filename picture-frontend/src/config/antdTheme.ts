import type { ThemeConfig } from 'ant-design-vue/es/config-provider/context'

/**
 * Ant Design Vue 4.x 主题定制（等价于修改 less 变量，但无需 less 编译配置）
 * 目标：去掉默认的"后台蓝"，换成高级灰 + 莫兰迪鼠尾草绿
 *
 * 注意：4.2.x 的组件级 token 类型定义较窄（不少键未收录），运行时是支持的，
 * 因此 components 部分显式断言为 any，避免类型检查报错。
 */
export const antdTheme: ThemeConfig = {
  token: {
    colorPrimary: '#7C8B7F',
    colorInfo: '#7C8B7F',
    colorSuccess: '#7C8B7F',
    colorWarning: '#C2A878',
    colorError: '#B4746A',
    colorLink: '#6B7A6E',
    colorTextBase: '#1C1C1A',
    colorBgBase: '#FFFFFF',
    colorBgLayout: '#F7F7F5',
    colorBorder: '#E6E6E1',
    colorBorderSecondary: '#EFEFEA',
    borderRadius: 10,
    borderRadiusLG: 16,
    borderRadiusSM: 6,
    controlHeight: 38,
    fontSize: 14,
    fontFamily: "'Inter', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', system-ui, -apple-system, sans-serif",
    boxShadow: '0 4px 12px rgba(28,28,26,.06), 0 2px 6px rgba(28,28,26,.04)',
    boxShadowSecondary: '0 14px 36px rgba(28,28,26,.10), 0 4px 12px rgba(28,28,26,.06)',
    wireframe: false,
  },
  components: {
    Button: {
      primaryShadow: 'none',
      defaultShadow: 'none',
      fontWeight: 500,
    },
    Card: {
      headerBg: 'transparent',
      paddingLG: 20,
      colorBorderSecondary: '#EFEFEA',
    },
    Menu: {
      itemBorderRadius: 8,
      itemSelectedBg: '#E9EFE9',
      itemSelectedColor: '#4F5F53',
      itemHoverBg: '#F1F1EE',
      activeBarBorderWidth: 0,
      horizontalItemSelectedColor: '#4F5F53',
      darkItemBg: 'transparent',
      darkSubMenuItemBg: 'transparent',
      darkItemSelectedBg: 'rgba(124,139,127,.28)',
    },
    Tabs: {
      inkBarColor: '#7C8B7F',
      itemSelectedColor: '#1C1C1A',
      itemHoverColor: '#4F5F53',
      titleFontSize: 15,
    },
    Table: {
      headerBg: '#F7F7F5',
      headerColor: '#5A5A56',
      rowHoverBg: '#FAFAF8',
      borderColor: '#EFEFEA',
    },
    Input: { activeShadow: '0 0 0 3px rgba(124,139,127,.12)' },
    Select: { optionSelectedBg: '#E9EFE9' },
    Tag: { defaultBg: '#F1F1EE', defaultColor: '#5A5A56' },
    Modal: { borderRadiusLG: 16 },
    Drawer: { paddingLG: 20 },
    Message: { contentBg: '#FFFFFF' },
  } as any,
}
