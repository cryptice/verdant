import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { api, type SeedBatchPlanResponse, type SeedInventoryResponse } from '../api/client'
import i18n from '../i18n'
import { SeedPlanDialog } from './SeedPlanDialog'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n: { ...i18n, language: 'sv' } }),
}))

const batch = { id: 9, speciesId: 3, speciesName: 'Dahlia – Lilliput', quantity: 250 } as SeedInventoryResponse
const plan: SeedBatchPlanResponse = {
  seedBatchId: 9, speciesId: 3, seeds: 200, plants: 150, readyDate: '2099-05-01', taskIds: [],
  steps: [
    { name: 'Sådd', activityType: 'SOW', date: '2099-01-01', count: 200, optional: false },
    { name: 'Avhärdning', activityType: 'INSPECT', date: '2099-04-13', count: 150, optional: true },
    { name: 'Säljklar', activityType: 'INSPECT', date: '2099-05-01', count: 150, optional: false },
  ],
}

beforeEach(() => {
  vi.spyOn(api.inventory, 'previewPlan').mockResolvedValue(plan)
  vi.spyOn(api.inventory, 'createPlan').mockResolvedValue({ ...plan, taskIds: [11, 12] })
})
afterEach(() => vi.restoreAllMocks())

function show() {
  render(
    <MemoryRouter><QueryClientProvider client={new QueryClient()}>
      <SeedPlanDialog batch={batch} germinationRate={80} onClose={() => {}} />
    </QueryClientProvider></MemoryRouter>,
  )
}

describe('SeedPlanDialog', () => {
  it('previews the schedule and creates a task per required step', async () => {
    show()
    expect(screen.getByText(/ger fröna cirka 200 plantor/)).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Frön att så'), { target: { value: '200' } })
    fireEvent.change(screen.getByLabelText('Plantor'), { target: { value: '150' } })
    fireEvent.change(screen.getByLabelText('Klara'), { target: { value: '2099-05-01' } })

    expect(await screen.findByText('Säljklar')).toBeInTheDocument()
    expect(api.inventory.previewPlan).toHaveBeenCalledWith(9, { seeds: 200, plants: 150, readyDate: '2099-05-01' })
    expect(screen.getByText(/valfritt, ingen uppgift/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Skapa 2 uppgifter' }))
    await waitFor(() => expect(api.inventory.createPlan).toHaveBeenCalledWith(9, { seeds: 200, plants: 150, readyDate: '2099-05-01' }))
    expect(await screen.findByText(/2 uppgifter skapade/)).toBeInTheDocument()
  })

  it('stops more seeds than the batch holds before asking the server', () => {
    show()
    fireEvent.change(screen.getByLabelText('Frön att så'), { target: { value: '300' } })
    expect(screen.getByText('Partiet har bara 250 frön.')).toBeInTheDocument()
    expect(api.inventory.previewPlan).not.toHaveBeenCalled()
  })
})
