import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, setActiveOrgId } from '../api/client'
import { useAuth } from '../auth/AuthContext'

const GARDEN_ICONS = [
  '🌱', '🌿', '🌾', '🌻', '🌸', '🌺', '🌼', '🍀',
  '🌲', '🌳', '🌴', '🎋', '🪴', '🍃', '🍂', '🍁',
  '🥬', '🥦', '🧅', '🧄', '🍅', '🫑', '🥕', '🌽',
  '🍓', '🫐', '🍇', '🍎', '🍋', '🍊', '🫒', '🌰',
]

export function OrgSetup() {
  const { t } = useTranslation()
  const { refreshUser } = useAuth()
  const navigate = useNavigate()

  const [orgName, setOrgName] = useState('')
  const [orgEmoji, setOrgEmoji] = useState(GARDEN_ICONS[0])

  const { data: invites, refetch: refetchInvites } = useQuery({
    queryKey: ['pending-invites'],
    queryFn: api.invites.pending,
  })

  const createMutation = useMutation({
    mutationFn: () => api.organizations.create({ name: orgName.trim(), emoji: orgEmoji || undefined }),
    onSuccess: async (org) => {
      setActiveOrgId(org.id)
      await refreshUser()
      navigate('/', { replace: true })
    },
  })

  const acceptMutation = useMutation({
    mutationFn: (id: number) => api.invites.accept(id),
    onSuccess: async (org) => {
      setActiveOrgId(org.id)
      await refreshUser()
      navigate('/', { replace: true })
    },
  })

  const declineMutation = useMutation({
    mutationFn: (id: number) => api.invites.decline(id),
    onSuccess: () => refetchInvites(),
  })

  const hasInvites = invites && invites.length > 0

  return (
    <div style={{ minHeight: '100vh', background: 'var(--color-cream)' }}>
      {/* Editorial top strip */}
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, padding: '22px 40px', borderBottom: '1px solid var(--color-ink)' }}>
        <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 32, fontWeight: 300, color: 'var(--color-ink)' }}>
          Verdant<span style={{ color: 'var(--color-accent)' }}>.</span>
        </span>
        <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: '0.08em', textTransform: 'uppercase', color: 'var(--color-forest)' }}>
          {t('app.subtitle')}
        </span>
      </div>

      <div style={{ maxWidth: 680, margin: '60px auto', padding: '0 24px' }}>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 48, fontWeight: 300, letterSpacing: -1, fontVariationSettings: '"SOFT" 100, "opsz" 144', margin: 0 }}>
          {t('orgSetup.hero.headline')}
        </h1>
        <p style={{ fontFamily: 'Georgia, var(--font-display)', fontSize: 16, lineHeight: 1.6, color: 'var(--color-forest)', marginTop: 22 }}>
          {t('orgSetup.intro')}
        </p>

        <div style={{ marginTop: 48, display: 'grid', gridTemplateColumns: hasInvites ? '1fr 1fr' : '1fr', gap: 32, alignItems: 'start' }}>
          {/* Pending invites */}
          {hasInvites && (
            <div>
              <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6, marginBottom: 16 }}>
                § {t('org.setup.pendingInvites')}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
                {invites.map((invite) => (
                  <div key={invite.id} style={{ border: '1px solid var(--color-ink)', padding: '16px 20px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
                      <span style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 28 }}>
                        {invite.orgName.charAt(0)}
                      </span>
                      <div>
                        <p style={{ fontFamily: 'var(--font-display)', fontSize: 18, fontWeight: 300, margin: 0 }}>{invite.orgName}</p>
                        <p style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6, margin: 0, marginTop: 2 }}>
                          {t('org.setup.invitedBy', { name: invite.invitedByName })}
                        </p>
                      </div>
                    </div>
                    <div style={{ display: 'flex', gap: 8 }}>
                      <button
                        onClick={() => acceptMutation.mutate(invite.id)}
                        disabled={acceptMutation.isPending || declineMutation.isPending}
                        className="btn-primary"
                        style={{ flex: 1 }}
                      >
                        {acceptMutation.isPending ? t('org.setup.accepting') : t('org.setup.accept')}
                      </button>
                      <button
                        onClick={() => declineMutation.mutate(invite.id)}
                        disabled={acceptMutation.isPending || declineMutation.isPending}
                        className="btn-secondary"
                      >
                        {t('org.setup.decline')}
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Create org */}
          <div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6, marginBottom: 16 }}>
              § {t('org.setup.createTitle')}
            </div>

            <div style={{ display: 'grid', gap: 20 }}>
              {/* Emoji picker */}
              <div>
                <span style={{ display: 'block', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 8 }}>
                  {t('common.iconLabel')}
                </span>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(8, 1fr)', gap: 4 }}>
                  {GARDEN_ICONS.map((icon) => (
                    <button
                      key={icon}
                      type="button"
                      onClick={() => setOrgEmoji(orgEmoji === icon ? '' : icon)}
                      style={{
                        fontSize: 20,
                        padding: 4,
                        background: orgEmoji === icon ? 'var(--color-paper)' : 'transparent',
                        border: `1px solid ${orgEmoji === icon ? 'var(--color-ink)' : 'transparent'}`,
                        cursor: 'pointer',
                        lineHeight: 1,
                      }}
                    >
                      {icon}
                    </button>
                  ))}
                </div>
              </div>

              {/* Org name input */}
              <label style={{ display: 'block' }}>
                <span style={{ display: 'block', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginBottom: 4 }}>
                  {t('common.nameLabel')}
                </span>
                <input
                  value={orgName}
                  onChange={(e) => setOrgName(e.target.value)}
                  placeholder={t('org.setup.namePlaceholder')}
                  style={{
                    display: 'block',
                    width: '100%',
                    background: 'transparent',
                    border: 'none',
                    borderBottom: '1px solid var(--color-ink)',
                    borderRadius: 0,
                    padding: '4px 0',
                    fontFamily: 'var(--font-display)',
                    fontSize: 20,
                    fontWeight: 300,
                    color: 'var(--color-ink)',
                    outline: 'none',
                    boxSizing: 'border-box',
                  }}
                />
              </label>

              {createMutation.error && (
                <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-error)', margin: 0 }}>
                  {createMutation.error instanceof Error
                    ? createMutation.error.message
                    : String(createMutation.error)}
                </p>
              )}

              <div style={{ textAlign: 'right' }}>
                <button
                  className="btn-primary"
                  onClick={() => createMutation.mutate()}
                  disabled={!orgName.trim() || createMutation.isPending}
                >
                  {createMutation.isPending ? t('org.setup.creating') : t('orgSetup.submit')}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
