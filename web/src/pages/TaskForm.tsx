import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const activityTypes = ['SOW', 'POT_UP', 'PLANT', 'HARVEST', 'RECOVER', 'DISCARD']

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
}

export function TaskForm() {
  const { taskId } = useParams<{ taskId: string }>()
  const isEdit = !!taskId
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const { completeStep } = useOnboarding()

  const { data: existing } = useQuery({
    queryKey: ['task', Number(taskId)],
    queryFn: () => api.tasks.get(Number(taskId)),
    enabled: isEdit,
  })

  const { data: presetSpecies } = useQuery({
    queryKey: ['species-by-id', existing?.speciesId],
    queryFn: () => api.species.search(String(existing!.speciesId), 1).then(list => list.find(s => s.id === existing!.speciesId) ?? null),
    enabled: !!existing?.speciesId,
  })

  const [selectedSpecies, setSelectedSpecies] = useState<SpeciesResponse | null>(null)
  const [selectedGroupId, setSelectedGroupId] = useState<number | null>(null)
  const [selectedGroupName, setSelectedGroupName] = useState<string>('')
  const [groupSpecies, setGroupSpecies] = useState<SpeciesResponse[]>([])
  const [checkedSpeciesIds, setCheckedSpeciesIds] = useState<Set<number>>(new Set())
  const [activityType, setActivityType] = useState('SOW')
  const [deadline, setDeadline] = useState('')
  const [targetCount, setTargetCount] = useState('')
  const [notes, setNotes] = useState('')

  const { data: fetchedGroupSpecies } = useQuery({
    queryKey: ['species-by-group', selectedGroupId],
    queryFn: () => api.species.byGroup(selectedGroupId!),
    enabled: !!selectedGroupId,
  })

  useEffect(() => {
    if (fetchedGroupSpecies) {
      setGroupSpecies(fetchedGroupSpecies)
      setCheckedSpeciesIds(new Set(fetchedGroupSpecies.map(s => s.id)))
    }
  }, [fetchedGroupSpecies])

  useEffect(() => {
    if (existing) {
      setActivityType(existing.activityType)
      setDeadline(existing.deadline)
      setTargetCount(String(existing.targetCount))
      setNotes(existing.notes ?? '')
    }
  }, [existing])

  useEffect(() => {
    if (presetSpecies && !selectedSpecies) setSelectedSpecies(presetSpecies)
  }, [presetSpecies, selectedSpecies])

  const handleGroupSelect = (groupId: number, groupName: string) => {
    setSelectedSpecies(null)
    setSelectedGroupId(groupId)
    setSelectedGroupName(groupName)
  }

  const handleSpeciesSelect = (species: SpeciesResponse | null) => {
    setSelectedSpecies(species)
    setSelectedGroupId(null)
    setSelectedGroupName('')
    setGroupSpecies([])
    setCheckedSpeciesIds(new Set())
  }

  const toggleSpecies = (id: number) => {
    setCheckedSpeciesIds(prev => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const isGroupMode = selectedGroupId !== null
  const hasSelection = isGroupMode ? checkedSpeciesIds.size > 0 : !!selectedSpecies
  const valid = hasSelection && deadline && Number(targetCount) > 0

  const createMut = useMutation({
    mutationFn: () => {
      if (isGroupMode) {
        return api.tasks.create({
          speciesGroupId: selectedGroupId!,
          speciesIds: Array.from(checkedSpeciesIds),
          activityType,
          deadline,
          targetCount: Number(targetCount),
          notes: notes || undefined,
        })
      }
      return api.tasks.create({
        speciesId: selectedSpecies!.id,
        activityType,
        deadline,
        targetCount: Number(targetCount),
        notes: notes || undefined,
      })
    },
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); completeStep('create_task'); navigate('/tasks', { replace: true }) },
  })

  const updateMut = useMutation({
    mutationFn: () => api.tasks.update(Number(taskId), {
      activityType, deadline, targetCount: Number(targetCount), notes: notes || undefined,
    }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['tasks'] }); navigate('/tasks', { replace: true }) },
  })

  const mutation = isEdit ? updateMut : createMut

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('nav.tasks'), to: '/tasks' }]

  return (
    <div className="max-w-lg">
      <PageHeader title={isEdit ? t('tasks.editTaskTitle') : t('tasks.newTaskTitle')} breadcrumbs={breadcrumbs} />
      <OnboardingHint />
      <div data-onboarding="task-form" className="form-card">
        <div>
          <label className="field-label">{t('tasks.activityLabel')}</label>
          <select value={activityType} onChange={e => setActivityType(e.target.value)} className="input w-full">
            {activityTypes.map(tp => (
              <option key={tp} value={tp}>{t(`activityType.${tp}`, { defaultValue: tp.replace(/_/g, ' ') })}</option>
            ))}
          </select>
        </div>

        <div>
          <label className="field-label">{t('common.speciesLabel')}</label>
          {isEdit && existing ? (
            <div>
              {existing.originGroupName && (
                <div className="flex items-center gap-1 mb-1">
                  <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{t('common.group')}</span>
                  <span className="text-sm font-medium">{existing.originGroupName}</span>
                </div>
              )}
              <div className="text-sm text-text-secondary">
                {existing.acceptableSpecies.map(s => s.speciesName).join(', ')}
              </div>
            </div>
          ) : isGroupMode ? (
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{t('common.group')}</span>
                <span className="text-sm font-medium">{selectedGroupName}</span>
                <button onClick={() => handleSpeciesSelect(null)} className="text-xs text-text-secondary ml-auto">{t('common.clear')}</button>
              </div>
              <div className="border border-divider rounded-xl p-2 max-h-48 overflow-y-auto space-y-1">
                {groupSpecies.map(s => (
                  <label key={s.id} className="flex items-center gap-2 px-2 py-1.5 rounded-lg hover:bg-surface cursor-pointer text-sm">
                    <input
                      type="checkbox"
                      checked={checkedSpeciesIds.has(s.id)}
                      onChange={() => toggleSpecies(s.id)}
                      className="rounded"
                    />
                    {speciesLabel(s, i18n.language)}
                  </label>
                ))}
                {groupSpecies.length === 0 && (
                  <p className="text-xs text-text-secondary px-2 py-1">{t('tasks.loadingGroupSpecies')}</p>
                )}
              </div>
            </div>
          ) : (
            <SpeciesAutocomplete
              value={selectedSpecies}
              onChange={handleSpeciesSelect}
              onGroupSelect={handleGroupSelect}
              showGroups
            />
          )}
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('tasks.deadlineLabel')}</label>
            <input type="date" value={deadline} onChange={e => setDeadline(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('tasks.targetCountLabel')}</label>
            <input type="number" value={targetCount} onChange={e => setTargetCount(e.target.value)} placeholder="e.g. 10" className="input w-full" />
          </div>
        </div>

        <div>
          <label className="field-label">{t('common.notesLabel')}</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} placeholder={t('common.optional')} rows={2} className="input w-full" />
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
