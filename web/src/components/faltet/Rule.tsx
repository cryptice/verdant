type Variant = 'ink' | 'soft'

export function Rule({ variant = 'ink', inline = false }: { variant?: Variant; inline?: boolean }) {
  const borderColor =
    variant === 'ink'
      ? 'var(--color-ink)'
      : 'color-mix(in srgb, var(--color-ink) 20%, transparent)'
  if (inline) {
    return <span style={{ flex: 1, height: 1, borderTop: `1px solid ${borderColor}` }} />
  }
  return <hr style={{ border: 0, borderTop: `1px solid ${borderColor}`, margin: 0 }} />
}
