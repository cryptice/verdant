import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import type { BreadcrumbItem } from '../components/Breadcrumb'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

export function GardenForm() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [emoji, setEmoji] = useState('')

  const mutation = useMutation({
    mutationFn: () => api.gardens.create({ name, description: description || undefined, emoji: emoji || undefined }),
    onSuccess: (g) => { qc.invalidateQueries({ queryKey: ['dashboard'] }); navigate(`/garden/${g.id}`, { replace: true }) },
  })

  const breadcrumbs: BreadcrumbItem[] = [{ label: t('nav.myWorld'), to: '/' }]

  return (
    <div className="max-w-lg">
      <PageHeader title={t('garden.newGardenTitle')} breadcrumbs={breadcrumbs} />
      <div className="form-card">
        <div>
          <label className="field-label">{t('common.iconLabel')}</label>
          <div className="grid grid-cols-8 gap-1 p-2 bg-surface rounded-md border border-divider">
            {GARDEN_ICONS.map(icon => (
              <button
                key={icon}
                type="button"
                onClick={() => setEmoji(emoji === icon ? '' : icon)}
                className={`text-xl p-1.5 rounded-md transition-colors leading-none ${
                  emoji === icon
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
          <input value={name} onChange={e => setName(e.target.value)} placeholder={t('garden.gardenNamePlaceholder')} className="input" />
        </div>
        <div>
          <label className="field-label">{t('common.descriptionLabel')}</label>
          <textarea value={description} onChange={e => setDescription(e.target.value)} placeholder={t('common.optional')} rows={3} className="input" />
        </div>
      </div>
      {mutation.error && <p className="text-error text-sm mt-3">{(mutation.error as Error).message}</p>}
      <div className="mt-4 flex justify-end">
        <button onClick={() => mutation.mutate()} disabled={!name.trim() || mutation.isPending} className="btn-primary">
          {mutation.isPending ? t('garden.creatingGarden') : t('garden.createGarden')}
        </button>
      </div>
    </div>
  )
}
