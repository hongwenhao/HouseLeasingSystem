/**
 * vite.config.js —— Vite 构建工具配置文件
 *
 * 主要配置内容：
 *   1. 启用 Vue 单文件组件支持（@vitejs/plugin-vue）
 *   2. 配置开发服务器代理：将 /api 前缀的请求转发到后端
 *      Spring Boot 服务（localhost:8080），解决开发期跨域问题
 */

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // 插件配置：启用 Vue SFC 支持
  plugins: [vue()],
  server: {
    // 开发服务器代理配置
    proxy: {
      // 匹配所有以 /api 开头的请求
      '/api': {
        // 代理目标：后端 Spring Boot 服务地址
        target: 'http://localhost:8080',
        // 修改请求头中的 Origin 为目标地址，避免跨域限制
        changeOrigin: true
      }
    }
  }
})
