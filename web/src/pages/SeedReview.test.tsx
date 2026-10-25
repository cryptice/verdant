import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { api, type SeasonResponse, type SeedReviewResponse } from '../api/client'
import i18n from '../i18n'
import { SeedReview, defaultSeasonId } from './SeedReview'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))

const t = i18n.getFixedT('sv')
const season = (id: number, year: number, endDate?: string) =>
  ({ id, name: `Säsong ${year}`, year, endDate, isActive: false, createdAt: '', updatedAt: '' }) as SeasonResponse

const review = (estimated = false): SeedReviewResponse => ({
  season: { id: 12, name: 'Säsong 2025', year: 2025 },
  estimated,
  varieties: [
    {
      speciesId: 5, speciesName: 'Rosenskära – Purity', unitType: 'SEED',
      steps: [{ name: 'Sådd', eventType: 'SEEDED', count: 10 }, { name: 'Utplantering', eventType: 'PLANTED_OUT', count: 14 }],
      sown: 10, bought: 14, harvestedStems: 34, soldStems: 11,
      revenueCents: 10500, seedCostCents: 300, leftInStock: 4, suggestedOrder: 6, savedOrder: 12,
    },
    {
      speciesId: 7, speciesName: 'Zinnia – Lilliput', unitType: 'SEED',
      steps: [{ name: 'Sådd', eventType: 'SEEDED', count: 20 }],
      sown: 20, bought: 20, harvestedStems: 0, soldStems: 0,
      revenueCents: 0, seedCostCents: 5000, leftInStock: 0, suggestedOrder: 25, savedOrder: null,
    },
  ],
})

beforeEach(() => {
  vi.spyOn(api.seasons, 'list').mockResolvedValue([season(12, 2025, '2025-12-31'), season(13, 2027, '2027-12-31')])
  vi.spyOn(api.seedReview, 'get').mockResolvedValue(review())
  vi.spyOn(api.seedReview, 'saveOrderList').mockResolvedValue(review())
})
afterEach(() => vi.restoreAllMocks())

function show() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={qc}><MemoryRouter><SeedReview /></MemoryRouter></QueryClientProvider>)
}

describe('defaultSeasonId', () => {
  const s1 = { ...season(1, 2026, '2026-10-31'), startDate: '2026-03-01' }
  const s2 = { ...season(2, 2027, '2027-10-31'), startDate: '2027-03-01' }

  it('prefers the latest ended season', () => {
    expect(defaultSeasonId([s1, s2], '2027-06-01')).toBe(1)
  })

  it('falls back to the season containing today when none has ended', () => {
    expect(defaultSeasonId([s1, s2], '2026-06-01')).toBe(1)
    expect(defaultSeasonId([s2, s1], '2026-06-01')).toBe(1)
  })

  it('falls back to the latest season when none contains today', () => {
    expect(defaultSeasonId([s1, s2], '2025-06-01')).toBe(2)
  })
})

describe('SeedReview', () => {
  it('defaults to the latest season that has ended', async () => {
    show()
    await screen.findByText('Rosenskära – Purity')
    expect(api.seedReview.get).toHaveBeenCalledWith(12)
    expect(api.seedReview.get).not.toHaveBeenCalledWith(13)
  })

  it('shows a card per variety with its steps and figures', async () => {
    show()
    const card = (await screen.findByText('Rosenskära – Purity')).closest('[data-testid="variety-card"]') as HTMLElement
    expect(within(card).getByText('Sådd')).toBeInTheDocument()
    expect(within(card).getByText('Utplantering')).toBeInTheDocument()
    expect(within(card).getAllByText('14')).toHaveLength(2)
    expect(within(card).getByTestId('harvested')).toHaveTextContent('34')
    expect(within(card).getByTestId('sold')).toHaveTextContent('11')
    expect(within(card).getByTestId('cost')).toHaveTextContent(/3,00\s*kr/)
    expect(within(card).getByTestId('revenue')).toHaveTextContent(/105,00\s*kr/)
    expect(within(card).getByTestId('stock')).toHaveTextContent('4')
    expect(within(card).getByTestId('sown')).toHaveTextContent('10')
    expect(within(card).getByTestId('bought')).toHaveTextContent('14')
    expect(within(card).getByTestId('stock')).toHaveTextContent(t('unitTypes.SEED').toLowerCase())
    expect(within(card).getByText(new RegExp(`${t('seedReview.order')}.*${t('unitTypes.SEED').toLowerCase()}`, 'i'), { selector: 'label' })).toBeInTheDocument()
    expect(screen.getAllByTestId('variety-card')).toHaveLength(2)
  })

  it('prefills the order with the saved amount, else the suggestion', async () => {
    show()
    expect(await screen.findByLabelText(/Rosenskära – Purity/, { selector: 'input' })).toHaveValue(12)
    expect(screen.getByLabelText(/Zinnia – Lilliput/, { selector: 'input' })).toHaveValue(25)
  })

  it('marks an estimated season once for the whole page', async () => {
    vi.spyOn(api.seedReview, 'get').mockResolvedValue(review(true))
    show()
    await screen.findByText('Rosenskära – Purity')
    expect(screen.getAllByText(t('seedReview.estimated'))).toHaveLength(1)
  })

  it('does not show the estimate note for an exact season', async () => {
    show()
    await screen.findByText('Rosenskära – Purity')
    expect(screen.queryByText(t('seedReview.estimated'))).not.toBeInTheDocument()
  })

  it('saves the edited order list with every variety', async () => {
    show()
    const input = await screen.findByLabelText(/Zinnia – Lilliput/, { selector: 'input' })
    fireEvent.change(input, { target: { value: '30' } })
    fireEvent.click(screen.getByRole('button', { name: t('seedReview.save') }))
    await waitFor(() => expect(api.seedReview.saveOrderList).toHaveBeenCalledWith(12, [
      { speciesId: 5, quantity: 12 },
      { speciesId: 7, quantity: 30 },
    ]))
  })

  it('draws bars against the largest number on the card', async () => {
    show()
    await screen.findByText('Rosenskära – Purity')
    const widths = screen.getAllByTestId('step-bar').map(b => parseFloat((b as HTMLElement).style.width))
    expect(widths[0]).toBeCloseTo(71.43, 1)
    expect(widths[1]).toBe(100)
    widths.forEach(w => { expect(w).toBeGreaterThanOrEqual(0); expect(w).toBeLessThanOrEqual(100) })
  })
})
