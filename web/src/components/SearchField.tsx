/** The list search box: every word typed must appear somewhere in the row (see utils/search). */
export function SearchField({ value, onChange, placeholder }: { value: string; onChange: (value: string) => void; placeholder: string }) {
  return (
    <div style={{ marginBottom: 16 }}>
      <input
        type="search"
        className="search-field"
        aria-label={placeholder}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        style={{
          width: '100%',
          padding: '8px 14px',
          fontFamily: 'var(--font-mono)',
          fontSize: 12,
          letterSpacing: 0.8,
          border: '1px solid var(--color-ink)',
          background: 'transparent',
          color: 'var(--color-ink)',
        }}
      />
    </div>
  )
}
