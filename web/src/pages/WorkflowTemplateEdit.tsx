import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type WorkflowStepResponse, type SupplyTypeResponse } from '../api/client'
import { Masthead, Field, Rule } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const EVENT_TYPES = [
  '', 'SEEDED', 'POTTED_UP', 'PLANTED_OUT', 'HARVESTED',
  'PINCHED', 'FERTILIZED', 'WATERED', 'PRUNED', 'NOTE', 'OTHER', 'APPLIED_SUPPLY',
]

const selectStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  backgroundColor: 'transparent',
  border: 'none',
  borderBottom: '1px solid var(--color-ink)',
  borderRadius: 0,
  padding: '4px 0',
  fontFamily: 'var(--font-display)',
  fontSize: 20,
  fontWeight: 300,
  color: 'var(--color-ink)',
  outline: 'none',
}

const selectLabelStyle: React.CSSProperties = {
  display: 'block',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  marginBottom: 4,
}

function StepRow({
  step, index, onUpdate, onDelete, onMoveUp, onMoveDown, isFirst, isLast, supplyTypes,
}: {
  step: WorkflowStepResponse
  index: number
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
    const suggestSet = eventType === 'APPLIED_SUPPLY' && suggestedSupplyTypeId !== ''
    onUpdate({
      name, eventType: eventType || null, daysAfterPrevious: days || null,
      isOptional, isSideBranch, sideBranchName: isSideBranch ? sideBranchName : null,
      suggestedSupplyTypeId: suggestSet ? Number(suggestedSupplyTypeId) : null,
      suggestedQuantity: suggestSet && suggestedQuantity !== '' ? Number(suggestedQuantity) : null,
      clearSuggestedSupply: !suggestSet,
    })
    setEditing(false)
  }

  return (
    <>
      <div style={{ borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '40px 1.5fr 100px 140px 40px',
            gap: 18,
            padding: '14px 0',
            alignItems: 'center',
          }}
        >
          {/* № */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
            <button
              onClick={onMoveUp}
              disabled={isFirst}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--color-forest)',
                cursor: isFirst ? 'default' : 'pointer',
                opacity: isFirst ? 0.25 : 0.6,
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                padding: '1px 4px',
              }}
            >▲</button>
            <span
              style={{
                fontFamily: 'var(--font-display)',
                fontStyle: 'italic',
                fontSize: 18,
                color: 'var(--color-forest)',
                lineHeight: 1,
              }}
            >
              {String(index + 1).padStart(2, '0')}
            </span>
            <button
              onClick={onMoveDown}
              disabled={isLast}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--color-forest)',
                cursor: isLast ? 'default' : 'pointer',
                opacity: isLast ? 0.25 : 0.6,
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                padding: '1px 4px',
              }}
            >▼</button>
          </div>

          {/* Name field — click to open edit dialog */}
          <div onClick={() => setEditing(true)} style={{ cursor: 'pointer' }}>
            <Field
              label={t('workflows.stepName')}
              value={step.name}
            />
          </div>

          {/* Days after previous */}
          <div onClick={() => setEditing(true)} style={{ cursor: 'pointer' }}>
            <Field
              label={t('workflows.daysAfterPrevious')}
              value={step.daysAfterPrevious != null ? String(step.daysAfterPrevious) : ''}
              accent="mustard"
            />
          </div>

          {/* Event type */}
          <div>
            <span style={selectLabelStyle}>{t('workflows.eventType')}</span>
            <select
              value={step.eventType ?? ''}
              onChange={(e) => {
                onUpdate({ eventType: e.target.value || null })
              }}
              style={selectStyle}
            >
              {EVENT_TYPES.map((et) => (
                <option key={et} value={et}>
                  {et ? t(`eventType.${et}`, { defaultValue: et }) : t('workflows.noneEvent')}
                </option>
              ))}
            </select>
          </div>

          {/* Delete */}
          <button
            onClick={onDelete}
            style={{
              background: 'transparent',
              border: 'none',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              padding: 0,
            }}
          >
            ↵
          </button>
        </div>

        {/* APPLIED_SUPPLY sub-grid */}
        {step.eventType === 'APPLIED_SUPPLY' && (
          <div
            style={{
              gridColumn: '1 / -1',
              paddingBottom: 14,
              paddingLeft: 58,
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: 18,
            }}
          >
            <div>
              <span style={selectLabelStyle}>{t('supplyApplication.suggestedSupply')}</span>
              <select
                value={step.suggestedSupplyTypeId ?? ''}
                onChange={(e) =>
                  onUpdate({
                    suggestedSupplyTypeId: e.target.value ? Number(e.target.value) : null,
                    clearSuggestedSupply: !e.target.value,
                  })
                }
                style={selectStyle}
              >
                <option value="">{t('common.none')}</option>
                {supplyTypes?.filter((st) => st.category === 'FERTILIZER').map((st) => (
                  <option key={st.id} value={st.id}>{st.name}</option>
                ))}
              </select>
            </div>
            <Field
              label={t('supplyApplication.suggestedQuantity')}
              editable
              value={step.suggestedQuantity != null ? String(step.suggestedQuantity) : ''}
              onChange={(v) =>
                onUpdate({
                  suggestedQuantity: v ? Number(v) : null,
                })
              }
            />
          </div>
        )}
      </div>

      {/* Full edit dialog for optional/side-branch and other advanced fields */}
      {editing && (
        <Dialog open={editing} title={step.name} onClose={() => setEditing(false)}>
          <div className="space-y-3">
            <div>
              <label className="field-label">{t('workflows.stepName')}</label>
              <input value={name} onChange={(e) => setName(e.target.value)} className="input w-full" autoFocus />
            </div>
            <div>
              <label className="field-label">{t('workflows.eventType')}</label>
              <select value={eventType} onChange={(e) => setEventType(e.target.value)} className="input w-full">
                {EVENT_TYPES.map((et) => (
                  <option key={et} value={et}>{et || t('workflows.noneEvent')}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="field-label">{t('workflows.daysAfterPrevious')}</label>
              <input type="number" min={0} value={days} onChange={(e) => setDays(Number(e.target.value))} className="input w-full" />
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
                <label className="field-label">{t('workflows.sideBranchName')}</label>
                <input value={sideBranchName} onChange={(e) => setSideBranchName(e.target.value)} className="input w-full" />
              </div>
            )}
            {eventType === 'APPLIED_SUPPLY' && (
              <>
                <div>
                  <label className="field-label">{t('supplyApplication.suggestedSupply')}</label>
                  <select
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
                  <label className="field-label">{t('supplyApplication.suggestedQuantity')}</label>
                  <input
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

      <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
        {/* Template-level fields */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
          <Field
            label={t('common.nameLabel')}
            editable
            value={name}
            onChange={setName}
          />
          <Field
            label={t('common.descriptionLabel')}
            editable
            value={desc}
            onChange={setDesc}
          />
        </div>

        {/* § Steg. heading */}
        <div style={{ marginTop: 40, display: 'flex', alignItems: 'center', gap: 12 }}>
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
          <Rule inline variant="ink" />
          <button
            className="btn-secondary"
            onClick={() => setShowAddStep(true)}
            style={{ whiteSpace: 'nowrap' }}
          >
            + {t('workflows.addStep')}
          </button>
        </div>

        {/* Step rows */}
        <div style={{ marginTop: 14 }}>
          {sortedSteps.length === 0 && (
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
          )}
          {sortedSteps.map((step, idx) => (
            <StepRow
              key={step.id}
              step={step}
              index={idx}
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
