import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'

const CHECK_MS = 60 * 60_000

/** The deployed build's version, or null when it can't be read (offline, or a server without version.json). */
async function deployedVersion(): Promise<string | null> {
  try {
    const response = await fetch(`/version.json?t=${Date.now()}`, { cache: 'no-store' })
    if (!response.ok) return null
    const body = await response.json() as { version?: unknown }
    return typeof body.version === 'string' ? body.version : null
  } catch {
    return null
  }
}

/**
 * Offers a reload once a newer build has been deployed. Looks every hour and whenever the app comes back to the
 * front, so a tab left open for days still learns of it. Only in a production build, which knows its own version.
 */
export function UpdatePrompt() {
  const { t } = useTranslation()
  const [newVersion, setNewVersion] = useState(false)
  const [later, setLater] = useState(false)

  useEffect(() => {
    const current = import.meta.env.VITE_APP_VERSION as string | undefined
    if (!import.meta.env.PROD || !current) return
    const check = async () => {
      if (navigator.onLine === false) return
      const deployed = await deployedVersion()
      if (deployed && deployed !== current) setNewVersion(true)
    }
    const onVisible = () => { if (document.visibilityState === 'visible') void check() }
    const timer = setInterval(() => void check(), CHECK_MS)
    document.addEventListener('visibilitychange', onVisible)
    return () => {
      clearInterval(timer)
      document.removeEventListener('visibilitychange', onVisible)
    }
  }, [])

  if (!newVersion || later) return null
  return (
    <div className="update-prompt" role="status">
      {t('updatePrompt.ready')}{' '}
      <button type="button" className="update-prompt-button" onClick={() => window.location.reload()}>{t('updatePrompt.reload')}</button>{' '}
      <button type="button" className="update-prompt-button" onClick={() => setLater(true)}>{t('updatePrompt.later')}</button>
    </div>
  )
}
