import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'
import type { Plugin } from 'vite'

// Identifies this build. The app compares it with /version.json to offer a reload once a newer build is deployed
// (components/UpdatePrompt.tsx).
const appVersion = new Date().toISOString()

function versionFile(): Plugin {
  return {
    name: 'verdant-version-file',
    apply: 'build',
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: 'version.json', source: JSON.stringify({ version: appVersion }) })
    },
  }
}

export default defineConfig({
  plugins: [react(), tailwindcss(), versionFile()],
  define: {
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(appVersion),
  },
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
