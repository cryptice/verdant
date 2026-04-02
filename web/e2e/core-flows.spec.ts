import { test, expect, type Page } from '@playwright/test'
import { loginAsAdmin } from './helpers'

const API_BASE = 'http://localhost:8081'

// ── API helpers ──────────────────────────────────────────────────────────────

async function apiPost<T>(page: Page, path: string, body: unknown): Promise<T> {
  const token = await page.evaluate(() => localStorage.getItem('verdant_token'))
  const res = await page.request.post(`${API_BASE}${path}`, {
    data: body,
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!res.ok()) throw new Error(`POST ${path} failed: ${res.status()} ${await res.text()}`)
  return res.json()
}

async function apiDelete(page: Page, path: string): Promise<void> {
  const token = await page.evaluate(() => localStorage.getItem('verdant_token'))
  await page.request.delete(`${API_BASE}${path}`, {
    headers: { Authorization: `Bearer ${token}` },
  })
}

// ── Test: login page ──────────────────────────────────────────────────────────

test('login page shows Verdant branding', async ({ page }) => {
  await page.goto('/login')
  await expect(page.locator('text=Verdant').first()).toBeVisible()
})

// ── Test: authenticated dashboard ────────────────────────────────────────────

test('authenticated user sees the dashboard', async ({ page }) => {
  await page.goto('/')
  await loginAsAdmin(page)
  await page.goto('/')
  await page.waitForSelector('h1', { timeout: 10000 })

  // Dashboard renders a personalised greeting ("Hej, <name>")
  const greeting = page.locator('h1')
  await expect(greeting).toBeVisible()

  // "New garden" button is present
  await expect(page.getByRole('button', { name: /ny trädgård|new garden/i })).toBeVisible()
})

// ── Serial suite: create season → garden → bed → sow ─────────────────────────
//
// Each test builds on the previous one's data.  Shared IDs are stored in
// module-level variables so the serial suite can pass them between tests.

let seasonId: number
let gardenId: number
let gardenName: string
let bedId: number
let speciesId: number

test.describe.serial('core CRUD flows', () => {
  // Log in once, then reuse the same page context for all tests in the suite.
  test.beforeEach(async ({ page }) => {
    await page.goto('/')
    await loginAsAdmin(page)
  })

  // ── 1. Create a season ──────────────────────────────────────────────────────

  test('create a season', async ({ page }) => {
    await page.goto('/seasons')
    await page.waitForSelector('h1', { timeout: 10000 })

    // Open the "new season" dialog
    await page.getByRole('button', { name: /ny säsong|new season/i }).click()

    // Fill name and year — these are the only required fields
    const nameInput = page.locator('[placeholder="e.g. Spring 2026"]')
    await nameInput.fill('E2E Test Season')

    const yearInput = page.locator('[placeholder="e.g. 2026"]')
    await yearInput.fill('2026')

    // Submit
    await page.getByRole('button', { name: /^lägg till$|^add$/i }).click()

    // Dialog closes and the new season appears in the table
    await expect(page.locator('text=E2E Test Season')).toBeVisible({ timeout: 8000 })

    // Capture the season id for cleanup via the API
    const res = await page.request.get(`${API_BASE}/api/seasons`, {
      headers: {
        Authorization: `Bearer ${await page.evaluate(() => localStorage.getItem('verdant_token'))}`,
      },
    })
    const seasons = await res.json()
    const created = seasons.find((s: { name: string; id: number }) => s.name === 'E2E Test Season')
    expect(created).toBeDefined()
    seasonId = created.id
  })

  // ── 2. Create a garden ─────────────────────────────────────────────────────

  test('create a garden', async ({ page }) => {
    await page.goto('/garden/new')
    await page.waitForSelector('h1', { timeout: 10000 })

    gardenName = `E2E Garden ${Date.now()}`
    const nameInput = page.locator('input.input')
    await nameInput.fill(gardenName)

    // Submit — button text is "Skapa trädgård" / "Create garden"
    await page.getByRole('button', { name: /skapa trädgård|create garden/i }).click()

    // On success the app redirects to /garden/:id
    await page.waitForURL(/\/garden\/\d+/, { timeout: 10000 })

    // Garden name appears as the page heading
    await expect(page.locator('h1')).toHaveText(gardenName, { timeout: 8000 })

    gardenId = Number(page.url().match(/\/garden\/(\d+)/)?.[1])
    expect(gardenId).toBeGreaterThan(0)
  })

  // ── 3. Create a bed in the garden ─────────────────────────────────────────

  test('create a bed in a garden', async ({ page }) => {
    await page.goto(`/garden/${gardenId}`)
    await page.waitForSelector('h1', { timeout: 10000 })

    // "+ Ny bädd" / "+ New bed" button
    const addBedBtn = page.getByRole('button', { name: /ny bädd|new bed/i })
    await expect(addBedBtn).toBeVisible()
    await addBedBtn.click()

    // Navigated to /garden/:id/bed/new
    await page.waitForURL(/\/garden\/\d+\/bed\/new/, { timeout: 8000 })

    const nameInput = page.locator('input.input')
    await nameInput.fill('E2E Test Bed')

    await page.getByRole('button', { name: /skapa bädd|create bed/i }).click()

    // Redirects back to garden detail
    await page.waitForURL(`/garden/${gardenId}`, { timeout: 10000 })

    // Bed appears in the list
    await expect(page.locator('text=E2E Test Bed')).toBeVisible({ timeout: 8000 })

    // Capture bed id via the API
    const token = await page.evaluate(() => localStorage.getItem('verdant_token'))
    const res = await page.request.get(`${API_BASE}/api/gardens/${gardenId}/beds`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    const beds = await res.json()
    const created = beds.find((b: { name: string; id: number }) => b.name === 'E2E Test Bed')
    expect(created).toBeDefined()
    bedId = created.id
  })

  // ── 4. Sow seeds ───────────────────────────────────────────────────────────
  //
  // We need a species to sow.  Create one via the API so the test stays
  // focused on the sow UI rather than the species creation UI.

  test('sow seeds into a bed', async ({ page }) => {
    // Ensure a species exists that we can sow
    const species = await apiPost<{ id: number }>(page, '/api/species', {
      commonName: 'E2E Tomato',
      commonNameSv: 'E2E Tomat',
    })
    speciesId = species.id

    // Navigate to /sow with the bed pre-selected via query param
    await page.goto(`/sow?bedId=${bedId}`)
    await page.waitForSelector('h1', { timeout: 10000 })

    // Type in the species autocomplete
    const speciesInput = page.locator('input.input').first()
    await speciesInput.fill('E2E Tomat')
    // Wait for the dropdown result and click it
    await page.locator('button:has-text("E2E Tomat")').first().waitFor({ timeout: 8000 })
    await page.locator('button:has-text("E2E Tomat")').first().click()

    // Fill seed count
    const seedCountInput = page.locator('input[type="number"]')
    await seedCountInput.fill('5')

    // Submit — button text is "Så" / "Sow"
    const sowBtn = page.locator('[data-onboarding="sow-submit"]')
    await expect(sowBtn).toBeEnabled({ timeout: 5000 })
    await sowBtn.click()

    // On success navigate(-1) is called, which takes us back to the bed page
    await page.waitForURL(/\/bed\/\d+|\/garden\/\d+|\//, { timeout: 10000 })
  })

  // ── Cleanup ────────────────────────────────────────────────────────────────
  //
  // Run after all serial tests complete to leave the database clean.

  test.afterAll(async ({ page }) => {
    await page.goto('/')
    await loginAsAdmin(page)
    try {
      if (speciesId) await apiDelete(page, `/api/species/${speciesId}`)
    } catch { /* species may not exist yet if earlier tests failed */ }
    try {
      if (gardenId) await apiDelete(page, `/api/gardens/${gardenId}`)
    } catch { /* garden may not exist */ }
    try {
      if (seasonId) await apiDelete(page, `/api/seasons/${seasonId}`)
    } catch { /* season may not exist */ }
  })
})
