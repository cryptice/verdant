import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Chip, SectionHeader, MetaCell } from '../components/faltet'
import { Dialog } from '../components/Dialog'
import { Snackbar, useSnackbar } from '../components/Snackbar'
import { AreaPhotosSection } from '../components/area/AreaPhotosSection'
import { MaintenanceRules } from '../components/maintenance/MaintenanceRules'
import { areaCategoryLabelSv, areaEventLabelSv } from '../lib/area'
import { activitiesForTarget, maintenanceActivityLabelSv, type MaintenanceActivity } from '../lib/maintenance'

type LogActivity = MaintenanceActivity | 'NOTE'

export function AreaDetail() {
  const { id } = useParams<{ id: string }>()
  const areaId = Number(id)
  const { t } = useTranslation()
  const navigate = useNavigate()

  const { data: area } = useQuery({ queryKey: ['area', areaId], queryFn: () => api.areas.get(areaId) })
  const { data: events = [] } = useQuery({
    queryKey: ['area-events', areaId],
    queryFn: () => api.areas.events(areaId, 20),
  })

  const { message: toast, show: showToast } = useSnackbar()

  const [showLog, setShowLog] = useState(false)
  const [showDelete, setShowDelete] = useState(false)

  const deleteMut = useMutation({
    mutationFn: () => api.areas.delete(areaId),
    onSuccess: () => navigate(area ? `/garden/${area.gardenId}` : '/gardens'),
    onError: () => showToast(t('area.deleteError')),
  })

  if (!area) return null

  return (
    <div>
      <Masthead
        left={
          <span>
            <Link to="/gardens" style={{ color: 'inherit', textDecoration: 'none' }}>
              {t('nav.gardens')}
            </Link>
            {' / '}
            <Link to={`/garden/${area.gardenId}`} style={{ color: 'inherit', textDecoration: 'none' }}>
              {area.gardenName ?? '…'}
            </Link>
            {' / '}
            <span style={{ color: 'var(--color-accent)' }}>{area.name}</span>
          </span>
        }
        center={t('area.masthead.center')}
        right={
          <button onClick={() => setShowLog(true)} className="btn-secondary">
            {t('area.logMaintenance')}
          </button>
        }
      />

      <div className="page-body">
        {/* Hero row */}
        <div>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 18 }}>
            <Chip tone="sage">{areaCategoryLabelSv(area.category)}</Chip>
          </div>
          <h1
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 80,
              fontWeight: 300,
              lineHeight: 1,
              letterSpacing: -1.5,
              margin: 0,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            <span style={{ fontStyle: 'italic', color: 'var(--color-accent)' }}>{area.name}.</span>
          </h1>
          {area.description && (
            <p
              style={{
                marginTop: 16,
                fontFamily: 'Georgia, var(--font-display)',
                fontSize: 15,
                lineHeight: 1.6,
                color: 'var(--color-forest)',
              }}
            >
              {area.description}
            </p>
          )}
        </div>

        {/* Meta grid */}
        <div
          style={{
            marginTop: 32,
            display: 'grid',
            gridTemplateColumns: '1fr 1fr',
            gap: 0,
            border: '1px solid var(--color-ink)',
          }}
        >
          <MetaCell label={t('area.meta.category')} value={areaCategoryLabelSv(area.category)} />
          <MetaCell label={t('area.meta.size')} value={area.sizeSqm != null ? `${area.sizeSqm} m²` : '—'} />
        </div>

        {/* Underhåll — shared rule component, generic over bed/area */}
        <MaintenanceRules target={{ kind: 'AREA', id: areaId }} />

        {/* Historik — area-level maintenance log */}
        <SectionHeader title={t('area.history.title')} meta={t('area.history.meta', { count: events.length })} />
        {events.length === 0 ? (
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', margin: '8px 0' }}>
            {t('area.history.empty')}
          </p>
        ) : (
          <div style={{ marginBottom: 8 }}>
            {events.map((ev) => (
              <div
                key={ev.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 80px',
                  gap: 12,
                  padding: '10px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  alignItems: 'baseline',
                }}
              >
                <div>
                  <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16 }}>
                    {areaEventLabelSv(ev.eventType)}
                  </div>
                  {ev.notes && (
                    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, color: 'var(--color-forest)' }}>
                      {ev.notes}
                    </div>
                  )}
                </div>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 10,
                    letterSpacing: 1.2,
                    textAlign: 'right',
                    color: 'var(--color-forest)',
                  }}
                >
                  {ev.eventDate}
                </span>
              </div>
            ))}
          </div>
        )}

        <AreaPhotosSection
          areaId={areaId}
          onError={(msg) => showToast(msg)}
          onSuccess={(msg) => showToast(msg)}
        />

        {/* Danger callout */}
        <div
          style={{
            marginTop: 48,
            border: '1px solid color-mix(in srgb, var(--color-accent) 40%, transparent)',
            padding: '22px 28px',
          }}
        >
          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              marginBottom: 10,
            }}
          >
            {t('area.danger.title')}
          </div>
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, margin: 0 }}>
            {t('area.danger.warning')}
          </p>
          <button
            onClick={() => setShowDelete(true)}
            style={{
              marginTop: 10,
              background: 'transparent',
              border: 'none',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              padding: 0,
            }}
          >
            → {t('area.danger.delete')}
          </button>
        </div>
      </div>

      {showLog && (
        <LogMaintenanceDialog
          areaId={areaId}
          onClose={() => setShowLog(false)}
          onSuccess={(msg) => showToast(msg)}
          onError={(msg) => showToast(msg)}
        />
      )}

      {showDelete && (
        <Dialog
          open={true}
          title={t('area.danger.confirmTitle')}
          onClose={() => setShowDelete(false)}
          actions={
            <>
              <button className="btn-secondary" onClick={() => setShowDelete(false)}>{t('common.cancel')}</button>
              <button
                className="btn-primary"
                style={{ background: 'var(--color-accent)', borderColor: 'var(--color-accent)' }}
                onClick={() => deleteMut.mutate()}
                disabled={deleteMut.isPending}
              >
                {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
              </button>
            </>
          }
        >
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic' }}>
            {t('area.danger.confirm')}
          </p>
        </Dialog>
      )}

      <Snackbar message={toast} />
    </div>
  )
}

function LogMaintenanceDialog({
  areaId,
  onClose,
  onSuccess,
  onError,
}: {
  areaId: number
  onClose: () => void
  onSuccess: (message: string) => void
  onError: (message: string) => void
}) {
  const { t } = useTranslation()
  const qc = useQueryClient()

  const activityOptions: LogActivity[] = [...activitiesForTarget('AREA'), 'NOTE']

  const [activityType, setActivityType] = useState<LogActivity | ''>('')
  const [eventDate, setEventDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [notes, setNotes] = useState('')

  const canSave = activityType !== ''

  const logMut = useMutation({
    mutationFn: () =>
      api.areas.logEvent(areaId, {
        activityType: activityType as string,
        eventDate: eventDate || undefined,
        notes: notes.trim() || undefined,
      }),
    onSuccess: () => {
      // The next-due date on each rule is derived from this event log, not a
      // stored timestamp — invalidate both so history AND the rules section
      // reflect the just-logged work.
      qc.invalidateQueries({ queryKey: ['area-events', areaId] })
      qc.invalidateQueries({ queryKey: ['maintenance-rules', 'AREA', areaId] })
      onSuccess(t('area.logSuccess'))
      onClose()
    },
    onError: () => onError(t('area.logError')),
  })

  return (
    <Dialog
      open={true}
      onClose={onClose}
      title={t('area.log.title')}
      actions={
        <>
          <button className="btn-secondary" onClick={onClose}>{t('common.cancel')}</button>
          <button
            className="btn-primary"
            disabled={!canSave || logMut.isPending}
            onClick={() => logMut.mutate()}
          >
            {logMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label className="field-label">{t('area.log.activityLabel')}</label>
          <select
            value={activityType}
            onChange={(e) => setActivityType(e.target.value as LogActivity)}
            className="input w-full mt-1"
          >
            <option value="">{t('common.select')}</option>
            {activityOptions.map((a) => (
              <option key={a} value={a}>
                {a === 'NOTE' ? areaEventLabelSv('NOTE') : maintenanceActivityLabelSv(a)}
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="field-label">{t('area.log.dateLabel')}</label>
          <input
            type="date"
            value={eventDate}
            onChange={(e) => setEventDate(e.target.value)}
            className="input w-full mt-1"
          />
        </div>

        <div>
          <label className="field-label">{t('area.log.notesLabel')}</label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
            placeholder={t('area.log.notesPlaceholder') ?? ''}
            className="input w-full mt-1"
          />
        </div>
      </div>
    </Dialog>
  )
}
