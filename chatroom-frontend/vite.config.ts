import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // Import path for alias

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    }
  },
  server: {
    proxy: {
      // Proxy requests based on your AuthController
      '/login': { target: 'http://localhost:8080', changeOrigin: true },
      '/register': { target: 'http://localhost:8080', changeOrigin: true },
      '/logout': { target: 'http://localhost:8080', changeOrigin: true },
      // Proxy requests for the /api path
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  },
  define: {
    global: 'window',
  },
})
