import { apiRequest } from './client'

export interface ApiKey { id: string; name: string; organizationId: number; createdAt: string; expiresAt: string; lastUsedAt?: string | null }
export interface CreatedApiKey { key: ApiKey; token: string }
export const apiKeysApi = {
  list: () => apiRequest<ApiKey[]>('/api/api-keys'),
  create: (name: string, validDays: number) =>
    apiRequest<CreatedApiKey>('/api/api-keys', { method: 'POST', body: JSON.stringify({ name, validDays }) }),
  revoke: (id: string) => apiRequest<void>(`/api/api-keys/${id}`, { method: 'DELETE' }),
}
