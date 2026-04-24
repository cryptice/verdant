import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SeedInventoryResponse, type SpeciesResponse } from '../api/client'
import { Masthead, LedgerPagination } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { useOnboarding } from '../onboarding/OnboardingContext'

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
  const { completeStep } = useOnboarding()

  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['seed-inventory'],
    queryFn: () => api.inventory.list(),
  })

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })

  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const toggle = (speciesId: number) => setExpanded(prev => {
    const next = new Set(prev)
    next.has(speciesId) ? next.delete(speciesId) : next.add(speciesId)
    return next
  })

  const [showAdd, setShowAdd] = useState(false)
  const [addSpecies, setAddSpecies] = useState<SpeciesResponse | null>(null)
  const [addPerPackage, setAddPerPackage] = useState('')
  const [addPackages, setAddPackages] = useState('')
  const addQuantity = (Number(addPerPackage) || 0) * (Number(addPackages) || 0)
  const [addCollection, setAddCollection] = useState('')
  const [addExpiration, setAddExpiration] = useState('')
  const [addCostUnit, setAddCostUnit] = useState('')
  const [addCostPackage, setAddCostPackage] = useState('')
  const [addUnitType, setAddUnitType] = useState('SEED')
  const [addProviderId, setAddProviderId] = useState<number | ''>('')

  // Deep-link: /seed-stock?add=1&speciesId=N opens the dialog preselected to
  // that species (e.g. from the sow page hint). Clear the query so reopening
  // the page isn't sticky.
  useEffect(() => {
    if (searchParams.get('add') !== '1') return
    const idParam = searchParams.get('speciesId')
    const id = idParam ? Number(idParam) : NaN
    const sp = Number.isFinite(id) ? species?.find(s => s.id === id) ?? null : null
    if (idParam && !species) return // wait for species list
    setShowAdd(true)
    setAddSpecies(sp)
    if (sp) {
      setAddProviderId(sp.providers.length > 0 ? sp.providers[0].id : '')
      setAddUnitType(sp.defaultUnitType ?? 'SEED')
    }
    setSearchParams({}, { replace: true })
  }, [searchParams, species, setSearchParams])

  const [deleteItem, setDeleteItem] = useState<SeedInventoryResponse | null>(null)
  const [editItem, setEditItem] = useState<SeedInventoryResponse | null>(null)
  const [editQuantity, setEditQuantity] = useState('')
  const [editCollection, setEditCollection] = useState('')
  const [editExpiration, setEditExpiration] = useState('')
  const [editCostUnit, setEditCostUnit] = useState('')
  const [editCostPackage, setEditCostPackage] = useState('')
  const [editUnitType, setEditUnitType] = useState('')
  const [editProviderId, setEditProviderId] = useState<number | ''>('')

  const createMut = useMutation({
    mutationFn: () => api.inventory.create({
      speciesId: addSpecies!.id,
      quantity: addQuantity,
      collectionDate: addCollection || undefined,
      expirationDate: addExpiration || undefined,
      costPerUnitSek: addCostUnit ? Math.round(Number(addCostUnit) * 100) : undefined,
      unitType: addUnitType || undefined,
      speciesProviderId: addProviderId !== '' ? addProviderId : undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      setShowAdd(false); setAddSpecies(null); setAddPerPackage(''); setAddPackages(''); setAddCollection(''); setAddExpiration('')
      setAddCostUnit(''); setAddCostPackage(''); setAddUnitType('SEED'); setAddProviderId('')
      completeStep('add_seeds')
    },
  })

  const [updateError, setUpdateError] = useState<string | null>(null)
  const [deleteError, setDeleteError] = useState<string | null>(null)

  const updateMut = useMutation({
    mutationFn: () => api.inventory.update(editItem!.id, {
      quantity: Number(editQuantity),
      collectionDate: editCollection || undefined,
      expirationDate: editExpiration || undefined,
      costPerUnitSek: editCostUnit ? Math.round(Number(editCostUnit) * 100) : undefined,
      unitType: editUnitType || undefined,
      speciesProviderId: editProviderId !== '' ? editProviderId : undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      setEditItem(null); setUpdateError(null)
    },
    onError: (err) => { setUpdateError(err instanceof Error ? err.message : String(err)) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.inventory.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['seed-inventory'] }); setDeleteItem(null); setDeleteError(null) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  const openEditItem = (item: SeedInventoryResponse) => {
    setEditItem(item)
    setEditQuantity(String(item.quantity))
    setEditCollection(item.collectionDate ?? '')
    setEditExpiration(item.expirationDate ?? '')
    setEditCostUnit(item.costPerUnitSek != null ? String(item.costPerUnitSek / 100) : '')
    setEditCostPackage(item.costPerUnitSek != null && item.quantity ? String((item.costPerUnitSek * item.quantity / 100).toFixed(2)) : '')
    setEditUnitType(item.unitType ?? '')
    setEditProviderId(item.speciesProviderId ?? '')
  }

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
          <button onClick={() => setShowAdd(true)} className="btn-primary" data-onboarding="add-seed-btn">
            {t('seeds.addSeeds')}
          </button>
        }
      />
      <div style={{ padding: '28px 40px' }}>
        {items.length === 0 && (
          <div style={{ padding: '40px 22px', textAlign: 'center', borderBottom: '1px solid var(--color-ink)', borderTop: '1px solid var(--color-ink)' }}>
            <div style={{ ...HEADER_STYLE, opacity: 1, marginBottom: 16 }}>{t('seeds.noBatchesTitle')}</div>
            <p style={{ fontFamily: 'var(--font-display)', fontSize: 16, color: 'var(--color-forest)', marginBottom: 20 }}>{t('seeds.noBatchesHint')}</p>
            <button onClick={() => setShowAdd(true)} className="btn-primary">{t('seeds.addSeeds')}</button>
          </div>
        )}

        {items.length > 0 && (
          <div>
            {/* Outer header */}
            <div style={{ display: 'grid', gridTemplateColumns: SPECIES_TEMPLATE, gap: 18, padding: '10px 0', borderBottom: '1px solid var(--color-ink)' }}>
              <span style={HEADER_STYLE}>№</span>
              <span style={HEADER_STYLE}>{t('seeds.colName')}</span>
              <span style={{ ...HEADER_STYLE, textAlign: 'right' }}>{t('seeds.colSeeds')}</span>
              <span />
            </div>

            {groupEntries.slice(page * pageSize, (page + 1) * pageSize).map(([speciesId, batches], i) => {
              const globalIndex = page * pageSize + i
              const sp = species?.find(s => s.id === speciesId)
              const speciesDisplayName = sp?.commonNameSv ?? sp?.commonName ?? batches[0].speciesName
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
                    }}
                    className="ledger-row"
                  >
                    <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-mustard)' }}>
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
                    <div style={{ padding: '10px 0 10px 78px' }}>
                      {/* Batch header */}
                      <div style={{ display: 'grid', gridTemplateColumns: BATCH_TEMPLATE, gap: 12, padding: '6px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}>
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
                            style={{
                              display: 'grid',
                              gridTemplateColumns: BATCH_TEMPLATE,
                              gap: 12,
                              padding: '8px 0',
                              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 15%, transparent)',
                              alignItems: 'center',
                            }}
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
                            <span style={{ fontFamily: 'var(--font-mono)', fontVariantNumeric: 'tabular-nums', fontSize: 10, textAlign: 'right' }}>
                              {batch.costPerUnitSek != null ? `${(batch.costPerUnitSek / 100).toFixed(2)} kr` : '—'}
                            </span>
                            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
                              {batch.expirationDate ?? '—'}
                            </span>
                            <button
                              onClick={() => openEditItem(batch)}
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

      <Dialog
        open={showAdd}
        onClose={() => { setShowAdd(false); setAddSpecies(null); setAddPerPackage(''); setAddPackages(''); setAddCollection(''); setAddExpiration(''); setAddCostUnit(''); setAddCostPackage(''); setAddUnitType('SEED'); setAddProviderId('') }}
        title={t('seeds.addSeedsTitle')}
        actions={
          <>
            <button
              onClick={() => { setShowAdd(false); setAddSpecies(null); setAddPerPackage(''); setAddPackages(''); setAddCollection(''); setAddExpiration(''); setAddCostUnit(''); setAddCostPackage(''); setAddUnitType('SEED'); setAddProviderId('') }}
              className="px-4 py-2 text-sm text-text-secondary"
            >
              {t('common.cancel')}
            </button>
            <button
              onClick={() => createMut.mutate()}
              disabled={!addSpecies || !addPerPackage || !addPackages || !addUnitType || createMut.isPending}
              className="btn-primary text-sm"
            >
              {createMut.isPending ? t('species.adding') : t('common.add')}
            </button>
          </>
        }
      >
        <div data-onboarding="seed-form" className="space-y-4">
          <div>
            <label className="field-label">{t('common.speciesLabel')}</label>
            <SpeciesAutocomplete value={addSpecies} onChange={s => { setAddSpecies(s); setAddProviderId(s && s.providers.length > 0 ? s.providers[0].id : ''); setAddUnitType(s?.defaultUnitType ?? 'SEED') }} />
          </div>
          <div>
            <label className="field-label">{t('seeds.provider')}</label>
            <select
              value={addProviderId}
              onChange={e => setAddProviderId(e.target.value ? Number(e.target.value) : '')}
              disabled={!addSpecies}
              className="input w-full"
            >
              <option value="">{addSpecies ? t('common.none') : t('common.select')}</option>
              {addSpecies?.providers.map(p => (
                <option key={p.id} value={p.id}>{p.providerName}</option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="field-label">{t('seeds.perPackageLabel')}</label>
              <input type="number" min="0" value={addPerPackage} onChange={e => {
                const v = e.target.value
                setAddPerPackage(v)
                const per = Number(v) || 0
                if (addCostPackage && per) setAddCostUnit(String(Math.round((Number(addCostPackage) / per) * 100) / 100))
              }} placeholder="e.g. 50" className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('seeds.packagesLabel')}</label>
              <input type="number" min="0" value={addPackages} onChange={e => setAddPackages(e.target.value)} placeholder="e.g. 2" className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('seeds.unitType')} *</label>
              <select value={addUnitType} onChange={e => setAddUnitType(e.target.value)} className="input w-full">
                <option value="">{t('common.select')}</option>
                {['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT'].map(ut => (
                  <option key={ut} value={ut}>{t(`unitTypes.${ut}`)}</option>
                ))}
              </select>
            </div>
          </div>
          <p className="text-xs text-text-secondary -mt-2">
            {t('seeds.totalLabel')}: {addQuantity} {t(`unitTypes.${addUnitType || 'SEED'}`).toLowerCase()}
          </p>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('seeds.collectionDate')}</label>
              <input type="date" value={addCollection} onChange={e => setAddCollection(e.target.value)} className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('seeds.expirationDate')}</label>
              <input type="date" value={addExpiration} onChange={e => setAddExpiration(e.target.value)} className="input w-full" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('seeds.costPerUnit')}</label>
              <input type="number" value={addCostUnit} onChange={e => {
                const v = e.target.value
                setAddCostUnit(v)
                const per = Number(addPerPackage) || 0
                setAddCostPackage(v && per ? String(Math.round(Number(v) * per * 100) / 100) : '')
              }} placeholder="e.g. 2" className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('seeds.costPerPackage')}</label>
              <input type="number" value={addCostPackage} onChange={e => {
                const v = e.target.value
                setAddCostPackage(v)
                const per = Number(addPerPackage) || 0
                setAddCostUnit(v && per ? String(Math.round((Number(v) / per) * 100) / 100) : '')
              }} placeholder="e.g. 100" className="input w-full" />
            </div>
          </div>
          <p className="text-xs text-text-secondary -mt-2">{t('seeds.costHint')}</p>
        </div>
      </Dialog>

      <Dialog
        open={editItem !== null}
        onClose={() => { setEditItem(null); setUpdateError(null) }}
        title={t('seeds.editSeedsTitle')}
        actions={
          <>
            <button onClick={() => { setEditItem(null); setUpdateError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => updateMut.mutate()}
              disabled={!editQuantity || !editUnitType || updateMut.isPending}
              className="btn-primary text-sm"
            >
              {updateMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {(() => {
            const sp = editItem ? species?.find(s => s.id === editItem.speciesId) : null
            return (
              <div>
                <label className="field-label">{t('seeds.provider')}</label>
                <select
                  value={editProviderId}
                  onChange={e => setEditProviderId(e.target.value ? Number(e.target.value) : '')}
                  disabled={!sp}
                  className="input w-full"
                >
                  <option value="">{sp ? t('common.none') : t('common.select')}</option>
                  {sp?.providers.map(p => (
                    <option key={p.id} value={p.id}>{p.providerName}</option>
                  ))}
                </select>
              </div>
            )
          })()}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('seeds.quantityLabel')}</label>
              <input type="number" value={editQuantity} onChange={e => {
                const v = e.target.value
                setEditQuantity(v)
                const qty = Number(v)
                if (editCostPackage && qty) setEditCostUnit(String(Math.round((Number(editCostPackage) / qty) * 100) / 100))
              }} className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('seeds.unitType')} *</label>
              <select value={editUnitType} onChange={e => setEditUnitType(e.target.value)} className="input w-full">
                <option value="">{t('common.select')}</option>
                {['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT'].map(ut => (
                  <option key={ut} value={ut}>{t(`unitTypes.${ut}`)}</option>
                ))}
              </select>
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('seeds.collectionDate')}</label>
              <input type="date" value={editCollection} onChange={e => setEditCollection(e.target.value)} className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('seeds.expirationDate')}</label>
              <input type="date" value={editExpiration} onChange={e => setEditExpiration(e.target.value)} className="input w-full" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('seeds.costPerUnit')}</label>
              <input type="number" value={editCostUnit} onChange={e => {
                const v = e.target.value
                setEditCostUnit(v)
                const qty = Number(editQuantity)
                setEditCostPackage(v && qty ? String(Math.round(Number(v) * qty)) : '')
              }} placeholder="e.g. 2" className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('seeds.costPerPackage')}</label>
              <input type="number" value={editCostPackage} onChange={e => {
                const v = e.target.value
                setEditCostPackage(v)
                const qty = Number(editQuantity)
                setEditCostUnit(v && qty ? String(Math.round((Number(v) / qty) * 100) / 100) : '')
              }} placeholder="e.g. 100" className="input w-full" />
            </div>
          </div>
          <p className="text-xs text-text-secondary -mt-2">{t('seeds.costHint')}</p>
          <button
            onClick={() => { setEditItem(null); setUpdateError(null); setDeleteItem(editItem) }}
            className="text-sm text-error hover:underline"
          >
            {t('seeds.deleteBatch')}
          </button>
          {updateError && <p className="text-error text-sm mt-2">{updateError}</p>}
        </div>
      </Dialog>

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
