export function BedSectionHeader({
  title,
  meta,
  actions,
}: {
  title: string
  meta?: string
  actions?: React.ReactNode
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, margin: '40px 0 12px' }}>
      <h2
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 30,
          fontWeight: 300,
          margin: 0,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {title}
        <span style={{ color: 'var(--color-accent)' }}>.</span>
      </h2>
      {meta && (
        <span
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
          }}
        >
          {meta}
        </span>
      )}
      {actions && <div style={{ marginLeft: 'auto' }}>{actions}</div>}
    </div>
  )
}
