import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AreaEditDialog } from './AreaEditDialog'
import type { GardenAreaResponse } from '../../api/client'

vi.mock('../../api/client', async (orig) => {
  const actual = await orig<typeof import('../../api/client')>()
  return {
    ...actual,
    api: { ...actual.api, areas: { ...actual.api.areas, update: vi.fn() } },
  }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

import { api } from '../../api/client'

function area(over: Partial<GardenAreaResponse> = {}): GardenAreaResponse {
  return {
    id: 5, gardenId: 1, gardenName: 'Trädgården', name: 'Grusgången',
    description: 'Vid växthuset', category: 'WALKWAY', boundaryJson: null, sizeSqm: 12.5,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

function renderDialog(a: GardenAreaResponse = area()) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  const utils = render(
    <QueryClientProvider client={qc}>
      <AreaEditDialog open area={a} onClose={() => {}} />
    </QueryClientProvider>,
  )
  const description = utils.container.querySelector('textarea')!
  const size = utils.container.querySelector('input[type="number"]')!
  return { ...utils, description, size }
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.areas.update).mockResolvedValue(area())
})

describe('AreaEditDialog', () => {
  it('empties the description and size with the clear flags, not with undefined', async () => {
    const { description, size } = renderDialog()

    fireEvent.change(description, { target: { value: '' } })
    fireEvent.change(size, { target: { value: '' } })
    fireEvent.click(screen.getByText('common.save'))

    // An omitted field reads as "keep the current value" server-side, so the
    // only way to empty one is the flag — which may not carry a value.
    await waitFor(() => expect(api.areas.update).toHaveBeenCalledWith(5, expect.objectContaining({
      clearDescription: true,
      clearSizeSqm: true,
      description: undefined,
      sizeSqm: undefined,
    })))
  })

  it('sends a rewritten description as a value, never with the clear flag', async () => {
    const { description, size } = renderDialog()

    fireEvent.change(description, { target: { value: 'Ny text' } })
    fireEvent.change(size, { target: { value: '4' } })
    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.areas.update).toHaveBeenCalledWith(5, expect.objectContaining({
      description: 'Ny text',
      sizeSqm: 4,
      clearDescription: false,
      clearSizeSqm: false,
    })))
  })

  it('does not ask to clear fields an area never had', async () => {
    renderDialog(area({ description: null, sizeSqm: null }))

    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.areas.update).toHaveBeenCalledWith(5, expect.objectContaining({
      clearDescription: false,
      clearSizeSqm: false,
    })))
  })
})
