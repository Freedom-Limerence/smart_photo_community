import { expect, test, type Page } from '@playwright/test'

async function fixture(page: Page) {
  await page.addInitScript(() => {
    const original = CanvasRenderingContext2D.prototype.drawImage
    const state = window as unknown as { photoFrames: number[][] }
    state.photoFrames = []
    CanvasRenderingContext2D.prototype.drawImage = function (...args: Parameters<typeof original>) {
      if (args[0] instanceof HTMLImageElement && args[0].src.includes('portrait-fixture.svg') && args.length === 9) {
        state.photoFrames.push([Number(args[7]), Number(args[8])])
        if (state.photoFrames.length > 20) state.photoFrames.shift()
      }
      return original.apply(this, args)
    }
  })
  await page.route('**/portrait-fixture.svg', route => route.fulfill({ contentType: 'image/svg+xml', body: '<svg xmlns="http://www.w3.org/2000/svg" width="80" height="120"><rect width="80" height="120" fill="#7c8b7f"/></svg>' }))
  await page.route('**/cosmos/graph**', route => route.fulfill({ json: { code: 0, data: {
    hubs: [{ tag: '城市', count: 1 }, { tag: '夜景', count: 1 }],
    leaves: [{ workId: '42', title: '街灯', author: '摄影师', cover: '/portrait-fixture.svg', heat: 1, tags: ['城市', '夜景'] }],
    // Older services send only one edge; the page restores the second from tags.
    edges: [{ hub: '城市', leaf: '42' }],
  } } }))
  await page.route('**/work/get/vo**', route => route.fulfill({ json: { code: 0, data: {
    id: '42', title: '街灯', authorId: '7', author: { userName: '摄影师' }, pictures: [{ id: '1', url: '/portrait-fixture.svg' }],
  } } }))
  await page.route('**/work/comments**', route => route.fulfill({ json: { code: 0, data: [] } }))
  await page.route('**/work/related**', route => route.fulfill({ json: { code: 0, data: [] } }))
}
async function frames(page: Page) {
  return page.evaluate(() => (window as unknown as { photoFrames: number[][] }).photoFrames)
}
async function clearFrames(page: Page) {
  await page.evaluate(() => { (window as unknown as { photoFrames: number[][] }).photoFrames = [] })
}
async function searchWork(page: Page, title: string) {
  await page.getByRole('textbox', { name: '搜索题材、作品或摄影师' }).fill(title)
  await page.locator('.search-results button').first().click()
  await expect(page.locator('.graph-canvas')).toHaveAttribute('data-level', 'photos')
  await page.waitForTimeout(650)
}

test('初始仅题材星点，放大出现照片，缩小隐藏照片，重排保留竖幅', async ({ page }) => {
  await fixture(page)
  await page.goto('/cosmos')
  const canvas = page.locator('.graph-canvas')
  await expect(canvas).toHaveAttribute('data-level', 'topics')
  await expect(page.locator('.overview-intro')).toContainText('2 条真实关联')
  await page.waitForTimeout(1100)
  expect(await frames(page)).toEqual([])
  const box = (await canvas.boundingBox())!
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.mouse.wheel(0, -550)
  await expect(canvas).toHaveAttribute('data-level', 'photos')
  await expect.poll(async () => (await frames(page)).length).toBeGreaterThan(0)
  expect((await frames(page)).every(([w, h]) => h! > w!)).toBe(true)
  await page.getByRole('button', { name: '返回题材星图' }).click()
  await expect(canvas).toHaveAttribute('data-level', 'topics')
  await page.waitForTimeout(650)
  await clearFrames(page)
  await page.waitForTimeout(200)
  expect(await frames(page)).toEqual([])
  await page.getByRole('button', { name: '重新布局' }).click()
  await page.waitForTimeout(1200)
  await searchWork(page, '街灯')
  await expect.poll(async () => (await frames(page)).length).toBeGreaterThan(0)
  expect((await frames(page)).every(([w, h]) => h! > w!)).toBe(true)
})

test('点击放大后的照片直接进入对应作品详情', async ({ page }) => {
  await fixture(page)
  await page.goto('/cosmos')
  await searchWork(page, '街灯')
  const box = (await page.locator('.graph-canvas').boundingBox())!
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.waitForTimeout(150)
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
  await expect(page).toHaveURL(/\/works\/42$/)
  await expect(page.locator('.work-page h1')).toHaveText('街灯')
  await expect(page.locator('.lightbox, .graph-inspector')).toHaveCount(0)
})

test('桌面题材自动靠近，节点拖拽固定与画布平移保持可用', async ({ page }) => {
  const errors: string[] = []
  page.on('pageerror', e => errors.push(e.message))
  await page.goto('/cosmos?demo=photography')
  const canvas = page.locator('.graph-canvas')
  await expect(canvas).toHaveAttribute('data-level', 'topics')
  await page.waitForTimeout(1200)
  await page.locator('.theme-index button').filter({ hasText: '自然光' }).click()
  await expect(canvas).toHaveAttribute('data-level', 'photos')
  await page.waitForTimeout(700)
  const box = (await canvas.boundingBox())!
  const x = box.x + box.width / 2, y = box.y + box.height / 2
  await page.mouse.move(x, y)
  await page.waitForTimeout(150)
  await page.mouse.down()
  await page.mouse.move(x + 85, y + 45, { steps: 12 })
  await page.mouse.up()
  await expect(page.locator('.graph-announcement')).toContainText('已移动并固定「自然光」')
  const beforePan = await canvas.getAttribute('data-pan')
  await page.mouse.move(box.x + 15, box.y + 15)
  await page.mouse.down()
  await page.mouse.move(box.x + 85, box.y + 50, { steps: 8 })
  await page.mouse.up()
  await expect.poll(() => canvas.getAttribute('data-pan')).not.toBe(beforePan)
  await page.getByRole('button', { name: '返回题材星图' }).click()
  await expect(canvas).toHaveAttribute('data-level', 'topics')
  expect(errors).toEqual([])
})

test('摄影演示也能进入只读详情，不请求虚构作品接口', async ({ page }) => {
  const demoRequests: string[] = []
  page.on('request', req => { if (req.url().includes('/api/') && req.url().includes('demo-')) demoRequests.push(req.url()) })
  await page.goto('/cosmos?demo=photography')
  await searchWork(page, '守望者')
  const box = (await page.locator('.graph-canvas').boundingBox())!
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2)
  await page.waitForTimeout(150)
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2)
  await expect(page).toHaveURL(/\/works\/demo-002\?demo=photography/)
  await expect(page.locator('.work-page h1')).toContainText('守望者')
  await expect(page.locator('.work-page .actions, .work-page .composer')).toHaveCount(0)
  expect(demoRequests).toEqual([])
})
