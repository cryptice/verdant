import type { SupplyInventoryResponse, SeasonResponse } from '../../api/client'
import type { GroupedType } from '../../pages/supplies/utils'
import { TYPE_TEMPLATE } from '../../pages/supplies/constants'
import { formatUnit, formatCost } from '../../pages/supplies/utils'

export interface SupplyTypeRowProps {
  item: GroupedType
  isOpen: boolean
  showUsed: boolean
  setShowUsed: (v: boolean) => void
  seasons: SeasonResponse[]
  onToggleExpand: () => void
  onAddBatchToType: () => void
  onEditType: () => void
  onDecrementBatch: (batch: SupplyInventoryResponse) => void
  onEditBatch: (batch: SupplyInventoryResponse) => void
  onDeleteBatch: (batch: SupplyInventoryResponse) => void
  t: (key: string, opts?: Record<string, unknown>) => string
}

export function SupplyTypeRow({
  item, isOpen, showUsed, setShowUsed,
  seasons,
  onToggleExpand, onAddBatchToType, onEditType,
  onDecrementBatch, onEditBatch, onDeleteBatch,
  t,
}: SupplyTypeRowProps) {
  const visibleBatches = (showUsed ? item.batches : item.batches.filter(b => b.quantity > 0))
    .slice().sort((a, b) => (a.quantity === 0 ? 1 : 0) - (b.quantity === 0 ? 1 : 0))
  const hasUsed = item.batches.some(b => b.quantity === 0)

  return (
    <div>
      {/* Type row — click to expand */}
      <button
        onClick={onToggleExpand}
        style={{
          display: 'grid',
          gridTemplateColumns: TYPE_TEMPLATE,
          gap: 18,
          padding: '14px 0',
          borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
          width: '100%',
          background: 'transparent',
          border: 'none',
          textAlign: 'left',
          cursor: 'pointer',
          alignItems: 'center',
        }}
        className="ledger-row"
      >
        {/* Name + inline add */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
          <span style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            color: 'var(--color-forest)',
            opacity: 0.5,
            flexShrink: 0,
          }}>
            {isOpen ? '▼' : '▶'}
          </span>
          <span style={{
            fontFamily: 'var(--font-display)',
            fontSize: 18,
            fontWeight: 300,
            color: 'var(--color-ink)',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
          }}>
            {item.label}
          </span>
          <span
            role="button"
            onClick={e => {
              e.stopPropagation()
              onAddBatchToType()
            }}
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              letterSpacing: 1.2,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              flexShrink: 0,
            }}
          >
            {t('common.add')}
          </span>
        </div>

        {/* Total quantity (or obegränsad indicator) */}
        <div style={{
          fontFamily: 'var(--font-display)',
          fontSize: 16,
          fontVariantNumeric: 'tabular-nums',
          color: item.type.inexhaustible ? 'var(--color-accent)' : 'var(--color-ink)',
          textAlign: 'right',
        }}>
          {item.type.inexhaustible && item.batches.length === 0
            ? 'obegränsad'
            : item.type.inexhaustible
              ? `${formatUnit(item.totalQuantity, item.type.unit, t)} · obegränsad`
              : formatUnit(item.totalQuantity, item.type.unit, t)}
        </div>

        {/* Edit pencil */}
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <span
            role="button"
            onClick={e => {
              e.stopPropagation()
              onEditType()
            }}
            style={{
              color: 'var(--color-forest)',
              opacity: 0.5,
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
            }}
            aria-label={t('common.edit')}
          >
            <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9.5 2.5 L11.5 4.5 L4.5 11.5 L2 12 L2.5 9.5 Z" />
              <line x1="8" y1="4" x2="10" y2="6" />
            </svg>
          </span>
        </div>
      </button>

      {/* Expanded batch rows */}
      {isOpen && (
        <div style={{
          borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
        }}>
          {hasUsed && (
            <label style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8,
              padding: '8px 0 8px 32px',
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              letterSpacing: 1.2,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              cursor: 'pointer',
            }}>
              <input type="checkbox" checked={showUsed} onChange={e => setShowUsed(e.target.checked)} className="rounded" />
              {t('supplies.showUsed')}
            </label>
          )}

          {visibleBatches.length === 0 && (
            <p style={{
              padding: '12px 0 12px 32px',
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 13,
              color: 'var(--color-forest)',
              opacity: 0.6,
            }}>
              {t('supplies.noBatches')}
            </p>
          )}

          {visibleBatches.map(batch => {
            const seasonName = seasons.find(s => s.id === batch.seasonId)?.name
            return (
              <div
                key={batch.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '10px 0 10px 32px',
                  borderTop: '1px solid color-mix(in srgb, var(--color-ink) 12%, transparent)',
                }}
              >
                <div style={{
                  display: 'flex',
                  gap: 14,
                  alignItems: 'baseline',
                  flexWrap: 'wrap',
                }}>
                  <span style={{
                    fontFamily: 'var(--font-display)',
                    fontSize: 16,
                    fontVariantNumeric: 'tabular-nums',
                    color: 'var(--color-ink)',
                  }}>
                    {formatUnit(batch.quantity, batch.unit, t)}
                  </span>
                  {batch.costCents != null && (
                    <span style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      color: 'var(--color-forest)',
                      opacity: 0.7,
                    }}>
                      {formatCost(batch.costCents)}
                      {batch.quantity > 0 && ` (${formatCost(Math.round(batch.costCents / batch.quantity))}/${t(`supplyUnit.${batch.unit}`)})`}
                    </span>
                  )}
                  {seasonName && (
                    <span style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      color: 'var(--color-forest)',
                      opacity: 0.55,
                    }}>
                      {seasonName}
                    </span>
                  )}
                  {batch.notes && (
                    <span style={{
                      fontFamily: 'var(--font-display)',
                      fontStyle: 'italic',
                      fontSize: 13,
                      color: 'var(--color-forest)',
                      opacity: 0.65,
                    }}>
                      {batch.notes}
                    </span>
                  )}
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                  <button
                    onClick={() => onDecrementBatch(batch)}
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 9,
                      letterSpacing: 1.2,
                      textTransform: 'uppercase',
                      color: 'var(--color-accent)',
                      background: 'color-mix(in srgb, var(--color-accent) 10%, transparent)',
                      border: '1px solid color-mix(in srgb, var(--color-accent) 30%, transparent)',
                      borderRadius: 4,
                      padding: '4px 8px',
                      cursor: 'pointer',
                    }}
                  >
                    {t('supplies.decrement')}
                  </button>
                  <button
                    onClick={() => onEditBatch(batch)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      color: 'var(--color-forest)',
                      opacity: 0.5,
                      cursor: 'pointer',
                      padding: 2,
                      display: 'flex',
                      alignItems: 'center',
                    }}
                    aria-label={t('common.edit')}
                  >
                    <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M9.5 2.5 L11.5 4.5 L4.5 11.5 L2 12 L2.5 9.5 Z" />
                      <line x1="8" y1="4" x2="10" y2="6" />
                    </svg>
                  </button>
                  <button
                    onClick={() => onDeleteBatch(batch)}
                    style={{
                      background: 'transparent',
                      border: 'none',
                      color: 'var(--color-forest)',
                      opacity: 0.45,
                      cursor: 'pointer',
                      padding: 2,
                      display: 'flex',
                      alignItems: 'center',
                    }}
                    aria-label={t('common.delete')}
                  >
                    <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="2" y1="4" x2="12" y2="4" />
                      <path d="M5 4V2.5a.5.5 0 0 1 .5-.5h3a.5.5 0 0 1 .5.5V4" />
                      <path d="M3 4l.5 8.5a.5.5 0 0 0 .5.5h6a.5.5 0 0 0 .5-.5L11 4" />
                    </svg>
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
