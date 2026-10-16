---
version: 1
slug: "picture-frontend-src-pages-discover-homepage-vue"
primary_target: "picture-frontend/src/pages/discover/HomePage.vue"
related_targets: ["picture-frontend/src/pages/work/WorkDetailPage.vue","picture-frontend/src/layouts/UserLayout.vue","picture-frontend/src/layouts/AdminLayout.vue"]
---

# 光影集 · 接触印样全站延展

THESIS: 让一张摄影作品占据第一眼，让连续的影像索引引导下一次停留。个人与团队从发现、选片到发布的任务始终可达。

OWN-WORLD: 沿用已经由方向选择页选定并提交的“光的接触印样”（开发日志 / 2f98ed2）。冷白印相纸、精确的照片边界、中文衬线展示字、雾灰绿 #7C8B7F。保留现有 code-led 执行记录，不重新要求用户选择已经选定的方向。

STORY: 发现页大幅主作品 → 连续选片索引 → 有尺度变化的社区作品 → 专题策展入口。作品详情先完整看图，故事与交流在画面下方展开。创作中心与后台遵循 Operate：清晰的任务分组、标准表单与表格。

FIRST VIEWPORT: 照片占据主空间；作品标题有可靠对比；图片切换为一次短促显影。缩略图拥有可访问名称、当前状态与键盘路径。公共页面不出现管理员面板。

FORM: candidate 3, seed:e8d074688c48e620。保留接触印样机制；展示字使用自托管衬线字库。移动端重排，不缩小整张桌面页面。开发摄影预览需显式 ?demo=photography，生产构建排除演示模块。

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
