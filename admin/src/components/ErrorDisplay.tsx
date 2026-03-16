import { ApiError } from '../api/client'

type ErrorKind = 'network' | 'client' | 'server'

function classifyError(error: Error): ErrorKind {
  if (error instanceof ApiError) {
    if (error.isNetworkError) return 'network'
    if (error.status && error.status >= 400 && error.status < 500) return 'client'
  }
  return 'server'
}

const styles: Record<ErrorKind, { bg: string; iconBg: string; icon: string; heading: string; text: string; button: string }> = {
  network: {
    bg: 'bg-orange-50 border border-orange-200',
    iconBg: 'bg-orange-100',
    icon: 'text-orange-600',
    heading: 'text-orange-800',
    text: 'text-orange-700',
    button: 'bg-orange-600 text-white hover:bg-orange-700',
  },
  client: {
    bg: 'bg-yellow-50 border border-yellow-200',
    iconBg: 'bg-yellow-100',
    icon: 'text-yellow-600',
    heading: 'text-yellow-800',
    text: 'text-yellow-700',
    button: 'bg-yellow-600 text-white hover:bg-yellow-700',
  },
  server: {
    bg: 'bg-red-50 border border-red-200',
    iconBg: 'bg-red-100',
    icon: 'text-red-600',
    heading: 'text-red-800',
    text: 'text-red-700',
    button: 'bg-red-600 text-white hover:bg-red-700',
  },
}

const titles: Record<ErrorKind, string> = {
  network: 'Connection Error',
  client: 'Invalid Request',
  server: 'Something went wrong',
}

export default function ErrorDisplay({ error, onRetry }: { error: Error | null; onRetry?: () => void }) {
  if (!error) return null

  const kind = classifyError(error)
  const s = styles[kind]
  const message = kind === 'server' ? 'An unexpected error occurred. Please try again later.' : error.message

  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      <div className={`rounded-xl p-8 max-w-md w-full text-center ${s.bg}`}>
        <div className={`w-12 h-12 mx-auto mb-4 rounded-full flex items-center justify-center ${s.iconBg}`}>
          {kind === 'network' ? (
            <svg className={`w-6 h-6 ${s.icon}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 5.636a9 9 0 11-12.728 0M12 9v4m0 4h.01" />
            </svg>
          ) : (
            <svg className={`w-6 h-6 ${s.icon}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          )}
        </div>
        <h3 className={`text-lg font-semibold mb-2 ${s.heading}`}>
          {titles[kind]}
        </h3>
        <p className={`text-sm mb-4 ${s.text}`}>
          {message}
        </p>
        {onRetry && (
          <button
            onClick={onRetry}
            className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${s.button}`}
          >
            Try again
          </button>
        )}
      </div>
    </div>
  )
}
