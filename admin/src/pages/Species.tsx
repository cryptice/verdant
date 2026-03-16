import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, type Species, type UpdateSpeciesRequest, type CreateSpeciesRequest, type SpeciesPhoto } from '../api/client'
import { useState, useRef, useCallback } from 'react'

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

export default function SpeciesPage() {
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [search, setSearch] = useState('')
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const { data: species, isLoading, error } = useQuery({
    queryKey: ['admin', 'species'],
    queryFn: api.admin.getSpecies
  })

  const deleteMutation = useMutation({
    mutationFn: api.admin.deleteSpecies,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      setDeletingId(null)
    }
  })

  const filtered = species?.filter(s => {
    const q = search.toLowerCase()
    return !q ||
      s.commonName.toLowerCase().includes(q) ||
      s.scientificName?.toLowerCase().includes(q) ||
      s.groupName?.toLowerCase().includes(q)
  })

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-gray-500">Loading...</div></div>
  if (error) return <div className="text-red-600">Error loading species</div>

  if (editingId !== null) {
    return <SpeciesDetail speciesId={editingId} onBack={() => setEditingId(null)} />
  }

  if (creating) {
    return <SpeciesCreate onBack={() => setCreating(false)} />
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">Species</h2>
          <p className="text-gray-500">{species?.length || 0} species</p>
        </div>
        <button
          onClick={() => setCreating(true)}
          className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors font-medium"
        >
          + Add Species
        </button>
      </div>

      <div className="mb-4">
        <input
          type="text"
          placeholder="Search by name, scientific name, or group..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full max-w-md px-4 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none"
        />
      </div>

      <div className="bg-white rounded-xl shadow-sm overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b bg-gray-50">
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">ID</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Image</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Name</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Scientific Name</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Group</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Type</th>
              <th className="text-left px-6 py-3 text-sm font-medium text-gray-500">Photos</th>
              <th className="text-right px-6 py-3 text-sm font-medium text-gray-500">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filtered?.map((s: Species) => (
              <tr key={s.id} className="border-b last:border-0 hover:bg-gray-50 cursor-pointer" onClick={() => setEditingId(s.id)}>
                <td className="px-6 py-3 text-sm text-gray-600">{s.id}</td>
                <td className="px-6 py-3">
                  {s.imageFrontUrl ? (
                    <img src={s.imageFrontUrl} alt="" className="w-10 h-10 rounded object-cover" />
                  ) : (
                    <div className="w-10 h-10 rounded bg-gray-100 flex items-center justify-center text-gray-400 text-xs">
                      No img
                    </div>
                  )}
                </td>
                <td className="px-6 py-3">
                  <div className="font-medium text-gray-800">
                    {s.commonName}
                    {s.variantName && <span className="text-gray-500 font-normal"> — {s.variantName}</span>}
                  </div>
                  {s.commonNameSv && s.commonNameSv !== s.commonName && (
                    <div className="text-xs text-gray-400">
                      {s.commonNameSv}
                      {s.variantNameSv && <span> — {s.variantNameSv}</span>}
                    </div>
                  )}
                </td>
                <td className="px-6 py-3 text-sm text-gray-600 italic">{s.scientificName || '—'}</td>
                <td className="px-6 py-3 text-sm text-gray-600">{s.groupName || '—'}</td>
                <td className="px-6 py-3">
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                    s.isSystem ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600'
                  }`}>
                    {s.isSystem ? 'System' : 'User'}
                  </span>
                </td>
                <td className="px-6 py-3 text-sm text-gray-600">{s.photos.length + (s.imageFrontUrl ? 1 : 0) + (s.imageBackUrl ? 1 : 0)}</td>
                <td className="px-6 py-3 text-right" onClick={e => e.stopPropagation()}>
                  {deletingId === s.id ? (
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => deleteMutation.mutate(s.id)}
                        className="text-red-600 text-sm font-medium hover:text-red-700"
                        disabled={deleteMutation.isPending}
                      >
                        Confirm
                      </button>
                      <button onClick={() => setDeletingId(null)} className="text-gray-500 text-sm hover:text-gray-700">
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <button onClick={() => setDeletingId(s.id)} className="text-red-500 text-sm hover:text-red-700">
                      Delete
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
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
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <div className="border-2 border-dashed border-gray-300 rounded-xl p-3 text-center hover:border-green-400 transition-colors">
        {displayUrl ? (
          <div className="relative">
            <img src={displayUrl} alt={label} className="max-h-[600px] mx-auto rounded-lg object-contain" />
            <div className="mt-2 flex gap-2 justify-center">
              <button
                type="button"
                onClick={() => inputRef.current?.click()}
                className="text-sm text-green-600 hover:text-green-700"
              >
                Replace
              </button>
              {onClear && (
                <button
                  type="button"
                  onClick={() => { setPreview(null); onClear() }}
                  className="text-sm text-red-500 hover:text-red-700"
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
            className="py-6 w-full text-sm text-gray-500 hover:text-green-600"
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

function ChipToggle({ label, selected, onClick }: { label: string; selected: boolean; onClick: () => void }) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`px-3 py-1.5 rounded-full text-sm font-medium transition-colors ${
        selected
          ? 'bg-green-600 text-white'
          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
      }`}
    >
      {label}
    </button>
  )
}

// ── Species Detail / Edit ──

function SpeciesDetail({ speciesId, onBack }: { speciesId: number; onBack: () => void }) {
  const queryClient = useQueryClient()

  const { data: species, isLoading } = useQuery({
    queryKey: ['admin', 'species', speciesId],
    queryFn: () => api.admin.getSpeciesById(speciesId)
  })

  const updateMutation = useMutation({
    mutationFn: (req: UpdateSpeciesRequest) => api.admin.updateSpecies(speciesId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      onBack()
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

  if (isLoading || !species) return <div className="flex justify-center py-12"><div className="text-gray-500">Loading...</div></div>

  return (
    <SpeciesForm
      species={species}
      title={`Edit: ${species.commonName}`}
      submitLabel="Save Changes"
      isSubmitting={updateMutation.isPending}
      error={updateMutation.error?.message ?? null}
      onBack={onBack}
      onSubmit={req => updateMutation.mutate(req as UpdateSpeciesRequest)}
      photos={species.photos}
      onUploadPhoto={async (base64) => { uploadPhotoMutation.mutate(base64) }}
      onDeletePhoto={(photoId) => { deletePhotoMutation.mutate(photoId) }}
      isUploadingPhoto={uploadPhotoMutation.isPending}
    />
  )
}

// ── Species Create ──

function SpeciesCreate({ onBack }: { onBack: () => void }) {
  const queryClient = useQueryClient()

  const createMutation = useMutation({
    mutationFn: (req: CreateSpeciesRequest) => api.admin.createSpecies(req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      onBack()
    }
  })

  return (
    <SpeciesForm
      species={null}
      title="Add Species"
      submitLabel="Create Species"
      isSubmitting={createMutation.isPending}
      error={createMutation.error?.message ?? null}
      onBack={onBack}
      onSubmit={req => createMutation.mutate(req as CreateSpeciesRequest)}
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
}: {
  species: Species | null
  title: string
  submitLabel: string
  isSubmitting: boolean
  error: string | null
  onBack: () => void
  onSubmit: (req: CreateSpeciesRequest | UpdateSpeciesRequest) => void
  photos: SpeciesPhoto[]
  onUploadPhoto: (base64: string) => void
  onDeletePhoto: (photoId: number) => void
  isUploadingPhoto: boolean
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
  const [bloomTime, setBloomTime] = useState(species?.bloomTime ?? '')
  const [germinationRate, setGerminationRate] = useState(species?.germinationRate?.toString() ?? '')
  const [positions, setPositions] = useState<Set<string>>(new Set(species?.growingPositions ?? []))
  const [soils, setSoils] = useState<Set<string>>(new Set(species?.soils ?? []))
  const [imageFrontBase64, setImageFrontBase64] = useState<string | null>(null)
  const [imageBackBase64, setImageBackBase64] = useState<string | null>(null)
  const [deletingPhotoId, setDeletingPhotoId] = useState<number | null>(null)

  const photoInputRef = useRef<HTMLInputElement>(null)

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
      bloomTime: bloomTime || undefined,
      germinationRate: germinationRate ? parseInt(germinationRate) : undefined,
      growingPositions: [...positions],
      soils: [...soils],
    }
    onSubmit(req)
  }

  return (
    <div>
      <button onClick={onBack} className="flex items-center gap-1 text-gray-600 hover:text-gray-800 mb-4">
        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
        Back to list
      </button>

      <h2 className="text-2xl font-bold text-gray-800 mb-6">{title}</h2>

      <form onSubmit={handleSubmit} className="space-y-8">
        {/* Images */}
        <section className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Main Images</h3>
          <div className="grid grid-cols-2 gap-6">
            <ImageUpload
              label="Front"
              currentUrl={imageFrontBase64 ? `data:image/jpeg;base64,${imageFrontBase64}` : species?.imageFrontUrl ?? null}
              onUpload={setImageFrontBase64}
            />
            <ImageUpload
              label="Back"
              currentUrl={imageBackBase64 ? `data:image/jpeg;base64,${imageBackBase64}` : species?.imageBackUrl ?? null}
              onUpload={setImageBackBase64}
            />
          </div>
        </section>

        {/* Additional Photos — only in edit mode */}
        {isEdit && (
          <section className="bg-white rounded-xl shadow-sm p-6">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-800">
                Additional Photos
                <span className="ml-2 text-sm font-normal text-gray-500">({photos.length})</span>
              </h3>
              <button
                type="button"
                onClick={() => photoInputRef.current?.click()}
                disabled={isUploadingPhoto}
                className="px-3 py-1.5 bg-green-600 text-white text-sm rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors"
              >
                {isUploadingPhoto ? 'Uploading...' : '+ Upload Photo'}
              </button>
              <input ref={photoInputRef} type="file" accept="image/*" className="hidden" onChange={handlePhotoUpload} />
            </div>
            {photos.length === 0 ? (
              <p className="text-gray-400 text-sm">No additional photos yet</p>
            ) : (
              <div className="grid grid-cols-4 gap-4">
                {photos.map(photo => (
                  <div key={photo.id} className="relative group">
                    <img src={photo.imageUrl} alt="" className="w-full h-32 object-cover rounded-lg" />
                    <div className="absolute inset-0 bg-black/0 group-hover:bg-black/30 transition-colors rounded-lg flex items-center justify-center">
                      {deletingPhotoId === photo.id ? (
                        <div className="flex gap-2">
                          <button
                            type="button"
                            onClick={() => { onDeletePhoto(photo.id); setDeletingPhotoId(null) }}
                            className="px-2 py-1 bg-red-600 text-white text-xs rounded font-medium"
                          >
                            Confirm
                          </button>
                          <button
                            type="button"
                            onClick={() => setDeletingPhotoId(null)}
                            className="px-2 py-1 bg-white text-gray-700 text-xs rounded font-medium"
                          >
                            Cancel
                          </button>
                        </div>
                      ) : (
                        <button
                          type="button"
                          onClick={() => setDeletingPhotoId(photo.id)}
                          className="opacity-0 group-hover:opacity-100 px-2 py-1 bg-red-600 text-white text-xs rounded font-medium transition-opacity"
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
        <section className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Basic Information</h3>
          <div className="grid grid-cols-2 gap-4">
            <Field label="Common Name *" value={commonName} onChange={setCommonName} />
            <Field label="Common Name (Swedish)" value={commonNameSv} onChange={setCommonNameSv} />
            <Field label="Variant Name" value={variantName} onChange={setVariantName} />
            <Field label="Variant Name (Swedish)" value={variantNameSv} onChange={setVariantNameSv} />
            <Field label="Scientific Name" value={scientificName} onChange={setScientificName} className="col-span-2" />
          </div>
        </section>

        {/* Growth Info */}
        <section className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Growth Information</h3>
          <div className="grid grid-cols-3 gap-4">
            <Field label="Days to Sprout" value={daysToSprout} onChange={v => setDaysToSprout(v.replace(/\D/g, ''))} type="text" />
            <Field label="Days to Harvest" value={daysToHarvest} onChange={v => setDaysToHarvest(v.replace(/\D/g, ''))} type="text" />
            <Field label="Germination Time (days)" value={germinationTimeDays} onChange={v => setGerminationTimeDays(v.replace(/\D/g, ''))} type="text" />
            <Field label="Sowing Depth (mm)" value={sowingDepthMm} onChange={v => setSowingDepthMm(v.replace(/\D/g, ''))} type="text" />
            <Field label="Height (cm)" value={heightCm} onChange={v => setHeightCm(v.replace(/\D/g, ''))} type="text" />
            <Field label="Germination Rate (%)" value={germinationRate} onChange={v => setGerminationRate(v.replace(/\D/g, ''))} type="text" />
            <Field label="Bloom Time" value={bloomTime} onChange={setBloomTime} className="col-span-3" />
          </div>
        </section>

        {/* Growing Conditions */}
        <section className="bg-white rounded-xl shadow-sm p-6">
          <h3 className="text-lg font-semibold text-gray-800 mb-4">Growing Conditions</h3>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Growing Position</label>
              <div className="flex gap-2 flex-wrap">
                {GROWING_POSITIONS.map(p => (
                  <ChipToggle key={p} label={positionLabel[p]} selected={positions.has(p)} onClick={() => togglePosition(p)} />
                ))}
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">Soil Type</label>
              <div className="flex gap-2 flex-wrap">
                {SOIL_TYPES.map(s => (
                  <ChipToggle key={s} label={soilLabel[s]} selected={soils.has(s)} onClick={() => toggleSoil(s)} />
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Providers (read-only, edit mode only) */}
        {isEdit && species!.providers.length > 0 && (
          <section className="bg-white rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Providers</h3>
            <div className="space-y-3">
              {species!.providers.map(p => (
                <div key={p.id} className="flex items-center gap-4 p-3 bg-gray-50 rounded-lg">
                  <div className="flex gap-2">
                    {p.imageFrontUrl && <img src={p.imageFrontUrl} alt="" className="w-12 h-12 object-cover rounded" />}
                    {p.imageBackUrl && <img src={p.imageBackUrl} alt="" className="w-12 h-12 object-cover rounded" />}
                  </div>
                  <div className="flex-1">
                    <div className="font-medium text-gray-800">{p.providerName}</div>
                    <div className="text-sm text-gray-500">{p.providerIdentifier}</div>
                  </div>
                  {p.productUrl && (
                    <a href={p.productUrl} target="_blank" rel="noopener noreferrer" className="text-sm text-green-600 hover:text-green-700">
                      Product page &rarr;
                    </a>
                  )}
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Tags (read-only for now) */}
        {isEdit && species!.tags.length > 0 && (
          <section className="bg-white rounded-xl shadow-sm p-6">
            <h3 className="text-lg font-semibold text-gray-800 mb-4">Tags</h3>
            <div className="flex gap-2 flex-wrap">
              {species!.tags.map(t => (
                <span key={t.id} className="px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm font-medium">{t.name}</span>
              ))}
            </div>
          </section>
        )}

        {/* Submit */}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-xl text-sm">{error}</div>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={isSubmitting || !commonName.trim()}
            className="px-6 py-2.5 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 transition-colors font-medium"
          >
            {isSubmitting ? 'Saving...' : submitLabel}
          </button>
          <button
            type="button"
            onClick={onBack}
            className="px-6 py-2.5 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
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
      <label className="block text-sm font-medium text-gray-700 mb-1">{label}</label>
      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        className="w-full px-3 py-2 border border-gray-300 rounded-xl focus:ring-2 focus:ring-green-500 focus:border-green-500 outline-none text-sm"
      />
    </div>
  )
}
