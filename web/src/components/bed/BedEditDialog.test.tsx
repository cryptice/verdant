import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BedEditDialog } from './BedEditDialog'
import type { BedResponse } from '../../api/client'

vi.mock('../../api/client', async (orig) => {
  const actual = await orig<typeof import('../../api/client')>()
  return {
    ...actual,
    api: { ...actual.api, beds: { ...actual.api.beds, update: vi.fn() } },
  }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

import { api } from '../../api/client'

function bed(over: Partial<BedResponse> = {}): BedResponse {
  return {
    id: 3, name: 'Bädd 1', gardenId: 1, description: 'Vid muren',
    lengthMeters: 4, widthMeters: 1.2,
    soilType: 'SANDY', soilPh: 6.5, sunExposure: 'FULL_SUN', drainage: 'GOOD',
    sunDirections: ['S'], irrigationType: 'DRIP', protection: 'OPEN_FIELD',
    raisedBed: false,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

function renderDialog(b: BedResponse = bed()) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const utils = render(
    <QueryClientProvider client={qc}>
      <BedEditDialog open bed={b} onClose={() => {}} />
    </QueryClientProvider>,
  )
  // The soil/sun/water fields live behind a collapsed section.
  fireEvent.click(screen.getByText('bed.conditions.sectionTitle'))
  return utils
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.beds.update).mockResolvedValue(bed())
})

describe('BedEditDialog', () => {
  it('empties a cleared select with its flag rather than omitting the field', async () => {
    const { container } = renderDialog()

    // Every condition select shares the "unset" empty-string option.
    const selects = Array.from(container.querySelectorAll('select'))
    selects.forEach((s) => fireEvent.change(s, { target: { value: '' } }))
    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.beds.update).toHaveBeenCalled())
    const sent = vi.mocked(api.beds.update).mock.calls[0][1]
    expect(sent.clearSoilType).toBe(true)
    expect(sent.clearSunExposure).toBe(true)
    expect(sent.clearDrainage).toBe(true)
    expect(sent.clearIrrigationType).toBe(true)
    expect(sent.clearProtection).toBe(true)
    // A flag alongside a value for the same field is a 400.
    expect(sent.soilType).toBeUndefined()
  })

  it('empties cleared numbers with their flags', async () => {
    const { container } = renderDialog()

    const numbers = Array.from(container.querySelectorAll('input[type="number"]'))
    numbers.forEach((n) => fireEvent.change(n, { target: { value: '' } }))
    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.beds.update).toHaveBeenCalled())
    const sent = vi.mocked(api.beds.update).mock.calls[0][1]
    expect(sent.clearLengthMeters).toBe(true)
    expect(sent.clearWidthMeters).toBe(true)
    expect(sent.clearSoilPh).toBe(true)
    expect(sent.lengthMeters).toBeUndefined()
  })

  it('sends an emptied description as a blank string, which the server clears on', async () => {
    const { container } = renderDialog()

    fireEvent.change(container.querySelector('textarea')!, { target: { value: '' } })
    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.beds.update).toHaveBeenCalled())
    expect(vi.mocked(api.beds.update).mock.calls[0][1].description).toBe('')
  })

  it('does not ask to clear fields the bed never had', async () => {
    renderDialog(bed({
      description: undefined, lengthMeters: undefined, widthMeters: undefined,
      soilType: undefined, soilPh: undefined, sunExposure: undefined,
      drainage: undefined, sunDirections: [], irrigationType: undefined, protection: undefined,
    }))

    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.beds.update).toHaveBeenCalled())
    const sent = vi.mocked(api.beds.update).mock.calls[0][1]
    expect(sent.clearSoilType).toBe(false)
    expect(sent.clearSoilPh).toBe(false)
    expect(sent.clearSunDirections).toBe(false)
  })

  it('keeps untouched values as values, with no flags', async () => {
    renderDialog()

    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.beds.update).toHaveBeenCalled())
    const sent = vi.mocked(api.beds.update).mock.calls[0][1]
    expect(sent.soilType).toBe('SANDY')
    expect(sent.soilPh).toBe(6.5)
    expect(sent.clearSoilType).toBe(false)
    expect(sent.clearSoilPh).toBe(false)
  })
})
