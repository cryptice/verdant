import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Dialog } from './Dialog'

/**
 * The web counterpart of the Android dashboard's bolt button: a floating
 * entry point to start an activity from anywhere on the page. Android also
 * offers "register plants", which has no web screen yet.
 */
export function ActivityFab() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const [open, setOpen] = useState(false)

  const choose = (path: string) => {
    setOpen(false)
    navigate(path)
  }

  return (
    <>
      <button className="activity-fab" aria-label={t('dashboard.activity.fab')} onClick={() => setOpen(true)}>
        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8z" />
        </svg>
      </button>

      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        title={t('dashboard.activity.title')}
        actions={
          <button className="btn-secondary" onClick={() => setOpen(false)}>
            {t('common.cancel')}
          </button>
        }
      >
        <div style={{ display: 'grid', gap: 12 }}>
          <ActivityChoice
            title={t('dashboard.activity.createTask')}
            subtitle={t('dashboard.activity.createTaskHint')}
            onClick={() => choose('/task/new?type=TODO')}
          />
          <ActivityChoice
            title={t('dashboard.activity.sow')}
            subtitle={t('dashboard.activity.sowHint')}
            onClick={() => choose('/sow')}
          />
        </div>
      </Dialog>
    </>
  )
}

function ActivityChoice({ title, subtitle, onClick }: { title: string; subtitle: string; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'block',
        width: '100%',
        textAlign: 'left',
        padding: '14px 16px',
        border: '1px solid var(--color-ink)',
        borderRadius: 12,
        background: 'transparent',
        cursor: 'pointer',
      }}
    >
      <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 20, color: 'var(--color-ink)' }}>
        {title}
      </div>
      <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-forest)' }}>
        {subtitle}
      </div>
    </button>
  )
}
