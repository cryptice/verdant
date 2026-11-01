import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { api, type SeedInventoryResponse, type SpeciesResponse, type WorkflowTemplateResponse } from '../api/client'
import i18n from '../i18n'
import { WorkflowTemplates } from './WorkflowTemplates'
import { SeedInventory } from './SeedInventory'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n: { ...i18n, language: 'sv' } }),
}))

afterEach(() => vi.restoreAllMocks())

const variety = (id: number, commonNameSv: string, variantNameSv: string, workflowTemplateId: number | null = null, scientificName?: string) =>
  ({ id, commonName: commonNameSv, commonNameSv, variantNameSv, scientificName, workflowTemplateId, groups: [] }) as unknown as SpeciesResponse
const template = (id: number, name: string, steps: string[]) =>
  ({ id, name, createdAt: '', steps: steps.map((n, i) => ({ id: id * 10 + i, name: n, sortOrder: i })) }) as unknown as WorkflowTemplateResponse

function show(page: React.ReactNode) {
  render(<MemoryRouter><QueryClientProvider client={new QueryClient()}>{page}</QueryClientProvider></MemoryRouter>)
}

describe('workflow search', () => {
  it('finds a workflow by its name, its steps or a variety that follows it', async () => {
    vi.spyOn(api.workflows, 'templates').mockResolvedValue([
      template(6, 'Dahlia från frö – säljklar planta', ['Sådd', 'Toppning']),
      template(9, 'Zinnia – snittblommor', ['Sådd i pluggar', 'Utplantering']),
    ])
    vi.spyOn(api.species, 'list').mockResolvedValue([variety(1, 'Pompondahlia', 'Lilliput', 6)])
    show(<WorkflowTemplates />)
    expect(await screen.findByText('Zinnia – snittblommor')).toBeInTheDocument()

    const search = screen.getByRole('searchbox')
    fireEvent.change(search, { target: { value: 'lilliput' } })
    expect(screen.getByText('Dahlia från frö – säljklar planta')).toBeInTheDocument()
    expect(screen.queryByText('Zinnia – snittblommor')).toBeNull()

    fireEvent.change(search, { target: { value: 'utplantering zinnia' } })
    expect(screen.getByText('Zinnia – snittblommor')).toBeInTheDocument()
    expect(screen.queryByText('Dahlia från frö – säljklar planta')).toBeNull()
  })
})

describe('seed stock search', () => {
  it('finds a variety by its names or a batch supplier, and says when nothing matches', async () => {
    const batch = (id: number, speciesId: number, speciesName: string, providerName?: string) =>
      ({ id, speciesId, speciesName, quantity: 100, providerName }) as SeedInventoryResponse
    vi.spyOn(api.inventory, 'list').mockResolvedValue([
      batch(1, 10, 'Rävsvans – Coral Fountain', 'Impecta'),
      batch(2, 11, 'Zinnia – Queen Lime', 'Runåbergs'),
    ])
    vi.spyOn(api.species, 'list').mockResolvedValue([
      variety(10, 'Rävsvans', 'Coral Fountain', null, 'Amaranthus caudatus'),
      variety(11, 'Zinnia', 'Queen Lime', null, 'Zinnia elegans'),
    ])
    show(<SeedInventory />)
    expect(await screen.findByText('Zinnia – Queen Lime')).toBeInTheDocument()

    const search = screen.getByRole('searchbox')
    fireEvent.change(search, { target: { value: 'amaranthus' } })
    expect(screen.getByText('Rävsvans – Coral Fountain')).toBeInTheDocument()
    expect(screen.queryByText('Zinnia – Queen Lime')).toBeNull()

    fireEvent.change(search, { target: { value: 'runåbergs' } })
    expect(screen.getByText('Zinnia – Queen Lime')).toBeInTheDocument()

    fireEvent.change(search, { target: { value: 'tulpan' } })
    expect(screen.getByText('Inget utsäde matchar sökningen eller filtret.')).toBeInTheDocument()
  })

  it('shows batches expiring within the chosen months, expired ones only on their own, and all without a filter', async () => {
    const inMonths = (m: number) => { const d = new Date(); d.setMonth(d.getMonth() + m); return d.toISOString().slice(0, 10) }
    const batch = (id: number, speciesId: number, speciesName: string, expirationDate?: string) =>
      ({ id, speciesId, speciesName, quantity: 100, expirationDate }) as SeedInventoryResponse
    vi.spyOn(api.inventory, 'list').mockResolvedValue([
      batch(1, 10, 'Rävsvans – Coral Fountain', inMonths(2)),
      batch(2, 11, 'Zinnia – Queen Lime', inMonths(18)),
      batch(3, 12, 'Solros – Vanilla Ice', inMonths(-1)),
      batch(4, 13, 'Dahlia – Lilliput'),
    ])
    vi.spyOn(api.species, 'list').mockResolvedValue([])
    show(<SeedInventory />)
    await screen.findByText('Zinnia – Queen Lime')

    fireEvent.change(screen.getByLabelText('Utgångsdatum'), { target: { value: '3' } })
    expect(screen.getByText('Rävsvans – Coral Fountain')).toBeInTheDocument()
    expect(screen.queryByText('Solros – Vanilla Ice')).toBeNull()
    expect(screen.queryByText('Zinnia – Queen Lime')).toBeNull()
    expect(screen.queryByText('Dahlia – Lilliput')).toBeNull()

    fireEvent.change(screen.getByLabelText('Utgångsdatum'), { target: { value: 'expired' } })
    expect(screen.getByText('Solros – Vanilla Ice')).toBeInTheDocument()
    expect(screen.queryByText('Rävsvans – Coral Fountain')).toBeNull()

    fireEvent.change(screen.getByLabelText('Utgångsdatum'), { target: { value: 'none' } })
    expect(screen.getByText('Dahlia – Lilliput')).toBeInTheDocument()
    expect(screen.queryByText('Solros – Vanilla Ice')).toBeNull()
    expect(screen.queryByText('Zinnia – Queen Lime')).toBeNull()

    fireEvent.change(screen.getByLabelText('Utgångsdatum'), { target: { value: '' } })
    expect(screen.getByText('Dahlia – Lilliput')).toBeInTheDocument()
    expect(screen.getByText('Solros – Vanilla Ice')).toBeInTheDocument()
  })
})
