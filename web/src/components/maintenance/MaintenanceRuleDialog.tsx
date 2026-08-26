import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type MaintenanceRuleResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import {
  activitiesForTarget,
  formatInterval,
  hasSeasonWindow,
  maintenanceActivityLabelSv,
  type MaintenanceActivity,
  type MaintenanceTarget,
} from '../../lib/maintenance'

export interface MaintenanceRuleTarget {
  kind: MaintenanceTarget
  id: number
}

export function MaintenanceRuleDialog({
  target,
  rule,
  onClose,
  onSaved,
  onError,
}: {
  target: MaintenanceRuleTarget
  /** Present in edit mode; absent (or null) in create mode. */
  rule?: MaintenanceRuleResponse | null
  onClose: () => void
  onSaved: () => void
  onError: (message: string) => void
}) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const isEdit = rule != null

  const activities = activitiesForTarget(target.kind)

  const [activityType, setActivityType] = useState<MaintenanceActivity | ''>(
    (rule?.activityType as MaintenanceActivity | undefined) ?? '',
  )
  const [intervalDays, setIntervalDays] = useState(rule ? String(rule.intervalDays) : '')
  const [anchorDate, setAnchorDate] = useState(rule?.anchorDate ?? '')
  const [active, setActive] = useState(rule?.active ?? true)

  // Snapshot at mount — whether *this* rule had a season window before any
  // edits. Only relevant in edit mode; drives the clearSeasonWindow decision.
  const [initialHasSeason] = useState(() => (rule ? hasSeasonWindow(rule) : false))
  const [seasonEnabled, setSeasonEnabled] = useState(initialHasSeason)
  const [seasonStartMonth, setSeasonStartMonth] = useState(
    rule?.seasonStartMonth != null ? String(rule.seasonStartMonth) : '',
  )
  const [seasonStartDay, setSeasonStartDay] = useState(
    rule?.seasonStartDay != null ? String(rule.seasonStartDay) : '',
  )
  const [seasonEndMonth, setSeasonEndMonth] = useState(
    rule?.seasonEndMonth != null ? String(rule.seasonEndMonth) : '',
  )
  const [seasonEndDay, setSeasonEndDay] = useState(
    rule?.seasonEndDay != null ? String(rule.seasonEndDay) : '',
  )

  const intervalNum = intervalDays !== '' ? Number(intervalDays) : NaN
  const seasonIncomplete =
    seasonEnabled && [seasonStartMonth, seasonStartDay, seasonEndMonth, seasonEndDay].some((f) => f === '')

  const canSave =
    activityType !== '' && Number.isFinite(intervalNum) && intervalNum >= 1 && !seasonIncomplete

  const queryKey = ['maintenance-rules', target.kind, target.id]

  const saveMut = useMutation({
    mutationFn: () => {
      if (isEdit && rule) {
        const seasonPatch = seasonEnabled
          ? {
              seasonStartMonth: Number(seasonStartMonth),
              seasonStartDay: Number(seasonStartDay),
              seasonEndMonth: Number(seasonEndMonth),
              seasonEndDay: Number(seasonEndDay),
            }
          : initialHasSeason
            ? { clearSeasonWindow: true as const }
            : {}
        return api.maintenanceRules.update(rule.id, {
          activityType,
          intervalDays: intervalNum,
          anchorDate: anchorDate || undefined,
          active,
          ...seasonPatch,
        })
      }

      const seasonPatch = seasonEnabled
        ? {
            seasonStartMonth: Number(seasonStartMonth),
            seasonStartDay: Number(seasonStartDay),
            seasonEndMonth: Number(seasonEndMonth),
            seasonEndDay: Number(seasonEndDay),
          }
        : {}
      return api.maintenanceRules.create({
        ...(target.kind === 'AREA' ? { gardenAreaId: target.id } : { bedId: target.id }),
        activityType,
        intervalDays: intervalNum,
        anchorDate: anchorDate || undefined,
        ...seasonPatch,
      })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey })
      onSaved()
    },
    onError: (e) => onError(e instanceof Error ? e.message : t('maintenance.saveError')),
  })

  return (
    <Dialog
      open={true}
      onClose={onClose}
      title={isEdit ? t('maintenance.editRuleTitle') : t('maintenance.newRuleTitle')}
      actions={
        <>
          <button className="btn-secondary" onClick={onClose}>{t('common.cancel')}</button>
          <button
            className="btn-primary"
            disabled={!canSave || saveMut.isPending}
            onClick={() => saveMut.mutate()}
          >
            {saveMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label className="field-label">{t('maintenance.activityLabel')}</label>
          <select
            value={activityType}
            onChange={(e) => setActivityType(e.target.value as MaintenanceActivity)}
            className="input w-full mt-1"
          >
            <option value="">{t('common.select')}</option>
            {activities.map((a) => (
              <option key={a} value={a}>{maintenanceActivityLabelSv(a)}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="field-label">{t('maintenance.intervalLabel')}</label>
          <input
            type="number"
            min={1}
            value={intervalDays}
            onChange={(e) => setIntervalDays(e.target.value)}
            className="input w-full mt-1"
          />
          {Number.isFinite(intervalNum) && intervalNum >= 1 && (
            <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', marginTop: 4 }}>
              {formatInterval(intervalNum)}
            </p>
          )}
        </div>

        <div>
          <label className="field-label">{t('maintenance.anchorDateLabel')}</label>
          <input
            type="date"
            value={anchorDate}
            onChange={(e) => setAnchorDate(e.target.value)}
            className="input w-full mt-1"
          />
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', marginTop: 4 }}>
            {t('maintenance.anchorDateHint')}
          </p>
        </div>

        <div>
          <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={seasonEnabled}
              onChange={(e) => setSeasonEnabled(e.target.checked)}
              className="h-4 w-4 rounded border-divider accent-accent"
            />
            <span className="field-label" style={{ marginBottom: 0 }}>{t('maintenance.season.toggle')}</span>
          </label>
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', marginTop: 4 }}>
            {t('maintenance.season.hint')}
          </p>
          {seasonEnabled && (
            <div className="grid grid-cols-2 gap-3 mt-2">
              <div>
                <label className="field-label">{t('maintenance.season.start')} · {t('maintenance.season.month')}</label>
                <input
                  type="number" min={1} max={12}
                  value={seasonStartMonth}
                  onChange={(e) => setSeasonStartMonth(e.target.value)}
                  className="input w-full"
                />
              </div>
              <div>
                <label className="field-label">{t('maintenance.season.start')} · {t('maintenance.season.day')}</label>
                <input
                  type="number" min={1} max={31}
                  value={seasonStartDay}
                  onChange={(e) => setSeasonStartDay(e.target.value)}
                  className="input w-full"
                />
              </div>
              <div>
                <label className="field-label">{t('maintenance.season.end')} · {t('maintenance.season.month')}</label>
                <input
                  type="number" min={1} max={12}
                  value={seasonEndMonth}
                  onChange={(e) => setSeasonEndMonth(e.target.value)}
                  className="input w-full"
                />
              </div>
              <div>
                <label className="field-label">{t('maintenance.season.end')} · {t('maintenance.season.day')}</label>
                <input
                  type="number" min={1} max={31}
                  value={seasonEndDay}
                  onChange={(e) => setSeasonEndDay(e.target.value)}
                  className="input w-full"
                />
              </div>
            </div>
          )}
        </div>

        {isEdit && (
          <div>
            <div className="flex items-center gap-2">
              <input
                id="maintenance-rule-active"
                type="checkbox"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="h-4 w-4 rounded border-divider accent-accent"
              />
              <label htmlFor="maintenance-rule-active" className="text-sm">{t('maintenance.activeLabel')}</label>
            </div>
            <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', marginTop: 4 }}>
              {t('maintenance.activeHint')}
            </p>
          </div>
        )}
      </div>
    </Dialog>
  )
}
