import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
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
      <Route path="/task/new" element={<TaskForm />} />
      <Route path="/task/:taskId/edit" element={<TaskForm />} />
    </Routes>
  </MemoryRouter></QueryClientProvider>)
}

describe('Task species selection', () => {
  it.each(['/sow?taskId=1&speciesId=1991', '/sow?taskId=1'])(
    'summarizes the linked species without an editable species field: %s', async url => {
      renderPage(url)
      expect(await screen.findByText('Chilipeppar – Naga Black')).toBeInTheDocument()
      expect(screen.queryByPlaceholderText('Sök art...')).not.toBeInTheDocument()
      expect(screen.getByText('Så chilifrön.')).toBeInTheDocument()
      expect(await screen.findByDisplayValue('15')).toBeEnabled()
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

  it('labels the count as seeds only while the task is a sowing', async () => {
    renderPage('/task/1/edit')
    expect(await screen.findByDisplayValue('15')).toBeInTheDocument()
    expect(screen.getByText('tasks.seedCountLabel')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('tasks.form.type'), { target: { value: 'POT_UP' } })
    expect(screen.getByText('tasks.targetCountLabel')).toBeInTheDocument()
    expect(screen.queryByText('tasks.seedCountLabel')).not.toBeInTheDocument()
  })
})

describe('General TODO tasks', () => {
  it('starts as a TODO when opened from the activity button', () => {
    renderPage('/task/new?type=TODO')
    expect(screen.getByLabelText('tasks.form.type')).toHaveValue('TODO')
    expect(screen.getByLabelText('tasks.form.description')).toBeInTheDocument()
  })

  it('ignores an unknown type in the link', () => {
    renderPage('/task/new?type=NOPE')
    expect(screen.getByLabelText('tasks.form.type')).toHaveValue('SOW')
  })

  it('creates a TODO from only a description, without species, count or deadline', async () => {
    const create = vi.spyOn(api.tasks, 'create').mockResolvedValue({ ...task, activityType: 'TODO' })
    renderPage('/task/new')
    fireEvent.change(screen.getByLabelText('tasks.form.type'), { target: { value: 'TODO' } })
    expect(screen.queryByText('common.speciesLabel')).not.toBeInTheDocument()
    expect(screen.queryByText('tasks.targetCountLabel')).not.toBeInTheDocument()
    const save = screen.getByRole('button', { name: 'tasks.createTask' })
    expect(save).toBeDisabled()
    fireEvent.change(screen.getByLabelText('tasks.form.description'), { target: { value: '  Beställ chilifrön ' } })
    fireEvent.click(save)
    await waitFor(() => expect(create).toHaveBeenCalledWith({
      activityType: 'TODO', earliestDate: null, deadline: null, notes: 'Beställ chilifrön',
    }))
  })

  it('edits an existing TODO without offering other activity types', async () => {
    vi.mocked(api.tasks.get).mockResolvedValue({ ...task, speciesId: null, speciesName: null, activityType: 'TODO', notes: 'Beställ chilifrön', acceptableSpecies: [] })
    const update = vi.spyOn(api.tasks, 'update').mockResolvedValue({ ...task, activityType: 'TODO' })
    renderPage('/task/1/edit')
    // The form loads the task and species lazily; allow for a busy parallel run.
    expect(await screen.findByDisplayValue('Beställ chilifrön', undefined, { timeout: 3000 })).toBeInTheDocument()
    const type = screen.getByLabelText('tasks.form.type')
    expect(within(type).getAllByRole('option').map(option => option.getAttribute('value'))).toEqual(['TODO'])
    fireEvent.click(screen.getByRole('button', { name: 'tasks.updateTask' }))
    await waitFor(() => expect(update).toHaveBeenCalledWith(1, {
      activityType: 'TODO', earliestDate: null, deadline: '2026-10-19', notes: 'Beställ chilifrön',
    }))
  })
})
