import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Ledger } from '../components/faltet'
import { Dialog } from '../components/Dialog'

export function WorkflowTemplates() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [showCreate, setShowCreate] = useState(false)
  const [newName, setNewName] = useState('')
  const [newDesc, setNewDesc] = useState('')
  const [deleteId, setDeleteId] = useState<number | null>(null)

  const { data: templates = [], isLoading } = useQuery({
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
      <Masthead
        left={t('nav.workflows')}
        center="— Arbetsflödesliggaren —"
        right={
          <button onClick={() => setShowCreate(true)} className="btn-primary">
            {t('workflows.newTemplate')}
          </button>
        }
      />

      <div style={{ padding: '28px 40px' }}>
        <Ledger
          columns={[
            {
              key: 'id',
              label: '№',
              width: '60px',
              render: (_w, i) => (
                <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 22, color: 'var(--color-sage)' }}>
                  {String(i + 1).padStart(2, '0')}
                </span>
              ),
            },
            {
              key: 'name',
              label: t('workflows.templateName'),
              width: '1.5fr',
              render: (w) => (
                <div>
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>{w.name}</div>
                  {w.description && (
                    <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-forest)', marginTop: 2 }}>
                      {w.description}
                    </div>
                  )}
                </div>
              ),
            },
            {
              key: 'stepCount',
              label: t('workflows.steps'),
              width: '100px',
              align: 'right',
              render: (w) => (
                <span style={{ fontVariantNumeric: 'tabular-nums' }}>{w.steps.length}</span>
              ),
            },
            {
              key: 'delete',
              label: '',
              width: '80px',
              align: 'right',
              render: (w) => (
                <button
                  onClick={(e) => { e.stopPropagation(); setDeleteId(w.id) }}
                  className="text-xs text-error"
                  style={{ background: 'transparent', border: 'none', cursor: 'pointer' }}
                >
                  {t('common.delete')}
                </button>
              ),
            },
            {
              key: 'goto',
              label: '',
              width: '40px',
              align: 'right',
              render: () => (
                <span style={{ color: 'var(--color-sage)', fontFamily: 'var(--font-mono)' }}>→</span>
              ),
            },
          ]}
          rows={templates}
          rowKey={(w) => w.id}
          onRowClick={(w) => navigate(`/workflows/${w.id}/edit`)}
        />
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
