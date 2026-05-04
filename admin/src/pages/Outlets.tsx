import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type Outlet, type OutletChannel } from '../api/client'
import { useState } from 'react'
import ErrorDisplay from '../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'

const CHANNELS: OutletChannel[] = ['FLORIST', 'FARMERS_MARKET', 'CSA', 'WEDDING', 'WHOLESALE', 'DIRECT', 'OTHER']

export default function OutletsPage() {
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editName, setEditName] = useState('')
  const [editChannel, setEditChannel] = useState<OutletChannel>('FLORIST')
  const [editContactInfo, setEditContactInfo] = useState('')
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [newChannel, setNewChannel] = useState<OutletChannel>('FLORIST')
  const [newOrgId, setNewOrgId] = useState<number | null>(null)
  const [newContactInfo, setNewContactInfo] = useState('')
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const { t } = useTranslation()

  const { data: outlets, isLoading, error } = useQuery({
    queryKey: ['admin', 'outlets'],
    queryFn: api.admin.getOutlets,
  })

  const { data: orgs } = useQuery({
    queryKey: ['admin', 'organizations'],
    queryFn: api.admin.getOrganizations,
  })

  const createMutation = useMutation({
    mutationFn: api.admin.createOutlet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'outlets'] })
      setCreating(false)
      setNewName('')
      setNewChannel('FLORIST')
      setNewOrgId(null)
      setNewContactInfo('')
    },
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, ...req }: { id: number; name: string; channel: OutletChannel; contactInfo: string | null }) =>
      api.admin.updateOutlet(id, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'outlets'] })
      setEditingId(null)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: api.admin.deleteOutlet,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'outlets'] })
      setDeletingId(null)
    },
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => queryClient.invalidateQueries({ queryKey: ['admin', 'outlets'] })} />

  const orgName = (orgId: number) => orgs?.find(o => o.id === orgId)?.name ?? `#${orgId}`

  const startEdit = (outlet: Outlet) => {
    setEditingId(outlet.id)
    setEditName(outlet.name)
    setEditChannel(outlet.channel)
    setEditContactInfo(outlet.contactInfo ?? '')
  }

  const startCreate = () => {
    setCreating(true)
    if (!newOrgId && orgs && orgs.length > 0) setNewOrgId(orgs[0].id)
  }

  return (
    <div>
      <div className="flex items-start justify-between gap-3 mb-5 sm:mb-6">
        <div className="min-w-0">
          <h2 className="text-xl sm:text-2xl font-semibold text-[#37352F]">{t('outlets.title')}</h2>
          <p className="text-sm text-[#787774] mt-1">{t('outlets.count', { count: outlets?.length || 0 })}</p>
        </div>
        {!creating && (
          <button
            onClick={startCreate}
            className="shrink-0 px-3 py-2 sm:py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] transition-colors text-sm font-medium"
          >
            {t('common.new')}
          </button>
        )}
      </div>

      {creating && (
        <div className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5 mb-6 bg-[#FBFBFA]">
          <h3 className="text-sm font-semibold text-[#37352F] mb-4">{t('outlets.newOutlet')}</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('outlets.organization')}</label>
              <select
                value={newOrgId ?? ''}
                onChange={e => setNewOrgId(Number(e.target.value))}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
              >
                {orgs?.map(o => <option key={o.id} value={o.id}>{o.name}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('outlets.channel')}</label>
              <select
                value={newChannel}
                onChange={e => setNewChannel(e.target.value as OutletChannel)}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
              >
                {CHANNELS.map(c => <option key={c} value={c}>{t(`outlets.channels.${c}`)}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('outlets.name')}</label>
              <input
                type="text"
                value={newName}
                onChange={e => setNewName(e.target.value)}
                placeholder={t('outlets.namePlaceholder')}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
              />
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('outlets.contactInfo')}</label>
              <input
                type="text"
                value={newContactInfo}
                onChange={e => setNewContactInfo(e.target.value)}
                placeholder={t('outlets.contactPlaceholder')}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
              />
            </div>
          </div>
          <div className="flex gap-2 mt-4">
            <button
              onClick={() => newOrgId && createMutation.mutate({
                orgId: newOrgId,
                name: newName,
                channel: newChannel,
                contactInfo: newContactInfo || null,
              })}
              disabled={!newOrgId || !newName.trim() || createMutation.isPending}
              className="px-3 py-2 sm:py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors text-sm font-medium"
            >
              {createMutation.isPending ? t('outlets.creating') : t('outlets.create')}
            </button>
            <button
              onClick={() => { setCreating(false); setNewName(''); setNewContactInfo('') }}
              className="px-3 py-2 sm:py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
            >
              {t('common.cancel')}
            </button>
          </div>
        </div>
      )}

      {/* Mobile card list */}
      <div className="md:hidden space-y-2">
        {outlets?.map((o: Outlet) => (
          <div key={o.id} className="border border-[#E9E9E7] rounded-lg p-3 bg-white">
            {editingId === o.id ? (
              <div className="space-y-2">
                <div>
                  <label className="block text-xs font-medium text-[#787774] mb-1">{t('outlets.name')}</label>
                  <input
                    type="text"
                    value={editName}
                    onChange={e => setEditName(e.target.value)}
                    className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md text-sm bg-[#FBFBFA] focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-[#787774] mb-1">{t('outlets.channel')}</label>
                  <select
                    value={editChannel}
                    onChange={e => setEditChannel(e.target.value as OutletChannel)}
                    className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md text-sm bg-[#FBFBFA] focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none"
                  >
                    {CHANNELS.map(c => <option key={c} value={c}>{t(`outlets.channels.${c}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-xs font-medium text-[#787774] mb-1">{t('outlets.contactInfo')}</label>
                  <input
                    type="text"
                    value={editContactInfo}
                    onChange={e => setEditContactInfo(e.target.value)}
                    className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md text-sm bg-[#FBFBFA] focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none"
                  />
                </div>
                <div className="flex justify-end gap-3 pt-1">
                  <button onClick={() => setEditingId(null)} className="text-[#787774] text-sm px-2 py-1">{t('common.cancel')}</button>
                  <button
                    onClick={() => updateMutation.mutate({ id: o.id, name: editName, channel: editChannel, contactInfo: editContactInfo || null })}
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
                  <span className="text-sm font-medium text-[#37352F] truncate">{o.name}</span>
                  <span className="text-xs text-[#A5A29C] shrink-0">{t(`outlets.channels.${o.channel}`)}</span>
                </div>
                <div className="text-xs text-[#787774] mt-1">{orgName(o.orgId)}{o.contactInfo && ` · ${o.contactInfo}`}</div>
                <div className="mt-2 pt-2 border-t border-[#E9E9E7] flex justify-end">
                  {deletingId === o.id ? (
                    <div className="flex items-center gap-3">
                      <button
                        onClick={() => deleteMutation.mutate(o.id)}
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
                        onClick={() => startEdit(o)}
                        className="text-[#787774] text-sm hover:text-[#37352F] px-2 py-1 transition-colors"
                      >
                        {t('common.edit')}
                      </button>
                      <button
                        onClick={() => setDeletingId(o.id)}
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
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('outlets.name')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('outlets.channel')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('outlets.organization')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('outlets.contactInfo')}</th>
              <th className="text-right px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('outlets.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {outlets?.map((o: Outlet) => (
              <tr key={o.id} className="border-b border-[#E9E9E7] last:border-0 hover:bg-[#FBFBFA] transition-colors">
                <td className="px-4 py-2.5">
                  {editingId === o.id ? (
                    <input
                      type="text"
                      value={editName}
                      onChange={e => setEditName(e.target.value)}
                      className="px-2 py-1 border border-[#E9E9E7] rounded-md text-sm"
                    />
                  ) : (
                    <span className="text-sm font-medium text-[#37352F]">{o.name}</span>
                  )}
                </td>
                <td className="px-4 py-2.5">
                  {editingId === o.id ? (
                    <select
                      value={editChannel}
                      onChange={e => setEditChannel(e.target.value as OutletChannel)}
                      className="px-2 py-1 border border-[#E9E9E7] rounded-md text-sm"
                    >
                      {CHANNELS.map(c => <option key={c} value={c}>{t(`outlets.channels.${c}`)}</option>)}
                    </select>
                  ) : (
                    <span className="text-sm text-[#787774]">{t(`outlets.channels.${o.channel}`)}</span>
                  )}
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774]">{orgName(o.orgId)}</td>
                <td className="px-4 py-2.5">
                  {editingId === o.id ? (
                    <input
                      type="text"
                      value={editContactInfo}
                      onChange={e => setEditContactInfo(e.target.value)}
                      className="px-2 py-1 border border-[#E9E9E7] rounded-md text-sm"
                    />
                  ) : (
                    <span className="text-sm text-[#787774]">{o.contactInfo || '—'}</span>
                  )}
                </td>
                <td className="px-4 py-2.5 text-right">
                  {editingId === o.id ? (
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => updateMutation.mutate({ id: o.id, name: editName, channel: editChannel, contactInfo: editContactInfo || null })}
                        disabled={updateMutation.isPending}
                        className="text-[#2EAADC] text-sm font-medium hover:underline"
                      >
                        {t('common.save')}
                      </button>
                      <button onClick={() => setEditingId(null)} className="text-[#787774] text-sm hover:underline">
                        {t('common.cancel')}
                      </button>
                    </div>
                  ) : deletingId === o.id ? (
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => deleteMutation.mutate(o.id)}
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
                        onClick={() => startEdit(o)}
                        className="text-[#787774] text-sm hover:text-[#37352F] transition-colors"
                      >
                        {t('common.edit')}
                      </button>
                      <button onClick={() => setDeletingId(o.id)} className="text-[#787774] text-sm hover:text-[#E03E3E] transition-colors">
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
