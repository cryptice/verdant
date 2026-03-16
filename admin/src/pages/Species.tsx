import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type Species, type UpdateSpeciesRequest, type CreateSpeciesRequest, type SpeciesPhoto, type SpeciesExportEntry, type Provider, type SpeciesProvider, type AddSpeciesProviderRequest } from '../api/client'
import { useState, useRef, useCallback } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import ErrorDisplay from '../components/ErrorDisplay'

const GROWING_POSITIONS = ['SUNNY', 'PARTIALLY_SUNNY', 'SHADOWY'] as const
const SOIL_TYPES = ['CLAY', 'SANDY', 'LOAMY', 'CHALKY', 'PEATY', 'SILTY'] as const

const positionLabel: Record<string, string> = {
  SUNNY: 'Sunny', PARTIALLY_SUNNY: 'Partial sun', SHADOWY: 'Shadowy'
}
const soilLabel: Record<string, string> = {
  CLAY: 'Clay', SANDY: 'Sandy', LOAMY: 'Loamy', CHALKY: 'Chalky', PEATY: 'Peaty', SILTY: 'Silty'
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = reader.result as string
      resolve(result.split(',')[1]) // strip data:image/...;base64,
    }
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

// ── Species List ──

export function SpeciesListPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [previewSpecies, setPreviewSpecies] = useState<Species | null>(null)
  const [importStatus, setImportStatus] = useState<string | null>(null)
  const [exporting, setExporting] = useState(false)
  const [importing, setImporting] = useState(false)
  const importInputRef = useRef<HTMLInputElement>(null)

  const { data: species, isLoading, error } = useQuery({
    queryKey: ['admin', 'species'],
    queryFn: api.admin.getSpecies
  })

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
      setImportStatus(`Export failed: ${e instanceof Error ? e.message : 'Unknown error'}`)
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
      setImportStatus(`Imported ${result.created} species, skipped ${result.skipped} duplicates`)
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
    } catch (err) {
      setImportStatus(`Import failed: ${err instanceof Error ? err.message : 'Unknown error'}`)
    } finally {
      setImporting(false)
      e.target.value = ''
    }
  }

  const filtered = species?.filter(s => {
    const q = search.toLowerCase()
    return !q ||
      s.commonName.toLowerCase().includes(q) ||
      s.commonNameSv?.toLowerCase().includes(q) ||
      s.scientificName?.toLowerCase().includes(q) ||
      s.groupName?.toLowerCase().includes(q)
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">Loading...</div></div>
  if (error) return <ErrorDisplay error={error} onRetry={() => queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })} />

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-semibold text-[#37352F]">Species</h2>
          <p className="text-sm text-[#787774] mt-1">{species?.length || 0} species</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={handleExport}
            disabled={exporting}
            className="px-3 py-1.5 border border-[#E9E9E7] text-[#37352F] rounded-md hover:bg-[#F0F0EE] transition-colors text-sm disabled:opacity-50"
          >
            {exporting ? 'Exporting...' : 'Export'}
          </button>
          <button
            onClick={() => importInputRef.current?.click()}
            disabled={importing}
            className="px-3 py-1.5 border border-[#E9E9E7] text-[#37352F] rounded-md hover:bg-[#F0F0EE] transition-colors text-sm disabled:opacity-50"
          >
            {importing ? 'Importing...' : 'Import'}
          </button>
          <input ref={importInputRef} type="file" accept=".json" className="hidden" onChange={handleImportFile} />
          <button
            onClick={() => navigate('/species/new')}
            className="px-3 py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] transition-colors text-sm font-medium"
          >
            New
          </button>
        </div>
      </div>

      {importStatus && (
        <div className={`mb-4 px-3 py-2.5 rounded-md text-sm ${
          importStatus.startsWith('Import failed') || importStatus.startsWith('Export failed')
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
          placeholder="Search by name, scientific name, or group..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full max-w-md px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
        />
      </div>

      <div className="border border-[#E9E9E7] rounded-lg overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-[#E9E9E7] bg-[#FBFBFA]">
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Name</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Scientific Name</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Group</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Type</th>
              <th className="text-left px-4 py-2 text-xs font-medium text-[#787774] uppercase tracking-wider">Providers</th>
            </tr>
          </thead>
          <tbody>
            {filtered?.map((s: Species) => (
              <tr key={s.id} className="border-b border-[#E9E9E7] last:border-0 hover:bg-[#FBFBFA] cursor-pointer transition-colors" onClick={() => navigate(`/species/${s.id}`)}>
                <td className="px-4 py-2.5">
                  <div className="text-sm font-medium text-[#37352F]">
                    {s.commonNameSv || s.commonName}
                    {(s.variantNameSv || s.variantName) && <span className="text-[#787774] font-normal"> — {s.variantNameSv || s.variantName}</span>}
                  </div>
                  {s.commonNameSv && s.commonName !== s.commonNameSv && (
                    <div className="text-xs text-[#A5A29C]">
                      {s.commonName}
                      {s.variantName && <span> — {s.variantName}</span>}
                    </div>
                  )}
                </td>
                <td className="px-4 py-2.5 text-sm text-[#787774] italic">{s.scientificName || '—'}</td>
                <td className="px-4 py-2.5 text-sm text-[#787774]">{s.groupName || '—'}</td>
                <td className="px-4 py-2.5">
                  <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                    s.isSystem ? 'bg-[#D3E5EF] text-[#2B6CB0]' : 'bg-[#E9E9E7] text-[#787774]'
                  }`}>
                    {s.isSystem ? 'System' : 'User'}
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
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Provider Images Modal */}
      {previewSpecies && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50" onClick={() => setPreviewSpecies(null)}>
          <div className="bg-white rounded-lg border border-[#E9E9E7] max-w-4xl w-full mx-4 max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between px-6 py-4 border-b border-[#E9E9E7]">
              <h3 className="text-base font-semibold text-[#37352F]">
                {previewSpecies.commonNameSv || previewSpecies.commonName}
                {(previewSpecies.variantNameSv || previewSpecies.variantName) && <span className="text-[#787774] font-normal"> — {previewSpecies.variantNameSv || previewSpecies.variantName}</span>}
              </h3>
              <button onClick={() => setPreviewSpecies(null)} className="text-[#787774] hover:text-[#37352F] text-xl leading-none">&times;</button>
            </div>
            <div className="p-6 space-y-6">
              {previewSpecies.providers.map(sp => (
                <div key={sp.id}>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-sm font-medium text-[#37352F]">{sp.providerName}</span>
                    <span className="text-xs text-[#787774]">{sp.providerIdentifier}</span>
                    {sp.productUrl && (
                      <a href={sp.productUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-[#2EAADC] hover:underline ml-auto">
                        Product page &rarr;
                      </a>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    {sp.imageFrontUrl ? (
                      <img src={sp.imageFrontUrl} alt="Front" className="w-full rounded-md object-contain max-h-[500px]" />
                    ) : (
                      <div className="h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">No front image</div>
                    )}
                    {sp.imageBackUrl ? (
                      <img src={sp.imageBackUrl} alt="Back" className="w-full rounded-md object-contain max-h-[500px]" />
                    ) : (
                      <div className="h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">No back image</div>
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

// ── Image Upload Component ──

function ImageUpload({ label, currentUrl, onUpload, onClear }: {
  label: string
  currentUrl: string | null
  onUpload: (base64: string) => void
  onClear?: () => void
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [preview, setPreview] = useState<string | null>(null)

  const handleFile = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setPreview(URL.createObjectURL(file))
    const b64 = await fileToBase64(file)
    onUpload(b64)
  }, [onUpload])

  const displayUrl = preview || currentUrl

  return (
    <div>
      <label className="block text-xs font-medium text-[#787774] mb-1.5">{label}</label>
      <div className="border border-dashed border-[#D3D1CB] rounded-md p-3 text-center hover:border-[#2EAADC] transition-colors">
        {displayUrl ? (
          <div className="relative">
            <img src={displayUrl} alt={label} className="max-h-[600px] mx-auto rounded-md object-contain" />
            <div className="mt-2 flex gap-2 justify-center">
              <button
                type="button"
                onClick={() => inputRef.current?.click()}
                className="text-sm text-[#2EAADC] hover:underline"
              >
                Replace
              </button>
              {onClear && (
                <button
                  type="button"
                  onClick={() => { setPreview(null); onClear() }}
                  className="text-sm text-[#E03E3E] hover:underline"
                >
                  Remove
                </button>
              )}
            </div>
          </div>
        ) : (
          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            className="py-6 w-full text-sm text-[#A5A29C] hover:text-[#2EAADC]"
          >
            Click to upload
          </button>
        )}
        <input ref={inputRef} type="file" accept="image/*" className="hidden" onChange={handleFile} />
      </div>
    </div>
  )
}

// ── Chip Toggle ──

function ExtractStatus({ extractingFront, extractingBack, extractedFront, extractedBack, extractError }: {
  extractingFront: boolean
  extractingBack: boolean
  extractedFront: boolean
  extractedBack: boolean
  extractError: string | null
}) {
  if (!extractingFront && !extractingBack && !extractedFront && !extractedBack && !extractError) return null

  return (
    <div className="mt-3 flex flex-col gap-1 text-sm">
      {extractingFront && <span className="text-[#787774]">Extracting front with Gemini...</span>}
      {!extractingFront && extractedFront && <span className="text-[#0F7B0F]">Front data extracted</span>}
      {extractingBack && <span className="text-[#787774]">Extracting back with Gemini...</span>}
      {!extractingBack && extractedBack && <span className="text-[#0F7B0F]">Back data extracted</span>}
      {extractError && <span className="text-[#E03E3E]">{extractError}</span>}
    </div>
  )
}

function ChipToggle({ label, selected, onClick }: { label: string; selected: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-2.5 py-1 rounded-md text-sm transition-colors ${
        selected
          ? 'bg-[#37352F] text-white'
          : 'bg-[#F0F0EE] text-[#787774] hover:bg-[#E9E9E7]'
      }`}
    >
      {label}
    </button>
  )
}

// ── Species Detail (read-only) ──

const MONTH_LABELS_SHORT = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export function SpeciesDetailPage() {
  const { id } = useParams()
  const speciesId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [deleteStep, setDeleteStep] = useState<0 | 1 | 2>(0)

  const { data: species, isLoading, error } = useQuery({
    queryKey: ['admin', 'species', speciesId],
    queryFn: () => api.admin.getSpeciesById(speciesId)
  })

  const deleteMutation = useMutation({
    mutationFn: () => api.admin.deleteSpecies(speciesId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      navigate('/species')
    }
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">Loading...</div></div>
  if (error || !species) return <ErrorDisplay error={error ?? new Error('Species not found')} onRetry={() => navigate(0)} />

  return (
    <div>
      <button onClick={() => navigate('/species')} className="flex items-center gap-1 text-sm text-[#787774] hover:text-[#37352F] mb-4 transition-colors">
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        Back to list
      </button>

      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-semibold text-[#37352F]">
            {species.commonNameSv || species.commonName}
            {(species.variantNameSv || species.variantName) && <span className="text-[#787774] font-normal"> — {species.variantNameSv || species.variantName}</span>}
          </h2>
          {species.commonNameSv && species.commonName !== species.commonNameSv && (
            <p className="text-sm text-[#787774]">
              {species.commonName}
              {species.variantName && <span> — {species.variantName}</span>}
            </p>
          )}
          {species.scientificName && <p className="text-sm text-[#A5A29C] italic">{species.scientificName}</p>}
        </div>
        <div className="flex items-center gap-3">
          <span className={`px-2 py-0.5 rounded text-xs font-medium ${species.isSystem ? 'bg-[#D3E5EF] text-[#2B6CB0]' : 'bg-[#E9E9E7] text-[#787774]'}`}>
            {species.isSystem ? 'System' : 'User'}
          </span>
          <button
            onClick={() => navigate(`/species/${speciesId}/edit`)}
            className="px-3 py-1.5 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] transition-colors text-sm font-medium"
          >
            Edit
          </button>
        </div>
      </div>

      <div className="space-y-6">
        {/* Providers & Images */}
        {species.providers.length > 0 && (
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Seed Providers ({species.providers.length})</h3>
            <div className="space-y-6">
              {species.providers.map(sp => (
                <div key={sp.id}>
                  <div className="flex items-center gap-2 mb-3">
                    <span className="text-sm font-medium text-[#37352F]">{sp.providerName}</span>
                    <span className="text-xs text-[#787774]">{sp.providerIdentifier}</span>
                    {sp.productUrl && (
                      <a href={sp.productUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-[#2EAADC] hover:underline ml-auto">
                        Product page &rarr;
                      </a>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    {sp.imageFrontUrl ? (
                      <img src={sp.imageFrontUrl} alt="Front" className="w-full rounded-md object-contain max-h-[500px]" />
                    ) : (
                      <div className="h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">No front image</div>
                    )}
                    {sp.imageBackUrl ? (
                      <img src={sp.imageBackUrl} alt="Back" className="w-full rounded-md object-contain max-h-[500px]" />
                    ) : (
                      <div className="h-48 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md flex items-center justify-center text-[#A5A29C] text-sm">No back image</div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Growth Info, Months & Growing Conditions */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Growth Information</h3>
            <div className="grid grid-cols-2 gap-3">
              <InfoField label="Days to Sprout" value={species.daysToSprout} />
              <InfoField label="Days to Harvest" value={species.daysToHarvest} />
              <InfoField label="Germination (days)" value={species.germinationTimeDays} />
              <InfoField label="Sowing Depth (mm)" value={species.sowingDepthMm} />
              <InfoField label="Height (cm)" value={species.heightCm} />
              <InfoField label="Germ. Rate (%)" value={species.germinationRate} />
            </div>
          </section>

          {(species.sowingMonths.length > 0 || species.bloomMonths.length > 0) && (
            <section className="border border-[#E9E9E7] rounded-lg p-5">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Sowing & Bloom Months</h3>
              <div className="space-y-3">
                {species.sowingMonths.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">Sowing</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.sowingMonths.map(m => (
                        <span key={m} className="px-2 py-0.5 bg-[#DBEDDB] text-[#0F7B0F] rounded text-xs font-medium">{MONTH_LABELS_SHORT[m - 1]}</span>
                      ))}
                    </div>
                  </div>
                )}
                {species.bloomMonths.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">Bloom</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.bloomMonths.map(m => (
                        <span key={m} className="px-2 py-0.5 bg-[#F0E5FF] text-[#6940A5] rounded text-xs font-medium">{MONTH_LABELS_SHORT[m - 1]}</span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </section>
          )}

          {(species.growingPositions.length > 0 || species.soils.length > 0) && (
            <section className="border border-[#E9E9E7] rounded-lg p-5">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Growing Conditions</h3>
              <div className="space-y-3">
                {species.growingPositions.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">Position</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.growingPositions.map(p => (
                        <span key={p} className="px-2 py-0.5 bg-[#FBF3DB] text-[#73641C] rounded text-xs font-medium">{positionLabel[p] ?? p}</span>
                      ))}
                    </div>
                  </div>
                )}
                {species.soils.length > 0 && (
                  <div>
                    <label className="block text-xs font-medium text-[#787774] mb-1">Soil</label>
                    <div className="flex gap-1 flex-wrap">
                      {species.soils.map(s => (
                        <span key={s} className="px-2 py-0.5 bg-[#FADEC9] text-[#93592F] rounded text-xs font-medium">{soilLabel[s] ?? s}</span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </section>
          )}
        </div>

        {/* Group & Tags */}
        {(species.groupName || species.tags.length > 0) && (
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Group & Tags</h3>
            {species.groupName && (
              <div className="mb-3">
                <label className="block text-xs font-medium text-[#787774] mb-1">Group</label>
                <span className="text-sm text-[#37352F]">{species.groupName}</span>
              </div>
            )}
            {species.tags.length > 0 && (
              <div>
                <label className="block text-xs font-medium text-[#787774] mb-1">Tags</label>
                <div className="flex gap-1.5 flex-wrap">
                  {species.tags.map(t => (
                    <span key={t.id} className="px-2 py-0.5 bg-[#D3E5EF] text-[#2B6CB0] rounded text-xs font-medium">{t.name}</span>
                  ))}
                </div>
              </div>
            )}
          </section>
        )}

        {/* Additional Photos */}
        {species.photos.length > 0 && (
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Additional Photos ({species.photos.length})</h3>
            <div className="grid grid-cols-4 gap-4">
              {species.photos.map(photo => (
                <img key={photo.id} src={photo.imageUrl} alt="" className="w-full h-32 object-cover rounded-md" />
              ))}
            </div>
          </section>
        )}

        {/* Delete */}
        <section className="border border-[#E9E9E7] rounded-lg p-5 mt-2">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-2">Danger Zone</h3>
          <p className="text-sm text-[#787774] mb-4">Permanently delete this species and all associated data.</p>
          {deleteStep === 0 && (
            <button
              onClick={() => setDeleteStep(1)}
              className="px-3 py-1.5 border border-[#E03E3E] text-[#E03E3E] rounded-md hover:bg-[#FBE4E4] transition-colors text-sm font-medium"
            >
              Delete Species
            </button>
          )}
          {deleteStep === 1 && (
            <div className="flex items-center gap-3">
              <span className="text-sm text-[#E03E3E]">Are you sure?</span>
              <button
                onClick={() => setDeleteStep(2)}
                className="px-3 py-1.5 bg-[#E03E3E] text-white rounded-md hover:bg-[#C73535] transition-colors text-sm font-medium"
              >
                Yes, delete
              </button>
              <button
                onClick={() => setDeleteStep(0)}
                className="px-3 py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
              >
                Cancel
              </button>
            </div>
          )}
          {deleteStep === 2 && (
            <div className="flex items-center gap-3">
              <span className="text-sm text-[#E03E3E] font-medium">This cannot be undone.</span>
              <button
                onClick={() => deleteMutation.mutate()}
                disabled={deleteMutation.isPending}
                className="px-3 py-1.5 bg-[#E03E3E] text-white rounded-md hover:bg-[#C73535] disabled:opacity-50 transition-colors text-sm font-medium"
              >
                {deleteMutation.isPending ? 'Deleting...' : 'Permanently delete'}
              </button>
              <button
                onClick={() => setDeleteStep(0)}
                className="px-3 py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
              >
                Cancel
              </button>
            </div>
          )}
          {deleteMutation.error && (
            <p className="text-sm text-[#E03E3E] mt-2">{deleteMutation.error.message}</p>
          )}
        </section>
      </div>
    </div>
  )
}

function InfoField({ label, value }: { label: string; value: string | number | null | undefined }) {
  return (
    <div>
      <label className="block text-xs font-medium text-[#787774]">{label}</label>
      <span className="text-sm text-[#37352F]">{value ?? '—'}</span>
    </div>
  )
}

// ── Species Edit ──

export function SpeciesEditPage() {
  const { id } = useParams()
  const speciesId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const { data: species, isLoading, error: loadError } = useQuery({
    queryKey: ['admin', 'species', speciesId],
    queryFn: () => api.admin.getSpeciesById(speciesId)
  })

  const updateMutation = useMutation({
    mutationFn: (req: UpdateSpeciesRequest) => api.admin.updateSpecies(speciesId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      navigate(`/species/${speciesId}`)
    }
  })

  const uploadPhotoMutation = useMutation({
    mutationFn: (base64: string) => api.admin.uploadSpeciesPhoto(speciesId, base64),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species', speciesId] })
    }
  })

  const deletePhotoMutation = useMutation({
    mutationFn: (photoId: number) => api.admin.deleteSpeciesPhoto(speciesId, photoId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species', speciesId] })
    }
  })

  const addProviderMutation = useMutation({
    mutationFn: (req: AddSpeciesProviderRequest) => api.admin.addSpeciesProvider(speciesId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species', speciesId] })
    }
  })

  const deleteProviderMutation = useMutation({
    mutationFn: (spId: number) => api.admin.deleteSpeciesProvider(speciesId, spId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species', speciesId] })
    }
  })

  const updateProviderMutation = useMutation({
    mutationFn: ({ spId, req }: { spId: number; req: { imageFrontBase64?: string; imageBackBase64?: string } }) =>
      api.admin.updateSpeciesProvider(speciesId, spId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species', speciesId] })
    }
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">Loading...</div></div>
  if (loadError || !species) return <ErrorDisplay error={loadError ?? new Error('Species not found')} onRetry={() => navigate(0)} />

  return (
    <SpeciesForm
      species={species}
      title={`Edit: ${species.commonNameSv || species.commonName}`}
      submitLabel="Save Changes"
      isSubmitting={updateMutation.isPending}
      error={updateMutation.error?.message ?? null}
      onBack={() => navigate(`/species/${speciesId}`)}
      onSubmit={req => updateMutation.mutate(req as UpdateSpeciesRequest)}
      photos={species.photos}
      onUploadPhoto={async (base64) => { uploadPhotoMutation.mutate(base64) }}
      onDeletePhoto={(photoId) => { deletePhotoMutation.mutate(photoId) }}
      isUploadingPhoto={uploadPhotoMutation.isPending}
      onAddProvider={(req) => addProviderMutation.mutate(req)}
      onDeleteProvider={(spId) => deleteProviderMutation.mutate(spId)}
      onUpdateProvider={(spId, req) => updateProviderMutation.mutate({ spId, req })}
      isAddingProvider={addProviderMutation.isPending}
    />
  )
}

// ── Species Create ──

interface PendingProvider {
  providerId: number
  imageFrontBase64: string | null
  imageBackBase64: string | null
  productUrl: string | null
}

export function SpeciesCreatePage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  const createMutation = useMutation({
    mutationFn: async ({ req, provider }: { req: CreateSpeciesRequest; provider?: PendingProvider }) => {
      const species = await api.admin.createSpecies(req)
      if (provider) {
        await api.admin.addSpeciesProvider(species.id, {
          providerId: provider.providerId,
          imageFrontBase64: provider.imageFrontBase64,
          imageBackBase64: provider.imageBackBase64,
          productUrl: provider.productUrl,
        })
      }
      return species
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      navigate('/species')
    }
  })

  return (
    <SpeciesForm
      species={null}
      title="Add Species"
      submitLabel="Create Species"
      isSubmitting={createMutation.isPending}
      error={createMutation.error?.message ?? null}
      onBack={() => navigate('/species')}
      onSubmit={(req, provider) => createMutation.mutate({ req: req as CreateSpeciesRequest, provider })}
      photos={[]}
      onUploadPhoto={async () => {}}
      onDeletePhoto={() => {}}
      isUploadingPhoto={false}
    />
  )
}

// ── Shared Form ──

function SpeciesForm({
  species,
  title,
  submitLabel,
  isSubmitting,
  error,
  onBack,
  onSubmit,
  photos,
  onUploadPhoto,
  onDeletePhoto,
  isUploadingPhoto,
  onAddProvider,
  onDeleteProvider,
  onUpdateProvider,
  isAddingProvider,
}: {
  species: Species | null
  title: string
  submitLabel: string
  isSubmitting: boolean
  error: string | null
  onBack: () => void
  onSubmit: (req: CreateSpeciesRequest | UpdateSpeciesRequest, provider?: PendingProvider) => void
  photos: SpeciesPhoto[]
  onUploadPhoto: (base64: string) => void
  onDeletePhoto: (photoId: number) => void
  isUploadingPhoto: boolean
  onAddProvider?: (req: AddSpeciesProviderRequest) => void
  onDeleteProvider?: (spId: number) => void
  onUpdateProvider?: (spId: number, req: { imageFrontBase64?: string; imageBackBase64?: string }) => void
  isAddingProvider?: boolean
}) {
  const isEdit = species !== null

  const [commonName, setCommonName] = useState(species?.commonName ?? '')
  const [commonNameSv, setCommonNameSv] = useState(species?.commonNameSv ?? '')
  const [variantName, setVariantName] = useState(species?.variantName ?? '')
  const [variantNameSv, setVariantNameSv] = useState(species?.variantNameSv ?? '')
  const [scientificName, setScientificName] = useState(species?.scientificName ?? '')
  const [daysToSprout, setDaysToSprout] = useState(species?.daysToSprout?.toString() ?? '')
  const [daysToHarvest, setDaysToHarvest] = useState(species?.daysToHarvest?.toString() ?? '')
  const [germinationTimeDays, setGerminationTimeDays] = useState(species?.germinationTimeDays?.toString() ?? '')
  const [sowingDepthMm, setSowingDepthMm] = useState(species?.sowingDepthMm?.toString() ?? '')
  const [heightCm, setHeightCm] = useState(species?.heightCm?.toString() ?? '')
  const [bloomMonths, setBloomMonths] = useState<Set<number>>(new Set(species?.bloomMonths ?? []))
  const [sowingMonths, setSowingMonths] = useState<Set<number>>(new Set(species?.sowingMonths ?? []))
  const [germinationRate, setGerminationRate] = useState(species?.germinationRate?.toString() ?? '')
  const [positions, setPositions] = useState<Set<string>>(new Set(species?.growingPositions ?? []))
  const [soils, setSoils] = useState<Set<string>>(new Set(species?.soils ?? []))
  const [imageFrontBase64, setImageFrontBase64] = useState<string | null>(null)
  const [imageBackBase64, setImageBackBase64] = useState<string | null>(null)
  const [deletingPhotoId, setDeletingPhotoId] = useState<number | null>(null)
  const [extractingFront, setExtractingFront] = useState(false)
  const [extractingBack, setExtractingBack] = useState(false)
  const [extractedFront, setExtractedFront] = useState(false)
  const [extractedBack, setExtractedBack] = useState(false)
  const [extractError, setExtractError] = useState<string | null>(null)

  // Provider state
  const [selectedProviderId, setSelectedProviderId] = useState<number | null>(null)
  const [providerProductUrl, setProviderProductUrl] = useState('')
  const [addingNewProvider, setAddingNewProvider] = useState(false)
  const [newProviderProviderId, setNewProviderProviderId] = useState<number | null>(null)
  const [newProviderFrontBase64, setNewProviderFrontBase64] = useState<string | null>(null)
  const [newProviderBackBase64, setNewProviderBackBase64] = useState<string | null>(null)
  const [newProviderProductUrl, setNewProviderProductUrl] = useState('')
  const [deletingProviderId, setDeletingProviderId] = useState<number | null>(null)

  const { data: availableProviders } = useQuery({
    queryKey: ['admin', 'providers'],
    queryFn: api.admin.getProviders
  })

  const photoInputRef = useRef<HTMLInputElement>(null)

  const handleExtractFront = useCallback(async (base64: string) => {
    setExtractingFront(true)
    setExtractError(null)
    try {
      const info = await api.admin.extractFront(base64)
      if (info.commonName && !commonName) setCommonName(info.commonName)
      if (info.commonNameSv && !commonNameSv) setCommonNameSv(info.commonNameSv)
      if (info.variantName && !variantName) setVariantName(info.variantName)
      if (info.variantNameSv && !variantNameSv) setVariantNameSv(info.variantNameSv)
      if (info.scientificName && !scientificName) setScientificName(info.scientificName)
      setExtractedFront(true)
    } catch (e) {
      setExtractError(e instanceof Error ? e.message : 'Front extraction failed')
    } finally {
      setExtractingFront(false)
    }
  }, [commonName, commonNameSv, variantName, variantNameSv, scientificName])

  const handleExtractBack = useCallback(async (base64: string) => {
    setExtractingBack(true)
    setExtractError(null)
    try {
      const info = await api.admin.extractBack(base64)
      if (info.commonName && !commonName) setCommonName(info.commonName)
      if (info.scientificName && !scientificName) setScientificName(info.scientificName)
      if (info.daysToSprout != null) setDaysToSprout(info.daysToSprout.toString())
      if (info.daysToHarvest != null) setDaysToHarvest(info.daysToHarvest.toString())
      if (info.germinationTimeDays != null) setGerminationTimeDays(info.germinationTimeDays.toString())
      if (info.sowingDepthMm != null) setSowingDepthMm(info.sowingDepthMm.toString())
      if (info.heightCm != null) setHeightCm(info.heightCm.toString())
      if (info.bloomMonths) setBloomMonths(new Set(info.bloomMonths))
      if (info.sowingMonths) setSowingMonths(new Set(info.sowingMonths))
      if (info.germinationRate != null) setGerminationRate(info.germinationRate.toString())
      if (info.growingPositions) setPositions(new Set(info.growingPositions))
      if (info.soils) setSoils(new Set(info.soils))
      setExtractedBack(true)
    } catch (e) {
      setExtractError(e instanceof Error ? e.message : 'Back extraction failed')
    } finally {
      setExtractingBack(false)
    }
  }, [commonName, scientificName])

  const handlePhotoUpload = useCallback(async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const b64 = await fileToBase64(file)
    onUploadPhoto(b64)
    e.target.value = ''
  }, [onUploadPhoto])

  const togglePosition = (p: string) => {
    const next = new Set(positions)
    if (next.has(p)) next.delete(p); else next.add(p)
    setPositions(next)
  }

  const toggleSoil = (s: string) => {
    const next = new Set(soils)
    if (next.has(s)) next.delete(s); else next.add(s)
    setSoils(next)
  }

  const toggleBloomMonth = (m: number) => {
    const next = new Set(bloomMonths)
    if (next.has(m)) next.delete(m); else next.add(m)
    setBloomMonths(next)
  }

  const toggleSowingMonth = (m: number) => {
    const next = new Set(sowingMonths)
    if (next.has(m)) next.delete(m); else next.add(m)
    setSowingMonths(next)
  }

  const MONTH_LABELS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const req: CreateSpeciesRequest & UpdateSpeciesRequest = {
      commonName: commonName || undefined,
      commonNameSv: commonNameSv || undefined,
      variantName: variantName || undefined,
      variantNameSv: variantNameSv || undefined,
      scientificName: scientificName || undefined,
      imageFrontBase64: imageFrontBase64 ?? undefined,
      imageBackBase64: imageBackBase64 ?? undefined,
      daysToSprout: daysToSprout ? parseInt(daysToSprout) : undefined,
      daysToHarvest: daysToHarvest ? parseInt(daysToHarvest) : undefined,
      germinationTimeDays: germinationTimeDays ? parseInt(germinationTimeDays) : undefined,
      sowingDepthMm: sowingDepthMm ? parseInt(sowingDepthMm) : undefined,
      heightCm: heightCm ? parseInt(heightCm) : undefined,
      bloomMonths: [...bloomMonths].sort((a, b) => a - b),
      sowingMonths: [...sowingMonths].sort((a, b) => a - b),
      germinationRate: germinationRate ? parseInt(germinationRate) : undefined,
      growingPositions: [...positions],
      soils: [...soils],
    }
    const provider = !isEdit && selectedProviderId ? {
      providerId: selectedProviderId,
      imageFrontBase64,
      imageBackBase64,
      productUrl: providerProductUrl || null,
    } : undefined
    onSubmit(req, provider)
  }

  return (
    <div>
      <button onClick={onBack} className="flex items-center gap-1 text-sm text-[#787774] hover:text-[#37352F] mb-4 transition-colors">
        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        Back
      </button>

      <h2 className="text-2xl font-semibold text-[#37352F] mb-6">{title}</h2>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Provider & Images — create mode */}
        {!isEdit && (
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Seed Provider & Images</h3>
            <div className="mb-4">
              <label className="block text-xs font-medium text-[#787774] mb-1.5">Provider</label>
              <select
                value={selectedProviderId ?? ''}
                onChange={e => setSelectedProviderId(e.target.value ? Number(e.target.value) : null)}
                className="w-full max-w-xs px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
              >
                <option value="">Select a provider...</option>
                {availableProviders?.map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
            {selectedProviderId && (
              <>
                <div className="grid grid-cols-2 gap-6">
                  <ImageUpload
                    label="Front"
                    currentUrl={imageFrontBase64 ? `data:image/jpeg;base64,${imageFrontBase64}` : null}
                    onUpload={(b64) => { setImageFrontBase64(b64); handleExtractFront(b64) }}
                  />
                  <ImageUpload
                    label="Back"
                    currentUrl={imageBackBase64 ? `data:image/jpeg;base64,${imageBackBase64}` : null}
                    onUpload={(b64) => { setImageBackBase64(b64); handleExtractBack(b64) }}
                  />
                </div>
                <div className="mt-4">
                  <Field label="Product URL" value={providerProductUrl} onChange={setProviderProductUrl} />
                </div>
                <ExtractStatus
                  extractingFront={extractingFront}
                  extractingBack={extractingBack}
                  extractedFront={extractedFront}
                  extractedBack={extractedBack}
                  extractError={extractError}
                />
              </>
            )}
          </section>
        )}

        {/* Additional Photos — only in edit mode */}
        {isEdit && (
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider">
                Additional Photos
                <span className="ml-2 text-xs font-normal text-[#787774] normal-case">({photos.length})</span>
              </h3>
              <button
                type="button"
                onClick={() => photoInputRef.current?.click()}
                disabled={isUploadingPhoto}
                className="px-3 py-1.5 bg-[#2EAADC] text-white text-sm rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors"
              >
                {isUploadingPhoto ? 'Uploading...' : 'Upload'}
              </button>
              <input ref={photoInputRef} type="file" accept="image/*" className="hidden" onChange={handlePhotoUpload} />
            </div>
            {photos.length === 0 ? (
              <p className="text-[#A5A29C] text-sm">No additional photos yet</p>
            ) : (
              <div className="grid grid-cols-4 gap-4">
                {photos.map(photo => (
                  <div key={photo.id} className="relative group">
                    <img src={photo.imageUrl} alt="" className="w-full h-32 object-cover rounded-md" />
                    <div className="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-colors rounded-md flex items-center justify-center">
                      {deletingPhotoId === photo.id ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => { onDeletePhoto(photo.id); setDeletingPhotoId(null) }}
                            className="px-2 py-1 bg-[#E03E3E] text-white text-xs rounded font-medium"
                          >
                            Confirm
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingPhotoId(null)}
                            className="px-2 py-1 bg-white text-[#37352F] text-xs rounded font-medium"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setDeletingPhotoId(photo.id)}
                          className="opacity-0 group-hover:opacity-100 px-2 py-1 bg-[#E03E3E] text-white text-xs rounded font-medium transition-opacity"
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </section>
        )}

        {/* Basic Info */}
        <section className="border border-[#E9E9E7] rounded-lg p-5">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider">Basic Information</h3>
            <button
              type="button"
              onClick={() => {
                if (!commonName && commonNameSv) setCommonName(commonNameSv)
                if (!variantName && variantNameSv) setVariantName(variantNameSv)
              }}
              className="text-xs text-[#2EAADC] hover:underline font-medium"
            >
              Copy to English
            </button>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Common Name (Swedish) *" value={commonNameSv} onChange={setCommonNameSv} />
            <Field label="Common Name (English)" value={commonName} onChange={setCommonName} />
            <Field label="Variant Name (Swedish)" value={variantNameSv} onChange={setVariantNameSv} />
            <Field label="Variant Name (English)" value={variantName} onChange={setVariantName} />
            <Field label="Scientific Name" value={scientificName} onChange={setScientificName} className="col-span-2" />
          </div>
        </section>

        {/* Growth Info */}
        <section className="border border-[#E9E9E7] rounded-lg p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Growth Information</h3>
          <div className="grid grid-cols-3 gap-4">
            <Field label="Days to Sprout" value={daysToSprout} onChange={v => setDaysToSprout(v.replace(/\D/g, ''))} type="text" />
            <Field label="Days to Harvest" value={daysToHarvest} onChange={v => setDaysToHarvest(v.replace(/\D/g, ''))} type="text" />
            <Field label="Germination Time (days)" value={germinationTimeDays} onChange={v => setGerminationTimeDays(v.replace(/\D/g, ''))} type="text" />
            <Field label="Sowing Depth (mm)" value={sowingDepthMm} onChange={v => setSowingDepthMm(v.replace(/\D/g, ''))} type="text" />
            <Field label="Height (cm)" value={heightCm} onChange={v => setHeightCm(v.replace(/\D/g, ''))} type="text" />
            <Field label="Germination Rate (%)" value={germinationRate} onChange={v => setGerminationRate(v.replace(/\D/g, ''))} type="text" />
          </div>
        </section>

        {/* Sowing & Bloom Months */}
        <section className="border border-[#E9E9E7] rounded-lg p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Sowing & Bloom Months</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">Sowing Months</label>
              <div className="flex gap-1.5 flex-wrap">
                {MONTH_LABELS.map((label, i) => (
                  <ChipToggle key={i} label={label} selected={sowingMonths.has(i + 1)} onClick={() => toggleSowingMonth(i + 1)} />
                ))}
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">Bloom Months</label>
              <div className="flex gap-1.5 flex-wrap">
                {MONTH_LABELS.map((label, i) => (
                  <ChipToggle key={i} label={label} selected={bloomMonths.has(i + 1)} onClick={() => toggleBloomMonth(i + 1)} />
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Growing Conditions */}
        <section className="border border-[#E9E9E7] rounded-lg p-5">
          <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Growing Conditions</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">Growing Position</label>
              <div className="flex gap-1.5 flex-wrap">
                {GROWING_POSITIONS.map(p => (
                  <ChipToggle key={p} label={positionLabel[p]} selected={positions.has(p)} onClick={() => togglePosition(p)} />
                ))}
              </div>
            </div>
            <div>
              <label className="block text-xs font-medium text-[#787774] mb-2">Soil Type</label>
              <div className="flex gap-1.5 flex-wrap">
                {SOIL_TYPES.map(s => (
                  <ChipToggle key={s} label={soilLabel[s]} selected={soils.has(s)} onClick={() => toggleSoil(s)} />
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Providers — edit mode */}
        {isEdit && (
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider">
                Seed Providers
                <span className="ml-2 text-xs font-normal text-[#787774] normal-case">({species!.providers.length})</span>
              </h3>
              {!addingNewProvider && (
                <button
                  type="button"
                  onClick={() => setAddingNewProvider(true)}
                  className="px-3 py-1.5 bg-[#2EAADC] text-white text-sm rounded-md hover:bg-[#2898C4] transition-colors"
                >
                  Add Provider
                </button>
              )}
            </div>

            {/* Existing providers */}
            <div className="space-y-4">
              {species!.providers.map(sp => (
                <div key={sp.id} className="p-4 bg-[#FBFBFA] border border-[#E9E9E7] rounded-md">
                  <div className="flex items-center justify-between mb-3">
                    <div>
                      <span className="text-sm font-medium text-[#37352F]">{sp.providerName}</span>
                      <span className="ml-2 text-xs text-[#787774]">{sp.providerIdentifier}</span>
                    </div>
                    <div className="flex items-center gap-2">
                      {sp.productUrl && (
                        <a href={sp.productUrl} target="_blank" rel="noopener noreferrer" className="text-xs text-[#2EAADC] hover:underline">
                          Product page &rarr;
                        </a>
                      )}
                      {deletingProviderId === sp.id ? (
                        <>
                          <button
                            type="button"
                            onClick={() => { onDeleteProvider?.(sp.id); setDeletingProviderId(null) }}
                            className="text-[#E03E3E] text-sm font-medium hover:underline"
                          >
                            Confirm
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingProviderId(null)}
                            className="text-[#787774] text-sm hover:underline"
                          >
                            Cancel
                          </button>
                        </>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setDeletingProviderId(sp.id)}
                          className="text-[#787774] text-sm hover:text-[#E03E3E] transition-colors"
                        >
                          Remove
                        </button>
                      )}
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <ImageUpload
                      label="Front"
                      currentUrl={sp.imageFrontUrl}
                      onUpload={(b64) => { onUpdateProvider?.(sp.id, { imageFrontBase64: b64 }); handleExtractFront(b64) }}
                    />
                    <ImageUpload
                      label="Back"
                      currentUrl={sp.imageBackUrl}
                      onUpload={(b64) => { onUpdateProvider?.(sp.id, { imageBackBase64: b64 }); handleExtractBack(b64) }}
                    />
                  </div>
                  <ExtractStatus
                    extractingFront={extractingFront}
                    extractingBack={extractingBack}
                    extractedFront={extractedFront}
                    extractedBack={extractedBack}
                    extractError={extractError}
                  />
                </div>
              ))}
            </div>

            {/* Add new provider form */}
            {addingNewProvider && (
              <div className="mt-4 p-4 border border-dashed border-[#2EAADC] rounded-md bg-[#FBFBFA]">
                <h4 className="text-xs font-semibold text-[#37352F] uppercase tracking-wider mb-3">Add Provider</h4>
                <div className="mb-3">
                  <label className="block text-xs font-medium text-[#787774] mb-1.5">Provider</label>
                  <select
                    value={newProviderProviderId ?? ''}
                    onChange={e => setNewProviderProviderId(e.target.value ? Number(e.target.value) : null)}
                    className="w-full max-w-xs px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-white"
                  >
                    <option value="">Select a provider...</option>
                    {availableProviders
                      ?.filter(ap => !species!.providers.some(sp => sp.providerId === ap.id))
                      .map(p => (
                        <option key={p.id} value={p.id}>{p.name}</option>
                      ))}
                  </select>
                </div>
                {newProviderProviderId && (
                  <>
                    <div className="grid grid-cols-2 gap-4 mb-3">
                      <ImageUpload
                        label="Front"
                        currentUrl={newProviderFrontBase64 ? `data:image/jpeg;base64,${newProviderFrontBase64}` : null}
                        onUpload={setNewProviderFrontBase64}
                      />
                      <ImageUpload
                        label="Back"
                        currentUrl={newProviderBackBase64 ? `data:image/jpeg;base64,${newProviderBackBase64}` : null}
                        onUpload={setNewProviderBackBase64}
                      />
                    </div>
                    <div className="mb-3">
                      <Field label="Product URL" value={newProviderProductUrl} onChange={setNewProviderProductUrl} />
                    </div>
                  </>
                )}
                <div className="flex gap-2">
                  <button
                    type="button"
                    disabled={!newProviderProviderId || isAddingProvider}
                    onClick={() => {
                      if (!newProviderProviderId) return
                      onAddProvider?.({
                        providerId: newProviderProviderId,
                        imageFrontBase64: newProviderFrontBase64,
                        imageBackBase64: newProviderBackBase64,
                        productUrl: newProviderProductUrl || undefined,
                      })
                      setAddingNewProvider(false)
                      setNewProviderProviderId(null)
                      setNewProviderFrontBase64(null)
                      setNewProviderBackBase64(null)
                      setNewProviderProductUrl('')
                    }}
                    className="px-3 py-1.5 bg-[#2EAADC] text-white text-sm rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors font-medium"
                  >
                    {isAddingProvider ? 'Adding...' : 'Add'}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setAddingNewProvider(false)
                      setNewProviderProviderId(null)
                      setNewProviderFrontBase64(null)
                      setNewProviderBackBase64(null)
                      setNewProviderProductUrl('')
                    }}
                    className="px-3 py-1.5 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}

            {species!.providers.length === 0 && !addingNewProvider && (
              <p className="text-[#A5A29C] text-sm">No providers linked yet</p>
            )}
          </section>
        )}

        {/* Tags (read-only for now) */}
        {isEdit && species!.tags.length > 0 && (
          <section className="border border-[#E9E9E7] rounded-lg p-5">
            <h3 className="text-sm font-semibold text-[#37352F] uppercase tracking-wider mb-4">Tags</h3>
            <div className="flex gap-1.5 flex-wrap">
              {species!.tags.map(t => (
                <span key={t.id} className="px-2 py-0.5 bg-[#D3E5EF] text-[#2B6CB0] rounded text-xs font-medium">{t.name}</span>
              ))}
            </div>
          </section>
        )}

        {/* Submit */}
        {error && (
          <div className="bg-[#FBE4E4] text-[#E03E3E] px-3 py-2.5 rounded-md text-sm">{error}</div>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting || (!commonNameSv.trim() && !commonName.trim())}
            className="px-4 py-2 bg-[#2EAADC] text-white rounded-md hover:bg-[#2898C4] disabled:opacity-50 transition-colors text-sm font-medium"
          >
            {isSubmitting ? 'Saving...' : submitLabel}
          </button>
          <button
            type="button"
            onClick={onBack}
            className="px-4 py-2 text-[#787774] hover:bg-[#F0F0EE] rounded-md transition-colors text-sm"
          >
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}

// ── Text Field ──

function Field({ label, value, onChange, type = 'text', className = '' }: {
  label: string; value: string; onChange: (v: string) => void; type?: string; className?: string
}) {
  return (
    <div className={className}>
      <label className="block text-xs font-medium text-[#787774] mb-1.5">{label}</label>
      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        className="w-full px-3 py-2 border border-[#E9E9E7] rounded-md focus:ring-2 focus:ring-[#2EAADC]/30 focus:border-[#2EAADC] outline-none text-sm bg-[#FBFBFA]"
      />
    </div>
  )
}
