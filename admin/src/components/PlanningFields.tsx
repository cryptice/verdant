import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import { buttonClass } from '../api/planning'

export function PlanningField({ label, children }: { label: string; children: ReactNode }) {
  return <label className="block space-y-1 text-sm"><span className="text-[#787774]">{label}</span>{children}</label>
}

export function PlanningError({ error }: { error: unknown }) {
  return error ? <p role="alert" className="rounded border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error instanceof Error ? error.message : String(error)}</p> : null
}

export function PlanningConfirm({ children, onConfirm, onCancel, pending }: { children: ReactNode; onConfirm: () => void; onCancel: () => void; pending: boolean }) {
  const { t } = useTranslation()
  return <div role="alertdialog" aria-label={t('common.confirm')} className="border border-amber-300 rounded p-4 space-y-3 bg-amber-50">
    <p className="text-sm">{children}</p>
    <div className="flex gap-2">
      <button type="button" className={buttonClass} disabled={pending} onClick={onConfirm}>{t('common.confirm')}</button>
      <button type="button" className={buttonClass} disabled={pending} onClick={onCancel}>{t('common.cancel')}</button>
    </div>
  </div>
}

export function PlanningMonths({ label, value, onChange }: { label: string; value: number[]; onChange: (months: number[]) => void }) {
  const { i18n } = useTranslation()
  return <fieldset className="space-y-2"><legend className="text-sm text-[#787774]">{label}</legend><div className="flex flex-wrap gap-3">
    {Array.from({ length: 12 }, (_, i) => i + 1).map(month => <label key={month} className="text-sm flex gap-1 items-center">
      <input type="checkbox" checked={value.includes(month)} onChange={e => onChange(e.target.checked ? [...value, month].sort((a, b) => a - b) : value.filter(m => m !== month))} />
      {new Intl.DateTimeFormat(i18n.language, { month: 'short' }).format(new Date(2020, month - 1, 1))}
    </label>)}
  </div></fieldset>
}
