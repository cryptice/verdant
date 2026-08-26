import { describe, it, expect } from 'vitest'
import i18n from '../i18n'
import { taskTitle, taskSubject } from './Dashboard'
import type { ScheduledTaskResponse } from '../api/client'

const t = i18n.getFixedT('sv')

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

const areaTask = task({
  activityType: 'MOW', gardenAreaId: 5, gardenAreaName: 'Gången', gardenName: 'Hemma',
})

describe('taskTitle', () => {
  it('names the place for an area-scoped maintenance task', () => {
    expect(taskTitle(areaTask, t)).toBe('Gången')
  })

  it('does not render the raw enum for an area-scoped maintenance task', () => {
    expect(taskTitle(areaTask, t)).not.toBe('MOW')
  })

  it('still names the bed for bed-scoped maintenance', () => {
    expect(taskTitle(task({ activityType: 'WEED', bedId: 3, bedName: 'Bädd 3' }), t)).toBe('Bädd 3')
  })

  it('still names the species for a species task', () => {
    expect(taskTitle(task({ activityType: 'SOW', speciesId: 2, speciesName: 'Tomat' }), t)).toBe('Tomat')
  })
})

describe('taskSubject', () => {
  it('translates an area-only activity instead of echoing the enum', () => {
    expect(taskSubject(areaTask, t)).toBe('Klippa gräs · Hemma')
  })

  it('carries the garden for bed-scoped maintenance', () => {
    expect(taskSubject(task({ activityType: 'WEED', bedId: 3, bedName: 'Bädd 3', gardenName: 'Hemma' }), t))
      .toBe('Rensa ogräs · Hemma')
  })

  it('carries the bed for a species task', () => {
    expect(taskSubject(task({ activityType: 'SOW', speciesId: 2, speciesName: 'Tomat', bedId: 3, bedName: 'Bädd 3' }), t))
      .toBe('Så · Bädd 3')
  })

  it('has a Swedish label for every area-only activity', () => {
    for (const a of ['MOW', 'RAKE', 'PRUNE', 'EDGE', 'SWEEP', 'TOP_UP', 'CLEAN', 'INSPECT']) {
      expect(t(`activityType.${a}`)).not.toBe(a)
    }
  })
})
