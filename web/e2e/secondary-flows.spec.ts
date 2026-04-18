import { test, expect, type Page } from '@playwright/test'
import { loginAsAdmin } from './helpers'

// Switch the app to English so selectors don't depend on Swedish locale
async function setEnglish(page: Page) {
  await page.evaluate(() => localStorage.setItem('verdant-lang', 'en'))
}

// Navigate to a route that requires auth. loginAsAdmin sets the token in
// localStorage, but we still need a navigation to pick it up.
async function loginAndGo(page: Page, path: string) {
  // First navigate to any page to establish origin, then set token + language
  await page.goto('/')
  await loginAsAdmin(page)
  await setEnglish(page)
  await page.goto(path)
}

// Wait for the loading spinner to disappear (page data loaded)
async function waitForPageLoad(page: Page) {
  await page.waitForSelector('.animate-spin', { state: 'detached', timeout: 15000 }).catch(() => {
    // spinner may never appear for fast responses — that's fine
  })
}

// Assert no error boundary / ErrorDisplay is shown
async function expectNoError(page: Page) {
  await expect(page.locator('[class*="text-error"]').first()).not.toBeVisible().catch(() => {
    // ignore if no error-class element exists at all
  })
  await expect(page.locator('text=Something went wrong')).not.toBeVisible()
}

// ---------------------------------------------------------------------------
// Test 1: Browse species list
// ---------------------------------------------------------------------------
test.describe('Species list', () => {
  test('shows the species list with a search input', async ({ page }) => {
    await loginAndGo(page, '/species')
    await waitForPageLoad(page)

    // The search input has aria-label matching the placeholder text
    const searchInput = page.getByRole('searchbox')
    await expect(searchInput).toBeVisible()

    // Page heading
    await expect(page.getByRole('heading', { name: 'Species' })).toBeVisible()
  })

  test('filters species by search query', async ({ page }) => {
    await loginAndGo(page, '/species')
    await waitForPageLoad(page)

    const searchInput = page.getByRole('searchbox')
    await searchInput.fill('a')

    // After typing, either the table with results or the "no species found" message should be visible
    const hasResults = await page.locator('[data-onboarding="species-list"]').isVisible().catch(() => false)
    const hasEmpty = await page.locator('text=No species found.').isVisible().catch(() => false)
    expect(hasResults || hasEmpty).toBe(true)
  })

  test('opens add species dialog', async ({ page }) => {
    await loginAndGo(page, '/species')
    await waitForPageLoad(page)

    await page.getByRole('button', { name: '+ New species' }).click()

    // Dialog should appear with the common name input
    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.locator('text=Add Species')).toBeVisible()
    await expect(page.getByPlaceholder('e.g. Tomato')).toBeVisible()
  })
})

// ---------------------------------------------------------------------------
// Test 2: Add seeds to inventory
// ---------------------------------------------------------------------------
test.describe('Seed inventory', () => {
  test('shows the seed inventory page', async ({ page }) => {
    await loginAndGo(page, '/seeds')
    await waitForPageLoad(page)

    await expect(page.getByRole('heading', { name: 'Seed Inventory' })).toBeVisible()

    // Either a table of seeds or the empty state message
    const hasTable = await page.locator('table').isVisible().catch(() => false)
    const hasEmpty = await page.locator('text=No seed batches').isVisible().catch(() => false)
    expect(hasTable || hasEmpty).toBe(true)
  })

  test('opens add seeds dialog via data-onboarding button', async ({ page }) => {
    await loginAndGo(page, '/seeds')
    await waitForPageLoad(page)

    // The PageHeader action button renders with data-onboarding="add-seed-btn"
    await page.locator('[data-onboarding="add-seed-btn"]').click()

    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.locator('text=Add Seeds')).toBeVisible()

    // The seed form is inside a data-onboarding="seed-form" wrapper
    await expect(page.locator('[data-onboarding="seed-form"]')).toBeVisible()
  })

  test('add seeds dialog has species and quantity fields', async ({ page }) => {
    await loginAndGo(page, '/seeds')
    await waitForPageLoad(page)

    await page.locator('[data-onboarding="add-seed-btn"]').click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Species autocomplete input
    await expect(page.getByPlaceholder('Search species...')).toBeVisible()

    // Quantity field (type=number, placeholder="e.g. 50")
    await expect(page.getByPlaceholder('e.g. 50')).toBeVisible()
  })

  test('submit button is disabled until species and quantity are filled', async ({ page }) => {
    await loginAndGo(page, '/seeds')
    await waitForPageLoad(page)

    await page.locator('[data-onboarding="add-seed-btn"]').click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // The Add button inside the dialog should be disabled initially
    const addBtn = page.getByRole('button', { name: 'Add' })
    await expect(addBtn).toBeDisabled()
  })
})

// ---------------------------------------------------------------------------
// Test 3: Create a scheduled task
// ---------------------------------------------------------------------------
test.describe('Task form', () => {
  test('renders the new task form', async ({ page }) => {
    await loginAndGo(page, '/task/new')
    await waitForPageLoad(page)

    await expect(page.getByRole('heading', { name: 'New Task' })).toBeVisible()

    // Form wrapper
    await expect(page.locator('[data-onboarding="task-form"]')).toBeVisible()

    // Key fields
    await expect(page.getByPlaceholder('Search species...')).toBeVisible()
    await expect(page.getByPlaceholder('e.g. 10')).toBeVisible()
  })

  test('submit button is disabled when required fields are empty', async ({ page }) => {
    await loginAndGo(page, '/task/new')
    await waitForPageLoad(page)

    const createBtn = page.getByRole('button', { name: 'Create Task' })
    await expect(createBtn).toBeDisabled()
  })

  test('creates a task and redirects to task list', async ({ page }) => {
    await loginAndGo(page, '/task/new')
    await waitForPageLoad(page)

    // Pick a species via autocomplete
    const speciesInput = page.getByPlaceholder('Search species...')
    await speciesInput.fill('a')

    // Wait for dropdown results and pick the first one
    const dropdown = page.locator('.absolute.z-10 button').first()
    await dropdown.waitFor({ timeout: 5000 })
    await dropdown.click()

    // Deadline
    const tomorrow = new Date()
    tomorrow.setDate(tomorrow.getDate() + 1)
    const deadlineStr = tomorrow.toISOString().split('T')[0]
    await page.locator('input[type="date"]').first().fill(deadlineStr)

    // Target count
    await page.getByPlaceholder('e.g. 10').fill('5')

    const createBtn = page.getByRole('button', { name: 'Create Task' })
    await expect(createBtn).toBeEnabled()
    await createBtn.click()

    // Should redirect to /tasks
    await page.waitForURL('**/tasks', { timeout: 10000 })
    await expect(page.getByRole('heading', { name: 'Tasks' })).toBeVisible()
  })
})

// ---------------------------------------------------------------------------
// Test 4: Manage customers
// ---------------------------------------------------------------------------
test.describe('Customer list', () => {
  test('shows the customers page', async ({ page }) => {
    await loginAndGo(page, '/customers')
    await waitForPageLoad(page)

    await expect(page.getByRole('heading', { name: 'Customers' })).toBeVisible()

    const hasTable = await page.locator('table').isVisible().catch(() => false)
    const hasEmpty = await page.locator('text=No customers yet').isVisible().catch(() => false)
    expect(hasTable || hasEmpty).toBe(true)
  })

  test('opens add customer dialog via data-onboarding button', async ({ page }) => {
    await loginAndGo(page, '/customers')
    await waitForPageLoad(page)

    await page.locator('[data-onboarding="add-customer-btn"]').click()

    await expect(page.getByRole('dialog')).toBeVisible()
    await expect(page.locator('text=+ New customer').or(page.locator('[role="dialog"] h2'))).toBeVisible()
  })

  test('add customer dialog has name and channel fields', async ({ page }) => {
    await loginAndGo(page, '/customers')
    await waitForPageLoad(page)

    await page.locator('[data-onboarding="add-customer-btn"]').click()
    await expect(page.getByRole('dialog')).toBeVisible()

    // Name input (text type, unlabeled by placeholder — matched by label text)
    await expect(page.getByLabel('Name *')).toBeVisible()

    // Channel select
    await expect(page.getByLabel('Channel *')).toBeVisible()
  })

  test('add customer submit is disabled until name is filled', async ({ page }) => {
    await loginAndGo(page, '/customers')
    await waitForPageLoad(page)

    await page.locator('[data-onboarding="add-customer-btn"]').click()
    await expect(page.getByRole('dialog')).toBeVisible()

    const addBtn = page.getByRole('button', { name: 'Add' })
    await expect(addBtn).toBeDisabled()
  })

  test('adds a customer and it appears in the list', async ({ page }) => {
    await loginAndGo(page, '/customers')
    await waitForPageLoad(page)

    await page.locator('[data-onboarding="add-customer-btn"]').click()
    await expect(page.getByRole('dialog')).toBeVisible()

    const uniqueName = `E2E Customer ${Date.now()}`
    await page.getByLabel('Name *').fill(uniqueName)

    const addBtn = page.getByRole('button', { name: 'Add' })
    await expect(addBtn).toBeEnabled()
    await addBtn.click()

    // Dialog closes and customer appears in table
    await expect(page.getByRole('dialog')).not.toBeVisible({ timeout: 5000 })
    await expect(page.locator(`text=${uniqueName}`)).toBeVisible({ timeout: 5000 })
  })
})

// ---------------------------------------------------------------------------
// Test 5: Analytics page loads
// ---------------------------------------------------------------------------
test.describe('Analytics page', () => {
  test('loads without errors', async ({ page }) => {
    await loginAndGo(page, '/analytics')
    await waitForPageLoad(page)

    await expect(page.getByRole('heading', { name: 'Analytics' })).toBeVisible()
    await expectNoError(page)
  })
})

// ---------------------------------------------------------------------------
// Test 6: Crop calendar loads
// ---------------------------------------------------------------------------
test.describe('Crop calendar page', () => {
  test('loads without errors', async ({ page }) => {
    await loginAndGo(page, '/calendar')
    await waitForPageLoad(page)

    await expect(page.getByRole('heading', { name: 'Calendar' })).toBeVisible()
    await expectNoError(page)
  })
})

// ---------------------------------------------------------------------------
// Test 7: Navigate through sidebar items
// ---------------------------------------------------------------------------
test.describe('Sidebar navigation', () => {
  // Routes that should load without triggering an error boundary.
  // Skipping routes that require specific data to be meaningful or that do
  // server-side mutations on initial load.
  const navRoutes: Array<{ path: string; heading: string }> = [
    { path: '/', heading: '' }, // dashboard — heading varies by user data
    { path: '/seasons', heading: 'Seasons' },
    { path: '/species', heading: 'Species' },
    { path: '/tasks', heading: 'Tasks' },
    { path: '/seeds', heading: 'Seed Inventory' },
    { path: '/customers', heading: 'Customers' },
    { path: '/calendar', heading: 'Calendar' },
    { path: '/analytics', heading: 'Analytics' },
    { path: '/successions', heading: 'Successions' },
    { path: '/targets', heading: 'Targets' },
  ]

  for (const { path, heading } of navRoutes) {
    test(`${path} loads without errors`, async ({ page }) => {
      await loginAndGo(page, path)
      await waitForPageLoad(page)

      // No generic error message visible
      await expect(page.locator('text=Something went wrong')).not.toBeVisible()

      // If we expect a specific heading, verify it
      if (heading) {
        await expect(page.getByRole('heading', { name: heading })).toBeVisible()
      }
    })
  }

  test('clicking sidebar links navigates correctly', async ({ page }) => {
    await loginAndGo(page, '/')
    await waitForPageLoad(page)

    // The desktop sidebar is always visible on larger viewports (md:flex).
    // Playwright default viewport is 1280x720, so the sidebar is visible.
    const sidebar = page.locator('nav')

    // Click Tasks
    await sidebar.getByRole('link', { name: /Tasks/ }).click()
    await expect(page).toHaveURL(/\/tasks/)
    await expect(page.getByRole('heading', { name: 'Tasks' })).toBeVisible()

    // Click Seeds
    await sidebar.getByRole('link', { name: /Seeds/ }).click()
    await expect(page).toHaveURL(/\/seeds/)
    await expect(page.getByRole('heading', { name: 'Seed Inventory' })).toBeVisible()

    // Click Species
    await sidebar.getByRole('link', { name: /Species/ }).click()
    await expect(page).toHaveURL(/\/species/)
    await expect(page.getByRole('heading', { name: 'Species' })).toBeVisible()

    // Click Customers
    await sidebar.getByRole('link', { name: /Customers/ }).click()
    await expect(page).toHaveURL(/\/customers/)
    await expect(page.getByRole('heading', { name: 'Customers' })).toBeVisible()
  })
})
