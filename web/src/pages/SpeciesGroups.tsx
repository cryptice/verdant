import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { PageHeader } from '../components/PageHeader'

function groupByMainName(species: SpeciesResponse[], lang: string) {
  const grouped = new Map<string, string[]>()
  for (const s of species) {
    const main = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
    const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
    const variants = grouped.get(main) ?? []
    if (variant) variants.push(variant)
    grouped.set(main, variants)
  }
  return grouped
}

export function SpeciesGroups() {
  const { t, i18n } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [newName, setNewName] = useState('')

  const { data: groups } = useQuery({
    queryKey: ['species-groups'],
    queryFn: api.speciesGroups.list,
  })

  // Fetch all species to resolve group memberships for display
  const { data: allSpecies } = useQuery({
    queryKey: ['species'],
    queryFn: api.species.list,
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

  // Build a map of groupId -> species list from the species data
  const speciesByGroup = new Map<number, SpeciesResponse[]>()
  if (allSpecies) {
    for (const s of allSpecies) {
      for (const g of s.groups) {
        const list = speciesByGroup.get(g.id) ?? []
        list.push(s)
        speciesByGroup.set(g.id, list)
      }
    }
  }

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
        {groups?.map(group => {
          const members = speciesByGroup.get(group.id) ?? []
          const grouped = groupByMainName(members, i18n.language)

          return (
            <div key={group.id} className="border border-divider rounded-xl">
              <div className="flex items-center gap-2 px-3 py-2.5 bg-surface rounded-t-xl">
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
              {members.length > 0 && (
                <div className="px-3 py-2 text-xs text-text-secondary space-y-0.5">
                  {Array.from(grouped.entries()).map(([main, variants]) => (
                    <p key={main}>{main}{variants.length > 0 ? `: ${variants.join(', ')}` : ''}</p>
                  ))}
                </div>
              )}
            </div>
          )
        })}

        {groups?.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-8">{t('groups.noGroups')}</p>
        )}
      </div>
    </div>
  )
}
