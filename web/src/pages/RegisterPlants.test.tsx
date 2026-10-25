import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { api, type BedWithGardenResponse, type SpeciesResponse, type TrayLocationResponse } from '../api/client'
import i18n from '../i18n'
import { RegisterPlants, daysAgoIsoLocal } from './RegisterPlants'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))
const chili = { id: 100, commonName: 'Chili', commonNameSv: 'Chilipeppar', variantNameSv: 'Naga Black', providers: [] } as unknown as SpeciesResponse
vi.mock('../components/SpeciesAutocomplete', () => ({
  SpeciesAutocomplete: ({ onChange }: { onChange: (s: SpeciesResponse) => void }) => (
    <button type="button" onClick={() => onChange(chili)}>Välj chili</button>
  ),
}))

const beds = [{ id: 3, name: 'Bädd 3', gardenName: 'Köksträdgården' }] as BedWithGardenResponse[]
const trays = [{ id: 1, name: 'Växthuset' }, { id: 2, name: 'Fönstret' }] as TrayLocationResponse[]

beforeEach(() => {
  vi.spyOn(api.beds, 'list').mockResolvedValue(beds)
  vi.spyOn(api.trayLocations, 'list').mockResolvedValue(trays)
  vi.spyOn(api.plants, 'batchSow').mockResolvedValue({ plantIds: [1, 2, 3], count: 3 })
})
afterEach(() => vi.restoreAllMocks())

function show() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={qc}><MemoryRouter><RegisterPlants /></MemoryRouter></QueryClientProvider>)
}

describe('RegisterPlants', () => {
  it('registers existing plants in a tray, dated by an age preset', async () => {
    show()
    fireEvent.click(screen.getByRole('button', { name: 'Välj chili' }))
    expect(await screen.findByRole('button', { name: 'Bricka' })).toHaveAttribute('aria-pressed', 'true')
    const register = screen.getByRole('button', { name: 'Registrera' })
    expect(register).toBeDisabled()
    fireEvent.change(screen.getByLabelText('Odlingsplats *'), { target: { value: '2' } })
    fireEvent.change(screen.getByLabelText('Antal plantor *'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: '2 v' }))
    fireEvent.click(register)
    await waitFor(() => expect(api.plants.batchSow).toHaveBeenCalledWith({
      speciesId: 100, name: 'Naga Black', seedCount: 3, trayLocationId: 2, bedId: undefined,
      plantedDate: daysAgoIsoLocal(14), notes: undefined, origin: 'REGISTERED',
    }))
    expect(await screen.findByRole('dialog', { name: 'Plantor registrerade' })).toBeInTheDocument()
  })

  it('registers plants straight into a bed', async () => {
    show()
    fireEvent.click(screen.getByRole('button', { name: 'Välj chili' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Bädd' }))
    fireEvent.change(screen.getByLabelText('Bädd *'), { target: { value: '3' } })
    fireEvent.click(screen.getByRole('button', { name: 'Registrera' }))
    await waitFor(() => expect(api.plants.batchSow).toHaveBeenCalledWith(expect.objectContaining({
      bedId: 3, trayLocationId: undefined, seedCount: 1, plantedDate: daysAgoIsoLocal(0), origin: 'REGISTERED',
    })))
  })
})
