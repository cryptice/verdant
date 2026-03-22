import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'

const STATUS_COLORS: Record<string, string> = {
  PLACED: 'bg-blue-100 text-blue-700',
  ACCEPTED: 'bg-green-100 text-green-700',
  FULFILLED: 'bg-purple-100 text-purple-700',
  DELIVERED: 'bg-gray-100 text-gray-600',
  CANCELLED: 'bg-red-100 text-red-700',
}

const NEXT_STATUS: Record<string, { target: string; labelKey: string }> = {
  PLACED: { target: 'ACCEPTED', labelKey: 'orders.accept' },
  ACCEPTED: { target: 'FULFILLED', labelKey: 'orders.fulfill' },
  FULFILLED: { target: 'DELIVERED', labelKey: 'orders.deliver' },
}

export function IncomingOrders() {
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['incoming-orders'],
    queryFn: () => api.market.incomingOrders(),
  })

  const [expanded, setExpanded] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const statusMut = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api.market.updateOrderStatus(id, status),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['incoming-orders'] }); setActionError(null) },
    onError: (err) => { setActionError(err instanceof Error ? err.message : String(err)) },
  })

  const formatTotal = (cents: number) => `${(cents / 100).toFixed(2)} kr`

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <PageHeader title={t('orders.incomingTitle')} />

      {actionError && (
        <div className="mb-4 px-4 py-3 rounded-xl bg-red-50 text-red-700 text-sm">
          {actionError}
        </div>
      )}

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
                  <th className="text-left px-4 py-2 text-xs font-medium text-text-secondary">{t('orders.purchaser')}</th>
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
                      <td className="px-4 py-2.5 text-sm">{order.purchaserName}</td>
                      <td className="px-4 py-2.5 text-sm">
                        <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-medium ${STATUS_COLORS[order.status] ?? 'bg-gray-100 text-gray-600'}`}>
                          {t(`orderStatus.${order.status}` as const)}
                        </span>
                      </td>
                      <td className="px-4 py-2.5 text-sm text-right tabular-nums">{formatTotal(order.totalCents)}</td>
                      <td className="px-4 py-2.5 text-sm text-text-secondary">{order.deliveryDate ?? '—'}</td>
                      <td className="px-4 py-2.5 text-sm text-right">
                        {NEXT_STATUS[order.status] && (
                          <button
                            onClick={e => {
                              e.stopPropagation()
                              statusMut.mutate({ id: order.id, status: NEXT_STATUS[order.status].target })
                            }}
                            disabled={statusMut.isPending}
                            className="text-xs font-medium text-accent hover:underline"
                          >
                            {t(NEXT_STATUS[order.status].labelKey as never)}
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
    </div>
  )
}
