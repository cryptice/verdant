import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  base: '/market/',
  server: {
    port: 5176,
    proxy: {
      '/api': 'http://localhost:8081'
    }
  }
})
