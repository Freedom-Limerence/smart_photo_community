/** Explicit, URL-scoped preview. The dynamic module is removed from production builds. */
export function isPhotographyPreview() {
  return import.meta.env.DEV && new URLSearchParams(window.location.search).get('demo') === 'photography'
}

export async function loadPhotographyPreview() {
  // DEV 必须内联判断：打包器据此静态消除动态 import，保证演示模块不进生产包
  if (!import.meta.env.DEV) return null
  if (new URLSearchParams(window.location.search).get('demo') !== 'photography') return null
  return import('../dev/photographyDemo')
}

export function isPreviewResource(id: unknown) {
  return isPhotographyPreview() && String(id).startsWith('demo-')
}
