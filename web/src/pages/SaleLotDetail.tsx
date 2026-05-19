import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  api,
  type CustomerResponse,
  type RecordSaleRequest,
  type SaleLotEventResponse,
  type SaleResponse,
} from '../api/client'
import { Masthead, Ledger, Chip } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { Snackbar, useSnackbar } from '../components/Snackbar'

type DialogKind = 'none' | 'price' | 'outlet' | 'recordSale' | 'returned' | 'notSold' | 'delete'

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString('sv-SE', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('sv-SE', { dateStyle: 'short', timeStyle: 'short' })
  } catch {
    return iso
  }
}

const STATUS_TONE = {
  OFFERED: 'sage',
  SOLD_OUT: 'forest',
  NOT_SOLD: 'clay',
} as const

export function SaleLotDetail() {
  const { id } = useParams<{ id: string }>()
  const lotId = Number(id)
  const qc = useQueryClient()
  const { t } = useTranslation()
  const navigate = useNavigate()

  const detailQuery = useQuery({
    queryKey: ['sale-lot', lotId],
    queryFn: () => api.saleLots.get(lotId),
    enabled: Number.isFinite(lotId),
  })
  const outletsQuery = useQuery({
    queryKey: ['outlets'],
    queryFn: () => api.outlets.list(),
  })
  const customersQuery = useQuery({
    queryKey: ['customers'],
    queryFn: () => api.customers.list(),
  })

  const [dialog, setDialog] = useState<DialogKind>('none')
  const [actionError, setActionError] = useState<string | null>(null)
  const { message: toast, show: showToast } = useSnackbar()

  const invalidate = () => {
    qc.invalidateQueries({ queryKey: ['sale-lot', lotId] })
    qc.invalidateQueries({ queryKey: ['sale-lots'] })
    qc.invalidateQueries({ queryKey: ['sale-ledger'] })
  }

  const handleError = (err: unknown) => {
    const msg = err instanceof Error ? err.message : String(err)
    setActionError(msg)
    showToast(msg)
  }

  const changePriceMut = useMutation({
    mutationFn: (newPriceCents: number) => api.saleLots.changePrice(lotId, newPriceCents),
    onSuccess: () => { invalidate(); setDialog('none'); setActionError(null) },
    onError: handleError,
  })
  const changeOutletMut = useMutation({
    mutationFn: (newOutletId: number) => api.saleLots.changeOutlet(lotId, newOutletId),
    onSuccess: () => { invalidate(); setDialog('none'); setActionError(null) },
    onError: handleError,
  })
  const recordSaleMut = useMutation({
    mutationFn: (req: RecordSaleRequest) => api.saleLots.recordSale(lotId, req),
    onSuccess: () => { invalidate(); setDialog('none'); setActionError(null) },
    onError: handleError,
  })
  const returnMut = useMutation({
    mutationFn: (fromOutletId: number) => api.saleLots.markReturned(lotId, fromOutletId),
    onSuccess: () => { invalidate(); setDialog('none'); setActionError(null) },
    onError: handleError,
  })
  const notSoldMut = useMutation({
    mutationFn: () => api.saleLots.markNotSold(lotId),
    onSuccess: () => { invalidate(); setDialog('none'); setActionError(null) },
    onError: handleError,
  })
  const deleteMut = useMutation({
    mutationFn: () => api.saleLots.delete(lotId),
    onSuccess: () => { invalidate(); navigate('/sales') },
    onError: handleError,
  })

  if (detailQuery.isLoading) {
    return (
      <div className="flex justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (detailQuery.error) return <ErrorDisplay error={detailQuery.error} onRetry={detailQuery.refetch} />

  const detail = detailQuery.data
  if (!detail) return null
  const { lot, sales, events } = detail
  const outlets = outletsQuery.data ?? []
  const customers = customersQuery.data ?? []

  const openDialog = (kind: DialogKind) => { setActionError(null); setDialog(kind) }
  const isOffered = lot.status === 'OFFERED'

  return (
    <div>
      <Masthead
        left={
          <Link to="/sales" className="text-sm text-text-secondary hover:text-accent">
            ← {t('common.back')}
          </Link>
        }
        center={`${t('saleLot.title')} #${lot.id}`}
      />

      <div className="page-body">
        {/* Source + status header */}
        <div
          style={{
            display: 'flex', alignItems: 'flex-end', gap: 18, flexWrap: 'wrap',
            paddingBottom: 14, borderBottom: '1px solid var(--color-ink)', marginBottom: 18,
          }}
        >
          <div style={{ flex: 1, minWidth: 240 }}>
            <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 28 }}>
              {lot.sourceSummary ?? t(`sourceKind.${lot.sourceKind}`)}
            </div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', marginTop: 4 }}>
              {t(`sourceKind.${lot.sourceKind}`)} · {t(`unitKind.${lot.unitKind}`).toLowerCase()}
            </div>
          </div>
          <Chip tone={STATUS_TONE[lot.status]}>
            {t(`saleLotStatus.${lot.status}`)}
          </Chip>
        </div>

        {/* Stats */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
            gap: 18,
            marginBottom: 18,
          }}
        >
          <StatCell
            label={t('saleLot.remaining')}
            value={`${lot.quantityRemaining} / ${lot.quantityTotal}`}
            unit={t(`unitKind.${lot.unitKind}`).toLowerCase()}
          />
          <StatCell
            label={t('saleLot.currentPrice')}
            value={`${formatPrice(lot.currentRequestedPriceCents)}`}
            unit="KR"
            sub={
              lot.currentRequestedPriceCents !== lot.initialRequestedPriceCents
                ? `${t('saleLot.initialPrice').toLowerCase()}: ${formatPrice(lot.initialRequestedPriceCents)} KR`
                : undefined
            }
          />
          <StatCell
            label={t('saleLot.currentOutlet')}
            value={lot.currentOutletName}
          />
        </div>

        {/* Actions */}
        <div className="flex flex-wrap gap-2 mb-6">
          {isOffered && (
            <>
              <button className="btn-primary text-sm" onClick={() => openDialog('recordSale')}>
                {t('saleLot.recordSale')}
              </button>
              <button className="btn-secondary text-sm" onClick={() => openDialog('price')}>
                {t('saleLot.changePrice')}
              </button>
              <button className="btn-secondary text-sm" onClick={() => openDialog('outlet')}>
                {t('saleLot.changeOutlet')}
              </button>
              <button className="btn-secondary text-sm" onClick={() => openDialog('returned')}>
                {t('saleLot.markReturned')}
              </button>
              <button className="btn-secondary text-sm" onClick={() => openDialog('notSold')}>
                {t('saleLot.markNotSold')}
              </button>
            </>
          )}
          <button
            onClick={() => openDialog('delete')}
            className="text-sm text-error hover:underline ml-auto"
          >
            {t('saleLot.deleteLot')}
          </button>
        </div>

        {/* Sales history */}
        <h3
          style={{
            fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
            textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
            marginBottom: 6,
          }}
        >
          § {t('saleLot.salesHistory')}
        </h3>
        <Ledger
          columns={[
            { key: 'date', label: t('sales.soldAt'), width: '120px',
              render: (s: SaleResponse) => (
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11 }}>
                  {s.soldAt.slice(0, 10)}
                </span>
              ),
            },
            { key: 'qty', label: t('sales.quantity'), width: '90px', align: 'right',
              render: (s: SaleResponse) => (
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
                  {s.quantity}
                </span>
              ),
            },
            { key: 'price', label: t('sales.pricePerUnit'), width: '110px', align: 'right',
              render: (s: SaleResponse) => (
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
                  {formatPrice(s.pricePerUnitCents)}
                </span>
              ),
            },
            { key: 'outlet', label: t('sales.outlet'), width: '1fr',
              render: (s: SaleResponse) => (
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
                  {s.outletName}
                  {s.customerName ? ` · ${s.customerName}` : ''}
                </span>
              ),
            },
            { key: 'total', label: t('sales.total'), width: '110px', align: 'right',
              render: (s: SaleResponse) => (
                <span style={{ fontFamily: 'var(--font-mono)', fontSize: 14 }}>
                  {formatPrice(s.quantity * s.pricePerUnitCents)} KR
                </span>
              ),
            },
          ]}
          rows={sales}
          rowKey={(s: SaleResponse) => s.id}
          emptyMessage={t('sales.noSales')}
        />

        {/* Audit log */}
        <div style={{ marginTop: 28 }}>
          <h3
            style={{
              fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
              textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
              marginBottom: 6,
            }}
          >
            § {t('saleLot.auditLog')}
          </h3>
          <Ledger
            columns={[
              { key: 'date', label: t('sales.soldAt'), width: '170px',
                render: (e: SaleLotEventResponse) => (
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
                    {formatDate(e.createdAt)}
                  </span>
                ),
              },
              { key: 'type', label: '', width: '1fr',
                render: (e: SaleLotEventResponse) => (
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>
                    {t(`saleLotEventType.${e.eventType}`)}
                  </span>
                ),
              },
              { key: 'payload', label: '', width: '1.5fr',
                render: (e: SaleLotEventResponse) => (
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-forest)' }}>
                    {e.payloadJson ?? ''}
                  </span>
                ),
              },
            ]}
            rows={events}
            rowKey={(e: SaleLotEventResponse) => e.id}
            emptyMessage="—"
          />
        </div>
      </div>

      <ChangePriceDialog
        open={dialog === 'price'}
        currentCents={lot.currentRequestedPriceCents}
        onClose={() => setDialog('none')}
        onSubmit={(c) => changePriceMut.mutate(c)}
        isSaving={changePriceMut.isPending}
        error={actionError}
      />
      <ChangeOutletDialog
        open={dialog === 'outlet'}
        currentOutletId={lot.currentOutletId}
        outlets={outlets}
        onClose={() => setDialog('none')}
        onSubmit={(o) => changeOutletMut.mutate(o)}
        isSaving={changeOutletMut.isPending}
        error={actionError}
      />
      <RecordSaleDialog
        open={dialog === 'recordSale'}
        defaultPriceCents={lot.currentRequestedPriceCents}
        maxQuantity={lot.quantityRemaining}
        customers={customers}
        onClose={() => setDialog('none')}
        onSubmit={(req) => recordSaleMut.mutate(req)}
        isSaving={recordSaleMut.isPending}
        error={actionError}
      />
      <ReturnDialog
        open={dialog === 'returned'}
        currentOutletId={lot.currentOutletId}
        outlets={outlets}
        onClose={() => setDialog('none')}
        onSubmit={(id) => returnMut.mutate(id)}
        isSaving={returnMut.isPending}
        error={actionError}
      />
      <ConfirmDialog
        open={dialog === 'notSold'}
        title={t('saleLot.markNotSold')}
        message={t('saleLot.deleteLotConfirm')}
        confirmLabel={t('saleLot.markNotSold')}
        onClose={() => setDialog('none')}
        onConfirm={() => notSoldMut.mutate()}
        isSaving={notSoldMut.isPending}
        error={actionError}
      />
      <ConfirmDialog
        open={dialog === 'delete'}
        title={t('saleLot.deleteLot')}
        message={t('saleLot.deleteLotConfirm')}
        confirmLabel={t('common.delete')}
        danger
        onClose={() => setDialog('none')}
        onConfirm={() => deleteMut.mutate()}
        isSaving={deleteMut.isPending}
        error={actionError}
      />

      <Snackbar message={toast} />
    </div>
  )
}

// ── Small subcomponents ──

function StatCell({ label, value, unit, sub }: { label: string; value: string; unit?: string; sub?: string }) {
  return (
    <div>
      <div
        style={{
          fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
          textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
        }}
      >
        {label}
      </div>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 6, marginTop: 4 }}>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: 24, color: 'var(--color-ink)' }}>
          {value}
        </span>
        {unit && (
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-forest)', letterSpacing: 1.2 }}>
            {unit}
          </span>
        )}
      </div>
      {sub && (
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-forest)', marginTop: 2 }}>
          {sub}
        </div>
      )}
    </div>
  )
}

function ChangePriceDialog({
  open, currentCents, onClose, onSubmit, isSaving, error,
}: {
  open: boolean; currentCents: number; onClose: () => void
  onSubmit: (cents: number) => void; isSaving?: boolean; error?: string | null
}) {
  const { t } = useTranslation()
  const [v, setV] = useState('')
  const parsed = v ? Math.round(parseFloat(v.replace(',', '.')) * 100) : NaN
  const valid = Number.isFinite(parsed) && parsed >= 0

  return (
    <Dialog
      open={open}
      onClose={() => { setV(''); onClose() }}
      title={t('saleLot.changePrice')}
      actions={
        <>
          <button onClick={() => { setV(''); onClose() }} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
          <button onClick={() => valid && onSubmit(parsed)} disabled={!valid || isSaving} className="btn-primary text-sm">
            {isSaving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <p className="text-sm text-text-secondary mb-2">
        {t('saleLot.currentPrice')}: {formatPrice(currentCents)} KR
      </p>
      <label className="field-label">{t('saleLot.newPrice')} (KR)</label>
      <input
        type="text"
        inputMode="decimal"
        value={v}
        onChange={(e) => setV(e.target.value.replace(/[^\d.,]/g, ''))}
        className="input"
        autoFocus
      />
      {error && <p className="text-error text-sm mt-2">{error}</p>}
    </Dialog>
  )
}

function ChangeOutletDialog({
  open, currentOutletId, outlets, onClose, onSubmit, isSaving, error,
}: {
  open: boolean; currentOutletId: number
  outlets: { id: number; name: string }[]
  onClose: () => void; onSubmit: (id: number) => void
  isSaving?: boolean; error?: string | null
}) {
  const { t } = useTranslation()
  const [v, setV] = useState<number | ''>('')
  const valid = v !== '' && v !== currentOutletId

  return (
    <Dialog
      open={open}
      onClose={() => { setV(''); onClose() }}
      title={t('saleLot.changeOutlet')}
      actions={
        <>
          <button onClick={() => { setV(''); onClose() }} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
          <button
            onClick={() => valid && onSubmit(Number(v))}
            disabled={!valid || isSaving}
            className="btn-primary text-sm"
          >
            {isSaving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <label className="field-label">{t('saleLot.newOutlet')}</label>
      <select
        value={v}
        onChange={(e) => setV(e.target.value ? Number(e.target.value) : '')}
        className="input"
      >
        <option value="">{t('common.select')}</option>
        {outlets.map((o) => (
          <option key={o.id} value={o.id} disabled={o.id === currentOutletId}>
            {o.name}
          </option>
        ))}
      </select>
      {error && <p className="text-error text-sm mt-2">{error}</p>}
    </Dialog>
  )
}

function RecordSaleDialog({
  open, defaultPriceCents, maxQuantity, customers, onClose, onSubmit, isSaving, error,
}: {
  open: boolean; defaultPriceCents: number; maxQuantity: number
  customers: CustomerResponse[]
  onClose: () => void; onSubmit: (req: RecordSaleRequest) => void
  isSaving?: boolean; error?: string | null
}) {
  const { t } = useTranslation()
  const [qty, setQty] = useState('')
  const [price, setPrice] = useState(() => (defaultPriceCents / 100).toString())
  const [customerId, setCustomerId] = useState<number | ''>('')
  const [soldAt, setSoldAt] = useState(() => new Date().toISOString().slice(0, 10))
  const [notes, setNotes] = useState('')

  const qtyNum = parseInt(qty, 10)
  const priceCents = price ? Math.round(parseFloat(price.replace(',', '.')) * 100) : NaN
  const valid =
    Number.isFinite(qtyNum) && qtyNum >= 1 && qtyNum <= maxQuantity &&
    Number.isFinite(priceCents) && priceCents >= 0

  const reset = () => {
    setQty(''); setPrice((defaultPriceCents / 100).toString())
    setCustomerId(''); setSoldAt(new Date().toISOString().slice(0, 10)); setNotes('')
  }

  return (
    <Dialog
      open={open}
      onClose={() => { reset(); onClose() }}
      title={t('saleLot.recordSale').replace('+ ', '')}
      actions={
        <>
          <button onClick={() => { reset(); onClose() }} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
          <button
            onClick={() => valid && onSubmit({
              quantity: qtyNum,
              pricePerUnitCents: priceCents,
              customerId: customerId === '' ? undefined : Number(customerId),
              soldAt: soldAt || undefined,
              notes: notes.trim() || undefined,
            })}
            disabled={!valid || isSaving}
            className="btn-primary text-sm"
          >
            {isSaving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('sales.quantity')} * (max {maxQuantity})</label>
            <input
              type="number" min={1} max={maxQuantity}
              value={qty}
              onChange={(e) => setQty(e.target.value.replace(/[^\d]/g, ''))}
              className="input"
            />
          </div>
          <div>
            <label className="field-label">{t('sales.pricePerUnit')} *</label>
            <input
              type="text" inputMode="decimal"
              value={price}
              onChange={(e) => setPrice(e.target.value.replace(/[^\d.,]/g, ''))}
              className="input"
            />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('sales.soldAt')}</label>
            <input type="date" value={soldAt} onChange={(e) => setSoldAt(e.target.value)} className="input" />
          </div>
          <div>
            <label className="field-label">{t('sales.customer')}</label>
            <select
              value={customerId}
              onChange={(e) => setCustomerId(e.target.value ? Number(e.target.value) : '')}
              className="input"
            >
              <option value="">{t('common.none')}</option>
              {customers.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
        </div>
        <div>
          <label className="field-label">{t('common.notesLabel')}</label>
          <textarea
            value={notes} onChange={(e) => setNotes(e.target.value)}
            rows={2} placeholder={t('common.optional')} className="input"
          />
        </div>
        {error && <p className="text-error text-sm">{error}</p>}
      </div>
    </Dialog>
  )
}

function ReturnDialog({
  open, currentOutletId, outlets, onClose, onSubmit, isSaving, error,
}: {
  open: boolean; currentOutletId: number
  outlets: { id: number; name: string }[]
  onClose: () => void; onSubmit: (fromOutletId: number) => void
  isSaving?: boolean; error?: string | null
}) {
  const { t } = useTranslation()
  const [v, setV] = useState<number | ''>(currentOutletId)

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('saleLot.markReturned')}
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
          <button
            onClick={() => v !== '' && onSubmit(Number(v))}
            disabled={v === '' || isSaving}
            className="btn-primary text-sm"
          >
            {isSaving ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <label className="field-label">{t('saleLot.fromOutlet')}</label>
      <select
        value={v}
        onChange={(e) => setV(e.target.value ? Number(e.target.value) : '')}
        className="input"
      >
        <option value="">{t('common.select')}</option>
        {outlets.map((o) => (
          <option key={o.id} value={o.id}>{o.name}</option>
        ))}
      </select>
      {error && <p className="text-error text-sm mt-2">{error}</p>}
    </Dialog>
  )
}

function ConfirmDialog({
  open, title, message, confirmLabel, onClose, onConfirm, isSaving, error, danger,
}: {
  open: boolean; title: string; message: string; confirmLabel: string
  onClose: () => void; onConfirm: () => void
  isSaving?: boolean; error?: string | null; danger?: boolean
}) {
  const { t } = useTranslation()
  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={title}
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">
            {t('common.cancel')}
          </button>
          <button
            onClick={onConfirm}
            disabled={isSaving}
            className={danger ? 'px-4 py-2 text-sm text-error font-semibold' : 'btn-primary text-sm'}
          >
            {isSaving ? t('common.saving') : confirmLabel}
          </button>
        </>
      }
    >
      <p className="text-text-secondary">{message}</p>
      {error && <p className="text-error text-sm mt-2">{error}</p>}
    </Dialog>
  )
}
