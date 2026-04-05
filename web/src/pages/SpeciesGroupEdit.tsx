import { useState, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import type { BreadcrumbItem } from '../components/Breadcrumb'

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
}

export function SpeciesGroupEdit() {
  const { id } = useParams<{ id: string }>()
  const groupId = Number(id)
  const navigate = useNavigate()
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()

  const { data: groups } = useQuery({
    queryKey: ['species-groups'],
    queryFn: api.speciesGroups.list,
  })
  const group = groups?.find(g => g.id === groupId)

  const [name, setName] = useState('')
  const [nameInitialized, setNameInitialized] = useState(false)
  if (group && !nameInitialized) {
    setName(group.name)
    setNameInitialized(true)
  }

  const { data: members, error, isLoading, refetch } = useQuery({
    queryKey: ['group-members', groupId],
    queryFn: () => api.speciesGroups.members(groupId),
  })

  const memberIds = useMemo(() => new Set(members?.map(s => s.id) ?? []), [members])

  const renameMut = useMutation({
    mutationFn: (newName: string) => api.speciesGroups.update(groupId, newName),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species-groups'] }) },
  })

  const addSpeciesMut = useMutation({
    mutationFn: (speciesId: number) => api.speciesGroups.addSpecies(groupId, speciesId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['group-members', groupId] })
      qc.invalidateQueries({ queryKey: ['species'] })
    },
  })

  const removeSpeciesMut = useMutation({
    mutationFn: (speciesId: number) => api.speciesGroups.removeSpecies(groupId, speciesId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['group-members', groupId] })
      qc.invalidateQueries({ queryKey: ['species'] })
    },
  })

  const deleteMut = useMutation({
    mutationFn: () => api.speciesGroups.delete(groupId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-groups'] })
      qc.invalidateQueries({ queryKey: ['species'] })
      navigate('/species-groups', { replace: true })
    },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('groups.title'), to: '/species-groups' }]

  return (
    <div className="max-w-lg">
      <PageHeader title={group?.name ?? ''} breadcrumbs={breadcrumbs} />

      <div className="px-4 space-y-4">
        {/* Group name */}
        <section className="form-card">
          <label className="field-label">{t('groups.nameLabel')}</label>
          <div className="flex gap-2">
            <input
              value={name}
              onChange={e => setName(e.target.value)}
              onKeyDown={e => { if (e.key === 'Enter' && name.trim()) renameMut.mutate(name.trim()) }}
              className="input flex-1"
            />
            <button
              onClick={() => name.trim() && renameMut.mutate(name.trim())}
              disabled={!name.trim() || name === group?.name || renameMut.isPending}
              className="btn-primary text-sm px-4"
            >
              {renameMut.isPending ? t('common.saving') : t('common.save')}
            </button>
          </div>
        </section>

        {/* Members */}
        <section className="form-card">
          <label className="field-label">{t('groups.speciesLabel')}</label>
          <div className="space-y-1 mb-3">
            {members?.map(s => (
              <div key={s.id} className="flex items-center justify-between text-sm py-1.5 px-2 rounded-lg hover:bg-surface">
                <span>{speciesLabel(s, i18n.language)}</span>
                <button
                  onClick={() => removeSpeciesMut.mutate(s.id)}
                  className="text-xs text-error"
                >
                  {t('groups.remove')}
                </button>
              </div>
            ))}
            {members?.length === 0 && (
              <p className="text-xs text-text-secondary py-1">{t('groups.empty')}</p>
            )}
          </div>
          <div className="max-w-xs">
            <SpeciesAutocomplete
              value={null}
              onChange={s => { if (s) addSpeciesMut.mutate(s.id) }}
              placeholder={t('groups.addSpeciesPlaceholder')}
              keepSearchOnSelect
              excludeIds={memberIds}
            />
          </div>
        </section>

        {/* Delete */}
        <button
          onClick={() => deleteMut.mutate()}
          disabled={deleteMut.isPending}
          className="text-sm text-error"
        >
          {deleteMut.isPending ? t('common.deleting') : t('groups.deleteGroup')}
        </button>
      </div>
    </div>
  )
}
