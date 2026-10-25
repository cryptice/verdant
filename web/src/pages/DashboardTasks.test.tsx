import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { DashboardTasks, dashboardOverdueTasks, dashboardTaskDays } from './Dashboard'
import i18n from '../i18n'
import type { ScheduledTaskResponse } from '../api/client'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))
const now = new Date(2026, 9, 10, 23, 30)
function task(id: number, date: string | null, extra: Partial<ScheduledTaskResponse> = {}): ScheduledTaskResponse {
  return { id, speciesId: null, speciesName: null, bedId: null, bedName: null, gardenName: null, gardenAreaId: null, gardenAreaName: null, maintenanceRuleId: null, activityType: 'TODO', deadline: date, notes: `Task ${id}`, targetCount: 1, remainingCount: 1, status: 'PENDING', acceptableSpecies: [], createdAt: '', updatedAt: '', ...extra }
}
function show(tasks: ScheduledTaskResponse[]) {
  return render(<MemoryRouter><DashboardTasks tasks={tasks} now={now} /></MemoryRouter>)
}
describe('dashboard task schedule', () => {
  it('groups pending tasks into seven local calendar days and excludes undated and outside dates', () => {
    const days = dashboardTaskDays([task(1, '2026-10-10'), task(2, '2026-10-11'), task(3, '2026-10-16'), task(4, '2026-10-17'), task(5, null), task(6, '2026-10-09'), task(7, '2026-10-10', { status: 'COMPLETED' }), task(8, '2026-10-20', { earliestDate: '2026-10-12' })], now)
    expect(days.map(day => day.tasks.map(task => task.id))).toEqual([[1], [2], [8], [], [], [], [3]])
  })
  it('shows today and tomorrow and keeps later tasks in an expandable section', () => {
    show([task(1, '2026-10-10'), task(2, '2026-10-11'), task(3, '2026-10-16')])
    expect(screen.getByText('Idag')).toBeInTheDocument()
    expect(screen.getByText('Imorgon')).toBeInTheDocument()
    const details = screen.getByText('Kommande vecka (1)').closest('details')!
    expect(details.open).toBe(false)
    expect(screen.getByText('Task 3').closest('details')).toBe(details)
    fireEvent.click(screen.getByText('Kommande vecka (1)'))
    expect(details.open).toBe(true)
  })
  it('links to all tasks when the entire window is empty', () => {
    show([task(1, '2026-10-17')])
    expect(screen.getByText('Inga uppgifter de kommande sju dagarna.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Visa alla uppgifter' })).toHaveAttribute('href', '/tasks')
  })
  it('offers the week even when today and tomorrow are empty', () => {
    show([task(1, '2026-10-12')])
    expect(screen.getByText('Kommande vecka (1)')).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Visa alla uppgifter' })).not.toBeInTheDocument()
  })
  it('collects pending tasks past their deadline, oldest first', () => {
    const overdue = dashboardOverdueTasks([task(1, '2026-10-09'), task(2, '2026-10-01'), task(3, '2026-10-10'), task(4, '2026-10-08', { status: 'COMPLETED' }), task(5, null)], now)
    expect(overdue.map(task => task.id)).toEqual([2, 1])
  })
  it('shows overdue tasks in their own section ahead of today', () => {
    show([task(1, '2026-10-08'), task(2, '2026-10-10')])
    const headings = screen.getAllByRole('heading', { level: 3 }).map(heading => heading.textContent)
    expect(headings.slice(0, 2)).toEqual(['Försenat (1)', 'Idag'])
    expect(screen.getByText('Task 1')).toBeInTheDocument()
    expect(screen.queryByText('Inga uppgifter de kommande sju dagarna.')).not.toBeInTheDocument()
  })
})
