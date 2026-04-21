type Tone = 'sage' | 'blush' | 'butter'
type Aspect = 'wide' | 'tall' | 'square'

const TONE_RGB: Record<Tone, string> = {
  sage:   '107, 143, 106',
  blush:  '233, 184, 168',
  butter: '242, 210, 122',
}

const ASPECT_PADDING: Record<Aspect, string> = {
  wide:   '56.25%', // 16:9
  tall:   '140%',   // portrait
  square: '100%',
}

export function PhotoPlaceholder({
  tone = 'sage',
  label,
  aspect = 'wide',
}: {
  tone?: Tone
  label: string
  aspect?: Aspect
}) {
  const rgb = TONE_RGB[tone]
  return (
    <div
      style={{
        position: 'relative',
        width: '100%',
        paddingTop: ASPECT_PADDING[aspect],
        background: `radial-gradient(ellipse at 30% 30%, rgba(${rgb},0.35), rgba(${rgb},0.12) 60%, var(--color-cream))`,
        border: '1px solid var(--color-ink)',
      }}
    >
      <span
        style={{
          position: 'absolute',
          left: 12,
          bottom: 12,
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
        }}
      >
        {label}
      </span>
    </div>
  )
}
