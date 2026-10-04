import { type Page } from '@playwright/test'
import type { AuthResponse } from '../src/api/client'

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
  const { token, user } = await res.json() as AuthResponse
  const requestedOrg = process.env.TEST_ORG_ID
  const org = requestedOrg
    ? user.organizations.find(org => String(org.orgId) === requestedOrg)
    : user.organizations[0]
  if (!org) throw new Error('E2E admin must belong to the selected test organization (TEST_ORG_ID)')
  await page.evaluate(({ token, orgId }) => {
    localStorage.setItem('verdant_token', token)
    localStorage.setItem('verdant_org_id', String(orgId))
  }, { token, orgId: org.orgId })
}

/** Navigate to the app root and wait for the main heading */
export async function goToDashboard(page: Page) {
  await page.goto('/')
  await page.waitForSelector('h1', { timeout: 10000 })
}

/** Headers for direct Playwright API calls, matching the browser client. */
export async function apiHeaders(page: Page) {
  return page.evaluate(() => {
    const token = localStorage.getItem('verdant_token')
    const orgId = localStorage.getItem('verdant_org_id')
    if (!token || !orgId) throw new Error('Log in and select an organization first')
    return { Authorization: `Bearer ${token}`, 'X-Organization-Id': orgId }
  })
}
