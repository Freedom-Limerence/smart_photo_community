---
target: 首页首屏 contact-sheet（discover/HomePage.vue + ContactSheet + ContactFeed + PublicHeader）
total_score: 20
max_score: 36
na_heuristics: 10
p0_count: 1
p1_count: 2
target_identity: "file:F:\\Code\\final_gra\\picture-frontend\\src\\pages\\discover\\HomePage.vue"
target_fingerprint: "sha256:c854e77c907a0c14f46e9403462213d935e2ca7fd1251601a4d1f5b3628b5954"
target_path: "F:\\Code\\final_gra\\picture-frontend\\src\\pages\\discover\\HomePage.vue"
timestamp: 2026-10-16T11-48-28Z
slug: picture-frontend-src-pages-discover-homepage-vue
---
# 首页首屏设计评审（contact-sheet 方向 · discover/HomePage.vue + ContactSheet + ContactFeed + PublicHeader）

> Method: dual-agent（A：设计评审子代理 · B：检测器子代理，隔离执行）。浏览器环节降级：dev server 未启动 + 本项目无头渲染永不 settle（Vite HMR WS + devtools 插件 + SSE 动画），全部结论来自源码证据，渲染细节已标注「需浏览器实测确认」。

## Design Health Score

| # | 启发式 | 分数 | 关键问题 |
|---|--------|------|----------|
| 1 | 系统状态可见 | 2/4 | 加载中 `v-if="works.length"` 整屏替换，无过渡无占位（discover/HomePage.vue:4）；序号/计数器/索引条选中态好 |
| 2 | 贴近真实世界 | 3/4 | 印样隐喻、01/12 计数、序号是摄影师语言；「印样」未言语化，普通访客未必解码 |
| 3 | 用户控制与自由 | 1/4 | 7s 自动轮播无暂停控件；触屏 hover 恒 false；reduced-motion 下内容仍被替换（WCAG 2.2.2） |
| 4 | 一致性与标准 | 2/4 | ContactSheet/ContactFeed 硬编码 #F5F6F4/#202621/#E4E6E2，绕过 tokens.css 禁令；sheet-title 未用衬线字体 |
| 5 | 错误预防 | 2/4 | 未登录点收藏是事后告知；heroOf() 可能空串仍渲染 img |
| 6 | 识别而非回忆 | 3/4 | 当前作品、位置、前后关系（邻近透明度衰减）全部可见 |
| 7 | 灵活与效率 | 2/4 | 键盘←→需先聚焦 section 且无提示；移动端导航按钮 display:none 且无 swipe |
| 8 | 美学极简 | 3/4 | 主画面克制、浮层贴边；扣分在加载态借用 opening 编辑模板，与印样两套人格 |
| 9 | 错误恢复 | 2/4 | 加载失败只有 toast，feed 区无重试入口 |
| 10 | 帮助文档 | n/a | Experience 模式探索型首屏，引入帮助即违背「界面退后」 |

**总分：20/36 —— Acceptable（需要显著改进后用户才会满意）**

## 设计专属性判定（Design Specificity）

**LLM 评估**：方向兑现是真实的——主作品全幅 min(78vh, 760px)、连续索引条带序号与邻近透明度衰减、clip-path+blur「显影」揭示、tabular-nums 计数器，这些在任意图片站模板里找不到，是「为光影集而作」的印记。但三处明显滑回通用：① 首屏最大的字不穿制服——sheet-title 继承 Inter 无衬线，「衬线标题停顿」恰好在最需要停顿的地方失效；② 色板私自立国——#F5F6F4/#202621/#E4E6E2 不在令牌表内，且 #F5F6F4 与 --bg #F7F7F5 形成肉眼难辨的「近似但不同」；③ 空态/加载态人格分裂——opening 区是另一套编辑式模板，有数据后整体消失，「接触印样」没有接管全部首屏状态。结论：核心视觉是专属的，外围状态层还是可套用的。

**确定性扫描**：检测器对 4 个目标 .vue 文件返回 0 命中（退出码 0），三次独立运行一致，负向对照（传入不存在文件产生 warning）证明文件被实际读取。但检测器自身声明：对 .vue 仅运行 regex 规则，需要解析 DOM 的元素级规则（tiny-text、low-contrast 等）未运行，结果为欠计数。与 LLM 评估的关系：A 发现的硬编码色值、衬线缺失等问题属于令牌/字体层面，检测器规则未覆盖——不是误报，是检测盲区；无一处两者矛盾。

## Priority Issues

**[P0] ContactFeed 的作品键盘完全不可达 + Enter 双重触发**
- 现状：ContactFeed.vue:3 用 `<article @click>`，无 tabindex/role/button——键盘用户无法打开 feed 里任何一件作品；ContactSheet.vue:83-87 的 @keydown 挂在 section 上，索引条按钮上按 Enter 冒泡触发跳转详情，控件语义被劫持
- 伤害：主任务「发现并打开一个作品」对键盘用户被腰斩（Sam 画像直接失败）
- 修法：ContactFeed 的 frame 改为 `<button>`（:focus-visible 时同步显示 .frame-meta，目前只响应 :hover）；ContactSheet 的 onKeydown 先判 e.target === sheetRef.value 再处理 Enter
- 建议命令：audit

**[P1] 自动轮播无暂停控件，触屏与 reduced-motion 用户被强制「翻页」**
- 现状：ContactSheet.vue:103-110 每 7s step(1)，暂停条件只有 document.hidden 和 :hover；触屏 hover 恒 false；app.css:218-226 只压制动画时长，压制不了内容切换
- 伤害：Casey 读作者名的 7 秒内图被换走；违反 WCAG 2.2.2 暂停/停止/隐藏
- 修法：.sheet-nav 加播放/暂停按钮（或默认暂停）；matchMedia('(prefers-reduced-motion: reduce)') 为真不启动 timer；焦点进入 section 时暂停
- 建议命令：quieter

**[P1] Header 下拉触发器键盘不可达**
- 现状：PublicHeader.vue:17-20「找图」「我的图库」触发器是 span（非可聚焦），a-dropdown 默认 hover 触发——键盘用户进不了语义检索/以图搜图/云图库等六个入口
- 修法：触发器改 `<button>`，trigger="click" 或补 keydown.enter，打开时焦点入菜单
- 建议命令：audit

**[P2] 首屏加载态整屏替换，方向在状态层断裂**
- 现状：discover/HomePage.vue:4,6 v-if 互斥使 ContactSheet 与 opening 区切换，布局跳变（CLS 需实测），暴露「印样没有自己的加载态」
- 修法：ContactSheet 常驻渲染，内部用暗场 + 骨架/显影占位承接 loading，opening 区只留作真正的空态
- 建议命令：layout（或 animate）

**[P2] 首屏标题排版与令牌双漂移**
- 现状：sheet-title（ContactSheet.vue:119）无 font-family，最大标题不是 Noto Serif SC；#F5F6F4/#202621/#E4E6E2/#E9EFE9 硬编码绕过 tokens.css:2 禁令，#F5F6F4 与 --bg #F7F7F5 近似但不同
- 修法：font-family: var(--font-display)；颜色映射既有令牌，确需「印样底色」则在 tokens.css 新增令牌（如 --surface-print）
- 建议命令：typeset

## Persona Red Flags

**Jordan（困惑的初次访客）**：产品自我介绍只在空态出现（opening 区被 v-if 门控，有作品后新访客第一屏没有任何「这是什么」）；主图可点的唯一线索是 cursor:pointer，误触即离首页；收藏按钮事后才告知需登录。

**Casey（单手刷手机的移动用户）**：768px 以下 .sheet-nav display:none 且无 touch/swipe 监听，翻图只能滚到底部索引条；触屏自动轮播永不暂停；搜索框 ≤1024px 隐藏，移动首屏无搜索入口。

**Sam（键盘 + 屏幕阅读器用户）**：ContactFeed 全部作品不可聚焦（主任务失败）；tabindex=0 的 section 无 role/aria-label，聚焦后不知 ←→ 可用；Enter 冒泡劫持控件语义；自动轮播无 aria-live 也无暂停手段，SR 上下文持续漂移。

## Minor Observations

1. discover/HomePage.vue:56-57 导入 MasonryGrid、WorkCard 从未使用（死引用）
2. favorited 初始值不读 works[0].favorited（ContactSheet.vue:56），首张已收藏作品显示态与真实不符
3. heroOf() 优先取原图 url，无 srcset/sizes，首屏直接拉原图（带宽需实测）
4. 非选中缩略图渲染 aria-current="false"，应条件省略
5. ContactSheet 焦点环与 ::selection 硬编码 #7C8B7F，应为 var(--accent)
6. 旧版 src/pages/HomePage.vue 仍留在仓库且内含成片硬编码色，建议删除防误用

## Questions to Consider

- 自动轮播到底服务谁？默认暂停、让用户自己「显影」下一张是否更贴 Experience 方向？
- 印样隐喻要不要言语化？一行 `CONTACT SHEET · 12 FRAMES` 既解释机制，也把「只看前 12 件」变成显性策展语义。
- loading/empty 态是否应统一为「暗房显影」语言，让首屏任何状态下只有一种人格？
- 收藏按钮在首屏对未登录访客是转化钩子还是噪音——首屏只读、收藏下沉详情页是否更「界面退后」？
