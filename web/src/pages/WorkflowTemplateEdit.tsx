import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type WorkflowStepResponse, type SupplyTypeResponse } from '../api/client'
import { Masthead, Field } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

// Plant event types a workflow step can record (PlantEventType on the backend), in growing order.
const EVENT_TYPES = [
  '', 'SEEDED', 'POTTED_UP', 'PINCHED', 'MOVED', 'PLANTED_OUT', 'WATERED', 'WEEDED', 'APPLIED_SUPPLY',
  'DISBUDDED', 'BUDDING', 'FIRST_BLOOM', 'HARVESTED', 'LIFTED', 'DIVIDED', 'STORED', 'NOTE',
]

const GRID_COLUMNS = '52px minmax(0, 1fr) 64px 48px 180px 28px'

const headStyle: React.CSSProperties = {
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
}

const arrowStyle = (disabled: boolean): React.CSSProperties => ({
  background: 'transparent',
  border: 'none',
  color: 'var(--color-forest)',
  cursor: disabled ? 'default' : 'pointer',
  opacity: disabled ? 0.2 : 0.55,
  fontSize: 9,
  lineHeight: 1,
  padding: '2px 3px',
})

const tagStyle: React.CSSProperties = {
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.2,
  textTransform: 'uppercase',
  color: 'var(--color-accent)',
  border: '1px solid color-mix(in srgb, var(--color-accent) 40%, transparent)',
  padding: '1px 5px',
  whiteSpace: 'nowrap',
}

function eventTypeOptions(current: string | null | undefined): string[] {
  return current && !EVENT_TYPES.includes(current) ? [...EVENT_TYPES, current] : EVENT_TYPES
}

function StepRow({
  step, index, day, onUpdate, onDelete, onMoveUp, onMoveDown, isFirst, isLast, supplyTypes,
}: {
  step: WorkflowStepResponse
  index: number
  /** Days from the first step to this one along the main flow; null for side-branch steps. */
  day: number | null
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
  const [days, setDays] = useState(String(step.daysAfterPrevious ?? 0))
  const eventLabel = (et: string) => (et ? t(`eventType.${et}`, { defaultValue: et }) : t('workflows.noneEvent'))

  const commitDays = () => {
    const parsed = Number.parseInt(days, 10)
    if (Number.isNaN(parsed) || parsed < 0) {
      setDays(String(step.daysAfterPrevious ?? 0))
      return
    }
    setDays(String(parsed))
    if (parsed !== (step.daysAfterPrevious ?? 0)) onUpdate({ daysAfterPrevious: parsed })
  }

  return (
    <div style={{ borderBottom: '1px solid var(--color-divider)' }}>
      <div
        className="ledger-grid"
        data-mobile-hide="4 5"
        style={{
          '--mobile-cols': 'auto minmax(0, 1fr) 52px auto',
          display: 'grid',
          gridTemplateColumns: GRID_COLUMNS,
          gap: 16,
          padding: '12px 0',
          alignItems: 'center',
        } as React.CSSProperties}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
          <span style={{ display: 'flex', flexDirection: 'column' }}>
            <button type="button" aria-label={t('workflows.moveUp')} onClick={onMoveUp} disabled={isFirst} style={arrowStyle(isFirst)}>▲</button>
            <button type="button" aria-label={t('workflows.moveDown')} onClick={onMoveDown} disabled={isLast} style={arrowStyle(isLast)}>▼</button>
          </span>
          <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, color: 'var(--color-forest)' }}>
            {String(index + 1).padStart(2, '0')}
          </span>
        </div>

        <button
          type="button"
          onClick={() => setEditing(true)}
          style={{ background: 'transparent', border: 'none', padding: 0, textAlign: 'left', cursor: 'pointer', minWidth: 0, color: 'inherit' }}
        >
          <span style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
            <span style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 300 }}>{step.name}</span>
            {step.isOptional && <span style={tagStyle}>{t('workflows.optional')}</span>}
            {step.isSideBranch && (
              <span style={tagStyle}>{t('workflows.sideBranch')}{step.sideBranchName ? `: ${step.sideBranchName}` : ''}</span>
            )}
          </span>
          {step.description && (
            <span
              style={{
                display: 'block',
                marginTop: 2,
                fontSize: 13,
                color: 'var(--color-forest)',
                opacity: 0.75,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
            >
              {step.description}
            </span>
          )}
        </button>

        <input
          className="inline-input"
          inputMode="numeric"
          aria-label={`${t('workflows.daysAfterPrevious')}, ${step.name}`}
          value={days}
          onChange={(e) => setDays(e.target.value.replace(/\D/g, ''))}
          onBlur={commitDays}
          onKeyDown={(e) => { if (e.key === 'Enter') e.currentTarget.blur() }}
          style={{ textAlign: 'right', fontFamily: 'var(--font-display)', fontSize: 18, color: 'var(--color-mustard)' }}
        />

        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 12, color: 'var(--color-forest)', opacity: 0.7, textAlign: 'right' }}>
          {day ?? '–'}
        </span>

        <select
          className="inline-select"
          aria-label={`${t('workflows.eventType')}, ${step.name}`}
          value={step.eventType ?? ''}
          onChange={(e) => onUpdate({ eventType: e.target.value })}
          style={{ fontSize: 14 }}
        >
          {eventTypeOptions(step.eventType).map((et) => <option key={et} value={et}>{eventLabel(et)}</option>)}
        </select>

        <button
          type="button"
          aria-label={`${t('workflows.deleteStep')}, ${step.name}`}
          onClick={onDelete}
          style={{ background: 'transparent', border: 'none', color: 'var(--color-clay)', cursor: 'pointer', fontSize: 16, lineHeight: 1, padding: 0, opacity: 0.7 }}
        >
          ×
        </button>
      </div>

      {step.eventType === 'APPLIED_SUPPLY' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'minmax(0, 1fr) 120px', gap: 16, padding: '0 0 12px 68px', maxWidth: 560 }}>
          <label style={{ display: 'block' }}>
            <span style={{ ...headStyle, display: 'block', marginBottom: 2 }}>{t('supplyApplication.suggestedSupply')}</span>
            <select
              className="inline-select"
              value={step.suggestedSupplyTypeId ?? ''}
              onChange={(e) =>
                onUpdate({
                  suggestedSupplyTypeId: e.target.value ? Number(e.target.value) : null,
                  clearSuggestedSupply: !e.target.value,
                })
              }
              style={{ fontSize: 14 }}
            >
              <option value="">{t('common.none')}</option>
              {supplyTypes?.filter((st) => st.category === 'FERTILIZER').map((st) => (
                <option key={st.id} value={st.id}>{st.name}</option>
              ))}
            </select>
          </label>
          <label style={{ display: 'block' }}>
            <span style={{ ...headStyle, display: 'block', marginBottom: 2 }}>{t('supplyApplication.suggestedQuantity')}</span>
            <input
              className="inline-input"
              inputMode="decimal"
              defaultValue={step.suggestedQuantity != null ? String(step.suggestedQuantity) : ''}
              onBlur={(e) => onUpdate({ suggestedQuantity: e.target.value ? Number(e.target.value) : null })}
              style={{ fontSize: 14 }}
            />
          </label>
        </div>
      )}

      {editing && (
        <StepDialog step={step} supplyTypes={supplyTypes} onClose={() => setEditing(false)} onSave={(data) => { onUpdate(data); setEditing(false) }} />
      )}
    </div>
  )
}

function StepDialog({ step, supplyTypes, onClose, onSave }: {
  step: WorkflowStepResponse
  supplyTypes?: SupplyTypeResponse[]
  onClose: () => void
  onSave: (data: Record<string, unknown>) => void
}) {
  const { t } = useTranslation()
  const [name, setName] = useState(step.name)
  const [description, setDescription] = useState(step.description ?? '')
  const [eventType, setEventType] = useState(step.eventType ?? '')
  const [days, setDays] = useState(step.daysAfterPrevious ?? 0)
  const [isOptional, setIsOptional] = useState(step.isOptional)
  const [isSideBranch, setIsSideBranch] = useState(step.isSideBranch)
  const [sideBranchName, setSideBranchName] = useState(step.sideBranchName ?? '')
  const [suggestedSupplyTypeId, setSuggestedSupplyTypeId] = useState<number | ''>(step.suggestedSupplyTypeId ?? '')
  const [suggestedQuantity, setSuggestedQuantity] = useState<string>(step.suggestedQuantity != null ? String(step.suggestedQuantity) : '')

  const save = () => {
    const suggestSet = eventType === 'APPLIED_SUPPLY' && suggestedSupplyTypeId !== ''
    onSave({
      name: name.trim(), description: description.trim(), eventType, daysAfterPrevious: days,
      isOptional, isSideBranch, sideBranchName: isSideBranch ? sideBranchName : null,
      suggestedSupplyTypeId: suggestSet ? Number(suggestedSupplyTypeId) : null,
      suggestedQuantity: suggestSet && suggestedQuantity !== '' ? Number(suggestedQuantity) : null,
      clearSuggestedSupply: !suggestSet,
    })
  }

  return (
    <Dialog open title={step.name} onClose={onClose}>
      <div className="space-y-3">
        <div>
          <label className="field-label" htmlFor="step-name">{t('workflows.stepName')}</label>
          <input id="step-name" value={name} onChange={(e) => setName(e.target.value)} className="input w-full" autoFocus />
        </div>
        <div>
          <label className="field-label" htmlFor="step-description">{t('workflows.stepDescription')}</label>
          <textarea id="step-description" value={description} onChange={(e) => setDescription(e.target.value)} className="input w-full" rows={4} maxLength={2000} />
        </div>
        <div className="flex gap-3">
          <div style={{ width: 120 }}>
            <label className="field-label" htmlFor="step-days">{t('workflows.daysAfterPrevious')}</label>
            <input id="step-days" type="number" min={0} value={days} onChange={(e) => setDays(Math.max(0, Number(e.target.value)))} className="input w-full" />
          </div>
          <div className="flex-1">
            <label className="field-label" htmlFor="step-event">{t('workflows.eventType')}</label>
            <select id="step-event" value={eventType} onChange={(e) => setEventType(e.target.value)} className="input w-full">
              {eventTypeOptions(eventType).map((et) => (
                <option key={et} value={et}>{et ? t(`eventType.${et}`, { defaultValue: et }) : t('workflows.noneEvent')}</option>
              ))}
            </select>
          </div>
        </div>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={isOptional} onChange={(e) => setIsOptional(e.target.checked)} />
          {t('workflows.optional')}
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={isSideBranch} onChange={(e) => setIsSideBranch(e.target.checked)} />
          {t('workflows.sideBranch')}
        </label>
        {isSideBranch && (
          <div>
            <label className="field-label" htmlFor="step-branch">{t('workflows.sideBranchName')}</label>
            <input id="step-branch" value={sideBranchName} onChange={(e) => setSideBranchName(e.target.value)} className="input w-full" />
          </div>
        )}
        {eventType === 'APPLIED_SUPPLY' && (
          <>
            <div>
              <label className="field-label" htmlFor="step-supply">{t('supplyApplication.suggestedSupply')}</label>
              <select
                id="step-supply"
                value={suggestedSupplyTypeId}
                onChange={(e) => setSuggestedSupplyTypeId(e.target.value !== '' ? Number(e.target.value) : '')}
                className="input w-full"
              >
                <option value="">{t('common.none')}</option>
                {supplyTypes?.filter((st) => st.category === 'FERTILIZER').map((st) => (
                  <option key={st.id} value={st.id}>{st.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="field-label" htmlFor="step-quantity">{t('supplyApplication.suggestedQuantity')}</label>
              <input
                id="step-quantity"
                type="number"
                step="0.01"
                value={suggestedQuantity}
                onChange={(e) => setSuggestedQuantity(e.target.value)}
                className="input w-full"
              />
            </div>
          </>
        )}
        <div className="flex gap-2">
          <button className="btn-secondary flex-1" onClick={onClose}>{t('common.cancel')}</button>
          <button className="btn-primary flex-1" disabled={!name.trim()} onClick={save}>{t('common.save')}</button>
        </div>
      </div>
    </Dialog>
  )
}

/** Days from the first step to each step along the main flow; side-branch steps run on their own clock. */
export function mainFlowDays(steps: WorkflowStepResponse[]): (number | null)[] {
  let total = 0
  return steps.map((step, index) => {
    if (step.isSideBranch) return null
    if (index > 0) total += step.daysAfterPrevious ?? 0
    return total
  })
}

export function WorkflowTemplateEdit() {
  const { id } = useParams<{ id: string }>()
  const templateId = Number(id)
  const { t } = useTranslation()
  const qc = useQueryClient()
  const navigate = useNavigate()

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
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workflow-template', templateId] })
      qc.invalidateQueries({ queryKey: ['workflow-templates'] })
    },
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
    mutationFn: ({ stepId, data }: { stepId: number; data: Record<string, unknown> }) =>
      api.workflows.updateStep(stepId, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workflow-template', templateId] })
    },
  })

  const deleteStepMut = useMutation({
    mutationFn: (stepId: number) => api.workflows.deleteStep(stepId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workflow-template', templateId] })
    },
  })

  const moveStep = (stepId: number, direction: 'up' | 'down') => {
    if (!template) return
    const steps = [...template.steps].sort((a, b) => a.sortOrder - b.sortOrder)
    const idx = steps.findIndex((s) => s.id === stepId)
    const swapIdx = direction === 'up' ? idx - 1 : idx + 1
    if (swapIdx < 0 || swapIdx >= steps.length) return
    updateStepMut.mutate({ stepId: steps[idx].id, data: { sortOrder: steps[swapIdx].sortOrder } })
    updateStepMut.mutate({ stepId: steps[swapIdx].id, data: { sortOrder: steps[idx].sortOrder } })
  }

  if (isLoading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
        <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
      </div>
    )
  }
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!template) return null

  const sortedSteps = [...template.steps].sort((a, b) => a.sortOrder - b.sortOrder)
  const days = mainFlowDays(sortedSteps)

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.workflows')} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>
              {template.name || t('workflows.newTitle')}
            </span>
          </span>
        }
        center={t('workflows.masthead.center')}
      />

      <div className="page-body-tight">
        {/* Template-level fields */}
        <div style={{ display: 'grid', gap: 20, maxWidth: 900 }}>
          <Field
            label={t('common.nameLabel')}
            editable
            value={name}
            onChange={setName}
          />
          <label style={{ display: 'block' }}>
            <span style={{ ...headStyle, display: 'block', marginBottom: 4 }}>{t('common.descriptionLabel')}</span>
            <textarea
              className="inline-input"
              value={desc}
              onChange={(e) => setDesc(e.target.value)}
              rows={2}
              maxLength={2000}
              style={{ fontSize: 15, resize: 'vertical' }}
            />
          </label>
        </div>

        {/* § Steg. heading */}
        <div style={{ marginTop: 36, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12 }}>
          <h2
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 30,
              fontWeight: 300,
              margin: 0,
              whiteSpace: 'nowrap',
            }}
          >
            {t('workflows.steps')}<span style={{ color: 'var(--color-accent)' }}>.</span>
          </h2>
          <button
            className="btn-secondary"
            onClick={() => setShowAddStep(true)}
            style={{ whiteSpace: 'nowrap' }}
          >
            + {t('workflows.addStep')}
          </button>
        </div>

        {/* Step table */}
        <div style={{ marginTop: 14 }}>
          {sortedSteps.length === 0 ? (
            <p
              style={{
                fontFamily: 'var(--font-display)',
                fontStyle: 'italic',
                fontSize: 16,
                color: 'var(--color-forest)',
                opacity: 0.6,
                padding: '14px 0',
              }}
            >
              {t('workflows.noSteps')}
            </p>
          ) : (
            <div
              className="ledger-head"
              style={{
                display: 'grid',
                gridTemplateColumns: GRID_COLUMNS,
                gap: 16,
                paddingBottom: 6,
                borderBottom: '1px solid var(--color-ink)',
              }}
            >
              <span style={headStyle}>#</span>
              <span style={headStyle}>{t('workflows.stepColumn')}</span>
              <span style={{ ...headStyle, textAlign: 'right' }} title={t('workflows.daysAfterPrevious')}>{t('workflows.daysColumn')}</span>
              <span style={{ ...headStyle, textAlign: 'right' }}>{t('workflows.dayColumn')}</span>
              <span style={headStyle}>{t('workflows.eventColumn')}</span>
              <span />
            </div>
          )}
          {sortedSteps.map((step, idx) => (
            <StepRow
              key={`${step.id}-${step.daysAfterPrevious ?? 0}`}
              step={step}
              index={idx}
              day={days[idx]}
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
      </div>

      {/* Sticky footer */}
      <div
        style={{
          position: 'sticky',
          bottom: 0,
          background: 'var(--color-cream)',
          borderTop: '1px solid var(--color-ink)',
          padding: '14px 40px',
          display: 'flex',
          justifyContent: 'flex-end',
          gap: 10,
        }}
      >
        <button className="btn-secondary" onClick={() => navigate('/workflows')}>
          {t('common.cancel')}
        </button>
        <button
          className="btn-primary"
          disabled={!name.trim() || updateTemplateMut.isPending}
          onClick={() =>
            updateTemplateMut.mutate({ name: name.trim(), description: desc.trim() || null })
          }
        >
          {updateTemplateMut.isPending ? t('common.saving') : t('common.save')}
        </button>
      </div>

      {/* Add step dialog */}
      {showAddStep && (
        <Dialog open={showAddStep} title={t('workflows.addStep')} onClose={() => setShowAddStep(false)}>
          <div className="space-y-3">
            <div>
              <label className="field-label">{t('workflows.stepName')}</label>
              <input
                value={newStepName}
                onChange={(e) => setNewStepName(e.target.value)}
                className="input w-full"
                autoFocus
              />
            </div>
            <div className="flex gap-2">
              <button className="btn-secondary flex-1" onClick={() => setShowAddStep(false)}>
                {t('common.cancel')}
              </button>
              <button
                className="btn-primary flex-1"
                disabled={!newStepName.trim() || addStepMut.isPending}
                onClick={() =>
                  addStepMut.mutate({ name: newStepName.trim(), sortOrder: sortedSteps.length })
                }
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
