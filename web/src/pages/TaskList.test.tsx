import { describe, it, expect } from 'vitest'
import { taskTitle } from './TaskList'
import type { ScheduledTaskResponse } from '../api/client'

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
