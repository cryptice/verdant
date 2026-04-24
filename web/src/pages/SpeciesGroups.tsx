import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { Dialog } from '../components/Dialog'

export function SpeciesGroups() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [newName, setNewName] = useState('')
  const [showCreate, setShowCreate] = useState(false)

  const { data: groups = [] } = useQuery({
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
      setShowCreate(false)
      navigate(`/species-groups/${group.id}/edit`)
    },
  })

  const handleCreate = () => {
    if (newName.trim()) createMut.mutate(newName.trim())
  }

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
      <Masthead
        left={t('nav.speciesGroups')}
        center="— Gruppliggaren —"
        right={
          <button onClick={() => setShowCreate(true)} className="btn-primary">
            {t('groups.create')}
          </button>
        }
      />

      <div className="page-body">
        <Ledger
          columns={[
            {
              key: 'id',
              label: '№',
              width: '60px',
              render: (_g, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-forest)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('groups.nameLabel'),
              width: '1.5fr',
              render: (g) => (
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{g.name}</span>
              ),
            },
            {
              key: 'count',
              label: t('species.title'),
              width: '120px',
              align: 'right',
              render: (g) => (
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>
                  {speciesByGroup.get(g.id)?.length ?? '—'}
                </span>
              ),
            },
            {
              key: 'goto',
              label: '',
              width: '40px',
              align: 'right',
              render: () => (
                <span style={{ color: 'var(--color-forest)', fontFamily: 'var(--font-mono)' }}>→</span>
              ),
            },
          ]}
          rows={groups}
          rowKey={(g) => g.id}
          onRowClick={(g) => navigate(`/species-groups/${g.id}/edit`)}
        />
      </div>

      <Dialog
        open={showCreate}
        onClose={() => { setShowCreate(false); setNewName('') }}
        title={t('groups.create')}
        actions={
          <>
            <button onClick={() => { setShowCreate(false); setNewName('') }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
            <button
              onClick={handleCreate}
              disabled={!newName.trim() || createMut.isPending}
              className="btn-primary text-sm"
            >
              {createMut.isPending ? t('common.creating') : t('groups.create')}
            </button>
          </>
        }
      >
        <div>
          <label className="field-label">{t('groups.nameLabel')}</label>
          <input
            value={newName}
            onChange={e => setNewName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleCreate() }}
            placeholder={t('groups.newGroupPlaceholder')}
            className="input w-full"
            autoFocus
          />
        </div>
      </Dialog>
    </div>
  )
}
