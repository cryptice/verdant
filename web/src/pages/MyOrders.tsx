import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type MarketOrderResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const STATUS_COLORS: Record<string, string> = {
  PLACED: 'bg-blue-100 text-blue-700',
  ACCEPTED: 'bg-green-100 text-green-700',
  FULFILLED: 'bg-purple-100 text-purple-700',
  DELIVERED: 'bg-gray-100 text-gray-600',
  CANCELLED: 'bg-red-100 text-red-700',
}

export function MyOrders() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['my-orders'],
    queryFn: () => api.market.myOrders(),
  })

  const [expanded, setExpanded] = useState<number | null>(null)
  const [cancelItem, setCancelItem] = useState<MarketOrderResponse | null>(null)
  const [cancelError, setCancelError] = useState<string | null>(null)

  const cancelMut = useMutation({
    mutationFn: (id: number) => api.market.updateOrderStatus(id, 'CANCELLED'),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['my-orders'] }); setCancelItem(null); setCancelError(null) },
    onError: (err) => { setCancelError(err instanceof Error ? err.message : String(err)) },
  })

  const formatTotal = (cents: number) => `${(cents / 100).toFixed(2)} kr`

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title={t('orders.myTitle')} />
      <div className="px-4 py-4">
        {data && data.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-4">{t('orders.noOrders')}</p>
        )}

        {data && data.length > 0 && (
          <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
            <table className="w-full">
              <thead>
                <tr className="border-b border-divider bg-surface">
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.orderDate')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.producer')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.status')}</th>
                  <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.total')}</th>
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.deliveryDate')}</th>
                  <th className="px-4 py-2"></th>
                </tr>
              </thead>
              <tbody>
                {data.map(order => (
                  <>
                    <tr
                      key={order.id}
                      className="border-b border-divider last:border-0 hover:bg-surface cursor-pointer transition-colors"
                      onClick={() => setExpanded(expanded === order.id ? null : order.id)}
                    >
                      <td className="px-4 py-2.5 text-sm">{order.createdAt.slice(0, 10)}</td>
                      <td className="px-4 py-2.5 text-sm">{order.producerName}</td>
                      <td className="px-4 py-2.5 text-sm">
                        <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[order.status] ?? 'bg-gray-100 text-gray-600'}`}>
                          {t(`orderStatus.${order.status}` as const)}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{formatTotal(order.totalCents)}</td>
                      <td className="px-4 py-2.5 text-sm text-text-secondary">{order.deliveryDate ?? '—'}</td>
                      <td className="px-4 py-2.5 text-sm text-right">
                        {order.status === 'PLACED' && (
                          <button
                            onClick={e => { e.stopPropagation(); setCancelItem(order); setCancelError(null) }}
                            className="text-xs text-error hover:underline"
                          >
                            {t('orders.cancel')}
                          </button>
                        )}
                      </td>
                    </tr>
                    {expanded === order.id && (
                      <tr key={`${order.id}-items`}>
                        <td colSpan={6} className="px-6 py-3 bg-surface/50">
                          <div className="space-y-1">
                            {order.items.map(item => (
                              <div key={item.id} className="flex items-center justify-between text-xs text-text-secondary">
                                <span>{item.speciesName}</span>
                                <span className="tabular-nums">
                                  {item.quantity} x {(item.pricePerStemCents / 100).toFixed(2)} kr
                                </span>
                              </div>
                            ))}
                          </div>
                          {order.notes && (
                            <p className="text-xs text-text-muted mt-2 italic">{order.notes}</p>
                          )}
                        </td>
                      </tr>
                    )}
                  </>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <Dialog open={cancelItem !== null} onClose={() => { setCancelItem(null); setCancelError(null) }} title={t('orders.cancel')} actions={
        <>
          <button onClick={() => { setCancelItem(null); setCancelError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => cancelItem && cancelMut.mutate(cancelItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('orders.cancel')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('orders.cancelConfirm')}</p>
        {cancelError && <p className="text-error text-sm mt-2">{cancelError}</p>}
      </Dialog>
    </div>
  )
}
