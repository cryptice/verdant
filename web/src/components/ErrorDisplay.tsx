import { ApiError } from '../api/client'

export function ErrorDisplay({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const isNetwork = error instanceof ApiError && error.isNetworkError
  return (
    <div className="flex flex-col items-center justify-center p-8 gap-4">
      <p className="text-5xl">{isNetwork ? '📡' : '⚠️'}</p>
      <p className="text-text-secondary text-center">
        {error instanceof Error ? error.message : 'Something went wrong'}
      </p>
      {onRetry && (
        <button onClick={onRetry} className="btn-primary text-sm">Try again</button>
      )}
    </div>
  )
}
