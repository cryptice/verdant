import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { api, type SpeciesResponse } from '../api/client'
import i18n from '../i18n'
import { SeedBatchDialog } from './SeedBatchDialog'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))
vi.mock('../onboarding/OnboardingContext', () => ({ useOnboarding: () => ({ completeStep: vi.fn() }) }))
const lilliput = { id: 2006, commonName: 'Dahlia', commonNameSv: 'Pompondahlia', defaultUnitType: 'SEED',
  providers: [{ id: 31, providerId: 1, providerName: 'Impecta', providerIdentifier: 'impecta' }] } as unknown as SpeciesResponse
vi.mock('./SpeciesAutocomplete', () => ({
  SpeciesAutocomplete: ({ onChange }: { onChange: (s: SpeciesResponse) => void }) => <button type="button" onClick={() => onChange(lilliput)}>Välj</button>,
}))

beforeEach(() => {
  vi.spyOn(api.providers, 'list').mockResolvedValue([
    { id: 1, name: 'Impecta', identifier: 'impecta' }, { id: 2, name: 'Florea', identifier: 'florea' }, { id: 3, name: 'Wexthuset', identifier: 'wexthuset' },
  ])
  vi.spyOn(api.inventory, 'create').mockResolvedValue({ id: 9 } as never)
})

describe('supplier', () => {
  it('is picked when the variety has one', async () => {
    render(<QueryClientProvider client={new QueryClient()}><SeedBatchDialog onClose={vi.fn()} /></QueryClientProvider>)
    await screen.findByRole('option', { name: 'Wexthuset' })
    fireEvent.click(screen.getByRole('button', { name: 'Välj' }))
    const select = screen.getByLabelText('Leverantör') as HTMLSelectElement
    expect(select.options[select.selectedIndex]?.text).toBe('Impecta')
  })

  it('can be any supplier, also one the variety is not linked to', async () => {
    render(<QueryClientProvider client={new QueryClient()}><SeedBatchDialog initialSpecies={lilliput} onClose={vi.fn()} /></QueryClientProvider>)
    await screen.findByRole('option', { name: 'Wexthuset' })
    fireEvent.change(screen.getByLabelText('Leverantör'), { target: { value: '3' } })
    fireEvent.change(screen.getByLabelText('Per förpackning *'), { target: { value: '20' } })
    fireEvent.change(screen.getByLabelText('Förpackningar *'), { target: { value: '1' } })
    fireEvent.click(screen.getByRole('button', { name: 'Lägg till' }))
    await waitFor(() => expect(api.inventory.create).toHaveBeenCalledWith(expect.objectContaining({ speciesId: 2006, providerId: 3 })))
  })

  it('is left for the grower to choose when the variety has several', async () => {
    const twoSuppliers = { ...lilliput, providers: [...lilliput.providers, { id: 32, providerId: 2, providerName: 'Florea', providerIdentifier: 'florea' }] } as unknown as SpeciesResponse
    render(<QueryClientProvider client={new QueryClient()}><SeedBatchDialog initialSpecies={twoSuppliers} onClose={vi.fn()} /></QueryClientProvider>)
    await screen.findByRole('option', { name: 'Wexthuset' })
    const select = screen.getByLabelText('Leverantör') as HTMLSelectElement
    expect(select.value).toBe('')
    expect([...select.options].map(o => o.text)).toEqual(['Ingen', 'Florea', 'Impecta', 'Wexthuset'])
  })
})
