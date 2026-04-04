import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
}

export function SpeciesGroups() {
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()
  const [newName, setNewName] = useState('')
  const [expandedGroupId, setExpandedGroupId] = useState<number | null>(null)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [editingName, setEditingName] = useState('')

  const { data: groups } = useQuery({
    queryKey: ['species-groups'],
    queryFn: api.speciesGroups.list,
  })

  const { data: members } = useQuery({
    queryKey: ['group-members', expandedGroupId],
    queryFn: () => api.speciesGroups.members(expandedGroupId!),
    enabled: !!expandedGroupId,
  })

  const createMut = useMutation({
    mutationFn: (name: string) => api.speciesGroups.create(name),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species-groups'] }); setNewName('') },
  })

  const renameMut = useMutation({
    mutationFn: ({ id, name }: { id: number; name: string }) => api.speciesGroups.update(id, name),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['species-groups'] }); setEditingId(null) },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.speciesGroups.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species-groups'] })
      qc.invalidateQueries({ queryKey: ['species'] })
      if (expandedGroupId === deleteMut.variables) setExpandedGroupId(null)
    },
  })

  const addSpeciesMut = useMutation({
    mutationFn: ({ groupId, speciesId }: { groupId: number; speciesId: number }) =>
      api.speciesGroups.addSpecies(groupId, speciesId),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['group-members', vars.groupId] })
      qc.invalidateQueries({ queryKey: ['species'] })
    },
  })

  const memberIds = useMemo(() => new Set(members?.map(s => s.id) ?? []), [members])

  const removeSpeciesMut = useMutation({
    mutationFn: ({ groupId, speciesId }: { groupId: number; speciesId: number }) =>
      api.speciesGroups.removeSpecies(groupId, speciesId),
    onSuccess: (_, vars) => {
      qc.invalidateQueries({ queryKey: ['group-members', vars.groupId] })
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
          <div key={group.id} className="border border-divider rounded-xl">
            {/* Group header */}
            <div className="flex items-center gap-2 px-3 py-2 bg-surface">
              {editingId === group.id ? (
                <input
                  autoFocus
                  value={editingName}
                  onChange={e => setEditingName(e.target.value)}
                  onKeyDown={e => {
                    if (e.key === 'Enter' && editingName.trim()) renameMut.mutate({ id: group.id, name: editingName.trim() })
                    if (e.key === 'Escape') setEditingId(null)
                  }}
                  onBlur={() => setEditingId(null)}
                  className="input text-sm flex-1"
                />
              ) : (
                <>
                  <button
                    onClick={() => setExpandedGroupId(expandedGroupId === group.id ? null : group.id)}
                    className="flex-1 text-left text-sm font-medium"
                  >
                    <span className="mr-1 text-text-secondary">{expandedGroupId === group.id ? '▾' : '▸'}</span>
                    {group.name}
                  </button>
                  <button
                    onClick={() => { setEditingId(group.id); setEditingName(group.name) }}
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
                </>
              )}
            </div>

            {/* Expanded: members + add */}
            {expandedGroupId === group.id && (
              <div className="px-3 py-2 space-y-2">
                {members?.map(s => (
                  <div key={s.id} className="flex items-center justify-between text-sm py-1">
                    <span>{speciesLabel(s, i18n.language)}</span>
                    <button
                      onClick={() => removeSpeciesMut.mutate({ groupId: group.id, speciesId: s.id })}
                      className="text-xs text-error"
                    >
                      {t('groups.remove')}
                    </button>
                  </div>
                ))}
                {members?.length === 0 && (
                  <p className="text-xs text-text-secondary">{t('groups.empty')}</p>
                )}
                <div className="pt-1">
                  <SpeciesAutocomplete
                    value={null}
                    onChange={s => {
                      if (s) addSpeciesMut.mutate({ groupId: group.id, speciesId: s.id })
                    }}
                    placeholder={t('groups.addSpeciesPlaceholder')}
                    keepSearchOnSelect
                    excludeIds={memberIds}
                  />
                </div>
              </div>
            )}
          </div>
        ))}

        {groups?.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-8">{t('groups.noGroups')}</p>
        )}
      </div>
    </div>
  )
}
