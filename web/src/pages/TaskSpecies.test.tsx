import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { api, type ScheduledTaskResponse, type SpeciesResponse } from '../api/client'
import { SowActivity } from './SowActivity'
import { TaskForm } from './TaskForm'

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (key: string) => key, i18n: { language: 'sv' } }),
  Trans: () => null,
}))
vi.mock('../onboarding/OnboardingContext', () => ({
  useOnboarding: () => ({ completeStep: vi.fn() }),
}))
vi.mock('../onboarding/OnboardingHint', () => ({ OnboardingHint: () => null }))

const species = {
  id: 1991, commonName: 'Chili Pepper', commonNameSv: 'Chilipeppar',
  variantName: 'Naga Black', variantNameSv: 'Naga Black',
} as SpeciesResponse
const task = {
  id: 1, speciesId: species.id, speciesName: 'Chilipeppar – Naga Black',
  activityType: 'SOW', deadline: '2026-10-19', targetCount: 15, remainingCount: 15,
  notes: 'Så chilifrön.', acceptableSpecies: [{
    speciesId: species.id, speciesName: 'Chilipeppar – Naga Black',
    commonName: species.commonName, commonNameSv: species.commonNameSv,
    variantName: species.variantName, variantNameSv: species.variantNameSv,
  }],
} as ScheduledTaskResponse

beforeEach(() => {
  vi.spyOn(api.tasks, 'get').mockResolvedValue(task)
  vi.spyOn(api.species, 'get').mockResolvedValue(species)
  // The linked variety is outside the first catalogue page.
  vi.spyOn(api.species, 'list').mockResolvedValue([])
  vi.spyOn(api.species, 'search').mockResolvedValue([])
  vi.spyOn(api.beds, 'list').mockResolvedValue([])
  vi.spyOn(api.trayLocations, 'list').mockResolvedValue([])
  vi.spyOn(api.inventory, 'list').mockResolvedValue([])
})
afterEach(() => vi.restoreAllMocks())

function renderPage(url: string) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={qc}><MemoryRouter initialEntries={[url]}>
    <Routes>
      <Route path="/sow" element={<SowActivity />} />
      <Route path="/task/:taskId/edit" element={<TaskForm />} />
    </Routes>
  </MemoryRouter></QueryClientProvider>)
}

describe('Task species selection', () => {
  it.each(['/sow?taskId=1&speciesId=1991', '/sow?taskId=1'])(
    'prefills the species field from its ID outside the first catalogue page: %s', async url => {
      renderPage(url)
      expect(await screen.findByDisplayValue('Chilipeppar — Naga Black')).toBeInTheDocument()
      expect(api.species.get).toHaveBeenCalledWith(1991)
      expect(api.species.search).not.toHaveBeenCalled()
    },
  )

  it('loads the linked species when editing so the task can be saved', async () => {
    renderPage('/task/1/edit')
    expect(await screen.findByText('Chilipeppar – Naga Black')).toBeInTheDocument()
    expect(await screen.findByRole('button', { name: 'tasks.updateTask' })).toBeEnabled()
    expect(api.species.get).toHaveBeenCalledWith(1991)
    expect(api.species.search).not.toHaveBeenCalled()
  })
})
