import { useState } from 'react'
import { useParams, useNavigate, type NavigateFunction } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesWorkflowStepResponse } from '../api/client'
import { Masthead, Chip, Rule } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

type StepTone = 'sage' | 'mustard' | 'forest'

function numberTone(count: number): StepTone {
  if (count === 0) return 'sage'
  if (count > 0) return 'mustard'
  return 'forest'
}

function eventTypeTone(eventType: string | null | undefined): 'mustard' | 'clay' | 'forest' {
  if (eventType === 'SEEDED' || eventType === 'POTTED_UP') return 'mustard'
  if (
    eventType === 'HARVESTED' ||
    eventType === 'FIRST_BLOOM' ||
    eventType === 'PEAK_BLOOM'
  ) return 'clay'
  return 'forest'
}

const selectStyle: React.CSSProperties = {
  background: 'transparent',
  border: 'none',
  borderBottom: '1px solid var(--color-ink)',
  borderRadius: 0,
  padding: '4px 0',
  fontFamily: 'var(--font-display)',
  fontSize: 20,
  fontWeight: 300,
  color: 'var(--color-ink)',
  outline: 'none',
  minWidth: 240,
}

export function WorkflowProgress() {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()
  const { speciesId: paramSpeciesId } = useParams<{ speciesId: string }>()

  const [speciesId, setSpeciesId] = useState<number | null>(
    paramSpeciesId ? Number(paramSpeciesId) : null,
  )
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

  const selectedSpecies = speciesList?.find(s => s.id === speciesId)

  const mainSteps =
    workflow?.steps.filter(s => !s.isSideBranch).sort((a, b) => a.sortOrder - b.sortOrder) ?? []
  const sideSteps =
    workflow?.steps.filter(s => s.isSideBranch).sort((a, b) => a.sortOrder - b.sortOrder) ?? []
  const sideBranches = new Map<string, SpeciesWorkflowStepResponse[]>()
  for (const s of sideSteps) {
    const key = s.sideBranchName ?? 'other'
    const list = sideBranches.get(key) ?? []
    list.push(s)
    sideBranches.set(key, list)
  }

  const mastheadLeft = selectedSpecies
    ? (
      <span>
        {t('nav.workflows')} /{' '}
        <span style={{ color: 'var(--color-accent)' }}>{selectedSpecies.commonName}</span>
      </span>
    )
    : t('nav.workflows')

  return (
    <div>
      <Masthead left={mastheadLeft} center={t('workflows.progress.masthead.center')} />

      <div style={{ padding: '28px 40px' }}>
        {/* Species selector */}
        <div style={{ marginBottom: 32 }}>
          <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 4 }}>
            {t('common.speciesLabel')}
          </div>
          <select
            value={speciesId ?? ''}
            onChange={e => {
              setSpeciesId(e.target.value ? Number(e.target.value) : null)
              setSelectedStep(null)
            }}
            style={selectStyle}
          >
            <option value="">{t('common.selectSpecies')}</option>
            {speciesList?.map(s => (
              <option key={s.id} value={s.id}>
                {s.commonName}{s.variantName ? ` — ${s.variantName}` : ''}
              </option>
            ))}
          </select>
        </div>

        {isLoading && speciesId && (
          <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
            <div style={{ width: 32, height: 32, border: '2px solid var(--color-ink)', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
          </div>
        )}

        {error && <ErrorDisplay error={error} onRetry={refetch} />}

        {workflow && speciesId && (
          <>
            {/* Compact hero strip */}
            {selectedSpecies && (
              <div style={{ marginBottom: 32, paddingBottom: 22, borderBottom: '1px solid var(--color-ink)' }}>
                <h1 style={{
                  fontFamily: 'var(--font-display)', fontSize: 44, fontWeight: 300,
                  letterSpacing: -0.8, margin: 0,
                  fontVariationSettings: '"SOFT" 100, "opsz" 144',
                }}>
                  {selectedSpecies.commonName}
                  {selectedSpecies.variantName && (
                    <span style={{ fontStyle: 'italic', color: 'var(--color-accent)' }}>
                      {' ’'}{selectedSpecies.variantName}{'’'}
                    </span>
                  )}
                </h1>
                {workflow.templateName && (
                  <div style={{ marginTop: 6, fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                    {t('workflows.assigned')}: {workflow.templateName}
                  </div>
                )}
              </div>
            )}

            {/* Main flow ledger */}
            {mainSteps.length > 0 && (
              <section style={{ marginBottom: 40 }}>
                <StepLedger
                  steps={mainSteps}
                  speciesId={speciesId}
                  onSelectStep={setSelectedStep}
                />
              </section>
            )}

            {/* Side branches */}
            {Array.from(sideBranches.entries()).map(([branchName, steps]) => (
              <section key={branchName} style={{ marginTop: 40 }}>
                <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, marginBottom: 14 }}>
                  <h2 style={{
                    fontFamily: 'var(--font-display)', fontStyle: 'italic',
                    fontSize: 20, fontWeight: 300, margin: 0,
                  }}>
                    {t('workflows.sideBranch')}: {branchName}
                    <span style={{ color: 'var(--color-accent)' }}>.</span>
                  </h2>
                  <Rule inline variant="soft" />
                </div>
                <StepLedger
                  steps={steps}
                  speciesId={speciesId}
                  onSelectStep={setSelectedStep}
                />
              </section>
            ))}

            {workflow.steps.length === 0 && (
              <div style={{ padding: '60px 22px', textAlign: 'center', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)' }}>
                <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
                  {t('workflows.progress.empty')}
                </div>
              </div>
            )}
          </>
        )}
      </div>

      {/* Complete step dialog — preserved from original */}
      {selectedStep && (
        <Dialog
          open={true}
          title={selectedStep.name}
          onClose={() => { setSelectedStep(null); setCompleteNotes('') }}
        >
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
                  <input
                    value={completeNotes}
                    onChange={e => setCompleteNotes(e.target.value)}
                    className="input w-full"
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    className="btn-secondary flex-1"
                    onClick={() => { setSelectedStep(null); setCompleteNotes('') }}
                  >
                    {t('common.cancel')}
                  </button>
                  <button
                    className="btn-primary flex-1"
                    disabled={!plantsAtStep || plantsAtStep.length === 0 || completeMut.isPending}
                    onClick={() =>
                      plantsAtStep &&
                      completeMut.mutate({
                        stepId: selectedStep.id,
                        plantIds: plantsAtStep,
                        notes: completeNotes.trim() || undefined,
                      })
                    }
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

function StepLedger({
  steps, speciesId, onSelectStep,
}: {
  steps: SpeciesWorkflowStepResponse[]
  speciesId: number
  onSelectStep: (step: SpeciesWorkflowStepResponse) => void
}) {
  const { t } = useTranslation()

  if (steps.length === 0) {
    return (
      <div style={{ padding: '40px 22px', textAlign: 'center', borderTop: '1px solid var(--color-ink)', borderBottom: '1px solid var(--color-ink)' }}>
        <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7 }}>
          {t('workflows.progress.empty')}
        </div>
      </div>
    )
  }

  return (
    <div>
      {/* Header */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '40px 1.5fr 100px 140px 120px',
        gap: 18,
        padding: '10px 0',
        borderBottom: '1px solid var(--color-ink)',
        fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4,
        textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7,
      }}>
        <span>№</span>
        <span>{t('workflows.stepName')}</span>
        <span>{t('workflows.daysAfterPrevious')}</span>
        <span>{t('workflows.eventType')}</span>
        <span style={{ textAlign: 'right' }}>{t('workflows.progress.completion')}</span>
      </div>

      {steps.map((step, i) => (
        <StepRow
          key={step.id}
          step={step}
          index={i}
          speciesId={speciesId}
          onSelect={() => onSelectStep(step)}
        />
      ))}
    </div>
  )
}

function StepRow({
  step, index, speciesId, onSelect,
}: {
  step: SpeciesWorkflowStepResponse
  index: number
  speciesId: number
  onSelect: () => void
}) {
  const { t } = useTranslation()

  const { data: plantIds } = useQuery({
    queryKey: ['plants-at-step', step.id, speciesId],
    queryFn: () => api.workflows.getPlantsAtStep(step.id, speciesId),
  })

  const count = plantIds?.length ?? 0
  const tone = numberTone(count)
  const toneVar =
    tone === 'sage'
      ? 'var(--color-sage)'
      : tone === 'mustard'
        ? 'var(--color-mustard)'
        : 'var(--color-forest)'

  return (
    <button
      onClick={onSelect}
      style={{
        display: 'grid',
        gridTemplateColumns: '40px 1.5fr 100px 140px 120px',
        gap: 18,
        padding: '14px 0',
        width: '100%',
        background: 'transparent',
        border: 'none',
        borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
        cursor: 'pointer',
        alignItems: 'center',
        textAlign: 'left',
      }}
    >
      <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: toneVar }}>
        {String(index + 1).padStart(2, '0')}
      </span>
      <span>
        <span style={{ fontFamily: 'var(--font-display)', fontSize: 20, display: 'block' }}>{step.name}</span>
        {step.isOptional && (
          <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
            {t('workflows.optional')}
          </span>
        )}
      </span>
      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
        {step.daysAfterPrevious != null
          ? t('workflows.progress.day', { n: step.daysAfterPrevious })
          : ''}
      </span>
      <span>
        {step.eventType && step.eventType !== 'NOTE' && (
          <Chip tone={eventTypeTone(step.eventType)}>
            {t(`eventType.${step.eventType}`, { defaultValue: step.eventType })}
          </Chip>
        )}
      </span>
      <span style={{ textAlign: 'right', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.4, textTransform: 'uppercase', color: toneVar }}>
        {t('workflows.plantsAtStep', { count })}
      </span>
    </button>
  )
}
