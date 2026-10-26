import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useRegisterSW } from 'virtual:pwa-register/react'

const CHECK_MS = 60 * 60_000

/** However the browser does or doesn't tell of the new version taking over, the page reloads by then. */
const FALLBACK_MS = 8_000

/**
 * Lets the new version take over and reloads once it has, whether or not the old one controls this page (a page
 * opened with a hard refresh isn't controlled, so it never hears that a new worker took over). A version deployed
 * again meanwhile is still being fetched: it takes over once it's ready. With none coming (another tab switched
 * already), the page just reloads into it. Whatever the browser says or doesn't, it reloads after a few seconds
 * anyway: then into the new version, or into the old one, which offers the new again.
 */
async function switchToNewVersion(): Promise<void> {
  let done = false
  const reload = () => {
    if (done) return
    done = true
    clearTimeout(fallback)
    window.location.reload()
  }
  const fallback = setTimeout(reload, FALLBACK_MS)
  // The new version taking control of this page.
  navigator.serviceWorker?.addEventListener?.('controllerchange', reload)
  const registration = await navigator.serviceWorker?.getRegistration().catch(() => undefined)
  const next = registration?.waiting ?? registration?.installing
  if (!next) {
    reload()
    return
  }
  const takeOver = () => next.postMessage({ type: 'SKIP_WAITING' })
  next.addEventListener('statechange', () => {
    if (next.state === 'installed') takeOver()
    // Taken over, or failed to install: either way the reload finds the version there is.
    else if (next.state === 'activated' || next.state === 'redundant') reload()
  })
  if (next.state === 'installed') takeOver()
  else if (next.state === 'activated') reload()
}

/** Offers a newer deployed version, looking every hour and whenever the app comes back to the front. */
export function UpdatePrompt() {
  const { t } = useTranslation()
  const { needRefresh: [needRefresh] } = useRegisterSW({
    onRegisteredSW(_url, registration) {
      if (!registration) return
      const check = () => { if (navigator.onLine !== false) registration.update().catch(() => {}) }
      setInterval(check, CHECK_MS)
      document.addEventListener('visibilitychange', () => { if (document.visibilityState === 'visible') check() })
    },
  })
  // Put off for now; the new version starts with the app's next launch anyway.
  const [later, setLater] = useState(false)
  // Shown from the tap, so it's clear it took while the new version takes over.
  const [updating, setUpdating] = useState(false)
  if (!needRefresh || later) return null
  if (updating) return <div className="update-prompt" role="status">{t('updatePrompt.updating')}</div>
  return (
    <div className="update-prompt" role="status">
      {t('updatePrompt.ready')}{' '}
      <button type="button" className="update-prompt-button" onClick={() => { setUpdating(true); void switchToNewVersion() }}>
        {t('updatePrompt.reload')}
      </button>{' '}
      <button type="button" className="update-prompt-button" onClick={() => setLater(true)}>{t('updatePrompt.later')}</button>
    </div>
  )
}
