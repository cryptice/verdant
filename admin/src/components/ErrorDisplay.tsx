import { ApiError } from '../api/client'
import { useTranslation } from 'react-i18next'

type ErrorKind = 'network' | 'client' | 'server'

function classifyError(error: Error): ErrorKind {
  if (error instanceof ApiError) {
    if (error.isNetworkError) return 'network'
    if (error.status && error.status >= 400 && error.status < 500) return 'client'
  }
  return 'server'
}

const styles: Record<ErrorKind, { bg: string; text: string; heading: string; button: string }> = {
  network: {
    bg: 'bg-[#FBF3DB] border border-[#F1E5BC]',
    heading: 'text-[#73641C]',
    text: 'text-[#73641C]',
    button: 'bg-[#73641C] text-white hover:bg-[#5C4F16]',
  },
  client: {
    bg: 'bg-[#FADEC9] border border-[#F5C6A1]',
    heading: 'text-[#93592F]',
    text: 'text-[#93592F]',
    button: 'bg-[#93592F] text-white hover:bg-[#7D4B28]',
  },
  server: {
    bg: 'bg-[#FBE4E4] border border-[#F5C6C6]',
    heading: 'text-[#E03E3E]',
    text: 'text-[#E03E3E]',
    button: 'bg-[#E03E3E] text-white hover:bg-[#C73535]',
  },
}

export default function ErrorDisplay({ error, onRetry }: { error: Error | null; onRetry?: () => void }) {
  const { t } = useTranslation()

  if (!error) return null

  const kind = classifyError(error)
  const s = styles[kind]

  const titles: Record<ErrorKind, string> = {
    network: t('errors.connectionError'),
    client: t('errors.invalidRequest'),
    server: t('errors.somethingWentWrong'),
  }

  const message = kind === 'server' ? t('errors.unexpectedError') : error.message

  return (
    <div className="flex flex-col items-center justify-center py-16 px-4">
      <div className={`rounded-lg p-6 max-w-md w-full text-center ${s.bg}`}>
        <h3 className={`text-base font-semibold mb-2 ${s.heading}`}>
          {titles[kind]}
        </h3>
        <p className={`text-sm mb-4 ${s.text}`}>
          {message}
        </p>
        {onRetry && (
          <button
            onClick={onRetry}
            className={`px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${s.button}`}
          >
            {t('errors.tryAgain')}
          </button>
        )}
      </div>
    </div>
  )
}
