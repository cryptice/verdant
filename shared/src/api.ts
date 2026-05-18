// Shared between admin/ and web/. The two apps had drifted copies of the
// same fetch wrapper, token retrieval, and 401 redirect logic. Keep this
// surface minimal — only the bits that are genuinely identical belong here.
// Anything that differs (token storage key, network-error wording) flows in
// via ApiClientConfig.

export class ApiError extends Error {
  status?: number
  isNetworkError: boolean

  constructor(message: string, status?: number, isNetworkError = false) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.isNetworkError = isNetworkError
  }
}

export interface ApiClientConfig {
  getToken: () => string | null
  getOrgId?: () => string | null
  onUnauthorized: () => void
  baseUrl?: string
  networkErrorMessage?: string
  treat403AsUnauthorized?: boolean
}

export type ApiRequest = <T>(path: string, options?: RequestInit) => Promise<T>

export function makeApiRequest(config: ApiClientConfig): ApiRequest {
  const base = config.baseUrl ?? ''
  const networkMsg =
    config.networkErrorMessage ?? 'Network error — check your connection'

  return async function apiRequest<T>(path: string, options?: RequestInit): Promise<T> {
    const headers: Record<string, string> = { 'Content-Type': 'application/json' }
    const token = config.getToken()
    if (token) headers['Authorization'] = `Bearer ${token}`
    const orgId = config.getOrgId?.()
    if (orgId) headers['X-Organization-Id'] = orgId

    let response: Response
    try {
      response = await fetch(`${base}${path}`, {
        ...options,
        headers: { ...headers, ...(options?.headers as Record<string, string> | undefined) },
      })
    } catch {
      throw new ApiError(networkMsg, undefined, true)
    }

    const isAuthFailure =
      response.status === 401 || (config.treat403AsUnauthorized && response.status === 403)
    if (isAuthFailure) {
      config.onUnauthorized()
      throw new ApiError('Unauthorized', response.status)
    }

    if (!response.ok) {
      const body = await response.text()
      let message = `Request failed (${response.status})`
      try {
        const parsed = JSON.parse(body)
        if (parsed && typeof parsed.message === 'string') message = parsed.message
      } catch {
        // body wasn't JSON — keep the default message
      }
      throw new ApiError(message, response.status)
    }

    if (response.status === 204) return undefined as T
    return response.json()
  }
}
