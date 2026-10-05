import { test, expect } from '@playwright/test'

test('admin manages shared groups, lifecycle schedules and assignments', async ({ page }) => {
  const login = await page.request.post('http://localhost:8081/api/auth/admin', {
    data: { email: process.env.TEST_ADMIN_EMAIL ?? 'admin@verdant.app', password: process.env.TEST_ADMIN_PASSWORD ?? 'admin' },
  })
  expect(login.ok()).toBeTruthy()
  const { token } = await login.json()
  const headers = { Authorization: `Bearer ${token}` }
  await page.addInitScript(token => {
    localStorage.setItem('admin_token', token)
    localStorage.setItem('verdant-lang', 'en')
  }, token)
  const suffix = crypto.randomUUID()
  const scheduleKey = `browser-${suffix}`
  const name = `Browser zinnia ${suffix}`
  const response = await page.request.post('http://localhost:8081/api/admin/species', {
    headers, data: { commonName: name, scientificName: 'Zinnia elegans', plantType: 'ANNUAL', defaultUnitType: 'SEED' },
  })
  expect(response.ok()).toBeTruthy()
  const sp = await response.json()
  let groupId: number | undefined
  try {
    await page.goto('/admin/schedules/zinnia')
    await expect(page.getByRole('heading', { name: 'Zinnia från frö' })).toBeVisible()
    await page.getByRole('link', { name: 'Duplicate schedule' }).click()
    await page.getByLabel('Name', { exact: true }).fill(`Browser schedule ${suffix}`)
    await page.getByLabel('Unique key (cannot be changed later)').fill(scheduleKey)
    await page.getByLabel('Sellable units per plant on the target date').fill('2')
    await page.getByLabel('Days before harvest').nth(1).fill('90')
    await page.getByRole('button', { name: 'Save', exact: true }).click()
    await page.getByRole('button', { name: 'Confirm', exact: true }).click()
    await expect(page).toHaveURL(new RegExp(`/schedules/${scheduleKey}$`))
    await page.reload()
    await expect(page.getByLabel('Sellable units per plant on the target date')).toHaveValue('2')
    await expect(page.getByLabel('Days before harvest').nth(1)).toHaveValue('90')
    await page.getByLabel('Name', { exact: true }).fill(`Edited browser schedule ${suffix}`)
    await page.getByRole('button', { name: 'Save', exact: true }).click()
    await page.getByRole('button', { name: 'Confirm', exact: true }).click()
    await expect(page.getByRole('heading', { name: `Edited browser schedule ${suffix}` })).toBeVisible()
    await page.setViewportSize({ width: 1280, height: 960 })
    await page.screenshot({ path: 'test-results/admin-schedule-desktop.png', fullPage: true })
    await page.setViewportSize({ width: 390, height: 844 })
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBeTruthy()
    await page.screenshot({ path: 'test-results/admin-schedule-mobile.png', fullPage: true })
    await page.setViewportSize({ width: 1280, height: 960 })

    await page.goto('/admin/groups')
    await page.getByLabel('New group name').fill(`Browser group ${suffix}`)
    await page.getByRole('button', { name: 'Add', exact: true }).click()
    await expect(page).toHaveURL(/\/groups\/\d+$/)
    groupId = Number(page.url().split('/').at(-1))
    await page.getByLabel('Search species or botanical name').fill(name)
    await page.getByRole('checkbox', { name, exact: true }).check()
    await page.getByRole('button', { name: 'Add selected (1)', exact: true }).click()
    await expect(page.getByRole('link', { name, exact: true })).toBeVisible()
    await page.getByLabel('Name', { exact: true }).fill(`Renamed group ${suffix}`)
    await page.getByRole('button', { name: 'Save', exact: true }).click()
    await expect(page.getByRole('heading', { name: `Renamed group ${suffix}` })).toBeVisible()
    await page.getByRole('link', { name: 'Manage schedules for these species' }).click()
    await page.getByLabel('Schedule to apply').selectOption(scheduleKey)
    await page.getByRole('checkbox', { name, exact: true }).check()
    await page.getByRole('button', { name: 'Apply to selected (1)' }).click()
    await page.getByRole('button', { name: 'Confirm', exact: true }).click()
    await expect(page.getByRole('status')).toHaveText('Changes saved.')
    await expect(page.getByText(/Explicit assignment/)).toBeVisible()
    await page.goto(`/admin/schedules/${scheduleKey}`)
    await expect(page.getByRole('button', { name: 'Delete schedule', exact: true })).toBeDisabled()
    await page.goto(`/admin/planning/species?species=${sp.id}`)
    await page.getByRole('checkbox', { name, exact: true }).check()
    await page.getByRole('button', { name: 'Apply to selected (1)' }).click()
    await page.getByRole('button', { name: 'Confirm', exact: true }).click()
    await expect(page.getByRole('status')).toHaveText('Changes saved.')
    await expect(page.getByRole('link', { name: 'Zinnia från frö' })).toBeVisible()

    await page.goto(`/admin/schedules/${scheduleKey}`)
    await page.getByRole('button', { name: 'Delete schedule', exact: true }).click()
    await page.getByRole('button', { name: 'Confirm', exact: true }).click()
    await expect(page).toHaveURL(/\/admin\/schedules$/)
    await page.goto(`/admin/groups/${groupId}`)
    await page.getByRole('button', { name: 'Remove', exact: true }).click()
    await page.getByRole('button', { name: 'Confirm', exact: true }).click()
    await expect(page.getByRole('heading', { name: '0 species', exact: true })).toBeVisible()
    await page.getByRole('button', { name: 'Delete group', exact: true }).click()
    await page.getByRole('button', { name: 'Confirm', exact: true }).click()
    await expect(page).toHaveURL(/\/admin\/groups$/)
    expect((await page.request.get(`http://localhost:8081/api/admin/species/${sp.id}`, { headers })).ok()).toBeTruthy()
  } finally {
    await page.request.put('http://localhost:8081/api/admin/planning/assignments', { headers, data: { speciesIds: [sp.id], scheduleKey: null } })
    const schedule = await page.request.get(`http://localhost:8081/api/admin/planning/schedules/${scheduleKey}`, { headers })
    if (schedule.ok()) {
      const { revision } = await schedule.json()
      await page.request.delete(`http://localhost:8081/api/admin/planning/schedules/${scheduleKey}?revision=${revision}`, { headers })
    }
    if (groupId) await page.request.delete(`http://localhost:8081/api/admin/planning/groups/${groupId}`, { headers })
    await page.request.delete(`http://localhost:8081/api/admin/species/${sp.id}`, { headers })
  }
})
