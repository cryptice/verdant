// Limit login returns to the consent page; never accept arbitrary redirect destinations.
export function loginReturnTo(search: string): string {
  const value = new URLSearchParams(search).get('returnTo')
  return value && /^\/mcp\/authorize\?request=[0-9a-f-]{36}$/i.test(value) ? value : '/'
}

export function mcpLoginUrl(path: string, search: string): string {
  const target = `${path}${search}`
  return loginReturnTo(`?returnTo=${encodeURIComponent(target)}`) === target
    ? `/login?returnTo=${encodeURIComponent(target)}` : '/login'
}
