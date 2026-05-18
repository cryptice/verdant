import { useTranslation } from 'react-i18next'
import type { Provider } from '../../api/client'
import { ImageUpload } from './ImageUpload'
import { Field } from './Field'

export function ProviderEditor({
  wrapperClassName,
  selectClassName,
  providerId,
  onProviderIdChange,
  frontBase64,
  onFrontChange,
  backBase64,
  onBackChange,
  productUrl,
  onProductUrlChange,
  availableProviders,
  excludedProviderIds,
  isPending,
  errorMessage,
  onAdd,
  onCancel,
  showHeading = true,
}: {
  wrapperClassName: string
  selectClassName: string
  providerId: number | null
  onProviderIdChange: (id: number | null) => void
  frontBase64: string | null
  onFrontChange: (b64: string) => void
  backBase64: string | null
  onBackChange: (b64: string) => void
  productUrl: string
  onProductUrlChange: (v: string) => void
  availableProviders: Provider[] | undefined
  excludedProviderIds: number[]
  isPending: boolean
  errorMessage?: string | null
  onAdd: () => void
  onCancel: () => void
  showHeading?: boolean
}) {
  const { t } = useTranslation()

  return (
    <div className={wrapperClassName}>
      {showHeading && (
        <h4 className="text-xs font-semibold text-[#37352F] uppercase tracking-wider mb-3">{t('species.addProvider')}</h4>
      )}
      <div className="mb-3">
        <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('species.provider')}</label>
        <select
          value={providerId ?? ''}
          onChange={e => onProviderIdChange(e.target.value ? Number(e.target.value) : null)}
          className={selectClassName}
        >
          <option value="">{t('species.selectProvider')}</option>
          {availableProviders
            ?.filter(ap => !excludedProviderIds.includes(ap.id))
            .map(p => (
              <option key={p.id} value={p.id}>{p.name}</option>
            ))}
        </select>
      </div>
      {providerId && (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 mb-3">
            <ImageUpload
              label={t('species.front')}
              currentUrl={frontBase64 ? `data:image/jpeg;base64,${frontBase64}` : null}
              onUpload={onFrontChange}
            />
            <ImageUpload
              label={t('species.back')}
              currentUrl={backBase64 ? `data:image/jpeg;base64,${backBase64}` : null}
              onUpload={onBackChange}
            />
          </div>
          <div className="mb-3">
            <Field label={t('species.productUrl')} value={productUrl} onChange={onProductUrlChange} />
          </div>
        </>
      )}
      <div className="flex gap-2">
        <button
          type="button"
          disabled={!providerId || isPending}
          onClick={onAdd}
          className="px-3 py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors text-sm font-medium"
        >
          {isPending ? t('species.adding') : t('common.add')}
        </button>
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
        >
          {t('common.cancel')}
        </button>
      </div>
      {errorMessage && (
        <p className="text-sm text-[#E03E3E] mt-2">{errorMessage}</p>
      )}
    </div>
  )
}
