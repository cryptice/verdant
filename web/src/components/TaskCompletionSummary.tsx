import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { ScheduledTaskResponse } from '../api/client'

export function TaskCompletionSummary({ task }: { task: ScheduledTaskResponse }) {
  const { t } = useTranslation()
  const [expanded, setExpanded] = useState(false)
  const species = task.speciesName ?? task.originGroupName
  const place = task.gardenAreaName ?? task.bedName
  const rows = [
    ...(species ? [[t('tasks.summary.species'), species]] : []),
    // A sowing task's count is seeds: completing it on the Sow page deducts the sown seed count.
    [t(task.activityType === 'SOW' ? 'tasks.summary.seeds' : 'tasks.plannedQuantity'), String(task.targetCount)],
    ...(task.remainingCount !== task.targetCount ? [[t('tasks.summary.remaining'), String(task.remainingCount)]] : []),
    [t('tasks.summary.earliest'), task.earliestDate ?? '—'],
    [t('tasks.summary.latest'), task.deadline ?? '—'],
    ...(place ? [[t('tasks.summary.place'), place]] : []),
  ]
  return (
    <section style={{ gridColumn: '1 / -1', minWidth: 0 }}>
      {task.notes?.trim() && (
        <button
          type="button"
          aria-expanded={expanded}
          aria-label={t(expanded ? 'tasks.summary.collapse' : 'tasks.summary.expand')}
          onClick={() => setExpanded(value => !value)}
          style={{ display: 'flex', alignItems: 'flex-start', gap: 12, width: '100%', textAlign: 'left', background: 'transparent', border: 0, padding: '6px 0', marginBottom: 10, color: 'var(--color-ink)', fontSize: 16, cursor: 'pointer' }}
        >
          <span style={{ flex: 1, minWidth: 0, whiteSpace: expanded ? 'pre-wrap' : 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{task.notes}</span>
          <span aria-hidden="true">{expanded ? '▴' : '▾'}</span>
        </button>
      )}
      <table aria-label={t('tasks.summary.fields')} style={{ width: '100%', tableLayout: 'fixed', borderCollapse: 'collapse' }}>
        <tbody>
          {rows.map(([label, value]) => (
            <tr key={label}>
              <th scope="row" style={{ width: '40%', padding: '4px 12px 4px 0', verticalAlign: 'top', textAlign: 'left', fontSize: 12, fontWeight: 400, color: 'var(--color-forest)' }}>{label}</th>
              <td style={{ padding: '4px 0', verticalAlign: 'top', fontSize: 16, color: 'var(--color-ink)', overflowWrap: 'anywhere' }}>{value}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
