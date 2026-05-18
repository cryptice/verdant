import { Dialog } from '../Dialog'
import { CategoryPropertyFields } from './CategoryPropertyFields'
import { CATEGORIES, UNITS, UNITS_BY_CATEGORY, DEFAULT_UNIT } from '../../pages/supplies/constants'
import { arePropertiesValid, deriveTypeName } from '../../pages/supplies/utils'

export interface NewTypeDialogProps {
  open: boolean
  onClose: () => void
  onSubmit: () => void
  isPending: boolean

  typeName: string
  setTypeName: (v: string) => void
  typeNameEdited: boolean
  setTypeNameEdited: (v: boolean) => void
  typeCategory: string
  setTypeCategory: (v: string) => void
  typeUnit: string
  setTypeUnit: (v: string) => void
  typeProps: Record<string, unknown>
  setTypeProps: (v: Record<string, unknown>) => void
  typeInexhaustible: boolean
  setTypeInexhaustible: (v: boolean) => void

  mutError: string | null

  t: (key: string, opts?: Record<string, unknown>) => string
}

export function NewTypeDialog({
  open, onClose, onSubmit, isPending,
  typeName, setTypeName,
  typeNameEdited, setTypeNameEdited,
  typeCategory, setTypeCategory,
  typeUnit, setTypeUnit,
  typeProps, setTypeProps,
  typeInexhaustible, setTypeInexhaustible,
  mutError,
  t,
}: NewTypeDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('supplies.newType')}
      actions={
        <>
          <button onClick={onClose} className="btn-secondary">{t('common.cancel')}</button>
          <button
            onClick={onSubmit}
            disabled={!typeCategory || !typeUnit || !arePropertiesValid(typeCategory, typeProps) || isPending}
            className="btn-primary"
          >
            {isPending ? t('common.creating') : t('common.add')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label className="field-label">{t('supplies.category')} *</label>
          <select className="input w-full" value={typeCategory} onChange={e => {
            const cat = e.target.value
            setTypeCategory(cat)
            setTypeProps({})
            setTypeUnit(cat ? (DEFAULT_UNIT[cat] ?? 'COUNT') : '')
            if (!typeNameEdited) setTypeName(cat ? deriveTypeName(cat, {}, t) : '')
          }}>
            <option value="">{t('common.select')}</option>
            {CATEGORIES.map(c => <option key={c} value={c}>{t(`supplyCategory.${c}`)}</option>)}
          </select>
        </div>
        <div>
          <label className="field-label">{t('supplies.typeName')}</label>
          <input
            className="input w-full"
            value={typeName}
            onChange={e => { setTypeName(e.target.value); setTypeNameEdited(true) }}
          />
          <p className="text-xs text-text-secondary mt-1">{t('supplies.nameHint')}</p>
        </div>
        <div>
          <label className="field-label">{t('supplies.unit')} *</label>
          <select className="input w-full" value={typeUnit} onChange={e => setTypeUnit(e.target.value)}>
            <option value="">{t('common.select')}</option>
            {(typeCategory ? UNITS_BY_CATEGORY[typeCategory] ?? UNITS : UNITS).map(u => <option key={u} value={u}>{t(`supplyUnit.${u}`)}</option>)}
          </select>
        </div>
        {typeCategory && typeCategory !== 'OTHER' && (
          <>
            <p className="text-xs font-medium text-text-secondary uppercase tracking-wide">{t('supplies.properties')}</p>
            <CategoryPropertyFields category={typeCategory} props={typeProps} onChange={newProps => {
              setTypeProps(newProps)
              if (!typeNameEdited) setTypeName(deriveTypeName(typeCategory, newProps, t))
            }} t={t} />
          </>
        )}
        <label style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 8 }}>
          <input
            type="checkbox"
            checked={typeInexhaustible}
            onChange={e => setTypeInexhaustible(e.target.checked)}
          />
          <span>
            <span style={{ display: 'block', fontSize: 14 }}>Obegränsad</span>
            <span style={{ display: 'block', fontSize: 11, color: 'var(--color-forest)' }}>
              Behöver inte spåras (t.ex. egen hästgödsel).
            </span>
          </span>
        </label>
        {mutError && <p className="text-error text-sm">{mutError}</p>}
      </div>
    </Dialog>
  )
}
