import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import path from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  base: '/task-manager/',
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 5173,
    // Proxy API calls to Spring Boot services during development.
    // ORDER MATTERS: more-specific paths must come before '/api'.
    proxy: {
      // Notifications service (port 8081) — must be listed before '/api'
      '/api/notifications': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      // Task Manager API (port 8080)
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
