import { useState, useEffect, useRef } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'

interface Props {
  value: SpeciesResponse | null
  onChange: (species: SpeciesResponse | null) => void
  onGroupSelect?: (groupId: number, groupName: string) => void
  placeholder?: string
  showGroups?: boolean
  keepSearchOnSelect?: boolean
  excludeIds?: Set<number>
}

function speciesLabel(s: SpeciesResponse, lang: string) {
  const name = lang === 'sv' ? (s.commonNameSv ?? s.commonName) : s.commonName
  const variant = lang === 'sv' ? (s.variantNameSv ?? s.variantName) : s.variantName
  return variant ? `${name} — ${variant}` : name
}

export function SpeciesAutocomplete({ value, onChange, onGroupSelect, placeholder, showGroups = false, keepSearchOnSelect = false, excludeIds }: Props) {
  const { t, i18n } = useTranslation()
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!search) { setDebouncedSearch(''); return }
    const timer = setTimeout(() => setDebouncedSearch(search), 250)
    return () => clearTimeout(timer)
  }, [search])

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  const { data: results, isFetching } = useQuery({
    queryKey: ['species-search', debouncedSearch],
    queryFn: () => api.species.search(debouncedSearch),
    enabled: debouncedSearch.length >= 1,
    staleTime: 60_000,
  })

  const { data: groups } = useQuery({
    queryKey: ['species-groups'],
    queryFn: api.speciesGroups.list,
    enabled: showGroups,
    staleTime: 60_000,
  })

  const filteredResults = excludeIds && results
    ? results.filter(s => !excludeIds.has(s.id))
    : results

  const filteredGroups = showGroups && debouncedSearch && groups
    ? groups.filter(g => g.name.toLowerCase().includes(debouncedSearch.toLowerCase()))
    : []

  const displayValue = value ? speciesLabel(value, i18n.language) : ''

  return (
    <div className="relative" ref={ref}>
      <input
        value={open ? search : displayValue}
        onChange={e => { setSearch(e.target.value); onChange(null); setOpen(true) }}
        onFocus={() => { if (!value) setOpen(true) }}
        placeholder={placeholder ?? t('common.searchSpecies')}
        className="input w-full"
      />
      {open && debouncedSearch && (
        <div className="absolute z-10 left-0 right-0 mt-1 border border-divider rounded-xl bg-bg shadow-md max-h-48 overflow-y-auto">
          {isFetching && (!filteredResults || filteredResults.length === 0) && filteredGroups.length === 0 && (
            <p className="px-3 py-2 text-sm text-text-secondary">...</p>
          )}
          {filteredGroups.map(g => (
            <button
              key={`group-${g.id}`}
              onClick={() => {
                onGroupSelect?.(g.id, g.name)
                setSearch('')
                setOpen(false)
              }}
              className="w-full text-left px-3 py-2 text-sm hover:bg-surface transition-colors flex items-center gap-2"
            >
              <span className="text-xs bg-accent/15 text-accent px-1.5 py-0.5 rounded">{t('common.group')}</span>
              {g.name}
            </button>
          ))}
          {filteredGroups.length > 0 && filteredResults && filteredResults.length > 0 && (
            <div className="border-t border-divider" />
          )}
          {filteredResults?.map(s => (
            <button
              key={s.id}
              onClick={() => {
                onChange(s)
                if (!keepSearchOnSelect) { setSearch(''); setOpen(false) }
              }}
              className="w-full text-left px-3 py-2 text-sm hover:bg-surface transition-colors"
            >
              {speciesLabel(s, i18n.language)}
            </button>
          ))}
          {filteredResults && filteredResults.length === 0 && filteredGroups.length === 0 && !isFetching && (
            <p className="px-3 py-2 text-sm text-text-secondary">{t('species.noSpeciesFoundDropdown')}</p>
          )}
        </div>
      )}
    </div>
  )
}
