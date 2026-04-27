import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SupplyTypeResponse, type SupplyInventoryResponse } from '../api/client'
import { Masthead, LedgerPagination } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const CATEGORIES = ['SOIL', 'POT', 'FERTILIZER', 'TRAY', 'LABEL', 'OTHER'] as const
const UNITS = ['COUNT', 'LITERS', 'KILOGRAMS', 'GRAMS', 'METERS', 'PACKETS'] as const

const UNITS_BY_CATEGORY: Record<string, string[]> = {
  SOIL: ['LITERS', 'KILOGRAMS', 'PACKETS'],
  POT: ['COUNT'],
  FERTILIZER: ['KILOGRAMS', 'GRAMS', 'LITERS', 'PACKETS'],
  TRAY: ['COUNT'],
  LABEL: ['COUNT', 'PACKETS'],

  OTHER: [...UNITS],
}

const DEFAULT_UNIT: Record<string, string> = {
  SOIL: 'LITERS',
  POT: 'COUNT',
  FERTILIZER: 'KILOGRAMS',
  TRAY: 'COUNT',
  LABEL: 'COUNT',

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
      const dims = [properties.widthMm, properties.heightMm].filter(Boolean).join('x')
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
  }
  return parts.length > 0 ? `${cat}, ${parts.join(' ')}` : cat
}

function displayTypeName(type: { name: string; category: string; properties: Record<string, unknown> }, t: (key: string) => string): string {
  return type.name || deriveTypeName(type.category, type.properties, t)
}

function formatCost(costCents?: number): string {
  if (costCents == null) return ''
  return `${(costCents / 100).toFixed(2)} kr`
}

function formatTypeLabel(type: SupplyTypeResponse, t: (key: string) => string): string {
  if (!type.name) return displayTypeName(type, t)
  const props = type.properties ?? {}
  switch (type.category) {
    case 'POT': {
      const nameNorm = type.name.toLowerCase().replace(/\s/g, '')
      const shape = props.shape ? t(`supplies.${props.shape as string}`) : ''
      const dims = [props.widthMm, props.heightMm].filter(Boolean).join('x')
      const parts = [
        shape && !nameNorm.includes(shape.toLowerCase()) ? shape : '',
        dims && !nameNorm.includes(dims.replace(/\s/g, '')) ? `${dims} mm` : '',
      ].filter(Boolean)
      return parts.length > 0 ? `${type.name} (${parts.join(' ')})` : type.name
    }
    case 'TRAY': {
      const grid = props.rows && props.columns ? `${props.rows}x${props.columns}` : ''
      const vol = props.volumePerPlugMl ? `${props.volumePerPlugMl}ml` : ''
      const nameNorm = type.name.replace(/\s/g, '')
      const parts = [grid, vol].filter(p => p && !nameNorm.includes(p.replace(/\s/g, '')))
      const extra = parts.join(' ')
      return extra ? `${type.name} (${extra})` : type.name
    }
    case 'FERTILIZER': {
      const npk = props.npk as string | undefined
      if (!npk || type.name.toLowerCase().includes(npk.toLowerCase())) return type.name
      return `${type.name} (${npk})`
    }
    case 'SOIL': {
      const soilType = props.type as string | undefined
      if (!soilType || type.name.toLowerCase().includes(soilType.toLowerCase())) return type.name
      return `${type.name} — ${soilType}`
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

function mmWarning(value: unknown, label: string, minMm: number, maxMm: number, t: (key: string, opts?: Record<string, unknown>) => string): string | null {
  const v = Number(value)
  if (!v || v <= 0) return null
  if (v < minMm) return t('supplies.warnTooSmall', { field: label, value: v, hint: `${v * 10} mm` })
  if (v > maxMm) return t('supplies.warnTooLarge', { field: label, value: v })
  return null
}

function mlWarning(value: unknown, minMl: number, maxMl: number, t: (key: string, opts?: Record<string, unknown>) => string): string | null {
  const v = Number(value)
  if (!v || v <= 0) return null
  if (v < minMl) return t('supplies.warnVolumeTooSmall', { value: v })
  if (v > maxMl) return t('supplies.warnVolumeTooLarge', { value: v })
  return null
}

function Warning({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 4 }}>
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-accent)' }}>
        ⚠
      </span>
      <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 13, color: 'var(--color-accent)' }}>
        {message}
      </span>
    </div>
  )
}

function getRequiredFields(category: string): string[] {
  switch (category) {
    case 'SOIL': return ['type']
    case 'POT': return ['shape', 'widthMm', 'heightMm']
    case 'FERTILIZER': return []
    case 'TRAY': return ['rows', 'columns']
    case 'LABEL': return []
    default: return []
  }
}

function arePropertiesValid(category: string, props: Record<string, unknown>): boolean {
  const required = getRequiredFields(category)
  for (const field of required) {
    const val = props[field]
    if (val == null || val === '') return false
  }
  if (category === 'FERTILIZER' && props.npk) {
    if (!/^\d+-\d+-\d+$/.test(String(props.npk).trim())) return false
  }
  return true
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
  const [blurred, setBlurred] = useState<Set<string>>(new Set())
  const set = (key: string, value: unknown) => onChange({ ...props, [key]: value })
  const numVal = (key: string) => (props[key] != null ? String(props[key]) : '')
  const onBlur = (key: string) => setBlurred(prev => new Set(prev).add(key))
  const onFocus = (key: string) => setBlurred(prev => { const next = new Set(prev); next.delete(key); return next })
  const showWarn = (key: string) => blurred.has(key)

  switch (category) {
    case 'SOIL':
      return (
        <div>
          <label className="field-label">{t('supplies.type')} *</label>
          <input className="input w-full" list="soil-types" value={(props.type as string) ?? ''} onChange={e => set('type', e.target.value)} />
          <datalist id="soil-types">
            <option value={t('soilTypes.planting')} />
            <option value={t('soilTypes.sowing')} />
            <option value={t('soilTypes.rhododendron')} />
            <option value={t('soilTypes.rose')} />
            <option value={t('soilTypes.mediterranean')} />
          </datalist>
        </div>
      )
    case 'POT':
      return (
        <>
          <div>
            <label className="field-label">{t('supplies.shape')} *</label>
            <select className="input w-full" value={(props.shape as string) ?? ''} onChange={e => set('shape', e.target.value)}>
              <option value="">{t('common.select')}</option>
              <option value="round">{t('supplies.round')}</option>
              <option value="square">{t('supplies.square')}</option>
            </select>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{props.shape === 'round' ? t('supplies.diameterMm') : t('supplies.widthMm')} *</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('widthMm')} onBlur={() => onBlur('widthMm')} />
              {showWarn('widthMm') && <Warning message={mmWarning(props.widthMm, props.shape === 'round' ? t('supplies.diameter') : t('supplies.width'), 20, 1000, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.heightMm')} *</label>
              <input type="number" className="input w-full" value={numVal('heightMm')} onChange={e => set('heightMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('heightMm')} onBlur={() => onBlur('heightMm')} />
              {showWarn('heightMm') && <Warning message={mmWarning(props.heightMm, t('supplies.height'), 20, 1000, t)} />}
            </div>
          </div>
        </>
      )
    case 'FERTILIZER': {
      const npkVal = (props.npk as string) ?? ''
      const npkValid = !npkVal || /^\d+-\d+-\d+$/.test(npkVal.trim())
      return (
        <div>
          <label className="field-label">{t('supplies.npk')}</label>
          <input className="input w-full" value={npkVal} onChange={e => set('npk', e.target.value)} placeholder="e.g. 10-5-10" />
          {npkVal && !npkValid && <Warning message={t('supplies.warnNpkFormat')} />}
        </div>
      )
    }
    case 'TRAY':
      return (
        <>
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="field-label">{t('supplies.rows')} *</label>
              <input type="number" className="input w-full" value={numVal('rows')} onChange={e => set('rows', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
            <div>
              <label className="field-label">{t('supplies.columns')} *</label>
              <input type="number" className="input w-full" value={numVal('columns')} onChange={e => set('columns', e.target.value ? Number(e.target.value) : undefined)} />
            </div>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="field-label">{t('supplies.lengthMm')}</label>
              <input type="number" className="input w-full" value={numVal('lengthMm')} onChange={e => set('lengthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('lengthMm')} onBlur={() => onBlur('lengthMm')} />
              {showWarn('lengthMm') && <Warning message={mmWarning(props.lengthMm, t('supplies.length'), 50, 2000, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.widthMm')}</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('widthMm')} onBlur={() => onBlur('widthMm')} />
              {showWarn('widthMm') && <Warning message={mmWarning(props.widthMm, t('supplies.width'), 50, 2000, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.volumePerPlugMl')}</label>
              <input type="number" className="input w-full" value={numVal('volumePerPlugMl')} onChange={e => set('volumePerPlugMl', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('volumePerPlugMl')} onBlur={() => onBlur('volumePerPlugMl')} />
              {showWarn('volumePerPlugMl') && <Warning message={mlWarning(props.volumePerPlugMl, 1, 500, t)} />}
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
              <input type="number" className="input w-full" value={numVal('heightMm')} onChange={e => set('heightMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('heightMm')} onBlur={() => onBlur('heightMm')} />
              {showWarn('heightMm') && <Warning message={mmWarning(props.heightMm, t('supplies.height'), 5, 300, t)} />}
            </div>
            <div>
              <label className="field-label">{t('supplies.widthMm')}</label>
              <input type="number" className="input w-full" value={numVal('widthMm')} onChange={e => set('widthMm', e.target.value ? Number(e.target.value) : undefined)} onFocus={() => onFocus('widthMm')} onBlur={() => onBlur('widthMm')} />
              {showWarn('widthMm') && <Warning message={mmWarning(props.widthMm, t('supplies.width'), 5, 300, t)} />}
            </div>
          </div>
        </>
      )
    default:
      return null
  }
}

// Grid template: type name | total quantity | edit pencil
const TYPE_TEMPLATE = '1fr 140px 36px'

const headerStyle: React.CSSProperties = {
  display: 'grid',
  gridTemplateColumns: TYPE_TEMPLATE,
  gap: 18,
  padding: '10px 0',
  borderBottom: '1px solid var(--color-ink)',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  alignItems: 'center',
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
  const PACKAGE_MODE_UNITS = ['LITERS', 'KILOGRAMS', 'GRAMS']
  const PACKAGE_MODE_CATEGORIES = ['LABEL']
  const isPackageMode = selectedBatchType && (
    PACKAGE_MODE_UNITS.includes(selectedBatchType.unit) || PACKAGE_MODE_CATEGORIES.includes(selectedBatchType.category)
  )

  // New type dialog
  const [showNewType, setShowNewType] = useState(false)
  const [typeName, setTypeName] = useState('')
  const [typeInexhaustible, setTypeInexhaustible] = useState(false)
  const [editTypeInexhaustible, setEditTypeInexhaustible] = useState(false)
  const [typeNameEdited, setTypeNameEdited] = useState(false)
  const [typeCategory, setTypeCategory] = useState('')
  const [typeUnit, setTypeUnit] = useState('')
  const [typeProps, setTypeProps] = useState<Record<string, unknown>>({})

  const resetTypeForm = () => {
    setTypeName(''); setTypeCategory(''); setTypeUnit(''); setTypeProps({}); setTypeNameEdited(false)
    setTypeInexhaustible(false)
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
      const totalCostCents = isPackageMode && batchCost
        ? Math.round(Number(batchCost) * 100 * Number(batchPackageCount))
        : batchCost ? Math.round(Number(batchCost) * 100) : undefined
      return api.supplies.create({
        supplyTypeId: batchTypeId as number,
        quantity: totalQuantity,
        costCents: totalCostCents,
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
      inexhaustible: typeInexhaustible,
    }),
    onSuccess: () => { invalidate(); setShowNewType(false); resetTypeForm(); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const updateTypeMut = useMutation({
    mutationFn: () => api.supplies.updateType(editType!.id, {
      name: editTypeName,
      unit: editTypeUnit,
      properties: Object.keys(editTypeProps).length > 0 ? editTypeProps : undefined,
      inexhaustible: editTypeInexhaustible,
    }),
    onSuccess: () => { invalidate(); setEditType(null); setMutError(null) },
    onError: (err) => setMutError(err instanceof Error ? err.message : String(err)),
  })

  const updateBatchMut = useMutation({
    mutationFn: () => api.supplies.update(editBatch!.id, {
      quantity: Number(editBatchQuantity),
      costCents: editBatchCost ? Math.round(Number(editBatchCost) * 100) : undefined,
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

  // Flat ordered list of all supply type entries for pagination
  const allInventoryEntries: { cat: string; item: GroupedType }[] = CATEGORIES.flatMap(cat =>
    (grouped.get(cat) ?? []).map(item => ({ cat, item }))
  )
  const [inventoryPage, setInventoryPage] = useState(0)
  const inventoryPageSize = 50
  useEffect(() => { setInventoryPage(0) }, [allInventoryEntries.length])
  const pagedEntries = allInventoryEntries.slice(inventoryPage * inventoryPageSize, (inventoryPage + 1) * inventoryPageSize)

  // Rebuild a per-category map from the visible slice for rendering
  const pagedGrouped = new Map<string, GroupedType[]>()
  for (const { cat, item } of pagedEntries) {
    const arr = pagedGrouped.get(cat) ?? []
    arr.push(item)
    pagedGrouped.set(cat, arr)
  }
  // Note: when a category's items span page boundaries, only the current
  // page's slice is rendered. Empty-state within a category reflects the
  // current page — typically Supplies sits well under 50 items across all
  // categories so users rarely hit the boundary.

  return (
    <div>
      <Masthead
        left={t('nav.supplies')}
        center="— Förrådet —"
        right={
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
            <button
              onClick={() => { resetTypeForm(); setMutError(null); setShowNewType(true) }}
              className="btn-secondary"
              style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, textTransform: 'uppercase' }}
            >
              {t('supplies.newType')}
            </button>
            <button
              onClick={() => { resetBatchForm(); setMutError(null); setAddBatchCategoryFilter(null); setShowAddBatch(true) }}
              className="btn-primary"
              style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, textTransform: 'uppercase' }}
            >
              {t('common.add')}
            </button>
          </div>
        }
      />

      <div className="page-body">
        {CATEGORIES.map(cat => {
          const items = pagedGrouped.get(cat) ?? []
          const allExpanded = items.length > 0 && items.every(item => expanded.has(item.type.id))

          return (
            <div key={cat} style={{ marginBottom: 40 }}>
              {/* Category section header */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                marginBottom: 8,
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
                  <span style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.8,
                    textTransform: 'uppercase',
                    color: 'var(--color-forest)',
                  }}>
                    § {t(`supplyCategory.${cat}`)}
                  </span>
                  {items.length > 1 && (
                    <button
                      onClick={() => {
                        setExpanded(prev => {
                          const next = new Set(prev)
                          if (allExpanded) {
                            items.forEach(item => next.delete(item.type.id))
                          } else {
                            items.forEach(item => next.add(item.type.id))
                          }
                          return next
                        })
                      }}
                      style={{
                        background: 'transparent',
                        border: 'none',
                        fontFamily: 'var(--font-mono)',
                        fontSize: 9,
                        letterSpacing: 1.2,
                        textTransform: 'uppercase',
                        color: 'var(--color-accent)',
                        cursor: 'pointer',
                        padding: 0,
                      }}
                    >
                      {allExpanded ? t('supplies.collapseAll') : t('supplies.expandAll')}
                    </button>
                  )}
                </div>
                <button
                  onClick={() => { resetTypeForm(); setTypeCategory(cat); setTypeUnit(DEFAULT_UNIT[cat] ?? 'COUNT'); setMutError(null); setShowNewType(true) }}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.2,
                    textTransform: 'uppercase',
                    color: 'var(--color-accent)',
                    cursor: 'pointer',
                  }}
                >
                  {t(`supplies.addCategoryType.${cat}`)}
                </button>
              </div>

              {items.length === 0 && (
                <div style={{
                  padding: '20px 0',
                  fontFamily: 'var(--font-display)',
                  fontStyle: 'italic',
                  fontSize: 13,
                  color: 'var(--color-forest)',
                  opacity: 0.6,
                  borderTop: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                }}>
                  {t('supplies.noBatches')}
                </div>
              )}

              {items.length > 0 && (
                <div>
                  {/* Ledger header */}
                  <div style={headerStyle as React.CSSProperties}>
                    <span>{t('supplies.typeName')}</span>
                    <span style={{ textAlign: 'right' }}>{t('supplies.quantity')}</span>
                    <span />
                  </div>

                  {/* Type rows */}
                  {items.map(item => {
                    const isOpen = expanded.has(item.type.id)
                    const visibleBatches = (showUsed ? item.batches : item.batches.filter(b => b.quantity > 0))
                      .slice().sort((a, b) => (a.quantity === 0 ? 1 : 0) - (b.quantity === 0 ? 1 : 0))
                    const hasUsed = item.batches.some(b => b.quantity === 0)

                    return (
                      <div key={item.type.id}>
                        {/* Type row — click to expand */}
                        <button
                          onClick={() => toggleExpand(item.type.id)}
                          style={{
                            display: 'grid',
                            gridTemplateColumns: TYPE_TEMPLATE,
                            gap: 18,
                            padding: '14px 0',
                            borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                            width: '100%',
                            background: 'transparent',
                            border: 'none',
                            textAlign: 'left',
                            cursor: 'pointer',
                            alignItems: 'center',
                          }}
                          className="ledger-row"
                        >
                          {/* Name + inline add */}
                          <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 }}>
                            <span style={{
                              fontFamily: 'var(--font-mono)',
                              fontSize: 10,
                              color: 'var(--color-forest)',
                              opacity: 0.5,
                              flexShrink: 0,
                            }}>
                              {isOpen ? '▼' : '▶'}
                            </span>
                            <span style={{
                              fontFamily: 'var(--font-display)',
                              fontSize: 18,
                              fontWeight: 300,
                              color: 'var(--color-ink)',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis',
                              whiteSpace: 'nowrap',
                            }}>
                              {item.label}
                            </span>
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
                              style={{
                                fontFamily: 'var(--font-mono)',
                                fontSize: 9,
                                letterSpacing: 1.2,
                                textTransform: 'uppercase',
                                color: 'var(--color-accent)',
                                cursor: 'pointer',
                                flexShrink: 0,
                              }}
                            >
                              {t('common.add')}
                            </span>
                          </div>

                          {/* Total quantity (or obegränsad indicator) */}
                          <div style={{
                            fontFamily: 'var(--font-display)',
                            fontSize: 16,
                            fontVariantNumeric: 'tabular-nums',
                            color: item.type.inexhaustible ? 'var(--color-accent)' : 'var(--color-ink)',
                            textAlign: 'right',
                          }}>
                            {item.type.inexhaustible && item.batches.length === 0
                              ? 'obegränsad'
                              : item.type.inexhaustible
                                ? `${formatUnit(item.totalQuantity, item.type.unit, t)} · obegränsad`
                                : formatUnit(item.totalQuantity, item.type.unit, t)}
                          </div>

                          {/* Edit pencil */}
                          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                            <span
                              role="button"
                              onClick={e => {
                                e.stopPropagation()
                                setEditType(item.type)
                                setEditTypeName(item.type.name)
                                setEditTypeUnit(item.type.unit)
                                setEditTypeProps(item.type.properties ?? {})
                                setEditTypeInexhaustible(item.type.inexhaustible)
                                setMutError(null)
                              }}
                              style={{
                                color: 'var(--color-forest)',
                                opacity: 0.5,
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                              }}
                              aria-label={t('common.edit')}
                            >
                              <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M9.5 2.5 L11.5 4.5 L4.5 11.5 L2 12 L2.5 9.5 Z" />
                                <line x1="8" y1="4" x2="10" y2="6" />
                              </svg>
                            </span>
                          </div>
                        </button>

                        {/* Expanded batch rows */}
                        {isOpen && (
                          <div style={{
                            borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                          }}>
                            {hasUsed && (
                              <label style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 8,
                                padding: '8px 0 8px 32px',
                                fontFamily: 'var(--font-mono)',
                                fontSize: 9,
                                letterSpacing: 1.2,
                                textTransform: 'uppercase',
                                color: 'var(--color-forest)',
                                cursor: 'pointer',
                              }}>
                                <input type="checkbox" checked={showUsed} onChange={e => setShowUsed(e.target.checked)} className="rounded" />
                                {t('supplies.showUsed')}
                              </label>
                            )}

                            {visibleBatches.length === 0 && (
                              <p style={{
                                padding: '12px 0 12px 32px',
                                fontFamily: 'var(--font-display)',
                                fontStyle: 'italic',
                                fontSize: 13,
                                color: 'var(--color-forest)',
                                opacity: 0.6,
                              }}>
                                {t('supplies.noBatches')}
                              </p>
                            )}

                            {visibleBatches.map(batch => {
                              const seasonName = seasons?.find(s => s.id === batch.seasonId)?.name
                              return (
                                <div
                                  key={batch.id}
                                  style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'space-between',
                                    padding: '10px 0 10px 32px',
                                    borderTop: '1px solid color-mix(in srgb, var(--color-ink) 12%, transparent)',
                                  }}
                                >
                                  <div style={{
                                    display: 'flex',
                                    gap: 14,
                                    alignItems: 'baseline',
                                    flexWrap: 'wrap',
                                  }}>
                                    <span style={{
                                      fontFamily: 'var(--font-display)',
                                      fontSize: 16,
                                      fontVariantNumeric: 'tabular-nums',
                                      color: 'var(--color-ink)',
                                    }}>
                                      {formatUnit(batch.quantity, batch.unit, t)}
                                    </span>
                                    {batch.costCents != null && (
                                      <span style={{
                                        fontFamily: 'var(--font-mono)',
                                        fontSize: 10,
                                        color: 'var(--color-forest)',
                                        opacity: 0.7,
                                      }}>
                                        {formatCost(batch.costCents)}
                                        {batch.quantity > 0 && ` (${formatCost(Math.round(batch.costCents / batch.quantity))}/${t(`supplyUnit.${batch.unit}`)})`}
                                      </span>
                                    )}
                                    {seasonName && (
                                      <span style={{
                                        fontFamily: 'var(--font-mono)',
                                        fontSize: 10,
                                        color: 'var(--color-forest)',
                                        opacity: 0.55,
                                      }}>
                                        {seasonName}
                                      </span>
                                    )}
                                    {batch.notes && (
                                      <span style={{
                                        fontFamily: 'var(--font-display)',
                                        fontStyle: 'italic',
                                        fontSize: 13,
                                        color: 'var(--color-forest)',
                                        opacity: 0.65,
                                      }}>
                                        {batch.notes}
                                      </span>
                                    )}
                                  </div>
                                  <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                                    <button
                                      onClick={() => { setDecrementBatch(batch); setDecrementAmount(String(batch.quantity)); setMutError(null) }}
                                      style={{
                                        fontFamily: 'var(--font-mono)',
                                        fontSize: 9,
                                        letterSpacing: 1.2,
                                        textTransform: 'uppercase',
                                        color: 'var(--color-accent)',
                                        background: 'color-mix(in srgb, var(--color-accent) 10%, transparent)',
                                        border: '1px solid color-mix(in srgb, var(--color-accent) 30%, transparent)',
                                        borderRadius: 4,
                                        padding: '4px 8px',
                                        cursor: 'pointer',
                                      }}
                                    >
                                      {t('supplies.decrement')}
                                    </button>
                                    <button
                                      onClick={() => {
                                        setEditBatch(batch)
                                        setEditBatchQuantity(String(batch.quantity))
                                        setEditBatchCost(batch.costCents != null ? String(batch.costCents / 100) : '')
                                        setEditBatchSeasonId(batch.seasonId ?? '')
                                        setEditBatchNotes(batch.notes ?? '')
                                        setMutError(null)
                                      }}
                                      style={{
                                        background: 'transparent',
                                        border: 'none',
                                        color: 'var(--color-forest)',
                                        opacity: 0.5,
                                        cursor: 'pointer',
                                        padding: 2,
                                        display: 'flex',
                                        alignItems: 'center',
                                      }}
                                      aria-label={t('common.edit')}
                                    >
                                      <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M9.5 2.5 L11.5 4.5 L4.5 11.5 L2 12 L2.5 9.5 Z" />
                                        <line x1="8" y1="4" x2="10" y2="6" />
                                      </svg>
                                    </button>
                                    <button
                                      onClick={() => { setDeleteBatch(batch); setMutError(null) }}
                                      style={{
                                        background: 'transparent',
                                        border: 'none',
                                        color: 'var(--color-forest)',
                                        opacity: 0.45,
                                        cursor: 'pointer',
                                        padding: 2,
                                        display: 'flex',
                                        alignItems: 'center',
                                      }}
                                      aria-label={t('common.delete')}
                                    >
                                      <svg width="13" height="13" viewBox="0 0 14 14" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
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
                        )}
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          )
        })}
        <LedgerPagination
          page={inventoryPage}
          pageSize={inventoryPageSize}
          total={allInventoryEntries.length}
          onChange={setInventoryPage}
        />
      </div>

      {/* Add batch dialog */}
      <Dialog
        open={showAddBatch}
        onClose={() => { setShowAddBatch(false); resetBatchForm(); setMutError(null) }}
        title={t('supplies.addBatch')}
        actions={
          <>
            <button onClick={() => { setShowAddBatch(false); resetBatchForm(); setMutError(null) }} className="btn-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => createBatchMut.mutate()}
              disabled={!batchTypeId || (isPackageMode ? (!batchPackageSize || !batchPackageCount) : !batchQuantity) || createBatchMut.isPending}
              className="btn-primary"
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
                    <label className="field-label">{selectedBatchType?.unit === 'COUNT' ? t('supplies.pricePerUnit') : t('supplies.packageCost')} (kr)</label>
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
            <button onClick={() => { setShowNewType(false); resetTypeForm(); setMutError(null) }} className="btn-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => createTypeMut.mutate()}
              disabled={!typeCategory || !typeUnit || !arePropertiesValid(typeCategory, typeProps) || createTypeMut.isPending}
              className="btn-primary"
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
          <label style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 8 }}>
            <input
              type="checkbox"
              checked={typeInexhaustible}
              onChange={e => setTypeInexhaustible(e.target.checked)}
            />
            <span>
              <span style={{ display: 'block', fontSize: 14 }}>Obegränsad</span>
              <span style={{ display: 'block', fontSize: 11, color: 'var(--color-forest)' }}>
                Behöver inte spåras (t.ex. egen hästgödsel).
              </span>
            </span>
          </label>
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
            <button onClick={() => { setEditType(null); setMutError(null) }} className="btn-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => updateTypeMut.mutate()}
              disabled={!editTypeUnit || (editType ? !arePropertiesValid(editType.category, editTypeProps) : false) || updateTypeMut.isPending}
              className="btn-primary"
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
          <label style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <input
              type="checkbox"
              checked={editTypeInexhaustible}
              onChange={e => setEditTypeInexhaustible(e.target.checked)}
            />
            <span style={{ fontSize: 14 }}>Obegränsad</span>
          </label>
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
            <button onClick={() => { setEditBatch(null); setMutError(null) }} className="btn-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => updateBatchMut.mutate()}
              disabled={!editBatchQuantity || updateBatchMut.isPending}
              className="btn-primary"
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
            <button onClick={() => { setDecrementBatch(null); setDecrementAmount(''); setMutError(null) }} className="btn-secondary">{t('common.cancel')}</button>
            <button
              onClick={() => decrementMut.mutate()}
              disabled={!decrementAmount || Number(decrementAmount) <= 0 || decrementMut.isPending}
              className="btn-primary"
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
            <button onClick={() => { setDeleteBatch(null); setMutError(null) }} className="btn-secondary">{t('common.cancel')}</button>
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
            <button onClick={() => { setDeleteType(null); setMutError(null) }} className="btn-secondary">{t('common.cancel')}</button>
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
