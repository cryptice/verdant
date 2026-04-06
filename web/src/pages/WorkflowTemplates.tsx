import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { PageHeader } from '../components/PageHeader'
import { Dialog } from '../components/Dialog'

export function WorkflowTemplates() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [newName, setNewName] = useState('')
  const [newDesc, setNewDesc] = useState('')
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data: templates, isLoading } = useQuery({
    queryKey: ['workflow-templates'],
    queryFn: api.workflows.templates,
  })

  const createMut = useMutation({
    mutationFn: (data: { name: string; description?: string }) => api.workflows.createTemplate(data),
    onSuccess: (tmpl) => {
      qc.invalidateQueries({ queryKey: ['workflow-templates'] })
      setShowCreate(false)
      setNewName('')
      setNewDesc('')
      navigate(`/workflows/${tmpl.id}/edit`)
    },
  })

  const deleteMut = useMutation({
    mutationFn: (id: number) => api.workflows.deleteTemplate(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['workflow-templates'] })
      setDeleteId(null)
    },
  })

  if (isLoading) return <div className="flex justify-center p-16"><div className="animate-spin h-8 w-8 border-2 border-accent border-t-transparent rounded-full" /></div>

  return (
    <div>
      <PageHeader
        title={t('workflows.title')}
        action={{ label: t('workflows.newTemplate'), onClick: () => setShowCreate(true) }}
      />

      <div className="px-4 space-y-3">
        {templates && templates.length === 0 && (
          <p className="text-text-secondary text-sm text-center py-8">{t('workflows.noTemplates')}</p>
        )}

        {templates?.map(tmpl => (
          <div
            key={tmpl.id}
            className="border border-divider rounded-xl cursor-pointer hover:bg-surface/50 transition-colors"
            onClick={() => navigate(`/workflows/${tmpl.id}/edit`)}
          >
            <div className="flex items-center gap-2 px-3 py-2.5">
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium truncate">{tmpl.name}</p>
                {tmpl.description && (
                  <p className="text-xs text-text-secondary truncate mt-0.5">{tmpl.description}</p>
                )}
                <p className="text-xs text-text-secondary mt-0.5">
                  {t('workflows.stepCount', { count: tmpl.steps.length })}
                </p>
              </div>
              <button
                onClick={(e) => { e.stopPropagation(); setDeleteId(tmpl.id) }}
                className="text-xs text-error px-1 shrink-0"
              >
                {t('common.delete')}
              </button>
            </div>
          </div>
        ))}
      </div>

      {showCreate && (
        <Dialog open={showCreate} title={t('workflows.newTemplate')} onClose={() => setShowCreate(false)}>
          <div className="space-y-3">
            <div>
              <label className="field-label">{t('workflows.templateName')}</label>
              <input
                value={newName}
                onChange={e => setNewName(e.target.value)}
                className="input w-full"
                autoFocus
              />
            </div>
            <div>
              <label className="field-label">{t('workflows.description')}</label>
              <input
                value={newDesc}
                onChange={e => setNewDesc(e.target.value)}
                className="input w-full"
              />
            </div>
            <div className="flex gap-2">
              <button className="btn-secondary flex-1" onClick={() => setShowCreate(false)}>
                {t('common.cancel')}
              </button>
              <button
                className="btn-primary flex-1"
                disabled={!newName.trim() || createMut.isPending}
                onClick={() => createMut.mutate({ name: newName.trim(), description: newDesc.trim() || undefined })}
              >
                {createMut.isPending ? t('common.creating') : t('common.save')}
              </button>
            </div>
          </div>
        </Dialog>
      )}

      {deleteId != null && (
        <Dialog open={true} title={t('workflows.deleteTemplate')} onClose={() => setDeleteId(null)}>
          <p className="text-sm mb-4">{t('workflows.deleteTemplate')}?</p>
          <div className="flex gap-2">
            <button className="btn-secondary flex-1" onClick={() => setDeleteId(null)}>{t('common.cancel')}</button>
            <button
              className="btn-primary flex-1 bg-error"
              onClick={() => deleteMut.mutate(deleteId)}
              disabled={deleteMut.isPending}
            >
              {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
            </button>
          </div>
        </Dialog>
      )}
    </div>
  )
}
