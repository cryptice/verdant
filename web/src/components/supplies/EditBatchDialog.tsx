import type { SupplyInventoryResponse, SeasonResponse } from '../../api/client'
import { Dialog } from '../Dialog'

export interface EditBatchDialogProps {
  editBatch: SupplyInventoryResponse | null
  onClose: () => void
  onSubmit: () => void
  isPending: boolean

  editBatchQuantity: string
  setEditBatchQuantity: (v: string) => void
  editBatchCost: string
  setEditBatchCost: (v: string) => void
  editBatchSeasonId: number | ''
  setEditBatchSeasonId: (v: number | '') => void
  editBatchNotes: string
  setEditBatchNotes: (v: string) => void

  seasons: SeasonResponse[]

  onDeleteRequested: () => void

  mutError: string | null

  t: (key: string, opts?: Record<string, unknown>) => string
}

export function EditBatchDialog({
  editBatch,
  onClose, onSubmit, isPending,
  editBatchQuantity, setEditBatchQuantity,
  editBatchCost, setEditBatchCost,
  editBatchSeasonId, setEditBatchSeasonId,
  editBatchNotes, setEditBatchNotes,
  seasons,
  onDeleteRequested,
  mutError,
  t,
}: EditBatchDialogProps) {
  return (
    <Dialog
      open={editBatch !== null}
      onClose={onClose}
      title={t('common.edit')}
      actions={
        <>
          <button onClick={onClose} className="btn-secondary">{t('common.cancel')}</button>
          <button
            onClick={onSubmit}
            disabled={!editBatchQuantity || isPending}
            className="btn-primary"
          >
            {isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('supplies.quantity')} *</label>
            <input type="number" step="any" className="input w-full" value={editBatchQuantity} onChange={e => setEditBatchQuantity(e.target.value)} />
          </div>
          <div>
            <label className="field-label">{t('supplies.packageCost')} (kr)</label>
            <input type="number" step="any" className="input w-full" value={editBatchCost} onChange={e => setEditBatchCost(e.target.value)} />
          </div>
        </div>
        <div>
          <label className="field-label">{t('supplies.season')}</label>
          <select className="input w-full" value={editBatchSeasonId} onChange={e => setEditBatchSeasonId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">{t('common.none')}</option>
            {seasons.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
          </select>
        </div>
        <div>
          <label className="field-label">{t('supplies.notes')}</label>
          <input className="input w-full" value={editBatchNotes} onChange={e => setEditBatchNotes(e.target.value)} />
        </div>
        <button
          onClick={onDeleteRequested}
          className="text-sm text-error hover:underline"
        >
          {t('common.delete')}
        </button>
        {mutError && <p className="text-error text-sm">{mutError}</p>}
      </div>
    </Dialog>
  )
}
