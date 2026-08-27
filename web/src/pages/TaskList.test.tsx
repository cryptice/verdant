import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { isPlaceScoped, taskTitle, TaskList } from './TaskList'
import type { ScheduledTaskResponse } from '../api/client'

vi.mock('../api/client', async (orig) => {
  const actual = await orig<typeof import('../api/client')>()
  return {
    ...actual,
    api: {
      ...actual.api,
      tasks: { ...actual.api.tasks, list: vi.fn(), complete: vi.fn(), delete: vi.fn() },
    },
  }
})

// i18n `t` is identity-ish in tests; assert on keys and Swedish helper output.
vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (k: string) => k }),
}))

const navigate = vi.fn()
vi.mock('react-router-dom', () => ({
  useNavigate: () => navigate,
}))

import { api } from '../api/client'

function task(over: Partial<ScheduledTaskResponse> = {}): ScheduledTaskResponse {
  return {
    id: 1, speciesId: null, speciesName: null,
    bedId: null, bedName: null, gardenName: null,
    activityType: 'MOW',
    deadline: '2026-06-25', targetCount: 1, remainingCount: 1,
    status: 'PENDING',
    acceptableSpecies: [],
    gardenAreaId: null, gardenAreaName: null,
    maintenanceRuleId: null,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

/** Deadlines relative to the local calendar day, so the buckets are date-proof. */
function isoOffsetDays(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() + days)
  return d.toLocaleDateString('sv-SE')
}

function renderList() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={qc}>
      <TaskList />
    </QueryClientProvider>,
  )
}

beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})

describe('taskTitle', () => {
  it('renders the species name unchanged when a species task', () => {
    expect(taskTitle(task({ speciesName: 'Tomat', activityType: 'SOW' }))).toBe('Tomat')
  })

  it('renders the activity label with the garden area name for an area-scoped maintenance task', () => {
    expect(taskTitle(task({ activityType: 'MOW', gardenAreaId: 5, gardenAreaName: 'Gången' })))
      .toBe('Klippa gräs · Gången')
  })

  it('renders the activity label with the bed name for a bed-scoped maintenance task', () => {
    expect(taskTitle(task({ activityType: 'WEED', bedId: 3, bedName: 'Bädd 3' })))
      .toBe('Rensa ogräs · Bädd 3')
  })

  it('renders the activity label alone when there is no species and no place', () => {
    expect(taskTitle(task({ activityType: 'MOW' }))).toBe('Klippa gräs')
  })

  it('does not fall back to the raw enum for a place-less maintenance task', () => {
    expect(taskTitle(task({ activityType: 'MOW' }))).not.toBe('MOW')
  })
})

describe('isPlaceScoped', () => {
  it('is true for a bed task', () => {
    expect(isPlaceScoped(task({ bedId: 3 }))).toBe(true)
  })

  it('is true for an area task', () => {
    expect(isPlaceScoped(task({ gardenAreaId: 5 }))).toBe(true)
  })

  it('is false for a species task', () => {
    expect(isPlaceScoped(task({ speciesId: 9, activityType: 'SOW' }))).toBe(false)
  })
})

describe('TaskList buckets', () => {
  it('renders a task past its deadline in the overdue section', async () => {
    vi.mocked(api.tasks.list).mockResolvedValue([
      task({ id: 1, gardenAreaId: 5, gardenAreaName: 'Gången', deadline: isoOffsetDays(-3) }),
    ])
    renderList()
    expect(await screen.findByText('tasks.overdue')).toBeInTheDocument()
    expect(screen.getByText(isoOffsetDays(-3))).toBeInTheDocument()
  })

  it('leaves the overdue section out entirely when nothing is late', async () => {
    vi.mocked(api.tasks.list).mockResolvedValue([task({ deadline: isoOffsetDays(0) })])
    renderList()
    await screen.findByText('tasks.today')
    expect(screen.queryByText('tasks.overdue')).not.toBeInTheDocument()
  })

  it('does not surface completed tasks in any bucket', async () => {
    vi.mocked(api.tasks.list).mockResolvedValue([
      task({ id: 2, status: 'COMPLETED', deadline: isoOffsetDays(-5), remainingCount: 0 }),
    ])
    renderList()
    await screen.findByText('tasks.today')
    expect(screen.queryByText('tasks.overdue')).not.toBeInTheDocument()
    expect(screen.queryByText(isoOffsetDays(-5))).not.toBeInTheDocument()
  })
})

describe('TaskList drawer completion', () => {
  it('completes an area maintenance task without a species', async () => {
    const areaTask = task({ id: 7, gardenAreaId: 5, gardenAreaName: 'Gången', deadline: isoOffsetDays(-1) })
    vi.mocked(api.tasks.list).mockResolvedValue([areaTask])
    vi.mocked(api.tasks.complete).mockResolvedValue(undefined)

    renderList()
    fireEvent.click(await screen.findByText('Klippa gräs'))
    fireEvent.click(await screen.findByText('maintenance.markDone'))

    await waitFor(() => expect(api.tasks.complete).toHaveBeenCalledWith(7, null, 1))
  })

  it('completes a bed maintenance task for its whole remaining count', async () => {
    const bedTask = task({
      id: 8, bedId: 3, bedName: 'Bädd 3', activityType: 'WEED',
      deadline: isoOffsetDays(0), targetCount: 2, remainingCount: 2,
    })
    vi.mocked(api.tasks.list).mockResolvedValue([bedTask])
    vi.mocked(api.tasks.complete).mockResolvedValue(undefined)

    renderList()
    fireEvent.click(await screen.findByText('Rensa ogräs'))
    fireEvent.click(await screen.findByText('maintenance.markDone'))

    await waitFor(() => expect(api.tasks.complete).toHaveBeenCalledWith(8, null, 2))
  })

  it('keeps the navigate action alongside the completion action', async () => {
    vi.mocked(api.tasks.list).mockResolvedValue([
      task({ id: 9, gardenAreaId: 5, gardenAreaName: 'Gången', deadline: isoOffsetDays(0) }),
    ])
    renderList()
    fireEvent.click(await screen.findByText('Klippa gräs'))

    expect(screen.getByText('maintenance.markDone')).toBeInTheDocument()
    fireEvent.click(screen.getByText('tasks.perform'))
    expect(navigate).toHaveBeenCalledWith('/area/5')
  })

  it('offers no completion action for a species task', async () => {
    vi.mocked(api.tasks.list).mockResolvedValue([
      task({ id: 10, speciesId: 4, speciesName: 'Tomat', activityType: 'HARVEST', deadline: isoOffsetDays(0) }),
    ])
    renderList()
    fireEvent.click(await screen.findByText('Tomat'))

    expect(screen.getByText('tasks.perform')).toBeInTheDocument()
    expect(screen.queryByText('maintenance.markDone')).not.toBeInTheDocument()
  })

  it('reports a failed completion inside the drawer', async () => {
    vi.mocked(api.tasks.list).mockResolvedValue([
      task({ id: 11, gardenAreaId: 5, gardenAreaName: 'Gången', deadline: isoOffsetDays(0) }),
    ])
    vi.mocked(api.tasks.complete).mockRejectedValue(new Error('boom'))

    renderList()
    fireEvent.click(await screen.findByText('Klippa gräs'))
    fireEvent.click(await screen.findByText('maintenance.markDone'))

    expect(await screen.findByText('maintenance.markDoneError')).toBeInTheDocument()
  })
})
