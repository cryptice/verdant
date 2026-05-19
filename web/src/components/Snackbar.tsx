import { useCallback, useEffect, useState } from 'react'

/**
 * Auto-dismissing snackbar pattern. Returns the current message, a setter
 * (call with `null` to clear), and a convenience `show(message)` helper.
 * Mirrors the Android SnackbarHostState contract: subsequent `show` calls
 * replace the visible message.
 */
export function useSnackbar(autoDismissMs = 2500) {
  const [message, setMessage] = useState<string | null>(null)

  useEffect(() => {
    if (!message) return
    const id = window.setTimeout(() => setMessage(null), autoDismissMs)
    return () => window.clearTimeout(id)
  }, [message, autoDismissMs])

  const show = useCallback((m: string) => setMessage(m), [])
  return { message, setMessage, show }
}

export function Snackbar({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        position: 'fixed',
        bottom: 24,
        left: '50%',
        transform: 'translateX(-50%)',
        background: 'var(--color-ink)',
        color: 'var(--color-cream)',
        padding: '10px 18px',
        borderRadius: 8,
        fontFamily: 'var(--font-mono)',
        fontSize: 11,
        letterSpacing: 1.2,
        boxShadow: '0 6px 24px rgba(0,0,0,0.18)',
        zIndex: 1000,
      }}
    >
      {message}
    </div>
  )
}
