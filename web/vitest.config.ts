import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import { fileURLToPath } from 'node:url'

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@verdant/shared': fileURLToPath(new URL('../shared/src/index.ts', import.meta.url)),
      'virtual:pwa-register/react': fileURLToPath(new URL('./src/test/pwaRegisterStub.ts', import.meta.url)),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    exclude: ['node_modules', 'dist', 'e2e'],
    // The deploy runs these alongside the image build on a shared machine, where the heavier form
    // tests take several times longer than locally; 5 s is too tight there.
    testTimeout: 20_000,
  },
})
