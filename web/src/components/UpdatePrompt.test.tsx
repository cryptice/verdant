import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { act, fireEvent, render, screen } from '@testing-library/react'
import i18n from '../i18n'
import { UpdatePrompt } from './UpdatePrompt'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))

const reload = vi.fn()
let deployed = 'build-1'

async function cameToFront() {
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible')
  await act(async () => { document.dispatchEvent(new Event('visibilitychange')) })
}

beforeEach(() => {
  vi.stubEnv('PROD', true)
  vi.stubEnv('VITE_APP_VERSION', 'build-1')
  deployed = 'build-1'
  vi.stubGlobal('fetch', vi.fn(async () => ({ ok: true, json: async () => ({ version: deployed }) })))
  reload.mockClear()
  Object.defineProperty(window, 'location', { value: { ...window.location, reload }, configurable: true })
})
afterEach(() => {
  vi.unstubAllEnvs()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

describe('UpdatePrompt', () => {
  it('says nothing while the app is up to date', async () => {
    render(<UpdatePrompt />)
    await cameToFront()
    expect(fetch).toHaveBeenCalledWith(expect.stringMatching(/^\/version\.json\?t=\d+$/), { cache: 'no-store' })
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('offers a reload once a newer build is deployed, and reloads on Ladda om', async () => {
    deployed = 'build-2'
    render(<UpdatePrompt />)
    await cameToFront()
    expect(screen.getByRole('status')).toHaveTextContent('En ny version av Verdant finns.')
    fireEvent.click(screen.getByRole('button', { name: 'Ladda om' }))
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('can be put off until later', async () => {
    deployed = 'build-2'
    render(<UpdatePrompt />)
    await cameToFront()
    fireEvent.click(screen.getByRole('button', { name: 'Senare' }))
    expect(screen.queryByRole('status')).toBeNull()
    expect(reload).not.toHaveBeenCalled()
  })

  it('checks every hour', async () => {
    vi.useFakeTimers()
    try {
      deployed = 'build-2'
      render(<UpdatePrompt />)
      await act(async () => { await vi.advanceTimersByTimeAsync(60 * 60_000) })
      expect(screen.getByRole('status')).toBeInTheDocument()
    } finally {
      vi.useRealTimers()
    }
  })

  it('stays quiet when the version cannot be read, and outside a production build', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('offline') }))
    const { unmount } = render(<UpdatePrompt />)
    await cameToFront()
    expect(screen.queryByRole('status')).toBeNull()
    unmount()

    vi.stubEnv('PROD', false)
    const fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)
    render(<UpdatePrompt />)
    await cameToFront()
    expect(fetchSpy).not.toHaveBeenCalled()
  })
})
