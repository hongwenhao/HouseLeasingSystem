/**
 * main.js —— 应用入口文件
 *
 * 负责创建 Vue 应用实例，并完成以下全局注册：
 *   1. Pinia 状态管理（持久化用户状态）
 *   2. Vue Router 路由系统
 *   3. Element Plus UI 组件库（含全套图标注册）
 * 最终将应用挂载到 index.html 中的 #app 根节点。
 */

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
// 引入 Element Plus 全局样式
import 'element-plus/dist/index.css'
// 引入 Element Plus 图标库（全量注册）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router/index.js'

// 创建 Vue 应用实例
const app = createApp(App)

// 遍历并全局注册所有 Element Plus 图标组件，使其在任意组件中可直接使用
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册 Pinia 状态管理插件
app.use(createPinia())
// 注册路由插件
app.use(router)
// 注册 Element Plus UI 库
app.use(ElementPlus)

// 将应用挂载到页面中的 #app 容器
app.mount('#app')
