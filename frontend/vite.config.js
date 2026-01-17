import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// 完整版：保留你所有原有配置 + 新增代理解决HTTPS/IP/8443端口请求问题
export default defineConfig({
    plugins: [vue()],
    base: './',
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src')
        }
    },
    build: {
        target: 'es2015',
        outDir: 'dist'
    },
    // ========== 核心新增：开发环境代理配置 (解决你的请求拦截问题) ==========
    server: {
        proxy: {
            // 匹配所有以 /api 开头的请求，全部转发到你的后端服务器
            '/api': {
                target: 'http://123.207.43.214:8080', // 你的后端真实地址（不用改）
                changeOrigin: true, // 必须开启：解决跨域问题，模拟请求源一致
                secure: false,      // 必须开启：忽略后端SSL证书不可信的问题 ✔️ 核心解决你的报错
                rewrite: (path) => path.replace(/^\/api/, '') // 必须配置：把请求里的/api去掉再转发
            }
        }
    }
})