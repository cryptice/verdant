import { useState } from 'react'
import { Warning } from './Warning'
import { isValidNpk, mmWarning, mlWarning } from '../../pages/supplies/utils'

export function CategoryPropertyFields({
  category, props, onChange,
  t,
}: {
  category: string
  props: Record<string, unknown>
  onChange: (p: Record<string, unknown>) => void
  t: (key: string) => string
}) {
  const [blurred, setBlurred] = useState<Set<string>>(new Set())
  const set = (key: string, value: unknown) => onChange({ ...props, [key]: value })
  const numVal = (key: string) => (props[key] != null ? String(props[key]) : '')
  const onBlur = (key: string) => setBlurred(prev => new Set(prev).add(key))
  const onFocus = (key: string) => setBlurred(prev => { const next = new Set(prev); next.delete(key); return next })
  const showWarn = (key: string) => blurred.has(key)

  switch (category) {
    case 'SOIL':
      return (
        <div>
          <label className="field-label">{t('supplies.type')} *</label>
          <input className="input w-full" list="soil-types" value={(props.type as string) ?? ''} onChange={e => set('type', e.target.value)} />
          <datalist id="soil-types">
            <option value={t('soilTypes.planting')} />
            <option value={t('soilTypes.sowing')} />
            <option value={t('soilTypes.rhododendron')} />
            <option value={t('soilTypes.rose')} />
            <option value={t('soilTypes.mediterranean')} />
          </datalist>
        </div>
      )
    case 'POT':
      return (
        <>
          <div>
            <label className="field-label">{t('supplies.shape')} *</label>
            <select className="input w-full" value={(props.shape as string) ?? ''} onChange={e => set('shape', e.target.value)}>
              <option value="">{t('common.select')}</option>
              <option value="round">{t('supplies.round')}</option>
              <option value="square">{t('supplies.square')}</option>
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{props.shape === 'round' ? t('supplies.diameterMm') : t('supplies.widthMm')} *</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('widthMm')} onBlur={() => onBlur('widthMm')} />
              {showWarn('widthMm') && <Warning message={mmWarning(props.widthMm, props.shape === 'round' ? t('supplies.diameter') : t('supplies.width'), 20, 1000, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.heightMm')} *</label>
              <input type="number" className="input w-full" value={numVal('heightMm')} onChange={e => set('heightMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('heightMm')} onBlur={() => onBlur('heightMm')} />
              {showWarn('heightMm') && <Warning message={mmWarning(props.heightMm, t('supplies.height'), 20, 1000, t)} />}
            </div>
          </div>
        </>
      )
    case 'FERTILIZER': {
      const npkVal = (props.npk as string) ?? ''
      const npkValid = !npkVal || isValidNpk(npkVal)
      return (
        <div>
          <label className="field-label">{t('supplies.npk')}</label>
          <input className="input w-full" value={npkVal} onChange={e => set('npk', e.target.value)} placeholder="e.g. 10-5-10" />
          {npkVal && !npkValid && <Warning message={t('supplies.warnNpkFormat')} />}
        </div>
      )
    }
    case 'TRAY':
      return (
        <>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('supplies.rows')} *</label>
              <input type="number" className="input w-full" value={numVal('rows')} onChange={e => set('rows', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.columns')} *</label>
              <input type="number" className="input w-full" value={numVal('columns')} onChange={e => set('columns', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="field-label">{t('supplies.lengthMm')}</label>
              <input type="number" className="input w-full" value={numVal('lengthMm')} onChange={e => set('lengthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('lengthMm')} onBlur={() => onBlur('lengthMm')} />
              {showWarn('lengthMm') && <Warning message={mmWarning(props.lengthMm, t('supplies.length'), 50, 2000, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.widthMm')}</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('widthMm')} onBlur={() => onBlur('widthMm')} />
              {showWarn('widthMm') && <Warning message={mmWarning(props.widthMm, t('supplies.width'), 50, 2000, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.volumePerPlugMl')}</label>
              <input type="number" className="input w-full" value={numVal('volumePerPlugMl')} onChange={e => set('volumePerPlugMl', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('volumePerPlugMl')} onBlur={() => onBlur('volumePerPlugMl')} />
              {showWarn('volumePerPlugMl') && <Warning message={mlWarning(props.volumePerPlugMl, 1, 500, t)} />}
            </div>
          </div>
        </>
      )
    case 'LABEL':
      return (
        <>
          <div>
            <label className="field-label">{t('supplies.material')}</label>
            <input className="input w-full" value={(props.material as string) ?? ''} onChange={e => set('material', e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('supplies.heightMm')}</label>
              <input type="number" className="input w-full" value={numVal('heightMm')} onChange={e => set('heightMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('heightMm')} onBlur={() => onBlur('heightMm')} />
              {showWarn('heightMm') && <Warning message={mmWarning(props.heightMm, t('supplies.height'), 5, 300, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.widthMm')}</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('widthMm')} onBlur={() => onBlur('widthMm')} />
              {showWarn('widthMm') && <Warning message={mmWarning(props.widthMm, t('supplies.width'), 5, 300, t)} />}
            </div>
          </div>
        </>
      )
    default:
      return null
  }
}
