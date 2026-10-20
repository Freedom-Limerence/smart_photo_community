import { defineConfig } from '@playwright/test'

/**
 * 冒烟测试（2026-10-18）：五条主链路的快速安全网。
 * 前提：本地 dev 服务已在跑（vite 5173 / backend 8123），复用现有服务不另起 webServer。
 * 运行：npx playwright test tests/smoke.spec.ts
 */
export default defineConfig({
  testDir: './tests',
  outputDir: 'C:/Users/无妄生欢/AppData/Local/Temp/pw-results',
  timeout: 45_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  retries: 0,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:5173',
    screenshot: 'only-on-failure',
    viewport: { width: 1440, height: 900 },
  },
  projects: [{ name: 'chromium', use: { browserName: 'chromium' } }],
})
