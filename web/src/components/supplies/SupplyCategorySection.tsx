import type { SupplyInventoryResponse, SeasonResponse } from '../../api/client'
import type { GroupedType } from '../../pages/supplies/utils'
import { headerStyle } from '../../pages/supplies/constants'
import { SupplyTypeRow } from './SupplyTypeRow'

export interface SupplyCategorySectionProps {
  cat: string
  items: GroupedType[]
  expanded: Set<number>
  showUsed: boolean
  setShowUsed: (v: boolean) => void
  seasons: SeasonResponse[]
  onToggleExpand: (typeId: number) => void
  onToggleExpandAll: (items: GroupedType[], allExpanded: boolean) => void
  onAddCategoryType: () => void
  onAddBatchToType: (item: GroupedType) => void
  onEditType: (item: GroupedType) => void
  onDecrementBatch: (batch: SupplyInventoryResponse) => void
  onEditBatch: (batch: SupplyInventoryResponse) => void
  onDeleteBatch: (batch: SupplyInventoryResponse) => void
  t: (key: string, opts?: Record<string, unknown>) => string
}

export function SupplyCategorySection({
  cat, items, expanded, showUsed, setShowUsed,
  seasons,
  onToggleExpand, onToggleExpandAll,
  onAddCategoryType,
  onAddBatchToType, onEditType,
  onDecrementBatch, onEditBatch, onDeleteBatch,
  t,
}: SupplyCategorySectionProps) {
  const allExpanded = items.length > 0 && items.every(item => expanded.has(item.type.id))

  return (
    <div style={{ marginBottom: 40 }}>
      {/* Category section header */}
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 8,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 9,
            letterSpacing: 1.8,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
          }}>
            § {t(`supplyCategory.${cat}`)}
          </span>
          {items.length > 1 && (
            <button
              onClick={() => onToggleExpandAll(items, allExpanded)}
              style={{
                background: 'transparent',
                border: 'none',
                fontFamily: 'var(--font-mono)',
                fontSize: 9,
                letterSpacing: 1.2,
                textTransform: 'uppercase',
                color: 'var(--color-accent)',
                cursor: 'pointer',
                padding: 0,
              }}
            >
              {allExpanded ? t('supplies.collapseAll') : t('supplies.expandAll')}
            </button>
          )}
        </div>
        <button
          onClick={onAddCategoryType}
          style={{
            background: 'transparent',
            border: 'none',
            fontFamily: 'var(--font-mono)',
            fontSize: 9,
            letterSpacing: 1.2,
            textTransform: 'uppercase',
            color: 'var(--color-accent)',
            cursor: 'pointer',
          }}
        >
          {t(`supplies.addCategoryType.${cat}`)}
        </button>
      </div>

      {items.length === 0 && (
        <div style={{
          padding: '20px 0',
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 13,
          color: 'var(--color-forest)',
          opacity: 0.6,
          borderTop: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
          borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
        }}>
          {t('supplies.noBatches')}
        </div>
      )}

      {items.length > 0 && (
        <div>
          {/* Ledger header */}
          <div style={headerStyle as React.CSSProperties}>
            <span>{t('supplies.typeName')}</span>
            <span style={{ textAlign: 'right' }}>{t('supplies.quantity')}</span>
            <span />
          </div>

          {/* Type rows */}
          {items.map(item => (
            <SupplyTypeRow
              key={item.type.id}
              item={item}
              isOpen={expanded.has(item.type.id)}
              showUsed={showUsed}
              setShowUsed={setShowUsed}
              seasons={seasons}
              onToggleExpand={() => onToggleExpand(item.type.id)}
              onAddBatchToType={() => onAddBatchToType(item)}
              onEditType={() => onEditType(item)}
              onDecrementBatch={onDecrementBatch}
              onEditBatch={onEditBatch}
              onDeleteBatch={onDeleteBatch}
              t={t}
            />
          ))}
        </div>
      )}
    </div>
  )
}
