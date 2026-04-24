import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead, Rule } from '../components/faltet'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const activityTypes = ['SOW', 'SOAK', 'POT_UP', 'PLANT', 'HARVEST', 'RECOVER', 'DISCARD']

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
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

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
      {children}
    </div>
  )
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
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      completeStep('create_task')
      navigate('/tasks', { replace: true })
    },
  })

  const updateMut = useMutation({
    mutationFn: () => api.tasks.update(Number(taskId), {
      activityType, deadline, targetCount: Number(targetCount), notes: notes || undefined,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['tasks'] })
      navigate('/tasks', { replace: true })
    },
  })

  const mutation = isEdit ? updateMut : createMut

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.tasks')} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>
              {isEdit ? t('tasks.editTitle') : t('tasks.newTitle')}
            </span>
          </span>
        }
        center={t('form.masthead.center')}
      />

      <div className="page-body-tight">
        <OnboardingHint />

        {/* § Detaljer */}
        <SectionLabel>§ {t('form.sections.details')}</SectionLabel>
        <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>

        <div style={{ marginTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
          {/* Activity type */}
          <label style={{ display: 'block' }}>
            <span style={selectLabelStyle}>{t('tasks.form.type')}</span>
            <select value={activityType} onChange={e => setActivityType(e.target.value)} style={selectStyle}>
              {activityTypes.map(tp => (
                <option key={tp} value={tp}>{t(`activityType.${tp}`, { defaultValue: tp.replace(/_/g, ' ') })}</option>
              ))}
            </select>
          </label>

          {/* Deadline */}
          <div>
            <span style={selectLabelStyle}>{t('tasks.form.deadline')}</span>
            <input
              type="date"
              value={deadline}
              onChange={e => setDeadline(e.target.value)}
              style={{ ...selectStyle, fontFamily: 'var(--font-mono)', fontSize: 14 }}
            />
          </div>

          {/* Target count */}
          <div>
            <span style={selectLabelStyle}>{t('tasks.targetCountLabel')}</span>
            <input
              type="number"
              value={targetCount}
              onChange={e => setTargetCount(e.target.value)}
              style={{ ...selectStyle, fontFamily: 'var(--font-mono)', fontSize: 14 }}
            />
          </div>
        </div>

        {/* § Schema */}
        <div style={{ marginTop: 28 }}>
          <SectionLabel>§ {t('form.sections.scheduling')}</SectionLabel>
          <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>
        </div>

        <div style={{ marginTop: 14 }}>
          {/* Species picker */}
          <div data-onboarding="task-form">
            <span style={selectLabelStyle}>{t('common.speciesLabel')}</span>
            {isEdit && existing ? (
              <div style={{ paddingTop: 6 }}>
                {existing.originGroupName && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
                    <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', background: 'var(--color-paper)', padding: '2px 6px', border: '1px solid var(--color-ink)' }}>
                      {t('common.group')}
                    </span>
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{existing.originGroupName}</span>
                  </div>
                )}
                <div style={{ fontFamily: 'var(--font-display)', fontSize: 16, color: 'var(--color-forest)' }}>
                  {existing.acceptableSpecies.map(s => s.speciesName).join(', ')}
                </div>
              </div>
            ) : isGroupMode ? (
              <div style={{ marginTop: 6 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 10 }}>
                  <span style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', background: 'var(--color-paper)', padding: '2px 6px', border: '1px solid var(--color-ink)' }}>
                    {t('common.group')}
                  </span>
                  <span style={{ fontFamily: 'var(--font-display)', fontSize: 18 }}>{selectedGroupName}</span>
                  <button
                    onClick={() => handleSpeciesSelect(null)}
                    style={{ marginLeft: 'auto', background: 'transparent', border: 'none', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-accent)', cursor: 'pointer' }}
                  >
                    {t('common.clear')}
                  </button>
                </div>
                <div style={{ border: '1px solid var(--color-ink)', background: 'var(--color-paper)', padding: '8px 12px', maxHeight: 192, overflowY: 'auto' }}>
                  {groupSpecies.map(s => (
                    <label
                      key={s.id}
                      style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '6px 0', cursor: 'pointer', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 12%, transparent)' }}
                    >
                      <input
                        type="checkbox"
                        checked={checkedSpeciesIds.has(s.id)}
                        onChange={() => toggleSpecies(s.id)}
                      />
                      <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{speciesLabel(s, i18n.language)}</span>
                    </label>
                  ))}
                  {groupSpecies.length === 0 && (
                    <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-forest)' }}>
                      {t('tasks.loadingGroupSpecies')}
                    </p>
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
        </div>

        {/* Notes */}
        <div style={{ marginTop: 28, border: '1px solid var(--color-ink)', background: 'var(--color-paper)', padding: '14px 16px' }}>
          <div style={selectLabelStyle}>{t('common.notesLabel')}</div>
          <textarea
            value={notes}
            onChange={e => setNotes(e.target.value)}
            placeholder={t('common.optional')}
            rows={3}
            style={{ width: '100%', minHeight: 80, background: 'transparent', border: 'none', outline: 'none', fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, resize: 'vertical', boxSizing: 'border-box' }}
          />
        </div>

        {mutation.error && (
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-error)', marginTop: 12 }}>
            {mutation.error instanceof Error ? mutation.error.message : String(mutation.error)}
          </p>
        )}
      </div>

      {/* Sticky footer */}
      <div style={{ position: 'sticky', bottom: 0, background: 'var(--color-cream)', borderTop: '1px solid var(--color-ink)', padding: '14px 40px', display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
        <button className="btn-secondary" onClick={() => navigate('/tasks')}>
          {t('common.cancel')}
        </button>
        <button
          className="btn-primary"
          onClick={() => mutation.mutate()}
          disabled={!valid || mutation.isPending}
        >
          {mutation.isPending ? t('common.saving') : isEdit ? t('tasks.updateTask') : t('tasks.createTask')}
        </button>
      </div>
    </div>
  )
}
