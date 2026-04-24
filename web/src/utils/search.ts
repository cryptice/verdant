// Shared client-side search helper — splits a free-form query on whitespace
// and requires every token to appear (substring, case-insensitive) somewhere
// in the concatenated haystacks. Matches the server-side species search so
// "da cor" hits "Dahlia — Cornel" client-side too.

export function tokenize(query: string): string[] {
  return query.trim().toLowerCase().split(/\s+/).filter(Boolean)
}

export function matchesAllTokens(
  haystacks: Array<string | null | undefined>,
  query: string,
): boolean {
  const tokens = tokenize(query)
  if (tokens.length === 0) return true
  const combined = haystacks
    .filter((h): h is string => typeof h === 'string' && h.length > 0)
    .join(' ')
    .toLowerCase()
  return tokens.every((tok) => combined.includes(tok))
}
