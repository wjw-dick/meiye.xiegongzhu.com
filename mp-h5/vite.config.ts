import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5175,
    proxy: { '/api': { target: 'http://127.0.0.1:8081', changeOrigin: true } }
  },
  build: { outDir: 'dist', chunkSizeWarningLimit: 1200 }
})
