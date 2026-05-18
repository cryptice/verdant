import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fileToBase64 } from '../../lib/fileToBase64'

export function ImageUpload({ label, currentUrl, onUpload, onClear }: {
  label: string
  currentUrl: string | null
  onUpload: (base64: string) => void
  onClear?: () => void
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const { t } = useTranslation()

  const handleFile = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setPreview(URL.createObjectURL(file))
    const b64 = await fileToBase64(file)
    onUpload(b64)
  }, [onUpload])

  const displayUrl = preview || currentUrl

  return (
    <div>
      <label className="block text-xs font-medium text-[#787774] mb-1.5">{label}</label>
      <div className="border border-dashed border-[#D3D1CB] rounded-md p-3 text-center hover:border-[#2EAADC] transition-colors">
        {displayUrl ? (
          <div className="relative">
            <img src={displayUrl} alt={label} className="max-h-[600px] mx-auto rounded-md object-contain" />
            <div className="mt-2 flex gap-2 justify-center">
              <button
                type="button"
                onClick={() => inputRef.current?.click()}
                className="text-sm text-[#2EAADC] hover:underline"
              >
                {t('common.replace')}
              </button>
              {onClear && (
                <button
                  type="button"
                  onClick={() => { setPreview(null); onClear() }}
                  className="text-sm text-[#E03E3E] hover:underline"
                >
                  {t('common.remove')}
                </button>
              )}
            </div>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            className="py-6 w-full text-sm text-[#A5A29C] hover:text-[#2EAADC]"
          >
            {t('common.clickToUpload')}
          </button>
        )}
        <input ref={inputRef} type="file" accept="image/*" className="hidden" onChange={handleFile} />
      </div>
    </div>
  )
}
