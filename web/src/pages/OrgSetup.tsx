import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
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

  const [orgName, setOrgName] = useState('')
  const [orgEmoji, setOrgEmoji] = useState(GARDEN_ICONS[0])

  const { data: invites, refetch: refetchInvites } = useQuery({
    queryKey: ['pending-invites'],
    queryFn: api.invites.pending,
  })

  const createMutation = useMutation({
    mutationFn: () => api.organizations.create({ name: orgName.trim(), emoji: orgEmoji || undefined }),
    onSuccess: () => refreshUser(),
  })

  const acceptMutation = useMutation({
    mutationFn: (id: number) => api.invites.accept(id),
    onSuccess: () => refreshUser(),
  })

  const declineMutation = useMutation({
    mutationFn: (id: number) => api.invites.decline(id),
    onSuccess: () => refetchInvites(),
  })

  const hasInvites = invites && invites.length > 0

  return (
    <div className="min-h-screen bg-surface flex items-start justify-center pt-16 px-4">
      <div className="w-full max-w-3xl">
        <div className="text-center mb-10">
          <span className="text-5xl">🌱</span>
          <h1 className="text-2xl font-semibold mt-4">{t('org.setup.title')}</h1>
          <p className="text-text-secondary text-sm mt-2">{t(hasInvites ? 'org.setup.subtitleWithInvites' : 'org.setup.subtitle')}</p>
        </div>

        <div className={`grid gap-6 ${hasInvites ? 'md:grid-cols-2' : ''}`}>
          {hasInvites && (
            <div className="space-y-3">
              <h2 className="font-semibold text-text-primary">{t('org.setup.pendingInvites')}</h2>
              {invites.map(invite => (
                <div key={invite.id} className="card rounded-2xl space-y-3">
                  <div className="flex items-center gap-3">
                    <span className="text-3xl">{invite.orgName.charAt(0)}</span>
                    <div className="min-w-0">
                      <p className="font-semibold truncate">{invite.orgName}</p>
                      <p className="text-sm text-text-secondary">
                        {t('org.setup.invitedBy', { name: invite.invitedByName })}
                      </p>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      onClick={() => acceptMutation.mutate(invite.id)}
                      disabled={acceptMutation.isPending || declineMutation.isPending}
                      className="btn-primary flex-1 text-sm"
                    >
                      {acceptMutation.isPending ? t('org.setup.accepting') : t('org.setup.accept')}
                    </button>
                    <button
                      onClick={() => declineMutation.mutate(invite.id)}
                      disabled={acceptMutation.isPending || declineMutation.isPending}
                      className="px-4 py-2 text-sm text-text-secondary hover:text-text-primary transition-colors"
                    >
                      {t('org.setup.decline')}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="card rounded-2xl">
            <h2 className="font-semibold text-text-primary mb-4">{t('org.setup.createTitle')}</h2>
            <div className="space-y-4">
              <div>
                <label className="field-label">{t('common.iconLabel')}</label>
                <div className="grid grid-cols-8 gap-1 p-2 bg-surface rounded-md border border-divider">
                  {GARDEN_ICONS.map(icon => (
                    <button
                      key={icon}
                      type="button"
                      onClick={() => setOrgEmoji(orgEmoji === icon ? '' : icon)}
                      className={`text-xl p-1.5 rounded-md transition-colors leading-none ${
                        orgEmoji === icon
                          ? 'bg-accent-light ring-1 ring-accent'
                          : 'hover:bg-divider'
                      }`}
                    >
                      {icon}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="field-label">{t('common.nameLabel')}</label>
                <input
                  value={orgName}
                  onChange={e => setOrgName(e.target.value)}
                  placeholder={t('org.setup.namePlaceholder')}
                  className="input w-full"
                />
              </div>
              {createMutation.error && (
                <p className="text-error text-sm">
                  {createMutation.error instanceof Error
                    ? createMutation.error.message
                    : String(createMutation.error)}
                </p>
              )}
              <button
                onClick={() => createMutation.mutate()}
                disabled={!orgName.trim() || createMutation.isPending}
                className="btn-primary w-full"
              >
                {createMutation.isPending ? t('org.setup.creating') : t('org.setup.create')}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
