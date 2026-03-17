import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'

export function BedForm() {
  const { gardenId } = useParams<{ gardenId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')

  const mutation = useMutation({
    mutationFn: () => api.beds.create(Number(gardenId), { name, description: description || undefined }),
    onSuccess: (b) => { qc.invalidateQueries({ queryKey: ['garden-beds', Number(gardenId)] }); navigate(`/bed/${b.id}`, { replace: true }) },
  })

  return (
    <div className="max-w-lg">
      <PageHeader title={t('bed.newBedTitle')} back />
      <div className="form-card">
        <div>
          <label className="field-label">{t('common.nameLabel')}</label>
          <input value={name} onChange={e => setName(e.target.value)} placeholder={t('bed.bedNamePlaceholder')} className="input" />
        </div>
        <div>
          <label className="field-label">{t('common.descriptionLabel')}</label>
          <textarea value={description} onChange={e => setDescription(e.target.value)} placeholder={t('common.optional')} rows={2} className="input" />
        </div>
      </div>
      {mutation.error && <p className="text-error text-sm mt-3">{(mutation.error as Error).message}</p>}
      <div className="mt-4 flex justify-end">
        <button onClick={() => mutation.mutate()} disabled={!name.trim() || mutation.isPending} className="btn-primary">
          {mutation.isPending ? t('bed.creatingBed') : t('bed.createBed')}
        </button>
      </div>
    </div>
  )
}
