# 视觉基调定稿（2026-10-19 · 与 17 确认）

> 本章为视觉方向唯一权威；下方旧章节如与本章冲突，以本章为准。
> 审美锚点：motionsites.ai / lusion.co / activetheory.net —— 动效沉浸风，非安静编辑风。

## 影调
- **前台 = 暗色画廊**：`#0B0B0C` 底，照片主导，chrome 隐形（黑盒子画廊逻辑，同 Lightroom/VSCO）。
- **后台管理 = 亮色**（AdminLayout `data-theme="light"`）：数据密集场景保持功能性，**刻意与前台差异化**。

## 适用范围（重要）
**精致感全套（暗色影调 + 大字排版 + 全套动效）只作用于门面页（2026-10-19 定稿七页 → 2026-10-20 扩至 10 个界面）：**

> **2026-10-20 范围更新**：门面新增 `/tags`、`/tags/:tag`、`/picture/:id`（+3）；新增 **"AI 特色半门面"** 档（`/ai/catalog`、`/ai/search`、`/ai/similar`）；**移动端精致化延后**。
> **完整范围（分档矩阵、每页唯一瞬间、验收清单、执行顺序）以《前端精致化范围.md》为准。**
- ✅ 首页（`/` 电影级着陆页——首屏为动态图片墙 + `/discover` 发现页）
- ✅ 作品画廊页（社区/作品流）
- ✅ 作品详情页（沉浸舞台已就位）
- ✅ **摄影师主页**（对外个人名片，分享传播落点）
- ✅ **专题展览 + 展览详情**（策展亮点，答辩演示动线）
- ✅ **关于页**（项目故事 + 设计系统展示，答辩用）
- ✅ `/wall` 今日影像大屏（已就位）

**❌ 完全豁免：后台管理员管理页（/admin/**）**——不暗色、不动效、只做功能性优化（表格、表单、效率优先）。个人中心/设置等工具页取中间态：跟随暗色令牌但不加电影动效。

## 动效 token（来自 cinematic-gsap-lenis-motion-system skill，已实现于 src/motion/index.ts）
- 缓动：仅 `power3.out` / `power4.out` / `expo.out`；禁 bounce/elastic/spring
- 滚动联动：`scrub: 1`；揭示触发：`start: "top 82%"`
- 文字 stagger：词 0.035-0.07s，行 0.08-0.14s；卡片 0.06-0.1s
- Lenis：lerp 0.09，由 `gsap.ticker` 驱动（src/motion/index.ts）
- 挂载方式：声明式 data 属性——`data-magnetic`（磁吸 CTA）、`data-scrub-fade`（区块入场）、`data-scrub-words`（大字滚动显影）、`v-reveal`/`v-reveal:clip`（滚动显现）、`v-tilt`（3D 倾斜）
- 降级铁律：所有动效门控 `hover: hover` + `prefers-reduced-motion`

## 五条铁律
1. 一页一个标志性瞬间，其余全部克制
2. 排版尺度差要极端（84px 大字 × 12px 眉标）
3. 动效只动 transform / opacity / filter（合成层，60fps）
4. 缓动只用 power3 / power4 / expo.out
5. 每个动效都有降级

## 四技能工作流（每次开发页面按此循环）
1. **frontend-design** 定方向（情绪/字体/色彩 60-30-10/骨架）
2. **cinematic-scroll-storytelling** 定结构（页面分幕，每幕一个瞬间）
3. **cinematic-gsap-lenis-motion-system** 做动效（token 统一 + data 属性）
4. **impeccable** 终审（对比度/网格/降级/空态）→ `npm run verify` + `npm run test:smoke`

---

---
name: 光影集
description: 兼顾摄影创作、作品发现与策展的影像工作室视觉系统
colors:
  # —— 暗色画廊定稿（2026-10-19）：前台默认即此套 ——
  gallery-bg: "#0B0B0C"
  gallery-surface: "#141416"
  gallery-surface-2: "#1A1A1C"
  gallery-text: "#EDEDEA"
  gallery-text-2: "#A8A8A2"
  gallery-text-3: "#82827E"
  gallery-accent: "#8FA394"
  gallery-accent-hover: "#A3B6A8"
  gallery-accent-ink: "#C9D3CB"
  gallery-text-media: "#F2F4F1"
  gallery-border: "rgba(255, 255, 255, .08)"
  gallery-border-strong: "rgba(255, 255, 255, .16)"
  # —— 亮色套：仅后台管理页使用（AdminLayout data-theme=light）——
  primary-misty-grey-green: "#7C8B7F"
  primary-hover: "#6B7A6E"
  primary-ink: "#4F5F53"
  primary-wash: "#E9EFE9"
  secondary-sand: "#C2A878"
  tertiary-brick: "#B4746A"
  neutral-page: "#F7F7F5"
  neutral-surface: "#FFFFFF"
  neutral-soft: "#F1F1EE"
  neutral-sunken: "#EFEFEA"
  neutral-text: "#1C1C1A"
  neutral-body: "#5A5A56"
  neutral-muted: "#8E8E88"
  neutral-border: "#E6E6E1"
  neutral-border-strong: "#D6D6CF"
  immersive-black: "#0B0B0C"
  immersive-surface: "#141416"
  immersive-text: "#EDEDEA"
  immersive-sage: "#8FA394"
  admin-pine: "#22302A"
  admin-text: "#F2F4F1"
typography:
  display:
    fontFamily: "Noto Serif SC, Songti SC, STSong, Source Han Serif SC, Georgia, serif"
    fontSize: "40px"
    fontWeight: 400
    lineHeight: 1.2
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Noto Serif SC, Songti SC, STSong, Source Han Serif SC, Georgia, serif"
    fontSize: "28px"
    fontWeight: 400
    lineHeight: 1.25
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Inter, PingFang SC, Hiragino Sans GB, Microsoft YaHei, system-ui, sans-serif"
    fontSize: "22px"
    fontWeight: 600
    lineHeight: 1.3
  body:
    fontFamily: "Inter, PingFang SC, Hiragino Sans GB, Microsoft YaHei, system-ui, sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.65
  label:
    fontFamily: "Inter, PingFang SC, Hiragino Sans GB, Microsoft YaHei, system-ui, sans-serif"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: 1.5
    letterSpacing: "0.2em"
rounded:
  xs: "4px"
  sm: "6px"
  base: "10px"
  lg: "16px"
  pill: "999px"
spacing:
  space-1: "4px"
  space-2: "8px"
  space-3: "12px"
  space-4: "16px"
  space-5: "24px"
  space-6: "32px"
  space-7: "40px"
  space-8: "56px"
components:
  button-primary:
    backgroundColor: "{colors.primary-misty-grey-green}"
    textColor: "{colors.neutral-surface}"
    rounded: "{rounded.base}"
    height: "38px"
  button-primary-hover:
    backgroundColor: "{colors.primary-hover}"
  button-secondary:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-body}"
    rounded: "{rounded.base}"
    height: "38px"
  button-featured:
    backgroundColor: "{colors.primary-ink}"
    textColor: "{colors.neutral-surface}"
    rounded: "{rounded.lg}"
    height: "46px"
  chip-selected:
    backgroundColor: "{colors.primary-wash}"
    textColor: "{colors.primary-ink}"
    rounded: "{rounded.pill}"
  chip-default:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-body}"
    rounded: "{rounded.pill}"
  work-card:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.lg}"
  search-field:
    backgroundColor: "{colors.neutral-surface}"
    textColor: "{colors.neutral-text}"
    rounded: "{rounded.base}"
    height: "38px"
  nav-item:
    textColor: "{colors.neutral-body}"
    rounded: "{rounded.pill}"
  page-header:
    textColor: "{colors.neutral-text}"
    typography: "{typography.headline}"
  empty-state:
    textColor: "{colors.neutral-muted}"
    rounded: "{rounded.lg}"
---

# Design System: 光影集

## Overview

**Creative North Star: "一间兼顾创作与策展的工作室"**

这套系统以摄影作品为主角：浅色页面像整理与交流用的工作台，留白和分隔线帮助阅读，低饱和的雾灰绿标出可以行动的地方。衬线标题提供画册般的编辑气质，正文和操作控件保持清晰、朴素。作品详情使用深色看图表面；后台用深松绿侧栏和密度更高的信息布局承担治理工作。

当前实现已经有共享令牌、前台作品卡片与瀑布流、独立的个人中心和管理外壳。此文档记录这些已落地的视觉规律；少量旧页面仍有硬编码颜色或不一致的细节，它们不是可复制的设计标准。

**Key Characteristics:**
- 摄影内容优先，界面保持安静，次要操作随悬停或焦点显现。
- 浅暖纸面、白色表面、细边线，雾灰绿只用于关键状态和操作。
- 展示标题用中文衬线，正文与后台数据用无衬线。
- 前台、深色看图界面与后台共享令牌关系，但各有明确的使用场景。

## Colors

主强调色是**雾灰绿 Misty Grey-Green**（莫兰迪色系，亦称桉树绿 Eucalyptus）。它低饱和、接近中性，不与摄影作品争夺注意力。下列名称对应 frontmatter 中的规范值，源码入口为 `picture-frontend/src/styles/tokens.css`。

### Primary
- **雾灰绿**：主要操作、焦点与选中状态；深一档用于悬停。
- **深雾绿**：前台主按钮、活动文字与标题悬停。
- **浅雾绿**：轻量筛选、选中标签和反馈背景。

### Secondary
- **暖沙色**：警示与极少量暖色辅助，不承担常规主操作。

### Tertiary
- **柔砖红**：错误与危险操作，不用于装饰。

### Neutral
- **暖纸白**：公共页面背景；**纯白表面**：卡片、表单与导航。
- **浅灰表面、下沉灰**：骨架、次级区块和输入周围的层级。
- **近黑、正文灰、静音灰**：标题、正文和辅助信息三级文字。
- **细分隔线、强调分隔线**：区块与控件边界。
- **深色看图组**：近黑底、深灰表面、浅色正文，局部使用亮一档的雾绿。
- **后台深松绿**：侧栏及后台品牌标识的深色基底。

**The Rare Accent Rule.** 雾灰绿标记主要行动、选中与焦点；大面积内容保持中性，让照片本身提供最丰富的色彩。

## Typography

**Display Font:** `Noto Serif SC`，回退到宋体家族与 Georgia。  
**Body Font:** `Inter`，中文回退到苹方、系统中文无衬线。  
**Label Font:** 与正文同族，用字重和字距区分层级。

**Character:** 中文衬线用于少量展示标题，形成画册般的停顿；无衬线承担密集信息、导航、表单和治理数据。字体栈是现有 CSS 声明，未内置或下载指定字形时会按设备字体回退。

### Hierarchy
- **Display**（400，标准 40px，1.2）：页面主标题；首页首屏会按视口放大至 `clamp(38px, 4vw, 64px)`。
- **Headline**（400，28px，1.25）：页面标题与内容区标题。
- **Title**（600，22px，1.3）：区块标题；作品卡片标题另用较克制的 14px 中等字重。
- **Body**（400，14px，1.65）：说明、内容和大多数控件；长段落宜保持舒适行宽，现有 `--measure` 为 68ch。
- **Label**（600，12px，0.2em 字距）：眉标与栏目提示，通常配合大写英文，保持稀疏使用。

**The Serif Pause Rule.** 衬线留给引导浏览和阅读的标题；表格、筛选、输入和操作说明继续使用清晰的无衬线。

## Layout

公共页面使用居中的弹性版心：默认最大宽度 1280px，左右边距 32px；在 1440、1680、1920px 视口下逐级放宽，最宽到 1560px。平板和手机边距分别收至 24px、16px。内容型窄版心为 960px，后台内容上限为 1720px。垂直节奏以 4px 为基准，常见区块间距为 24、32、40、56px。

首页首屏采用文字与主视觉双列，约 900px 以下堆叠。作品瀑布流通常为四列，1680px 以上五列，并在 1280、900、560px 以下依次减为三、二、一列。公共导航在窄屏变成两行并允许横向滚动；个人中心保持横向标签导航。管理后台采用独立侧栏和内容区，服务密集查看与操作。

**The Image Leads Rule.** 当页面展示作品时，先保证图像尺度和浏览节奏，再安放标题、作者与操作；表格密度只用于管理工作流。

## Elevation & Depth

页面以纸面与白色表面的轻微色差、细边框建立层级；阴影为辅助。常规卡片保持低调，交互卡片悬停后边框变清晰并轻微上浮。首页主视觉比普通卡片高一层。深色看图界面主要用明度差，而非大量投影。

### Shadow Vocabulary
- **轻环境影**（`0 1px 2px rgba(28, 28, 26, .04), 0 1px 3px rgba(28, 28, 26, .06)`）：静态表面。
- **交互影**（`0 4px 12px rgba(28, 28, 26, .06), 0 2px 6px rgba(28, 28, 26, .04)`）：悬停反馈和主视觉静态层级。
- **强调浮起影**（`0 14px 36px rgba(28, 28, 26, .10), 0 4px 12px rgba(28, 28, 26, .06)`）：主视觉悬停或明确的可点击卡片反馈。

**The Quiet Surface Rule.** 默认先用表面明度和边线分层；只有交互或主视觉需要更明显的阴影。

## Shapes

表面柔和但不软塌：细小控件用 4–6px 圆角，常规按钮和输入用 10px，作品卡片与主视觉用 16px。筛选胶囊、导航标签和头像使用全圆角。图片封面由卡片裁切；看图舞台以深色矩形托住照片，不额外叠加装饰边框。

## Components

### Buttons
- **Primary:** Ant Design Vue 的基本按钮消费雾灰绿主题值；首页主按钮使用深雾绿、46px 高和更宽的水平内边距。
- **Secondary / Ghost:** 白色或透明表面、细边框和正文灰文字，悬停时转向雾灰绿。
- **Hover / Focus:** 状态切换约 140–220ms，键盘焦点使用可见的雾灰绿外框；尊重减少动效偏好。

### Chips
- **Style:** 细边框、白底、全圆角、小号文字。
- **State:** 选中态采用浅雾绿底与深雾绿字；悬停先加强边线和文字。

### Cards / Containers
- **作品卡片:** 白底、16px 圆角、细边框；封面在上，标题与作者在下。悬停时封面轻推近，操作浮在照片边缘；键盘焦点也应显现操作。
- **通用表面:** 白底、细边框和轻环境影；内部内边距通常从 16–24px 选取。
- **空态:** 圆形浅灰图标容器、简短衬线标题、说明与一个后续操作。

### Inputs / Fields
- **Style:** 白底、细边框、约 10px 圆角；搜索框随版心伸缩。
- **Focus:** 输入与其他可交互元素共享清晰焦点反馈；错误状态使用柔砖红，不靠颜色单独表达错误。

### Navigation
- **公共导航:** 半透明白色吸顶栏，默认文字灰，当前项为深雾绿文字与小圆点；移动端横向滚动。
- **个人中心:** 吸顶横向标签，当前项用深雾绿细线标记。
- **管理后台:** 深松绿侧栏、浅色顶部信息栏；以权限和任务为中心组织菜单。

### Photography Viewer

作品详情使用 `data-theme="immersive"` 覆盖共享颜色变量，照片显示在近黑舞台；切换、全屏、缩略图与作品信息围绕看图任务安排。现有页面的右侧信息栏是该页面目前的实现，不是其他作品展示页必须复用的布局。

## Do's and Don'ts

### Do:
- **Do** 从共享令牌取色、字阶、间距、圆角和动效时长；新增页面先复用这些关系。
- **Do** 让作品图片决定浏览节奏，用留白、排版与轻量互动支撑它。
- **Do** 保持前台、个人中心与后台的使用场景清楚，并为键盘焦点和减少动效偏好提供对应状态。
- **Do** 将暗色看图界面作为有目的的场景切换，而非全站装饰。

### Don't:
- **Don't** 把云图库上传或后台表格的视觉语言带到公共作品发现页。
- **Don't** 使用高饱和装饰、廉价渐变、卡通或电商式促销外观掩盖摄影作品。
- **Don't** 把遗留页面中的单次硬编码值直接升级为新组件的规范。
- **Don't** 让悬停才出现的操作在键盘或触屏环境下无法触达。
