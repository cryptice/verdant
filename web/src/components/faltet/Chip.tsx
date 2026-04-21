import type { ReactNode } from 'react'

type Tone = 'clay' | 'mustard' | 'berry' | 'sky' | 'sage' | 'forest'

const TONE_VAR: Record<Tone, string> = {
  clay:    'var(--color-clay)',
  mustard: 'var(--color-mustard)',
  berry:   'var(--color-berry)',
  sky:     'var(--color-sky)',
  sage:    'var(--color-sage)',
  forest:  'var(--color-forest)',
}

export function Chip({ tone = 'forest', children }: { tone?: Tone; children: ReactNode }) {
  const color = TONE_VAR[tone]
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 6,
        fontFamily: 'var(--font-mono)',
        fontSize: 10,
        letterSpacing: 1.4,
        textTransform: 'uppercase',
        color,
        padding: '4px 8px',
        border: `1px solid ${color}`,
        borderRadius: 999,
      }}
    >
      {children}
    </span>
  )
}
