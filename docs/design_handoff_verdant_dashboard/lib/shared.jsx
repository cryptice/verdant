// Shared placeholder + utility components for the Verdant dashboard directions.
// Kept small; each direction defines its own visual vocabulary.

function PhotoPlaceholder({ label, tone = 'sage', style = {}, rotate = 0 }) {
  const tones = {
    sage:   { a: '#c7d8c4', b: '#b4c9b2', text: '#3a4a38' },
    cream:  { a: '#ede4d0', b: '#e0d4bb', text: '#5a4a2a' },
    peach:  { a: '#f0d4c1', b: '#e5c0a7', text: '#6b4430' },
    ink:    { a: '#2e3630', b: '#252b26', text: '#c7d8c4' },
    butter: { a: '#f1e4b0', b: '#e5d491', text: '#6b5a1e' },
    rose:   { a: '#e9c8c8', b: '#d9b0b0', text: '#5a2e2e' },
  };
  const t = tones[tone] || tones.sage;
  return (
    <div style={{
      background: `repeating-linear-gradient(135deg, ${t.a} 0, ${t.a} 6px, ${t.b} 6px, ${t.b} 12px)`,
      color: t.text,
      fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
      fontSize: 10,
      letterSpacing: 0.4,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      textAlign: 'center',
      padding: 8,
      textTransform: 'uppercase',
      transform: rotate ? `rotate(${rotate}deg)` : undefined,
      ...style,
    }}>
      {label}
    </div>
  );
}

window.PhotoPlaceholder = PhotoPlaceholder;
