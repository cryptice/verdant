import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type MaintenanceRuleResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import { Snackbar, useSnackbar } from '../Snackbar'
import {
  dueState,
  formatInterval,
  formatSeasonWindow,
  maintenanceActivityLabelSv,
  type DueState,
} from '../../lib/maintenance'
import { MaintenanceRuleDialog, type MaintenanceRuleTarget } from './MaintenanceRuleDialog'

const DUE_TONE: Record<DueState['kind'], string> = {
  overdue:  'var(--color-berry)',
  due:      'var(--color-mustard)',
  upcoming: 'var(--color-sage)',
  inactive: 'var(--color-text-muted)',
}

export function MaintenanceRules({ target }: { target: MaintenanceRuleTarget }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const { message: toast, show: showToast } = useSnackbar()

  const [showAdd, setShowAdd] = useState(false)
  const [editingRule, setEditingRule] = useState<MaintenanceRuleResponse | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<MaintenanceRuleResponse | null>(null)

  const queryKey = ['maintenance-rules', target.kind, target.id]

  const { data: rules = [] } = useQuery({
    queryKey,
    // At most one filter — supplying both bedId and areaId is a 400.
    queryFn: () =>
      api.maintenanceRules.list(
        target.kind === 'AREA' ? { areaId: target.id } : { bedId: target.id },
      ),
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.maintenanceRules.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey })
      setConfirmDelete(null)
      showToast(t('maintenance.ruleDeleted'))
    },
    onError: () => showToast(t('maintenance.deleteError')),
  })

  const todayIso = new Date().toISOString().slice(0, 10)

  return (
    <div data-testid="maintenance-rules">
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, margin: '40px 0 12px' }}>
        <h2
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 30,
            fontWeight: 300,
            margin: 0,
            fontVariationSettings: '"SOFT" 100, "opsz" 144',
          }}
        >
          {t('maintenance.title')}<span style={{ color: 'var(--color-accent)' }}>.</span>
        </h2>
        <div style={{ marginLeft: 'auto' }}>
          <button onClick={() => setShowAdd(true)} className="btn-secondary" style={{ whiteSpace: 'nowrap' }}>
            {t('maintenance.addRule')}
          </button>
        </div>
      </div>

      {rules.length === 0 ? (
        <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', margin: '8px 0' }}>
          {t('maintenance.emptyState')}
        </p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {rules.map((rule) => {
            const state = dueState(rule, todayIso)
            const season = formatSeasonWindow(rule)
            const dueText =
              state.kind === 'overdue' ? t('maintenance.due.overdue', { count: state.days }) :
              state.kind === 'upcoming' ? t('maintenance.due.upcoming', { count: state.days }) :
              state.kind === 'due' ? t('maintenance.due.today') :
              t('maintenance.due.paused')

            return (
              <div
                key={rule.id}
                className="list-tile"
                style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16 }}
              >
                <div>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
                    {maintenanceActivityLabelSv(rule.activityType)}
                  </div>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', marginTop: 2 }}>
                    {formatInterval(rule.intervalDays)}
                    {season && <> · {season}</>}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <span
                    data-testid={`rule-due-${rule.id}`}
                    data-due-kind={state.kind}
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 10,
                      letterSpacing: 1.2,
                      textTransform: 'uppercase',
                      color: DUE_TONE[state.kind],
                      border: `1px solid ${DUE_TONE[state.kind]}`,
                      borderRadius: 999,
                      padding: '4px 8px',
                      whiteSpace: 'nowrap',
                    }}
                  >
                    {dueText}
                  </span>
                  <button
                    onClick={() => setEditingRule(rule)}
                    style={{
                      background: 'transparent', border: 'none', cursor: 'pointer',
                      fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
                      textTransform: 'uppercase', color: 'var(--color-accent)',
                    }}
                  >
                    {t('common.edit')}
                  </button>
                  <button
                    onClick={() => setConfirmDelete(rule)}
                    style={{
                      background: 'transparent', border: 'none', cursor: 'pointer',
                      fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
                      textTransform: 'uppercase', color: 'var(--color-accent)',
                    }}
                  >
                    {t('common.delete')}
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      {showAdd && (
        <MaintenanceRuleDialog
          target={target}
          onClose={() => setShowAdd(false)}
          onSaved={() => { setShowAdd(false); showToast(t('maintenance.ruleSaved')) }}
          onError={(msg) => showToast(msg)}
        />
      )}

      {editingRule && (
        <MaintenanceRuleDialog
          target={target}
          rule={editingRule}
          onClose={() => setEditingRule(null)}
          onSaved={() => { setEditingRule(null); showToast(t('maintenance.ruleSaved')) }}
          onError={(msg) => showToast(msg)}
        />
      )}

      {confirmDelete && (
        <Dialog
          open={true}
          title={t('maintenance.deleteRuleTitle')}
          onClose={() => setConfirmDelete(null)}
          actions={
            <>
              <button className="btn-secondary" onClick={() => setConfirmDelete(null)}>{t('common.cancel')}</button>
              <button
                className="btn-primary"
                style={{ background: 'var(--color-accent)', borderColor: 'var(--color-accent)' }}
                onClick={() => deleteMut.mutate(confirmDelete.id)}
                disabled={deleteMut.isPending}
              >
                {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
              </button>
            </>
          }
        >
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic' }}>
            {t('maintenance.deleteRuleConfirm')}
          </p>
        </Dialog>
      )}

      <Snackbar message={toast} />
    </div>
  )
}
