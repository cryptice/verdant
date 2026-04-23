import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Field } from '../components/faltet'
import { OnboardingHint } from '../onboarding/OnboardingHint'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

export function GardenForm() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [emoji, setEmoji] = useState('')

  const mutation = useMutation({
    mutationFn: () => api.gardens.create({ name, description: description || undefined, emoji: emoji || undefined }),
    onSuccess: (g) => {
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      navigate(`/garden/${g.id}`, { replace: true })
    },
  })

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.gardens')} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>{t('garden.newGardenTitle')}</span>
          </span>
        }
        center={t('form.masthead.center')}
      />

      <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
        <OnboardingHint />

        <div data-onboarding="garden-form">
          {/* Icon picker */}
          <div style={{ marginBottom: 20 }}>
            <span style={{ display: 'block', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 8 }}>
              {t('common.iconLabel')}
            </span>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: 4 }}>
              {GARDEN_ICONS.map((icon) => (
                <button
                  key={icon}
                  type="button"
                  onClick={() => setEmoji(emoji === icon ? '' : icon)}
                  style={{
                    fontSize: 22,
                    padding: 4,
                    background: emoji === icon ? 'var(--color-paper)' : 'transparent',
                    border: `1px solid ${emoji === icon ? 'var(--color-ink)' : 'transparent'}`,
                    cursor: 'pointer',
                    lineHeight: 1,
                  }}
                >
                  {icon}
                </button>
              ))}
            </div>
          </div>

          <div style={{ display: 'grid', gap: 20 }}>
            <Field
              label={t('common.nameLabel')}
              editable
              value={name}
              onChange={setName}
              placeholder={t('garden.gardenNamePlaceholder')}
            />
            <Field
              label={t('common.descriptionLabel')}
              editable
              value={description}
              onChange={setDescription}
            />
          </div>
        </div>

        {mutation.error && (
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-error)', marginTop: 16 }}>
            {mutation.error instanceof Error ? mutation.error.message : String(mutation.error)}
          </p>
        )}
      </div>

      {/* Sticky footer */}
      <div style={{ position: 'sticky', bottom: 0, background: 'var(--color-cream)', borderTop: '1px solid var(--color-ink)', padding: '14px 40px', display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
        <button className="btn-secondary" onClick={() => navigate('/')}>
          {t('common.cancel')}
        </button>
        <button
          className="btn-primary"
          onClick={() => mutation.mutate()}
          disabled={!name.trim() || mutation.isPending}
        >
          {mutation.isPending ? t('garden.creatingGarden') : t('garden.createGarden')}
        </button>
      </div>
    </div>
  )
}
