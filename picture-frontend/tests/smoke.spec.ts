import { expect, test } from '@playwright/test'

/**
 * 冒烟套件（五条主链路）：
 * 1. 首页加载（hero + feed）
 * 2. 作品详情
 * 3. 登录流程（e2e-admin）
 * 4. 后台看板（KPI + 分组菜单 + 保持亮色）
 * 5. 展览页渲染
 *
 * 选择器优先用语义角色/文本，避免依赖实现细节。
 */

test.describe('首页', () => {
  test('hero 印样与作品 feed 均渲染', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.contact-sheet')).toBeVisible()
    await expect(page.locator('.sheet-title')).toBeVisible()
    await expect(page.locator('.feed-heading h2, .feed-heading h1')).toContainText(/光影|视角/)
    await expect(page.locator('.contact-feed .frame').first()).toBeVisible({ timeout: 15_000 })
  })

  test('点作品卡可进入详情', async ({ page }) => {
    await page.goto('/')
    const firstFrame = page.locator('.contact-feed .frame').first()
    // Lenis 平滑滚动接管原生 scrollIntoView，用真实滚轮滚动触发揭示
    await page.mouse.wheel(0, 900)
    await page.waitForTimeout(300)
    await page.mouse.wheel(0, 600)
    // clip-path 揭示完成后中心点才可命中
    await expect(firstFrame).toHaveClass(/is-revealed/, { timeout: 15_000 })
    await page.locator('.contact-feed .frame .frame-image').first().click()
    await expect(page).toHaveURL(/\/works\/\d+/)
    await expect(page.locator('.stage-img')).toBeVisible()
  })
})

test.describe('作品详情', () => {
  test('核心区块完整：舞台 / 标题 / 互动区', async ({ page }) => {
    await page.goto('/works/76')
    await expect(page.locator('.stage-img')).toBeVisible()
    await expect(page.locator('.work-page h1, .detail-view h1, .work-page h2').first()).toBeVisible()
    await expect(page.locator('.act').first()).toBeVisible()
    await expect(page.locator('.notes')).toBeVisible()
  })
})

test.describe('登录与后台', () => {
  test('e2e-admin 登录成功并进入后台看板', async ({ page }) => {
    await page.goto('/user/login')
    const inputs = page.locator('.ant-input')
    await inputs.nth(0).fill('e2e-admin')
    await inputs.nth(1).fill('e2e123456')
    await page.locator('button[type=submit], .ant-btn-primary').first().click()
    await page.waitForURL((u) => !u.pathname.includes('/user/login'), { timeout: 15_000 })

    await page.goto('/admin/dashboard')
    await expect(page.locator('.admin-layout')).toBeVisible()
    await expect(page.locator('.metric').first()).toBeVisible()
    // 后台保持亮色（前暗后亮差异化）
    await expect(page.locator('.admin-layout')).toHaveCSS('background-color', 'rgb(247, 247, 245)')
  })

  test('后台菜单分组渲染', async ({ page }) => {
    await page.goto('/user/login')
    const inputs = page.locator('.ant-input')
    await inputs.nth(0).fill('e2e-admin')
    await inputs.nth(1).fill('e2e123456')
    await page.locator('button[type=submit], .ant-btn-primary').first().click()
    await page.waitForURL((u) => !u.pathname.includes('/user/login'), { timeout: 15_000 })

    await page.goto('/admin/dashboard')
    await expect(page.locator('.admin-menu .ant-menu-item-group-title').first()).toBeVisible()
    await expect(page.locator('.role-tag')).toHaveText(/管理员/)
  })
})

test.describe('公开页', () => {
  test('展览页标题与卡片网格', async ({ page }) => {
    await page.goto('/exhibitions')
    await expect(page.locator('.page-heading h1')).toHaveText('专题展览')
    await expect(page.locator('.exhibition-grid').first()).toBeVisible()
  })

  test('标签广场渲染', async ({ page }) => {
    await page.goto('/tags')
    await expect(page.locator('.tag-index .tag-head').first()).toBeVisible()
  })
})
