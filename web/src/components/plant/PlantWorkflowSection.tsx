import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type PlantWorkflowStepResponse } from '../../api/client'
import { Dialog } from '../Dialog'

/**
 * Per-plant workflow editor. Lives on the PlantDetail page. Steps were
 * cloned from the species on plant creation and can now be customised
 * independently (rename, delete, mark complete, add ad-hoc steps).
 *
 * If new steps appear on the species later, "Synka" re-pulls them
 * without disturbing existing local edits or completions.
 */
export function PlantWorkflowSection({ plantId }: { plantId: number }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const [editing, setEditing] = useState<PlantWorkflowStepResponse | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null)

  const { data: progress } = useQuery({
    queryKey: ['plant-workflow', plantId],
    queryFn: () => api.workflows.getPlantProgress(plantId),
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['plant-workflow', plantId] })

  const completeMut = useMutation({
    mutationFn: (stepId: number) => api.workflows.completePlantStep(stepId),
    onSuccess: invalidate,
  })

  const deleteMut = useMutation({
    mutationFn: (stepId: number) => api.workflows.deletePlantStep(stepId),
    onSuccess: () => { invalidate(); setConfirmDelete(null) },
  })

  const resyncMut = useMutation({
    mutationFn: () => api.workflows.resyncPlantFromSpecies(plantId),
    onSuccess: invalidate,
  })

  if (!progress) return null

  const completedSet = new Set(progress.completedStepIds)
  const sorted = [...progress.steps].sort((a, b) => a.sortOrder - b.sortOrder)
  const main = sorted.filter((s) => !s.isSideBranch)
  const sideByName = new Map<string, PlantWorkflowStepResponse[]>()
  for (const s of sorted.filter((s) => s.isSideBranch && s.sideBranchName)) {
    const list = sideByName.get(s.sideBranchName!) ?? []
    list.push(s)
    sideByName.set(s.sideBranchName!, list)
  }

  return (
    <>
      <div style={{ marginTop: 40, display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', gap: 12 }}>
        <h2
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 30,
            fontWeight: 300,
            margin: 0,
            fontVariationSettings: '"SOFT" 100, "opsz" 144',
          }}
        >
          {t('workflows.title')}<span style={{ color: 'var(--color-accent)' }}>.</span>
        </h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <button
            onClick={() => resyncMut.mutate()}
            disabled={resyncMut.isPending}
            className="btn-secondary"
            style={{ whiteSpace: 'nowrap' }}
          >
            {resyncMut.isPending ? '…' : t('workflows.syncFromSpecies')}
          </button>
          <button onClick={() => setShowAdd(true)} className="btn-secondary">
            + {t('workflows.addStep')}
          </button>
        </div>
      </div>

      {sorted.length === 0 ? (
        <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', margin: '12px 0' }}>
          {t('workflows.progress.empty')}
        </p>
      ) : (
        <>
          <StepTable
            steps={main}
            completedSet={completedSet}
            currentStepId={progress.currentStepId ?? null}
            onComplete={(id) => completeMut.mutate(id)}
            onEdit={setEditing}
            onDelete={(id) => setConfirmDelete(id)}
          />

          {Array.from(sideByName.entries()).map(([branchName, branchSteps]) => (
            <div key={branchName} style={{ marginTop: 28 }}>
              <div
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 9,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-forest)',
                  opacity: 0.7,
                  marginBottom: 8,
                }}
              >
                {t('workflows.sideBranch')}: {branchName}
              </div>
              <StepTable
                steps={branchSteps}
                completedSet={completedSet}
                currentStepId={null}
                onComplete={(id) => completeMut.mutate(id)}
                onEdit={setEditing}
                onDelete={(id) => setConfirmDelete(id)}
              />
            </div>
          ))}
        </>
      )}

      {(showAdd || editing) && (
        <StepEditorDialog
          plantId={plantId}
          step={editing}
          existingCount={sorted.length}
          onClose={() => { setShowAdd(false); setEditing(null) }}
          onSaved={() => { invalidate(); setShowAdd(false); setEditing(null) }}
        />
      )}

      {confirmDelete != null && (
        <Dialog
          open={true}
          title={t('workflows.deleteStep')}
          onClose={() => setConfirmDelete(null)}
          actions={
            <>
              <button className="btn-secondary" onClick={() => setConfirmDelete(null)}>{t('common.cancel')}</button>
              <button
                className="btn-primary"
                style={{ background: 'var(--color-accent)', borderColor: 'var(--color-accent)' }}
                disabled={deleteMut.isPending}
                onClick={() => deleteMut.mutate(confirmDelete)}
              >
                {deleteMut.isPending ? '…' : t('common.delete')}
              </button>
            </>
          }
        >
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic' }}>
            {t('workflows.deleteStepConfirm')}
          </p>
        </Dialog>
      )}
    </>
  )
}

function StepTable({
  steps,
  completedSet,
  currentStepId,
  onComplete,
  onEdit,
  onDelete,
}: {
  steps: PlantWorkflowStepResponse[]
  completedSet: Set<number>
  currentStepId: number | null
  onComplete: (id: number) => void
  onEdit: (step: PlantWorkflowStepResponse) => void
  onDelete: (id: number) => void
}) {
  const { t } = useTranslation()

  return (
    <div>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: '36px 1fr 90px 110px 110px',
          gap: 18,
          padding: '10px 0',
          borderBottom: '1px solid var(--color-ink)',
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
        }}
      >
        <span>№</span>
        <span>{t('workflows.stepName')}</span>
        <span>{t('workflows.daysAfterPrevious')}</span>
        <span>{t('workflows.eventType')}</span>
        <span style={{ textAlign: 'right' }}>{t('common.actions', { defaultValue: '' })}</span>
      </div>
      {steps.map((step, i) => {
        const isComplete = completedSet.has(step.id)
        const isCurrent = currentStepId === step.id
        return (
          <div
            key={step.id}
            style={{
              display: 'grid',
              gridTemplateColumns: '36px 1fr 90px 110px 110px',
              gap: 18,
              padding: '12px 0',
              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              alignItems: 'center',
              opacity: isComplete ? 0.55 : 1,
            }}
          >
            <span
              style={{
                fontFamily: 'var(--font-display)',
                fontStyle: 'italic',
                fontSize: 20,
                color: isComplete ? 'var(--color-sage)' : isCurrent ? 'var(--color-accent)' : 'var(--color-forest)',
              }}
            >
              {String(i + 1).padStart(2, '0')}
            </span>
            <span>
              <span
                style={{
                  fontFamily: 'var(--font-display)',
                  fontSize: 18,
                  textDecoration: isComplete ? 'line-through' : 'none',
                }}
              >
                {step.name}
              </span>
              {step.isOptional && (
                <span
                  style={{
                    marginLeft: 8,
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.2,
                    textTransform: 'uppercase',
                    color: 'var(--color-forest)',
                    opacity: 0.6,
                  }}
                >
                  {t('workflows.optional')}
                </span>
              )}
              {step.speciesStepId == null && (
                <span
                  style={{
                    marginLeft: 8,
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.2,
                    textTransform: 'uppercase',
                    color: 'var(--color-accent)',
                    opacity: 0.7,
                  }}
                  title={t('workflows.customStepHint')}
                >
                  ★
                </span>
              )}
            </span>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, color: 'var(--color-forest)' }}>
              {step.daysAfterPrevious != null ? `+${step.daysAfterPrevious}` : ''}
            </span>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, color: 'var(--color-forest)' }}>
              {step.eventType ?? ''}
            </span>
            <span style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
              {!isComplete && (
                <button
                  onClick={() => onComplete(step.id)}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    padding: 0,
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-accent)',
                    cursor: 'pointer',
                  }}
                >
                  ✓ {t('workflows.complete')}
                </button>
              )}
              <button
                onClick={() => onEdit(step)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  padding: 0,
                  fontFamily: 'var(--font-mono)',
                  fontSize: 9,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-forest)',
                  cursor: 'pointer',
                }}
              >
                {t('common.edit')}
              </button>
              <button
                onClick={() => onDelete(step.id)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  padding: 0,
                  fontFamily: 'var(--font-mono)',
                  fontSize: 9,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-accent)',
                  cursor: 'pointer',
                }}
              >
                {t('common.delete')}
              </button>
            </span>
          </div>
        )
      })}
    </div>
  )
}

function StepEditorDialog({
  plantId,
  step,
  existingCount,
  onClose,
  onSaved,
}: {
  plantId: number
  step: PlantWorkflowStepResponse | null
  existingCount: number
  onClose: () => void
  onSaved: () => void
}) {
  const { t } = useTranslation()
  const isEdit = step != null
  const [name, setName] = useState(step?.name ?? '')
  const [description, setDescription] = useState(step?.description ?? '')
  const [eventType, setEventType] = useState(step?.eventType ?? '')
  const [daysAfter, setDaysAfter] = useState(step?.daysAfterPrevious?.toString() ?? '')
  const [isOptional, setIsOptional] = useState(step?.isOptional ?? false)

  const saveMut = useMutation({
    mutationFn: () => {
      const body: Record<string, unknown> = {
        name: name.trim(),
        description: description.trim() || undefined,
        eventType: eventType || undefined,
        daysAfterPrevious: daysAfter ? Number(daysAfter) : undefined,
        isOptional,
      }
      if (isEdit) {
        return api.workflows.updatePlantStep(step!.id, body)
      }
      return api.workflows.addPlantStep(plantId, { ...body, sortOrder: existingCount })
    },
    onSuccess: onSaved,
  })

  const isDirty =
    name !== (step?.name ?? '') ||
    description !== (step?.description ?? '') ||
    eventType !== (step?.eventType ?? '') ||
    daysAfter !== (step?.daysAfterPrevious?.toString() ?? '') ||
    isOptional !== (step?.isOptional ?? false)

  return (
    <Dialog
      open={true}
      title={isEdit ? t('workflows.editStep') : t('workflows.addStep')}
      onClose={onClose}
      isDirty={isDirty}
      actions={
        <>
          <button className="btn-secondary" onClick={onClose}>{t('common.cancel')}</button>
          <button
            className="btn-primary"
            disabled={!name.trim() || saveMut.isPending}
            onClick={() => saveMut.mutate()}
          >
            {saveMut.isPending ? '…' : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label className="field-label">{t('workflows.stepName')} *</label>
          <input value={name} onChange={(e) => setName(e.target.value)} className="input w-full mt-1" autoFocus />
        </div>
        <div>
          <label className="field-label">{t('workflows.description')}</label>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={2} className="input w-full mt-1" />
        </div>
        <div>
          <label className="field-label">{t('workflows.eventType')}</label>
          <input
            value={eventType}
            onChange={(e) => setEventType(e.target.value.toUpperCase())}
            className="input w-full mt-1"
            placeholder="SEEDED, POTTED_UP, …"
          />
        </div>
        <div>
          <label className="field-label">{t('workflows.daysAfterPrevious')}</label>
          <input
            type="number"
            value={daysAfter}
            onChange={(e) => setDaysAfter(e.target.value)}
            className="input w-full mt-1"
          />
        </div>
        <label style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <input type="checkbox" checked={isOptional} onChange={(e) => setIsOptional(e.target.checked)} />
          <span>{t('workflows.optional')}</span>
        </label>
      </div>
    </Dialog>
  )
}
