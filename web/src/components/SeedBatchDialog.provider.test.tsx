import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import type { SpeciesResponse } from '../api/client'
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

describe('supplier', () => {
  it('is picked when the variety has one', () => {
    render(<QueryClientProvider client={new QueryClient()}><SeedBatchDialog onClose={vi.fn()} /></QueryClientProvider>)
    fireEvent.click(screen.getByRole('button', { name: 'Välj' }))
    const select = screen.getByLabelText('Leverantör') as HTMLSelectElement
    expect(select.options[select.selectedIndex]?.text).toBe('Impecta')
  })

  it('is left for the grower to choose when the variety has several', () => {
    const twoSuppliers = { ...lilliput, providers: [...lilliput.providers, { id: 32, providerId: 2, providerName: 'Florea', providerIdentifier: 'florea' }] } as unknown as SpeciesResponse
    render(<QueryClientProvider client={new QueryClient()}><SeedBatchDialog initialSpecies={twoSuppliers} onClose={vi.fn()} /></QueryClientProvider>)
    const select = screen.getByLabelText('Leverantör') as HTMLSelectElement
    expect(select.value).toBe('')
    expect([...select.options].map(o => o.text)).toEqual(['Ingen', 'Impecta', 'Florea'])
  })
})
