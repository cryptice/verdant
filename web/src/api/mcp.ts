import { apiRequest } from './client'

export interface McpAuthorization { clientName: string; callbackOrigin: string; scope: string }
export interface McpConnection { id: string; clientName: string; organizationName: string; createdAt: string; expiresAt: string }
export const mcpApi = {
  request: (id: string) => apiRequest<McpAuthorization>(`/api/mcp/oauth/requests/${id}`),
  approve: (id: string, orgId: number) => apiRequest<{ redirectUrl: string }>(`/api/mcp/oauth/requests/${id}/approve`, { method: 'POST', body: JSON.stringify({ orgId }) }),
  deny: (id: string) => apiRequest<{ redirectUrl: string }>(`/api/mcp/oauth/requests/${id}/deny`, { method: 'POST' }),
  connections: () => apiRequest<McpConnection[]>('/api/mcp/oauth/connections'),
  revoke: (id: string) => apiRequest<void>(`/api/mcp/oauth/connections/${id}`, { method: 'DELETE' }),
}
