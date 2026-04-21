import { useState } from 'react'
import { useParams, useNavigate, type NavigateFunction } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesWorkflowStepResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import type { BreadcrumbItem } from '../components/Breadcrumb'

export function WorkflowProgress() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { speciesId: paramSpeciesId } = useParams<{ speciesId: string }>()

  const [speciesId, setSpeciesId] = useState<number | null>(paramSpeciesId ? Number(paramSpeciesId) : null)
  const [selectedStep, setSelectedStep] = useState<SpeciesWorkflowStepResponse | null>(null)
  const [completeNotes, setCompleteNotes] = useState('')

  const { data: speciesList } = useQuery({
    queryKey: ['species'],
    queryFn: api.species.list,
  })

  const { data: workflow, error, isLoading, refetch } = useQuery({
    queryKey: ['species-workflow', speciesId],
    queryFn: () => api.workflows.getSpeciesWorkflow(speciesId!),
    enabled: !!speciesId,
  })

  const { data: plantsAtStep } = useQuery({
    queryKey: ['plants-at-step', selectedStep?.id, speciesId],
    queryFn: () => api.workflows.getPlantsAtStep(selectedStep!.id, speciesId!),
    enabled: !!selectedStep && !!speciesId,
  })

  const completeMut = useMutation({
    mutationFn: ({ stepId, plantIds, notes }: { stepId: number; plantIds: number[]; notes?: string }) =>
      api.workflows.completeStep(stepId, { plantIds, notes }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['plants-at-step'] })
      qc.invalidateQueries({ queryKey: ['species-workflow', speciesId] })
      setSelectedStep(null)
      setCompleteNotes('')
    },
  })

  const breadcrumbs: BreadcrumbItem[] = [
    { label: t('workflows.title'), to: '/workflows' },
    { label: t('workflows.progress'), to: '#' },
  ]

  const mainSteps = workflow?.steps.filter(s => !s.isSideBranch).sort((a, b) => a.sortOrder - b.sortOrder) ?? []
  const sideSteps = workflow?.steps.filter(s => s.isSideBranch).sort((a, b) => a.sortOrder - b.sortOrder) ?? []
  const sideBranches = new Map<string, SpeciesWorkflowStepResponse[]>()
  for (const s of sideSteps) {
    const key = s.sideBranchName ?? 'other'
    const list = sideBranches.get(key) ?? []
    list.push(s)
    sideBranches.set(key, list)
  }

  return (
    <div className="max-w-lg">
      <PageHeader title={t('workflows.progress')} breadcrumbs={breadcrumbs} />

      <div className="px-4 space-y-4">
        {/* Species selector */}
        <div>
          <label className="field-label">{t('common.speciesLabel')}</label>
          <select
            value={speciesId ?? ''}
            onChange={e => { setSpeciesId(e.target.value ? Number(e.target.value) : null); setSelectedStep(null) }}
            className="input w-full"
          >
            <option value="">{t('common.selectSpecies')}</option>
            {speciesList?.map(s => (
              <option key={s.id} value={s.id}>{s.commonName}{s.variantName ? ` — ${s.variantName}` : ''}</option>
            ))}
          </select>
        </div>

        {isLoading && speciesId && (
          <div className="flex justify-center p-8"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
        )}

        {error && <ErrorDisplay error={error} onRetry={refetch} />}

        {workflow && speciesId && (
          <>
            {workflow.templateName && (
              <p className="text-xs text-text-secondary">{t('workflows.assigned')}: {workflow.templateName}</p>
            )}

            {/* Main flow */}
            {mainSteps.length > 0 && (
              <section className="form-card">
                <label className="field-label">{t('workflows.mainFlow')}</label>
                <div className="space-y-1">
                  {mainSteps.map(step => (
                    <StepProgressRow
                      key={step.id}
                      step={step}
                      speciesId={speciesId}
                      onClick={() => setSelectedStep(step)}
                    />
                  ))}
                </div>
              </section>
            )}

            {/* Side branches */}
            {Array.from(sideBranches.entries()).map(([branchName, steps]) => (
              <section key={branchName} className="form-card">
                <label className="field-label">{t('workflows.sideBranch')}: {branchName}</label>
                <div className="space-y-1">
                  {steps.map(step => (
                    <StepProgressRow
                      key={step.id}
                      step={step}
                      speciesId={speciesId}
                      onClick={() => setSelectedStep(step)}
                    />
                  ))}
                </div>
              </section>
            ))}

            {workflow.steps.length === 0 && (
              <p className="text-text-secondary text-sm text-center py-4">{t('workflows.noSteps')}</p>
            )}
          </>
        )}
      </div>

      {/* Complete step dialog */}
      {selectedStep && (
        <Dialog open={true} title={selectedStep.name} onClose={() => { setSelectedStep(null); setCompleteNotes('') }}>
          <div className="space-y-3">
            {plantsAtStep && (
              <p className="text-sm">{t('workflows.plantsAtStep', { count: plantsAtStep.length })}</p>
            )}
            {selectedStep.eventType === 'APPLIED_SUPPLY' ? (
              <ApplySupplyStepAction
                step={selectedStep}
                plantIds={plantsAtStep ?? []}
                onNavigate={() => setSelectedStep(null)}
                navigate={navigate}
              />
            ) : (
              <>
                <div>
                  <label className="field-label">{t('common.notesLabel')}</label>
                  <input value={completeNotes} onChange={e => setCompleteNotes(e.target.value)} className="input w-full" />
                </div>
                <div className="flex gap-2">
                  <button className="btn-secondary flex-1" onClick={() => { setSelectedStep(null); setCompleteNotes('') }}>
                    {t('common.cancel')}
                  </button>
                  <button
                    className="btn-primary flex-1"
                    disabled={!plantsAtStep || plantsAtStep.length === 0 || completeMut.isPending}
                    onClick={() => plantsAtStep && completeMut.mutate({
                      stepId: selectedStep.id,
                      plantIds: plantsAtStep,
                      notes: completeNotes.trim() || undefined,
                    })}
                  >
                    {completeMut.isPending
                      ? t('common.saving')
                      : t('workflows.completeForPlants', { count: plantsAtStep?.length ?? 0 })}
                  </button>
                </div>
              </>
            )}
          </div>
        </Dialog>
      )}
    </div>
  )
}

function ApplySupplyStepAction({
  step, plantIds, onNavigate, navigate,
}: {
  step: SpeciesWorkflowStepResponse
  plantIds: number[]
  onNavigate: () => void
  navigate: NavigateFunction
}) {
  const { t } = useTranslation()

  // Fetch the first plant to resolve its bedId for the apply-supply route
  const { data: firstPlant } = useQuery({
    queryKey: ['plant', plantIds[0]],
    queryFn: () => api.plants.get(plantIds[0]),
    enabled: plantIds.length > 0,
  })

  const handleGo = () => {
    if (!firstPlant?.bedId) return
    const qs = new URLSearchParams({
      bedId: String(firstPlant.bedId),
      plantIds: plantIds.join(','),
      stepId: String(step.id),
      ...(step.suggestedSupplyTypeId ? { supplyTypeId: String(step.suggestedSupplyTypeId) } : {}),
      ...(step.suggestedQuantity ? { quantity: String(step.suggestedQuantity) } : {}),
    })
    onNavigate()
    navigate(`/activity/apply-supply?${qs.toString()}`)
  }

  return (
    <div className="flex gap-2">
      <button className="btn-secondary flex-1" onClick={onNavigate}>{t('common.cancel')}</button>
      <button
        className="btn-primary flex-1"
        disabled={plantIds.length === 0 || !firstPlant?.bedId}
        onClick={handleGo}
      >
        {t('supplyApplication.submit')}
      </button>
    </div>
  )
}

function StepProgressRow({
  step, speciesId, onClick,
}: {
  step: SpeciesWorkflowStepResponse
  speciesId: number
  onClick: () => void
}) {
  const { t } = useTranslation()

  const { data: plantIds } = useQuery({
    queryKey: ['plants-at-step', step.id, speciesId],
    queryFn: () => api.workflows.getPlantsAtStep(step.id, speciesId),
  })

  const count = plantIds?.length ?? 0

  return (
    <div
      className="flex items-center gap-2 py-2 px-3 border border-divider rounded-lg cursor-pointer hover:bg-surface/50 transition-colors"
      onClick={onClick}
    >
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium truncate">{step.name}</p>
        <div className="flex gap-1 mt-0.5">
          {step.eventType && (
            <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{step.eventType}</span>
          )}
          {step.isOptional && (
            <span className="text-xs bg-warning/15 text-warning px-1.5 py-0.5 rounded">{t('workflows.optional')}</span>
          )}
        </div>
      </div>
      <span className="text-xs text-text-secondary shrink-0">{t('workflows.plantsAtStep', { count })}</span>
    </div>
  )
}
