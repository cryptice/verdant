import type { SupplyTypeResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import { CategoryPropertyFields } from './CategoryPropertyFields'
import { UNITS, UNITS_BY_CATEGORY } from '../../pages/supplies/constants'
import { arePropertiesValid, deriveTypeName } from '../../pages/supplies/utils'

export interface EditTypeDialogProps {
  editType: SupplyTypeResponse | null
  onClose: () => void
  onSubmit: () => void
  isPending: boolean

  editTypeName: string
  setEditTypeName: (v: string) => void
  editTypeUnit: string
  setEditTypeUnit: (v: string) => void
  editTypeProps: Record<string, unknown>
  setEditTypeProps: (v: Record<string, unknown>) => void
  editTypeInexhaustible: boolean
  setEditTypeInexhaustible: (v: boolean) => void

  onDeleteRequested: () => void

  mutError: string | null

  t: (key: string, opts?: Record<string, unknown>) => string
}

export function EditTypeDialog({
  editType,
  onClose, onSubmit, isPending,
  editTypeName, setEditTypeName,
  editTypeUnit, setEditTypeUnit,
  editTypeProps, setEditTypeProps,
  editTypeInexhaustible, setEditTypeInexhaustible,
  onDeleteRequested,
  mutError,
  t,
}: EditTypeDialogProps) {
  return (
    <Dialog
      open={editType !== null}
      onClose={onClose}
      title={t('supplies.editType')}
      actions={
        <>
          <button onClick={onClose} className="btn-secondary">{t('common.cancel')}</button>
          <button
            onClick={onSubmit}
            disabled={!editTypeUnit || (editType ? !arePropertiesValid(editType.category, editTypeProps) : false) || isPending}
            className="btn-primary"
          >
            {isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label className="field-label">{t('supplies.typeName')}</label>
          <input
            className="input w-full"
            value={editTypeName}
            onChange={e => setEditTypeName(e.target.value)}
            placeholder={editType ? deriveTypeName(editType.category, editTypeProps, t) : t('common.optional')}
          />
        </div>
        <div>
          <label className="field-label">{t('supplies.unit')} *</label>
          <select className="input w-full" value={editTypeUnit} onChange={e => setEditTypeUnit(e.target.value)}>
            {(editType ? UNITS_BY_CATEGORY[editType.category] ?? UNITS : UNITS).map(u => <option key={u} value={u}>{t(`supplyUnit.${u}`)}</option>)}
          </select>
        </div>
        {editType && editType.category !== 'OTHER' && (
          <>
            <p className="text-xs font-medium text-text-secondary uppercase tracking-wide">{t('supplies.properties')}</p>
            <CategoryPropertyFields category={editType.category} props={editTypeProps} onChange={setEditTypeProps} t={t} />
          </>
        )}
        <label style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <input
            type="checkbox"
            checked={editTypeInexhaustible}
            onChange={e => setEditTypeInexhaustible(e.target.checked)}
          />
          <span style={{ fontSize: 14 }}>Obegränsad</span>
        </label>
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
