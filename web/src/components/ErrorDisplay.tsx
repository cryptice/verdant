import { useTranslation } from 'react-i18next'
import { ApiError } from '../api/client'

export function ErrorDisplay({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const { t } = useTranslation()
  const isNetwork = error instanceof ApiError && error.isNetworkError
  return (
    <div className="flex flex-col items-center justify-center p-8 gap-4">
      <p className="text-5xl">{isNetwork ? '📡' : '⚠️'}</p>
      <p className="text-text-secondary text-center">
        {error instanceof Error ? error.message : t('error.generic')}
      </p>
      {onRetry && (
        <button onClick={onRetry} className="btn-primary text-sm">{t('error.retry')}</button>
      )}
    </div>
  )
}
