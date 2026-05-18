export const CATEGORIES = ['SOIL', 'POT', 'FERTILIZER', 'TRAY', 'LABEL', 'OTHER'] as const
export const UNITS = ['COUNT', 'LITERS', 'KILOGRAMS', 'GRAMS', 'METERS', 'PACKETS'] as const

export const UNITS_BY_CATEGORY: Record<string, string[]> = {
  SOIL: ['LITERS', 'KILOGRAMS', 'PACKETS'],
  POT: ['COUNT'],
  FERTILIZER: ['KILOGRAMS', 'GRAMS', 'LITERS', 'PACKETS'],
  TRAY: ['COUNT'],
  LABEL: ['COUNT', 'PACKETS'],

  OTHER: [...UNITS],
}

export const DEFAULT_UNIT: Record<string, string> = {
  SOIL: 'LITERS',
  POT: 'COUNT',
  FERTILIZER: 'KILOGRAMS',
  TRAY: 'COUNT',
  LABEL: 'COUNT',

  OTHER: 'COUNT',
}

// Grid template: type name | total quantity | edit pencil
export const TYPE_TEMPLATE = '1fr 140px 36px'

export const headerStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: TYPE_TEMPLATE,
  gap: 18,
  padding: '10px 0',
  borderBottom: '1px solid var(--color-ink)',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  alignItems: 'center',
}
