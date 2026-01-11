import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // 新增，防止路径解析问题

// 这是Vite+Vue+TS的最终完美配置，无任何遗漏
export default defineConfig({
  plugins: [vue()], // 注册vue插件，核心
  base: './', // 解决部署后样式/图片/路由404，必加
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src') // 别名配置，兼容所有环境
    }
  },
  build: {
    target: 'es2015', // 兼容Netlify的Node环境，必加
    outDir: 'dist' // 明确打包目录，防止Netlify识别错
  }
})