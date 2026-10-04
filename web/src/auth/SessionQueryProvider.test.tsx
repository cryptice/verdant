import { StrictMode } from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { useQuery } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'
import { OrgProvider, useOrg } from './OrgContext'
import { SessionQueryProvider } from './SessionQueryProvider'
import { api, type UserResponse } from '../api/client'

vi.mock('../api/client', async (original) => {
  const actual = await original<typeof import('../api/client')>()
  return { ...actual, api: { ...actual.api, user: { ...actual.api.user, me: vi.fn() } } }
})

const alice: UserResponse = {
  id: 1, email: 'alice@example.com', displayName: 'Alice', role: 'USER',
  advancedMode: false, createdAt: '2026-01-01T00:00:00Z',
  organizations: [
    { orgId: 10, orgName: 'Garden A', role: 'OWNER' },
    { orgId: 20, orgName: 'Garden B', role: 'OWNER' },
  ],
}
const bob: UserResponse = { ...alice, id: 2, displayName: 'Bob', organizations: [alice.organizations[1]] }
const loadDashboard = vi.fn<() => Promise<string>>()

function Controls() {
  const { login, logout, user } = useAuth()
  const { switchOrg } = useOrg()
  return <>
    <span data-testid="user">{user?.displayName ?? 'guest'}</span>
    <button onClick={() => login('alice', alice)}>Alice</button>
    <button onClick={() => login('bob', bob)}>Bob</button>
    <button onClick={logout}>Logout</button>
    <button onClick={() => switchOrg(20)}>Garden B</button>
  </>
}

function Dashboard() {
  const { user } = useAuth()
  const { data } = useQuery({ queryKey: ['dashboard'], queryFn: loadDashboard, enabled: !!user })
  return <span data-testid="dashboard">{data ?? 'empty'}</span>
}

function mount() {
  render(<StrictMode><MemoryRouter><AuthProvider><OrgProvider>
    <Controls />
    <SessionQueryProvider><Dashboard /></SessionQueryProvider>
  </OrgProvider></AuthProvider></MemoryRouter></StrictMode>)
}

beforeEach(() => {
  localStorage.clear()
  vi.resetAllMocks()
  vi.mocked(api.user.me).mockImplementation(async () => localStorage.getItem('verdant_token') === 'bob' ? bob : alice)
  loadDashboard.mockImplementation(async () => `${localStorage.getItem('verdant_token')}:${localStorage.getItem('verdant_org_id')}`)
})

it('never reuses a previous account dashboard after logout and login', async () => {
  mount()
  fireEvent.click(screen.getByText('Alice'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('alice:10'))
  fireEvent.click(screen.getByText('Logout'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('empty'))
  expect(localStorage.getItem('verdant_org_id')).toBeNull()
  fireEvent.click(screen.getByText('Bob'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('bob:20'))
})

it('switches organization headers and cache before requesting the new dashboard', async () => {
  mount()
  fireEvent.click(screen.getByText('Alice'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('alice:10'))
  fireEvent.click(screen.getByText('Garden B'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('alice:20'))
})

it('discards late query results from the previous organization', async () => {
  let resolveOld!: (value: string) => void
  const old = new Promise<string>(resolve => { resolveOld = resolve })
  loadDashboard.mockImplementation(() => localStorage.getItem('verdant_org_id') === '10' ? old : Promise.resolve('new garden'))
  mount()
  fireEvent.click(screen.getByText('Alice'))
  await waitFor(() => expect(loadDashboard).toHaveBeenCalled())
  fireEvent.click(screen.getByText('Garden B'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('new garden'))
  await act(async () => resolveOld('old garden'))
  expect(screen.getByTestId('dashboard')).toHaveTextContent('new garden')
})

it('ignores a previous login profile response after changing accounts', async () => {
  let resolveOld!: (value: UserResponse) => void
  const old = new Promise<UserResponse>(resolve => { resolveOld = resolve })
  vi.mocked(api.user.me).mockImplementation(() => localStorage.getItem('verdant_token') === 'alice' ? old : Promise.resolve(bob))
  mount()
  fireEvent.click(screen.getByText('Alice'))
  await waitFor(() => expect(api.user.me).toHaveBeenCalled())
  fireEvent.click(screen.getByText('Bob'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('bob:20'))
  await act(async () => resolveOld(alice))
  expect(screen.getByTestId('user')).toHaveTextContent('Bob')
})

it('refreshes the session and organization when another tab changes storage', async () => {
  mount()
  fireEvent.click(screen.getByText('Alice'))
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('alice:10'))
  act(() => {
    localStorage.setItem('verdant_org_id', '20')
    window.dispatchEvent(new StorageEvent('storage', { key: 'verdant_org_id' }))
  })
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('alice:20'))
  act(() => {
    localStorage.setItem('verdant_token', 'bob')
    window.dispatchEvent(new StorageEvent('storage', { key: 'verdant_token' }))
  })
  await waitFor(() => expect(screen.getByTestId('dashboard')).toHaveTextContent('bob:20'))
  expect(screen.getByTestId('user')).toHaveTextContent('Bob')
})
