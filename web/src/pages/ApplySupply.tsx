import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'

export function ApplySupply() {
  const [params] = useSearchParams()
  const bedId = Number(params.get('bedId'))
  const initialPlantIds = params.get('plantIds')?.split(',').map(Number) ?? []
  const stepId = params.get('stepId') ? Number(params.get('stepId')) : undefined
  const suggestedSupplyTypeId = params.get('supplyTypeId') ? Number(params.get('supplyTypeId')) : undefined
  const suggestedQuantity = params.get('quantity') ? Number(params.get('quantity')) : undefined

  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()

  const [scope, setScope] = useState<'BED' | 'PLANTS'>(initialPlantIds.length > 0 ? 'PLANTS' : 'BED')
  const [selectedPlantIds, setSelectedPlantIds] = useState<number[]>(initialPlantIds)
  const [inventoryId, setInventoryId] = useState<number | null>(null)
  const [quantity, setQuantity] = useState<string>(suggestedQuantity != null ? String(suggestedQuantity) : '')
  const [notes, setNotes] = useState('')
  const [showAllCategories, setShowAllCategories] = useState(false)

  const { data: bedPlants } = useQuery({
    queryKey: ['bed-plants', bedId],
    queryFn: () => api.beds.plants(bedId),
  })

  const { data: inventory } = useQuery({
    queryKey: ['supply-inventory'],
    queryFn: () => api.supplies.list(),
  })

  const { data: supplyTypes } = useQuery({
    queryKey: ['supply-types'],
    queryFn: () => api.supplies.types(),
  })

  // Pick first matching lot when suggestedSupplyTypeId arrives
  useEffect(() => {
    if (suggestedSupplyTypeId && inventory && inventoryId == null) {
      const firstLot = inventory.find(i => i.supplyTypeId === suggestedSupplyTypeId && i.quantity > 0)
      if (firstLot) setInventoryId(firstLot.id)
    }
  }, [suggestedSupplyTypeId, inventory, inventoryId])

  const selectedLot = inventory?.find(i => i.id === inventoryId)
  const visibleInventory = (inventory ?? []).filter(i => {
    if (i.quantity <= 0) return false
    if (showAllCategories) return true
    return i.category === 'FERTILIZER'
  })

  const quantityNum = Number(quantity)
  const quantityExceeds = selectedLot != null && quantityNum > selectedLot.quantity

  const createMut = useMutation({
    mutationFn: () => api.supplyApplications.create({
      bedId,
      supplyInventoryId: inventoryId!,
      quantity: quantityNum,
      targetScope: scope,
      plantIds: scope === 'PLANTS' ? selectedPlantIds : undefined,
      workflowStepId: stepId,
      notes: notes || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bed-applications', bedId] })
      qc.invalidateQueries({ queryKey: ['supply-inventory'] })
      if (stepId) qc.invalidateQueries({ queryKey: ['workflow-progress'] })
      navigate(-1)
    },
  })

  const canSubmit = inventoryId != null &&
    quantityNum > 0 &&
    !quantityExceeds &&
    (scope === 'BED' || selectedPlantIds.length > 0)

  return (
    <div className="max-w-lg">
      <PageHeader title={t('supplyApplication.applyTitle')} />

      <div className="form-card space-y-4">
        <fieldset>
          <label className="field-label">{t('supplyApplication.targetLabel')}</label>
          <div className="flex gap-4">
            <label className="flex items-center gap-2">
              <input type="radio" name="scope" value="BED" checked={scope === 'BED'} onChange={() => setScope('BED')} />
              <span>{t('supplyApplication.wholeBed')}</span>
            </label>
            <label className="flex items-center gap-2">
              <input type="radio" name="scope" value="PLANTS" checked={scope === 'PLANTS'} onChange={() => setScope('PLANTS')} />
              <span>{t('supplyApplication.selectedPlants')}</span>
            </label>
          </div>
        </fieldset>

        {scope === 'PLANTS' && bedPlants && (
          <div>
            <label className="field-label">{t('supplyApplication.selectPlants')}</label>
            <div className="border border-divider rounded p-2 max-h-60 overflow-y-auto">
              {bedPlants.filter(p => p.status !== 'REMOVED').map(p => (
                <label key={p.id} className="flex items-center gap-2 py-1">
                  <input
                    type="checkbox"
                    checked={selectedPlantIds.includes(p.id)}
                    onChange={e => {
                      setSelectedPlantIds(prev =>
                        e.target.checked ? [...prev, p.id] : prev.filter(id => id !== p.id))
                    }}
                  />
                  <span className="text-sm">{p.name}</span>
                </label>
              ))}
            </div>
          </div>
        )}

        <div>
          <label className="field-label">{t('supplyApplication.selectSupply')}</label>
          <select value={inventoryId ?? ''} onChange={e => setInventoryId(Number(e.target.value) || null)} className="input">
            <option value="">{t('common.select')}</option>
            {visibleInventory.map(i => {
              const type = supplyTypes?.find(st => st.id === i.supplyTypeId)
              return (
                <option key={i.id} value={i.id}>
                  {type?.name ?? i.supplyTypeName} ({i.quantity.toFixed(2)} {(type?.unit ?? i.unit).toLowerCase()} {t('supplyApplication.remaining')})
                </option>
              )
            })}
          </select>
          <label className="flex items-center gap-2 mt-2 text-xs">
            <input type="checkbox" checked={showAllCategories} onChange={e => setShowAllCategories(e.target.checked)} />
            <span>{t('supplyApplication.showAllCategories')}</span>
          </label>
        </div>

        <div>
          <label className="field-label">{t('supplyApplication.quantity')}</label>
          <div className="flex items-center gap-2">
            <input type="number" step="0.01" value={quantity} onChange={e => setQuantity(e.target.value)} className="input flex-1" />
            <span className="text-sm text-text-secondary">
              {selectedLot ? (supplyTypes?.find(st => st.id === selectedLot.supplyTypeId)?.unit ?? selectedLot.unit).toLowerCase() : ''}
            </span>
          </div>
          {quantityExceeds && (
            <p className="text-error text-xs mt-1">{t('supplyApplication.insufficientQuantity')}</p>
          )}
        </div>

        <div>
          <label className="field-label">{t('common.notesLabel')}</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2} className="input" />
        </div>
      </div>

      {createMut.error && <p className="text-error text-sm mt-3">{(createMut.error as Error).message}</p>}

      <div className="mt-4 flex justify-end gap-2">
        <button onClick={() => navigate(-1)} className="px-4 py-2 text-sm text-text-secondary">
          {t('common.cancel')}
        </button>
        <button onClick={() => createMut.mutate()} disabled={!canSubmit || createMut.isPending} className="btn-primary">
          {createMut.isPending ? t('supplyApplication.submitting') : t('supplyApplication.submit')}
        </button>
      </div>
    </div>
  )
}
