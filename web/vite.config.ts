import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath } from 'node:url'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      // A new version waits until the grower reloads (components/UpdatePrompt.tsx), so it never swaps the app
      // mid-task and the old version keeps its own files meanwhile.
      registerType: 'prompt',
      // Quarkus serves unknown extensions such as .webmanifest as text/html; .json goes out as JSON.
      manifestFilename: 'manifest.json',
      pwaAssets: { preset: 'minimal-2023', image: 'public/favicon.svg' },
      manifest: {
        name: 'Verdant Planner',
        short_name: 'Verdant',
        description: 'Plan, sow, grow and sell cut flowers.',
        display: 'standalone',
        start_url: '/',
        background_color: '#F5EFE2',
        theme_color: '#458262',
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico}'],
        // The admin UI, the API, MCP and Quarkus share this host: only the web app's own pages fall back to it.
        navigateFallbackDenylist: [/^\/admin/, /^\/api\//, /^\/mcp/, /^\/q\//, /^\/\.well-known\//, /\.html$/],
        // Google Fonts, kept so an offline start (or a bad connection) still has the app's own type.
        runtimeCaching: [
          {
            urlPattern: ({ url }) => url.origin === 'https://fonts.googleapis.com',
            handler: 'StaleWhileRevalidate',
            options: { cacheName: 'google-fonts-stylesheets' },
          },
          {
            urlPattern: ({ url }) => url.origin === 'https://fonts.gstatic.com',
            handler: 'CacheFirst',
            options: {
              cacheName: 'google-fonts-files',
              cacheableResponse: { statuses: [0, 200] },
              expiration: { maxEntries: 30, maxAgeSeconds: 365 * 24 * 60 * 60 },
            },
          },
        ],
      },
    }),
  ],
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
