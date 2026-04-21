// web/src/components/faltet/LedgerFilters.tsx
import { useEffect } from 'react'

export type LedgerFilterOption<Id extends string> = {
  id: Id
  label: string
  tone?: 'clay' | 'mustard' | 'berry' | 'sky' | 'sage' | 'forest'
}

const TONE_VAR: Record<string, string> = {
  clay: 'var(--color-clay)', mustard: 'var(--color-mustard)', berry: 'var(--color-berry)',
  sky: 'var(--color-sky)', sage: 'var(--color-sage)', forest: 'var(--color-forest)',
}

type Props<Id extends string> = {
  options: LedgerFilterOption<Id>[]
  value: Set<Id>
  onChange: (next: Set<Id>) => void
  atLeastOne?: boolean
  storageKey?: string
}

export function LedgerFilters<Id extends string>({
  options, value, onChange, atLeastOne = true, storageKey,
}: Props<Id>) {
  // Hydrate from localStorage on first mount
  useEffect(() => {
    if (!storageKey) return
    const raw = localStorage.getItem(storageKey)
    if (!raw) return
    try {
      const parsed = JSON.parse(raw) as Id[]
      if (Array.isArray(parsed)) onChange(new Set(parsed))
    } catch { /* ignore */ }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Persist on change
  useEffect(() => {
    if (storageKey) localStorage.setItem(storageKey, JSON.stringify(Array.from(value)))
  }, [value, storageKey])

  const toggle = (id: Id) => {
    const has = value.has(id)
    if (has && atLeastOne && value.size === 1) return // no-op: keep at least one active
    const next = new Set(value)
    has ? next.delete(id) : next.add(id)
    onChange(next)
  }

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 22 }}>
      {options.map((opt) => {
        const active = value.has(opt.id)
        const color = TONE_VAR[opt.tone ?? 'forest']
        return (
          <button
            key={opt.id}
            onClick={() => toggle(opt.id)}
            style={{
              fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4,
              textTransform: 'uppercase',
              padding: '6px 12px', borderRadius: 999,
              border: `1px solid ${color}`,
              background: active ? color : 'transparent',
              color: active ? 'var(--color-cream)' : color,
              cursor: 'pointer',
            }}
          >
            {opt.label}
          </button>
        )
      })}
    </div>
  )
}
