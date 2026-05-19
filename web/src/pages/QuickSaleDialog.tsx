import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import {
  api,
  type CustomerResponse,
  type OutletResponse,
  type QuickSaleRequest,
  type SpeciesResponse,
  type UnitKind,
} from '../api/client'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

const QUICK_UNIT_KINDS: UnitKind[] = ['STEM', 'PLUG', 'BULB', 'TUBER', 'PLANT', 'BOUQUET']

type Props = {
  open: boolean
  outlets: OutletResponse[]
  onClose: () => void
  onSubmit: (req: QuickSaleRequest) => void
  isSaving?: boolean
  error?: string | null
}

export function QuickSaleDialog({ open, outlets, onClose, onSubmit, isSaving, error }: Props) {
  const { t } = useTranslation()
  const { data: customers } = useQuery({
    queryKey: ['customers'],
    queryFn: () => api.customers.list(),
    enabled: open,
  })

  const [unitKind, setUnitKind] = useState<UnitKind | null>(null)
  const [species, setSpecies] = useState<SpeciesResponse | null>(null)
  const [adhocLabel, setAdhocLabel] = useState('')
  const [qty, setQty] = useState('')
  const [price, setPrice] = useState('')
  const [outletId, setOutletId] = useState<number | ''>('')
  const [customerId, setCustomerId] = useState<number | ''>('')
  const [soldAt, setSoldAt] = useState<string>(() => new Date().toISOString().slice(0, 10))
  const [notes, setNotes] = useState('')

  const isBouquet = unitKind === 'BOUQUET'
  const qtyNum = parseInt(qty, 10)
  const priceCents = price ? Math.round(parseFloat(price.replace(',', '.')) * 100) : NaN

  const valid =
    !!unitKind &&
    (isBouquet ? adhocLabel.trim().length > 0 : species !== null) &&
    Number.isFinite(qtyNum) && qtyNum >= 1 &&
    Number.isFinite(priceCents) && priceCents >= 0 &&
    outletId !== ''

  const reset = () => {
    setUnitKind(null); setSpecies(null); setAdhocLabel('')
    setQty(''); setPrice(''); setOutletId(''); setCustomerId('')
    setSoldAt(new Date().toISOString().slice(0, 10)); setNotes('')
  }

  const handleClose = () => { reset(); onClose() }

  const handleSubmit = () => {
    if (!valid) return
    onSubmit({
      speciesId: isBouquet ? undefined : species!.id,
      adhocLabel: isBouquet ? adhocLabel.trim() : undefined,
      unitKind: unitKind!,
      quantity: qtyNum,
      pricePerUnitCents: priceCents,
      outletId: Number(outletId),
      customerId: customerId === '' ? undefined : Number(customerId),
      soldAt: soldAt || undefined,
      notes: notes.trim() || undefined,
    })
  }

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      title={t('sales.quickSale').replace('+ ', '')}
      actions={
        <>
          <button onClick={handleClose} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
          <button
            onClick={handleSubmit}
            disabled={!valid || isSaving}
            className="btn-primary text-sm"
          >
            {isSaving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label className="field-label">{t('sales.unitKind')} *</label>
          <div className="flex flex-wrap gap-2 mt-1">
            {QUICK_UNIT_KINDS.map((u) => (
              <button
                key={u}
                type="button"
                onClick={() => setUnitKind(u)}
                className={`px-3 py-1 rounded-full border text-sm ${
                  unitKind === u
                    ? 'bg-accent text-white border-accent'
                    : 'bg-surface border-divider text-text-secondary'
                }`}
              >
                {t(`unitKind.${u}`)}
              </button>
            ))}
          </div>
        </div>

        {isBouquet ? (
          <div>
            <label className="field-label">{t('saleLot.title')} *</label>
            <input
              type="text"
              value={adhocLabel}
              onChange={(e) => setAdhocLabel(e.target.value.slice(0, 200))}
              placeholder={t('sales.adhocLabelPlaceholder')}
              className="input"
            />
          </div>
        ) : (
          <div>
            <label className="field-label">{t('common.speciesLabel')}</label>
            <SpeciesAutocomplete value={species} onChange={setSpecies} />
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('sales.quantity')} *</label>
            <input
              type="number"
              min={1}
              value={qty}
              onChange={(e) => setQty(e.target.value.replace(/[^\d]/g, ''))}
              className="input"
            />
          </div>
          <div>
            <label className="field-label">{t('sales.pricePerUnit')} *</label>
            <input
              type="text"
              inputMode="decimal"
              value={price}
              onChange={(e) => setPrice(e.target.value.replace(/[^\d.,]/g, ''))}
              className="input"
            />
          </div>
        </div>

        <div>
          <label className="field-label">{t('sales.outlet')} *</label>
          <select
            value={outletId}
            onChange={(e) => setOutletId(e.target.value ? Number(e.target.value) : '')}
            className="input"
          >
            <option value="">{t('common.select')}</option>
            {outlets.map((o) => (
              <option key={o.id} value={o.id}>{o.name}</option>
            ))}
          </select>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('sales.soldAt')}</label>
            <input
              type="date"
              value={soldAt}
              onChange={(e) => setSoldAt(e.target.value)}
              className="input"
            />
          </div>
          <div>
            <label className="field-label">{t('sales.customer')}</label>
            <select
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : '')}
              className="input"
            >
              <option value="">{t('common.none')}</option>
              {(customers as CustomerResponse[] | undefined ?? []).map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
        </div>

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
    </Dialog>
  )
}
