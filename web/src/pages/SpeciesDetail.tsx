import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import { useState } from 'react'

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="border border-divider rounded-xl bg-bg overflow-hidden">
      <div className="px-4 py-2 bg-surface border-b border-divider">
        <h2 className="text-sm font-medium text-text-primary">{title}</h2>
      </div>
      <div className="px-4 py-3 space-y-2">{children}</div>
    </div>
  )
}

function Field({ label, value }: { label: string; value?: string | number | null }) {
  if (value == null || value === '') return null
  return (
    <div className="flex justify-between text-sm">
      <span className="text-text-secondary">{label}</span>
      <span className="text-text-primary">{value}</span>
    </div>
  )
}

function Chip({ label }: { label: string }) {
  return (
    <span className="inline-block text-xs bg-accent/15 text-accent px-2 py-0.5 rounded-full">
      {label}
    </span>
  )
}

export function SpeciesDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()
  const [showDelete, setShowDelete] = useState(false)

  const { data: species, error, isLoading, refetch } = useQuery({
    queryKey: ['species', id],
    queryFn: () => api.species.get(Number(id)),
    enabled: !!id,
  })

  const deleteMut = useMutation({
    mutationFn: () => api.species.delete(Number(id)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species'] })
      navigate('/species')
    },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!species) return null

  const lang = i18n.language
  const name = lang === 'sv' ? (species.commonNameSv ?? species.commonName) : species.commonName
  const variant = lang === 'sv' ? (species.variantNameSv ?? species.variantName) : species.variantName
  const displayName = variant ? `${name} — ${variant}` : name

  const canDelete = species.custom || !species.isSystem

  const hasGrowing = species.germinationTimeDaysMin != null || species.daysToHarvestMin != null ||
    species.sowingDepthMm != null || species.heightCmMin != null ||
    species.germinationRate != null || species.bloomMonths || species.sowingMonths

  const hasCommercial = species.costPerSeedSek != null || species.expectedStemsPerPlant != null ||
    species.expectedVaseLifeDays != null

  return (
    <div className="max-w-md">
      <PageHeader
        title={displayName}
        breadcrumbs={[
          { label: t('nav.species'), to: '/species' },
          { label: displayName, to: `/species/${id}` },
        ]}
      />

      <div className="px-4 pb-24 space-y-4">
        {/* Basic Info */}
        <Section title={t('speciesDetail.basicInfo')}>
          <Field label={t('species.commonName')} value={species.commonName} />
          {species.commonNameSv && (
            <Field label={t('speciesDetail.commonNameSv')} value={species.commonNameSv} />
          )}
          {species.variantName && (
            <Field label={t('species.variantName')} value={species.variantName} />
          )}
          {species.variantNameSv && (
            <Field label={t('species.variantNameSv')} value={species.variantNameSv} />
          )}
          {species.scientificName && (
            <Field label={t('species.scientificName')} value={species.scientificName} />
          )}
          {species.plantType && (
            <Field label={t('speciesDetail.plantType')} value={species.plantType} />
          )}
          {species.defaultUnitType && (
            <Field label={t('speciesDetail.unitType')} value={species.defaultUnitType} />
          )}
        </Section>

        {/* Growing */}
        {hasGrowing && (
          <Section title={t('speciesDetail.growing')}>
            {species.germinationTimeDaysMin != null && (
              <Field label={t('speciesDetail.germinationTime')} value={
                species.germinationTimeDaysMax && species.germinationTimeDaysMax !== species.germinationTimeDaysMin
                  ? `${species.germinationTimeDaysMin}–${species.germinationTimeDaysMax} ${t('speciesDetail.days')}`
                  : `${species.germinationTimeDaysMin} ${t('speciesDetail.days')}`
              } />
            )}
            {species.daysToHarvestMin != null && (
              <Field label={t('speciesDetail.daysToHarvest')} value={
                species.daysToHarvestMax && species.daysToHarvestMax !== species.daysToHarvestMin
                  ? `${species.daysToHarvestMin}–${species.daysToHarvestMax} ${t('speciesDetail.days')}`
                  : `${species.daysToHarvestMin} ${t('speciesDetail.days')}`
              } />
            )}
            {species.sowingDepthMm != null && (
              <Field label={t('speciesDetail.sowingDepth')} value={`${species.sowingDepthMm} mm`} />
            )}
            {species.heightCmMin != null && (
              <Field label={t('speciesDetail.height')} value={
                species.heightCmMax && species.heightCmMax !== species.heightCmMin
                  ? `${species.heightCmMin}–${species.heightCmMax} cm`
                  : `${species.heightCmMin} cm`
              } />
            )}
            {species.germinationRate != null && (
              <Field label={t('speciesDetail.germinationRate')} value={`${species.germinationRate}%`} />
            )}
            {species.bloomMonths && (
              <Field label={t('speciesDetail.bloomMonths')} value={species.bloomMonths} />
            )}
            {species.sowingMonths && (
              <Field label={t('speciesDetail.sowingMonths')} value={species.sowingMonths} />
            )}
          </Section>
        )}

        {/* Commercial */}
        {hasCommercial && (
          <Section title={t('speciesDetail.commercial')}>
            {species.costPerSeedSek != null && (
              <Field label={t('speciesDetail.costPerSeed')} value={`${species.costPerSeedSek} kr`} />
            )}
            {species.expectedStemsPerPlant != null && (
              <Field label={t('speciesDetail.stemsPerPlant')} value={species.expectedStemsPerPlant} />
            )}
            {species.expectedVaseLifeDays != null && (
              <Field label={t('speciesDetail.vaseLife')} value={`${species.expectedVaseLifeDays} ${t('speciesDetail.days')}`} />
            )}
          </Section>
        )}

        {/* Groups */}
        {species.groups && species.groups.length > 0 && (
          <Section title={t('speciesDetail.groups')}>
            <div className="flex flex-wrap gap-2">
              {species.groups.map(g => <Chip key={g.id} label={g.name} />)}
            </div>
          </Section>
        )}

        {/* Tags */}
        {species.tags && species.tags.length > 0 && (
          <Section title={t('speciesDetail.tags')}>
            <div className="flex flex-wrap gap-2">
              {species.tags.map(tag => <Chip key={tag.id} label={tag.name} />)}
            </div>
          </Section>
        )}

        {/* Providers */}
        {species.providers && species.providers.length > 0 && (
          <Section title={t('speciesDetail.providers')}>
            {species.providers.map(p => (
              <div key={p.id} className="text-sm text-text-primary">{p.providerName}</div>
            ))}
          </Section>
        )}

        {/* Delete */}
        {canDelete && (
          <div className="pt-4">
            <button
              onClick={() => setShowDelete(true)}
              className="btn-primary bg-error w-full"
            >
              {t('common.delete')}
            </button>
          </div>
        )}
      </div>

      {showDelete && (
        <Dialog open={showDelete} title={t('species.deleteSpeciesTitle')} onClose={() => setShowDelete(false)}>
          <p className="text-sm mb-4">{t('common.delete')} &ldquo;{displayName}&rdquo;?</p>
          <div className="flex gap-2">
            <button className="btn-secondary flex-1" onClick={() => setShowDelete(false)}>{t('common.cancel')}</button>
            <button
              className="btn-primary flex-1 bg-error"
              onClick={() => deleteMut.mutate()}
              disabled={deleteMut.isPending}
            >
              {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
            </button>
          </div>
        </Dialog>
      )}
    </div>
  )
}
