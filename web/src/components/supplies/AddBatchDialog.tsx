import type { SupplyTypeResponse, SeasonResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import { CATEGORIES } from '../../pages/supplies/constants'
import { formatTypeLabel } from '../../pages/supplies/utils'

export interface AddBatchDialogProps {
  open: boolean
  onClose: () => void
  onSubmit: () => void
  isPending: boolean

  types: SupplyTypeResponse[]
  seasons: SeasonResponse[]
  selectedBatchType: SupplyTypeResponse | undefined
  isPackageMode: boolean | undefined

  addBatchCategoryFilter: string | null

  batchTypeId: number | ''
  setBatchTypeId: (v: number | '') => void
  batchQuantity: string
  setBatchQuantity: (v: string) => void
  batchPackageSize: string
  setBatchPackageSize: (v: string) => void
  batchPackageCount: string
  setBatchPackageCount: (v: string) => void
  batchCost: string
  setBatchCost: (v: string) => void
  batchSeasonId: number | ''
  setBatchSeasonId: (v: number | '') => void
  batchNotes: string
  setBatchNotes: (v: string) => void

  onOpenNewType: () => void

  mutError: string | null

  t: (key: string, opts?: Record<string, unknown>) => string
}

export function AddBatchDialog({
  open, onClose, onSubmit, isPending,
  types, seasons, selectedBatchType, isPackageMode,
  addBatchCategoryFilter,
  batchTypeId, setBatchTypeId,
  batchQuantity, setBatchQuantity,
  batchPackageSize, setBatchPackageSize,
  batchPackageCount, setBatchPackageCount,
  batchCost, setBatchCost,
  batchSeasonId, setBatchSeasonId,
  batchNotes, setBatchNotes,
  onOpenNewType,
  mutError,
  t,
}: AddBatchDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('supplies.addBatch')}
      actions={
        <>
          <button onClick={onClose} className="btn-secondary">{t('common.cancel')}</button>
          <button
            onClick={onSubmit}
            disabled={!batchTypeId || (isPackageMode ? (!batchPackageSize || !batchPackageCount) : !batchQuantity) || isPending}
            className="btn-primary"
          >
            {isPending ? t('common.creating') : t('common.add')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        {types.length === 0 ? (
          <div className="text-center py-4">
            <p className="text-sm text-text-secondary mb-2">{t('supplies.noTypes')}</p>
            <button onClick={onOpenNewType} className="text-sm text-accent hover:underline">
              {t('supplies.newType')}
            </button>
          </div>
        ) : (
          <>
            <div>
              <label className="field-label">{t('supplies.selectType')}</label>
              <select className="input w-full" value={batchTypeId} onChange={e => setBatchTypeId(e.target.value ? Number(e.target.value) : '')}>
                <option value="">{t('supplies.selectType')}</option>
                {CATEGORIES.filter(cat => !addBatchCategoryFilter || cat === addBatchCategoryFilter).map(cat => {
                  const catTypes = types.filter(ty => ty.category === cat)
                  if (catTypes.length === 0) return null
                  return addBatchCategoryFilter ? (
                    catTypes.map(ty => (
                      <option key={ty.id} value={ty.id}>{formatTypeLabel(ty, t)}</option>
                    ))
                  ) : (
                    <optgroup key={cat} label={t(`supplyCategory.${cat}`)}>
                      {catTypes.map(ty => (
                        <option key={ty.id} value={ty.id}>{formatTypeLabel(ty, t)}</option>
                      ))}
                    </optgroup>
                  )
                })}
              </select>
            </div>
            {isPackageMode ? (
              <>
                <div className="grid grid-cols-3 gap-3">
                  <div>
                    <label className="field-label">{t('supplies.packageSize')} ({t(`supplyUnit.${selectedBatchType!.unit}`)})</label>
                    <input type="number" step="any" className="input w-full" value={batchPackageSize} onChange={e => setBatchPackageSize(e.target.value)} />
                  </div>
                  <div>
                    <label className="field-label">{t('supplies.packageCount')} *</label>
                    <input type="number" className="input w-full" value={batchPackageCount} onChange={e => setBatchPackageCount(e.target.value)} />
                  </div>
                  <div>
                    <label className="field-label">{t('supplies.pricePerPackage')} (kr)</label>
                    <input type="number" step="any" className="input w-full" value={batchCost} onChange={e => setBatchCost(e.target.value)} />
                  </div>
                </div>
                <p className="text-xs text-text-secondary mt-1">
                  {(() => {
                    const totalQty = (Number(batchPackageSize) || 0) * (Number(batchPackageCount) || 0)
                    const unitLabel = t(`supplyUnit.${selectedBatchType!.unit}`)
                    const totalCost = (Number(batchCost) || 0) * (Number(batchPackageCount) || 0)
                    return (
                      <>
                        {t('supplies.totalSummary', { quantity: totalQty % 1 === 0 ? totalQty : totalQty.toFixed(1), unit: unitLabel })}
                        {totalCost > 0 && ` — ${totalCost.toFixed(2)} kr`}
                      </>
                    )
                  })()}
                </p>
              </>
            ) : (
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="field-label">{t('supplies.quantity')} *</label>
                  <input type="number" step="any" className="input w-full" value={batchQuantity} onChange={e => setBatchQuantity(e.target.value)} />
                </div>
                <div>
                  <label className="field-label">{selectedBatchType?.unit === 'COUNT' ? t('supplies.pricePerUnit') : t('supplies.packageCost')} (kr)</label>
                  <input type="number" step="any" className="input w-full" value={batchCost} onChange={e => setBatchCost(e.target.value)} />
                </div>
              </div>
            )}
            <div>
              <label className="field-label">{t('supplies.season')}</label>
              <select className="input w-full" value={batchSeasonId} onChange={e => setBatchSeasonId(e.target.value ? Number(e.target.value) : '')}>
                <option value="">{t('common.none')}</option>
                {seasons.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
              </select>
            </div>
            <div>
              <label className="field-label">{t('supplies.notes')}</label>
              <input className="input w-full" value={batchNotes} onChange={e => setBatchNotes(e.target.value)} />
            </div>
          </>
        )}
        {mutError && <p className="text-error text-sm">{mutError}</p>}
      </div>
    </Dialog>
  )
}
