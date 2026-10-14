import { saveAs } from 'file-saver'

/**
 * 格式化文件大小
 * @param size
 */
export const formatSize = (size?: number) => {
  if (!size) return '未知'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  return (size / (1024 * 1024)).toFixed(2) + ' MB'
}

/**
 * 下载图片
 * @param url 图片下载地址
 * @param fileName 要保存为的文件名
 */
export function downloadImage(url?: string, fileName?: string) {
  if (!url) {
    return
  }
  saveAs(url, fileName)
}

/**
 * 将颜色值转换为标准 #RRGGBB 格式
 * @param input
 */
export function toHexColor(input: string) {
  // 去掉 0x 前缀
  const colorValue = input.startsWith('0x') ? input.slice(2) : input

  // 将剩余部分解析为十六进制数，再转成 6 位十六进制字符串
  const hexColor = parseInt(colorValue, 16).toString(16).padStart(6, '0')

  // 返回标准 #RRGGBB 格式
  return `#${hexColor}`
}


/**
 * 安全解析雪花 ID（Long）。
 * 雪花 ID 超出 JS 安全整数范围（2^53），Number() 转换会丢失末尾精度，
 * 因此 ID 一律以字符串形式流转，仅做格式校验。
 * @param value 任意来源的 ID（route query / props / 后端返回等）
 * @returns 合法时返回纯数字字符串，否则返回 undefined
 */
export function parseId(value: unknown): string | undefined {
  if (value == null) return undefined
  const str = typeof value === 'string' ? value.trim() : String(value)
  return /^\d+$/.test(str) ? str : undefined
}
