import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { api, type SpeciesResponse } from '../../api/client'
import i18n from '../../i18n'
import { SpeciesForm } from './SpeciesForm'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n: { ...i18n, language: 'sv' } }),
}))

const species = (extra: Partial<SpeciesResponse> = {}): SpeciesResponse => ({
  id: 7, commonName: 'Dahlia', commonNameSv: 'Dahlia', variantName: 'Lilliput', variantNameSv: 'Lilliput',
  bloomMonths: [7, 8], sowingMonths: [2], growingPositions: ['SUNNY'], soils: [],
  photos: [], tags: [], groups: [], custom: true, isSystem: false,
  providers: [{ id: 3, providerId: 1, providerName: 'Impecta', providerIdentifier: 'impecta', productUrl: 'https://impecta.se/dahlia' }],
  ...extra,
})

beforeEach(() => {
  vi.spyOn(api.providers, 'list').mockResolvedValue([{ id: 1, name: 'Impecta', identifier: 'impecta' }, { id: 2, name: 'Runåbergs', identifier: 'runabergs' }])
  vi.spyOn(api.species, 'tags').mockResolvedValue([{ id: 5, name: 'Snitt' }])
  vi.spyOn(api.workflows, 'templates').mockResolvedValue([{ id: 6, name: 'Dahlia från frö', createdAt: '', steps: [] }])
  vi.spyOn(api.species, 'addProvider').mockResolvedValue(species().providers[0])
  vi.spyOn(api.species, 'search').mockResolvedValue([])
})
afterEach(() => vi.restoreAllMocks())

function show(props: Partial<Parameters<typeof SpeciesForm>[0]> = {}) {
  const onSubmit = vi.fn()
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(
    <MemoryRouter><QueryClientProvider client={qc}>
      <SpeciesForm species={null} title="Ny sort" submitLabel="Skapa sort" isSubmitting={false} error={null}
        onCancel={() => {}} onSubmit={onSubmit} {...props} />
    </QueryClientProvider></MemoryRouter>,
  )
  return onSubmit
}

describe('SpeciesForm', () => {
  it('creates a variety with every admin section, a provider without packet images, and a workflow', async () => {
    const onSubmit = show()
    fireEvent.change(screen.getByLabelText('Namn (svenska)'), { target: { value: 'Zinnia' } })
    fireEvent.change(screen.getByLabelText('Groningstid (dagar), Min'), { target: { value: '5' } })
    fireEvent.click(screen.getAllByRole('button', { name: 'mars' })[0])
    fireEvent.click(screen.getByRole('button', { name: 'Sol' }))
    fireEvent.click(await screen.findByRole('button', { name: 'Snitt' }))
    fireEvent.change(screen.getByLabelText('Leverantör'), { target: { value: '2' } })
    fireEvent.change(await screen.findByLabelText('Produkt-URL'), { target: { value: 'https://runabergs.se/zinnia' } })
    await screen.findByRole('option', { name: 'Dahlia från frö' })
    fireEvent.change(screen.getByLabelText('Arbetsflödesmall'), { target: { value: '6' } })
    expect(document.querySelector('input[type=file]')).toBeNull()

    fireEvent.click(screen.getByRole('button', { name: 'Skapa sort' }))

    expect(onSubmit).toHaveBeenCalledWith(
      expect.objectContaining({ commonName: 'Zinnia', commonNameSv: 'Zinnia', germinationTimeDaysMin: 5, sowingMonths: [3], growingPositions: ['SUNNY'], tagIds: [5] }),
      { providerId: 2, productUrl: 'https://runabergs.se/zinnia', workflowTemplateId: 6 },
    )
  })

  it('edits providers by name and link only', async () => {
    const qc = new QueryClient()
    render(
      <MemoryRouter><QueryClientProvider client={qc}>
        <SpeciesForm species={species()} title="Redigera" submitLabel="Spara" isSubmitting={false} error={null} onCancel={() => {}} onSubmit={() => {}} />
      </QueryClientProvider></MemoryRouter>,
    )
    expect(screen.getByText('Impecta')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Lägg till leverantör' }))
    await screen.findByRole('option', { name: 'Runåbergs' })
    expect(screen.queryByRole('option', { name: 'Impecta' })).toBeNull()
    fireEvent.change(screen.getByLabelText('Leverantör'), { target: { value: '2' } })
    expect(screen.queryByText('Framsida')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: 'Lägg till' }))
    await waitFor(() => expect(api.species.addProvider).toHaveBeenCalledWith(7, { providerId: 2, productUrl: undefined }))
  })

  it('shows catalogue varieties read-only', () => {
    show({ species: species({ isSystem: true }) })
    expect(screen.getByText(/Kopiera den för att ändra/)).toBeInTheDocument()
    expect(screen.getByLabelText('Namn (svenska)')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Skapa sort' })).toBeNull()
  })

  it('warns when the new variety already exists in the catalogue and offers to copy it', async () => {
    vi.mocked(api.species.search).mockResolvedValue([
      species({ id: 40, commonName: 'Pompon dahlia', commonNameSv: 'Pompondahlia', variantName: 'Lilliput', variantNameSv: 'Lilliput', isSystem: true }),
    ])
    show()
    fireEvent.change(screen.getByLabelText('Namn (svenska)'), { target: { value: 'Dahlia' } })
    fireEvent.change(screen.getByLabelText('Sortnamn (svenska)'), { target: { value: 'lilliput' } })

    expect(await screen.findByText(/Den här sorten finns redan/, undefined, { timeout: 2000 })).toBeInTheDocument()
    expect(api.species.search).toHaveBeenCalledWith('lilliput', 50)
    expect(screen.getByText('Pompondahlia – Lilliput')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Kopiera' })).toHaveAttribute('href', '/species/new?from=40')
  })
})
