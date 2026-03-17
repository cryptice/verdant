import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { PageHeader } from '../components/PageHeader'
import { Dialog } from '../components/Dialog'

export function Account() {
  const { user, logout } = useAuth()
  const [showDelete, setShowDelete] = useState(false)

  const deleteMut = useMutation({
    mutationFn: () => api.user.delete(),
    onSuccess: logout,
  })

  if (!user) return null

  return (
    <div>
      <PageHeader title="Account" />
      <div className="px-4 py-4 space-y-6">
        <div className="card flex items-center gap-4">
          {user.avatarUrl ? (
            <img src={user.avatarUrl} alt="" className="w-14 h-14 rounded-full" />
          ) : (
            <div className="w-14 h-14 rounded-full bg-green-primary/20 flex items-center justify-center text-xl font-bold text-green-primary">
              {user.displayName.charAt(0)}
            </div>
          )}
          <div>
            <p className="font-semibold">{user.displayName}</p>
            <p className="text-sm text-text-secondary">{user.email}</p>
          </div>
        </div>

        <button onClick={logout} className="w-full border border-cream-dark rounded-xl px-4 py-3 text-sm font-medium text-left">
          Sign out
        </button>

        <button onClick={() => setShowDelete(true)} className="w-full border border-error/30 rounded-xl px-4 py-3 text-sm font-medium text-error text-left">
          Delete account
        </button>
      </div>

      <Dialog open={showDelete} onClose={() => setShowDelete(false)} title="Delete Account" actions={
        <>
          <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">Cancel</button>
          <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">
            {deleteMut.isPending ? 'Deleting...' : 'Delete'}
          </button>
        </>
      }>
        <p className="text-text-secondary">This will permanently delete your account and all your data. This action cannot be undone.</p>
      </Dialog>
    </div>
  )
}
