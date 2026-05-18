import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type AddSpeciesProviderRequest } from '../../api/client'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import ErrorDisplay from '../../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'
import { InfoField } from '../../components/species/InfoField'
import { ProviderEditor } from '../../components/species/ProviderEditor'
import { usePositionLabel, useSoilLabel } from './shared'

const MONTH_LABELS_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function SpeciesDetailPage() {
  const { id } = useParams()
  const speciesId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [deleteStep, setDeleteStep] = useState<0 | 1 | 2>(0)
  const [addingProvider, setAddingProvider] = useState(false)
  const [newProviderId, setNewProviderId] = useState<number | null>(null)
  const [newFrontBase64, setNewFrontBase64] = useState<string | null>(null)
  const [newBackBase64, setNewBackBase64] = useState<string | null>(null)
  const [newProductUrl, setNewProductUrl] = useState('')
  const { t } = useTranslation()
  const positionLabel = usePositionLabel()
  const soilLabel = useSoilLabel()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [speciesId])

  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key !== 'c' || e.metaKey || e.ctrlKey || e.altKey) return
      const target = e.target as HTMLElement | null
      if (target && (target.isContentEditable || ['INPUT', 'TEXTAREA', 'SELECT'].includes(target.tagName))) return
      e.preventDefault()
      navigate(`/species/new?from=${speciesId}`)
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [navigate, speciesId])

  const { data: species, isLoading, error } = useQuery({
    queryKey: ['admin', 'species', speciesId],
    queryFn: () => api.admin.getSpeciesById(speciesId)
  })

  const { data: availableProviders } = useQuery({
    queryKey: ['admin', 'providers'],
    queryFn: api.admin.getProviders,
    enabled: addingProvider,
  })

  const deleteMutation = useMutation({
    mutationFn: () => api.admin.deleteSpecies(speciesId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      navigate('/species')
    }
  })

  const addProviderMutation = useMutation({
    mutationFn: (req: AddSpeciesProviderRequest) => api.admin.addSpeciesProvider(speciesId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species', speciesId] })
      resetAddProvider()
    }
  })

  const resetAddProvider = () => {
    setAddingProvider(false)
    setNewProviderId(null)
    setNewFrontBase64(null)
    setNewBackBase64(null)
    setNewProductUrl('')
  }

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  if (error || !species) return <ErrorDisplay error={error ?? new Error(t('errors.speciesNotFound'))} onRetry={() => navigate(0)} />

  return (
    <div>
      <button onClick={() => navigate('/species')} className="-ml-2 inline-flex items-center gap-1 text-sm text-[#787774] hover:text-[#37352F] hover:bg-[#F0F0EE] rounded-md px-2 py-1.5 mb-3 sm:mb-4 transition-colors">
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        {t('species.backToList')}
      </button>

      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-3 mb-5 sm:mb-6">
        <div className="min-w-0">
          <h2 className="text-xl sm:text-2xl font-semibold text-[#37352F] break-words">
            {species.commonNameSv || species.commonName}
            {(species.variantNameSv || species.variantName) && <span className="text-[#787774] font-normal"> — {species.variantNameSv || species.variantName}</span>}
            {' '}<span className={`inline-block align-middle px-2 py-0.5 rounded text-xs font-medium ${species.isSystem ? 'bg-[#D3E5EF] text-[#2B6CB0]' : 'bg-[#E9E9E7] text-[#787774]'}`}>
              {species.isSystem ? t('species.system') : t('species.user')}
            </span>
          </h2>
          {species.commonNameSv && species.commonName !== species.commonNameSv && (
            <p className="text-sm text-[#787774]">
              {species.commonName}
              {species.variantName && <span> — {species.variantName}</span>}
            </p>
          )}
          {species.scientificName && <p className="text-sm text-[#A5A29C] italic break-words">{species.scientificName}</p>}
        </div>
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <button
            onClick={() => navigate(`/species/new?from=${speciesId}`)}
            className="px-3 py-2 sm:py-1.5 border border-[#E9E9E7] text-[#37352F] rounded-md hover:bg-[#F0F0EE] transition-colors text-sm inline-flex items-center gap-1.5"
          >
            <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
            {t('species.copy')}
          </button>
          <button
            onClick={() => navigate(`/species/${speciesId}/edit`)}
            className="ml-auto px-3 py-2 sm:py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] transition-colors text-sm font-medium"
          >
            {t('common.edit')}
          </button>
        </div>
      </div>

      <div className="space-y-4 sm:space-y-6">
        {/* Providers & Images */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider">
              {t('species.seedProviders')}
              {species.providers.length > 0 && <span className="ml-1 text-xs font-normal text-[#787774] normal-case">({species.providers.length})</span>}
            </h3>
            {!addingProvider && (
              <button
                onClick={() => setAddingProvider(true)}
                className="text-sm text-[#2EAADC] hover:underline cursor-pointer"
              >
                {t('species.addProvider')}
              </button>
            )}
          </div>

          {species.providers.length > 0 ? (
            <div className="space-y-6">
              {species.providers.map(sp => (
                <div key={sp.id}>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-sm font-medium text-[#37352F]">{sp.providerName}</span>
                    <span className="text-xs text-[#787774]">{sp.providerIdentifier}</span>
                    {sp.productUrl && (
                      <a href={sp.productUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-[#2EAADC] hover:underline ml-auto">
                        {t('species.productPage')} &rarr;
                      </a>
                    )}
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                    {sp.imageFrontUrl ? (
                      <img src={sp.imageFrontUrl} alt={t('species.front')} className="w-full rounded-md object-contain max-h-[60vh] sm:max-h-[500px]" />
                    ) : (
                      <div className="h-40 sm:h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">{t('species.noFrontImage')}</div>
                    )}
                    {sp.imageBackUrl ? (
                      <img src={sp.imageBackUrl} alt={t('species.back')} className="w-full rounded-md object-contain max-h-[60vh] sm:max-h-[500px]" />
                    ) : (
                      <div className="h-40 sm:h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">{t('species.noBackImage')}</div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : !addingProvider ? (
            <p className="text-sm text-[#A5A29C]">{t('species.noProvidersYet')}</p>
          ) : null}

          {addingProvider && (
            <ProviderEditor
              wrapperClassName={species.providers.length > 0 ? 'mt-6 pt-6 border-t border-[#E9E9E7]' : ''}
              selectClassName="w-full max-w-xs px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
              providerId={newProviderId}
              onProviderIdChange={setNewProviderId}
              frontBase64={newFrontBase64}
              onFrontChange={setNewFrontBase64}
              backBase64={newBackBase64}
              onBackChange={setNewBackBase64}
              productUrl={newProductUrl}
              onProductUrlChange={setNewProductUrl}
              availableProviders={availableProviders}
              excludedProviderIds={species.providers.map(sp => sp.providerId)}
              isPending={addProviderMutation.isPending}
              errorMessage={addProviderMutation.error?.message ?? null}
              onAdd={() => {
                if (!newProviderId) return
                addProviderMutation.mutate({
                  providerId: newProviderId,
                  imageFrontBase64: newFrontBase64,
                  imageBackBase64: newBackBase64,
                  productUrl: newProductUrl || undefined,
                })
              }}
              onCancel={resetAddProvider}
            />
          )}
        </section>

        {/* Growth Info, Months & Growing Conditions */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.growthInfo')}</h3>
            <div className="grid grid-cols-2 gap-3">
              <InfoField label={t('species.germinationDays')} value={species.germinationTimeDaysMin != null ? (species.germinationTimeDaysMax && species.germinationTimeDaysMax !== species.germinationTimeDaysMin ? `${species.germinationTimeDaysMin}–${species.germinationTimeDaysMax}` : species.germinationTimeDaysMin) : null} />
              <InfoField label={t('species.daysToHarvest')} value={species.daysToHarvestMin != null ? (species.daysToHarvestMax && species.daysToHarvestMax !== species.daysToHarvestMin ? `${species.daysToHarvestMin}–${species.daysToHarvestMax}` : species.daysToHarvestMin) : null} />
              <InfoField label={t('species.sowingDepthMm')} value={species.sowingDepthMm} />
              <InfoField label={t('species.heightCm')} value={species.heightCmMin != null ? (species.heightCmMax && species.heightCmMax !== species.heightCmMin ? `${species.heightCmMin}–${species.heightCmMax}` : species.heightCmMin) : null} />
              <InfoField label={t('species.germinationRate')} value={species.germinationRate} />
            </div>
          </section>

          {(species.sowingMonths.length > 0 || species.bloomMonths.length > 0) && (
            <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.sowingBloomMonths')}</h3>
              <div className="space-y-3">
                {species.sowingMonths.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">{t('species.sowing')}</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.sowingMonths.map(m => (
                        <span key={m} className="px-2 py-0.5 bg-[#DBEDDB] text-[#0F7B0F] rounded text-xs font-medium">{MONTH_LABELS_SHORT[m - 1]}</span>
                      ))}
                    </div>
                  </div>
                )}
                {species.bloomMonths.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">{t('species.bloom')}</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.bloomMonths.map(m => (
                        <span key={m} className="px-2 py-0.5 bg-[#F0E5FF] text-[#6940A5] rounded text-xs font-medium">{MONTH_LABELS_SHORT[m - 1]}</span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </section>
          )}

          {(species.growingPositions.length > 0 || species.soils.length > 0) && (
            <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.growingConditions')}</h3>
              <div className="space-y-3">
                {species.growingPositions.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">{t('species.position')}</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.growingPositions.map(p => (
                        <span key={p} className="px-2 py-0.5 bg-[#FBF3DB] text-[#73641C] rounded text-xs font-medium">{positionLabel[p] ?? p}</span>
                      ))}
                    </div>
                  </div>
                )}
                {species.soils.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">{t('species.soil')}</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.soils.map(s => (
                        <span key={s} className="px-2 py-0.5 bg-[#FADEC9] text-[#93592F] rounded text-xs font-medium">{soilLabel[s] ?? s}</span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </section>
          )}
        </div>

        {/* Group & Tags */}
        {(species.groups.length > 0 || species.tags.length > 0) && (
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.groupsAndTags')}</h3>
            {species.groups.length > 0 && (
              <div className="mb-3">
                <label className="block text-xs font-medium text-[#787774] mb-1">{t('species.groups')}</label>
                <span className="text-sm text-[#37352F]">{species.groups.map(g => g.name).join(', ')}</span>
              </div>
            )}
            {species.tags.length > 0 && (
              <div>
                <label className="block text-xs font-medium text-[#787774] mb-1">{t('species.tags')}</label>
                <div className="flex gap-1.5 flex-wrap">
                  {species.tags.map(tg => (
                    <span key={tg.id} className="px-2 py-0.5 bg-[#D3E5EF] text-[#2B6CB0] rounded text-xs font-medium">{tg.name}</span>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}

        {/* Commercial & Classification */}
        {(species.costPerSeedCents != null || species.expectedStemsPerPlant != null || species.expectedVaseLifeDays != null || species.plantType || species.defaultUnitType) && (
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.commercialClassification')}</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
              <InfoField label={t('species.costPerSeed')} value={species.costPerSeedCents != null ? `${(species.costPerSeedCents / 100).toFixed(2)} kr` : undefined} />
              <InfoField label={t('species.stemsPerPlant')} value={species.expectedStemsPerPlant} />
              <InfoField label={t('species.vaseLifeDays')} value={species.expectedVaseLifeDays} />
              <InfoField label={t('species.plantType')} value={species.plantType} />
              <InfoField label={t('species.defaultUnitType')} value={species.defaultUnitType} />
            </div>
          </section>
        )}

        {/* Additional Photos */}
        {species.photos.length > 0 && (
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.additionalPhotos')} ({species.photos.length})</h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3 sm:gap-4">
              {species.photos.map(photo => (
                <img key={photo.id} src={photo.imageUrl} alt="" className="w-full h-28 sm:h-32 object-cover rounded-md" />
              ))}
            </div>
          </section>
        )}

        {/* Delete */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5 mt-2">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-2">{t('species.dangerZone')}</h3>
          <p className="text-sm text-[#787774] mb-4">{t('species.dangerDescription')}</p>
          {deleteStep === 0 && (
            <button
              onClick={() => setDeleteStep(1)}
              className="px-3 py-1.5 border border-[#E03E3E] text-[#E03E3E] rounded-md hover:bg-[#FBE4E4] transition-colors text-sm font-medium"
            >
              {t('species.deleteSpecies')}
            </button>
          )}
          {deleteStep === 1 && (
            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
              <span className="text-sm text-[#E03E3E] w-full sm:w-auto">{t('species.areYouSure')}</span>
              <button
                onClick={() => setDeleteStep(2)}
                className="px-3 py-2 sm:py-1.5 bg-[#E03E3E] text-white rounded-md hover:bg-[#C73535] transition-colors text-sm font-medium"
              >
                {t('species.yesDelete')}
              </button>
              <button
                onClick={() => setDeleteStep(0)}
                className="px-3 py-2 sm:py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
              >
                {t('common.cancel')}
              </button>
            </div>
          )}
          {deleteStep === 2 && (
            <div className="flex flex-wrap items-center gap-2 sm:gap-3">
              <span className="text-sm text-[#E03E3E] font-medium w-full sm:w-auto">{t('species.cannotBeUndone')}</span>
              <button
                onClick={() => deleteMutation.mutate()}
                disabled={deleteMutation.isPending}
                className="px-3 py-2 sm:py-1.5 bg-[#E03E3E] text-white rounded-md hover:bg-[#C73535] disabled:opacity-50 transition-colors text-sm font-medium"
              >
                {deleteMutation.isPending ? t('species.deleting') : t('species.permanentlyDelete')}
              </button>
              <button
                onClick={() => setDeleteStep(0)}
                className="px-3 py-2 sm:py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
              >
                {t('common.cancel')}
              </button>
            </div>
          )}
          {deleteMutation.error && (
            <p className="text-sm text-[#E03E3E] mt-2">{deleteMutation.error.message}</p>
          )}
        </section>
      </div>
    </div>
  )
}
