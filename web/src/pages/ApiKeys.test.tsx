import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { apiKeysApi, type ApiKey } from '../api/apiKeys'
import i18n from '../i18n'
import { ApiKeys } from './ApiKeys'

vi.mock('react-i18next', async original => ({
  ...await original<typeof import('react-i18next')>(),
  useTranslation: () => ({ t: i18n.getFixedT('sv'), i18n }),
}))

const key: ApiKey = { id: 'k1', name: 'Claude Code', organizationId: 1, createdAt: '2026-10-26T10:00:00Z', expiresAt: '2027-01-24T10:00:00Z', lastUsedAt: null }

afterEach(() => vi.restoreAllMocks())

function show() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={qc}><MemoryRouter><ApiKeys /></MemoryRouter></QueryClientProvider>)
}

describe('ApiKeys', () => {
  it('creates a key with the chosen lifetime and shows the token once', async () => {
    vi.spyOn(apiKeysApi, 'list').mockResolvedValue([])
    vi.spyOn(apiKeysApi, 'create').mockResolvedValue({ key, token: 'header.payload.signature' })
    show()
    expect(await screen.findByText('Inga aktiva nycklar.')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('Giltig'), { target: { value: '365' } })
    fireEvent.click(screen.getByRole('button', { name: 'Skapa nyckel' }))
    await waitFor(() => expect(apiKeysApi.create).toHaveBeenCalledWith('Claude Code', 365))
    expect(await screen.findByLabelText('Nyckel')).toHaveValue('header.payload.signature')
    fireEvent.click(screen.getByRole('button', { name: 'Klar' }))
    expect(screen.queryByLabelText('Nyckel')).not.toBeInTheDocument()
  })

  it('lists active keys and revokes one', async () => {
    vi.spyOn(apiKeysApi, 'list').mockResolvedValue([key])
    vi.spyOn(apiKeysApi, 'revoke').mockResolvedValue(undefined)
    show()
    expect(await screen.findByText('Claude Code', { selector: 'strong' })).toBeInTheDocument()
    expect(screen.getByText(/aldrig använd/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Återkalla' }))
    await waitFor(() => expect(apiKeysApi.revoke).toHaveBeenCalledWith('k1', expect.anything()))
  })
})
