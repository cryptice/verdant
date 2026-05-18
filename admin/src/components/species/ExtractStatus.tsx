import { useTranslation } from 'react-i18next'

export function ExtractStatus({ extractingFront, extractingBack, extractedFront, extractedBack, extractError }: {
  extractingFront: boolean
  extractingBack: boolean
  extractedFront: boolean
  extractedBack: boolean
  extractError: string | null
}) {
  const { t } = useTranslation()

  if (!extractingFront && !extractingBack && !extractedFront && !extractedBack && !extractError) return null

  return (
    <div className="mt-3 flex flex-col gap-1 text-sm">
      {extractingFront && <span className="text-[#787774]">{t('species.extractingFront')}</span>}
      {!extractingFront && extractedFront && <span className="text-[#0F7B0F]">{t('species.frontDataExtracted')}</span>}
      {extractingBack && <span className="text-[#787774]">{t('species.extractingBack')}</span>}
      {!extractingBack && extractedBack && <span className="text-[#0F7B0F]">{t('species.backDataExtracted')}</span>}
      {extractError && <span className="text-[#E03E3E]">{extractError}</span>}
    </div>
  )
}
