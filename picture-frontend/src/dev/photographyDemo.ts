/**
 * 摄影演示数据（仅开发环境 ?demo=photography 时经 loadPhotographyPreview 动态加载）。
 * - 图片来源：AI 生成的示例摄影内容，存于 public/demo/，仅用于视觉预览，不代表真实用户作品。
 * - 生产构建：photographyPreview.ts 中由 import.meta.env.DEV 门控，此模块不会进入生产包。
 * - 数据形状与后端 WorkVO 对齐（id/title/author/pictures/coverUrl/favorited），演示 id 一律以 demo- 前缀。
 */

const IMG = {
  mist: '/demo/hero-mist.png',
  fisher: '/demo/portrait-fisher.png',
  cyclist: '/demo/street-cyclist.png',
  sea: '/demo/sea-dawn.png',
}

function pictures(...urls: string[]) {
  return urls.map((url) => ({ url, thumbnailUrl: url }))
}

export interface DemoWork {
  id: string
  title: string
  introduction: string
  author: { userName: string }
  coverUrl?: string
  thumbnailUrl?: string
  pictures: { url: string; thumbnailUrl: string }[]
  favorited?: boolean
}

export const demoWorks: DemoWork[] = [
  {
    id: 'demo-001',
    title: '雾起时，山把轮廓交还给光',
    introduction: '清晨五点的山脊线，雾是唯一的主角。',
    author: { userName: '林间纪事' },
    coverUrl: IMG.mist,
    pictures: pictures(IMG.mist, IMG.sea),
    favorited: true,
  },
  {
    id: 'demo-002',
    title: '守望者',
    introduction: '老码头的最后一代渔民。',
    author: { userName: '阿岸' },
    coverUrl: IMG.fisher,
    pictures: pictures(IMG.fisher),
  },
  {
    id: 'demo-003',
    title: '雨停之后的十字路口',
    introduction: '决定性瞬间：湿漉漉的街道与唯一的暖光。',
    author: { userName: '陈屿' },
    coverUrl: IMG.cyclist,
    pictures: pictures(IMG.cyclist),
  },
  {
    id: 'demo-004',
    title: '黎明前的静默',
    introduction: '长曝光下的海平线，一艘早航的船。',
    author: { userName: '远帆' },
    coverUrl: IMG.sea,
    pictures: pictures(IMG.sea, IMG.mist, IMG.sea),
  },
  {
    id: 'demo-005',
    title: '山雾的第五个层次',
    introduction: '同一座山，五种密度不同的雾。',
    author: { userName: '陆离' },
    coverUrl: IMG.mist,
    pictures: pictures(IMG.mist),
  },
  {
    id: 'demo-006',
    title: '归途',
    introduction: '把一整天的光收进船舱。',
    author: { userName: '阿岸' },
    coverUrl: IMG.fisher,
    pictures: pictures(IMG.fisher, IMG.sea),
  },
]

/** 影像星座的专用演示关系；页面通过同一个开发环境门控动态加载。 */
export const demoCosmosGraph = (() => {
  const themes: Record<string, string[]> = {
    'demo-001': ['山与雾', '自然光', '清晨'],
    'demo-002': ['人文纪事', '自然光'],
    'demo-003': ['城市切片', '人文纪事'],
    'demo-004': ['海的边界', '清晨'],
    'demo-005': ['山与雾', '自然光'],
    'demo-006': ['海的边界', '人文纪事'],
  }
  const count = new Map<string, number>()
  demoWorks.forEach((work) => themes[work.id]?.forEach((tag) => count.set(tag, (count.get(tag) || 0) + 1)))
  return {
    hubs: [...count].sort((a, b) => b[1] - a[1]).map(([tag, total]) => ({ tag, count: total })),
    leaves: demoWorks.map((work) => ({
      workId: work.id,
      title: work.title,
      author: work.author.userName,
      cover: work.coverUrl || work.pictures[0]?.url || '',
      heat: 0,
      hub: themes[work.id]?.[0] || '',
      tags: themes[work.id] || [],
    })),
    edges: demoWorks.flatMap((work) => (themes[work.id] || []).map((tag) => ({ hub: tag, leaf: work.id }))),
  }
})()
