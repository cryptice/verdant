import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'

type Props = {
  value: string | null
  onChange: (base64: string | null) => void
  /** Max edge in pixels for the downscaled image (default 1280). */
  maxEdge?: number
  /** JPEG quality 0–1 (default 0.85). */
  quality?: number
}

/**
 * File picker that downscales the chosen image to keep the base64 payload
 * reasonable, then surfaces it as a `data:image/jpeg;base64,…` string.
 * Stores the encoded value in `value`; parent can use it directly as the
 * `imageBase64` field on plant-event requests.
 */
export function PhotoPicker({ value, onChange, maxEdge = 1280, quality = 0.85 }: Props) {
  const { t } = useTranslation()
  const inputRef = useRef<HTMLInputElement>(null)
  const [processing, setProcessing] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleFile = async (file: File) => {
    setError(null)
    setProcessing(true)
    try {
      const dataUrl = await loadAsDataUrl(file)
      const img = await loadImage(dataUrl)
      const resized = downscaleToJpeg(img, maxEdge, quality)
      onChange(resized)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load image')
    } finally {
      setProcessing(false)
    }
  }

  return (
    <div>
      <label className="field-label">{t('photo.label')}</label>
      <div className="flex items-center gap-3 mt-1">
        {value ? (
          <img
            src={value}
            alt=""
            style={{
              width: 96, height: 96, objectFit: 'cover',
              border: '1px solid var(--color-ink)', borderRadius: 8,
            }}
          />
        ) : (
          <div
            style={{
              width: 96, height: 96,
              border: '1px dashed color-mix(in srgb, var(--color-ink) 35%, transparent)',
              borderRadius: 8,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4,
              textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6,
            }}
          >
            {t('photo.none')}
          </div>
        )}
        <div className="flex flex-col gap-1">
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            disabled={processing}
            className="btn-secondary text-sm"
          >
            {processing ? t('photo.processing') : value ? t('photo.replace') : t('photo.choose')}
          </button>
          {value && (
            <button
              type="button"
              onClick={() => onChange(null)}
              className="text-xs text-text-secondary hover:underline"
            >
              {t('common.clear')}
            </button>
          )}
        </div>
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          capture="environment"
          onChange={(e) => {
            const f = e.target.files?.[0]
            if (f) handleFile(f)
            e.target.value = ''
          }}
          style={{ display: 'none' }}
        />
      </div>
      {error && <p className="text-error text-sm mt-2">{error}</p>}
    </div>
  )
}

function loadAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result as string)
    reader.onerror = () => reject(new Error('FileReader failed'))
    reader.readAsDataURL(file)
  })
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('Image decode failed'))
    img.src = src
  })
}

function downscaleToJpeg(img: HTMLImageElement, maxEdge: number, quality: number): string {
  const ratio = Math.min(1, maxEdge / Math.max(img.naturalWidth, img.naturalHeight))
  const w = Math.round(img.naturalWidth * ratio)
  const h = Math.round(img.naturalHeight * ratio)
  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('Canvas 2D context unavailable')
  ctx.drawImage(img, 0, 0, w, h)
  return canvas.toDataURL('image/jpeg', quality)
}
