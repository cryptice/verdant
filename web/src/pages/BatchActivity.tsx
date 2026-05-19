import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type PlantGroupResponse } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Snackbar, useSnackbar } from '../components/Snackbar'

type BatchKind = 'pot-up' | 'plant-out'

const KIND_CONFIG: Record<BatchKind, {
  titleKey: string
  fromStatus: string
  toEventType: string
  needsTargetBed: boolean
}> = {
  'pot-up':   { titleKey: 'batch.potUpTitle',   fromStatus: 'SEEDED',     toEventType: 'POTTED_UP',   needsTargetBed: false },
  'plant-out':{ titleKey: 'batch.plantOutTitle',fromStatus: 'POTTED_UP',  toEventType: 'PLANTED_OUT', needsTargetBed: true  },
}

export function BatchActivity() {
  const { kind: kindStr } = useParams<{ kind: BatchKind }>()
  const kind = kindStr as BatchKind
  if (!kind || !KIND_CONFIG[kind]) {
    return <div className="page-body"><p>Unknown batch activity.</p></div>
  }
  return <BatchActivityInner kind={kind} />
}

function BatchActivityInner({ kind }: { kind: BatchKind }) {
  const cfg = KIND_CONFIG[kind]
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const groupsQuery = useQuery({
    queryKey: ['plant-groups', cfg.fromStatus],
    queryFn: () => api.plants.groups(cfg.fromStatus, /* trayOnly */ kind === 'pot-up'),
  })

  const bedsQuery = useQuery({
    queryKey: ['beds'],
    queryFn: () => api.beds.list(),
    enabled: cfg.needsTargetBed,
  })

  const [selected, setSelected] = useState<PlantGroupResponse | null>(null)
  const [count, setCount] = useState('')
  const [targetBedId, setTargetBedId] = useState<number | ''>('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const { message: toast, show: showToast } = useSnackbar()

  const submitMut = useMutation({
    mutationFn: () => api.plants.batchEvent({
      speciesId: selected!.speciesId,
      bedId: selected!.bedId,
      plantedDate: selected!.plantedDate,
      status: cfg.fromStatus,
      eventType: cfg.toEventType,
      count: Number(count),
      notes: notes.trim() || undefined,
      targetBedId: cfg.needsTargetBed ? (targetBedId === '' ? undefined : Number(targetBedId)) : undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['plant-groups'] })
      qc.invalidateQueries({ queryKey: ['plants'] })
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      if (notes.trim()) {
        api.comments.record(notes.trim()).catch(() => { /* best-effort */ })
      }
      navigate('/plants')
    },
    onError: (err) => {
      const msg = err instanceof Error ? err.message : String(err)
      setError(msg)
      showToast(msg)
    },
  })

  if (groupsQuery.isLoading) {
    return (
      <div className="flex justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (groupsQuery.error) return <ErrorDisplay error={groupsQuery.error} onRetry={groupsQuery.refetch} />

  const groups = groupsQuery.data ?? []
  const countNum = parseInt(count, 10)
  const valid =
    !!selected &&
    Number.isFinite(countNum) && countNum >= 1 && countNum <= selected.count &&
    (!cfg.needsTargetBed || targetBedId !== '')

  return (
    <div>
      <Masthead
        left={
          <Link to="/plants" className="text-sm text-text-secondary hover:text-accent">
            ← {t('nav.plants')}
          </Link>
        }
        center={t(cfg.titleKey)}
      />

      <div className="page-body">
        {groups.length === 0 ? (
          <div className="empty-state">
            <p className="text-text-secondary">{t('batch.noGroups')}</p>
          </div>
        ) : (
          <Ledger
            columns={[
              { key: 'species', label: t('common.speciesLabel'), width: '1.4fr',
                render: (g: PlantGroupResponse) => (
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
                    {g.variantName ? `${g.speciesName} — ${g.variantName}` : g.speciesName}
                  </span>
                ),
              },
              { key: 'where', label: t('batch.location'), width: '1fr',
                render: (g: PlantGroupResponse) => (
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
                    {g.bedName ?? '—'}
                    {g.gardenName ? ` · ${g.gardenName}` : ''}
                  </span>
                ),
              },
              { key: 'date', label: t('batch.plantedDate'), width: '120px',
                render: (g: PlantGroupResponse) => (
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11 }}>
                    {g.plantedDate ?? '—'}
                  </span>
                ),
              },
              { key: 'count', label: t('batch.count'), width: '90px', align: 'right',
                render: (g: PlantGroupResponse) => (
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 14 }}>{g.count}</span>
                ),
              },
            ]}
            rows={groups}
            rowKey={(g) => `${g.speciesId}_${g.bedId ?? 'none'}_${g.plantedDate ?? 'none'}`}
            onRowClick={(g) => {
              setSelected(g)
              setCount(String(g.count))
              setTargetBedId('')
              setNotes('')
              setError(null)
            }}
          />
        )}
      </div>

      <Dialog
        open={selected !== null}
        onClose={() => setSelected(null)}
        title={t(cfg.titleKey)}
        actions={
          <>
            <button onClick={() => setSelected(null)} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button
              onClick={() => valid && submitMut.mutate()}
              disabled={!valid || submitMut.isPending}
              className="btn-primary text-sm"
            >
              {submitMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        {selected && (
          <div className="space-y-4">
            <p className="text-sm">
              <strong>
                {selected.variantName ? `${selected.speciesName} — ${selected.variantName}` : selected.speciesName}
              </strong>
              {' · '}
              {selected.bedName ?? t('common.none')}
            </p>
            <div>
              <label className="field-label">
                {t('batch.count')} * (max {selected.count})
              </label>
              <input
                type="number"
                min={1}
                max={selected.count}
                value={count}
                onChange={(e) => setCount(e.target.value.replace(/[^\d]/g, ''))}
                className="input"
                autoFocus
              />
            </div>
            {cfg.needsTargetBed && (
              <div>
                <label className="field-label">{t('batch.targetBed')} *</label>
                <select
                  value={targetBedId}
                  onChange={(e) => setTargetBedId(e.target.value ? Number(e.target.value) : '')}
                  className="input"
                >
                  <option value="">{t('common.select')}</option>
                  {(bedsQuery.data ?? []).map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.name}{b.gardenName ? ` · ${b.gardenName}` : ''}
                    </option>
                  ))}
                </select>
              </div>
            )}
            <div>
              <label className="field-label">{t('common.notesLabel')}</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={2}
                placeholder={t('common.optional')}
                className="input"
              />
            </div>
            {error && <p className="text-error text-sm">{error}</p>}
          </div>
        )}
      </Dialog>

      <Snackbar message={toast} />
    </div>
  )
}
