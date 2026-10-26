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

      <SpeciesEditForm speciesId={speciesId} workflowSection={<WorkflowAccessPanel speciesId={speciesId} />} />

      <div style={{ padding: '0 28px 28px' }}>
        <Rule variant="soft" />
        <div style={{ marginTop: 22, display: 'flex', justifyContent: 'flex-end' }}>
          {!confirmDelete ? (
            <div style={{ display: 'flex', gap: 18 }}>
              <button
                onClick={() => navigate(`/species/new?from=${speciesId}`)}
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
                ⎘ {t('species.copy')}
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
 * The species form's workflow section: assign or switch the template,
 * sync from it, and links to progress and the template editor.
 */
function WorkflowAccessPanel({ speciesId }: { speciesId: number }) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [pendingTemplateId, setPendingTemplateId] = useState<string>('')

  const { data: workflow } = useQuery({
    queryKey: ['species-workflow', speciesId],
    queryFn: () => api.workflows.getSpeciesWorkflow(speciesId),
  })

  const { data: templates = [] } = useQuery({
    queryKey: ['workflow-templates'],
    queryFn: api.workflows.templates,
  })

  const invalidate = () => qc.invalidateQueries({ queryKey: ['species-workflow', speciesId] })

  const assignMut = useMutation({
    mutationFn: (templateId: number) => api.workflows.assignToSpecies(speciesId, templateId),
    onSuccess: () => {
      setPendingTemplateId('')
      invalidate()
    },
  })

  const syncMut = useMutation({
    mutationFn: () => api.workflows.syncSpeciesWorkflow(speciesId),
    onSuccess: invalidate,
  })

  const templateId = workflow?.templateId
  const templateName = workflow?.templateName
  const stepCount = workflow?.steps.length ?? 0
  // When a template is assigned we offer "switch template" via the same
  // dropdown — preselect the current one so the change is explicit.
  const selectValue = pendingTemplateId || (templateId ? String(templateId) : '')

  return (
    <div>
      <div>

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
            <div style={{ display: 'flex', gap: 18, alignItems: 'center', flexWrap: 'wrap' }}>
              <button
                onClick={() => syncMut.mutate()}
                disabled={syncMut.isPending}
                className="btn-secondary"
                style={{ whiteSpace: 'nowrap' }}
              >
                {syncMut.isPending ? '…' : t('workflows.syncFromTemplate')}
              </button>
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
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 16,
              color: 'var(--color-forest)',
              marginBottom: 8,
            }}
          >
            {t('workflows.noWorkflow')}
          </div>
        )}

        {/* Assign / switch row — visible whether or not a template is set. */}
        <div style={{ marginTop: 18, display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
          <select
            value={selectValue}
            onChange={(e) => setPendingTemplateId(e.target.value)}
            className="input"
            disabled={templates.length === 0 || assignMut.isPending}
          >
            <option value="">{t('workflows.selectTemplate')}</option>
            {templates.map((tmpl) => (
              <option key={tmpl.id} value={tmpl.id}>{tmpl.name}</option>
            ))}
          </select>
          <button
            className="btn-primary"
            disabled={
              !selectValue ||
              assignMut.isPending ||
              Number(selectValue) === templateId
            }
            onClick={() => assignMut.mutate(Number(selectValue))}
          >
            {assignMut.isPending
              ? '…'
              : templateId
                ? t('workflows.switchTemplate')
                : t('workflows.assignTemplate')}
          </button>
          {templates.length === 0 && (
            <Link
              to="/workflows"
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-accent)',
                textDecoration: 'none',
              }}
            >
              → {t('workflows.newTemplate')}
            </Link>
          )}
        </div>
        {assignMut.error && (
          <p style={{ marginTop: 8, fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-accent)' }}>
            {(assignMut.error as Error).message}
          </p>
        )}
        {syncMut.error && (
          <p style={{ marginTop: 8, fontFamily: 'var(--font-mono)', fontSize: 10, color: 'var(--color-accent)' }}>
            {(syncMut.error as Error).message}
          </p>
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
