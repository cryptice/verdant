export function BedMetaCell({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        padding: '10px 14px',
        borderTop: '1px solid var(--color-ink)',
        borderLeft: '1px solid var(--color-ink)',
      }}
    >
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
        }}
      >
        {label}
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 22 }}>{value}</div>
    </div>
  )
}
