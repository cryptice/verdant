import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MaintenanceRules } from './MaintenanceRules'
import type { MaintenanceRuleResponse } from '../../api/client'

vi.mock('../../api/client', async (orig) => {
  const actual = await orig<typeof import('../../api/client')>()
  return {
    ...actual,
    api: {
      ...actual.api,
      maintenanceRules: { list: vi.fn(), create: vi.fn(), update: vi.fn(), delete: vi.fn() },
    },
  }
})

// i18n `t` is identity-ish in tests; assert on Swedish helper output instead.
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

import { api } from '../../api/client'

function rule(over: Partial<MaintenanceRuleResponse> = {}): MaintenanceRuleResponse {
  return {
    id: 1, bedId: null, bedName: null, gardenAreaId: 5, gardenAreaName: 'Gången',
    activityType: 'WEED', intervalDays: 21, anchorDate: null,
    seasonStartMonth: null, seasonStartDay: null,
    seasonEndMonth: null, seasonEndDay: null,
    active: true, notes: null,
    lastDoneDate: null, nextDueDate: '2099-01-01',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

function renderRules() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <MaintenanceRules target={{ kind: 'AREA', id: 5 }} />
    </QueryClientProvider>,
  )
}

beforeEach(() => vi.clearAllMocks())

describe('MaintenanceRules', () => {
  it('renders an activity and its interval in Swedish', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([rule()])
    renderRules()
    expect(await screen.findByText('Rensa ogräs')).toBeInTheDocument()
    expect(screen.getByText(/Var 3:e vecka/)).toBeInTheDocument()
  })

  it('shows the season window when the rule has one', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([
      rule({ seasonStartMonth: 4, seasonStartDay: 1, seasonEndMonth: 10, seasonEndDay: 15 }),
    ])
    renderRules()
    expect(await screen.findByText(/1 apr – 15 okt/)).toBeInTheDocument()
  })

  it('marks an overdue rule', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([
      rule({ nextDueDate: '2020-01-01' }),
    ])
    renderRules()
    expect(await screen.findByTestId('rule-due-1')).toHaveAttribute('data-due-kind', 'overdue')
  })

  it('marks a paused rule inactive rather than overdue', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([
      rule({ active: false, nextDueDate: '2020-01-01' }),
    ])
    renderRules()
    expect(await screen.findByTestId('rule-due-1')).toHaveAttribute('data-due-kind', 'inactive')
  })

  it('queries scoped to the area, never with both filters', async () => {
    vi.mocked(api.maintenanceRules.list).mockResolvedValue([])
    renderRules()
    await screen.findByTestId('maintenance-rules')
    expect(api.maintenanceRules.list).toHaveBeenCalledWith({ areaId: 5 })
  })
})
