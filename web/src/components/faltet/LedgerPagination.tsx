import { useTranslation } from 'react-i18next'
import { Rule } from './Rule'

export type LedgerPaginationProps = {
  page: number
  pageSize: number
  total: number
  onChange: (nextPage: number) => void
}

export function LedgerPagination({ page, pageSize, total, onChange }: LedgerPaginationProps) {
  const { t } = useTranslation()
  if (total === 0) return null

  const lastPage = Math.max(0, Math.ceil(total / pageSize) - 1)
  const from = page * pageSize + 1
  const to = Math.min((page + 1) * pageSize, total)
  const atStart = page === 0
  const atEnd = page >= lastPage

  const btn = (disabled: boolean): React.CSSProperties => ({
    background: 'transparent',
    border: 'none',
    color: 'var(--color-clay)',
    fontFamily: 'var(--font-mono)',
    fontSize: 14,
    cursor: disabled ? 'default' : 'pointer',
    opacity: disabled ? 0.3 : 1,
    padding: '4px 8px',
  })

  return (
    <div>
      <Rule variant="soft" />
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 18, padding: '14px 0' }}>
        <button
          type="button"
          onClick={() => onChange(Math.max(0, page - 1))}
          disabled={atStart}
          style={btn(atStart)}
          aria-label={t('pagination.previous')}
        >←</button>
        <span
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
          }}
        >
          {from}–{to} {t('pagination.of')} {total}
        </span>
        <button
          type="button"
          onClick={() => onChange(Math.min(lastPage, page + 1))}
          disabled={atEnd}
          style={btn(atEnd)}
          aria-label={t('pagination.next')}
        >→</button>
      </div>
    </div>
  )
}
