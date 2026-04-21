type Accent = 'clay' | 'mustard' | 'sage' | 'sky' | 'berry'

const ACCENT_VAR: Record<Accent, string> = {
  clay:    'var(--color-clay)',
  mustard: 'var(--color-mustard)',
  sage:    'var(--color-sage)',
  sky:     'var(--color-sky)',
  berry:   'var(--color-berry)',
}

type Props = {
  label: string
  value?: string
  accent?: Accent
} & (
  | { editable?: false; onChange?: never }
  | { editable: true; onChange: (v: string) => void; placeholder?: string }
)

export function Field(props: Props) {
  const color = props.accent ? ACCENT_VAR[props.accent] : 'var(--color-ink)'
  return (
    <label style={{ display: 'block' }}>
      <span
        style={{
          display: 'block',
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
          marginBottom: 4,
        }}
      >
        {props.label}
      </span>
      {props.editable ? (
        <input
          value={props.value ?? ''}
          onChange={(e) => props.onChange(e.target.value)}
          placeholder={'placeholder' in props ? props.placeholder : undefined}
          style={{
            display: 'block',
            width: '100%',
            background: 'transparent',
            border: 'none',
            borderBottom: '1px solid var(--color-ink)',
            borderRadius: 0,
            padding: '4px 0',
            fontFamily: 'var(--font-display)',
            fontSize: 20,
            fontWeight: 300,
            color,
            outline: 'none',
          }}
        />
      ) : (
        <div
          style={{
            borderBottom: '1px solid var(--color-ink)',
            padding: '4px 0',
            fontFamily: 'var(--font-display)',
            fontSize: 20,
            fontWeight: 300,
            color,
          }}
        >
          {props.value ?? '—'}
        </div>
      )}
    </label>
  )
}
