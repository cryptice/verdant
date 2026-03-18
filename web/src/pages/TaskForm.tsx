import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import type { BreadcrumbItem } from '../components/Breadcrumb'

const activityTypes = ['SOW', 'POT_UP', 'PLANT', 'HARVEST', 'RECOVER', 'DISCARD']

export function TaskForm() {
  const { taskId } = useParams<{ taskId: string }>()
  const isEdit = !!taskId
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()

  const { data: species } = useQuery({ queryKey: ['species'], queryFn: api.species.list })
  const { data: existing } = useQuery({
    queryKey: ['task', Number(taskId)],
    queryFn: () => api.tasks.get(Number(taskId)),
    enabled: isEdit,
  })

  const [speciesId, setSpeciesId] = useState('')
  const [activityType, setActivityType] = useState('SOW')
  const [deadline, setDeadline] = useState('')
  const [targetCount, setTargetCount] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (existing) {
      setSpeciesId(String(existing.speciesId))
      setActivityType(existing.activityType)
      setDeadline(existing.deadline)
      setTargetCount(String(existing.targetCount))
      setNotes(existing.notes ?? '')
    }
  }, [existing])

  const createMut = useMutation({
    mutationFn: () => api.tasks.create({
      speciesId: Number(speciesId), activityType, deadline, targetCount: Number(targetCount), notes: notes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); navigate('/tasks', { replace: true }) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.tasks.update(Number(taskId), {
      speciesId: Number(speciesId), activityType, deadline, targetCount: Number(targetCount), notes: notes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); navigate('/tasks', { replace: true }) },
  })

  const mutation = isEdit ? updateMut : createMut
  const valid = speciesId && deadline && Number(targetCount) > 0

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('nav.tasks'), to: '/tasks' }]

  return (
    <div className="max-w-lg">
      <PageHeader title={isEdit ? t('tasks.editTaskTitle') : t('tasks.newTaskTitle')} breadcrumbs={breadcrumbs} />
      <div className="form-card">
        <div>
          <label className="field-label">{t('common.speciesLabel')}</label>
          <select value={speciesId} onChange={e => setSpeciesId(e.target.value)} className="input">
            <option value="">{t('common.selectSpecies')}</option>
            {species?.map(s => (
              <option key={s.id} value={s.id}>{s.commonName}{s.variantName ? ` — ${s.variantName}` : ''}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="field-label">{t('tasks.activityLabel')}</label>
          <select value={activityType} onChange={e => setActivityType(e.target.value)} className="input">
            {activityTypes.map(tp => (
              <option key={tp} value={tp}>{t(`activityType.${tp}`, { defaultValue: tp.replace(/_/g, ' ') })}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="field-label">{t('tasks.deadlineLabel')}</label>
          <input type="date" value={deadline} onChange={e => setDeadline(e.target.value)} className="input" />
        </div>

        <div>
          <label className="field-label">{t('tasks.targetCountLabel')}</label>
          <input type="number" value={targetCount} onChange={e => setTargetCount(e.target.value)} placeholder="e.g. 10" className="input" />
        </div>

        <div>
          <label className="field-label">{t('common.notesLabel')}</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
        </div>
      </div>

      {mutation.error && <p className="text-error text-sm mt-3">{mutation.error instanceof Error ? mutation.error.message : String(mutation.error)}</p>}
      <div className="mt-4 flex justify-end">
        <button onClick={() => mutation.mutate()} disabled={!valid || mutation.isPending} className="btn-primary">
          {mutation.isPending ? t('common.saving') : isEdit ? t('tasks.updateTask') : t('tasks.createTask')}
        </button>
      </div>
    </div>
  )
}
