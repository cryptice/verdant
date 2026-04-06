import { useState } from 'react'
import { apiRequest } from '../api/client'
import { useTranslation } from 'react-i18next'

export default function ResetData() {
  const [wiping, setWiping] = useState(false)
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [confirmWipe, setConfirmWipe] = useState(false)
  const { t } = useTranslation()

  const handleWipe = async () => {
    setWiping(true)
    setError(null)
    setResult(null)
    setConfirmWipe(false)
    try {
      const data = await apiRequest<{ message: string }>('/api/dev/wipe', { method: 'POST' })
      setResult(data.message)
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : t('resetData.wipeFailed'))
    } finally {
      setWiping(false)
    }
  }

  return (
    <div className="max-w-2xl">
      <h1 className="text-2xl font-semibold text-[#37352F] mb-1">{t('resetData.title')}</h1>
      <p className="text-sm text-[#787774] mb-8">{t('resetData.subtitle')}</p>

      <div className="border border-[#E9E9E7] rounded-lg p-6">
        <h2 className="text-base font-semibold text-[#37352F] mb-2">{t('resetData.wipeTitle')}</h2>
        <p className="text-sm text-[#787774] mb-4">
          {t('resetData.wipeDescription')}
        </p>

        <div className="bg-[#FBE4E4] border border-[#F5C6C6] rounded-md p-3 mb-4">
          <p className="text-sm text-[#E03E3E] font-medium">
            {t('resetData.wipeWarning')}
          </p>
        </div>

        {!confirmWipe ? (
          <button
            onClick={() => setConfirmWipe(true)}
            disabled={wiping}
            className="px-4 py-2 bg-[#E03E3E] text-white rounded-md text-sm font-medium hover:bg-[#C73535] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {t('resetData.wipeButton')}
          </button>
        ) : (
          <div className="flex items-center gap-3">
            <button
              onClick={handleWipe}
              disabled={wiping}
              className="px-4 py-2 bg-[#E03E3E] text-white rounded-md text-sm font-medium hover:bg-[#C73535] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {wiping ? t('resetData.wiping') : t('resetData.confirmWipe')}
            </button>
            <button
              onClick={() => setConfirmWipe(false)}
              className="px-4 py-2 text-[#787774] text-sm hover:text-[#37352F] transition-colors"
            >
              {t('common.cancel')}
            </button>
          </div>
        )}

        {error && (
          <div className="mt-4 bg-[#FBE4E4] border border-[#F5C6C6] rounded-md p-3">
            <p className="text-sm text-[#E03E3E]">{error}</p>
          </div>
        )}

        {result && (
          <div className="mt-4 bg-[#DBEDDB] border border-[#C4DFC4] rounded-md p-3">
            <p className="text-sm text-[#0F7B0F]">{result}</p>
          </div>
        )}
      </div>
    </div>
  )
}
