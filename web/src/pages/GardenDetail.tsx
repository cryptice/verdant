import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { BreadcrumbItem } from '../components/Breadcrumb'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { Dialog } from '../components/Dialog'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

export function GardenDetail() {
  const { id } = useParams<{ id: string }>()
  const gardenId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()

  const { data: garden, error, isLoading, refetch } = useQuery({
    queryKey: ['garden', gardenId],
    queryFn: () => api.gardens.get(gardenId),
  })

  const { data: beds } = useQuery({
    queryKey: ['garden-beds', gardenId],
    queryFn: () => api.gardens.beds(gardenId),
  })

  const [editing, setEditing] = useState(false)
  const [editName, setEditName] = useState('')
  const [editDesc, setEditDesc] = useState('')
  const [editEmoji, setEditEmoji] = useState('')
  const [showDelete, setShowDelete] = useState(false)

  const updateMut = useMutation({
    mutationFn: () => api.gardens.update(gardenId, { name: editName, description: editDesc || undefined, emoji: editEmoji || undefined }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['garden', gardenId] }); setEditing(false) },
  })

  const deleteMut = useMutation({
    mutationFn: () => api.gardens.delete(gardenId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['dashboard'] }); navigate('/') },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>
  if (error) return <ErrorDisplay error={error} onRetry={refetch} />
  if (!garden) return null

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('nav.myWorld'), to: '/' }]

  return (
    <div>
      <PageHeader
        title={garden.name}
        back
        breadcrumbs={breadcrumbs}
        action={{ label: t('common.edit'), onClick: () => { setEditName(garden.name); setEditDesc(garden.description ?? ''); setEditEmoji(garden.emoji ?? ''); setEditing(true) } }}
      />

      <div className="px-4 py-4 space-y-4">
        {garden.description && <p className="text-text-secondary">{garden.description}</p>}

        <div className="flex items-center justify-between">
          <h2 className="font-semibold">{t('garden.beds')}</h2>
          <div className="flex items-center gap-3">
            <button onClick={() => navigate(`/garden/${gardenId}/bed/new`)} className="btn-primary text-sm">{t('garden.newBed')}</button>
            <button onClick={() => setShowDelete(true)} className="text-error text-sm">{t('garden.deleteGarden')}</button>
          </div>
        </div>

        {beds && beds.length === 0 && (
          <p className="text-text-secondary text-sm">{t('garden.noBedsYet')}</p>
        )}

        {beds?.map(bed => (
          <Link key={bed.id} to={`/bed/${bed.id}`} className="card block no-underline text-inherit">
            <p className="font-semibold">{bed.name}</p>
            {bed.description && <p className="text-sm text-text-secondary mt-1">{bed.description}</p>}
          </Link>
        ))}
      </div>

      <Dialog open={editing} onClose={() => setEditing(false)} title={t('garden.editGardenTitle')} actions={
        <>
          <button onClick={() => setEditing(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => updateMut.mutate()} disabled={!editName.trim()} className="btn-primary text-sm">{t('common.save')}</button>
        </>
      }>
        <div className="space-y-4">
          <div>
            <label className="field-label">{t('common.iconLabel')}</label>
            <div className="grid grid-cols-8 gap-1 p-2 bg-surface rounded-md border border-divider">
              {GARDEN_ICONS.map(icon => (
                <button
                  key={icon}
                  type="button"
                  onClick={() => setEditEmoji(editEmoji === icon ? '' : icon)}
                  className={`text-xl p-1.5 rounded-md transition-colors leading-none ${
                    editEmoji === icon
                      ? 'bg-accent-light ring-1 ring-accent'
                      : 'hover:bg-divider'
                  }`}
                >
                  {icon}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="field-label">{t('common.nameLabel')}</label>
            <input value={editName} onChange={e => setEditName(e.target.value)} placeholder={t('garden.gardenNamePlaceholder')} className="input" />
          </div>
          <div>
            <label className="field-label">{t('common.descriptionLabel')}</label>
            <textarea value={editDesc} onChange={e => setEditDesc(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
          </div>
        </div>
      </Dialog>

      <Dialog open={showDelete} onClose={() => setShowDelete(false)} title={t('garden.deleteGardenTitle')} actions={
        <>
          <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">{t('common.delete')}</button>
        </>
      }>
        <p className="text-text-secondary">{t('garden.deleteGardenConfirm')}</p>
      </Dialog>

    </div>
  )
}
