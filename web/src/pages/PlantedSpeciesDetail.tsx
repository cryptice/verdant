import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesEventSummaryEntry, type PlantLocationGroup } from '../api/client'
import { Masthead } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const STATUS_ORDER = ['SEEDED', 'POTTED_UP', 'PLANTED_OUT', 'GROWING', 'HARVESTED', 'RECOVERED', 'REMOVED']

const STATUS_LABEL_PLURAL_SV: Record<string, string> = {
  SEEDED: 'Sådda',
  POTTED_UP: 'Omskolade',
  PLANTED_OUT: 'Utplanterade',
  GROWING: 'Utplanterade',
  HARVESTED: 'Skördade',
  RECOVERED: 'Återhämtade',
  REMOVED: 'Borttagna',
}

function labelForEventRow(e: { type: string; fromLoc?: string | null; toLoc?: string | null; notes?: string | null }): string {
  if (e.type === 'MOVED') {
    if (e.fromLoc && e.toLoc) return `Flyttade · ${e.fromLoc} → ${e.toLoc}`
    if (e.toLoc) return `Flyttade · till ${e.toLoc}`
    if (e.fromLoc) return `Flyttade · ut ur ${e.fromLoc}`
    return 'Flyttade'
  }
  if (e.type === 'NOTE' && e.notes) return e.notes
  return EVENT_LABEL_SV[e.type] ?? e.type
}

const EVENT_LABEL_SV: Record<string, string> = {
  SEEDED: 'Sådda',
  POTTED_UP: 'Omskolade',
  PLANTED_OUT: 'Utplanterade',
  HARVESTED: 'Skördade',
  RECOVERED: 'Återhämtade',
  REMOVED: 'Borttagna',
  NOTE: 'Notering',
  BUDDING: 'Knoppar',
  FIRST_BLOOM: 'Första blomman',
  PEAK_BLOOM: 'Toppblomning',
  LAST_BLOOM: 'Sista blomman',
  LIFTED: 'Uppgrävda',
  DIVIDED: 'Delade',
  STORED: 'Lagrade',
  PINCHED: 'Toppade',
  DISBUDDED: 'Knopprensade',
  APPLIED_SUPPLY: 'Gödslade',
  WATERED: 'Vattnade',
  MOVED: 'Flyttade',
  WEEDED: 'Rensade ogräs',
}

const STATUS_COLOR: Record<string, string> = {
  SEEDED: 'var(--color-mustard)',
  POTTED_UP: 'var(--color-sky)',
  PLANTED_OUT: 'var(--color-sage)',
  GROWING: 'var(--color-sage)',
  HARVESTED: 'var(--color-accent)',
  RECOVERED: 'var(--color-berry)',
  REMOVED: 'var(--color-forest)',
}

interface EventRow {
  type: string; date: string; current: number; total: number
  fromLoc?: string | null
  toLoc?: string | null
  notes?: string | null
}

interface EventTarget {
  eventType: string
  eventDate: string
  currentStatus: string
  current: number
}

export function PlantedSpeciesDetail() {
  const { speciesId } = useParams<{ speciesId: string }>()
  const id = Number(speciesId)
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { t } = useTranslation()

  const [chooserTarget, setChooserTarget] = useState<EventTarget | null>(null)
  const [editTarget, setEditTarget] = useState<EventTarget | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<EventTarget | null>(null)
  const [moveTarget, setMoveTarget] = useState<PlantLocationGroup | null>(null)

  const { data: trayLocations = [] } = useQuery({
    queryKey: ['tray-locations'],
    queryFn: () => api.trayLocations.list(),
  })

  const moveMut = useMutation({
    mutationFn: (vars: { sourceId: number | null; targetId: number | null; status: string; count: number }) =>
      api.plants.moveTrayPlants({
        fromTrayLocationId: vars.sourceId,
        toTrayLocationId: vars.targetId,
        speciesId: id,
        status: vars.status,
        count: vars.count,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-locations', id] })
      qc.invalidateQueries({ queryKey: ['species-events', id] })
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      qc.invalidateQueries({ queryKey: ['tray-locations'] })
    },
  })

  const { data: summary } = useQuery({
    queryKey: ['species-summary'],
    queryFn: api.plants.speciesSummary,
    select: (list) => list.find((s) => s.speciesId === id),
  })

  const { data: locations, error, isLoading, refetch } = useQuery({
    queryKey: ['species-locations', id],
    queryFn: () => api.plants.speciesLocations(id),
  })

  const { data: trayEvents } = useQuery({
    queryKey: ['species-events', id, true],
    queryFn: () => api.plants.speciesEvents(id, true),
  })

  const updateDateMut = useMutation({
    mutationFn: (vars: { eventType: string; oldDate: string; newDate: string; currentStatus: string }) =>
      api.plants.updateSpeciesEventDate(id, { ...vars, trayOnly: true }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-events', id] })
      qc.invalidateQueries({ queryKey: ['species-locations', id] })
    },
  })

  const deleteMut = useMutation({
    mutationFn: (vars: { eventType: string; eventDate: string; count: number; currentStatus: string }) =>
      api.plants.deleteSpeciesEvent(id, { ...vars, trayOnly: true }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-events', id] })
      qc.invalidateQueries({ queryKey: ['species-locations', id] })
      qc.invalidateQueries({ queryKey: ['species-summary'] })
    },
  })

  if (isLoading) return (
    <div className="flex justify-center p-16">
      <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
    </div>
  )
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const rows = locations ?? []
  const events = trayEvents ?? []
  const displayName = summary
    ? (summary.variantName ? `${summary.speciesName} – ${summary.variantName}` : summary.speciesName)
    : t('species.title')

  // Bucket locations by status, ordering them by STATUS_ORDER.
  const byStatus: { status: string; locations: PlantLocationGroup[] }[] = []
  const statusBuckets = new Map<string, PlantLocationGroup[]>()
  for (const loc of rows) {
    if (!statusBuckets.has(loc.status)) statusBuckets.set(loc.status, [])
    statusBuckets.get(loc.status)!.push(loc)
  }
  for (const status of STATUS_ORDER) {
    const list = statusBuckets.get(status)
    if (list && list.length > 0) byStatus.push({ status, locations: list })
  }
  for (const [status, list] of statusBuckets) {
    if (!STATUS_ORDER.includes(status)) byStatus.push({ status, locations: list })
  }

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.plants')} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>{displayName}</span>
          </span>
        }
      />

      <div className="page-body">
        <h1
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: 60,
            fontWeight: 300,
            letterSpacing: -1,
            margin: 0,
            fontVariationSettings: '"SOFT" 100, "opsz" 144',
          }}
        >
          {displayName}
          <span style={{ color: 'var(--color-accent)' }}>.</span>
        </h1>
        {summary?.scientificName && (
          <p
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 14,
              color: 'var(--color-sage)',
              marginTop: 6,
            }}
          >
            {summary.scientificName}
          </p>
        )}

        <div style={{ marginTop: 28, display: 'flex', flexDirection: 'column', gap: 16 }}>
          {byStatus.map(({ status, locations }) => (
            <StatusCard
              key={status}
              status={status}
              locations={locations}
              events={events}
              onBedClick={(bedId) => navigate(`/bed/${bedId}`)}
              onEventTap={(t) => setChooserTarget(t)}
              onMoveTray={(loc) => setMoveTarget(loc)}
            />
          ))}
        </div>

        {byStatus.length === 0 && (
          <p
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              color: 'var(--color-forest)',
              textAlign: 'center',
              marginTop: 20,
            }}
          >
            {t('species.noActivePlants')}
          </p>
        )}
      </div>

      {chooserTarget && (
        <Dialog open onClose={() => setChooserTarget(null)} title={`${EVENT_LABEL_SV[chooserTarget.eventType] ?? chooserTarget.eventType} · ${chooserTarget.eventDate}`}>
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, letterSpacing: 1.2, color: 'var(--color-forest)', marginBottom: 14 }}>
            {chooserTarget.current} plantor
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <button className="btn-primary" onClick={() => { setEditTarget(chooserTarget); setChooserTarget(null) }}>
              Ändra datum
            </button>
            <button
              className="btn-secondary"
              style={{ color: 'var(--color-accent)' }}
              onClick={() => { setDeleteTarget(chooserTarget); setChooserTarget(null) }}
            >
              Ta bort händelse…
            </button>
          </div>
        </Dialog>
      )}

      {editTarget && (
        <EditDateDialog
          target={editTarget}
          onClose={() => setEditTarget(null)}
          onSave={(newDate) => {
            if (newDate !== editTarget.eventDate) {
              updateDateMut.mutate({
                eventType: editTarget.eventType,
                oldDate: editTarget.eventDate,
                newDate,
                currentStatus: editTarget.currentStatus,
              })
            }
            setEditTarget(null)
          }}
        />
      )}

      {deleteTarget && (
        <DeleteEventDialog
          target={deleteTarget}
          onClose={() => setDeleteTarget(null)}
          onConfirm={(count) => {
            deleteMut.mutate({
              eventType: deleteTarget.eventType,
              eventDate: deleteTarget.eventDate,
              count,
              currentStatus: deleteTarget.currentStatus,
            })
            setDeleteTarget(null)
          }}
        />
      )}

      {moveTarget && (
        <MovePlantsDialog
          source={moveTarget}
          allLocations={trayLocations}
          onClose={() => setMoveTarget(null)}
          onConfirm={(targetId, count) => {
            moveMut.mutate({
              sourceId: moveTarget.trayLocationId ?? null,
              targetId,
              status: moveTarget.status,
              count,
            })
            setMoveTarget(null)
          }}
        />
      )}
    </div>
  )
}

function MovePlantsDialog({
  source,
  allLocations,
  onClose,
  onConfirm,
}: {
  source: PlantLocationGroup
  allLocations: { id: number; name: string }[]
  onClose: () => void
  onConfirm: (targetId: number | null, count: number) => void
}) {
  const others = allLocations.filter((l) => l.id !== source.trayLocationId)
  const [targetId, setTargetId] = useState<number | null>(null)
  const [detach, setDetach] = useState(false)
  const [countText, setCountText] = useState(String(source.count))
  const count = parseInt(countText, 10)
  const validCount = Number.isFinite(count) && count >= 1 && count <= source.count
  const canSubmit = validCount && (detach || targetId !== null)

  return (
    <Dialog
      open
      onClose={onClose}
      title="Flytta plantor"
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
          <button
            onClick={() => onConfirm(detach ? null : targetId, count)}
            disabled={!canSubmit}
            className="btn-primary text-sm"
          >
            Flytta
          </button>
        </>
      }
    >
      <div className="space-y-3">
        <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
          {source.trayLocationName ?? 'Utan plats'} · {STATUS_LABEL_PLURAL_SV[source.status] ?? source.status}
        </p>
        <div>
          <label className="field-label">Mål</label>
          <select
            className="input w-full"
            value={detach ? '__detach' : (targetId ?? '')}
            onChange={(e) => {
              const v = e.target.value
              if (v === '__detach') { setDetach(true); setTargetId(null) }
              else if (v === '') { setDetach(false); setTargetId(null) }
              else { setDetach(false); setTargetId(Number(v)) }
            }}
          >
            <option value="">Välj…</option>
            {others.map((l) => (
              <option key={l.id} value={l.id}>{l.name}</option>
            ))}
            <option value="__detach">Ingen plats (utan plats)</option>
          </select>
        </div>
        <div>
          <label className="field-label">Antal (max {source.count})</label>
          <input
            type="number"
            min={1}
            max={source.count}
            value={countText}
            onChange={(e) => setCountText(e.target.value.replace(/[^0-9]/g, ''))}
            className="input w-full"
          />
        </div>
      </div>
    </Dialog>
  )
}

function StatusCard({
  status,
  locations,
  events,
  onBedClick,
  onEventTap,
  onMoveTray,
}: {
  status: string
  locations: PlantLocationGroup[]
  events: SpeciesEventSummaryEntry[]
  onBedClick: (bedId: number) => void
  onEventTap: (target: EventTarget) => void
  onMoveTray: (loc: PlantLocationGroup) => void
}) {
  const total = locations.reduce((acc, l) => acc + l.count, 0)
  const label = STATUS_LABEL_PLURAL_SV[status] ?? status
  return (
    <section
      style={{
        border: '1px solid var(--color-ink)',
        borderRadius: 14,
        background: 'var(--color-paper)',
        overflow: 'hidden',
      }}
    >
      <header
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 10,
          padding: '14px 18px',
          borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
        }}
      >
        <span
          aria-hidden
          style={{
            display: 'inline-block', width: 10, height: 10, borderRadius: 999,
            background: STATUS_COLOR[status] ?? 'var(--color-forest)',
          }}
        />
        <span
          style={{
            flex: 1,
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
          }}
        >
          {label}
        </span>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 14, color: 'var(--color-ink)' }}>
          {total}
          <span style={{ fontSize: 9, letterSpacing: 1.2, color: 'var(--color-forest)' }}> ST</span>
        </span>
      </header>
      <div>
        {locations.map((loc, i) => {
          const isTray = loc.bedId == null
          const labelStr = isTray
            ? `Bricka${loc.trayLocationName ? ` · ${loc.trayLocationName}` : ''}`
            : [loc.gardenName, loc.bedName].filter(Boolean).join(' / ')
          const canMove = isTray
          return (
            <div key={`${loc.bedId ?? `tray_${loc.trayLocationId ?? 'none'}`}-${loc.year}-${i}`}>
              <div
                role="button"
                tabIndex={0}
                onClick={() => { if (loc.bedId != null) onBedClick(loc.bedId) }}
                onKeyDown={(e) => { if ((e.key === 'Enter' || e.key === ' ') && loc.bedId != null) onBedClick(loc.bedId) }}
                style={{
                  display: 'grid',
                  gridTemplateColumns: canMove ? '1.5fr 80px 60px 60px' : '1.5fr 80px 60px',
                  gap: 12,
                  padding: '12px 18px',
                  borderTop: i > 0 ? '1px solid color-mix(in srgb, var(--color-ink) 12%, transparent)' : 'none',
                  cursor: loc.bedId != null ? 'pointer' : 'default',
                  fontFamily: 'var(--font-display)',
                  fontSize: 16,
                }}
              >
                <span style={{ fontStyle: 'italic' }}>{labelStr || '—'}</span>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 10,
                    letterSpacing: 1.2,
                    color: 'var(--color-forest)',
                    textTransform: 'uppercase',
                    alignSelf: 'center',
                  }}
                >
                  {loc.year}
                </span>
                <span style={{ textAlign: 'right', alignSelf: 'center' }}>{loc.count}</span>
                {canMove && (
                  <button
                    onClick={(e) => { e.stopPropagation(); onMoveTray(loc) }}
                    style={{
                      background: 'none',
                      border: 'none',
                      color: 'var(--color-accent)',
                      fontFamily: 'var(--font-mono)',
                      fontSize: 11,
                      cursor: 'pointer',
                      textAlign: 'right',
                      padding: 0,
                    }}
                  >
                    Flytta
                  </button>
                )}
              </div>
              {isTray && (
                <TrayEventsExpansion
                  events={events}
                  currentStatus={loc.status}
                  onEventTap={onEventTap}
                />
              )}
            </div>
          )
        })}
      </div>
    </section>
  )
}

function TrayEventsExpansion({
  events,
  currentStatus,
  onEventTap,
}: {
  events: SpeciesEventSummaryEntry[]
  currentStatus: string
  onEventTap: (target: EventTarget) => void
}) {
  const grouped = new Map<string, EventRow>()
  for (const e of events) {
    const key = `${e.eventType}|${e.eventDate}|${e.fromLocationName ?? ''}|${e.toLocationName ?? ''}|${e.notes ?? ''}`
    const row = grouped.get(key) ?? {
      type: e.eventType, date: e.eventDate, current: 0, total: 0,
      fromLoc: e.fromLocationName ?? null,
      toLoc: e.toLocationName ?? null,
      notes: e.notes ?? null,
    }
    row.total += e.count
    if (e.currentStatus === currentStatus) row.current += e.count
    grouped.set(key, row)
  }
  const rows: EventRow[] = Array.from(grouped.values())
    .filter((r) => r.current > 0)
    .sort((a, b) => (b.date.localeCompare(a.date)) || a.type.localeCompare(b.type))

  const [showAll, setShowAll] = useState(false)
  const visible = rows.length <= 3 || showAll ? rows : rows.slice(0, 3)

  if (rows.length === 0) {
    return (
      <p
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 10,
          letterSpacing: 1.2,
          color: 'var(--color-forest)',
          padding: '6px 18px 14px 38px',
          margin: 0,
        }}
      >
        Inga händelser registrerade.
      </p>
    )
  }

  return (
    <div style={{ padding: '6px 18px 14px 38px', display: 'flex', flexDirection: 'column', gap: 6 }}>
      {visible.map((e, i) => {
        const isLatest = i === 0
        const countLabel = e.current < e.total ? `${e.current} (${e.total}) st` : `${e.current} st`
        return (
          <div
            key={`${e.type}-${e.date}-${i}`}
            role="button"
            tabIndex={0}
            onClick={() =>
              onEventTap({ eventType: e.type, eventDate: e.date, currentStatus, current: e.current })
            }
            onKeyDown={(ev) => {
              if (ev.key === 'Enter' || ev.key === ' ') {
                ev.preventDefault()
                onEventTap({ eventType: e.type, eventDate: e.date, currentStatus, current: e.current })
              }
            }}
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: 2,
              padding: '6px 0',
              cursor: 'pointer',
              borderRadius: 6,
            }}
          >
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '90px 1fr 110px',
                alignItems: 'center',
                gap: 12,
              }}
            >
              <span
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: isLatest ? 13 : 11,
                  fontWeight: isLatest ? 600 : 400,
                  color: isLatest ? 'var(--color-accent)' : 'var(--color-ink)',
                }}
              >
                {countLabel}
              </span>
              <span
                style={{
                  fontFamily: 'var(--font-display)',
                  fontStyle: 'italic',
                  fontSize: isLatest ? 17 : 14,
                  color: 'var(--color-ink)',
                }}
              >
                {labelForEventRow(e)}
              </span>
              <span
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: isLatest ? 11 : 10,
                  letterSpacing: 1.2,
                  textAlign: 'right',
                  color: isLatest ? 'var(--color-accent)' : 'var(--color-forest)',
                }}
              >
                {e.date}
              </span>
            </div>
            {e.notes && e.type !== 'NOTE' && (
              <div
                style={{
                  marginLeft: 102,
                  fontFamily: 'var(--font-display)',
                  fontStyle: 'italic',
                  fontSize: 13,
                  color: 'var(--color-forest)',
                }}
              >
                “{e.notes}”
              </div>
            )}
          </div>
        )
      })}
      {rows.length > 3 && (
        <button
          type="button"
          onClick={() => setShowAll((v) => !v)}
          style={{
            background: 'transparent',
            border: 'none',
            color: 'var(--color-accent)',
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: 1.2,
            textTransform: 'uppercase',
            cursor: 'pointer',
            alignSelf: 'flex-start',
            padding: 0,
          }}
        >
          {showAll ? 'Visa färre' : `Visa ${rows.length - 3} till`}
        </button>
      )}
    </div>
  )
}

function EditDateDialog({
  target,
  onClose,
  onSave,
}: {
  target: EventTarget
  onClose: () => void
  onSave: (newDate: string) => void
}) {
  const [date, setDate] = useState(target.eventDate)
  return (
    <Dialog open onClose={onClose} title="Ändra datum">
      <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, letterSpacing: 1.2, color: 'var(--color-forest)' }}>
        {EVENT_LABEL_SV[target.eventType] ?? target.eventType} · {target.eventDate}
      </p>
      <input
        type="date"
        className="input"
        value={date}
        onChange={(e) => setDate(e.target.value)}
        style={{ marginTop: 12, width: '100%' }}
      />
      <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
        <button className="btn-secondary" onClick={onClose}>Avbryt</button>
        <button className="btn-primary" disabled={!date} onClick={() => onSave(date)}>Spara</button>
      </div>
    </Dialog>
  )
}

function DeleteEventDialog({
  target,
  onClose,
  onConfirm,
}: {
  target: EventTarget
  onClose: () => void
  onConfirm: (count: number) => void
}) {
  const [countText, setCountText] = useState(String(target.current))
  const parsed = Number(countText)
  const valid = Number.isInteger(parsed) && parsed >= 1 && parsed <= target.current

  return (
    <Dialog open onClose={onClose} title="Ta bort händelse">
      <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, letterSpacing: 1.2, color: 'var(--color-forest)' }}>
        {EVENT_LABEL_SV[target.eventType] ?? target.eventType} · {target.eventDate}
      </p>
      <label style={{ marginTop: 12, display: 'block', fontSize: 13 }}>
        Antal plantor (max {target.current}):
      </label>
      <input
        type="number"
        className="input"
        min={1}
        max={target.current}
        value={countText}
        onChange={(e) => setCountText(e.target.value.replace(/[^\d]/g, ''))}
        style={{ marginTop: 4, width: '100%' }}
      />
      <p style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, color: 'var(--color-forest)', marginTop: 8 }}>
        Plantor som blir kvar utan händelser markeras som borttagna.
      </p>
      <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
        <button className="btn-secondary" onClick={onClose}>Avbryt</button>
        <button
          className="btn-primary"
          style={{ background: 'var(--color-accent)', borderColor: 'var(--color-accent)' }}
          disabled={!valid}
          onClick={() => valid && onConfirm(parsed)}
        >
          Ta bort
        </button>
      </div>
    </Dialog>
  )
}
