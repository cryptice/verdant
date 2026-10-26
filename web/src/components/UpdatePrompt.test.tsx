import { act, fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import i18n from '../i18n'
import { UpdatePrompt } from './UpdatePrompt'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))

const sw = vi.hoisted(() => ({ needRefresh: false, update: vi.fn(async () => {}), options: null as null | { onRegisteredSW?: (url: string, r: unknown) => void } }))
vi.mock('virtual:pwa-register/react', () => ({
  useRegisterSW: (options: typeof sw.options) => {
    sw.options = options
    return { needRefresh: [sw.needRefresh, vi.fn()], offlineReady: [false, vi.fn()], updateServiceWorker: sw.update }
  },
}))

beforeEach(() => { sw.needRefresh = false; sw.update.mockClear() })

// Lets the awaited registration lookup and the click handlers settle.
const settle = () => act(async () => { await Promise.resolve(); await Promise.resolve() })

it('says nothing while the app is up to date', () => {
  render(<UpdatePrompt />)
  expect(screen.queryByRole('status')).toBeNull()
})

describe('Ladda om', () => {
  const reload = vi.fn()
  type Worker = EventTarget & { state: string; postMessage: ReturnType<typeof vi.fn> }
  let waiting: Worker | null
  let installing: Worker | null
  beforeEach(() => {
    reload.mockClear()
    Object.defineProperty(window, 'location', { value: { ...window.location, reload }, configurable: true })
    waiting = Object.assign(new EventTarget(), { state: 'installed', postMessage: vi.fn() })
    installing = null
    // A page the service worker doesn't control (after a hard refresh, say): no controllerchange will come.
    Object.defineProperty(navigator, 'serviceWorker', {
      value: { controller: null, getRegistration: async () => ({ waiting, installing }) }, configurable: true,
    })
  })

  it('offers a new version once fetched, and on Ladda om switches to it and reloads once it has taken over', async () => {
    sw.needRefresh = true
    render(<UpdatePrompt />)
    expect(screen.getByRole('status')).toHaveTextContent('En ny version av Verdant finns.')
    fireEvent.click(screen.getByRole('button', { name: 'Ladda om' }))
    await settle()
    expect(waiting!.postMessage).toHaveBeenCalledWith({ type: 'SKIP_WAITING' })
    expect(reload).not.toHaveBeenCalled()
    expect(screen.getByRole('status')).toHaveTextContent('Uppdaterar Verdant…')
    expect(screen.queryByRole('button', { name: 'Ladda om' })).toBeNull()
    waiting!.state = 'activated'
    waiting!.dispatchEvent(new Event('statechange'))
    expect(reload).toHaveBeenCalled()
  })

  it('waits for a newer version still being fetched (deployed again meanwhile), then switches to it', async () => {
    installing = Object.assign(new EventTarget(), { state: 'installing', postMessage: vi.fn() })
    waiting = null
    sw.needRefresh = true
    render(<UpdatePrompt />)
    fireEvent.click(screen.getByRole('button', { name: 'Ladda om' }))
    await settle()
    expect(installing.postMessage).not.toHaveBeenCalled()
    installing.state = 'installed'
    installing.dispatchEvent(new Event('statechange'))
    expect(installing.postMessage).toHaveBeenCalledWith({ type: 'SKIP_WAITING' })
    installing.state = 'activated'
    installing.dispatchEvent(new Event('statechange'))
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it("reloads anyway if the new version can't be installed, rather than saying Uppdaterar for ever", async () => {
    installing = Object.assign(new EventTarget(), { state: 'installing', postMessage: vi.fn() })
    waiting = null
    sw.needRefresh = true
    render(<UpdatePrompt />)
    fireEvent.click(screen.getByRole('button', { name: 'Ladda om' }))
    await settle()
    installing.state = 'redundant'
    installing.dispatchEvent(new Event('statechange'))
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('reloads anyway when the browser never says the new version took over', async () => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    try {
      sw.needRefresh = true
      render(<UpdatePrompt />)
      fireEvent.click(screen.getByRole('button', { name: 'Ladda om' }))
      await settle()
      expect(waiting!.postMessage).toHaveBeenCalledWith({ type: 'SKIP_WAITING' })
      expect(reload).not.toHaveBeenCalled()
      await act(async () => { vi.advanceTimersByTime(8_000) })
      expect(reload).toHaveBeenCalledTimes(1)
    } finally {
      vi.useRealTimers()
    }
  })

  it('reloads once the new version takes control of the page', async () => {
    const container = Object.assign(new EventTarget(), { controller: {}, getRegistration: async () => ({ waiting, installing }) })
    Object.defineProperty(navigator, 'serviceWorker', { value: container, configurable: true })
    sw.needRefresh = true
    render(<UpdatePrompt />)
    fireEvent.click(screen.getByRole('button', { name: 'Ladda om' }))
    await settle()
    container.dispatchEvent(new Event('controllerchange'))
    expect(reload).toHaveBeenCalledTimes(1)
  })

  it('just reloads when no new version is waiting any more (another tab switched already)', async () => {
    waiting = null
    sw.needRefresh = true
    render(<UpdatePrompt />)
    fireEvent.click(screen.getByRole('button', { name: 'Ladda om' }))
    await settle()
    expect(reload).toHaveBeenCalled()
  })
})

it('can be put off until later', () => {
  sw.needRefresh = true
  render(<UpdatePrompt />)
  fireEvent.click(screen.getByRole('button', { name: 'Senare' }))
  expect(screen.queryByRole('status')).toBeNull()
  expect(sw.update).not.toHaveBeenCalled()
})

it('looks for a new version when the app comes back to the front', () => {
  render(<UpdatePrompt />)
  const update = vi.fn(async () => {})
  sw.options!.onRegisteredSW!('/sw.js', { update })
  vi.spyOn(document, 'visibilityState', 'get').mockReturnValue('visible')
  document.dispatchEvent(new Event('visibilitychange'))
  expect(update).toHaveBeenCalled()
})
