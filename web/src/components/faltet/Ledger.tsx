// web/src/components/faltet/Ledger.tsx
import { useEffect, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { LedgerPagination } from './LedgerPagination'

export type LedgerColumn<T> = {
  key: string
  label: string
  width?: string
  align?: 'left' | 'right'
  render?: (row: T, index: number) => ReactNode
}

type Props<T> = {
  columns: LedgerColumn<T>[]
  rows: T[]
  rowKey: (row: T) => string | number
  onRowClick?: (row: T) => void
  emptyMessage?: string
  sectionHeaders?: (row: T, index: number, prev: T | null) => ReactNode | null
  paginated?: boolean
  pageSize?: number
}

export function Ledger<T>({
  columns, rows, rowKey, onRowClick, emptyMessage, sectionHeaders,
  paginated = false, pageSize = 50,
}: Props<T>) {
  const { t } = useTranslation()
  const [page, setPage] = useState(0)

  // Reset page when row identity-set changes (filter/search/data refetch).
  const rowKeysFingerprint = rows.map(rowKey).join('|')
  useEffect(() => { setPage(0) }, [rowKeysFingerprint])

  const template = columns.map((c) => c.width ?? '1fr').join(' ')

  if (rows.length === 0) {
    return (
      <div
        style={{
          padding: '40px 22px',
          textAlign: 'center',
          borderBottom: '1px solid var(--color-ink)',
          borderTop: '1px solid var(--color-ink)',
        }}
      >
        <div
          style={{
            fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
            textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 6,
          }}
        >
          {emptyMessage ?? t('common.ledger.empty')}
        </div>
      </div>
    )
  }

  const visibleRows = paginated
    ? rows.slice(page * pageSize, (page + 1) * pageSize)
    : rows

  return (
    <div>
      {/* Header */}
      <div
        style={{
          display: 'grid', gridTemplateColumns: template, gap: 18,
          padding: '10px 0', borderBottom: '1px solid var(--color-ink)',
        }}
      >
        {columns.map((col) => (
          <div
            key={col.key}
            style={{
              fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
              textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
              textAlign: col.align === 'right' ? 'right' : 'left',
            }}
          >
            {col.label}
          </div>
        ))}
      </div>

      {/* Body */}
      {visibleRows.map((row, i) => {
        const globalIndex = paginated ? page * pageSize + i : i
        const prevRow = globalIndex > 0 ? rows[globalIndex - 1] : null
        // When paginated, force a section header on the first row of each page
        // even if the preceding row (off-page) shares the same group.
        const prevForHeader = paginated && i === 0 ? null : prevRow
        const sectionNode = sectionHeaders?.(row, globalIndex, prevForHeader)

        const rowStyle: React.CSSProperties = {
          display: 'grid', gridTemplateColumns: template, gap: 18,
          padding: '12px 0',
          borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
          alignItems: 'center',
          textAlign: 'left',
          background: 'transparent',
          cursor: onRowClick ? 'pointer' : 'default',
          width: '100%',
        }
        const RowComponent = onRowClick ? 'button' : 'div'

        return (
          <div key={rowKey(row)}>
            {sectionNode}
            <RowComponent
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              style={{
                ...rowStyle,
                borderWidth: 0,
                borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              }}
              className={onRowClick ? 'ledger-row' : undefined}
            >
              {columns.map((col) => (
                <div
                  key={col.key}
                  style={{
                    textAlign: col.align === 'right' ? 'right' : 'left',
                    fontFamily: 'var(--font-display)', fontSize: 16, fontWeight: 300,
                    color: 'var(--color-ink)',
                  }}
                >
                  {col.render ? col.render(row, globalIndex) : String((row as any)[col.key] ?? '')}
                </div>
              ))}
            </RowComponent>
          </div>
        )
      })}

      {paginated && (
        <LedgerPagination
          page={page}
          pageSize={pageSize}
          total={rows.length}
          onChange={setPage}
        />
      )}
    </div>
  )
}
