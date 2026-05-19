import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useQuery } from '@tanstack/react-query'
import {
  api,
  type CustomerResponse,
  type EditSaleRequest,
  type SaleLedgerEntry,
} from '../api/client'
import { Dialog } from '../components/Dialog'

type Props = {
  entry: SaleLedgerEntry | null
  onClose: () => void
  onSubmit: (req: EditSaleRequest) => void
  isSaving?: boolean
  error?: string | null
}

export function EditSaleDialog({ entry, onClose, onSubmit, isSaving, error }: Props) {
  const { t } = useTranslation()
  const open = entry !== null

  const { data: customers } = useQuery({
    queryKey: ['customers'],
    queryFn: () => api.customers.list(),
    enabled: open,
  })

  const [qty, setQty] = useState('')
  const [price, setPrice] = useState('')
  const [customerId, setCustomerId] = useState<number | ''>('')
  const [soldAt, setSoldAt] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (entry) {
      setQty(String(entry.quantity))
      setPrice((entry.pricePerUnitCents / 100).toString())
      setCustomerId(entry.customerId ?? '')
      setSoldAt(entry.soldAt.slice(0, 10))
      setNotes(entry.notes ?? '')
    }
  }, [entry?.id])

  const qtyNum = parseInt(qty, 10)
  const priceCents = price ? Math.round(parseFloat(price.replace(',', '.')) * 100) : NaN
  const valid =
    Number.isFinite(qtyNum) && qtyNum >= 1 &&
    Number.isFinite(priceCents) && priceCents >= 0

  const handleSubmit = () => {
    if (!entry || !valid) return
    onSubmit({
      quantity: qtyNum,
      pricePerUnitCents: priceCents,
      customerId: customerId === '' ? undefined : Number(customerId),
      soldAt: soldAt || undefined,
      notes: notes.trim() || undefined,
    })
  }

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('sales.editSale')}
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">
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
      {entry && (
        <div className="space-y-4">
          <p className="text-sm text-text-secondary">
            {entry.sourceSummary ?? t(`sourceKind.${entry.sourceKind}`)}
            {' · '}
            <Link to={`/sale-lots/${entry.saleLotId}`} className="text-accent hover:underline">
              {t('saleLot.title')} #{entry.saleLotId}
            </Link>
          </p>

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
      )}
    </Dialog>
  )
}
