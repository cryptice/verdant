import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type Provider } from '../api/client'
import { useState } from 'react'
import ErrorDisplay from '../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'

export default function ProvidersPage() {
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editIdentifier, setEditIdentifier] = useState('')
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [newIdentifier, setNewIdentifier] = useState('')
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const { t } = useTranslation()

  const { data: providers, isLoading, error } = useQuery({
    queryKey: ['admin', 'providers'],
    queryFn: api.admin.getProviders
  })

  const createMutation = useMutation({
    mutationFn: api.admin.createProvider,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'providers'] })
      setCreating(false)
      setNewName('')
      setNewIdentifier('')
    }
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, ...req }: { id: number; name: string; identifier: string }) =>
      api.admin.updateProvider(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'providers'] })
      setEditingId(null)
    }
  })

  const deleteMutation = useMutation({
    mutationFn: api.admin.deleteProvider,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'providers'] })
      setDeletingId(null)
    }
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => queryClient.invalidateQueries({ queryKey: ['admin', 'providers'] })} />

  return (
    <div>
      <div className="flex items-start justify-between gap-3 mb-5 sm:mb-6">
        <div className="min-w-0">
          <h2 className="text-xl sm:text-2xl font-semibold text-[#37352F]">{t('providers.title')}</h2>
          <p className="text-sm text-[#787774] mt-1">{t('providers.count', { count: providers?.length || 0 })}</p>
        </div>
        {!creating && (
          <button
            onClick={() => setCreating(true)}
            className="shrink-0 px-3 py-2 sm:py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] transition-colors text-sm font-medium"
          >
            {t('common.new')}
          </button>
        )}
      </div>

      {creating && (
        <div className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5 mb-6 bg-[#FBFBFA]">
          <h3 className="text-sm font-semibold text-[#37352F] mb-4">{t('providers.newProvider')}</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('providers.name')}</label>
              <input
                type="text"
                value={newName}
                onChange={e => setNewName(e.target.value)}
                placeholder={t('providers.namePlaceholder')}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('providers.identifier')}</label>
              <input
                type="text"
                value={newIdentifier}
                onChange={e => setNewIdentifier(e.target.value)}
                placeholder={t('providers.identifierPlaceholder')}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
              />
            </div>
          </div>
          <div className="flex gap-2 mt-4">
            <button
              onClick={() => createMutation.mutate({ name: newName, identifier: newIdentifier })}
              disabled={!newName.trim() || !newIdentifier.trim() || createMutation.isPending}
              className="px-3 py-2 sm:py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors text-sm font-medium"
            >
              {createMutation.isPending ? t('providers.creating') : t('providers.create')}
            </button>
            <button
              onClick={() => { setCreating(false); setNewName(''); setNewIdentifier('') }}
              className="px-3 py-2 sm:py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
            >
              {t('common.cancel')}
            </button>
          </div>
        </div>
      )}

      {/* Mobile card list */}
      <div className="md:hidden space-y-2">
        {providers?.map((p: Provider) => (
          <div key={p.id} className="border border-[#E9E9E7] rounded-lg p-3 bg-white">
            {editingId === p.id ? (
              <div className="space-y-2">
                <div>
                  <label className="block text-xs font-medium text-[#787774] mb-1">{t('providers.name')}</label>
                  <input
                    type="text"
                    value={editName}
                    onChange={e => setEditName(e.target.value)}
                    className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md text-sm bg-[#FBFBFA] focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-[#787774] mb-1">{t('providers.identifier')}</label>
                  <input
                    type="text"
                    value={editIdentifier}
                    onChange={e => setEditIdentifier(e.target.value)}
                    className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md text-sm bg-[#FBFBFA] focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none"
                  />
                </div>
                <div className="flex justify-end gap-3 pt-1">
                  <button onClick={() => setEditingId(null)} className="text-[#787774] text-sm px-2 py-1">{t('common.cancel')}</button>
                  <button
                    onClick={() => updateMutation.mutate({ id: p.id, name: editName, identifier: editIdentifier })}
                    disabled={updateMutation.isPending}
                    className="text-[#2EAADC] text-sm font-medium px-2 py-1"
                  >
                    {t('common.save')}
                  </button>
                </div>
              </div>
            ) : (
              <>
                <div className="flex items-baseline justify-between gap-2 min-w-0">
                  <span className="text-sm font-medium text-[#37352F] truncate">{p.name}</span>
                  <span className="text-xs text-[#A5A29C] shrink-0">{p.identifier}</span>
                </div>
                <div className="mt-2 pt-2 border-t border-[#E9E9E7] flex justify-end">
                  {deletingId === p.id ? (
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => deleteMutation.mutate(p.id)}
                        disabled={deleteMutation.isPending}
                        className="text-[#E03E3E] text-sm font-medium px-2 py-1"
                      >
                        {t('common.confirm')}
                      </button>
                      <button onClick={() => setDeletingId(null)} className="text-[#787774] text-sm px-2 py-1">{t('common.cancel')}</button>
                    </div>
                  ) : (
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => { setEditingId(p.id); setEditName(p.name); setEditIdentifier(p.identifier) }}
                        className="text-[#787774] text-sm hover:text-[#37352F] px-2 py-1 transition-colors"
                      >
                        {t('common.edit')}
                      </button>
                      <button
                        onClick={() => setDeletingId(p.id)}
                        className="text-[#787774] text-sm hover:text-[#E03E3E] px-2 py-1 transition-colors"
                      >
                        {t('common.delete')}
                      </button>
                    </div>
                  )}
                </div>
              </>
            )}
          </div>
        ))}
      </div>

      {/* Desktop table */}
      <div className="hidden md:block border border-[#E9E9E7] rounded-lg overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#E9E9E7] bg-[#FBFBFA]">
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('providers.name')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('providers.identifier')}</th>
              <th className="text-right px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('providers.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {providers?.map((p: Provider) => (
              <tr key={p.id} className="border-b border-[#E9E9E7] last:border-0 hover:bg-[#FBFBFA] transition-colors">
                <td className="px-4 py-2.5">
                  {editingId === p.id ? (
                    <input
                      type="text"
                      value={editName}
                      onChange={e => setEditName(e.target.value)}
                      className="px-2 py-1 border border-[#E9E9E7] rounded-md text-sm"
                    />
                  ) : (
                    <span className="text-sm font-medium text-[#37352F]">{p.name}</span>
                  )}
                </td>
                <td className="px-4 py-2.5">
                  {editingId === p.id ? (
                    <input
                      type="text"
                      value={editIdentifier}
                      onChange={e => setEditIdentifier(e.target.value)}
                      className="px-2 py-1 border border-[#E9E9E7] rounded-md text-sm"
                    />
                  ) : (
                    <span className="text-sm text-[#787774]">{p.identifier}</span>
                  )}
                </td>
                <td className="px-4 py-2.5 text-right">
                  {editingId === p.id ? (
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => updateMutation.mutate({ id: p.id, name: editName, identifier: editIdentifier })}
                        disabled={updateMutation.isPending}
                        className="text-[#2EAADC] text-sm font-medium hover:underline"
                      >
                        {t('common.save')}
                      </button>
                      <button onClick={() => setEditingId(null)} className="text-[#787774] text-sm hover:underline">
                        {t('common.cancel')}
                      </button>
                    </div>
                  ) : deletingId === p.id ? (
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => deleteMutation.mutate(p.id)}
                        disabled={deleteMutation.isPending}
                        className="text-[#E03E3E] text-sm font-medium hover:underline"
                      >
                        {t('common.confirm')}
                      </button>
                      <button onClick={() => setDeletingId(null)} className="text-[#787774] text-sm hover:underline">
                        {t('common.cancel')}
                      </button>
                    </div>
                  ) : (
                    <div className="flex items-center justify-end gap-3">
                      <button
                        onClick={() => { setEditingId(p.id); setEditName(p.name); setEditIdentifier(p.identifier) }}
                        className="text-[#787774] text-sm hover:text-[#37352F] transition-colors"
                      >
                        {t('common.edit')}
                      </button>
                      <button onClick={() => setDeletingId(p.id)} className="text-[#787774] text-sm hover:text-[#E03E3E] transition-colors">
                        {t('common.delete')}
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
