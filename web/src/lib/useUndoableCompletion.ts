import { useRef, useState } from 'react'
import type { ScheduledTaskResponse } from '../api/client'

export const COMPLETION_UNDO_MS = 6000

export function useUndoableCompletion(commit: (task: ScheduledTaskResponse) => Promise<unknown>) {
  const commitRef = useRef(commit)
  commitRef.current = commit
  const timers = useRef(new Map<number, ReturnType<typeof setTimeout>>())
  const [hiddenIds, setHiddenIds] = useState<Set<number>>(new Set())
  const [undoTask, setUndoTask] = useState<ScheduledTaskResponse | null>(null)
  const [failed, setFailed] = useState(false)
  // Pending checkmarks finish even when the user navigates to another page.
  const undo = () => {
    if (!undoTask) return
    const timer = timers.current.get(undoTask.id)
    if (timer === undefined) return
    clearTimeout(timer)
    timers.current.delete(undoTask.id)
    setHiddenIds(ids => { const next = new Set(ids); next.delete(undoTask.id); return next })
    setUndoTask(null)
  }
  const schedule = (task: ScheduledTaskResponse) => {
    if (timers.current.has(task.id)) return
    setFailed(false)
    setHiddenIds(ids => new Set(ids).add(task.id))
    setUndoTask(task)
    timers.current.set(task.id, setTimeout(async () => {
      timers.current.delete(task.id)
      setUndoTask(current => current?.id === task.id ? null : current)
      try { await commitRef.current(task) }
      catch {
        setHiddenIds(ids => { const next = new Set(ids); next.delete(task.id); return next })
        setFailed(true)
      }
    }, COMPLETION_UNDO_MS))
  }
  return { schedule, undo, undoTask, hiddenIds, failed }
}
