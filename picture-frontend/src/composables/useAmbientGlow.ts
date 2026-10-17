import { ref, watch } from 'vue'

const cache = new Map<string, string>()

/**
 * 环境光晕（P2-1）：从图片提取主色，供舞台呼吸光使用。
 * - 24×24 降采样取平均亮度适中的像素，过暗自动提亮
 * - canvas 被 CORS 污染或加载失败时返回 ''（CSS 有兜底色）
 * - 结果按 URL 缓存，翻页不重复计算
 */
export function useAmbientGlow(getUrl: () => string | undefined) {
  const glow = ref('')

  async function extract(url?: string) {
    if (!url) {
      glow.value = ''
      return
    }
    const cached = cache.get(url)
    if (cached) {
      glow.value = cached
      return
    }
    try {
      const img = new Image()
      img.crossOrigin = 'anonymous'
      // 独立缓存键：同 URL 的普通 <img> 响应（无 CORS 头）会被浏览器缓存并连累匿名请求，
      // 追加查询参数让取色请求拥有自己的缓存条目
      const glowUrl = url + (url.includes('?') ? '&' : '?') + 'glow=1'
      await new Promise<void>((resolve, reject) => {
        img.onload = () => resolve()
        img.onerror = () => reject(new Error('load failed'))
        img.src = glowUrl
      })
      const size = 24
      const canvas = document.createElement('canvas')
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d', { willReadFrequently: true })
      if (!ctx) throw new Error('no ctx')
      ctx.drawImage(img, 0, 0, size, size)
      const data = ctx.getImageData(0, 0, size, size).data
      let r = 0
      let g = 0
      let b = 0
      let n = 0
      for (let i = 0; i < data.length; i += 4) {
        const pr = data[i]
        const pg = data[i + 1]
        const pb = data[i + 2]
        const lum = 0.299 * pr + 0.587 * pg + 0.114 * pb
        if (lum < 18 || data[i + 3] < 128) continue
        r += pr
        g += pg
        b += pb
        n++
      }
      if (!n) throw new Error('empty')
      r = Math.round(r / n)
      g = Math.round(g / n)
      b = Math.round(b / n)
      const max = Math.max(r, g, b)
      if (max < 90) {
        const k = 90 / (max || 1)
        r = Math.min(255, r * k + 30)
        g = Math.min(255, g * k + 30)
        b = Math.min(255, b * k + 30)
      }
      const color = `rgba(${Math.round(r)},${Math.round(g)},${Math.round(b)},0.42)`
      glow.value = color
      cache.set(url, color)
    } catch {
      glow.value = ''
    }
  }

  watch(getUrl, (v) => void extract(v), { immediate: true })
  return glow
}
