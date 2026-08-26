import { describe, it, expect } from 'vitest'
import {
  MAINTENANCE_ACTIVITIES,
  activitiesForTarget,
  maintenanceActivityLabelSv,
  dueState,
  formatInterval,
  formatSeasonWindow,
  hasSeasonWindow,
  todayIsoLocal,
} from './maintenance'
import type { MaintenanceRuleResponse } from '../api/client'

function rule(over: Partial<MaintenanceRuleResponse> = {}): MaintenanceRuleResponse {
  return {
    id: 1, bedId: null, bedName: null, gardenAreaId: 5, gardenAreaName: 'Gången',
    activityType: 'WEED', intervalDays: 21, anchorDate: null,
    seasonStartMonth: null, seasonStartDay: null,
    seasonEndMonth: null, seasonEndDay: null,
    active: true, notes: null,
    lastDoneDate: null, nextDueDate: '2026-06-25',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over,
  }
}

describe('activitiesForTarget', () => {
  it('gives beds exactly the three activities the backend accepts', () => {
    expect(activitiesForTarget('BED')).toEqual(['WATER', 'WEED', 'FERTILIZE'])
  })

  it('excludes FERTILIZE from areas and includes the area-only work', () => {
    const areaActivities = activitiesForTarget('AREA')
    expect(areaActivities).not.toContain('FERTILIZE')
    expect(areaActivities).toContain('MOW')
    expect(areaActivities).toContain('WEED')
  })

  it('covers every activity across the two targets', () => {
    const union = new Set([...activitiesForTarget('BED'), ...activitiesForTarget('AREA')])
    expect([...union].sort()).toEqual([...MAINTENANCE_ACTIVITIES].sort())
  })

  it('has a Swedish label for every activity', () => {
    for (const a of MAINTENANCE_ACTIVITIES) {
      expect(maintenanceActivityLabelSv(a)).not.toBe(a)
    }
  })
})

describe('dueState', () => {
  const today = '2026-06-25'

  it('reports overdue when the due date has passed', () => {
    expect(dueState(rule({ nextDueDate: '2026-06-20' }), today))
      .toEqual({ kind: 'overdue', days: 5 })
  })

  it('reports due today on the due date itself', () => {
    expect(dueState(rule({ nextDueDate: today }), today).kind).toBe('due')
  })

  it('reports upcoming with days remaining', () => {
    expect(dueState(rule({ nextDueDate: '2026-07-02' }), today))
      .toEqual({ kind: 'upcoming', days: 7 })
  })

  it('reports inactive regardless of date, so a paused rule never nags', () => {
    expect(dueState(rule({ active: false, nextDueDate: '2026-01-01' }), today).kind)
      .toBe('inactive')
  })
})

describe('todayIsoLocal', () => {
  it('returns the local calendar date as YYYY-MM-DD, not the UTC one', () => {
    const now = new Date()
    const expected = [
      now.getFullYear(),
      String(now.getMonth() + 1).padStart(2, '0'),
      String(now.getDate()).padStart(2, '0'),
    ].join('-')
    expect(todayIsoLocal()).toBe(expected)
  })
})

describe('formatInterval', () => {
  it('uses days below a fortnight', () => {
    expect(formatInterval(1)).toBe('Varje dag')
    expect(formatInterval(3)).toBe('Var 3:e dag')
  })

  it('uses weeks for exact multiples', () => {
    expect(formatInterval(7)).toBe('Varje vecka')
    expect(formatInterval(21)).toBe('Var 3:e vecka')
  })

  it('falls back to days when not a whole number of weeks', () => {
    expect(formatInterval(10)).toBe('Var 10:e dag')
  })

  it('picks the Swedish ordinal suffix off the last digit, not just single digits', () => {
    // 1 (day) and 7 (day, i.e. 1 week) both short-circuit to "Varje X" and
    // never reach the ordinal — so :a for n=1 is not observable through this
    // public surface. n=2 and n=3 below exercise the day-ordinal branch,
    // and n=21/22 exercise the same ordinal logic via the week branch.
    expect(formatInterval(2)).toBe('Var 2:a dag')
    expect(formatInterval(3)).toBe('Var 3:e dag')
    expect(formatInterval(11)).toBe('Var 11:e dag')
    expect(formatInterval(12)).toBe('Var 12:e dag')
    expect(formatInterval(31)).toBe('Var 31:a dag')
    expect(formatInterval(147)).toBe('Var 21:a vecka') // 147 / 7 = 21
    expect(formatInterval(154)).toBe('Var 22:a vecka') // 154 / 7 = 22
  })
})

describe('season windows', () => {
  it('detects presence from all four bounds', () => {
    expect(hasSeasonWindow(rule())).toBe(false)
    expect(hasSeasonWindow(rule({
      seasonStartMonth: 4, seasonStartDay: 1, seasonEndMonth: 10, seasonEndDay: 15,
    }))).toBe(true)
  })

  it('formats a summer window', () => {
    expect(formatSeasonWindow(rule({
      seasonStartMonth: 4, seasonStartDay: 1, seasonEndMonth: 10, seasonEndDay: 15,
    }))).toBe('1 apr – 15 okt')
  })

  it('formats a wrap-around winter window without special-casing it', () => {
    expect(formatSeasonWindow(rule({
      seasonStartMonth: 11, seasonStartDay: 1, seasonEndMonth: 3, seasonEndDay: 31,
    }))).toBe('1 nov – 31 mar')
  })

  it('returns null when there is no window', () => {
    expect(formatSeasonWindow(rule())).toBeNull()
  })
})
