import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, Fragment } from 'react'
import { useTranslation } from 'react-i18next'
import { api, type ListingResponse } from '../api/client'

interface CartItem {
  listing: ListingResponse
  quantity: number
}

export function Browse() {
  const { t, i18n } = useTranslation()
  const queryClient = useQueryClient()
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['market-listings'],
    queryFn: () => api.market.listings(),
  })

  const [search, setSearch] = useState('')
  const [cart, setCart] = useState<CartItem[]>([])
  const [showCart, setShowCart] = useState(false)
  const [deliveryDate, setDeliveryDate] = useState('')
  const [orderNotes, setOrderNotes] = useState('')
  const [orderError, setOrderError] = useState<string | null>(null)
  const [successMsg, setSuccessMsg] = useState<string | null>(null)

  const placeMut = useMutation({
    mutationFn: () => api.market.placeOrder({
      deliveryDate: deliveryDate || undefined,
      notes: orderNotes || undefined,
      items: cart.map(ci => ({
        listingId: ci.listing.id,
        quantity: ci.quantity,
      })),
    }),
    onSuccess: () => {
      setCart([])
      setShowCart(false)
      setDeliveryDate('')
      setOrderNotes('')
      setOrderError(null)
      setSuccessMsg(t('market.orderPlaced'))
      setTimeout(() => setSuccessMsg(null), 3000)
      queryClient.invalidateQueries({ queryKey: ['market-listings'] })
    },
    onError: (err) => {
      setOrderError(err instanceof Error ? err.message : String(err))
    },
  })

  const addToCart = (listing: ListingResponse) => {
    setCart(prev => {
      const existing = prev.find(ci => ci.listing.id === listing.id)
      if (existing) {
        return prev.map(ci =>
          ci.listing.id === listing.id
            ? { ...ci, quantity: ci.quantity + 1 }
            : ci
        )
      }
      return [...prev, { listing, quantity: 1 }]
    })
  }

  const removeFromCart = (listingId: number) => {
    setCart(prev => prev.filter(ci => ci.listing.id !== listingId))
  }

  const updateCartQty = (listingId: number, qty: number) => {
    if (qty <= 0) {
      removeFromCart(listingId)
      return
    }
    setCart(prev => prev.map(ci =>
      ci.listing.id === listingId ? { ...ci, quantity: qty } : ci
    ))
  }

  const cartTotal = cart.reduce((sum, ci) => sum + ci.quantity * ci.listing.pricePerStemSek, 0)
  const cartItemCount = cart.reduce((sum, ci) => sum + ci.quantity, 0)

  const formatPrice = (cents: number) => `${(cents / 100).toFixed(2)} ${t('market.perStem')}`
  const formatTotal = (cents: number) => `${(cents / 100).toFixed(2)} kr`

  const speciesDisplayName = (listing: ListingResponse) => {
    if (i18n.language === 'sv' && listing.speciesNameSv) {
      return listing.speciesNameSv
    }
    return listing.speciesName
  }

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return (
    <div className="text-center py-12">
      <p className="text-error text-sm mb-3">{error instanceof Error ? error.message : 'Something went wrong'}</p>
      <button onClick={() => refetch()} className="btn-secondary text-sm">Try again</button>
    </div>
  )

  const filtered = data?.filter(l => {
    if (!search) return true
    const q = search.toLowerCase()
    return l.speciesName.toLowerCase().includes(q)
      || (l.speciesNameSv?.toLowerCase().includes(q))
      || l.title.toLowerCase().includes(q)
      || l.producerName.toLowerCase().includes(q)
  }) ?? []

  return (
    <div>
      <h1 className="text-xl font-semibold text-text-primary mb-1">{t('market.title')}</h1>
      <p className="text-text-secondary text-sm mb-5">{t('market.browse')}</p>

      {successMsg && (
        <div className="mb-4 px-4 py-3 rounded-xl bg-green-50 text-green-700 text-sm font-medium">
          {successMsg}
        </div>
      )}

      <div className="flex flex-col sm:flex-row gap-3 mb-6">
        <input
          type="text"
          value={search}
          onChange={e => setSearch(e.target.value)}
          placeholder={t('market.search')}
          className="input flex-1"
        />
        <button
          onClick={() => { setOrderError(null); setShowCart(true) }}
          className="btn-primary relative whitespace-nowrap"
        >
          {t('market.cart')}
          {cartItemCount > 0 && (
            <span className="ml-2 inline-flex items-center justify-center px-1.5 py-0.5 rounded-full text-xs bg-white/20 font-medium">
              {cartItemCount} {t('market.items')} &middot; {formatTotal(cartTotal)}
            </span>
          )}
        </button>
      </div>

      {filtered.length === 0 && (
        <p className="text-text-secondary text-sm text-center py-8">{t('market.noListings')}</p>
      )}

      {filtered.length > 0 && (
        <div className="grid gap-4 grid-cols-1 sm:grid-cols-2">
          {filtered.map(listing => (
            <div
              key={listing.id}
              className="border border-divider rounded-xl p-4 bg-bg shadow-sm hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0 flex-1">
                  <h3 className="font-medium text-sm text-text-primary truncate">
                    {speciesDisplayName(listing)}
                  </h3>
                  {listing.title && (
                    <p className="text-xs text-text-secondary mt-0.5 truncate">{listing.title}</p>
                  )}
                </div>
                {listing.quantityAvailable < 20 && (
                  <span className="inline-block px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 text-amber-700 whitespace-nowrap">
                    {t('market.lowStock')}
                  </span>
                )}
              </div>

              {listing.description && (
                <p className="text-xs text-text-muted mt-2 line-clamp-2">{listing.description}</p>
              )}

              <div className="mt-3 flex items-center gap-3 text-xs text-text-secondary">
                <span>{listing.producerName}</span>
                <span className="font-medium text-accent">{formatPrice(listing.pricePerStemSek)}</span>
              </div>

              <div className="mt-2 flex items-center gap-3 text-xs text-text-muted">
                <span>{listing.quantityAvailable} {t('market.stems')}</span>
                <span>{listing.availableFrom} &mdash; {listing.availableUntil}</span>
              </div>

              <div className="mt-3">
                <button
                  onClick={() => addToCart(listing)}
                  className="text-xs font-medium text-accent hover:underline"
                >
                  {t('market.addToOrder')}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Cart / Place Order dialog */}
      {showCart && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/20" onClick={() => { setShowCart(false); setOrderError(null) }} />
          <div className="relative bg-bg border border-divider rounded-xl shadow-lg max-w-md w-full max-h-[80vh] flex flex-col">
            <div className="px-5 py-4 border-b border-divider">
              <h2 className="text-base font-semibold text-text-primary">{t('market.cart')}</h2>
            </div>
            <div className="flex-1 overflow-y-auto px-5 py-4 space-y-4">
              {cart.length === 0 && (
                <p className="text-text-secondary text-sm">{t('market.cartEmpty')}</p>
              )}
              {cart.map(ci => (
                <div key={ci.listing.id} className="flex items-center gap-3 border border-divider rounded-xl p-3">
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-text-primary truncate">{speciesDisplayName(ci.listing)}</p>
                    <p className="text-xs text-text-secondary">{formatPrice(ci.listing.pricePerStemSek)}</p>
                  </div>
                  <input
                    type="number"
                    min="1"
                    value={ci.quantity}
                    onChange={e => updateCartQty(ci.listing.id, Number(e.target.value))}
                    className="input w-16 text-center text-sm"
                  />
                  <button
                    onClick={() => removeFromCart(ci.listing.id)}
                    className="text-xs text-error hover:underline"
                  >
                    {t('common.delete')}
                  </button>
                </div>
              ))}

              {cart.length > 0 && (
                <Fragment>
                  <div className="border-t border-divider pt-3 flex justify-between text-sm font-medium">
                    <span>{t('market.total')}</span>
                    <span>{formatTotal(cartTotal)}</span>
                  </div>
                  <div>
                    <label className="field-label">{t('market.deliveryDate')}</label>
                    <input type="date" value={deliveryDate} onChange={e => setDeliveryDate(e.target.value)} className="input w-full" />
                  </div>
                  <div>
                    <label className="field-label">{t('market.notes')}</label>
                    <textarea value={orderNotes} onChange={e => setOrderNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input w-full" />
                  </div>
                </Fragment>
              )}

              {orderError && <p className="text-error text-sm">{orderError}</p>}
            </div>
            <div className="px-5 py-3 border-t border-divider flex justify-end gap-2">
              <button onClick={() => { setShowCart(false); setOrderError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
              <button
                onClick={() => placeMut.mutate()}
                disabled={cart.length === 0 || placeMut.isPending}
                className="btn-primary text-sm"
              >
                {placeMut.isPending ? t('common.saving') : t('market.placeOrder')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
