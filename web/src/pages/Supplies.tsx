import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useEffect } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type SupplyTypeResponse, type SupplyInventoryResponse } from '../api/client'
import { Masthead, LedgerPagination } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { AddBatchDialog } from '../components/supplies/AddBatchDialog'
import { NewTypeDialog } from '../components/supplies/NewTypeDialog'
import { EditTypeDialog } from '../components/supplies/EditTypeDialog'
import { EditBatchDialog } from '../components/supplies/EditBatchDialog'
import { DecrementBatchDialog } from '../components/supplies/DecrementBatchDialog'
import { DeleteConfirmDialog } from '../components/supplies/DeleteConfirmDialog'
import { SupplyCategorySection } from '../components/supplies/SupplyCategorySection'
import { CATEGORIES, DEFAULT_UNIT } from './supplies/constants'
import { deriveTypeName, groupByCategory, type GroupedType } from './supplies/utils'

export { isValidNpk } from './supplies/utils'

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

          return (
            <SupplyCategorySection
              key={cat}
              cat={cat}
              items={items}
              expanded={expanded}
              showUsed={showUsed}
              setShowUsed={setShowUsed}
              seasons={seasons ?? []}
              onToggleExpand={toggleExpand}
              onToggleExpandAll={(catItems, allExpanded) => {
                setExpanded(prev => {
                  const next = new Set(prev)
                  if (allExpanded) {
                    catItems.forEach(item => next.delete(item.type.id))
                  } else {
                    catItems.forEach(item => next.add(item.type.id))
                  }
                  return next
                })
              }}
              onAddCategoryType={() => { resetTypeForm(); setTypeCategory(cat); setTypeUnit(DEFAULT_UNIT[cat] ?? 'COUNT'); setMutError(null); setShowNewType(true) }}
              onAddBatchToType={(item) => {
                resetBatchForm()
                setMutError(null)
                setBatchTypeId(item.type.id)
                setAddBatchCategoryFilter(null)
                setShowAddBatch(true)
              }}
              onEditType={(item) => {
                setEditType(item.type)
                setEditTypeName(item.type.name)
                setEditTypeUnit(item.type.unit)
                setEditTypeProps(item.type.properties ?? {})
                setEditTypeInexhaustible(item.type.inexhaustible)
                setMutError(null)
              }}
              onDecrementBatch={(batch) => { setDecrementBatch(batch); setDecrementAmount(String(batch.quantity)); setMutError(null) }}
              onEditBatch={(batch) => {
                setEditBatch(batch)
                setEditBatchQuantity(String(batch.quantity))
                setEditBatchCost(batch.costCents != null ? String(batch.costCents / 100) : '')
                setEditBatchSeasonId(batch.seasonId ?? '')
                setEditBatchNotes(batch.notes ?? '')
                setMutError(null)
              }}
              onDeleteBatch={(batch) => { setDeleteBatch(batch); setMutError(null) }}
              t={t}
            />
          )
        })}
        <LedgerPagination
          page={inventoryPage}
          pageSize={inventoryPageSize}
          total={allInventoryEntries.length}
          onChange={setInventoryPage}
        />
      </div>

      <AddBatchDialog
        open={showAddBatch}
        onClose={() => { setShowAddBatch(false); resetBatchForm(); setMutError(null) }}
        onSubmit={() => createBatchMut.mutate()}
        isPending={createBatchMut.isPending}
        types={types ?? []}
        seasons={seasons ?? []}
        selectedBatchType={selectedBatchType}
        isPackageMode={isPackageMode}
        addBatchCategoryFilter={addBatchCategoryFilter}
        batchTypeId={batchTypeId}
        setBatchTypeId={setBatchTypeId}
        batchQuantity={batchQuantity}
        setBatchQuantity={setBatchQuantity}
        batchPackageSize={batchPackageSize}
        setBatchPackageSize={setBatchPackageSize}
        batchPackageCount={batchPackageCount}
        setBatchPackageCount={setBatchPackageCount}
        batchCost={batchCost}
        setBatchCost={setBatchCost}
        batchSeasonId={batchSeasonId}
        setBatchSeasonId={setBatchSeasonId}
        batchNotes={batchNotes}
        setBatchNotes={setBatchNotes}
        onOpenNewType={() => { setShowAddBatch(false); setShowNewType(true) }}
        mutError={mutError}
        t={t}
      />

      <NewTypeDialog
        open={showNewType}
        onClose={() => { setShowNewType(false); resetTypeForm(); setMutError(null) }}
        onSubmit={() => createTypeMut.mutate()}
        isPending={createTypeMut.isPending}
        typeName={typeName}
        setTypeName={setTypeName}
        typeNameEdited={typeNameEdited}
        setTypeNameEdited={setTypeNameEdited}
        typeCategory={typeCategory}
        setTypeCategory={setTypeCategory}
        typeUnit={typeUnit}
        setTypeUnit={setTypeUnit}
        typeProps={typeProps}
        setTypeProps={setTypeProps}
        typeInexhaustible={typeInexhaustible}
        setTypeInexhaustible={setTypeInexhaustible}
        mutError={mutError}
        t={t}
      />

      <EditTypeDialog
        editType={editType}
        onClose={() => { setEditType(null); setMutError(null) }}
        onSubmit={() => updateTypeMut.mutate()}
        isPending={updateTypeMut.isPending}
        editTypeName={editTypeName}
        setEditTypeName={setEditTypeName}
        editTypeUnit={editTypeUnit}
        setEditTypeUnit={setEditTypeUnit}
        editTypeProps={editTypeProps}
        setEditTypeProps={setEditTypeProps}
        editTypeInexhaustible={editTypeInexhaustible}
        setEditTypeInexhaustible={setEditTypeInexhaustible}
        onDeleteRequested={() => { setEditType(null); if (editType) { setDeleteType(editType) } }}
        mutError={mutError}
        t={t}
      />

      <EditBatchDialog
        editBatch={editBatch}
        onClose={() => { setEditBatch(null); setMutError(null) }}
        onSubmit={() => updateBatchMut.mutate()}
        isPending={updateBatchMut.isPending}
        editBatchQuantity={editBatchQuantity}
        setEditBatchQuantity={setEditBatchQuantity}
        editBatchCost={editBatchCost}
        setEditBatchCost={setEditBatchCost}
        editBatchSeasonId={editBatchSeasonId}
        setEditBatchSeasonId={setEditBatchSeasonId}
        editBatchNotes={editBatchNotes}
        setEditBatchNotes={setEditBatchNotes}
        seasons={seasons ?? []}
        onDeleteRequested={() => { const b = editBatch; setEditBatch(null); if (b) setDeleteBatch(b) }}
        mutError={mutError}
        t={t}
      />

      <DecrementBatchDialog
        decrementBatch={decrementBatch}
        onClose={() => { setDecrementBatch(null); setDecrementAmount(''); setMutError(null) }}
        onSubmit={() => decrementMut.mutate()}
        isPending={decrementMut.isPending}
        decrementAmount={decrementAmount}
        setDecrementAmount={setDecrementAmount}
        mutError={mutError}
        t={t}
      />

      <DeleteConfirmDialog
        open={deleteBatch !== null}
        onClose={() => { setDeleteBatch(null); setMutError(null) }}
        onConfirm={() => deleteBatch && deleteBatchMut.mutate(deleteBatch.id)}
        isPending={deleteBatchMut.isPending}
        mutError={mutError}
        t={t}
      />

      <DeleteConfirmDialog
        open={deleteType !== null}
        onClose={() => { setDeleteType(null); setMutError(null) }}
        onConfirm={() => deleteType && deleteTypeMut.mutate(deleteType.id)}
        isPending={deleteTypeMut.isPending}
        mutError={mutError}
        t={t}
      />
    </div>
  )
}
