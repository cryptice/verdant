import { afterEach, expect, it, vi } from 'vitest'
import { makeApiRequest } from '@verdant/shared'

afterEach(() => vi.unstubAllGlobals())

it('does not log out a new session when an old request returns 401', async () => {
  let token = 'old'
  let respond!: (response: Response) => void
  vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(resolve => { respond = resolve })))
  const onUnauthorized = vi.fn()
  const request = makeApiRequest({ getToken: () => token, onUnauthorized })
  const pending = request('/api/dashboard')
  const rejection = expect(pending).rejects.toThrow('Unauthorized')
  token = 'new'
  respond(new Response(null, { status: 401 }))
  await rejection
  expect(onUnauthorized).not.toHaveBeenCalled()
})

it('still logs out the current session on 401', async () => {
  vi.stubGlobal('fetch', vi.fn(async () => new Response(null, { status: 401 })))
  const onUnauthorized = vi.fn()
  const request = makeApiRequest({ getToken: () => 'current', onUnauthorized })
  await expect(request('/api/dashboard')).rejects.toThrow('Unauthorized')
  expect(onUnauthorized).toHaveBeenCalledOnce()
})
