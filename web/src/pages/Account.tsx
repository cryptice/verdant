import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, downloadDataExport } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../components/PageHeader'
import { Dialog } from '../components/Dialog'

export function Account() {
  const { user, logout } = useAuth()
  const { t } = useTranslation()
  const [showDelete, setShowDelete] = useState(false)

  const deleteMut = useMutation({
    mutationFn: () => api.user.delete(),
    onSuccess: logout,
  })

  const exportMut = useMutation({
    mutationFn: () => downloadDataExport(),
  })

  if (!user) return null

  return (
    <div>
      <PageHeader title={t('account.title')} />
      <div className="px-4 py-4 space-y-6">
        <div className="card flex items-center gap-4">
          {user.avatarUrl ? (
            <img src={user.avatarUrl} alt={user.displayName} className="w-14 h-14 rounded-full" />
          ) : (
            <div className="w-14 h-14 rounded-full bg-accent/15 flex items-center justify-center text-xl font-bold text-accent">
              {user.displayName.charAt(0)}
            </div>
          )}
          <div>
            <p className="font-semibold">{user.displayName}</p>
            <p className="text-sm text-text-secondary">{user.email}</p>
          </div>
        </div>

        <button onClick={logout} className="btn-secondary w-full">
          {t('account.signOut')}
        </button>

        <Link to="/privacy" className="block px-3 py-2 text-sm text-text-secondary hover:text-text-primary hover:bg-surface rounded-md transition-colors">
          {t('privacy.title')}
        </Link>

        <button
          onClick={() => exportMut.mutate()}
          disabled={exportMut.isPending}
          className="w-full text-left px-3 py-2 text-sm text-text-secondary hover:bg-surface rounded-md transition-colors disabled:opacity-50"
        >
          {exportMut.isPending ? t('account.exportingData') : t('account.downloadMyData')}
        </button>

        <button onClick={() => setShowDelete(true)} className="w-full text-left px-3 py-2 text-sm text-error hover:bg-error/5 rounded-md transition-colors">
          {t('account.deleteAccount')}
        </button>
      </div>

      <Dialog open={showDelete} onClose={() => setShowDelete(false)} title={t('account.deleteAccountTitle')} actions={
        <>
          <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">
            {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
          </button>
        </>
      }>
        <p className="text-text-secondary">{t('account.deleteAccountConfirm')}</p>
      </Dialog>
    </div>
  )
}
