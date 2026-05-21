import { useParams, useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { Masthead, Rule } from '../components/faltet'
import { SpeciesEditForm } from '../components/faltet/SpeciesEditForm'

export function SpeciesDetail() {
  const { id } = useParams<{ id: string }>()
  const speciesId = Number(id)
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()

  const [confirmDelete, setConfirmDelete] = useState(false)

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [speciesId])

  const deleteMut = useMutation({
    mutationFn: () => api.species.delete(speciesId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species'] })
      navigate('/species')
    },
  })

  const copyMut = useMutation({
    mutationFn: async () => {
      const full = await api.species.get(speciesId)
      return api.species.create({
        commonName: full.commonName,
        commonNameSv: full.commonNameSv,
        variantName: full.variantName ? `${full.variantName} (kopia)` : '(kopia)',
        variantNameSv: full.variantNameSv ? `${full.variantNameSv} (kopia)` : undefined,
        scientificName: full.scientificName,
        germinationTimeDaysMin: full.germinationTimeDaysMin,
        germinationTimeDaysMax: full.germinationTimeDaysMax,
        daysToHarvestMin: full.daysToHarvestMin,
        daysToHarvestMax: full.daysToHarvestMax,
        sowingDepthMm: full.sowingDepthMm,
        heightCmMin: full.heightCmMin,
        heightCmMax: full.heightCmMax,
        germinationRate: full.germinationRate,
        bloomMonths: full.bloomMonths,
        sowingMonths: full.sowingMonths,
        costPerSeedCents: full.costPerSeedCents,
        expectedStemsPerPlant: full.expectedStemsPerPlant,
        expectedVaseLifeDays: full.expectedVaseLifeDays,
        plantType: full.plantType,
        defaultUnitType: full.defaultUnitType,
      })
    },
    onSuccess: (created) => {
      qc.invalidateQueries({ queryKey: ['species'] })
      navigate(`/species/${created.id}`)
    },
  })

  return (
    <div>
      <Masthead
        left={t('species.masthead.left')}
        center={t('species.masthead.center')}
        right={
          <Link
            to={`/workflows/progress/${speciesId}`}
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              textDecoration: 'none',
            }}
          >
            → {t('species.workflow.link')}
          </Link>
        }
      />

      <SpeciesEditForm speciesId={speciesId} />

      <WorkflowAccessPanel speciesId={speciesId} />

      <div style={{ padding: '0 28px 28px' }}>
        <Rule variant="soft" />
        <div style={{ marginTop: 22, display: 'flex', justifyContent: 'flex-end' }}>
          {!confirmDelete ? (
            <div style={{ display: 'flex', gap: 18 }}>
              <button
                onClick={() => copyMut.mutate()}
                disabled={copyMut.isPending}
                style={{
                  background: 'transparent',
                  border: 'none',
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-accent)',
                  cursor: 'pointer',
                }}
              >
                ⎘ {copyMut.isPending ? t('common.loading') : t('species.copy')}
              </button>
              <button
                onClick={() => setConfirmDelete(true)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-accent)',
                  cursor: 'pointer',
                }}
              >
                ↵ {t('species.delete.trigger')}
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
              <span
                style={{
                  fontFamily: 'var(--font-display)',
                  fontStyle: 'italic',
                  fontSize: 15,
                  color: 'var(--color-accent)',
                }}
              >
                {t('species.delete.confirm')}
              </span>
              <button onClick={() => setConfirmDelete(false)} className="btn-secondary">
                {t('common.cancel')}
              </button>
              <button
                onClick={() => deleteMut.mutate()}
                disabled={deleteMut.isPending}
                className="btn-primary"
                style={{ background: 'var(--color-accent)', borderColor: 'var(--color-accent)' }}
              >
                {deleteMut.isPending ? t('species.delete.deleting') : t('species.delete.confirmButton')}
              </button>
            </div>
          )}
        </div>
        {deleteMut.error && (
          <p
            style={{
              marginTop: 12,
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 14,
              color: 'var(--color-accent)',
              textAlign: 'right',
            }}
          >
            {(deleteMut.error as Error).message}
          </p>
        )}
      </div>
    </div>
  )
}

/**
 * Entry points into workflow management for this species. Keeps the
 * shared SpeciesEditForm clean while restoring access to the workflow
 * pages that used to be inline (assign / sync / add-step were a full
 * WorkflowSection prior to the form refactor).
 */
function WorkflowAccessPanel({ speciesId }: { speciesId: number }) {
  const { t } = useTranslation()

  const { data: workflow } = useQuery({
    queryKey: ['species-workflow', speciesId],
    queryFn: () => api.workflows.getSpeciesWorkflow(speciesId),
  })

  const templateId = workflow?.templateId
  const templateName = workflow?.templateName
  const stepCount = workflow?.steps.length ?? 0

  return (
    <div style={{ padding: '0 28px 22px' }}>
      <Rule variant="soft" />
      <div style={{ marginTop: 22 }}>
        <div
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 9,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
            opacity: 0.7,
            marginBottom: 8,
          }}
        >
          {t('workflows.title')}
        </div>

        {templateId ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'baseline',
              justifyContent: 'space-between',
              gap: 18,
              flexWrap: 'wrap',
            }}
          >
            <div>
              <div style={{ fontFamily: 'var(--font-display)', fontSize: 20, fontWeight: 300 }}>
                {templateName}
              </div>
              <div
                style={{
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-forest)',
                  opacity: 0.7,
                }}
              >
                {t('workflows.stepCount', { count: stepCount })}
              </div>
            </div>
            <div style={{ display: 'flex', gap: 18 }}>
              <WorkflowLink to={`/workflows/progress/${speciesId}`}>
                → {t('workflows.viewProgress')}
              </WorkflowLink>
              <WorkflowLink to={`/workflows/${templateId}/edit`}>
                → {t('workflows.editTemplate')}
              </WorkflowLink>
            </div>
          </div>
        ) : (
          <div
            style={{
              display: 'flex',
              alignItems: 'baseline',
              justifyContent: 'space-between',
              gap: 18,
              flexWrap: 'wrap',
            }}
          >
            <div
              style={{
                fontFamily: 'var(--font-display)',
                fontStyle: 'italic',
                fontSize: 16,
                color: 'var(--color-forest)',
              }}
            >
              {t('workflows.noWorkflow')}
            </div>
            <WorkflowLink to="/workflows">
              → {t('workflows.assignTemplate')}
            </WorkflowLink>
          </div>
        )}
      </div>
    </div>
  )
}

function WorkflowLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      style={{
        fontFamily: 'var(--font-mono)',
        fontSize: 10,
        letterSpacing: 1.4,
        textTransform: 'uppercase',
        color: 'var(--color-accent)',
        textDecoration: 'none',
      }}
    >
      {children}
    </Link>
  )
}
