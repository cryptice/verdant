import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  api,
  type EditSaleRequest,
  type QuickSaleRequest,
  type SaleLedgerEntry,
  type SaleLotResponse,
  type SaleLotStatus,
} from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Snackbar, useSnackbar } from '../components/Snackbar'
import { QuickSaleDialog } from './QuickSaleDialog'
import { EditSaleDialog } from './EditSaleDialog'

type Tab = 'OFFERED' | 'SOLD_OUT' | 'NOT_SOLD' | 'LEDGER'

const TAB_ORDER: { id: Tab; labelKey: string }[] = [
  { id: 'OFFERED',   labelKey: 'saleLotStatus.OFFERED' },
  { id: 'SOLD_OUT',  labelKey: 'saleLotStatus.SOLD_OUT' },
  { id: 'NOT_SOLD',  labelKey: 'saleLotStatus.NOT_SOLD' },
  { id: 'LEDGER',    labelKey: 'sales.title' },
]

function formatPrice(cents: number): string {
  return (cents / 100).toLocaleString('sv-SE', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

function formatDate(iso: string): string {
  try {
    return new Date(iso.slice(0, 10)).toLocaleDateString('sv-SE', { day: 'numeric', month: 'short' })
  } catch {
    return iso
  }
}

export function SalesScreen() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const navigate = useNavigate()

  const [tab, setTab] = useState<Tab>('LEDGER')
  const [selectedSeasonId, setSelectedSeasonId] = useState<number | ''>('')
  const [showQuickSale, setShowQuickSale] = useState(false)
  const [quickSaleError, setQuickSaleError] = useState<string | null>(null)
  const [editing, setEditing] = useState<SaleLedgerEntry | null>(null)
  const [editError, setEditError] = useState<string | null>(null)
  const { message: toast, show: showToast } = useSnackbar()

  const lotsQuery = useQuery({
    queryKey: ['sale-lots'],
    queryFn: () => api.saleLots.list({ limit: 500 }),
    enabled: tab !== 'LEDGER',
  })

  const seasonsQuery = useQuery({
    queryKey: ['seasons'],
    queryFn: () => api.seasons.list(),
  })

  const outletsQuery = useQuery({
    queryKey: ['outlets'],
    queryFn: () => api.outlets.list(),
  })

  // Pick the active season (or most recent) once seasons load.
  const defaultSeasonId = useMemo(() => {
    const seasons = seasonsQuery.data ?? []
    if (seasons.length === 0) return ''
    const active = seasons.find((s) => s.isActive)
    return (active ?? seasons[0]).id
  }, [seasonsQuery.data])

  const effectiveSeasonId = selectedSeasonId === '' ? defaultSeasonId : selectedSeasonId

  const ledgerQuery = useQuery({
    queryKey: ['sale-ledger', effectiveSeasonId],
    queryFn: () => api.sales.list({
      seasonId: typeof effectiveSeasonId === 'number' ? effectiveSeasonId : undefined,
    }),
    enabled: tab === 'LEDGER' && effectiveSeasonId !== '',
  })

  const quickSaleMut = useMutation({
    mutationFn: (req: QuickSaleRequest) => api.sales.recordQuick(req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sale-ledger'] })
      qc.invalidateQueries({ queryKey: ['sale-lots'] })
      setShowQuickSale(false)
      setQuickSaleError(null)
      showToast(t('sales.saleRecorded'))
    },
    onError: (err) => {
      const msg = err instanceof Error ? err.message : String(err)
      setQuickSaleError(msg)
      showToast(msg)
    },
  })

  const editMut = useMutation({
    mutationFn: ({ id, req }: { id: number; req: EditSaleRequest }) => api.sales.edit(id, req),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sale-ledger'] })
      qc.invalidateQueries({ queryKey: ['sale-lots'] })
      setEditing(null)
      setEditError(null)
    },
    onError: (err) => {
      const msg = err instanceof Error ? err.message : String(err)
      setEditError(msg)
      showToast(msg)
    },
  })

  const visibleLots: SaleLotResponse[] = useMemo(() => {
    if (tab === 'LEDGER' || !lotsQuery.data) return []
    return lotsQuery.data.filter((l) => l.status === (tab as SaleLotStatus))
  }, [lotsQuery.data, tab])

  const ledger = ledgerQuery.data ?? []
  const ledgerTotal = ledger.reduce((acc, e) => acc + e.totalCents, 0)
  const outlets = outletsQuery.data ?? []
  const noOutlets = outletsQuery.isFetched && outlets.length === 0

  const handleQuickSaleClick = () => {
    setQuickSaleError(null)
    setShowQuickSale(true)
  }

  return (
    <div>
      <Masthead
        left={t('nav.sales')}
        center={t('sales.ledgerTitle')}
        right={
          noOutlets ? (
            <Link to="/outlets" className="btn-primary">
              {t('sales.createOutletNow')}
            </Link>
          ) : (
            <button onClick={handleQuickSaleClick} className="btn-primary">
              {t('sales.quickSale')}
            </button>
          )
        }
      />

      <div className="page-body">
        {/* Tab strip */}
        <div
          style={{
            display: 'flex',
            gap: 18,
            borderBottom: '1px solid var(--color-ink)',
            marginBottom: 14,
          }}
        >
          {TAB_ORDER.map((entry) => {
            const active = tab === entry.id
            return (
              <button
                key={entry.id}
                onClick={() => setTab(entry.id)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  padding: '10px 0',
                  cursor: 'pointer',
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: active ? 'var(--color-accent)' : 'var(--color-forest)',
                  borderBottom: active ? '2px solid var(--color-accent)' : '2px solid transparent',
                  marginBottom: -1,
                }}
              >
                {t(entry.labelKey)}
              </button>
            )
          })}
        </div>

        {noOutlets && (
          <p className="text-sm text-text-secondary mb-3">{t('sales.noOutletWarning')}</p>
        )}

        {tab === 'LEDGER' ? (
          <LedgerTab
            seasons={seasonsQuery.data ?? []}
            selectedSeasonId={effectiveSeasonId}
            onSeasonChange={setSelectedSeasonId}
            ledger={ledger}
            ledgerTotal={ledgerTotal}
            isLoading={ledgerQuery.isLoading}
            error={ledgerQuery.error}
            onRetry={() => ledgerQuery.refetch()}
            onEntryClick={(e) => { setEditError(null); setEditing(e) }}
          />
        ) : (
          <LotsTab
            lots={visibleLots}
            isLoading={lotsQuery.isLoading}
            error={lotsQuery.error}
            onRetry={() => lotsQuery.refetch()}
            onLotClick={(id) => navigate(`/sale-lots/${id}`)}
          />
        )}
      </div>

      <QuickSaleDialog
        open={showQuickSale}
        outlets={outlets}
        onClose={() => setShowQuickSale(false)}
        onSubmit={(req) => quickSaleMut.mutate(req)}
        isSaving={quickSaleMut.isPending}
        error={quickSaleError}
      />

      <EditSaleDialog
        entry={editing}
        onClose={() => setEditing(null)}
        onSubmit={(req) => editing && editMut.mutate({ id: editing.id, req })}
        isSaving={editMut.isPending}
        error={editError}
      />

      <Snackbar message={toast} />
    </div>
  )
}

// ── Ledger tab ──

type LedgerTabProps = {
  seasons: { id: number; name: string; isActive: boolean }[]
  selectedSeasonId: number | ''
  onSeasonChange: (id: number | '') => void
  ledger: SaleLedgerEntry[]
  ledgerTotal: number
  isLoading: boolean
  error: unknown
  onRetry: () => void
  onEntryClick: (e: SaleLedgerEntry) => void
}

function LedgerTab({
  seasons, selectedSeasonId, onSeasonChange, ledger, ledgerTotal, isLoading, error, onRetry, onEntryClick,
}: LedgerTabProps) {
  const { t } = useTranslation()

  if (isLoading) {
    return (
      <div className="flex justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (error) return <ErrorDisplay error={error} onRetry={onRetry} />

  return (
    <div>
      <div className="flex items-center gap-3 mb-3 flex-wrap">
        <label className="field-label" style={{ margin: 0 }}>{t('sales.season')}:</label>
        <select
          value={selectedSeasonId}
          onChange={(e) => onSeasonChange(e.target.value ? Number(e.target.value) : '')}
          className="input"
          style={{ width: 'auto' }}
        >
          {seasons.length === 0 && <option value="">{t('sales.allSeasons')}</option>}
          {seasons.map((s) => (
            <option key={s.id} value={s.id}>{s.name}{s.isActive ? ' ●' : ''}</option>
          ))}
        </select>
        <div
          style={{
            marginLeft: 'auto',
            fontFamily: 'var(--font-mono)',
            fontSize: 11,
            letterSpacing: 1.2,
            color: 'var(--color-forest)',
          }}
        >
          {formatPrice(ledgerTotal)} KR · {ledger.length} {t('sales.title').toLowerCase()}
        </div>
      </div>

      <Ledger
        paginated
        pageSize={50}
        columns={[
          { key: 'date',  label: t('sales.soldAt'),   width: '90px',
            render: (e: SaleLedgerEntry) => (
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
                {formatDate(e.soldAt)}
              </span>
            ),
          },
          { key: 'source', label: t('sales.source'), width: '1.4fr',
            render: (e: SaleLedgerEntry) => (
              <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
                {e.sourceSummary ?? t(`sourceKind.${e.sourceKind}`)}
              </span>
            ),
          },
          { key: 'qty', label: t('sales.quantity'), width: '90px', align: 'right',
            render: (e: SaleLedgerEntry) => (
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
                {e.quantity} {t(`unitKind.${e.unitKind}`).toLowerCase()}
              </span>
            ),
          },
          { key: 'price', label: t('sales.pricePerUnit'), width: '110px', align: 'right',
            render: (e: SaleLedgerEntry) => (
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 13 }}>
                {formatPrice(e.pricePerUnitCents)}
              </span>
            ),
          },
          { key: 'outlet', label: t('sales.outlet'), width: '1fr',
            render: (e: SaleLedgerEntry) => (
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
                {e.outletName}
                {e.customerName ? ` · ${e.customerName}` : ''}
              </span>
            ),
          },
          { key: 'total', label: t('sales.total'), width: '110px', align: 'right',
            render: (e: SaleLedgerEntry) => (
              <span style={{ fontFamily: 'var(--font-mono)', fontSize: 14, color: 'var(--color-ink)' }}>
                {formatPrice(e.totalCents)} KR
              </span>
            ),
          },
        ]}
        rows={ledger}
        rowKey={(e: SaleLedgerEntry) => e.id}
        onRowClick={(e: SaleLedgerEntry) => onEntryClick(e)}
        emptyMessage={t('sales.noSales')}
      />
    </div>
  )
}

// ── Lots tab ──

type LotsTabProps = {
  lots: SaleLotResponse[]
  isLoading: boolean
  error: unknown
  onRetry: () => void
  onLotClick: (id: number) => void
}

function LotsTab({ lots, isLoading, error, onRetry, onLotClick }: LotsTabProps) {
  const { t } = useTranslation()

  if (isLoading) {
    return (
      <div className="flex justify-center p-16">
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (error) return <ErrorDisplay error={error} onRetry={onRetry} />

  return (
    <Ledger
      paginated
      pageSize={50}
      columns={[
        { key: 'source', label: t('sales.source'), width: '1.6fr',
          render: (l: SaleLotResponse) => (
            <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>
              {l.sourceSummary ?? t(`sourceKind.${l.sourceKind}`)}
            </span>
          ),
        },
        { key: 'remaining', label: t('saleLot.remaining'), width: '120px',
          render: (l: SaleLotResponse) => (
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12 }}>
              {l.quantityRemaining} {t('saleLot.of')} {l.quantityTotal}
              {' '}{t(`unitKind.${l.unitKind}`).toLowerCase()}
            </span>
          ),
        },
        { key: 'outlet', label: t('sales.outlet'), width: '1fr',
          render: (l: SaleLotResponse) => (
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)' }}>
              {l.currentOutletName}
            </span>
          ),
        },
        { key: 'price', label: t('saleLot.currentPrice'), width: '110px', align: 'right',
          render: (l: SaleLotResponse) => (
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 14 }}>
              {formatPrice(l.currentRequestedPriceCents)} KR
              {l.currentRequestedPriceCents !== l.initialRequestedPriceCents && (
                <span style={{ color: 'var(--color-clay)', marginLeft: 6 }}>↓</span>
              )}
            </span>
          ),
        },
        { key: 'goto', label: '', width: '40px', align: 'right',
          render: () => <span style={{ color: 'var(--color-accent)' }}>→</span>,
        },
      ]}
      rows={lots}
      rowKey={(l: SaleLotResponse) => l.id}
      onRowClick={(l: SaleLotResponse) => onLotClick(l.id)}
      emptyMessage={t('sales.noLots')}
    />
  )
}
