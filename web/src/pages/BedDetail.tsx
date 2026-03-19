import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { StatusBadge } from '../components/StatusBadge'
import { Dialog } from '../components/Dialog'

export function BedDetail() {
  const { id } = useParams<{ id: string }>()
  const bedId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()

  const { data: bed, error, isLoading, refetch } = useQuery({
    queryKey: ['bed', bedId],
    queryFn: () => api.beds.get(bedId),
  })

  const { data: plants } = useQuery({
    queryKey: ['bed-plants', bedId],
    queryFn: () => api.beds.plants(bedId),
  })

  const { data: garden } = useQuery({
    queryKey: ['garden', bed?.gardenId],
    queryFn: () => api.gardens.get(bed!.gardenId),
    enabled: !!bed,
  })

  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [showDelete, setShowDelete] = useState(false)
  const [showAdd, setShowAdd] = useState(false)
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())

  const updateMut = useMutation({
    mutationFn: () => api.beds.update(bedId, { name: editName, description: editDesc || undefined }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['bed', bedId] }); setEditing(false) },
  })

  const [deleteError, setDeleteError] = useState<string | null>(null)

  const deleteMut = useMutation({
    mutationFn: () => api.beds.delete(bedId),
    onSuccess: () => { navigate(-1); qc.invalidateQueries({ queryKey: ['garden-beds'] }) },
    onError: (err) => { setDeleteError(err instanceof Error ? err.message : String(err)) },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!bed) return null

  const grouped = new Map<string, typeof plants>()
  plants?.forEach(p => {
    const key = p.speciesName ?? p.name
    if (!grouped.has(key)) grouped.set(key, [])
    grouped.get(key)!.push(p)
  })

  const toggleGroup = (key: string) => {
    setExpandedGroups(prev => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key); else next.add(key)
      return next
    })
  }

  const breadcrumbs: BreadcrumbItem[] = [
    { label: t('nav.myWorld'), to: '/' },
    { label: garden?.name ?? '…', to: `/garden/${bed.gardenId}` },
  ]

  return (
    <div className="flex flex-col flex-1">
      <PageHeader
        title={bed.name}
        breadcrumbs={breadcrumbs}
        editAction={() => { setEditName(bed.name); setEditDesc(bed.description ?? ''); setEditing(true) }}
      />

      <div className="px-4 py-4 space-y-3">
        {bed.description && <p className="text-text-secondary">{bed.description}</p>}

        <div className="flex items-center justify-between">
          <h2 className="font-semibold">{t('bed.plants')}</h2>
          <button onClick={() => setShowAdd(true)} className="btn-primary text-sm">{t('bed.addPlant')}</button>
        </div>

        {plants && plants.length === 0 && (
          <p className="text-text-secondary text-sm">{t('bed.noPlantsYet')}</p>
        )}

        {Array.from(grouped.entries()).map(([species, group]) => {
          const expanded = expandedGroups.has(species)
          return (
            <div key={species} className="card p-0 overflow-hidden">
              <button onClick={() => toggleGroup(species)} className="w-full flex items-center justify-between px-4 py-3 text-left">
                <div>
                  <p className="font-semibold">{species}</p>
                  <p className="text-xs text-text-secondary">{t('bed.plantCount', { count: group!.length })}</p>
                </div>
                <span className="text-text-secondary text-lg">{expanded ? '▲' : '▼'}</span>
              </button>
              {expanded && (
                <div className="border-t border-divider">
                  {group!.map((p, i) => (
                    <div key={p.id}>
                      <Link to={`/plant/${p.id}`} className="flex items-center justify-between px-4 py-2.5 text-sm no-underline text-inherit hover:bg-surface">
                        <span>{p.name}</span>
                        <StatusBadge status={p.status} />
                      </Link>
                      {i < group!.length - 1 && <hr className="mx-4 border-divider" />}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )
        })}
      </div>

      <div className="mt-auto px-4 pb-4 pt-6">
        <div className="rounded-xl border border-error/20 overflow-hidden">
          <div className="px-4 py-2 bg-error/5 border-b border-error/20">
            <p className="text-xs font-semibold text-error uppercase tracking-wide">{t('common.dangerZone')}</p>
          </div>
          <div className="px-4 py-3 flex items-center justify-between">
            <p className="text-sm text-text-secondary">{t('bed.deleteBedConfirm')}</p>
            <button onClick={() => setShowDelete(true)} className="ml-4 shrink-0 text-sm font-medium text-error hover:underline">
              {t('bed.deleteBed')}
            </button>
          </div>
        </div>
      </div>

      <Dialog open={editing} onClose={() => setEditing(false)} title={t('bed.editBedTitle')} actions={
        <>
          <button onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => updateMut.mutate()} disabled={!editName.trim()} className="btn-primary text-sm">{t('common.save')}</button>
        </>
      }>
        <div className="space-y-3">
          <input value={editName} onChange={e => setEditName(e.target.value)} placeholder={t('bed.bedNamePlaceholder')} className="input" />
          <textarea value={editDesc} onChange={e => setEditDesc(e.target.value)} placeholder={t('bed.descriptionOptionalPlaceholder')} rows={2} className="input" />
        </div>
      </Dialog>

      <Dialog open={showDelete} onClose={() => { setShowDelete(false); setDeleteError(null) }} title={t('bed.deleteBedTitle')} actions={
        <>
          <button onClick={() => { setShowDelete(false); setDeleteError(null) }} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('bed.deleteBedConfirm')}</p>
        {deleteError && <p className="text-error text-sm mt-2">{deleteError}</p>}
      </Dialog>

      <Dialog open={showAdd} onClose={() => setShowAdd(false)} title={t('bed.addPlantTitle')} actions={
        <button onClick={() => setShowAdd(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
      }>
        <div className="space-y-2">
          <button onClick={() => { setShowAdd(false); navigate(`/sow?bedId=${bedId}`) }} className="btn-primary w-full text-sm">{t('bed.sowSeedsInBed')}</button>
        </div>
      </Dialog>

    </div>
  )
}
