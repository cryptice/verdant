import { describe, expect, it } from 'vitest'
import { act, renderHook } from '@testing-library/react'
import { useOnChange, useSessionState } from './listMemory'

describe('useSessionState', () => {
  it('keeps the state of a list for the session, so a remount starts where it was', () => {
    const first = renderHook(() => useSessionState('page.test', 0))
    act(() => first.result.current[1](3))
    first.unmount()
    expect(renderHook(() => useSessionState('page.test', 0)).result.current[0]).toBe(3)
    expect(renderHook(() => useSessionState('page.other', 0)).result.current[0]).toBe(0)
  })
})

describe('useOnChange', () => {
  it('reacts to a change, not to mounting', () => {
    let calls = 0
    const hook = renderHook(({ value }) => useOnChange(value, () => { calls++ }), { initialProps: { value: 'dahlia' } })
    expect(calls).toBe(0)
    hook.rerender({ value: 'dahlia' })
    expect(calls).toBe(0)
    hook.rerender({ value: 'zinnia' })
    expect(calls).toBe(1)
  })
})
