import { fireEvent, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, expect, it, vi } from 'vitest'
import { Supplies } from './Supplies'
import { api } from '../api/client'

vi.mock('../api/client', async (original) => {
  const actual = await original<typeof import('../api/client')>()
  return { ...actual, api: { ...actual.api,
    supplies: { ...actual.api.supplies, types: vi.fn(), list: vi.fn() },
    seasons: { ...actual.api.seasons, list: vi.fn() },
  } }
})
vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))

function mount() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<MemoryRouter><QueryClientProvider client={client}><Supplies /></QueryClientProvider></MemoryRouter>)
}

beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.supplies.types).mockResolvedValue([])
  vi.mocked(api.supplies.list).mockResolvedValue([])
  vi.mocked(api.seasons.list).mockResolvedValue([])
})

it('renders inventory after the initial loading state', async () => {
  mount()
  expect(await screen.findByText('nav.supplies')).toBeInTheDocument()
})

it('recovers from an error without changing hook order', async () => {
  vi.mocked(api.supplies.types).mockRejectedValueOnce(new Error('Inventory unavailable'))
  mount()
  expect(await screen.findByText('Inventory unavailable')).toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'error.retry' }))
  expect(await screen.findByText('nav.supplies')).toBeInTheDocument()
})
