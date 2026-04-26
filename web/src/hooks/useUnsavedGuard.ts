import { useEffect } from 'react'
import { useBlocker } from 'react-router-dom'

/**
 * Guards a form page against accidental loss of unsaved edits.
 *
 *   - When `isDirty` is true and the user tries to navigate away inside the
 *     SPA, a window.confirm prompt asks them to confirm. If they cancel,
 *     navigation is blocked and they stay on the page.
 *   - For full browser navigations (refresh, close, follow external link)
 *     we hook into `beforeunload` so the browser shows its native prompt.
 */
export function useUnsavedGuard(
  isDirty: boolean,
  message: string = 'Ändringar går förlorade. Är du säker?',
) {
  // Browser refresh / close / external link.
  useEffect(() => {
    if (!isDirty) return
    const handler = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = message
      return message
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }, [isDirty, message])

  // SPA navigation (router-driven).
  const blocker = useBlocker(({ currentLocation, nextLocation }) =>
    isDirty && currentLocation.pathname !== nextLocation.pathname,
  )

  useEffect(() => {
    if (blocker.state === 'blocked') {
      // eslint-disable-next-line no-alert
      if (window.confirm(message)) blocker.proceed()
      else blocker.reset()
    }
  }, [blocker, message])
}
