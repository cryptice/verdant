import { useState, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead, Field, Rule } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'

function speciesMainName(s: SpeciesResponse, lang: string) {
  return lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
}

function speciesVariant(s: SpeciesResponse, lang: string) {
  return lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
}

function groupByMainName(species: SpeciesResponse[], lang: string) {
  const grouped = new Map<string, SpeciesResponse[]>()
  for (const s of species) {
    const main = speciesMainName(s, lang)
    const list = grouped.get(main) ?? []
    list.push(s)
    grouped.set(main, list)
  }
  return grouped
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
  const group = groups?.find((g) => g.id === groupId)

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

  const memberIds = useMemo(() => new Set(members?.map((s) => s.id) ?? []), [members])

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

  if (isLoading) return (
    <div style={{ display: 'flex', justifyContent: 'center', padding: 64 }}>
      <div style={{ width: 32, height: 32, border: '2px solid var(--color-ink)', borderTopColor: 'transparent', borderRadius: '50%', animation: 'spin 0.8s linear infinite' }} />
    </div>
  )
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.speciesGroups')} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>{group?.name ?? t('speciesGroups.newTitle')}</span>
          </span>
        }
        center={t('form.masthead.center')}
      />

      <div className="page-body-tight">
        {/* Group name */}
        <div style={{ display: 'flex', gap: 12, alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <Field
              label={t('groups.nameLabel')}
              editable
              value={name}
              onChange={setName}
            />
          </div>
          <button
            onClick={() => name.trim() && renameMut.mutate(name.trim())}
            disabled={!name.trim() || name === group?.name || renameMut.isPending}
            className="btn-primary"
            style={{ marginBottom: 1 }}
          >
            {renameMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </div>

        {/* § Medlemmar */}
        <div style={{ marginTop: 28, fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
          § {t('speciesGroups.members')}
        </div>
        <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>

        <div style={{ marginTop: 14 }}>
          {members && members.length > 0 && Array.from(groupByMainName(members, i18n.language).entries()).map(([main, species]) => (
            <div key={main}>
              <p style={{ fontFamily: 'var(--font-display)', fontSize: 16, fontWeight: 300, padding: '8px 0 4px', color: 'var(--color-forest)' }}>
                {main}
              </p>
              {species.map((s) => {
                const variant = speciesVariant(s, i18n.language)
                return (
                  <div
                    key={s.id}
                    style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)' }}
                  >
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 300, color: 'var(--color-forest)', fontStyle: 'italic' }}>
                      {variant ?? '—'}
                    </span>
                    <button
                      onClick={() => removeSpeciesMut.mutate(s.id)}
                      style={{ background: 'transparent', border: 'none', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-error)', cursor: 'pointer' }}
                    >
                      {t('groups.remove')}
                    </button>
                  </div>
                )
              })}
            </div>
          ))}
          {members?.length === 0 && (
            <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, color: 'var(--color-forest)', opacity: 0.6, paddingTop: 8 }}>
              {t('groups.empty')}
            </p>
          )}
        </div>

        {/* Add species */}
        <div style={{ marginTop: 20, maxWidth: 360 }}>
          <SpeciesAutocomplete
            value={null}
            onChange={(s) => { if (s) addSpeciesMut.mutate(s.id) }}
            placeholder={t('groups.addSpeciesPlaceholder')}
            keepSearchOnSelect
            excludeIds={memberIds}
          />
        </div>

        {/* Delete group */}
        <div style={{ marginTop: 60 }}>
          <button
            onClick={() => deleteMut.mutate()}
            disabled={deleteMut.isPending}
            style={{ background: 'transparent', border: 'none', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-error)', cursor: 'pointer', padding: 0 }}
          >
            {deleteMut.isPending ? t('common.deleting') : t('groups.deleteGroup')}
          </button>
        </div>
      </div>

      {/* Sticky footer */}
      <div className="sticky-footer">
        <button className="btn-secondary" onClick={() => navigate('/species-groups')}>
          {t('common.cancel')}
        </button>
        <button
          className="btn-primary"
          onClick={() => name.trim() && renameMut.mutate(name.trim())}
          disabled={!name.trim() || name === group?.name || renameMut.isPending}
        >
          {renameMut.isPending ? t('common.saving') : t('common.save')}
        </button>
      </div>
    </div>
  )
}
