import { defineConfig } from '@playwright/test'

// Uses the existing browser test tooling, with the admin frontend and an isolated local backend.
export default defineConfig({
  testDir: './e2e',
  testMatch: 'admin-planning.spec.ts',
  timeout: 60000,
  workers: 1,
  use: { baseURL: 'http://localhost:5174', headless: true, screenshot: 'only-on-failure' },
  webServer: { command: 'npm --prefix ../admin run dev -- --host 127.0.0.1 --strictPort', port: 5174, reuseExistingServer: true },
})
