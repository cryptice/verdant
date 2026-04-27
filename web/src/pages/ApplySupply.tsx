import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Rule } from '../components/faltet'

const selectLabelStyle: React.CSSProperties = {
  display: 'block',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  marginBottom: 4,
}

const selectStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  backgroundColor: 'transparent',
  border: 'none',
  borderBottom: '1px solid var(--color-ink)',
  borderRadius: 0,
  padding: '4px 0',
  fontFamily: 'var(--font-display)',
  fontSize: 20,
  fontWeight: 300,
  color: 'var(--color-ink)',
  outline: 'none',
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
      {children}
    </div>
  )
}

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
  // Selection: encode as either `lot:<inventoryId>` or `type:<typeId>`.
  const [selectedKey, setSelectedKey] = useState<string>('')
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

  type SupplyOption =
    | { kind: 'lot'; key: string; label: string; inv: { id: number; quantity: number } }
    | { kind: 'type'; key: string; label: string; typeId: number }

  const supplyOptions: SupplyOption[] = (() => {
    const matchesCategory = (cat: string) => showAllCategories || cat === 'FERTILIZER'
    const lots: SupplyOption[] = (inventory ?? [])
      .filter(i => i.quantity > 0 && matchesCategory(i.category))
      .map(i => ({
        kind: 'lot',
        key: `lot:${i.id}`,
        label: `${i.supplyTypeName} · ${i.quantity} ${i.unit.toLowerCase()}`,
        inv: { id: i.id, quantity: i.quantity },
      }))
    const seen = new Set((inventory ?? []).map(i => i.supplyTypeId))
    void seen
    const inex: SupplyOption[] = (supplyTypes ?? [])
      .filter(typ => typ.inexhaustible && matchesCategory(typ.category))
      .map(typ => ({
        kind: 'type',
        key: `type:${typ.id}`,
        label: `${typ.name} · obegränsad`,
        typeId: typ.id,
      }))
    return [...lots, ...inex].sort((a, b) => a.label.localeCompare(b.label))
  })()

  // Q11 pre-selection: prefer largest finite lot; fall back to inexhaustible type.
  useEffect(() => {
    if (!suggestedSupplyTypeId || !inventory || !supplyTypes || selectedKey) return
    const matchingLots = inventory.filter(i => i.supplyTypeId === suggestedSupplyTypeId && i.quantity > 0)
    if (matchingLots.length > 0) {
      const biggest = matchingLots.reduce((a, b) => (a.quantity >= b.quantity ? a : b))
      setSelectedKey(`lot:${biggest.id}`)
    } else {
      const type = supplyTypes.find(typ => typ.id === suggestedSupplyTypeId && typ.inexhaustible)
      if (type) setSelectedKey(`type:${type.id}`)
    }
  }, [suggestedSupplyTypeId, inventory, supplyTypes, selectedKey])

  const selectedOpt = supplyOptions.find(o => o.key === selectedKey)
  const selectedLot = selectedOpt?.kind === 'lot'
    ? inventory?.find(i => i.id === selectedOpt.inv.id)
    : undefined
  const selectedType = selectedOpt?.kind === 'type'
    ? supplyTypes?.find(st => st.id === selectedOpt.typeId)
    : selectedLot
      ? supplyTypes?.find(st => st.id === selectedLot.supplyTypeId)
      : undefined
  const quantityNum = Number(quantity)
  const quantityExceeds = selectedLot != null && quantityNum > selectedLot.quantity

  const createMut = useMutation({
    mutationFn: () => api.supplyApplications.create({
      bedId,
      supplyInventoryId: selectedOpt?.kind === 'lot' ? selectedOpt.inv.id : undefined,
      supplyTypeId: selectedOpt?.kind === 'type' ? selectedOpt.typeId : undefined,
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

  const canSubmit = selectedOpt != null &&
    quantityNum > 0 &&
    !quantityExceeds &&
    (scope === 'BED' || selectedPlantIds.length > 0)

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.tasks')} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>{t('supplyApplication.applyTitle')}</span>
          </span>
        }
        center={t('form.masthead.center')}
      />

      <div className="page-body-tight">
        {/* § Applicera på */}
        <SectionLabel>§ {t('supplyApplication.targetLabel')}</SectionLabel>
        <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>

        <div style={{ marginTop: 14, display: 'flex', gap: 24 }}>
          <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer', fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 300 }}>
            <input type="radio" name="scope" value="BED" checked={scope === 'BED'} onChange={() => setScope('BED')} />
            {t('supplyApplication.wholeBed')}
          </label>
          <label style={{ display: 'flex', alignItems: 'center', gap: 10, cursor: 'pointer', fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 300 }}>
            <input type="radio" name="scope" value="PLANTS" checked={scope === 'PLANTS'} onChange={() => setScope('PLANTS')} />
            {t('supplyApplication.selectedPlants')}
          </label>
        </div>

        {/* Plant checklist — shown only in PLANTS mode */}
        {scope === 'PLANTS' && bedPlants && (
          <div style={{ marginTop: 20, border: '1px solid var(--color-ink)', background: 'var(--color-paper)', padding: '10px 14px', maxHeight: 240, overflowY: 'auto' }}>
            <span style={selectLabelStyle}>{t('supplyApplication.selectPlants')}</span>
            {bedPlants.filter(p => p.status !== 'REMOVED').map(p => (
              <label
                key={p.id}
                style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 0', cursor: 'pointer', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 12%, transparent)' }}
              >
                <input
                  type="checkbox"
                  checked={selectedPlantIds.includes(p.id)}
                  onChange={e => {
                    setSelectedPlantIds(prev =>
                      e.target.checked ? [...prev, p.id] : prev.filter(id => id !== p.id))
                  }}
                />
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{p.name}</span>
              </label>
            ))}
          </div>
        )}

        {/* § Förnödenhet */}
        <div style={{ marginTop: 28 }}>
          <SectionLabel>§ {t('supplyApplication.selectSupply')}</SectionLabel>
          <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>
        </div>

        <div style={{ marginTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
          {/* Supply select */}
          <div style={{ gridColumn: '1 / -1' }}>
            <label style={{ display: 'block' }}>
              <span style={selectLabelStyle}>{t('supplyApplication.selectSupply')}</span>
              <select
                value={selectedKey}
                onChange={e => setSelectedKey(e.target.value)}
                style={selectStyle}
              >
                <option value="">{t('common.select')}</option>
                {supplyOptions.map(opt => (
                  <option key={opt.key} value={opt.key}>{opt.label}</option>
                ))}
              </select>
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10, fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, cursor: 'pointer' }}>
              <input type="checkbox" checked={showAllCategories} onChange={e => setShowAllCategories(e.target.checked)} />
              {t('supplyApplication.showAllCategories')}
            </label>
          </div>

          {/* Quantity */}
          <div>
            <span style={selectLabelStyle}>{t('supplyApplication.quantity')}</span>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
              <input
                type="number"
                step="0.01"
                value={quantity}
                onChange={e => setQuantity(e.target.value)}
                style={{ ...selectStyle, width: 'auto', flex: 1 }}
              />
              {selectedType && (
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', opacity: 0.7 }}>
                  {selectedType.unit.toLowerCase()}
                </span>
              )}
            </div>
            {quantityExceeds && (
              <p style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.2, color: 'var(--color-error)', marginTop: 4 }}>
                {t('supplyApplication.insufficientQuantity')}
              </p>
            )}
          </div>
        </div>

        {/* Notes */}
        <div style={{ marginTop: 28, border: '1px solid var(--color-ink)', background: 'var(--color-paper)', padding: '14px 16px' }}>
          <div style={selectLabelStyle}>{t('common.notesLabel')}</div>
          <textarea
            value={notes}
            onChange={e => setNotes(e.target.value)}
            rows={3}
            style={{ width: '100%', minHeight: 80, background: 'transparent', border: 'none', outline: 'none', fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, resize: 'vertical', boxSizing: 'border-box' }}
          />
        </div>

        {createMut.error && (
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-error)', marginTop: 12 }}>
            {(createMut.error as Error).message}
          </p>
        )}
      </div>

      {/* Sticky footer */}
      <div className="sticky-footer">
        <button className="btn-secondary" onClick={() => navigate(-1)}>
          {t('common.cancel')}
        </button>
        <button
          className="btn-primary"
          onClick={() => createMut.mutate()}
          disabled={!canSubmit || createMut.isPending}
        >
          {createMut.isPending ? t('supplyApplication.submitting') : t('supplyApplication.submit')}
        </button>
      </div>
    </div>
  )
}
