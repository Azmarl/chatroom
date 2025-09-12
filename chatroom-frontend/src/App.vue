<template>
  <div id="app-wrapper">
    <!-- 使用 v-if 来决定是否渲染 SideBar 组件 -->
    <SideBar v-if="showSidebar" />
    
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SideBar from './components/SideBar.vue'

// 1. 获取当前路由信息的响应式对象
const route = useRoute()

// 2. 创建一个计算属性来判断是否应该显示侧边栏
const showSidebar = computed(() => {
  // 如果当前路由的 name 是 'login' 或 'register'，则不显示侧边栏
  // 我们检查 route.name 是否在我们的“无侧边栏”列表中
  return !['Login', 'Register'].includes(route.name as string);
})
</script>

<style lang="scss">
/* 全局样式 (无需修改) */
html, body {
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
  overflow: hidden;
}

#app-wrapper {
  display: flex;
  height: 100vh;
  width: 100vw;
}

.main-content {
  flex-grow: 1; 
  height: 100vh;
  overflow: hidden; 
}
</style>
