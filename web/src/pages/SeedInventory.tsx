import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SeedInventoryResponse, type SpeciesResponse } from '../api/client'
import { Masthead, LedgerPagination } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SeedBatchDialog } from '../components/SeedBatchDialog'

const SPECIES_TEMPLATE = '60px 1.5fr 120px 40px'
const BATCH_TEMPLATE = '1.5fr 100px 100px 120px 40px'

const HEADER_STYLE: React.CSSProperties = {
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
}

export function SeedInventory() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const { t } = useTranslation()

  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['seed-inventory'],
    queryFn: () => api.inventory.list(),
  })

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })

  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const toggle = (speciesId: number) => setExpanded(prev => {
    const next = new Set(prev)
    if (next.has(speciesId)) next.delete(speciesId)
    else next.add(speciesId)
    return next
  })

  // Undefined batch means adding; the key remounts the form so it starts fresh.
  const [dialog, setDialog] = useState<{ batch?: SeedInventoryResponse; initialSpecies?: SpeciesResponse | null; key: number } | null>(null)
  const openDialog = (opts: { batch?: SeedInventoryResponse; initialSpecies?: SpeciesResponse | null } = {}) =>
    setDialog({ ...opts, key: Date.now() })

  // Deep-link: /seed-stock?add=1&speciesId=N opens the dialog preselected to
  // that species (e.g. from the sow page hint). Clear the query so reopening
  // the page isn't sticky.
  useEffect(() => {
    if (searchParams.get('add') !== '1') return
    const idParam = searchParams.get('speciesId')
    const id = idParam ? Number(idParam) : NaN
    const sp = Number.isFinite(id) ? species?.find(s => s.id === id) ?? null : null
    if (idParam && !species) return // wait for species list
    openDialog({ initialSpecies: sp })
    setSearchParams({}, { replace: true })
  }, [searchParams, species, setSearchParams])

  const [deleteItem, setDeleteItem] = useState<SeedInventoryResponse | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.inventory.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seed-inventory'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  const [page, setPage] = useState(0)
  const pageSize = 50

  // Group batches by speciesId (computed every render so the effect below can
  // watch the group count without depending on loading/error state).
  const items = data ?? []
  const grouped = new Map<number, SeedInventoryResponse[]>()
  for (const item of items) {
    const group = grouped.get(item.speciesId) ?? []
    group.push(item)
    grouped.set(item.speciesId, group)
  }
  const groupEntries = Array.from(grouped.entries())

  useEffect(() => { setPage(0) }, [groupEntries.length])

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <Masthead
        left={t('nav.seeds')}
        center="— Utsädesliggaren —"
        right={
          <>
            <Link to="/seed-stock/review" className="btn-secondary" style={{ marginRight: 8 }}>{t('seedReview.title')}</Link>
            <button onClick={() => openDialog()} className="btn-primary" data-onboarding="add-seed-btn">
              {t('seeds.addSeeds')}
            </button>
          </>
        }
      />
      <div className="page-body">
        {items.length === 0 && (
          <div className="empty-state">
            <div style={{ ...HEADER_STYLE, opacity: 1, marginBottom: 16 }}>{t('seeds.noBatchesTitle')}</div>
            <p style={{ fontFamily: 'var(--font-display)', fontSize: 16, color: 'var(--color-forest)', marginBottom: 20 }}>{t('seeds.noBatchesHint')}</p>
            <button onClick={() => openDialog()} className="btn-primary">{t('seeds.addSeeds')}</button>
          </div>
        )}

        {items.length > 0 && (
          <div>
            {/* Outer header */}
            <div className="ledger-head" style={{ display: 'grid', gridTemplateColumns: SPECIES_TEMPLATE, gap: 18, padding: '10px 0', borderBottom: '1px solid var(--color-ink)' }}>
              <span style={HEADER_STYLE}>№</span>
              <span style={HEADER_STYLE}>{t('seeds.colName')}</span>
              <span style={{ ...HEADER_STYLE, textAlign: 'right' }}>{t('seeds.colSeeds')}</span>
              <span />
            </div>

            {groupEntries.slice(page * pageSize, (page + 1) * pageSize).map(([speciesId, batches], i) => {
              const globalIndex = page * pageSize + i
              const sp = species?.find(s => s.id === speciesId)
              const baseName = sp?.commonNameSv ?? sp?.commonName ?? batches[0].speciesName
              const variant = sp?.variantNameSv ?? sp?.variantName
              const speciesDisplayName = variant ? `${baseName} – ${variant}` : baseName
              const scientificName = sp?.scientificName
              const totalQuantity = batches.reduce((sum, b) => sum + (b.quantity ?? 0), 0)
              const isExpanded = expanded.has(speciesId)

              return (
                <div key={speciesId}>
                  {/* Species group row */}
                  <button
                    onClick={() => toggle(speciesId)}
                    style={{
                      display: 'grid',
                      gridTemplateColumns: SPECIES_TEMPLATE,
                      gap: 18,
                      padding: '14px 0',
                      borderBottom: '1px solid var(--color-ink)',
                      width: '100%',
                      background: 'transparent',
                      border: 'none',
                      borderBottomWidth: 1,
                      borderBottomStyle: 'solid',
                      borderBottomColor: 'var(--color-ink)',
                      textAlign: 'left',
                      cursor: 'pointer',
                      alignItems: 'center',
                      '--mobile-cols': 'minmax(0, 1fr) auto auto',
                    } as React.CSSProperties}
                    className="ledger-row ledger-grid"
                  >
                    <span className="mobile-hidden" style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-mustard)' }}>
                      {String(globalIndex + 1).padStart(2, '0')}
                    </span>
                    <div>
                      <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{speciesDisplayName}</div>
                      {scientificName && (
                        <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 9, color: 'var(--color-sage)' }}>
                          {scientificName}
                        </div>
                      )}
                    </div>
                    <span style={{ fontVariantNumeric: 'tabular-nums', fontSize: 20, fontFamily: 'var(--font-display)', textAlign: 'right' }}>
                      {totalQuantity}
                    </span>
                    <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--color-forest)' }}>
                      {isExpanded ? '▼' : '▶'}
                    </span>
                  </button>

                  {/* Expanded batch rows */}
                  {isExpanded && (
                    <div className="mobile-no-indent" style={{ padding: '10px 0 10px 78px' }}>
                      {/* Batch header */}
                      <div className="ledger-head" style={{ display: 'grid', gridTemplateColumns: BATCH_TEMPLATE, gap: 12, padding: '6px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}>
                        <span style={HEADER_STYLE}>{t('seeds.colName')}</span>
                        <span style={{ ...HEADER_STYLE, textAlign: 'right' }}>{t('seeds.colSeeds')}</span>
                        <span style={{ ...HEADER_STYLE, textAlign: 'right' }}>{t('seeds.colCost')}</span>
                        <span style={HEADER_STYLE}>{t('seeds.colExpires')}</span>
                        <span />
                      </div>
                      {batches.map(batch => {
                        const batchVariant = sp?.variantNameSv ?? sp?.variantName
                        const batchLabel = batchVariant ?? batch.providerName ?? '—'
                        return (
                          <div
                            key={batch.id}
                            className="ledger-grid"
                            style={{
                              display: 'grid',
                              gridTemplateColumns: BATCH_TEMPLATE,
                              gap: 12,
                              padding: '8px 0',
                              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 15%, transparent)',
                              alignItems: 'center',
                              '--mobile-cols': 'minmax(0, 1fr) auto auto auto',
                            } as React.CSSProperties}
                          >
                            <button
                              onClick={() => batch.quantity > 0 && navigate(`/sow?speciesId=${batch.speciesId}&seedBatchId=${batch.id}`)}
                              style={{
                                fontFamily: 'var(--font-display)',
                                fontSize: 14,
                                background: 'transparent',
                                border: 'none',
                                cursor: batch.quantity > 0 ? 'pointer' : 'default',
                                textAlign: 'left',
                                padding: 0,
                                color: 'var(--color-ink)',
                              }}
                            >
                              {batchLabel}
                            </button>
                            <span style={{ fontFamily: 'var(--font-mono)', fontVariantNumeric: 'tabular-nums', fontSize: 14, textAlign: 'right' }}>
                              {batch.quantity}
                            </span>
                            <span className="mobile-hidden" style={{ fontFamily: 'var(--font-mono)', fontVariantNumeric: 'tabular-nums', fontSize: 10, textAlign: 'right' }}>
                              {batch.costPerUnitCents != null ? `${(batch.costPerUnitCents / 100).toFixed(2)} kr` : '—'}
                            </span>
                            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
                              {batch.expirationDate ?? '—'}
                            </span>
                            <button
                              onClick={() => openDialog({ batch })}
                              style={{ background: 'transparent', border: 'none', cursor: 'pointer', color: 'var(--color-forest)', padding: 0 }}
                              aria-label={t('common.edit')}
                              title={t('common.edit')}
                            >
                              <svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M11.5 2.5l2 2M2 14l1-4L11 2l2 2-8 8-3 1z" />
                              </svg>
                            </button>
                          </div>
                        )
                      })}
                    </div>
                  )}
                </div>
              )
            })}
            <LedgerPagination page={page} pageSize={pageSize} total={groupEntries.length} onChange={setPage} />
          </div>
        )}
      </div>

      {dialog && (
        <SeedBatchDialog
          key={dialog.key}
          batch={dialog.batch}
          initialSpecies={dialog.initialSpecies}
          species={species}
          onClose={() => setDialog(null)}
          onDelete={batch => { setDialog(null); setDeleteItem(batch) }}
        />
      )}

      <Dialog
        open={deleteItem !== null}
        onClose={() => { setDeleteItem(null); setDeleteError(null) }}
        title={t('seeds.deleteBatchTitle')}
        actions={
          <>
            <button onClick={() => { setDeleteItem(null); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button onClick={() => deleteItem && deleteMut.mutate(deleteItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
          </>
        }
      >
        <p className="text-text-secondary">{t('seeds.deleteBatchConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>
    </div>
  )
}
