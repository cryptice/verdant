import { useParams, useNavigate, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
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

      <SpeciesEditForm speciesId={speciesId} />

      <div style={{ padding: '0 28px 28px' }}>
        <Rule variant="soft" />
        <div style={{ marginTop: 22, display: 'flex', justifyContent: 'flex-end' }}>
          {!confirmDelete ? (
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
