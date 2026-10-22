import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  // server: {
  //   proxy: {
  //     '/api': 'http://localhost:8123'
  //   }
  // },
  plugins: [
    vue(),
    vueDevTools(),
  ],
  server: {
    proxy: {
      // COS 图片同源反代：消除跨域（WebGL 纹理 taint / CORS 行为差异），生产由 nginx 提供同款 /cos 反代
      '/cos': {
        target: 'https://wzx-1428719669.cos.ap-beijing.myqcloud.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/cos/, ''),
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    // WorkBuddy 沙箱内回收站二进制清 dist 会 ETIMEDOUT；hash 文件名无覆盖风险
    emptyOutDir: false,
  },
})
