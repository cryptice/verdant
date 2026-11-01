import { describe, expect, it } from 'vitest'
import type { SpeciesResponse } from '../../api/client'
import { duplicateSearchText, findDuplicateVarieties } from './duplicates'

const variety = (id: number, commonNameSv: string, variantNameSv?: string): SpeciesResponse => ({
  id, commonName: commonNameSv, commonNameSv, variantName: variantNameSv, variantNameSv,
  bloomMonths: [], sowingMonths: [], growingPositions: [], soils: [], photos: [], tags: [], groups: [], providers: [],
  custom: false, isSystem: true,
})
const names = (commonNameSv: string, variantNameSv = '') => ({ commonName: '', commonNameSv, variantName: '', variantNameSv })

const catalogue = [
  variety(1, 'Sommardahlia', 'Opera White'),
  variety(2, 'Pompondahlia', 'Lilliput'),
  variety(3, 'Zinnia'),
  variety(4, 'Zinnia', 'Queen Lime'),
]

describe('findDuplicateVarieties', () => {
  it('matches the variety name when the species names overlap, ignoring case', () => {
    expect(findDuplicateVarieties(names('dahlia', 'opera white'), catalogue).map(s => s.id)).toEqual([1])
    expect(findDuplicateVarieties(names('Dahlia', 'Lilliput'), catalogue).map(s => s.id)).toEqual([2])
  })

  it('does not match the same variety name on another species', () => {
    expect(findDuplicateVarieties(names('Ros', 'Opera White'), catalogue)).toEqual([])
  })

  it('without a variety name, matches only a species without one', () => {
    expect(findDuplicateVarieties(names('Zinnia'), catalogue).map(s => s.id)).toEqual([3])
  })

  it('never matches a copy still named "(kopia)"', () => {
    expect(findDuplicateVarieties(names('Sommardahlia', 'Opera White (kopia)'), catalogue)).toEqual([])
  })

  it('searches by the variety name first', () => {
    expect(duplicateSearchText(names('Dahlia', ' Lilliput '))).toBe('Lilliput')
    expect(duplicateSearchText(names('Zinnia'))).toBe('Zinnia')
  })
})
