#!/usr/bin/env node
/**
 * 设计门禁（P0-5）：把"不再恶化"变成可见数字。
 *
 * 统计两个指标并与基线比较（只许变好，不许回潮）：
 *  1. hex     —— .vue 文件中硬编码颜色出现次数（令牌外颜色）
 *  2. smallFont —— font-size < 12px 出现次数（中文小字下限）
 *
 * 用法：npm run design:gate
 *  - 基线不存在时：写入当前值并通过（首次建档）
 *  - 当前值 <= 基线：通过，并将基线更新为当前值（棘轮下降）
 *  - 当前值 >  基线：失败并列出恶化指标
 */
import { readdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const srcDir = join(root, 'src')
const baselinePath = join(root, 'scripts', 'design-gate-baseline.json')

const HEX_RE = /#[0-9a-fA-F]{6}\b|#[0-9a-fA-F]{3}\b/g
const SMALL_FONT_RE = /font-size:\s*(?:9|10|11)px\b/g

function walk(dir, out = []) {
  let entries
  try { entries = readdirSync(dir, { withFileTypes: true }) } catch { return out }
  for (const e of entries) {
    // 跳过渲染失败遗留的 .tmpdir 垃圾目录与其他隐藏目录
    if (e.name.startsWith('.')) continue
    const p = join(dir, e.name)
    if (e.isDirectory()) walk(p, out)
    else if (e.isFile() && e.name.endsWith('.vue')) out.push(p)
  }
  return out
}

const files = walk(srcDir).filter((f) => !f.includes('.tmpdir'))
let hex = 0
let smallFont = 0
for (const f of files) {
  const text = readFileSync(f, 'utf8')
  hex += (text.match(HEX_RE) || []).length
  smallFont += (text.match(SMALL_FONT_RE) || []).length
}

const current = { hex, smallFont, files: files.length, generatedAt: new Date().toISOString() }

if (!existsSync(baselinePath)) {
  writeFileSync(baselinePath, JSON.stringify(current, null, 2) + '\n')
  console.log(`[design-gate] 基线已建档：hex=${hex}, smallFont=${smallFont} (${files.length} 个 .vue 文件)`)
  process.exit(0)
}

const baseline = JSON.parse(readFileSync(baselinePath, 'utf8'))
const regressions = []
if (current.hex > baseline.hex) regressions.push(`hex: ${baseline.hex} -> ${current.hex}（+${current.hex - baseline.hex}）`)
if (current.smallFont > baseline.smallFont) regressions.push(`smallFont: ${baseline.smallFont} -> ${current.smallFont}（+${current.smallFont - baseline.smallFont}）`)

if (regressions.length) {
  console.error('[design-gate] ✗ 设计指标回潮：')
  for (const r of regressions) console.error('  - ' + r)
  console.error('  请改用设计令牌（tokens.css）或提升小字至 12px 后重试。')
  process.exit(1)
}

// 通过：棘轮下降——基线永远等于历史最好成绩
writeFileSync(baselinePath, JSON.stringify(current, null, 2) + '\n')
const improved = []
if (current.hex < baseline.hex - 0) improved.push(`hex ${baseline.hex}→${current.hex}`)
if (current.smallFont < baseline.smallFont) improved.push(`smallFont ${baseline.smallFont}→${current.smallFont}`)
console.log(`[design-gate] ✓ 通过：hex=${current.hex}, smallFont=${current.smallFont}${improved.length ? '（改善：' + improved.join(', ') + '）' : ''}`)
