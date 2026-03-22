import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'

export function Account() {
  const { user, logout } = useAuth()
  const { t } = useTranslation()

  return (
    <div>
      <h1 className="text-xl font-semibold text-text-primary mb-5">{t('account.title')}</h1>

      <div className="card max-w-sm space-y-4">
        <div>
          <p className="text-sm font-medium text-text-primary">{user?.displayName}</p>
          <p className="text-xs text-text-secondary mt-0.5">{user?.email}</p>
        </div>
        <button onClick={logout} className="btn-secondary text-sm w-full">
          {t('account.signOut')}
        </button>
      </div>
    </div>
  )
}
