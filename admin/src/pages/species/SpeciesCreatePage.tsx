import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type Species, type CreateSpeciesRequest } from '../../api/client'
import { useNavigate, useSearchParams } from 'react-router-dom'
import ErrorDisplay from '../../components/ErrorDisplay'
import { useTranslation } from 'react-i18next'
import { SpeciesForm } from './SpeciesForm'
import type { PendingProvider } from './shared'

export function SpeciesCreatePage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { t } = useTranslation()
  const [searchParams] = useSearchParams()
  const fromParam = searchParams.get('from')
  const fromId = fromParam ? Number(fromParam) : null

  const { data: source, isLoading: sourceLoading, error: sourceError } = useQuery({
    queryKey: ['admin', 'species', fromId],
    queryFn: () => api.admin.getSpeciesById(fromId!),
    enabled: fromId !== null,
  })

  const template: Species | null = source ? {
    ...source,
    variantName: source.variantName ? `${source.variantName} (kopia)` : '(kopia)',
    variantNameSv: source.variantNameSv ? `${source.variantNameSv} (kopia)` : null,
    providers: [],
    photos: [],
  } : null

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
    onSuccess: (species) => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'species'] })
      navigate(`/species/${species.id}`)
    }
  })

  if (fromId !== null && sourceLoading) {
    return <div className="flex justify-center py-12"><div className="text-[#787774] text-sm">{t('common.loading')}</div></div>
  }
  if (fromId !== null && sourceError) {
    return <ErrorDisplay error={sourceError} onRetry={() => queryClient.invalidateQueries({ queryKey: ['admin', 'species', fromId] })} />
  }

  return (
    <SpeciesForm
      species={null}
      template={template}
      title={t('species.addSpecies')}
      submitLabel={t('species.createSpecies')}
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
