import { describe, expect, it } from 'vitest'
import { loginReturnTo, mcpLoginUrl } from './loginReturnTo'

describe('MCP login return', () => {
  const path = '/mcp/authorize?request=12345678-1234-1234-1234-123456789abc'
  it('preserves consent through login', () => {
    const login = mcpLoginUrl('/mcp/authorize', path.substring(path.indexOf('?')))
    expect(loginReturnTo(login.substring(login.indexOf('?')))).toBe(path)
  })
  it.each(['https://attacker.test', '//attacker.test', '/tasks', `${path}&returnTo=https://attacker.test`, '/mcp/authorize?request=invalid'])('rejects arbitrary destinations %s', value => {
    expect(loginReturnTo(`?returnTo=${encodeURIComponent(value)}`)).toBe('/')
  })
})
