import type { SupplyInventoryResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import { formatUnit } from '../../pages/supplies/utils'

export interface DecrementBatchDialogProps {
  decrementBatch: SupplyInventoryResponse | null
  onClose: () => void
  onSubmit: () => void
  isPending: boolean

  decrementAmount: string
  setDecrementAmount: (v: string) => void

  mutError: string | null

  t: (key: string, opts?: Record<string, unknown>) => string
}

export function DecrementBatchDialog({
  decrementBatch,
  onClose, onSubmit, isPending,
  decrementAmount, setDecrementAmount,
  mutError,
  t,
}: DecrementBatchDialogProps) {
  return (
    <Dialog
      open={decrementBatch !== null}
      onClose={onClose}
      title={t('supplies.decrementTitle')}
      actions={
        <>
          <button onClick={onClose} className="btn-secondary">{t('common.cancel')}</button>
          <button
            onClick={onSubmit}
            disabled={!decrementAmount || Number(decrementAmount) <= 0 || isPending}
            className="btn-primary"
          >
            {isPending ? t('common.saving') : t('supplies.decrement')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        {decrementBatch && (
          <p className="text-sm text-text-secondary">
            {t('supplies.total')}: {formatUnit(decrementBatch.quantity, decrementBatch.unit, t)}
          </p>
        )}
        <div>
          <label className="field-label">{t('supplies.amount')} *</label>
          <input type="number" step="any" className="input w-full" value={decrementAmount} onChange={e => setDecrementAmount(e.target.value)} />
        </div>
        {mutError && <p className="text-error text-sm">{mutError}</p>}
      </div>
    </Dialog>
  )
}
