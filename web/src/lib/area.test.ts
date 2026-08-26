import { describe, it, expect } from 'vitest'
import {
  AREA_CATEGORIES,
  areaCategoryLabelSv,
  areaEventLabelSv,
  sortAreasByNaturalName,
} from './area'
import type { GardenAreaResponse } from '../api/client'

function area(name: string, id = 1): GardenAreaResponse {
  return {
    id, gardenId: 1, gardenName: 'Trädgården', name,
    description: null, category: 'WALKWAY', boundaryJson: null, sizeSqm: null,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
  }
}

describe('AREA_CATEGORIES', () => {
  it('matches the backend enum exactly', () => {
    expect(AREA_CATEGORIES).toEqual([
      'WALKWAY', 'LAWN', 'HEDGE', 'COMPOST',
      'GREENHOUSE', 'WATER_FEATURE', 'STRUCTURE', 'OTHER',
    ])
  })

  it('has a Swedish label for every category', () => {
    for (const c of AREA_CATEGORIES) {
      const label = areaCategoryLabelSv(c)
      expect(label).toBeTruthy()
      expect(label).not.toBe(c)
    }
  })
})

describe('areaEventLabelSv', () => {
  it('translates known activities', () => {
    expect(areaEventLabelSv('WEED')).toBe('Rensade ogräs')
    expect(areaEventLabelSv('MOW')).toBe('Klippte gräs')
    expect(areaEventLabelSv('NOTE')).toBe('Anteckning')
  })

  it('falls back to a readable form for anything unknown', () => {
    expect(areaEventLabelSv('SOMETHING_NEW')).toBe('Something_new')
  })
})

describe('sortAreasByNaturalName', () => {
  it('orders digit runs numerically, so #10 follows #9', () => {
    const sorted = sortAreasByNaturalName([
      area('Gång #10', 1), area('Gång #9', 2), area('Gång #1', 3),
    ])
    expect(sorted.map((a) => a.name)).toEqual(['Gång #1', 'Gång #9', 'Gång #10'])
  })

  it('does not mutate its input', () => {
    const input = [area('B', 1), area('A', 2)]
    sortAreasByNaturalName(input)
    expect(input.map((a) => a.name)).toEqual(['B', 'A'])
  })
})
