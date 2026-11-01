import { useCallback, useEffect, useRef, useState } from 'react'

const PREFIX = 'verdant.list.'

function read<T>(key: string, initial: T): T {
  try {
    const raw = sessionStorage.getItem(PREFIX + key)
    return raw != null ? (JSON.parse(raw) as T) : initial
  } catch {
    return initial
  }
}

/**
 * State a list keeps for the browser session — its page, search or open groups — so leaving the list
 * and coming back, by the back button or the menu, lands where you were.
 */
export function useSessionState<T>(key: string, initial: T): [T, (value: T | ((prev: T) => T)) => void] {
  const [value, setValue] = useState<T>(() => read(key, initial))
  const set = useCallback((next: T | ((prev: T) => T)) => {
    setValue(prev => {
      const resolved = typeof next === 'function' ? (next as (p: T) => T)(prev) : next
      try { sessionStorage.setItem(PREFIX + key, JSON.stringify(resolved)) } catch { /* storage unavailable */ }
      return resolved
    })
  }, [key])
  return [value, set]
}

/** Calls [onChange] when [value] changes after the first render, so a remembered state survives mounting. */
export function useOnChange(value: unknown, onChange: () => void) {
  const previous = useRef(value)
  useEffect(() => {
    if (previous.current === value) return
    previous.current = value
    onChange()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value])
}
