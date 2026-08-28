import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@verdant/shared': fileURLToPath(new URL('../shared/src/index.ts', import.meta.url)),
    },
  },
  server: {
    port: 5175,
    // The OAuth client authorises http://localhost:5175 as a JavaScript
    // origin. Vite's default is to move to the next free port when 5175 is
    // taken, which silently breaks Google sign-in with an origin mismatch —
    // fail to start instead, so the cause is obvious.
    strictPort: true,
    proxy: {
      '/api': 'http://localhost:8081',
    },
  },
})
