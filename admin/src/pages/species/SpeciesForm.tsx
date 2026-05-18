import { useQuery } from '@tanstack/react-query'
import { api, type Species, type UpdateSpeciesRequest, type CreateSpeciesRequest, type SpeciesPhoto, type AddSpeciesProviderRequest, type SpeciesTag } from '../../api/client'
import { useCallback, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fileToBase64 } from '../../lib/fileToBase64'
import { ImageUpload } from '../../components/species/ImageUpload'
import { ChipToggle } from '../../components/species/ChipToggle'
import { ExtractStatus } from '../../components/species/ExtractStatus'
import { Field } from '../../components/species/Field'
import { RangeField } from '../../components/species/RangeField'
import { ProviderEditor } from '../../components/species/ProviderEditor'
import { GROWING_POSITIONS, SOIL_TYPES, usePositionLabel, useSoilLabel, type PendingProvider } from './shared'

// Re-export so peer pages (Create/Edit wrappers) can import from one place
export type { PendingProvider }

export function SpeciesForm({
  species,
  template,
  title,
  submitLabel,
  isSubmitting,
  error,
  onBack,
  onSubmit,
  photos,
  onUploadPhoto,
  onDeletePhoto,
  isUploadingPhoto,
  onAddProvider,
  onDeleteProvider,
  onUpdateProvider,
  isAddingProvider,
}: {
  species: Species | null
  template?: Species | null
  title: string
  submitLabel: string
  isSubmitting: boolean
  error: string | null
  onBack: () => void
  onSubmit: (req: CreateSpeciesRequest | UpdateSpeciesRequest, provider?: PendingProvider) => void
  photos: SpeciesPhoto[]
  onUploadPhoto: (base64: string) => void
  onDeletePhoto: (photoId: number) => void
  isUploadingPhoto: boolean
  onAddProvider?: (req: AddSpeciesProviderRequest) => void
  onDeleteProvider?: (spId: number) => void
  onUpdateProvider?: (spId: number, req: { imageFrontBase64?: string; imageBackBase64?: string }) => void
  isAddingProvider?: boolean
}) {
  const isEdit = species !== null
  const init = species ?? template ?? null
  const { t } = useTranslation()
  const positionLabel = usePositionLabel()
  const soilLabel = useSoilLabel()

  const [commonName, setCommonName] = useState(init?.commonName ?? '')
  const [commonNameSv, setCommonNameSv] = useState(init?.commonNameSv ?? '')
  const [variantName, setVariantName] = useState(init?.variantName ?? '')
  const [variantNameSv, setVariantNameSv] = useState(init?.variantNameSv ?? '')
  const [scientificName, setScientificName] = useState(init?.scientificName ?? '')
  const [germinationTimeDaysMin, setGerminationTimeDaysMin] = useState(init?.germinationTimeDaysMin?.toString() ?? '')
  const [germinationTimeDaysMax, setGerminationTimeDaysMax] = useState(init?.germinationTimeDaysMax?.toString() ?? '')
  const [daysToHarvestMin, setDaysToHarvestMin] = useState(init?.daysToHarvestMin?.toString() ?? '')
  const [daysToHarvestMax, setDaysToHarvestMax] = useState(init?.daysToHarvestMax?.toString() ?? '')
  const [sowingDepthMm, setSowingDepthMm] = useState(init?.sowingDepthMm?.toString() ?? '')
  const [heightCmMin, setHeightCmMin] = useState(init?.heightCmMin?.toString() ?? '')
  const [heightCmMax, setHeightCmMax] = useState(init?.heightCmMax?.toString() ?? '')
  const [bloomMonths, setBloomMonths] = useState<Set<number>>(new Set(init?.bloomMonths ?? []))
  const [sowingMonths, setSowingMonths] = useState<Set<number>>(new Set(init?.sowingMonths ?? []))
  const [germinationRate, setGerminationRate] = useState(init?.germinationRate?.toString() ?? '')
  const [positions, setPositions] = useState<Set<string>>(new Set(init?.growingPositions ?? []))
  const [soils, setSoils] = useState<Set<string>>(new Set(init?.soils ?? []))
  const [tagIds, setTagIds] = useState<Set<number>>(new Set(init?.tags.map(tg => tg.id) ?? []))
  const [costPerSeedSek, setCostPerSeedSek] = useState(init?.costPerSeedCents != null ? (init.costPerSeedCents / 100).toString() : '')
  const [expectedStemsPerPlant, setExpectedStemsPerPlant] = useState(init?.expectedStemsPerPlant?.toString() ?? '')
  const [expectedVaseLifeDays, setExpectedVaseLifeDays] = useState(init?.expectedVaseLifeDays?.toString() ?? '')
  const [plantType, setPlantType] = useState(init?.plantType ?? '')
  const [defaultUnitType, setDefaultUnitType] = useState(init?.defaultUnitType ?? '')
  const [workflowTemplateId, setWorkflowTemplateId] = useState<string>(init?.workflowTemplateId != null ? String(init.workflowTemplateId) : '')
  const [imageFrontBase64, setImageFrontBase64] = useState<string | null>(null)
  const [imageBackBase64, setImageBackBase64] = useState<string | null>(null)
  const [backImageSmall, setBackImageSmall] = useState(false)
  const [deletingPhotoId, setDeletingPhotoId] = useState<number | null>(null)
  const [extractingFront, setExtractingFront] = useState(false)
  const [extractingBack, setExtractingBack] = useState(false)
  const [extractedFront, setExtractedFront] = useState(false)
  const [extractedBack, setExtractedBack] = useState(false)
  const [extractError, setExtractError] = useState<string | null>(null)

  // Provider state
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null)
  const [providerProductUrl, setProviderProductUrl] = useState('')
  const [addingNewProvider, setAddingNewProvider] = useState(false)
  const [newProviderProviderId, setNewProviderProviderId] = useState<number | null>(null)
  const [newProviderFrontBase64, setNewProviderFrontBase64] = useState<string | null>(null)
  const [newProviderBackBase64, setNewProviderBackBase64] = useState<string | null>(null)
  const [newProviderProductUrl, setNewProviderProductUrl] = useState('')
  const [deletingProviderId, setDeletingProviderId] = useState<number | null>(null)

  const { data: availableProviders } = useQuery({
    queryKey: ['admin', 'providers'],
    queryFn: api.admin.getProviders
  })

  const { data: availableTags } = useQuery({
    queryKey: ['admin', 'speciesTags'],
    queryFn: api.admin.getSpeciesTags,
  })

  const { data: availableWorkflowTemplates } = useQuery({
    queryKey: ['admin', 'workflowTemplates'],
    queryFn: api.admin.getWorkflowTemplates,
  })

  const photoInputRef = useRef<HTMLInputElement>(null)

  const handleExtractFront = useCallback(async (base64: string) => {
    setExtractingFront(true)
    setExtractError(null)
    try {
      const info = await api.admin.extractFront(base64)
      if (info.commonName && !commonName) setCommonName(info.commonName)
      if (info.commonNameSv && !commonNameSv) setCommonNameSv(info.commonNameSv)
      if (info.variantName && !variantName) setVariantName(info.variantName)
      if (info.variantNameSv && !variantNameSv) setVariantNameSv(info.variantNameSv)
      if (info.scientificName && !scientificName) setScientificName(info.scientificName)
      setExtractedFront(true)
    } catch (e) {
      setExtractError(e instanceof Error ? e.message : 'Front extraction failed')
    } finally {
      setExtractingFront(false)
    }
  }, [commonName, commonNameSv, variantName, variantNameSv, scientificName])

  const handleExtractBack = useCallback(async (base64: string) => {
    setExtractingBack(true)
    setExtractError(null)
    try {
      const info = await api.admin.extractBack(base64)
      if (info.commonName && !commonName) setCommonName(info.commonName)
      if (info.scientificName && !scientificName) setScientificName(info.scientificName)
      if (info.germinationTimeDaysMin != null) setGerminationTimeDaysMin(info.germinationTimeDaysMin.toString())
      if (info.germinationTimeDaysMax != null) setGerminationTimeDaysMax(info.germinationTimeDaysMax.toString())
      if (info.daysToHarvestMin != null) setDaysToHarvestMin(info.daysToHarvestMin.toString())
      if (info.daysToHarvestMax != null) setDaysToHarvestMax(info.daysToHarvestMax.toString())
      if (info.sowingDepthMm != null) setSowingDepthMm(info.sowingDepthMm.toString())
      if (info.heightCmMin != null) setHeightCmMin(info.heightCmMin.toString())
      if (info.heightCmMax != null) setHeightCmMax(info.heightCmMax.toString())
      if (info.bloomMonths) setBloomMonths(new Set(info.bloomMonths))
      if (info.sowingMonths) setSowingMonths(new Set(info.sowingMonths))
      if (info.germinationRate != null) setGerminationRate(info.germinationRate.toString())
      if (info.growingPositions) setPositions(new Set(info.growingPositions))
      if (info.soils) setSoils(new Set(info.soils))
      setExtractedBack(true)
    } catch (e) {
      setExtractError(e instanceof Error ? e.message : 'Back extraction failed')
    } finally {
      setExtractingBack(false)
    }
  }, [commonName, scientificName])

  const handlePhotoUpload = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const b64 = await fileToBase64(file)
    onUploadPhoto(b64)
    e.target.value = ''
  }, [onUploadPhoto])

  const togglePosition = (p: string) => {
    const next = new Set(positions)
    if (next.has(p)) next.delete(p); else next.add(p)
    setPositions(next)
  }

  const toggleSoil = (s: string) => {
    const next = new Set(soils)
    if (next.has(s)) next.delete(s); else next.add(s)
    setSoils(next)
  }

  const toggleBloomMonth = (m: number) => {
    const next = new Set(bloomMonths)
    if (next.has(m)) next.delete(m); else next.add(m)
    setBloomMonths(next)
  }

  const toggleSowingMonth = (m: number) => {
    const next = new Set(sowingMonths)
    if (next.has(m)) next.delete(m); else next.add(m)
    setSowingMonths(next)
  }

  const toggleTag = (id: number) => {
    const next = new Set(tagIds)
    if (next.has(id)) next.delete(id); else next.add(id)
    setTagIds(next)
  }

  const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

  const handleSubmit = (e?: React.FormEvent) => {
    e?.preventDefault()
    const req: CreateSpeciesRequest & UpdateSpeciesRequest = {
      commonName: commonName,
      commonNameSv: commonNameSv || undefined,
      variantName: variantName || undefined,
      variantNameSv: variantNameSv || undefined,
      scientificName: scientificName || undefined,
      imageFrontBase64: imageFrontBase64 ?? undefined,
      imageBackBase64: imageBackBase64 ?? undefined,
      germinationTimeDaysMin: germinationTimeDaysMin ? parseInt(germinationTimeDaysMin) : undefined,
      germinationTimeDaysMax: germinationTimeDaysMax ? parseInt(germinationTimeDaysMax) : undefined,
      daysToHarvestMin: daysToHarvestMin ? parseInt(daysToHarvestMin) : undefined,
      daysToHarvestMax: daysToHarvestMax ? parseInt(daysToHarvestMax) : undefined,
      sowingDepthMm: sowingDepthMm ? parseInt(sowingDepthMm) : undefined,
      heightCmMin: heightCmMin ? parseInt(heightCmMin) : undefined,
      heightCmMax: heightCmMax ? parseInt(heightCmMax) : undefined,
      bloomMonths: [...bloomMonths].sort((a, b) => a - b),
      sowingMonths: [...sowingMonths].sort((a, b) => a - b),
      germinationRate: germinationRate ? parseInt(germinationRate) : undefined,
      growingPositions: [...positions],
      soils: [...soils],
      tagIds: tagIds.size > 0 ? [...tagIds] : undefined,
      costPerSeedCents: costPerSeedSek ? Math.round(parseFloat(costPerSeedSek) * 100) : undefined,
      expectedStemsPerPlant: expectedStemsPerPlant ? parseInt(expectedStemsPerPlant) : undefined,
      expectedVaseLifeDays: expectedVaseLifeDays ? parseInt(expectedVaseLifeDays) : undefined,
      plantType: plantType || undefined,
      defaultUnitType: defaultUnitType || undefined,
      workflowTemplateId: workflowTemplateId !== '' ? Number(workflowTemplateId) : undefined,
      ...(isEdit && workflowTemplateId === '' && species?.workflowTemplateId != null
        ? { clearWorkflowTemplate: true }
        : {}),
    }
    const provider = !isEdit && selectedProviderId ? {
      providerId: selectedProviderId,
      imageFrontBase64,
      imageBackBase64,
      productUrl: providerProductUrl || null,
    } : undefined
    onSubmit(req, provider)
  }

  return (
    <div>
      <button onClick={onBack} className="-ml-2 inline-flex items-center gap-1 text-sm text-[#787774] hover:text-[#37352F] hover:bg-[#F0F0EE] rounded-md px-2 py-1.5 mb-3 sm:mb-4 transition-colors">
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        {t('common.back')}
      </button>

      <h2 className="text-xl sm:text-2xl font-semibold text-[#37352F] mb-5 sm:mb-6 break-words">{title}</h2>

      {/* Provider back image floating right (desktop only) */}
      {isEdit && species?.providers?.[0]?.imageBackUrl && (
        <div className="hidden xl:block fixed right-6 top-24 z-10">
          <img
            src={species.providers[0].imageBackUrl}
            alt={t('species.seedPacketBack')}
            className="rounded-lg border border-[#E9E9E7] shadow-md cursor-pointer transition-all duration-200"
            style={{ width: backImageSmall ? '92px' : '506px' }}
            onClick={() => setBackImageSmall(!backImageSmall)}
          />
          <button
            type="button"
            onClick={() => setBackImageSmall(!backImageSmall)}
            className="absolute top-2 left-2 w-8 h-8 rounded-full bg-white/80 hover:bg-white border border-[#E9E9E7] shadow flex items-center justify-center text-[#787774] transition-colors"
            title={backImageSmall ? t('species.enlarge') : t('species.minimize')}
          >
            {backImageSmall ? (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="15 3 21 3 21 9" /><line x1="21" y1="3" x2="14" y2="10" />
                <polyline points="9 21 3 21 3 15" /><line x1="3" y1="21" x2="10" y2="14" />
              </svg>
            ) : (
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="4 14 4 20 10 20" /><line x1="4" y1="20" x2="11" y2="13" />
                <polyline points="20 10 20 4 14 4" /><line x1="20" y1="4" x2="13" y2="11" />
              </svg>
            )}
          </button>
        </div>
      )}

      <form id="species-form" onSubmit={handleSubmit} className="space-y-4 sm:space-y-6 pb-28 sm:pb-20">
        {/* Provider & Images — create mode */}
        {!isEdit && (
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.seedProviderImages')}</h3>
            <div className="mb-4">
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('species.provider')}</label>
              <select
                value={selectedProviderId ?? ''}
                onChange={e => setSelectedProviderId(e.target.value ? Number(e.target.value) : null)}
                className="w-full max-w-xs px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
              >
                <option value="">{t('species.selectProvider')}</option>
                {availableProviders?.map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
            {selectedProviderId && (
              <>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
                  <ImageUpload
                    label={t('species.front')}
                    currentUrl={imageFrontBase64 ? `data:image/jpeg;base64,${imageFrontBase64}` : null}
                    onUpload={(b64) => { setImageFrontBase64(b64); handleExtractFront(b64) }}
                  />
                  <ImageUpload
                    label={t('species.back')}
                    currentUrl={imageBackBase64 ? `data:image/jpeg;base64,${imageBackBase64}` : null}
                    onUpload={(b64) => { setImageBackBase64(b64); handleExtractBack(b64) }}
                  />
                </div>
                <div className="mt-4">
                  <Field label={t('species.productUrl')} value={providerProductUrl} onChange={setProviderProductUrl} />
                </div>
                <ExtractStatus
                  extractingFront={extractingFront}
                  extractingBack={extractingBack}
                  extractedFront={extractedFront}
                  extractedBack={extractedBack}
                  extractError={extractError}
                />
              </>
            )}
          </section>
        )}

        {/* Additional Photos — only in edit mode */}
        {isEdit && (
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider">
                {t('species.additionalPhotos')}
                <span className="ml-2 text-xs font-normal text-[#787774] normal-case">({photos.length})</span>
              </h3>
              <button
                type="button"
                onClick={() => photoInputRef.current?.click()}
                disabled={isUploadingPhoto}
                className="px-3 py-1.5 bg-[#2EAADC] text-white text-sm rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors"
              >
                {isUploadingPhoto ? t('common.uploading') : t('common.upload')}
              </button>
              <input ref={photoInputRef} type="file" accept="image/*" className="hidden" onChange={handlePhotoUpload} />
            </div>
            {photos.length === 0 ? (
              <p className="text-[#A5A29C] text-sm">{t('species.noAdditionalPhotos')}</p>
            ) : (
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3 sm:gap-4">
                {photos.map(photo => (
                  <div key={photo.id} className="relative group">
                    <img src={photo.imageUrl} alt="" className="w-full h-28 sm:h-32 object-cover rounded-md" />
                    <div className="absolute inset-0 bg-black/0 md:group-hover:bg-black/30 transition-colors rounded-md flex items-center justify-center">
                      {deletingPhotoId === photo.id ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => { onDeletePhoto(photo.id); setDeletingPhotoId(null) }}
                            className="px-2 py-1 bg-[#E03E3E] text-white text-xs rounded font-medium"
                          >
                            {t('common.confirm')}
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingPhotoId(null)}
                            className="px-2 py-1 bg-white text-[#37352F] text-xs rounded font-medium"
                          >
                            {t('common.cancel')}
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setDeletingPhotoId(photo.id)}
                          aria-label={t('common.delete')}
                          className="absolute top-1 right-1 md:opacity-0 md:group-hover:opacity-100 inline-flex items-center justify-center w-7 h-7 bg-[#E03E3E] text-white rounded-full font-medium shadow transition-opacity"
                        >
                          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {/* Basic Info */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider">{t('species.basicInfo')}</h3>
            <button
              type="button"
              onClick={() => {
                if (!commonName && commonNameSv) setCommonName(commonNameSv)
                if (!variantName && variantNameSv) setVariantName(variantNameSv)
              }}
              className="text-xs text-[#2EAADC] hover:underline font-medium"
            >
              {t('species.copyToEnglish')}
            </button>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
            <Field label={t('species.commonNameSv')} value={commonNameSv} onChange={setCommonNameSv} />
            <Field label={t('species.commonNameEn')} value={commonName} onChange={setCommonName} />
            <Field
              label={t('species.variantNameSv')}
              value={variantNameSv}
              onChange={setVariantNameSv}
              onBlur={() => {
                if (variantName.trimEnd().endsWith('(kopia)')) setVariantName(variantNameSv)
              }}
            />
            <Field label={t('species.variantNameEn')} value={variantName} onChange={setVariantName} />
            <Field label={t('species.scientificName')} value={scientificName} onChange={setScientificName} className="sm:col-span-2" />
          </div>
        </section>

        {/* Growth Info */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.growthInfo')}</h3>
          <div className="space-y-3">
            <RangeField label={t('species.germinationTimeDays')} min={germinationTimeDaysMin} max={germinationTimeDaysMax} onMinChange={v => setGerminationTimeDaysMin(v.replace(/\D/g, ''))} onMaxChange={v => setGerminationTimeDaysMax(v.replace(/\D/g, ''))} />
            <RangeField label={t('species.daysToHarvest')} min={daysToHarvestMin} max={daysToHarvestMax} onMinChange={v => setDaysToHarvestMin(v.replace(/\D/g, ''))} onMaxChange={v => setDaysToHarvestMax(v.replace(/\D/g, ''))} />
            <RangeField label={t('species.heightCm')} min={heightCmMin} max={heightCmMax} onMinChange={v => setHeightCmMin(v.replace(/\D/g, ''))} onMaxChange={v => setHeightCmMax(v.replace(/\D/g, ''))} />
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
              <Field label={t('species.sowingDepthMm')} value={sowingDepthMm} onChange={v => setSowingDepthMm(v.replace(/\D/g, ''))} type="text" />
              <Field label={t('species.germinationRatePercent')} value={germinationRate} onChange={v => setGerminationRate(v.replace(/\D/g, ''))} type="text" />
            </div>
          </div>
        </section>

        {/* Sowing & Bloom Months */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.sowingBloomMonths')}</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">{t('species.sowingMonths')}</label>
              <div className="flex gap-1.5 flex-wrap">
                {MONTH_LABELS.map((label, i) => (
                  <ChipToggle key={i} label={label} selected={sowingMonths.has(i + 1)} onClick={() => toggleSowingMonth(i + 1)} />
                ))}
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">{t('species.bloomMonths')}</label>
              <div className="flex gap-1.5 flex-wrap">
                {MONTH_LABELS.map((label, i) => (
                  <ChipToggle key={i} label={label} selected={bloomMonths.has(i + 1)} onClick={() => toggleBloomMonth(i + 1)} />
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Growing Conditions */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.growingConditions')}</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">{t('species.growingPosition')}</label>
              <div className="flex gap-1.5 flex-wrap">
                {GROWING_POSITIONS.map(p => (
                  <ChipToggle key={p} label={positionLabel[p]} selected={positions.has(p)} onClick={() => togglePosition(p)} />
                ))}
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">{t('species.soilType')}</label>
              <div className="flex gap-1.5 flex-wrap">
                {SOIL_TYPES.map(s => (
                  <ChipToggle key={s} label={soilLabel[s]} selected={soils.has(s)} onClick={() => toggleSoil(s)} />
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Providers — edit mode */}
        {isEdit && (
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider">
                {t('species.seedProviders')}
                <span className="ml-2 text-xs font-normal text-[#787774] normal-case">({species!.providers.length})</span>
              </h3>
              {!addingNewProvider && (
                <button
                  type="button"
                  onClick={() => setAddingNewProvider(true)}
                  className="px-3 py-1.5 bg-[#2EAADC] text-white text-sm rounded-md hover:bg-[#2898C4] transition-colors"
                >
                  {t('species.addProvider')}
                </button>
              )}
            </div>

            {/* Existing providers */}
            <div className="space-y-4">
              {species!.providers.map(sp => (
                <div key={sp.id} className="p-4 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md">
                  <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
                    <div className="min-w-0">
                      <span className="text-sm font-medium text-[#37352F]">{sp.providerName}</span>
                      <span className="ml-2 text-xs text-[#787774]">{sp.providerIdentifier}</span>
                    </div>
                    <div className="flex items-center gap-3">
                      {sp.productUrl && (
                        <a href={sp.productUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-[#2EAADC] hover:underline">
                          {t('species.productPage')} &rarr;
                        </a>
                      )}
                      {deletingProviderId === sp.id ? (
                        <>
                          <button
                            type="button"
                            onClick={() => { onDeleteProvider?.(sp.id); setDeletingProviderId(null) }}
                            className="text-[#E03E3E] text-sm font-medium hover:underline"
                          >
                            {t('common.confirm')}
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingProviderId(null)}
                            className="text-[#787774] text-sm hover:underline"
                          >
                            {t('common.cancel')}
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setDeletingProviderId(sp.id)}
                          className="text-[#787774] text-sm hover:text-[#E03E3E] transition-colors"
                        >
                          {t('common.remove')}
                        </button>
                      )}
                    </div>
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                    <ImageUpload
                      label={t('species.front')}
                      currentUrl={sp.imageFrontUrl}
                      onUpload={(b64) => { onUpdateProvider?.(sp.id, { imageFrontBase64: b64 }); handleExtractFront(b64) }}
                    />
                    <ImageUpload
                      label={t('species.back')}
                      currentUrl={sp.imageBackUrl}
                      onUpload={(b64) => { onUpdateProvider?.(sp.id, { imageBackBase64: b64 }); handleExtractBack(b64) }}
                    />
                  </div>
                  <ExtractStatus
                    extractingFront={extractingFront}
                    extractingBack={extractingBack}
                    extractedFront={extractedFront}
                    extractedBack={extractedBack}
                    extractError={extractError}
                  />
                </div>
              ))}
            </div>

            {/* Add new provider form */}
            {addingNewProvider && (
              <ProviderEditor
                wrapperClassName="mt-4 p-4 border border-dashed border-[#2EAADC] rounded-md bg-[#FBFBFA]"
                selectClassName="w-full max-w-xs px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
                providerId={newProviderProviderId}
                onProviderIdChange={setNewProviderProviderId}
                frontBase64={newProviderFrontBase64}
                onFrontChange={setNewProviderFrontBase64}
                backBase64={newProviderBackBase64}
                onBackChange={setNewProviderBackBase64}
                productUrl={newProviderProductUrl}
                onProductUrlChange={setNewProviderProductUrl}
                availableProviders={availableProviders}
                excludedProviderIds={species!.providers.map(sp => sp.providerId)}
                isPending={!!isAddingProvider}
                onAdd={() => {
                  if (!newProviderProviderId) return
                  onAddProvider?.({
                    providerId: newProviderProviderId,
                    imageFrontBase64: newProviderFrontBase64,
                    imageBackBase64: newProviderBackBase64,
                    productUrl: newProviderProductUrl || undefined,
                  })
                  setAddingNewProvider(false)
                  setNewProviderProviderId(null)
                  setNewProviderFrontBase64(null)
                  setNewProviderBackBase64(null)
                  setNewProviderProductUrl('')
                }}
                onCancel={() => {
                  setAddingNewProvider(false)
                  setNewProviderProviderId(null)
                  setNewProviderFrontBase64(null)
                  setNewProviderBackBase64(null)
                  setNewProviderProductUrl('')
                }}
              />
            )}

            {species!.providers.length === 0 && !addingNewProvider && (
              <p className="text-[#A5A29C] text-sm">{t('species.noProvidersYet')}</p>
            )}
          </section>
        )}

        {/* Tags */}
        {availableTags && availableTags.length > 0 && (
          <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.tags')}</h3>
            <div className="flex gap-1.5 flex-wrap">
              {availableTags.map((tg: SpeciesTag) => (
                <ChipToggle key={tg.id} label={tg.name} selected={tagIds.has(tg.id)} onClick={() => toggleTag(tg.id)} />
              ))}
            </div>
          </section>
        )}

        {/* Commercial & Classification */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.commercialClassification')}</h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 sm:gap-4 mb-4">
            <Field label={t('species.costPerSeedSek')} value={costPerSeedSek} onChange={setCostPerSeedSek} type="text" />
            <Field label={t('species.expectedStemsPerPlant')} value={expectedStemsPerPlant} onChange={v => setExpectedStemsPerPlant(v.replace(/\D/g, ''))} type="text" />
            <Field label={t('species.expectedVaseLifeDays')} value={expectedVaseLifeDays} onChange={v => setExpectedVaseLifeDays(v.replace(/\D/g, ''))} type="text" />
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4 sm:max-w-sm">
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('species.plantType')}</label>
              <select
                value={plantType}
                onChange={e => setPlantType(e.target.value)}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
              >
                <option value="">—</option>
                <option value="ANNUAL">{t('species.plantTypeAnnual')}</option>
                <option value="PERENNIAL">{t('species.plantTypePerennial')}</option>
                <option value="BULB">{t('species.plantTypeBulb')}</option>
                <option value="TUBER">{t('species.plantTypeTuber')}</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('species.defaultUnitType')}</label>
              <select
                value={defaultUnitType}
                onChange={e => setDefaultUnitType(e.target.value)}
                className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
              >
                <option value="">—</option>
                <option value="SEED">{t('species.unitTypeSeed')}</option>
                <option value="PLUG">{t('species.unitTypePlug')}</option>
                <option value="BULB">{t('species.unitTypeBulb')}</option>
                <option value="TUBER">{t('species.unitTypeTuber')}</option>
                <option value="PLANT">{t('species.unitTypePlant')}</option>
              </select>
            </div>
          </div>
        </section>

        {/* Workflow */}
        <section className="border border-[#E9E9E7] rounded-lg p-4 sm:p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">{t('species.workflow')}</h3>
          <div className="sm:max-w-sm">
            <label className="block text-xs font-medium text-[#787774] mb-1.5">{t('species.workflowTemplate')}</label>
            <select
              value={workflowTemplateId}
              onChange={e => setWorkflowTemplateId(e.target.value)}
              className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
            >
              <option value="">{t('species.workflowNone')}</option>
              {availableWorkflowTemplates?.map(tpl => (
                <option key={tpl.id} value={tpl.id}>{tpl.name}</option>
              ))}
              {workflowTemplateId !== '' && availableWorkflowTemplates && !availableWorkflowTemplates.some(t => String(t.id) === workflowTemplateId) && (
                <option value={workflowTemplateId}>{t('species.workflowMissing', { id: workflowTemplateId })}</option>
              )}
            </select>
          </div>
        </section>

        {/* Submit */}
        {error && (
          <div className="bg-[#FBE4E4] text-[#E03E3E] px-3 py-2.5 rounded-md text-sm">{error}</div>
        )}

      </form>

      {/* Floating save bar */}
      <div
        className="fixed bottom-0 left-0 right-0 bg-white border-t border-[#E9E9E7] px-4 sm:px-6 py-3 flex flex-wrap gap-2 sm:gap-3 justify-end sm:justify-center shadow-lg z-10"
        style={{ paddingBottom: 'max(0.75rem, env(safe-area-inset-bottom))' }}
      >
        {error && <span className="text-[#E03E3E] text-sm self-center w-full sm:w-auto sm:absolute sm:left-6">{error}</span>}
        <button
          type="button"
          onClick={onBack}
          className="px-4 py-2 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
        >
          {t('common.cancel')}
        </button>
        <button
          type="submit"
          form="species-form"
          disabled={isSubmitting || (!commonNameSv.trim() && !commonName.trim())}
          className="px-6 py-2 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors text-sm font-medium"
        >
          {isSubmitting ? t('species.saving') : submitLabel}
        </button>
      </div>
    </div>
  )
}
