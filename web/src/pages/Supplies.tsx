import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SupplyTypeResponse, type SupplyInventoryResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const CATEGORIES = ['SOIL', 'POT', 'FERTILIZER', 'TRAY', 'LABEL', 'TOOL', 'OTHER'] as const
const UNITS = ['COUNT', 'LITERS', 'KILOGRAMS', 'GRAMS', 'METERS', 'PACKETS'] as const

const UNITS_BY_CATEGORY: Record<string, string[]> = {
  SOIL: ['LITERS', 'KILOGRAMS', 'PACKETS'],
  POT: ['COUNT'],
  FERTILIZER: ['KILOGRAMS', 'GRAMS', 'LITERS', 'PACKETS'],
  TRAY: ['COUNT'],
  LABEL: ['COUNT', 'PACKETS'],
  TOOL: ['COUNT'],
  OTHER: [...UNITS],
}

const DEFAULT_UNIT: Record<string, string> = {
  SOIL: 'LITERS',
  POT: 'COUNT',
  FERTILIZER: 'KILOGRAMS',
  TRAY: 'COUNT',
  LABEL: 'COUNT',
  TOOL: 'COUNT',
  OTHER: 'COUNT',
}

function formatUnit(quantity: number, unit: string, t: (key: string) => string): string {
  return `${quantity} ${t(`supplyUnit.${unit}`)}`
}

function deriveTypeName(category: string, properties: Record<string, unknown>, t: (key: string) => string): string {
  const cat = t(`supplyCategory.${category}`)
  const parts: string[] = []
  switch (category) {
    case 'SOIL':
      if (properties.type) parts.push(String(properties.type))
      break
    case 'POT': {
      const shape = properties.shape ? t(`supplies.${properties.shape}`) : ''
      const dims = [properties.widthMm, properties.depthMm, properties.heightMm].filter(Boolean).join('x')
      if (shape) parts.push(shape.toLowerCase())
      if (dims) parts.push(`${dims} mm`)
      break
    }
    case 'FERTILIZER':
      if (properties.npk) parts.push(`NPK ${properties.npk}`)
      break
    case 'TRAY': {
      const rows = properties.rows as number | undefined
      const cols = properties.columns as number | undefined
      if (rows != null && cols != null) {
        parts.push(rows === 0 && cols === 0 ? t('supplies.pureTray') : `${rows}x${cols}`)
      }
      if (properties.volumePerPlugMl) parts.push(`${properties.volumePerPlugMl} ml`)
      break
    }
    case 'LABEL': {
      if (properties.material) parts.push(String(properties.material))
      const dims = [properties.widthMm, properties.heightMm].filter(Boolean).join('x')
      if (dims) parts.push(`${dims} mm`)
      break
    }
    case 'TOOL':
      if (properties.type) parts.push(String(properties.type))
      break
  }
  return parts.length > 0 ? `${cat}, ${parts.join(' ')}` : cat
}

function displayTypeName(type: { name: string; category: string; properties: Record<string, unknown> }, t: (key: string) => string): string {
  return type.name || deriveTypeName(type.category, type.properties, t)
}

function formatCost(costSek?: number): string {
  if (costSek == null) return ''
  return `${(costSek / 100).toFixed(2)} kr`
}

function formatTypeLabel(type: SupplyTypeResponse, t: (key: string) => string): string {
  if (!type.name) return displayTypeName(type, t)
  const props = type.properties ?? {}
  switch (type.category) {
    case 'POT': {
      const shape = props.shape ? t(`supplies.${props.shape as string}`) : ''
      const dims = [props.heightMm, props.widthMm, props.depthMm].filter(Boolean).join('x')
      return `${type.name}${shape ? ` (${shape}` : ''}${dims ? `${shape ? ' ' : ' ('}${dims} mm` : ''}${shape || dims ? ')' : ''}`
    }
    case 'TRAY': {
      const grid = props.rows && props.columns ? `${props.rows}x${props.columns}` : ''
      const vol = props.volumePerPlugMl ? `${props.volumePerPlugMl}ml` : ''
      const extra = [grid, vol].filter(Boolean).join(' ')
      return extra ? `${type.name} (${extra})` : type.name
    }
    case 'FERTILIZER': {
      const npk = props.npk as string | undefined
      return npk ? `${type.name} (${npk})` : type.name
    }
    case 'SOIL': {
      const soilType = props.type as string | undefined
      return soilType ? `${type.name} — ${soilType}` : type.name
    }
    default:
      return type.name
  }
}

interface GroupedType {
  type: SupplyTypeResponse
  label: string
  totalQuantity: number
  batches: SupplyInventoryResponse[]
}

function groupByCategory(
  types: SupplyTypeResponse[],
  batches: SupplyInventoryResponse[],
  t: (key: string) => string
): Map<string, GroupedType[]> {
  const batchesByType = new Map<number, SupplyInventoryResponse[]>()
  for (const b of batches) {
    const arr = batchesByType.get(b.supplyTypeId) ?? []
    arr.push(b)
    batchesByType.set(b.supplyTypeId, arr)
  }

  const groups = new Map<string, GroupedType[]>()
  for (const type of types) {
    const typeBatches = batchesByType.get(type.id) ?? []
    const totalQuantity = typeBatches.reduce((sum, b) => sum + b.quantity, 0)
    const entry: GroupedType = { type, label: formatTypeLabel(type, t), totalQuantity, batches: typeBatches }
    const arr = groups.get(type.category) ?? []
    arr.push(entry)
    groups.set(type.category, arr)
  }
  return groups
}

function CategoryPropertyFields({
  category, props, onChange,
  t,
}: {
  category: string
  props: Record<string, unknown>
  onChange: (p: Record<string, unknown>) => void
  t: (key: string) => string
}) {
  const set = (key: string, value: unknown) => onChange({ ...props, [key]: value })
  const numVal = (key: string) => (props[key] != null ? String(props[key]) : '')

  switch (category) {
    case 'SOIL':
      return (
        <div>
          <label className="field-label">{t('supplies.type')}</label>
          <input className="input w-full" value={(props.type as string) ?? ''} onChange={e => set('type', e.target.value)} />
        </div>
      )
    case 'POT':
      return (
        <>
          <div>
            <label className="field-label">{t('supplies.shape')}</label>
            <select className="input w-full" value={(props.shape as string) ?? ''} onChange={e => set('shape', e.target.value)}>
              <option value="">{t('common.select')}</option>
              <option value="round">{t('supplies.round')}</option>
              <option value="square">{t('supplies.square')}</option>
            </select>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="field-label">{t('supplies.heightMm')}</label>
              <input type="number" className="input w-full" value={numVal('heightMm')} onChange={e => set('heightMm', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.widthMm')}</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.depthMm')}</label>
              <input type="number" className="input w-full" value={numVal('depthMm')} onChange={e => set('depthMm', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
          </div>
        </>
      )
    case 'FERTILIZER':
      return (
        <div>
          <label className="field-label">{t('supplies.npk')}</label>
          <input className="input w-full" value={(props.npk as string) ?? ''} onChange={e => set('npk', e.target.value)} placeholder="e.g. 10-5-10" />
        </div>
      )
    case 'TRAY':
      return (
        <>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('supplies.rows')}</label>
              <input type="number" className="input w-full" value={numVal('rows')} onChange={e => set('rows', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.columns')}</label>
              <input type="number" className="input w-full" value={numVal('columns')} onChange={e => set('columns', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="field-label">{t('supplies.lengthMm')}</label>
              <input type="number" className="input w-full" value={numVal('lengthMm')} onChange={e => set('lengthMm', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.widthMm')}</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.volumePerPlugMl')}</label>
              <input type="number" className="input w-full" value={numVal('volumePerPlugMl')} onChange={e => set('volumePerPlugMl', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
          </div>
        </>
      )
    case 'LABEL':
      return (
        <>
          <div>
            <label className="field-label">{t('supplies.material')}</label>
            <input className="input w-full" value={(props.material as string) ?? ''} onChange={e => set('material', e.target.value)} />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('supplies.heightMm')}</label>
              <input type="number" className="input w-full" value={numVal('heightMm')} onChange={e => set('heightMm', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.widthMm')}</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
          </div>
        </>
      )
    case 'TOOL':
      return (
        <div>
          <label className="field-label">{t('supplies.type')}</label>
          <input className="input w-full" value={(props.type as string) ?? ''} onChange={e => set('type', e.target.value)} />
        </div>
      )
    default:
      return null
  }
}

export function Supplies() {
  const qc = useQueryClient()
  const { t } = useTranslation()

  const { data: types, error: typesError, isLoading: typesLoading, refetch: refetchTypes } = useQuery({
    queryKey: ['supply-types'],
    queryFn: () => api.supplies.types(),
  })
  const { data: batches, error: batchesError, isLoading: batchesLoading, refetch: refetchBatches } = useQuery({
    queryKey: ['supply-batches'],
    queryFn: () => api.supplies.list(),
  })
  const { data: seasons } = useQuery({ queryKey: ['seasons'], queryFn: api.seasons.list })

  // Expanded type rows
  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const [showUsed, setShowUsed] = useState(false)
  const toggleExpand = (typeId: number) => {
    setExpanded(prev => {
      const next = new Set(prev)
      if (next.has(typeId)) next.delete(typeId)
      else next.add(typeId)
      return next
    })
  }

  // Add batch dialog
  const [showAddBatch, setShowAddBatch] = useState(false)
  const [addBatchCategoryFilter, setAddBatchCategoryFilter] = useState<string | null>(null)
  const [batchTypeId, setBatchTypeId] = useState<number | ''>('')
  const [batchQuantity, setBatchQuantity] = useState('')
  const [batchPackageSize, setBatchPackageSize] = useState('')
  const [batchPackageCount, setBatchPackageCount] = useState('')
  const [batchCost, setBatchCost] = useState('')
  const [batchSeasonId, setBatchSeasonId] = useState<number | ''>('')
  const [batchNotes, setBatchNotes] = useState('')

  const getActiveSeasonId = () => seasons?.find(s => s.isActive)?.id ?? ''
  const resetBatchForm = () => {
    setBatchTypeId(''); setBatchQuantity(''); setBatchPackageSize(''); setBatchPackageCount(''); setBatchCost(''); setBatchSeasonId(getActiveSeasonId()); setBatchNotes('')
  }

  const selectedBatchType = (types ?? []).find(ty => ty.id === batchTypeId)
  const isPackageMode = selectedBatchType && ['LITERS', 'KILOGRAMS', 'GRAMS'].includes(selectedBatchType.unit)

  // New type dialog
  const [showNewType, setShowNewType] = useState(false)
  const [typeName, setTypeName] = useState('')
  const [typeNameEdited, setTypeNameEdited] = useState(false)
  const [typeCategory, setTypeCategory] = useState('')
  const [typeUnit, setTypeUnit] = useState('')
  const [typeProps, setTypeProps] = useState<Record<string, unknown>>({})

  const resetTypeForm = () => {
    setTypeName(''); setTypeCategory(''); setTypeUnit(''); setTypeProps({}); setTypeNameEdited(false)
  }

  // Edit type dialog
  const [editType, setEditType] = useState<SupplyTypeResponse | null>(null)
  const [editTypeName, setEditTypeName] = useState('')
  const [editTypeUnit, setEditTypeUnit] = useState('')
  const [editTypeProps, setEditTypeProps] = useState<Record<string, unknown>>({})

  // Edit batch dialog
  const [editBatch, setEditBatch] = useState<SupplyInventoryResponse | null>(null)
  const [editBatchQuantity, setEditBatchQuantity] = useState('')
  const [editBatchCost, setEditBatchCost] = useState('')
  const [editBatchSeasonId, setEditBatchSeasonId] = useState<number | ''>('')
  const [editBatchNotes, setEditBatchNotes] = useState('')

  // Decrement dialog
  const [decrementBatch, setDecrementBatch] = useState<SupplyInventoryResponse | null>(null)
  const [decrementAmount, setDecrementAmount] = useState('')

  // Delete confirm
  const [deleteBatch, setDeleteBatch] = useState<SupplyInventoryResponse | null>(null)
  const [deleteType, setDeleteType] = useState<SupplyTypeResponse | null>(null)

  // Error states
  const [mutError, setMutError] = useState<string | null>(null)

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['supply-types'] })
    qc.invalidateQueries({ queryKey: ['supply-batches'] })
  }

  const createBatchMut = useMutation({
    mutationFn: () => {
      const totalQuantity = isPackageMode
        ? Number(batchPackageSize) * Number(batchPackageCount)
        : Number(batchQuantity)
      const totalCostSek = isPackageMode && batchCost
        ? Math.round(Number(batchCost) * 100 * Number(batchPackageCount))
        : batchCost ? Math.round(Number(batchCost) * 100) : undefined
      return api.supplies.create({
        supplyTypeId: batchTypeId as number,
        quantity: totalQuantity,
        costSek: totalCostSek,
        seasonId: batchSeasonId !== '' ? batchSeasonId as number : undefined,
        notes: batchNotes || undefined,
      })
    },
    onSuccess: () => { invalidate(); setShowAddBatch(false); resetBatchForm(); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const createTypeMut = useMutation({
    mutationFn: () => api.supplies.createType({
      name: typeName || deriveTypeName(typeCategory, typeProps, t),
      category: typeCategory,
      unit: typeUnit,
      properties: Object.keys(typeProps).length > 0 ? typeProps : undefined,
    }),
    onSuccess: () => { invalidate(); setShowNewType(false); resetTypeForm(); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const updateTypeMut = useMutation({
    mutationFn: () => api.supplies.updateType(editType!.id, {
      name: editTypeName,
      unit: editTypeUnit,
      properties: Object.keys(editTypeProps).length > 0 ? editTypeProps : undefined,
    }),
    onSuccess: () => { invalidate(); setEditType(null); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const updateBatchMut = useMutation({
    mutationFn: () => api.supplies.update(editBatch!.id, {
      quantity: Number(editBatchQuantity),
      costSek: editBatchCost ? Math.round(Number(editBatchCost) * 100) : undefined,
      seasonId: editBatchSeasonId !== '' ? editBatchSeasonId : undefined,
      notes: editBatchNotes || undefined,
    }),
    onSuccess: () => { invalidate(); setEditBatch(null); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const decrementMut = useMutation({
    mutationFn: () => api.supplies.decrement(decrementBatch!.id, Number(decrementAmount)),
    onSuccess: () => { invalidate(); setDecrementBatch(null); setDecrementAmount(''); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const deleteBatchMut = useMutation({
    mutationFn: (id: number) => api.supplies.delete(id),
    onSuccess: () => { invalidate(); setDeleteBatch(null); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const deleteTypeMut = useMutation({
    mutationFn: (id: number) => api.supplies.deleteType(id),
    onSuccess: () => { invalidate(); setDeleteType(null); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const isLoading = typesLoading || batchesLoading
  const error = typesError || batchesError

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => { refetchTypes(); refetchBatches() }} />

  const grouped = groupByCategory(types ?? [], batches ?? [], t)
  const hasData = (types ?? []).length > 0

  return (
    <div>
      <PageHeader
        title={t('supplies.title')}
        secondaryAction={{ label: t('supplies.newType'), onClick: () => { setMutError(null); setShowNewType(true) } }}
        action={{ label: t('common.add'), onClick: () => { resetBatchForm(); setMutError(null); setAddBatchCategoryFilter(null); setShowAddBatch(true) } }}
      />

      <div className="px-4 py-4">

        {!hasData && (
          <div className="bg-accent-light/50 border border-accent/15 rounded-2xl px-6 py-6 text-center">
            <div className="w-10 h-10 rounded-full bg-accent/10 flex items-center justify-center mx-auto mb-3">
              <span className="text-xl">📦</span>
            </div>
            <p className="font-semibold text-text-primary">{t('supplies.noSupplies')}</p>
            <p className="text-sm text-text-secondary mt-1">{t('supplies.noTypes')}</p>
            <button onClick={() => { setMutError(null); setShowNewType(true) }} className="btn-primary mt-4">
              {t('supplies.newType')}
            </button>
          </div>
        )}

        {hasData && CATEGORIES.map(cat => {
          const items = grouped.get(cat)
          if (!items || items.length === 0) return null
          return (
            <div key={cat} className="mb-6">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-sm font-semibold text-text-secondary uppercase tracking-wide">
                  {t(`supplyCategory.${cat}`)}
                </h2>
                <button
                  onClick={() => { resetTypeForm(); setTypeCategory(cat); setTypeUnit(DEFAULT_UNIT[cat] ?? 'COUNT'); setMutError(null); setShowNewType(true) }}
                  className="text-xs text-accent hover:underline cursor-pointer"
                >
                  {t('supplies.addType')}
                </button>
              </div>
              <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
                {items.map((item, idx) => (
                  <div key={item.type.id}>
                    {idx > 0 && <div className="border-t border-divider" />}
                    <button
                      onClick={() => toggleExpand(item.type.id)}
                      className="w-full flex items-center justify-between px-4 py-3 hover:bg-surface transition-colors text-left"
                    >
                      <div className="flex items-center gap-2 min-w-0">
                        <svg
                          width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor"
                          strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"
                          className={`text-text-muted flex-shrink-0 transition-transform duration-150 ${expanded.has(item.type.id) ? 'rotate-90' : ''}`}
                        >
                          <polyline points="5 2 10 7 5 12" />
                        </svg>
                        <span className="text-sm font-medium text-text-primary truncate">{item.label}</span>
                        <span
                          role="button"
                          onClick={e => {
                            e.stopPropagation()
                            resetBatchForm()
                            setMutError(null)
                            setBatchTypeId(item.type.id)
                            setAddBatchCategoryFilter(null)
                            setShowAddBatch(true)
                          }}
                          className="text-xs text-accent hover:underline cursor-pointer shrink-0"
                        >
                          {t('common.add')}
                        </span>
                      </div>
                      <div className="flex items-center gap-3">
                        <span className="text-sm tabular-nums text-text-secondary">
                          {formatUnit(item.totalQuantity, item.type.unit, t)}
                        </span>
                        <button
                          onClick={(e) => {
                            e.stopPropagation()
                            setEditType(item.type)
                            setEditTypeName(item.type.name)
                            setEditTypeUnit(item.type.unit)
                            setEditTypeProps(item.type.properties ?? {})
                            setMutError(null)
                          }}
                          className="text-text-muted hover:text-text-secondary transition-colors p-0.5"
                          aria-label={t('common.edit')}
                          title={t('supplies.editType')}
                        >
                          <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                            <path d="M9.5 2.5 L11.5 4.5 L4.5 11.5 L2 12 L2.5 9.5 Z" />
                            <line x1="8" y1="4" x2="10" y2="6" />
                          </svg>
                        </button>
                      </div>
                    </button>
                    {expanded.has(item.type.id) && (() => {
                      const visibleBatches = (showUsed ? item.batches : item.batches.filter(b => b.quantity > 0))
                        .slice().sort((a, b) => (a.quantity === 0 ? 1 : 0) - (b.quantity === 0 ? 1 : 0))
                      const hasUsed = item.batches.some(b => b.quantity === 0)
                      return (
                      <div className="border-t border-divider bg-surface">
                        {hasUsed && (
                          <label className="flex items-center gap-2 px-4 py-2 text-xs text-text-secondary cursor-pointer">
                            <input type="checkbox" checked={showUsed} onChange={e => setShowUsed(e.target.checked)} className="rounded" />
                            {t('supplies.showUsed')}
                          </label>
                        )}
                        {visibleBatches.length === 0 && (
                          <p className="px-4 py-3 text-sm text-text-muted italic">{t('supplies.noBatches')}</p>
                        )}
                        {visibleBatches.map(batch => {
                          const seasonName = seasons?.find(s => s.id === batch.seasonId)?.name
                          return (
                            <div key={batch.id} className="flex items-center justify-between px-4 py-2.5 border-t border-divider first:border-0">
                              <div className="text-sm space-x-3">
                                <span className="tabular-nums font-medium">{formatUnit(batch.quantity, batch.unit, t)}</span>
                                {batch.costSek != null && (
                                  <span className="text-text-secondary">
                                    {formatCost(batch.costSek)}
                                    {batch.quantity > 0 && ` (${formatCost(Math.round(batch.costSek / batch.quantity))}/${t(`supplyUnit.${batch.unit}`)})`}
                                  </span>
                                )}
                                {seasonName && <span className="text-text-muted">{seasonName}</span>}
                                {batch.notes && <span className="text-text-muted italic">{batch.notes}</span>}
                              </div>
                              <div className="flex items-center gap-1.5">
                                <button
                                  onClick={() => { setDecrementBatch(batch); setDecrementAmount(String(batch.quantity)); setMutError(null) }}
                                  className="text-xs px-2 py-1 rounded-lg bg-accent-light text-accent hover:bg-accent/20 transition-colors"
                                  title={t('supplies.decrement')}
                                >
                                  {t('supplies.decrement')}
                                </button>
                                <button
                                  onClick={() => {
                                    setEditBatch(batch)
                                    setEditBatchQuantity(String(batch.quantity))
                                    setEditBatchCost(batch.costSek != null ? String(batch.costSek / 100) : '')
                                    setEditBatchSeasonId(batch.seasonId ?? '')
                                    setEditBatchNotes(batch.notes ?? '')
                                    setMutError(null)
                                  }}
                                  className="text-text-muted hover:text-text-secondary transition-colors p-0.5"
                                  aria-label={t('common.edit')}
                                >
                                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                    <path d="M9.5 2.5 L11.5 4.5 L4.5 11.5 L2 12 L2.5 9.5 Z" />
                                    <line x1="8" y1="4" x2="10" y2="6" />
                                  </svg>
                                </button>
                                <button
                                  onClick={() => { setDeleteBatch(batch); setMutError(null) }}
                                  className="text-text-muted hover:text-error transition-colors p-0.5"
                                  aria-label={t('common.delete')}
                                >
                                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                    <line x1="2" y1="4" x2="12" y2="4" />
                                    <path d="M5 4V2.5a.5.5 0 0 1 .5-.5h3a.5.5 0 0 1 .5.5V4" />
                                    <path d="M3 4l.5 8.5a.5.5 0 0 0 .5.5h6a.5.5 0 0 0 .5-.5L11 4" />
                                  </svg>
                                </button>
                              </div>
                            </div>
                          )
                        })}
                      </div>
                      )})()}
                  </div>
                ))}
              </div>
            </div>
          )
        })}
      </div>

      {/* Add batch dialog */}
      <Dialog
        open={showAddBatch}
        onClose={() => { setShowAddBatch(false); resetBatchForm(); setMutError(null) }}
        title={t('supplies.addBatch')}
        actions={
          <>
            <button onClick={() => { setShowAddBatch(false); resetBatchForm(); setMutError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => createBatchMut.mutate()}
              disabled={!batchTypeId || (isPackageMode ? (!batchPackageSize || !batchPackageCount) : !batchQuantity) || createBatchMut.isPending}
              className="btn-primary text-sm"
            >
              {createBatchMut.isPending ? t('common.creating') : t('common.add')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {(types ?? []).length === 0 ? (
            <div className="text-center py-4">
              <p className="text-sm text-text-secondary mb-2">{t('supplies.noTypes')}</p>
              <button onClick={() => { setShowAddBatch(false); setShowNewType(true) }} className="text-sm text-accent hover:underline">
                {t('supplies.newType')}
              </button>
            </div>
          ) : (
            <>
              <div>
                <label className="field-label">{t('supplies.selectType')}</label>
                <select className="input w-full" value={batchTypeId} onChange={e => setBatchTypeId(e.target.value ? Number(e.target.value) : '')}>
                  <option value="">{t('supplies.selectType')}</option>
                  {CATEGORIES.filter(cat => !addBatchCategoryFilter || cat === addBatchCategoryFilter).map(cat => {
                    const catTypes = (types ?? []).filter(ty => ty.category === cat)
                    if (catTypes.length === 0) return null
                    return addBatchCategoryFilter ? (
                      catTypes.map(ty => (
                        <option key={ty.id} value={ty.id}>{formatTypeLabel(ty, t)}</option>
                      ))
                    ) : (
                      <optgroup key={cat} label={t(`supplyCategory.${cat}`)}>
                        {catTypes.map(ty => (
                          <option key={ty.id} value={ty.id}>{formatTypeLabel(ty, t)}</option>
                        ))}
                      </optgroup>
                    )
                  })}
                </select>
              </div>
              {isPackageMode ? (
                <>
                  <div className="grid grid-cols-3 gap-3">
                    <div>
                      <label className="field-label">{t('supplies.packageSize')} ({t(`supplyUnit.${selectedBatchType!.unit}`)})</label>
                      <input type="number" step="any" className="input w-full" value={batchPackageSize} onChange={e => setBatchPackageSize(e.target.value)} />
                    </div>
                    <div>
                      <label className="field-label">{t('supplies.packageCount')} *</label>
                      <input type="number" className="input w-full" value={batchPackageCount} onChange={e => setBatchPackageCount(e.target.value)} />
                    </div>
                    <div>
                      <label className="field-label">{t('supplies.pricePerPackage')} (kr)</label>
                      <input type="number" step="any" className="input w-full" value={batchCost} onChange={e => setBatchCost(e.target.value)} />
                    </div>
                  </div>
                  <p className="text-xs text-text-secondary mt-1">
                    {(() => {
                      const totalQty = (Number(batchPackageSize) || 0) * (Number(batchPackageCount) || 0)
                      const unitLabel = t(`supplyUnit.${selectedBatchType!.unit}`)
                      const totalCost = (Number(batchCost) || 0) * (Number(batchPackageCount) || 0)
                      return (
                        <>
                          {t('supplies.totalSummary', { quantity: totalQty % 1 === 0 ? totalQty : totalQty.toFixed(1), unit: unitLabel })}
                          {totalCost > 0 && ` — ${totalCost.toFixed(2)} kr`}
                        </>
                      )
                    })()}
                  </p>
                </>
              ) : (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="field-label">{t('supplies.quantity')} *</label>
                    <input type="number" step="any" className="input w-full" value={batchQuantity} onChange={e => setBatchQuantity(e.target.value)} />
                  </div>
                  <div>
                    <label className="field-label">{t('supplies.packageCost')} (kr)</label>
                    <input type="number" step="any" className="input w-full" value={batchCost} onChange={e => setBatchCost(e.target.value)} />
                  </div>
                </div>
              )}
              <div>
                <label className="field-label">{t('supplies.season')}</label>
                <select className="input w-full" value={batchSeasonId} onChange={e => setBatchSeasonId(e.target.value ? Number(e.target.value) : '')}>
                  <option value="">{t('common.none')}</option>
                  {(seasons ?? []).map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                </select>
              </div>
              <div>
                <label className="field-label">{t('supplies.notes')}</label>
                <input className="input w-full" value={batchNotes} onChange={e => setBatchNotes(e.target.value)} />
              </div>
            </>
          )}
          {mutError && <p className="text-error text-sm">{mutError}</p>}
        </div>
      </Dialog>

      {/* New type dialog */}
      <Dialog
        open={showNewType}
        onClose={() => { setShowNewType(false); resetTypeForm(); setMutError(null) }}
        title={t('supplies.newType')}
        actions={
          <>
            <button onClick={() => { setShowNewType(false); resetTypeForm(); setMutError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => createTypeMut.mutate()}
              disabled={!typeCategory || !typeUnit || createTypeMut.isPending}
              className="btn-primary text-sm"
            >
              {createTypeMut.isPending ? t('common.creating') : t('common.add')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('supplies.category')} *</label>
            <select className="input w-full" value={typeCategory} onChange={e => {
              const cat = e.target.value
              setTypeCategory(cat)
              setTypeProps({})
              setTypeUnit(cat ? (DEFAULT_UNIT[cat] ?? 'COUNT') : '')
              if (!typeNameEdited) setTypeName(cat ? deriveTypeName(cat, {}, t) : '')
            }}>
              <option value="">{t('common.select')}</option>
              {CATEGORIES.map(c => <option key={c} value={c}>{t(`supplyCategory.${c}`)}</option>)}
            </select>
          </div>
          <div>
            <label className="field-label">{t('supplies.typeName')}</label>
            <input
              className="input w-full"
              value={typeName}
              onChange={e => { setTypeName(e.target.value); setTypeNameEdited(true) }}
            />
            <p className="text-xs text-text-secondary mt-1">{t('supplies.nameHint')}</p>
          </div>
          <div>
            <label className="field-label">{t('supplies.unit')} *</label>
            <select className="input w-full" value={typeUnit} onChange={e => setTypeUnit(e.target.value)}>
              <option value="">{t('common.select')}</option>
              {(typeCategory ? UNITS_BY_CATEGORY[typeCategory] ?? UNITS : UNITS).map(u => <option key={u} value={u}>{t(`supplyUnit.${u}`)}</option>)}
            </select>
          </div>
          {typeCategory && typeCategory !== 'OTHER' && (
            <>
              <p className="text-xs font-medium text-text-secondary uppercase tracking-wide">{t('supplies.properties')}</p>
              <CategoryPropertyFields category={typeCategory} props={typeProps} onChange={newProps => {
                setTypeProps(newProps)
                if (!typeNameEdited) setTypeName(deriveTypeName(typeCategory, newProps, t))
              }} t={t} />
            </>
          )}
          {mutError && <p className="text-error text-sm">{mutError}</p>}
        </div>
      </Dialog>

      {/* Edit type dialog */}
      <Dialog
        open={editType !== null}
        onClose={() => { setEditType(null); setMutError(null) }}
        title={t('supplies.editType')}
        actions={
          <>
            <button onClick={() => { setEditType(null); setMutError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => updateTypeMut.mutate()}
              disabled={!editTypeUnit || updateTypeMut.isPending}
              className="btn-primary text-sm"
            >
              {updateTypeMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('supplies.typeName')}</label>
            <input
              className="input w-full"
              value={editTypeName}
              onChange={e => setEditTypeName(e.target.value)}
              placeholder={editType ? deriveTypeName(editType.category, editTypeProps, t) : t('common.optional')}
            />
          </div>
          <div>
            <label className="field-label">{t('supplies.unit')} *</label>
            <select className="input w-full" value={editTypeUnit} onChange={e => setEditTypeUnit(e.target.value)}>
              {(editType ? UNITS_BY_CATEGORY[editType.category] ?? UNITS : UNITS).map(u => <option key={u} value={u}>{t(`supplyUnit.${u}`)}</option>)}
            </select>
          </div>
          {editType && editType.category !== 'OTHER' && (
            <>
              <p className="text-xs font-medium text-text-secondary uppercase tracking-wide">{t('supplies.properties')}</p>
              <CategoryPropertyFields category={editType.category} props={editTypeProps} onChange={setEditTypeProps} t={t} />
            </>
          )}
          <button
            onClick={() => { setEditType(null); if (editType) { setDeleteType(editType) } }}
            className="text-sm text-error hover:underline"
          >
            {t('common.delete')}
          </button>
          {mutError && <p className="text-error text-sm">{mutError}</p>}
        </div>
      </Dialog>

      {/* Edit batch dialog */}
      <Dialog
        open={editBatch !== null}
        onClose={() => { setEditBatch(null); setMutError(null) }}
        title={t('common.edit')}
        actions={
          <>
            <button onClick={() => { setEditBatch(null); setMutError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => updateBatchMut.mutate()}
              disabled={!editBatchQuantity || updateBatchMut.isPending}
              className="btn-primary text-sm"
            >
              {updateBatchMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('supplies.quantity')} *</label>
              <input type="number" step="any" className="input w-full" value={editBatchQuantity} onChange={e => setEditBatchQuantity(e.target.value)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.packageCost')} (kr)</label>
              <input type="number" step="any" className="input w-full" value={editBatchCost} onChange={e => setEditBatchCost(e.target.value)} />
            </div>
          </div>
          <div>
            <label className="field-label">{t('supplies.season')}</label>
            <select className="input w-full" value={editBatchSeasonId} onChange={e => setEditBatchSeasonId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">{t('common.none')}</option>
              {(seasons ?? []).map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
          <div>
            <label className="field-label">{t('supplies.notes')}</label>
            <input className="input w-full" value={editBatchNotes} onChange={e => setEditBatchNotes(e.target.value)} />
          </div>
          <button
            onClick={() => { const b = editBatch; setEditBatch(null); if (b) setDeleteBatch(b) }}
            className="text-sm text-error hover:underline"
          >
            {t('common.delete')}
          </button>
          {mutError && <p className="text-error text-sm">{mutError}</p>}
        </div>
      </Dialog>

      {/* Decrement dialog */}
      <Dialog
        open={decrementBatch !== null}
        onClose={() => { setDecrementBatch(null); setDecrementAmount(''); setMutError(null) }}
        title={t('supplies.decrementTitle')}
        actions={
          <>
            <button onClick={() => { setDecrementBatch(null); setDecrementAmount(''); setMutError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => decrementMut.mutate()}
              disabled={!decrementAmount || Number(decrementAmount) <= 0 || decrementMut.isPending}
              className="btn-primary text-sm"
            >
              {decrementMut.isPending ? t('common.saving') : t('supplies.decrement')}
            </button>
          </>
        }
      >
        <div className="space-y-4">
          {decrementBatch && (
            <p className="text-sm text-text-secondary">
              {t('supplies.total')}: {formatUnit(decrementBatch.quantity, decrementBatch.unit, t)}
            </p>
          )}
          <div>
            <label className="field-label">{t('supplies.amount')} *</label>
            <input type="number" step="any" className="input w-full" value={decrementAmount} onChange={e => setDecrementAmount(e.target.value)} />
          </div>
          {mutError && <p className="text-error text-sm">{mutError}</p>}
        </div>
      </Dialog>

      {/* Delete batch confirm */}
      <Dialog
        open={deleteBatch !== null}
        onClose={() => { setDeleteBatch(null); setMutError(null) }}
        title={t('common.delete')}
        actions={
          <>
            <button onClick={() => { setDeleteBatch(null); setMutError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => deleteBatch && deleteBatchMut.mutate(deleteBatch.id)}
              className="px-4 py-2 text-sm text-error font-semibold"
            >
              {deleteBatchMut.isPending ? t('common.deleting') : t('common.delete')}
            </button>
          </>
        }
      >
        <p className="text-text-secondary">{t('common.delete')}?</p>
        {mutError && <p className="text-error text-sm mt-2">{mutError}</p>}
      </Dialog>

      {/* Delete type confirm */}
      <Dialog
        open={deleteType !== null}
        onClose={() => { setDeleteType(null); setMutError(null) }}
        title={t('common.delete')}
        actions={
          <>
            <button onClick={() => { setDeleteType(null); setMutError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => deleteType && deleteTypeMut.mutate(deleteType.id)}
              className="px-4 py-2 text-sm text-error font-semibold"
            >
              {deleteTypeMut.isPending ? t('common.deleting') : t('common.delete')}
            </button>
          </>
        }
      >
        <p className="text-text-secondary">{t('common.delete')}?</p>
        {mutError && <p className="text-error text-sm mt-2">{mutError}</p>}
      </Dialog>
    </div>
  )
}
