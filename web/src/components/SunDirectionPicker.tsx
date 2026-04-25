import { useTranslation } from 'react-i18next'

export const COMPASS_DIRECTIONS = ['N', 'NE', 'E', 'SE', 'S', 'SW', 'W', 'NW'] as const
export type CompassDirection = typeof COMPASS_DIRECTIONS[number]

export function SunDirectionPicker({
  value,
  onChange,
}: {
  value: string[]
  onChange: (next: string[]) => void
}) {
  const { t } = useTranslation()
  const toggle = (d: string) =>
    onChange(value.includes(d) ? value.filter((x) => x !== d) : [...value, d])

  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
      {COMPASS_DIRECTIONS.map((d) => {
        const active = value.includes(d)
        return (
          <button
            key={d}
            type="button"
            onClick={() => toggle(d)}
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 11,
              letterSpacing: 1.4,
              padding: '6px 12px',
              borderRadius: 999,
              border: `1px solid ${active ? 'var(--color-accent)' : 'color-mix(in srgb, var(--color-ink) 30%, transparent)'}`,
              background: active ? 'var(--color-accent)' : 'transparent',
              color: active ? 'var(--color-cream)' : 'var(--color-ink)',
              cursor: 'pointer',
            }}
          >
            {t(`bed.conditions.compass.${d}`)}
          </button>
        )
      })}
    </div>
  )
}
