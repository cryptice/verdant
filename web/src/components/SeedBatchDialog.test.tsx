import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { api, type SeedInventoryResponse, type SpeciesResponse } from '../api/client'
import i18n from '../i18n'
import { SeedBatchDialog } from './SeedBatchDialog'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))
vi.mock('../onboarding/OnboardingContext', () => ({
  useOnboarding: () => ({ completeStep: vi.fn() }),
}))
vi.mock('./SpeciesAutocomplete', () => ({ SpeciesAutocomplete: () => null }))

const chili = { id: 100, commonName: 'Chili', commonNameSv: 'Chilipeppar', providers: [], defaultUnitType: 'SEED' } as unknown as SpeciesResponse
const batch: SeedInventoryResponse = {
  id: 7, speciesId: 100, speciesName: 'Chilipeppar', quantity: 15,
  costPerUnitCents: 200, unitType: 'SEED', seedsPerPackage: 5, createdAt: '',
}

beforeEach(() => {
  vi.spyOn(api.inventory, 'create').mockResolvedValue(batch)
  vi.spyOn(api.inventory, 'update').mockResolvedValue(batch)
})
afterEach(() => vi.restoreAllMocks())

function show(props: Partial<Parameters<typeof SeedBatchDialog>[0]>) {
  const qc = new QueryClient()
  render(<QueryClientProvider client={qc}><SeedBatchDialog species={[chili]} onClose={vi.fn()} {...props} /></QueryClientProvider>)
}

describe('SeedBatchDialog', () => {
  it('adds a batch from package size and package count', async () => {
    show({ initialSpecies: chili })
    fireEvent.change(screen.getByLabelText('Per förpackning *'), { target: { value: '5' } })
    fireEvent.change(screen.getByLabelText('Förpackningar *'), { target: { value: '3' } })
    fireEvent.change(screen.getByLabelText('Per enhet (SEK)'), { target: { value: '2' } })
    expect(screen.getByLabelText('Per förpackning (SEK)')).toHaveValue(10)
    fireEvent.click(screen.getByRole('button', { name: 'Lägg till' }))
    await waitFor(() => expect(api.inventory.create).toHaveBeenCalledWith(expect.objectContaining({
      speciesId: 100, quantity: 15, seedsPerPackage: 5, costPerUnitCents: 200, unitType: 'SEED',
    })))
  })

  it('edits with the same package layout and prices a package by its size, not the whole batch', async () => {
    show({ batch })
    expect(screen.getByText('Chilipeppar')).toBeInTheDocument()
    expect(screen.getByLabelText('Per förpackning')).toHaveValue(5)
    expect(screen.getByLabelText('Kvar *')).toHaveValue(15)
    expect(screen.getByText('= 3 förpackningar')).toBeInTheDocument()
    expect(screen.getByLabelText('Per förpackning (SEK)')).toHaveValue(10)
    fireEvent.change(screen.getByLabelText('Per förpackning (SEK)'), { target: { value: '15' } })
    expect(screen.getByLabelText('Per enhet (SEK)')).toHaveValue(3)
    fireEvent.click(screen.getByRole('button', { name: 'Spara' }))
    await waitFor(() => expect(api.inventory.update).toHaveBeenCalledWith(7, expect.objectContaining({
      quantity: 15, seedsPerPackage: 5, costPerUnitCents: 300, unitType: 'SEED',
    })))
  })

  it('leaves package pricing off for an older batch without a package size', () => {
    show({ batch: { ...batch, seedsPerPackage: undefined } })
    expect(screen.getByLabelText('Per förpackning')).toHaveValue(null)
    expect(screen.getByLabelText('Per förpackning (SEK)')).toBeDisabled()
    expect(screen.getByLabelText('Per enhet (SEK)')).toHaveValue(2)
  })
})
