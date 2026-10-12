import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import type { ScheduledTaskResponse } from '../api/client'
import { useUndoableCompletion, COMPLETION_UNDO_MS } from './useUndoableCompletion'
const task = { id: 1 } as ScheduledTaskResponse
beforeEach(() => vi.useFakeTimers())
afterEach(() => { vi.clearAllTimers(); vi.useRealTimers() })
it('undo cancels completion and restores the task', async () => {
  const commit = vi.fn().mockResolvedValue(undefined)
  const { result } = renderHook(() => useUndoableCompletion(commit))
  act(() => result.current.schedule(task))
  expect(result.current.hiddenIds.has(1)).toBe(true)
  act(() => result.current.undo())
  await act(() => vi.advanceTimersByTimeAsync(COMPLETION_UNDO_MS))
  expect(commit).not.toHaveBeenCalled()
  expect(result.current.hiddenIds.has(1)).toBe(false)
})
it('a checkmark still completes after navigating away', async () => {
  const commit = vi.fn().mockResolvedValue(undefined)
  const { result, unmount } = renderHook(() => useUndoableCompletion(commit))
  act(() => result.current.schedule(task))
  unmount()
  await act(() => vi.advanceTimersByTimeAsync(COMPLETION_UNDO_MS))
  expect(commit).toHaveBeenCalledExactlyOnceWith(task)
})
it('failed completion restores the task and reports failure', async () => {
  const commit = vi.fn().mockRejectedValue(new Error('Offline'))
  const { result } = renderHook(() => useUndoableCompletion(commit))
  act(() => result.current.schedule(task))
  await act(() => vi.advanceTimersByTimeAsync(COMPLETION_UNDO_MS))
  expect(result.current.hiddenIds.has(1)).toBe(false)
  expect(result.current.failed).toBe(true)
})
