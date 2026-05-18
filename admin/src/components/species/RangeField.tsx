import { useTranslation } from 'react-i18next'

export function RangeField({ label, min, max, onMinChange, onMaxChange }: {
  label: string; min: string; max: string; onMinChange: (v: string) => void; onMaxChange: (v: string) => void
}) {
  const { t } = useTranslation()
  const inputClass = "px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA] w-20"
  return (
    <div className="flex flex-col sm:flex-row sm:items-center gap-1.5 sm:gap-3">
      <label className="text-xs font-medium text-[#787774] sm:w-40 sm:shrink-0">{label}</label>
      <div className="flex items-center gap-3">
        <input type="text" inputMode="numeric" maxLength={4} value={min} onChange={e => onMinChange(e.target.value)} placeholder={t('species.min')} className={inputClass} />
        <span className="text-xs text-[#787774]">–</span>
        <input type="text" inputMode="numeric" maxLength={4} value={max} onChange={e => onMaxChange(e.target.value)} placeholder={t('species.max')} className={inputClass} />
      </div>
    </div>
  )
}
