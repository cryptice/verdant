import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type WorkflowStepResponse, type SupplyTypeResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'
import type { BreadcrumbItem } from '../components/Breadcrumb'

const EVENT_TYPES = [
  '', 'SEEDED', 'POTTED_UP', 'PLANTED_OUT', 'HARVESTED',
  'PINCHED', 'FERTILIZED', 'WATERED', 'PRUNED', 'NOTE', 'OTHER', 'APPLIED_SUPPLY',
]

function StepRow({
  step, onUpdate, onDelete, onMoveUp, onMoveDown, isFirst, isLast, supplyTypes,
}: {
  step: WorkflowStepResponse
  onUpdate: (data: Record<string, unknown>) => void
  onDelete: () => void
  onMoveUp: () => void
  onMoveDown: () => void
  isFirst: boolean
  isLast: boolean
  supplyTypes?: SupplyTypeResponse[]
}) {
  const { t } = useTranslation()
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(step.name)
  const [eventType, setEventType] = useState(step.eventType ?? '')
  const [days, setDays] = useState(step.daysAfterPrevious ?? 0)
  const [isOptional, setIsOptional] = useState(step.isOptional)
  const [isSideBranch, setIsSideBranch] = useState(step.isSideBranch)
  const [sideBranchName, setSideBranchName] = useState(step.sideBranchName ?? '')
  const [suggestedSupplyTypeId, setSuggestedSupplyTypeId] = useState<number | ''>(step.suggestedSupplyTypeId ?? '')
  const [suggestedQuantity, setSuggestedQuantity] = useState<string>(step.suggestedQuantity != null ? String(step.suggestedQuantity) : '')

  const save = () => {
    onUpdate({
      name, eventType: eventType || null, daysAfterPrevious: days || null,
      isOptional, isSideBranch, sideBranchName: isSideBranch ? sideBranchName : null,
      suggestedSupplyTypeId: eventType === 'APPLIED_SUPPLY' && suggestedSupplyTypeId !== '' ? Number(suggestedSupplyTypeId) : null,
      suggestedQuantity: eventType === 'APPLIED_SUPPLY' && suggestedQuantity !== '' ? Number(suggestedQuantity) : null,
    })
    setEditing(false)
  }

  return (
    <>
      <div className={`flex items-center gap-2 py-2 px-3 border border-divider rounded-lg ${step.isSideBranch ? 'ml-6 border-dashed' : ''}`}>
        <div className="flex flex-col gap-0.5 shrink-0">
          <button onClick={onMoveUp} disabled={isFirst} className="text-xs text-text-secondary disabled:opacity-30">&#x25B2;</button>
          <button onClick={onMoveDown} disabled={isLast} className="text-xs text-text-secondary disabled:opacity-30">&#x25BC;</button>
        </div>
        <div className="flex-1 min-w-0" onClick={() => setEditing(true)}>
          <p className="text-sm font-medium truncate cursor-pointer">{step.name}</p>
          <div className="flex flex-wrap gap-1 mt-0.5">
            {step.eventType && (
              <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{step.eventType}</span>
            )}
            {!step.eventType && (
              <span className="text-xs bg-surface text-text-secondary px-1.5 py-0.5 rounded">{t('workflows.noneEvent')}</span>
            )}
            {step.daysAfterPrevious != null && step.daysAfterPrevious > 0 && (
              <span className="text-xs text-text-secondary">+{step.daysAfterPrevious}d</span>
            )}
            {step.isOptional && (
              <span className="text-xs bg-warning/15 text-warning px-1.5 py-0.5 rounded">{t('workflows.optional')}</span>
            )}
            {step.isSideBranch && (
              <span className="text-xs bg-info/15 text-info px-1.5 py-0.5 rounded">{t('workflows.sideBranch')}: {step.sideBranchName}</span>
            )}
          </div>
        </div>
        <button onClick={onDelete} className="text-xs text-error px-1 shrink-0">{t('common.delete')}</button>
      </div>

      {editing && (
        <Dialog open={editing} title={step.name} onClose={() => setEditing(false)}>
          <div className="space-y-3">
            <div>
              <label className="field-label">{t('workflows.stepName')}</label>
              <input value={name} onChange={e => setName(e.target.value)} className="input w-full" autoFocus />
            </div>
            <div>
              <label className="field-label">{t('workflows.eventType')}</label>
              <select value={eventType} onChange={e => setEventType(e.target.value)} className="input w-full">
                {EVENT_TYPES.map(et => (
                  <option key={et} value={et}>{et || t('workflows.noneEvent')}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="field-label">{t('workflows.daysAfterPrevious')}</label>
              <input type="number" min={0} value={days} onChange={e => setDays(Number(e.target.value))} className="input w-full" />
            </div>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={isOptional} onChange={e => setIsOptional(e.target.checked)} />
              {t('workflows.optional')}
            </label>
            <label className="flex items-center gap-2 text-sm">
              <input type="checkbox" checked={isSideBranch} onChange={e => setIsSideBranch(e.target.checked)} />
              {t('workflows.sideBranch')}
            </label>
            {isSideBranch && (
              <div>
                <label className="field-label">{t('workflows.sideBranchName')}</label>
                <input value={sideBranchName} onChange={e => setSideBranchName(e.target.value)} className="input w-full" />
              </div>
            )}
            {eventType === 'APPLIED_SUPPLY' && (
              <>
                <div>
                  <label className="field-label">{t('supplyApplication.suggestedSupply')}</label>
                  <select
                    value={suggestedSupplyTypeId}
                    onChange={e => setSuggestedSupplyTypeId(e.target.value !== '' ? Number(e.target.value) : '')}
                    className="input w-full"
                  >
                    <option value="">{t('common.none')}</option>
                    {supplyTypes?.filter(st => st.category === 'FERTILIZER').map(st => (
                      <option key={st.id} value={st.id}>{st.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('supplyApplication.suggestedQuantity')}</label>
                  <input
                    type="number"
                    step="0.01"
                    value={suggestedQuantity}
                    onChange={e => setSuggestedQuantity(e.target.value)}
                    className="input w-full"
                  />
                </div>
              </>
            )}
            <div className="flex gap-2">
              <button className="btn-secondary flex-1" onClick={() => setEditing(false)}>{t('common.cancel')}</button>
              <button className="btn-primary flex-1" disabled={!name.trim()} onClick={save}>{t('common.save')}</button>
            </div>
          </div>
        </Dialog>
      )}
    </>
  )
}

export function WorkflowTemplateEdit() {
  const { id } = useParams<{ id: string }>()
  const templateId = Number(id)
  const { t } = useTranslation()
  const qc = useQueryClient()

  const [name, setName] = useState('')
  const [desc, setDesc] = useState('')
  const [nameInit, setNameInit] = useState(false)
  const [showAddStep, setShowAddStep] = useState(false)
  const [newStepName, setNewStepName] = useState('')

  const { data: template, error, isLoading, refetch } = useQuery({
    queryKey: ['workflow-template', templateId],
    queryFn: () => api.workflows.getTemplate(templateId),
  })

  const { data: supplyTypes } = useQuery({
    queryKey: ['supply-types'],
    queryFn: () => api.supplies.types(),
  })

  if (template && !nameInit) {
    setName(template.name)
    setDesc(template.description ?? '')
    setNameInit(true)
  }

  const updateTemplateMut = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.workflows.updateTemplate(templateId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['workflow-template', templateId] }); qc.invalidateQueries({ queryKey: ['workflow-templates'] }) },
  })

  const addStepMut = useMutation({
    mutationFn: (data: Record<string, unknown>) => api.workflows.addTemplateStep(templateId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workflow-template', templateId] })
      setShowAddStep(false)
      setNewStepName('')
    },
  })

  const updateStepMut = useMutation({
    mutationFn: ({ stepId, data }: { stepId: number; data: Record<string, unknown> }) => api.workflows.updateStep(stepId, data),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['workflow-template', templateId] }) },
  })

  const deleteStepMut = useMutation({
    mutationFn: (stepId: number) => api.workflows.deleteStep(stepId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['workflow-template', templateId] }) },
  })

  const moveStep = (stepId: number, direction: 'up' | 'down') => {
    if (!template) return
    const steps = [...template.steps].sort((a, b) => a.sortOrder - b.sortOrder)
    const idx = steps.findIndex(s => s.id === stepId)
    const swapIdx = direction === 'up' ? idx - 1 : idx + 1
    if (swapIdx < 0 || swapIdx >= steps.length) return
    updateStepMut.mutate({ stepId: steps[idx].id, data: { sortOrder: steps[swapIdx].sortOrder } })
    updateStepMut.mutate({ stepId: steps[swapIdx].id, data: { sortOrder: steps[idx].sortOrder } })
  }

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!template) return null

  const sortedSteps = [...template.steps].sort((a, b) => a.sortOrder - b.sortOrder)
  const breadcrumbs: BreadcrumbItem[] = [
    { label: t('workflows.title'), to: '/workflows' },
    { label: template.name, to: `/workflows/${templateId}/edit` },
  ]

  return (
    <div className="max-w-lg">
      <PageHeader title={t('workflows.editTemplate')} breadcrumbs={breadcrumbs} />

      <div className="px-4 space-y-4">
        {/* Name / description */}
        <section className="form-card">
          <div className="space-y-3">
            <div>
              <label className="field-label">{t('workflows.templateName')}</label>
              <input value={name} onChange={e => setName(e.target.value)} className="input w-full" />
            </div>
            <div>
              <label className="field-label">{t('workflows.description')}</label>
              <input value={desc} onChange={e => setDesc(e.target.value)} className="input w-full" />
            </div>
            <button
              className="btn-primary text-sm"
              disabled={!name.trim() || updateTemplateMut.isPending}
              onClick={() => updateTemplateMut.mutate({ name: name.trim(), description: desc.trim() || null })}
            >
              {updateTemplateMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </div>
        </section>

        {/* Steps */}
        <section className="form-card">
          <label className="field-label">{t('workflows.steps')}</label>
          <div className="space-y-2">
            {sortedSteps.length === 0 && (
              <p className="text-xs text-text-secondary py-1">{t('workflows.noSteps')}</p>
            )}
            {sortedSteps.map((step, idx) => (
              <StepRow
                key={step.id}
                step={step}
                onUpdate={(data) => updateStepMut.mutate({ stepId: step.id, data })}
                onDelete={() => deleteStepMut.mutate(step.id)}
                onMoveUp={() => moveStep(step.id, 'up')}
                onMoveDown={() => moveStep(step.id, 'down')}
                isFirst={idx === 0}
                isLast={idx === sortedSteps.length - 1}
                supplyTypes={supplyTypes}
              />
            ))}
          </div>
          <button className="btn-secondary text-sm mt-3" onClick={() => setShowAddStep(true)}>
            {t('workflows.addStep')}
          </button>
        </section>
      </div>

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
    </div>
  )
}
