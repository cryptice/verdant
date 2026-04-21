import type { ReactNode } from 'react'

export function Masthead({
  left,
  center,
  right,
}: {
  left: ReactNode
  center?: ReactNode
  right?: ReactNode
}) {
  return (
    <div
      style={{
        display: 'grid',
        gridTemplateColumns: '1fr auto 1fr',
        alignItems: 'center',
        padding: '14px 22px',
        background: 'var(--color-cream)',
        borderBottom: '1px solid var(--color-ink)',
      }}
    >
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          letterSpacing: 1.8,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
        }}
      >
        {left}
      </div>
      <div
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 14,
          color: 'var(--color-forest)',
          textAlign: 'center',
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {center}
      </div>
      <div style={{ textAlign: 'right' }}>{right}</div>
    </div>
  )
}
