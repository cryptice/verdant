import { useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type Species, type SpeciesExportEntry } from '../../api/client'
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import ErrorDisplay from '../../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'

export function SpeciesListPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [debouncedSearch, setDebouncedSearch] = useState('')
  const [previewSpecies, setPreviewSpecies] = useState<Species | null>(null)
  const [importStatus, setImportStatus] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)
  const [importing, setImporting] = useState(false)
  const [page, setPage] = useState(0)
  const importInputRef = useRef<HTMLInputElement>(null)
  const pageSize = 50
  const { t } = useTranslation()

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedSearch(search), 250)
    return () => clearTimeout(timer)
  }, [search])

  const { data: species, isLoading, error } = useQuery({
    queryKey: ['admin', 'species', debouncedSearch],
    queryFn: () => debouncedSearch ? api.admin.searchSpecies(debouncedSearch, 100) : api.admin.getSpecies(),
    placeholderData: (prev) => prev,
  })

  const handleCopy = (s: Species) => navigate(`/species/new?from=${s.id}`)

  const handleExport = async () => {
    setExporting(true)
    try {
      const data = await api.admin.exportSpecies()
      const json = JSON.stringify(data, null, 2)
      const blob = new Blob([json], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `verdant-species-${new Date().toISOString().slice(0, 10)}.json`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setImportStatus(t('species.exportFailed', { error: e instanceof Error ? e.message : 'Unknown error' }))
    } finally {
      setExporting(false)
    }
  }

  const handleImportFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImporting(true)
    setImportStatus(null)
    try {
      const text = await file.text()
      const entries: SpeciesExportEntry[] = JSON.parse(text)
      if (!Array.isArray(entries)) throw new Error('JSON must be an array')
      const result = await api.admin.importSpecies(entries)
      setImportStatus(t('species.importResult', { created: result.created, updated: result.updated, skipped: result.skipped }))
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
    } catch (err) {
      setImportStatus(t('species.importFailed', { error: err instanceof Error ? err.message : 'Unknown error' }))
    } finally {
      setImporting(false)
      e.target.value = ''
    }
  }

  const filtered = species
    ?.slice()
    .sort((a, b) => {
      const nameA = (a.commonNameSv || a.commonName).toLowerCase()
      const nameB = (b.commonNameSv || b.commonName).toLowerCase()
      if (nameA !== nameB) return nameA.localeCompare(nameB, 'sv')
      const varA = (a.variantNameSv || a.variantName || '').toLowerCase()
      const varB = (b.variantNameSv || b.variantName || '').toLowerCase()
      return varA.localeCompare(varB, 'sv')
    })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })} />

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-5 sm:mb-6">
        <div className="min-w-0">
          <h2 className="text-xl sm:text-2xl font-semibold text-[#37352F]">{t('species.title')}</h2>
          <p className="text-sm text-[#787774] mt-1">{t('species.count', { count: species?.length || 0 })}</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={handleExport}
            disabled={exporting}
            className="px-3 py-2 sm:py-1.5 border border-[#E9E9E7] text-[#37352F] rounded-md hover:bg-[#F0F0EE] transition-colors text-sm disabled:opacity-50"
          >
            {exporting ? t('species.exporting') : t('species.export')}
          </button>
          <button
            onClick={() => importInputRef.current?.click()}
            disabled={importing}
            className="px-3 py-2 sm:py-1.5 border border-[#E9E9E7] text-[#37352F] rounded-md hover:bg-[#F0F0EE] transition-colors text-sm disabled:opacity-50"
          >
            {importing ? t('species.importing') : t('species.import')}
          </button>
          <input ref={importInputRef} type="file" accept=".json" className="hidden" onChange={handleImportFile} />
          <button
            onClick={() => navigate('/species/new')}
            className="ml-auto sm:ml-0 px-3 py-2 sm:py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] transition-colors text-sm font-medium"
          >
            {t('common.new')}
          </button>
        </div>
      </div>

      {importStatus && (
        <div className={`mb-4 px-3 py-2.5 rounded-md text-sm ${
          importStatus.startsWith(t('species.importFailed', { error: '' }).split(':')[0]) || importStatus.startsWith(t('species.exportFailed', { error: '' }).split(':')[0])
            ? 'bg-[#FBE4E4] text-[#E03E3E]'
            : 'bg-[#DBEDDB] text-[#0F7B0F]'
        }`}>
          {importStatus}
          <button onClick={() => setImportStatus(null)} className="ml-3 text-current opacity-50 hover:opacity-100">&times;</button>
        </div>
      )}

      <div className="mb-4">
        <input
          type="text"
          placeholder={t('species.searchPlaceholder')}
          value={search}
          onChange={e => { setSearch(e.target.value); setPage(0) }}
          className="w-full max-w-md px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
        />
      </div>

      {/* Mobile card list */}
      <div className="md:hidden space-y-2">
        {filtered?.slice(page * pageSize, (page + 1) * pageSize).map((s: Species) => (
          <div
            key={s.id}
            className="border border-[#E9E9E7] rounded-lg p-3 bg-white active:bg-[#FBFBFA] transition-colors"
            onClick={() => navigate(`/species/${s.id}`)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => { if (e.key === 'Enter') navigate(`/species/${s.id}`) }}
          >
            <div className="flex items-start gap-2">
              <div className="flex-1 min-w-0">
                <div className="text-sm font-medium text-[#37352F]">
                  {s.commonNameSv || s.commonName}
                  {(s.variantNameSv || s.variantName) && <span className="text-[#787774] font-normal"> — {s.variantNameSv || s.variantName}</span>}
                </div>
                {s.scientificName && (
                  <div className="text-xs text-[#787774] italic mt-0.5 truncate">{s.scientificName}</div>
                )}
                <div className="flex flex-wrap items-center gap-2 mt-2">
                  <span className={`px-2 py-0.5 rounded text-[10px] font-medium ${
                    s.isSystem ? 'bg-[#D3E5EF] text-[#2B6CB0]' : 'bg-[#E9E9E7] text-[#787774]'
                  }`}>
                    {s.isSystem ? t('species.system') : t('species.user')}
                  </span>
                  {s.groups.length > 0 && (
                    <span className="text-xs text-[#787774] truncate">{s.groups.map(g => g.name).join(', ')}</span>
                  )}
                </div>
                {s.providers.length > 0 && (
                  <button
                    onClick={(e) => { e.stopPropagation(); setPreviewSpecies(s) }}
                    className="mt-2 text-xs text-[#2EAADC] hover:underline text-left truncate max-w-full"
                  >
                    {s.providers.map(p => p.providerName).join(', ')}
                  </button>
                )}
              </div>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); navigate(`/species/${s.id}`) }}
                aria-label={t('common.edit')}
                title={t('common.edit')}
                className="-mt-1 p-2 text-[#787774] hover:text-[#2EAADC] transition-colors"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                </svg>
              </button>
              <button
                type="button"
                onClick={(e) => { e.stopPropagation(); handleCopy(s) }}
                aria-label={t('species.copy')}
                title={t('species.copy')}
                className="-mr-1 -mt-1 p-2 text-[#787774] hover:text-[#2EAADC] transition-colors"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                </svg>
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Desktop table */}
      <div className="hidden md:block border border-[#E9E9E7] rounded-lg overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#E9E9E7] bg-[#FBFBFA]">
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('users.name')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('species.scientificName')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('species.group')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('species.type')}</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">{t('species.providersCol')}</th>
              <th className="w-10 px-2 py-2"></th>
            </tr>
          </thead>
          <tbody>
            {filtered?.slice(page * pageSize, (page + 1) * pageSize).map((s: Species) => (
              <tr key={s.id} className="border-b border-[#E9E9E7] last:border-0 hover:bg-[#FBFBFA] cursor-pointer transition-colors" onClick={() => navigate(`/species/${s.id}`)}>
                <td className="px-4 py-2.5">
                  <div className="text-sm font-medium text-[#37352F]">
                    {s.commonNameSv || s.commonName}
                    {(s.variantNameSv || s.variantName) && <span className="text-[#787774] font-normal"> — {s.variantNameSv || s.variantName}</span>}
                  </div>
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774] italic">{s.scientificName || '—'}</td>
                <td className="px-4 py-2.5 text-sm text-[#787774]">{s.groups.map(g => g.name).join(', ') || '—'}</td>
                <td className="px-4 py-2.5">
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                    s.isSystem ? 'bg-[#D3E5EF] text-[#2B6CB0]' : 'bg-[#E9E9E7] text-[#787774]'
                  }`}>
                    {s.isSystem ? t('species.system') : t('species.user')}
                  </span>
                </td>
                <td className="px-4 py-2.5 text-sm" onClick={e => e.stopPropagation()}>
                  {s.providers.length > 0 ? (
                    <button
                      onClick={() => setPreviewSpecies(s)}
                      className="text-[#2EAADC] hover:underline cursor-pointer"
                    >
                      {s.providers.map(p => p.providerName).join(', ')}
                    </button>
                  ) : (
                    <span className="text-[#A5A29C]">—</span>
                  )}
                </td>
                <td className="px-2 py-2.5 text-right" onClick={e => e.stopPropagation()}>
                  <div className="flex items-center justify-end gap-1">
                    <button
                      type="button"
                      onClick={() => navigate(`/species/${s.id}`)}
                      aria-label={t('common.edit')}
                      title={t('common.edit')}
                      className="p-1 text-[#787774] hover:text-[#2EAADC] transition-colors"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      type="button"
                      onClick={() => handleCopy(s)}
                      aria-label={t('species.copy')}
                      title={t('species.copy')}
                      className="p-1 text-[#787774] hover:text-[#2EAADC] transition-colors"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {filtered && Math.ceil(filtered.length / pageSize) > 1 && (
        <div className="flex items-center justify-between mt-3">
          <p className="text-xs text-[#787774]">
            {page * pageSize + 1}–{Math.min((page + 1) * pageSize, filtered.length)} {t('common.of')} {filtered.length}
          </p>
          <div className="flex items-center gap-1">
            <button
              onClick={() => setPage(page - 1)}
              disabled={page === 0}
              className="px-2.5 py-1 text-sm rounded-md border border-[#E9E9E7] text-[#787774] hover:bg-[#F0F0EE] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              ‹
            </button>
            <button
              onClick={() => setPage(page + 1)}
              disabled={page >= Math.ceil(filtered.length / pageSize) - 1}
              className="px-2.5 py-1 text-sm rounded-md border border-[#E9E9E7] text-[#787774] hover:bg-[#F0F0EE] disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
            >
              ›
            </button>
          </div>
        </div>
      )}

      {/* Provider Images Modal */}
      {previewSpecies && (
        <div className="fixed inset-0 bg-black/40 flex items-end sm:items-center justify-center z-50" onClick={() => setPreviewSpecies(null)}>
          <div
            className="bg-white sm:rounded-lg rounded-t-xl border-t sm:border border-[#E9E9E7] sm:max-w-4xl w-full sm:mx-4 max-h-[92vh] sm:max-h-[90vh] overflow-y-auto pb-[env(safe-area-inset-bottom)]"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-start justify-between gap-3 px-4 sm:px-6 py-3 sm:py-4 border-b border-[#E9E9E7] sticky top-0 bg-white z-10">
              <h3 className="text-base font-semibold text-[#37352F] min-w-0 break-words">
                {previewSpecies.commonNameSv || previewSpecies.commonName}
                {(previewSpecies.variantNameSv || previewSpecies.variantName) && <span className="text-[#787774] font-normal"> — {previewSpecies.variantNameSv || previewSpecies.variantName}</span>}
              </h3>
              <button
                onClick={() => setPreviewSpecies(null)}
                aria-label={t('common.cancel')}
                className="-mr-2 -mt-1 inline-flex items-center justify-center w-9 h-9 text-[#787774] hover:text-[#37352F] hover:bg-[#F0F0EE] rounded-md text-xl leading-none shrink-0"
              >&times;</button>
            </div>
            <div className="p-4 sm:p-6 space-y-6">
              {previewSpecies.providers.map(sp => (
                <div key={sp.id}>
                  <div className="flex flex-wrap items-center gap-2 mb-3">
                    <span className="text-sm font-medium text-[#37352F]">{sp.providerName}</span>
                    <span className="text-xs text-[#787774]">{sp.providerIdentifier}</span>
                    {sp.productUrl && (
                      <a href={sp.productUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-[#2EAADC] hover:underline sm:ml-auto">
                        {t('species.productPage')} &rarr;
                      </a>
                    )}
                  </div>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 sm:gap-4">
                    {sp.imageFrontUrl ? (
                      <img src={sp.imageFrontUrl} alt={t('species.front')} className="w-full rounded-md object-contain max-h-[60vh] sm:max-h-[500px]" />
                    ) : (
                      <div className="h-40 sm:h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">{t('species.noFrontImage')}</div>
                    )}
                    {sp.imageBackUrl ? (
                      <img src={sp.imageBackUrl} alt={t('species.back')} className="w-full rounded-md object-contain max-h-[60vh] sm:max-h-[500px]" />
                    ) : (
                      <div className="h-40 sm:h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">{t('species.noBackImage')}</div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
