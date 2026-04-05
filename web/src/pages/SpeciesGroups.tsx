import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'

export function SpeciesGroups() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [newName, setNewName] = useState('')

  const { data: groups } = useQuery({
    queryKey: ['species-groups'],
    queryFn: api.speciesGroups.list,
  })

  const createMut = useMutation({
    mutationFn: (name: string) => api.speciesGroups.create(name),
    onSuccess: (group) => {
      qc.invalidateQueries({ queryKey: ['species-groups'] })
      setNewName('')
      navigate(`/species-groups/${group.id}/edit`)
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.speciesGroups.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-groups'] })
      qc.invalidateQueries({ queryKey: ['species'] })
    },
  })

  return (
    <div>
      <PageHeader title={t('groups.title')} />

      <div className="px-4 space-y-3">
        {/* Create new group */}
        <div className="flex gap-2">
          <input
            value={newName}
            onChange={e => setNewName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter' && newName.trim()) createMut.mutate(newName.trim()) }}
            placeholder={t('groups.newGroupPlaceholder')}
            className="input flex-1"
          />
          <button
            onClick={() => newName.trim() && createMut.mutate(newName.trim())}
            disabled={!newName.trim() || createMut.isPending}
            className="btn-primary text-sm px-4"
          >
            {t('groups.create')}
          </button>
        </div>

        {/* Group list */}
        {groups?.map(group => (
          <div key={group.id} className="flex items-center gap-2 px-3 py-2.5 border border-divider rounded-xl bg-surface">
            <span className="flex-1 text-sm font-medium">{group.name}</span>
            <button
              onClick={() => navigate(`/species-groups/${group.id}/edit`)}
              className="text-xs text-text-secondary hover:text-text px-1"
            >
              {t('common.edit')}
            </button>
            <button
              onClick={() => deleteMut.mutate(group.id)}
              className="text-xs text-error px-1"
            >
              {t('common.delete')}
            </button>
          </div>
        ))}

        {groups?.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-8">{t('groups.noGroups')}</p>
        )}
      </div>
    </div>
  )
}
