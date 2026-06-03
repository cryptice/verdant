import { describe, expect, test } from 'vitest'
import { harvestDeltaPct } from './harvest'

describe('harvestDeltaPct', () => {
  test('returns null when there is no prior-year baseline', () => {
    expect(harvestDeltaPct(100, 0)).toBeNull()
    expect(harvestDeltaPct(0, 0)).toBeNull()
  })

  test('positive growth rounds to a whole percent', () => {
    expect(harvestDeltaPct(124, 100)).toBe(24)
    expect(harvestDeltaPct(133, 100)).toBe(33)
  })

  test('decline is negative', () => {
    expect(harvestDeltaPct(80, 100)).toBe(-20)
  })

  test('no change is zero', () => {
    expect(harvestDeltaPct(100, 100)).toBe(0)
  })
})
