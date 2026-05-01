import { describe, expect, it } from 'vitest'
import { isValidNpk } from './Supplies'

describe('isValidNpk', () => {
  it('accepts integer NPK like 10-5-10', () => {
    expect(isValidNpk('10-5-10')).toBe(true)
  })

  it('accepts decimal NPK like 0.6-0.3-0.5', () => {
    expect(isValidNpk('0.6-0.3-0.5')).toBe(true)
  })

  it('accepts mixed integer/decimal NPK like 3.0-2-2', () => {
    expect(isValidNpk('3.0-2-2')).toBe(true)
  })

  it('trims whitespace before validating', () => {
    expect(isValidNpk('  10-5-10  ')).toBe(true)
  })

  it('rejects non-NPK strings', () => {
    expect(isValidNpk('abc')).toBe(false)
  })

  it('rejects two-segment NPK like 1-2', () => {
    expect(isValidNpk('1-2')).toBe(false)
  })

  it('rejects leading-dash NPK like -1-2-3', () => {
    expect(isValidNpk('-1-2-3')).toBe(false)
  })

  it('rejects trailing-decimal NPK like 1.-2-3', () => {
    expect(isValidNpk('1.-2-3')).toBe(false)
  })
})
