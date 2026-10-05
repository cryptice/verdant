import { beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { HarvestPlans } from './HarvestPlans'
import { harvestPlans, type HarvestPlanPreview, type PlanningSpecies } from '../api/harvestPlans'
import { api } from '../api/client'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))
vi.mock('../api/harvestPlans', () => ({ harvestPlans: { list: vi.fn(), get: vi.fn(), species: vi.fn(), profile: vi.fn(), preview: vi.fn(), create: vi.fn(), complete: vi.fn(), cancel: vi.fn() } }))
vi.mock('../api/client', async original => {
  const actual = await original<typeof import('../api/client')>()
  return { ...actual, api: { ...actual.api, seasons: { list: vi.fn() } } }
})
vi.mock('../components/SpeciesAutocomplete', () => ({ SpeciesAutocomplete: ({ onGroupSelect }: { onGroupSelect: (id: number, name: string) => void }) => <button onClick={() => onGroupSelect(7, 'Pom pom dahlias')} type="button">Choose dahlias</button> }))

const candidate: PlanningSpecies = { speciesId: 1, speciesName: 'Dahlia', customized: true, profile: { sellableUnit: 'STEM', outputPerPlant: 2, establishmentPercent: 80, startingUnit: 'SEED', steps: [] } }
const preview: HarvestPlanPreview = {
  request: { seasonId: 1, groupId: 7, quantity: 300, sellableUnit: 'STEM', harvestDate: '2027-08-01', allocations: {} },
  allocations: [{ speciesId: 1, speciesName: 'Dahlia', outputQuantity: 300, plantsNeeded: 150, startingQuantity: 188, profile: candidate.profile, steps: [] }],
  issues: [], unallocatedQuantity: 0, canSave: true, fingerprint: 'snapshot-1',
}

function renderPage(url = '/targets') {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(<QueryClientProvider client={qc}><MemoryRouter initialEntries={[url]}><HarvestPlans /></MemoryRouter></QueryClientProvider>)
  return qc
}
async function fillTarget() {
  await screen.findByRole('option', { name: '2027' })
  fireEvent.change(screen.getByLabelText('planning.season'), { target: { value: '1' } })
  fireEvent.click(screen.getByText('Choose dahlias'))
  await screen.findByLabelText('Dahlia')
  fireEvent.change(screen.getByLabelText('planning.harvestDate'), { target: { value: '2027-08-01' } })
  fireEvent.click(screen.getByRole('button', { name: 'planning.preview' }))
  await screen.findByRole('button', { name: 'planning.savePlan' })
}
beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.seasons.list).mockResolvedValue([{ id: 1, name: '2027', year: 2027 } as Awaited<ReturnType<typeof api.seasons.list>>[number]])
  vi.mocked(harvestPlans.list).mockResolvedValue([])
  vi.mocked(harvestPlans.species).mockResolvedValue([candidate])
  vi.mocked(harvestPlans.preview).mockResolvedValue(preview)
})

describe('harvest planning', () => {
  it('preselects the highest year after loading and preserves a manual selection on refresh', async () => {
    const seasons = [
      { id: 99, name: 'Older active', year: 2026, isActive: true, createdAt: '', updatedAt: '' },
      { id: 2, name: 'Latest', year: 2028, isActive: false, createdAt: '', updatedAt: '' },
      { id: 7, name: 'Middle', year: 2027, isActive: false, createdAt: '', updatedAt: '' },
    ]
    vi.mocked(api.seasons.list).mockResolvedValue(seasons)
    const qc = renderPage()
    await screen.findByRole('option', { name: 'Latest' })
    await waitFor(() => expect(screen.getByLabelText('planning.season')).toHaveValue('2'))
    fireEvent.change(screen.getByLabelText('planning.season'), { target: { value: '99' } })
    act(() => qc.setQueryData(['seasons'], [...seasons, { ...seasons[1], id: 3, year: 2029 }]))
    expect(screen.getByLabelText('planning.season')).toHaveValue('99')
  })

  it('invalidates a preview after target or allocation edits', async () => {
    renderPage()
    await fillTarget()
    expect(harvestPlans.preview).toHaveBeenCalledWith(preview.request)
    fireEvent.change(screen.getByLabelText('Dahlia'), { target: { value: '0' } })
    expect(screen.queryByRole('button', { name: 'planning.savePlan' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'planning.preview' }))
    await waitFor(() => expect(harvestPlans.preview).toHaveBeenLastCalledWith({ ...preview.request, allocations: { 1: 0 } }))
  })

  it('keeps the idempotency key when retrying a failed save', async () => {
    vi.mocked(harvestPlans.create).mockRejectedValue(new Error('Connection interrupted'))
    renderPage()
    await fillTarget()
    fireEvent.click(screen.getByRole('button', { name: 'planning.savePlan' }))
    await screen.findByText('Connection interrupted')
    fireEvent.click(screen.getByRole('button', { name: 'planning.savePlan' }))
    await waitFor(() => expect(harvestPlans.create).toHaveBeenCalledTimes(2))
    expect(vi.mocked(harvestPlans.create).mock.calls[0][0]).toBe(vi.mocked(harvestPlans.create).mock.calls[1][0])
  })

  it('blocks saving an unallocated target', async () => {
    vi.mocked(harvestPlans.preview).mockResolvedValue({ ...preview, canSave: false, unallocatedQuantity: 300 })
    renderPage()
    await fillTarget()
    expect(screen.getByRole('button', { name: 'planning.savePlan' })).toBeDisabled()
  })

  it('invalidates task caches after saving and opens the saved plan', async () => {
    const plan = { id: 9, status: 'ACTIVE', snapshot: preview, tasks: [], createdAt: '2026-10-05T00:00:00Z' }
    vi.mocked(harvestPlans.create).mockResolvedValue(plan)
    vi.mocked(harvestPlans.get).mockResolvedValue(plan)
    const qc = renderPage()
    qc.setQueryData(['tasks'], [])
    await fillTarget()
    fireEvent.click(screen.getByRole('button', { name: 'planning.savePlan' }))
    await screen.findByText('planning.progressHelp')
    expect(harvestPlans.get).toHaveBeenCalledWith(9)
    expect(qc.getQueryState(['tasks'])?.isInvalidated).toBe(true)
  })
})
