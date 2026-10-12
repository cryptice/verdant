import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { McpAuthorizePage } from './McpAuthorizePage'
import { mcpApi } from '../api/mcp'

vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ token: 'app-token', loading: false, user: { organizations: [{ orgId: 1, orgName: 'Brukets blommor' }, { orgId: 2, orgName: 'Other organization' }] } }) }))
vi.mock('react-i18next', () => ({ useTranslation: () => ({ i18n: { language: 'sv' } }) }))
vi.mock('../api/mcp', () => ({ mcpApi: { request: vi.fn(), approve: vi.fn(), deny: vi.fn() } }))
const id = '12345678-1234-1234-1234-123456789abc'
function page() {
  return render(<MemoryRouter initialEntries={[`/mcp/authorize?request=${id}`]}><QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><McpAuthorizePage /></QueryClientProvider></MemoryRouter>)
}
beforeEach(() => {
  vi.clearAllMocks()
  vi.mocked(mcpApi.request).mockResolvedValue({ clientName: 'Test AI client', callbackOrigin: 'https://chatgpt.com', scope: 'verdant:read' })
  vi.mocked(mcpApi.approve).mockRejectedValue(new Error('Try again'))
  vi.mocked(mcpApi.deny).mockRejectedValue(new Error('Try again'))
})
describe('MCP explicit organization consent', () => {
  it('shows client and callback identity without approving automatically', async () => {
    page()
    await screen.findByText('Test AI client')
    expect(screen.getByText('https://chatgpt.com')).toBeInTheDocument()
    expect(mcpApi.approve).not.toHaveBeenCalled()
    expect(mcpApi.deny).not.toHaveBeenCalled()
  })
  it('approves only the selected organization after a click', async () => {
    page()
    await screen.findByText('Test AI client')
    fireEvent.change(screen.getByLabelText('Organisation'), { target: { value: '2' } })
    fireEvent.click(screen.getByRole('button', { name: 'Tillåt läsåtkomst' }))
    await waitFor(() => expect(mcpApi.approve).toHaveBeenCalledWith(id, 2))
    expect(await screen.findByRole('alert')).toHaveTextContent('Try again')
  })
  it('allows denying the request', async () => {
    page()
    await screen.findByText('Test AI client')
    fireEvent.click(screen.getByRole('button', { name: 'Avbryt' }))
    await waitFor(() => expect(mcpApi.deny).toHaveBeenCalledWith(id))
    expect(mcpApi.approve).not.toHaveBeenCalled()
  })
})
