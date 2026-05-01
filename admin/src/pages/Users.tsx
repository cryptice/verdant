import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type User } from '../api/client'
import { useState } from 'react'
import ErrorDisplay from '../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'

export default function Users() {
  const queryClient = useQueryClient()
  const [deletingId, setDeletingId] = useState<number | null>(null)
  const { t } = useTranslation()

  const { data: users, isLoading, error } = useQuery({
    queryKey: ['admin', 'users'],
    queryFn: api.admin.getUsers
  })

  const deleteMutation = useMutation({
    mutationFn: api.admin.deleteUser,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })
      setDeletingId(null)
    }
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] })} />

  return (
    <div>
      <div className="mb-5 sm:mb-6">
        <h2 className="text-xl sm:text-2xl font-semibold text-[#37352F]">{t('users.title')}</h2>
        <p className="text-sm text-[#787774] mt-1">{t('users.registeredUsers', { count: users?.length || 0 })}</p>
      </div>

      {/* Mobile card list */}
      <div className="md:hidden space-y-2">
        {users?.map((user: User) => (
          <div key={user.id} className="border border-[#E9E9E7] rounded-lg p-3 bg-white">
            <div className="flex items-start gap-3">
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt="" className="w-9 h-9 rounded-full shrink-0" />
              ) : (
                <div className="w-9 h-9 rounded-full bg-[#E9E9E7] flex items-center justify-center text-[#787774] text-sm font-medium shrink-0">
                  {user.displayName.charAt(0)}
                </div>
              )}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="text-sm font-medium text-[#37352F] truncate">{user.displayName}</span>
                  <span className={`px-2 py-0.5 rounded text-[10px] font-medium ${
                    user.role === 'ADMIN' ? 'bg-[#F0E5FF] text-[#6940A5]' : 'bg-[#E9E9E7] text-[#787774]'
                  }`}>
                    {user.role}
                  </span>
                </div>
                <p className="text-sm text-[#787774] truncate mt-0.5">{user.email}</p>
                <p className="text-xs text-[#A5A29C] mt-0.5">{t('users.joined')}: {new Date(user.createdAt).toLocaleDateString()}</p>
              </div>
            </div>
            {user.role !== 'ADMIN' && (
              <div className="mt-3 pt-3 border-t border-[#E9E9E7] flex justify-end">
                {deletingId === user.id ? (
                  <div className="flex items-center gap-3">
                    <button
                      onClick={() => deleteMutation.mutate(user.id)}
                      className="text-[#E03E3E] text-sm font-medium px-2 py-1"
                      disabled={deleteMutation.isPending}
                    >
                      {t('common.confirm')}
                    </button>
                    <button
                      onClick={() => setDeletingId(null)}
                      className="text-[#787774] text-sm px-2 py-1"
                    >
                      {t('common.cancel')}
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => setDeletingId(user.id)}
                    className="text-[#787774] text-sm px-2 py-1 hover:text-[#E03E3E] transition-colors"
                  >
                    {t('common.delete')}
                  </button>
                )}
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Desktop table */}
      <div className="hidden md:block border border-[#E9E9E7] rounded-lg overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#E9E9E7] bg-[#FBFBFA]">
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('users.name')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('users.email')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('users.role')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('users.joined')}</th>
              <th className="text-right px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('users.actions')}</th>
            </tr>
          </thead>
          <tbody>
            {users?.map((user: User) => (
              <tr key={user.id} className="border-b border-[#E9E9E7] last:border-0 hover:bg-[#FBFBFA] transition-colors">
                <td className="px-4 py-2.5">
                  <div className="flex items-center gap-2.5">
                    {user.avatarUrl ? (
                      <img src={user.avatarUrl} alt="" className="w-6 h-6 rounded-full" />
                    ) : (
                      <div className="w-6 h-6 rounded-full bg-[#E9E9E7] flex items-center justify-center text-[#787774] text-xs font-medium">
                        {user.displayName.charAt(0)}
                      </div>
                    )}
                    <span className="text-sm font-medium text-[#37352F]">{user.displayName}</span>
                  </div>
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774]">{user.email}</td>
                <td className="px-4 py-2.5">
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                    user.role === 'ADMIN'
                      ? 'bg-[#F0E5FF] text-[#6940A5]'
                      : 'bg-[#E9E9E7] text-[#787774]'
                  }`}>
                    {user.role}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774]">
                  {new Date(user.createdAt).toLocaleDateString()}
                </td>
                <td className="px-4 py-2.5 text-right">
                  {user.role === 'ADMIN' ? null : deletingId === user.id ? (
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => deleteMutation.mutate(user.id)}
                        className="text-[#E03E3E] text-sm font-medium hover:underline"
                        disabled={deleteMutation.isPending}
                      >
                        {t('common.confirm')}
                      </button>
                      <button
                        onClick={() => setDeletingId(null)}
                        className="text-[#787774] text-sm hover:underline"
                      >
                        {t('common.cancel')}
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => setDeletingId(user.id)}
                      className="text-[#787774] text-sm hover:text-[#E03E3E] transition-colors"
                    >
                      {t('common.delete')}
                    </button>
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
