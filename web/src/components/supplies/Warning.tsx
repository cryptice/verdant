export function Warning({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 4 }}>
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-accent)' }}>
        ⚠
      </span>
      <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 13, color: 'var(--color-accent)' }}>
        {message}
      </span>
    </div>
  )
}
