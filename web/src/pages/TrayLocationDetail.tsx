import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { api, type TraySummaryEntry } from '../api/client'
import { Masthead } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

export function TrayLocationDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const locationId = Number(id)

  const { data: locations = [] } = useQuery({
    queryKey: ['tray-locations'],
    queryFn: () => api.trayLocations.list(),
  })

  const { data: traySummary = [], error, isLoading, refetch } = useQuery({
    queryKey: ['tray-summary'],
    queryFn: () => api.plants.traySummary(),
  })

  const location = locations.find((l) => l.id === locationId)
  const entries = traySummary.filter((e) => e.trayLocationId === locationId)
  const totalCount = entries.reduce((acc, e) => acc + e.count, 0)
  const otherLocations = locations.filter((l) => l.id !== locationId)

  const [showWaterConfirm, setShowWaterConfirm] = useState(false)
  const [showNote, setShowNote] = useState(false)
  const [moveMode, setMoveMode] = useState(false)
  const [partialMove, setPartialMove] = useState<TraySummaryEntry | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [showEdit, setShowEdit] = useState(false)
  const [showDelete, setShowDelete] = useState(false)
  const [editName, setEditName] = useState('')

  const noteText = useState('')
  const [noteValue, setNoteValue] = noteText

  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['tray-locations'] })
    qc.invalidateQueries({ queryKey: ['tray-summary'] })
  }

  const waterMut = useMutation({
    mutationFn: () => api.trayLocations.water(locationId),
    onSuccess: (r) => { setInfo(`Vattnade · ${r.plantsAffected} plantor`); refresh() },
  })
  const noteMut = useMutation({
    mutationFn: (text: string) => api.trayLocations.note(locationId, text),
    onSuccess: (r) => { setInfo(`Anteckning · ${r.plantsAffected} plantor`); refresh() },
  })
  const moveMut = useMutation({
    mutationFn: (req: { targetLocationId: number | null; count: number; speciesId?: number; status?: string }) =>
      api.trayLocations.move(locationId, req),
    onSuccess: (r) => { setInfo(`Flyttade · ${r.plantsAffected} plantor`); refresh() },
  })
  const renameMut = useMutation({
    mutationFn: (newName: string) => api.trayLocations.update(locationId, newName),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tray-locations'] }); setShowEdit(false) },
  })
  const deleteMut = useMutation({
    mutationFn: () => api.trayLocations.delete(locationId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tray-locations'] })
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      navigate('/tray-locations')
    },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!location) {
    return (
      <div>
        <Masthead left="Plats" center="—" />
        <div className="page-body">
          <p>Den här platsen finns inte längre.</p>
          <button onClick={() => navigate('/tray-locations')} className="btn-primary mt-4">Tillbaka</button>
        </div>
      </div>
    )
  }

  const acting = waterMut.isPending || noteMut.isPending || moveMut.isPending

  return (
    <div>
      <Masthead
        left="Platser"
        center={`— ${location.name} —`}
        right={
          <button
            onClick={() => { setEditName(location.name); setShowEdit(true) }}
            aria-label="Redigera plats"
            title="Redigera plats"
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--color-forest)',
              cursor: 'pointer',
              fontSize: 18,
              padding: 4,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            ✎
          </button>
        }
      />

      <div className="page-body">
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 16, marginBottom: 16 }}>
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--color-forest)' }}>
            {totalCount} ST
          </span>
          {info && (
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-accent)' }}>
              {info}
            </span>
          )}
        </div>

        {moveMode ? (
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 24 }}>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-accent)', flex: 1 }}>
              Klicka en rad för att flytta
            </span>
            <button className="btn-secondary" onClick={() => setMoveMode(false)}>Avbryt</button>
          </div>
        ) : (
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 24 }}>
            <button
              className="btn-secondary"
              disabled={acting || totalCount === 0}
              onClick={() => setShowWaterConfirm(true)}
            >
              Vattna alla
            </button>
            <button
              className="btn-secondary"
              disabled={acting || totalCount === 0}
              onClick={() => { setNoteValue(''); setShowNote(true) }}
            >
              Anteckna
            </button>
            <button
              className="btn-secondary"
              disabled={acting || totalCount === 0}
              onClick={() => setMoveMode(true)}
            >
              Flytta
            </button>
          </div>
        )}

        <h3 style={{ fontFamily: 'var(--font-mono)', fontSize: 11, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', marginBottom: 8 }}>
          Plantor
        </h3>

        {entries.length === 0 ? (
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>—</p>
        ) : (
          <div>
            {entries.map((e) => {
              const clickable = moveMode || e.speciesId != null
              const handleClick = () => {
                if (moveMode) setPartialMove(e)
                else if (e.speciesId != null) navigate(`/species/${e.speciesId}/plants`)
              }
              return (
              <button
                key={`${e.speciesId}_${e.status}`}
                onClick={handleClick}
                disabled={!clickable}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1.5fr 80px 80px',
                  gap: 12,
                  padding: '10px 0',
                  width: '100%',
                  textAlign: 'left',
                  background: 'transparent',
                  border: 'none',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  cursor: clickable ? 'pointer' : 'default',
                  fontFamily: 'var(--font-display)',
                  fontSize: 16,
                }}
              >
                <span>{e.variantName ? `${e.speciesName} – ${e.variantName}` : e.speciesName}</span>
                <span style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{e.count}</span>
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, textAlign: 'right', textTransform: 'uppercase', letterSpacing: 1.2, color: 'var(--color-forest)' }}>
                  {e.status}
                </span>
              </button>
              )
            })}
          </div>
        )}
      </div>

      <Dialog
        open={showWaterConfirm}
        onClose={() => setShowWaterConfirm(false)}
        title="Vattna alla?"
        actions={
          <>
            <button onClick={() => setShowWaterConfirm(false)} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
            <button
              onClick={() => { waterMut.mutate(); setShowWaterConfirm(false) }}
              className="btn-primary text-sm"
            >
              Vattna
            </button>
          </>
        }
      >
        <p>Vattnar {totalCount} plantor i {location.name}.</p>
      </Dialog>

      <Dialog
        open={showNote}
        onClose={() => setShowNote(false)}
        title="Anteckna"
        actions={
          <>
            <button onClick={() => setShowNote(false)} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
            <button
              onClick={() => {
                const t = noteValue.trim()
                if (t) { noteMut.mutate(t); setShowNote(false) }
              }}
              disabled={!noteValue.trim()}
              className="btn-primary text-sm"
            >
              Spara
            </button>
          </>
        }
      >
        <textarea
          value={noteValue}
          onChange={(e) => setNoteValue(e.target.value)}
          rows={3}
          autoFocus
          className="input w-full"
          placeholder="Anteckning…"
        />
      </Dialog>

      <MoveDialog
        open={partialMove !== null}
        onClose={() => setPartialMove(null)}
        title={partialMove ? (partialMove.variantName ? `${partialMove.speciesName} – ${partialMove.variantName}` : partialMove.speciesName) : ''}
        sourceCount={partialMove?.count ?? 0}
        otherLocations={otherLocations}
        onSubmit={(targetId, count) => {
          if (partialMove) {
            moveMut.mutate({
              targetLocationId: targetId,
              count,
              speciesId: partialMove.speciesId,
              status: partialMove.status,
            })
          }
          setPartialMove(null)
          setMoveMode(false)
        }}
      />

      <Dialog
        open={showEdit}
        onClose={() => setShowEdit(false)}
        title="Redigera plats"
        actions={
          <>
            <button onClick={() => setShowEdit(false)} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
            <button
              onClick={() => renameMut.mutate(editName.trim())}
              disabled={!editName.trim() || editName.trim() === location.name || renameMut.isPending}
              className="btn-primary text-sm"
            >
              {renameMut.isPending ? 'Sparar…' : 'Spara'}
            </button>
          </>
        }
      >
        <div className="space-y-3">
          <div>
            <label className="field-label">Namn *</label>
            <input
              type="text"
              autoFocus
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              className="input w-full"
            />
          </div>
          <button
            onClick={() => { setShowEdit(false); setShowDelete(true) }}
            className="text-sm text-error hover:underline"
          >
            Ta bort plats
          </button>
        </div>
      </Dialog>

      <Dialog
        open={showDelete}
        onClose={() => setShowDelete(false)}
        title="Ta bort plats"
        actions={
          <>
            <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
            <button
              onClick={() => deleteMut.mutate()}
              className="px-4 py-2 text-sm text-error font-semibold"
              disabled={deleteMut.isPending}
            >
              Ta bort
            </button>
          </>
        }
      >
        <p className="text-text-secondary">
          {location.activePlantCount > 0
            ? `${location.activePlantCount} plantor i ${location.name} blir utan plats. Fortsätt?`
            : `Ta bort ${location.name}?`}
        </p>
      </Dialog>
    </div>
  )
}

function MoveDialog(props: {
  open: boolean
  onClose: () => void
  title: string
  sourceCount: number
  otherLocations: { id: number; name: string }[]
  onSubmit: (targetId: number | null, count: number) => void
}) {
  const [targetId, setTargetId] = useState<number | null>(null)
  const [detach, setDetach] = useState(false)
  const [countText, setCountText] = useState(String(props.sourceCount))

  const count = parseInt(countText, 10)
  const validCount = Number.isFinite(count) && count >= 1 && count <= props.sourceCount
  const canSubmit = validCount && (detach || targetId !== null)

  return (
    <Dialog
      open={props.open}
      onClose={props.onClose}
      title="Flytta plantor"
      actions={
        <>
          <button onClick={props.onClose} className="px-4 py-2 text-sm text-text-secondary">Avbryt</button>
          <button
            onClick={() => props.onSubmit(detach ? null : targetId, count)}
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
          {props.title}
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
            {props.otherLocations.map((l) => (
              <option key={l.id} value={l.id}>{l.name}</option>
            ))}
            <option value="__detach">Ingen plats (utan plats)</option>
          </select>
        </div>

        <div>
          <label className="field-label">Antal (max {props.sourceCount})</label>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <input
              type="number"
              min={1}
              max={props.sourceCount}
              value={countText}
              onChange={(e) => setCountText(e.target.value.replace(/[^0-9]/g, ''))}
              className="input"
              style={{ flex: 1 }}
            />
            <button
              type="button"
              onClick={() => setCountText(String(props.sourceCount))}
              style={{
                background: 'none',
                border: '1px solid var(--color-accent)',
                color: 'var(--color-accent)',
                fontFamily: 'var(--font-mono)',
                fontSize: 11,
                padding: '6px 12px',
                cursor: 'pointer',
              }}
            >
              Alla
            </button>
          </div>
        </div>
      </div>
    </Dialog>
  )
}
