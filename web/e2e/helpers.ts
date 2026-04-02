import { type Page } from '@playwright/test'

const API_BASE = 'http://localhost:8081'

/**
 * Log in by directly calling the admin auth endpoint and storing the token.
 * Requires TEST_ADMIN_EMAIL and TEST_ADMIN_PASSWORD env vars or defaults.
 */
export async function loginAsAdmin(page: Page) {
  const res = await page.request.post(`${API_BASE}/api/auth/admin`, {
    data: {
      email: process.env.TEST_ADMIN_EMAIL ?? 'admin@verdant.app',
      password: process.env.TEST_ADMIN_PASSWORD ?? 'admin',
    },
  })
  if (!res.ok()) throw new Error(`Admin login failed: ${res.status()}`)
  const { token } = await res.json()
  await page.evaluate((t) => localStorage.setItem('verdant_token', t), token)
}

/** Navigate to the app root and wait for the main heading */
export async function goToDashboard(page: Page) {
  await page.goto('/')
  await page.waitForSelector('h1', { timeout: 10000 })
}
