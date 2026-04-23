type Hue = 'sage' | 'clay' | 'mustard' | 'sky' | 'berry'
type Size = 'large' | 'medium' | 'small'

const VALUE_SIZE: Record<Size, number> = { large: 88, medium: 56, small: 32 }
const UNIT_SIZE: Record<Size, number> = { large: 28, medium: 18, small: 14 }
const HUE_VAR: Record<Hue, string> = {
  sage:    'var(--color-sage)',
  clay:    'var(--color-clay)',
  mustard: 'var(--color-mustard)',
  sky:     'var(--color-sky)',
  berry:   'var(--color-berry)',
}

export function Stat({
  value,
  unit,
  label,
  delta,
  hue = 'sage',
  size = 'large',
}: {
  value: number | string
  unit?: string
  label: string
  delta?: string
  hue?: Hue
  size?: Size
}) {
  const hueVar = HUE_VAR[hue]
  return (
    <div>
      <div
        style={{
          fontFamily: 'var(--font-display)',
          fontSize: VALUE_SIZE[size],
          lineHeight: 0.95,
          fontWeight: 300,
          letterSpacing: -1.2,
          color: 'var(--color-ink)',
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {value}
        {unit && (
          <span style={{ fontSize: UNIT_SIZE[size], marginLeft: 4, color: hueVar, fontStyle: 'italic' }}>
            {unit}
          </span>
        )}
      </div>
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 11,
          letterSpacing: 1.8,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          marginTop: 10,
          display: 'flex',
          gap: 10,
          alignItems: 'center',
        }}
      >
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: 6 }}>
          <span style={{ width: 6, height: 6, borderRadius: 999, background: hueVar }} />
          {label}
        </span>
        {delta && <span style={{ color: 'var(--color-accent)' }}>▲ {delta}</span>}
      </div>
    </div>
  )
}
