import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesWorkflowStepResponse } from '../api/client'
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

        {/* Workflow */}
        <WorkflowSection speciesId={Number(id)} />

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

function WorkflowStepItem({ step }: { step: SpeciesWorkflowStepResponse }) {
  const { t } = useTranslation()
  return (
    <div className={`flex items-center gap-2 py-1.5 ${step.isSideBranch ? 'ml-4' : ''}`}>
      <div className="flex-1 min-w-0">
        <p className="text-sm truncate">{step.name}</p>
        <div className="flex flex-wrap gap-1 mt-0.5">
          {step.eventType && (
            <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{step.eventType}</span>
          )}
          {step.daysAfterPrevious != null && step.daysAfterPrevious > 0 && (
            <span className="text-xs text-text-secondary">+{step.daysAfterPrevious}d</span>
          )}
          {step.isOptional && (
            <span className="text-xs bg-warning/15 text-warning px-1.5 py-0.5 rounded">{t('workflows.optional')}</span>
          )}
          {step.isSideBranch && (
            <span className="text-xs bg-info/15 text-info px-1.5 py-0.5 rounded">{step.sideBranchName}</span>
          )}
        </div>
      </div>
    </div>
  )
}

function WorkflowSection({ speciesId }: { speciesId: number }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null)
  const [showAddStep, setShowAddStep] = useState(false)
  const [newStepName, setNewStepName] = useState('')

  const { data: workflow, isLoading } = useQuery({
    queryKey: ['species-workflow', speciesId],
    queryFn: () => api.workflows.getSpeciesWorkflow(speciesId),
  })

  const { data: templates } = useQuery({
    queryKey: ['workflow-templates'],
    queryFn: api.workflows.templates,
  })

  const assignMut = useMutation({
    mutationFn: (templateId: number) => api.workflows.assignToSpecies(speciesId, templateId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species-workflow', speciesId] }) },
  })

  const syncMut = useMutation({
    mutationFn: () => api.workflows.syncSpeciesWorkflow(speciesId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species-workflow', speciesId] }) },
  })

  const addStepMut = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.workflows.addSpeciesStep(speciesId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-workflow', speciesId] })
      setShowAddStep(false)
      setNewStepName('')
    },
  })

  if (isLoading) return null

  const hasWorkflow = workflow && workflow.steps.length > 0
  const sortedSteps = hasWorkflow ? [...workflow.steps].sort((a, b) => a.sortOrder - b.sortOrder) : []

  return (
    <>
      <Section title={t('workflows.title')}>
        {!hasWorkflow && (
          <div className="space-y-2">
            <select
              value={selectedTemplateId ?? ''}
              onChange={e => setSelectedTemplateId(e.target.value ? Number(e.target.value) : null)}
              className="input w-full text-sm"
            >
              <option value="">{t('workflows.selectTemplate')}</option>
              {templates?.map(tmpl => (
                <option key={tmpl.id} value={tmpl.id}>{tmpl.name}</option>
              ))}
            </select>
            <button
              className="btn-primary text-sm w-full"
              disabled={!selectedTemplateId || assignMut.isPending}
              onClick={() => selectedTemplateId && assignMut.mutate(selectedTemplateId)}
            >
              {assignMut.isPending ? t('common.saving') : t('workflows.assignTemplate')}
            </button>
          </div>
        )}

        {hasWorkflow && (
          <div className="space-y-2">
            {workflow.templateName && (
              <p className="text-xs text-text-secondary">{t('workflows.assigned')}: {workflow.templateName}</p>
            )}
            <div className="space-y-0.5">
              {sortedSteps.map(step => (
                <WorkflowStepItem key={step.id} step={step} />
              ))}
            </div>
            <div className="flex gap-2 pt-1">
              {workflow.templateId && (
                <button
                  className="btn-secondary text-xs flex-1"
                  disabled={syncMut.isPending}
                  onClick={() => syncMut.mutate()}
                >
                  {syncMut.isPending ? t('common.saving') : t('workflows.syncFromTemplate')}
                </button>
              )}
              <button
                className="btn-secondary text-xs flex-1"
                onClick={() => setShowAddStep(true)}
              >
                {t('workflows.addStep')}
              </button>
              <button
                className="btn-secondary text-xs flex-1"
                onClick={() => navigate(`/workflows/progress/${speciesId}`)}
              >
                {t('workflows.progress')}
              </button>
            </div>
          </div>
        )}
      </Section>

      {showAddStep && (
        <Dialog open={showAddStep} title={t('workflows.addStep')} onClose={() => setShowAddStep(false)}>
          <div className="space-y-3">
            <div>
              <label className="field-label">{t('workflows.stepName')}</label>
              <input
                value={newStepName}
                onChange={e => setNewStepName(e.target.value)}
                className="input w-full"
                autoFocus
              />
            </div>
            <div className="flex gap-2">
              <button className="btn-secondary flex-1" onClick={() => setShowAddStep(false)}>{t('common.cancel')}</button>
              <button
                className="btn-primary flex-1"
                disabled={!newStepName.trim() || addStepMut.isPending}
                onClick={() => addStepMut.mutate({ name: newStepName.trim(), sortOrder: sortedSteps.length })}
              >
                {addStepMut.isPending ? t('common.creating') : t('common.add')}
              </button>
            </div>
          </div>
        </Dialog>
      )}
    </>
  )
}
