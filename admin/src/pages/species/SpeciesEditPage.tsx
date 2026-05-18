import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type UpdateSpeciesRequest, type AddSpeciesProviderRequest } from '../../api/client'
import { useNavigate, useParams } from 'react-router-dom'
import ErrorDisplay from '../../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'
import { SpeciesForm } from './SpeciesForm'

export function SpeciesEditPage() {
  const { id } = useParams()
  const speciesId = Number(id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { t } = useTranslation()

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

  if (isLoading) return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  if (loadError || !species) return <ErrorDisplay error={loadError ?? new Error(t('errors.speciesNotFound'))} onRetry={() => navigate(0)} />

  return (
    <SpeciesForm
      species={species}
      title={t('species.editTitle', { name: species.commonNameSv || species.commonName })}
      submitLabel={t('species.saveChanges')}
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
