import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MaintenanceRuleDialog } from './MaintenanceRuleDialog'
import type { MaintenanceRuleResponse } from '../../api/client'

vi.mock('../../api/client', async (orig) => {
  const actual = await orig<typeof import('../../api/client')>()
  return {
    ...actual,
    api: {
      ...actual.api,
      maintenanceRules: { ...actual.api.maintenanceRules, update: vi.fn(), create: vi.fn() },
    },
  }
})

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

import { api } from '../../api/client'

function rule(over: Partial<MaintenanceRuleResponse> = {}): MaintenanceRuleResponse {
  return {
    id: 1, bedId: null, bedName: null, gardenAreaId: 5, gardenAreaName: 'Gången',
    activityType: 'WEED', intervalDays: 21, anchorDate: '2026-05-01',
    seasonStartMonth: null, seasonStartDay: null,
    seasonEndMonth: null, seasonEndDay: null,
    active: true, notes: null,
    lastDoneDate: null, nextDueDate: '2099-01-01',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

function renderDialog(r: MaintenanceRuleResponse | null = rule()) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MaintenanceRuleDialog
        target={{ kind: 'AREA', id: 5 }}
        rule={r}
        onClose={() => {}}
        onSaved={() => {}}
        onError={() => {}}
      />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(api.maintenanceRules.update).mockResolvedValue(rule())
})

describe('MaintenanceRuleDialog', () => {
  it('empties a cleared anchor date with the flag, not by omitting it', async () => {
    const { container } = renderDialog()

    fireEvent.change(container.querySelector('input[type="date"]')!, { target: { value: '' } })
    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.maintenanceRules.update).toHaveBeenCalled())
    const sent = vi.mocked(api.maintenanceRules.update).mock.calls[0][1]
    expect(sent.clearAnchorDate).toBe(true)
    // The flag alongside a value for the same field is a 400.
    expect(sent.anchorDate).toBeUndefined()
  })

  it('sends a changed anchor date as a value, never with the flag', async () => {
    const { container } = renderDialog()

    fireEvent.change(container.querySelector('input[type="date"]')!, { target: { value: '2026-06-01' } })
    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.maintenanceRules.update).toHaveBeenCalled())
    const sent = vi.mocked(api.maintenanceRules.update).mock.calls[0][1]
    expect(sent.anchorDate).toBe('2026-06-01')
    expect(sent.clearAnchorDate).toBe(false)
  })

  it('does not ask to clear an anchor date the rule never had', async () => {
    renderDialog(rule({ anchorDate: null }))

    fireEvent.click(screen.getByText('common.save'))

    await waitFor(() => expect(api.maintenanceRules.update).toHaveBeenCalled())
    expect(vi.mocked(api.maintenanceRules.update).mock.calls[0][1].clearAnchorDate).toBe(false)
  })
})
