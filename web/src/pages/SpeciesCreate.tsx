import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead } from '../components/faltet'
import { ErrorDisplay } from '../components/ErrorDisplay'
import { SpeciesForm, type SpeciesFormExtras } from '../components/species/SpeciesForm'

/** New variety, optionally prefilled from another (`?from=<id>`) as a copy. */
export function SpeciesCreate() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const [searchParams] = useSearchParams()
  const fromId = searchParams.get('from') ? Number(searchParams.get('from')) : null

  const { data: source, isLoading, error, refetch } = useQuery({
    queryKey: ['species', fromId],
    queryFn: () => api.species.get(fromId!),
    enabled: fromId !== null,
  })

  const template: SpeciesResponse | null = source ? {
    ...source,
    variantName: source.variantName ? `${source.variantName} (kopia)` : '(kopia)',
    variantNameSv: source.variantNameSv ? `${source.variantNameSv} (kopia)` : undefined,
    providers: [],
    photos: [],
  } : null

  const createMut = useMutation({
    mutationFn: async ({ req, extras }: { req: Record<string, unknown>; extras: SpeciesFormExtras }) => {
      const created = await api.species.create(req)
      if (extras.providerId) await api.species.addProvider(created.id, { providerId: extras.providerId, productUrl: extras.productUrl })
      if (extras.workflowTemplateId) await api.workflows.assignToSpecies(created.id, extras.workflowTemplateId)
      return created
    },
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['species'] })
      navigate(`/species/${created.id}`, { replace: true })
    },
  })

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.species')} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>{t('speciesForm.newTitle')}</span>
          </span>
        }
        center={t('form.masthead.center')}
      />
      {fromId !== null && isLoading ? (
        <div className="flex justify-center p-16">
          <div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" />
        </div>
      ) : fromId !== null && error ? (
        <ErrorDisplay error={error} onRetry={refetch} />
      ) : (
        <SpeciesForm
          species={null}
          template={template}
          title={t('speciesForm.newTitle')}
          submitLabel={t('speciesForm.create')}
          isSubmitting={createMut.isPending}
          error={createMut.error?.message ?? null}
          onCancel={() => navigate(-1)}
          onSubmit={(req, extras) => createMut.mutate({ req, extras })}
        />
      )}
    </div>
  )
}
