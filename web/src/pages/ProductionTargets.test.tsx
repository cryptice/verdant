import { afterEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom'
import { ProductionTargets } from './ProductionTargets'
import { api, type SeasonResponse } from '../api/client'

vi.mock('react-i18next', () => ({ useTranslation: () => ({ t: (key: string) => key }) }))
vi.mock('../onboarding/OnboardingContext', () => ({ useOnboarding: () => ({ completeStep: vi.fn() }) }))
vi.mock('../onboarding/OnboardingHint', () => ({ OnboardingHint: () => null }))
vi.mock('./HarvestPlans', () => ({ HarvestPlans: () => <p>Targets content</p> }))
afterEach(() => vi.restoreAllMocks())

function Seasons() {
  const navigate = useNavigate()
  return <><h1>Seasons page</h1><button onClick={() => navigate(-1)}>Back</button></>
}
function renderPage() {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  render(<QueryClientProvider client={qc}><MemoryRouter initialEntries={['/', '/targets']}>
    <Routes>
      <Route path="/" element={<h1>Dashboard</h1>} />
      <Route path="/targets" element={<ProductionTargets />} />
      <Route path="/seasons" element={<Seasons />} />
    </Routes>
  </MemoryRouter></QueryClientProvider>)
}

describe('Targets season prerequisite', () => {
  it('redirects to Seasons and replaces Targets in history when no seasons exist', async () => {
    vi.spyOn(api.seasons, 'list').mockResolvedValue([])
    renderPage()
    await screen.findByRole('heading', { name: 'Seasons page' })
    expect(screen.queryByText('Targets content')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Back' }))
    await screen.findByRole('heading', { name: 'Dashboard' })
  })

  it('waits for seasons to load and allows even an inactive season', async () => {
    let resolve!: (seasons: SeasonResponse[]) => void
    vi.spyOn(api.seasons, 'list').mockImplementation(() => new Promise(done => { resolve = done }))
    renderPage()
    expect(screen.getByText('planning.loading')).toBeInTheDocument()
    expect(screen.queryByText('Seasons page')).not.toBeInTheDocument()
    resolve([{ id: 1, name: '2026', year: 2026, isActive: false, createdAt: '', updatedAt: '' }])
    await screen.findByText('Targets content')
    expect(screen.queryByText('Seasons page')).not.toBeInTheDocument()
  })

  it('preselects the highest-year season for each new weekly target', async () => {
    vi.spyOn(api.seasons, 'list').mockResolvedValue([
      { id: 99, name: 'Older active', year: 2026, isActive: true, createdAt: '', updatedAt: '' },
      { id: 2, name: 'Latest', year: 2028, isActive: false, createdAt: '', updatedAt: '' },
      { id: 7, name: 'Middle', year: 2027, isActive: false, createdAt: '', updatedAt: '' },
    ])
    vi.spyOn(api.productionTargets, 'list').mockResolvedValue([])
    renderPage()
    fireEvent.click(await screen.findByRole('button', { name: 'planning.weekly' }))
    fireEvent.click(await screen.findByRole('button', { name: 'targets.new' }))
    const dialog = within(screen.getByRole('dialog'))
    expect(dialog.getByRole('combobox')).toHaveValue('2')
    fireEvent.change(dialog.getByRole('combobox'), { target: { value: '99' } })
    expect(dialog.getByRole('combobox')).toHaveValue('99')
    fireEvent.click(dialog.getByRole('button', { name: 'common.cancel' }))
    fireEvent.click(screen.getByRole('button', { name: 'targets.new' }))
    expect(within(screen.getByRole('dialog')).getByRole('combobox')).toHaveValue('2')
  })

  it('shows a failed lookup with retry instead of treating it as no seasons', async () => {
    vi.spyOn(api.seasons, 'list').mockRejectedValueOnce(new Error('Offline')).mockResolvedValue([])
    renderPage()
    await screen.findByText('Offline')
    expect(screen.queryByText('Seasons page')).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'error.retry' }))
    await screen.findByRole('heading', { name: 'Seasons page' })
  })
})
