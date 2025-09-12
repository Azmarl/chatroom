/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'

  // 将最后的 a.tsny 替换为 unknown，这是类型安全的“任何类型”
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>

  export default component
}
