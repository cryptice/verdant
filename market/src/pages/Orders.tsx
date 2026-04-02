import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, Fragment } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type MarketOrderResponse } from '../api/client'

const STATUS_COLORS: Record<string, string> = {
  PLACED: 'bg-blue-100 text-blue-700',
  ACCEPTED: 'bg-green-100 text-green-700',
  FULFILLED: 'bg-purple-100 text-purple-700',
  DELIVERED: 'bg-gray-100 text-gray-600',
  CANCELLED: 'bg-red-100 text-red-700',
}

export function Orders() {
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
  if (error) return (
    <div className="text-center py-12">
      <p className="text-error text-sm mb-3">{error instanceof Error ? error.message : 'Something went wrong'}</p>
      <button onClick={() => refetch()} className="btn-secondary text-sm">Try again</button>
    </div>
  )

  return (
    <div>
      <h1 className="text-xl font-semibold text-text-primary mb-5">{t('orders.title')}</h1>

      {data && data.length === 0 && (
        <p className="text-text-secondary text-sm text-center py-8">{t('orders.noOrders')}</p>
      )}

      {data && data.length > 0 && (
        <div className="border border-divider rounded-xl overflow-hidden bg-bg shadow-sm">
          <table className="w-full">
            <thead>
              <tr className="border-b border-divider bg-surface">
                <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.date')}</th>
                <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.producer')}</th>
                <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.status')}</th>
                <th className="text-right px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.total')}</th>
                <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary hidden sm:table-cell">{t('orders.delivery')}</th>
                <th className="px-4 py-2"></th>
              </tr>
            </thead>
            <tbody>
              {data.map(order => (
                <Fragment key={order.id}>
                  <tr
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
                    <td className="px-4 py-2.5 text-sm text-right tabular-nums">{formatTotal(order.totalSek)}</td>
                    <td className="px-4 py-2.5 text-sm text-text-secondary hidden sm:table-cell">{order.deliveryDate ?? '\u2014'}</td>
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
                    <tr>
                      <td colSpan={6} className="px-6 py-3 bg-surface/50">
                        <div className="space-y-1">
                          {order.items.map(item => (
                            <div key={item.id} className="flex items-center justify-between text-xs text-text-secondary">
                              <span>{item.speciesName}</span>
                              <span className="tabular-nums">
                                {item.quantity} x {(item.pricePerStemSek / 100).toFixed(2)} kr
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
                </Fragment>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Cancel confirmation dialog */}
      {cancelItem !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/20" onClick={() => { setCancelItem(null); setCancelError(null) }} />
          <div className="relative bg-bg border border-divider rounded-xl shadow-lg max-w-sm w-full">
            <div className="px-5 py-4 border-b border-divider">
              <h2 className="text-base font-semibold text-text-primary">{t('orders.cancel')}</h2>
            </div>
            <div className="px-5 py-4">
              <p className="text-text-secondary">{t('orders.cancelConfirm')}</p>
              {cancelError && <p className="text-error text-sm mt-2">{cancelError}</p>}
            </div>
            <div className="px-5 py-3 border-t border-divider flex justify-end gap-2">
              <button onClick={() => { setCancelItem(null); setCancelError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
              <button onClick={() => cancelMut.mutate(cancelItem.id)} className="px-4 py-2 text-sm text-error font-semibold">{t('orders.cancel')}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
