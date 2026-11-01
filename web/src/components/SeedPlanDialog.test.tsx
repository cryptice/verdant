import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { api, type SeedBatchPlanResponse, type SeedInventoryResponse, type SpeciesResponse } from '../api/client'
import i18n from '../i18n'
import { SeedPlanDialog } from './SeedPlanDialog'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n: { ...i18n, language: 'sv' } }),
}))

const batch = (id: number, quantity: number, providerName?: string) =>
  ({ id, speciesId: 3, speciesName: 'Dahlia – Lilliput', quantity, providerName }) as SeedInventoryResponse
const variety = (extra: Partial<SpeciesResponse> = {}) => ({ id: 3, commonName: 'Dahlia', germinationRate: 80, isSystem: false, ...extra }) as SpeciesResponse
const impecta = batch(9, 250, 'Impecta')
const runabergs = batch(10, 100, 'Runåbergs')
const plan: SeedBatchPlanResponse = {
  seedBatchId: 9, speciesId: 3, seeds: 200, plants: 150, readyDate: '2099-05-01', taskIds: [],
  allocations: [{ seedBatchId: 9, seeds: 200 }],
  steps: [
    { name: 'Sådd', activityType: 'SOW', date: '2099-01-01', count: 200, optional: false },
    { name: 'Avhärdning', activityType: 'INSPECT', date: '2099-04-13', count: 150, optional: true },
    { name: 'Säljklar', activityType: 'INSPECT', date: '2099-05-01', count: 150, optional: false },
  ],
}

const workflow = { steps: [
  { id: 1, name: 'Sådd', daysAfterPrevious: 0, isOptional: false, isSideBranch: false, sortOrder: 0 },
  { id: 2, name: 'Säljklar', daysAfterPrevious: 120, isOptional: false, isSideBranch: false, sortOrder: 1 },
] }

beforeEach(() => {
  vi.spyOn(api.workflows, 'getSpeciesWorkflow').mockResolvedValue(workflow)
  vi.spyOn(api.inventory, 'previewPlan').mockResolvedValue(plan)
  vi.spyOn(api.inventory, 'createPlan').mockResolvedValue({ ...plan, taskIds: [11, 12] })
})
afterEach(() => vi.restoreAllMocks())

function show(props: Partial<Parameters<typeof SeedPlanDialog>[0]> = {}) {
  render(
    <MemoryRouter><QueryClientProvider client={new QueryClient()}>
      <SeedPlanDialog batches={[impecta]} species={variety()} onClose={() => {}} {...props} />
    </QueryClientProvider></MemoryRouter>,
  )
}

function fill(seeds: string | null, plants: string, date: string) {
  if (seeds !== null) fireEvent.change(screen.getByLabelText('Frön att så'), { target: { value: seeds } })
  fireEvent.change(screen.getByLabelText('Plantor'), { target: { value: plants } })
  fireEvent.change(screen.getByLabelText('Klara'), { target: { value: date } })
}

describe('SeedPlanDialog', () => {
  it('uses the only batch, previews the schedule and creates a task per required step', async () => {
    show()
    expect(await screen.findByText(/Impecta · 250 frön/)).toBeInTheDocument()
    expect(screen.getByText(/ger fröna cirka 200 plantor/)).toBeInTheDocument()
    fill('200', '150', '2099-05-01')

    expect(await screen.findByText('Säljklar')).toBeInTheDocument()
    const request = { seeds: 200, plants: 150, readyDate: '2099-05-01', seedBatchIds: [9] }
    expect(api.inventory.previewPlan).toHaveBeenCalledWith(request)
    expect(screen.getByText(/valfritt, ingen uppgift/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Skapa 2 uppgifter' }))
    await waitFor(() => expect(api.inventory.createPlan).toHaveBeenCalledWith(request))
    expect(await screen.findByText(/2 uppgifter skapade/)).toBeInTheDocument()
  })

  it('lets several batches be chosen to sow more than one holds', async () => {
    vi.mocked(api.inventory.previewPlan).mockResolvedValue({
      ...plan, seeds: 300, allocations: [{ seedBatchId: 9, seeds: 250 }, { seedBatchId: 10, seeds: 50 }],
      steps: plan.steps.map(s => s.activityType === 'SOW' ? { ...s, count: 300 } : s),
    })
    show({ batches: [impecta, runabergs] })

    expect(await screen.findByRole('button', { name: /Fröpartier/ })).toHaveTextContent('Välj fröpartier')
    fireEvent.click(screen.getByRole('checkbox', { name: /Impecta/ }))
    fireEvent.click(screen.getByRole('checkbox', { name: /Runåbergs/ }))
    expect(screen.getByRole('button', { name: /Fröpartier/ })).toHaveTextContent('2 partier · 350 frön')
    expect(screen.getByLabelText('Frön att så')).toHaveValue(350)

    fill('300', '220', '2099-05-01')
    expect(await screen.findByText('Impecta: 250 frön + Runåbergs: 50 frön')).toBeInTheDocument()
    expect(api.inventory.previewPlan).toHaveBeenCalledWith({ seeds: 300, plants: 220, readyDate: '2099-05-01', seedBatchIds: [9, 10] })
    expect(screen.getByRole('button', { name: 'Skapa 3 uppgifter' })).toBeEnabled()
  })

  it('starts from the batch it was opened from and stops more seeds than the chosen batches hold', async () => {
    show({ batches: [impecta, runabergs], initialBatchIds: [10] })
    expect(await screen.findByLabelText('Frön att så')).toHaveValue(100)
    fill('300', '150', '2099-05-01')
    expect(screen.getByText('Partiet har bara 100 frön.')).toBeInTheDocument()
    expect(api.inventory.previewPlan).not.toHaveBeenCalled()
  })

  it('says a variety without a workflow cannot be planned, and points to the variety', async () => {
    vi.mocked(api.workflows.getSpeciesWorkflow).mockResolvedValue({ steps: [] })
    show()
    expect(await screen.findByText(/har inget arbetsflöde/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Frön att så')).toBeNull()
    expect(screen.getByRole('button', { name: 'Öppna sorten' })).toBeInTheDocument()
  })

  it('names the workflow step without days', async () => {
    vi.mocked(api.workflows.getSpeciesWorkflow).mockResolvedValue({ steps: [workflow.steps[0], { ...workflow.steps[1], daysAfterPrevious: undefined }] })
    show()
    expect(await screen.findByText(/Steget "Säljklar" i sortens arbetsflöde saknar antal dagar/)).toBeInTheDocument()
  })

  it('tells a catalogue variety to be copied first, without asking for its workflow', async () => {
    show({ species: variety({ isSystem: true }) })
    expect(await screen.findByText(/sort från katalogen/)).toBeInTheDocument()
    expect(api.workflows.getSpeciesWorkflow).not.toHaveBeenCalled()
  })
})
