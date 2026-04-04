import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { OrgMemberResponse, OrgInviteResponse } from '../api/client'
import { useOrg } from '../auth/OrgContext'
import { PageHeader } from '../components/PageHeader'

export function OrgSettings() {
  const { t } = useTranslation()
  const { activeOrg } = useOrg()
  const queryClient = useQueryClient()

  const orgId = activeOrg?.orgId ?? 0
  const isOwner = activeOrg?.role === 'OWNER'

  const [orgName, setOrgName] = useState(activeOrg?.orgName ?? '')
  const [orgEmoji, setOrgEmoji] = useState(activeOrg?.orgEmoji ?? '')
  const [inviteEmail, setInviteEmail] = useState('')
  const [sentInvites, setSentInvites] = useState<OrgInviteResponse[]>([])

  const membersQuery = useQuery({
    queryKey: ['org-members', orgId],
    queryFn: () => api.organizations.members(orgId),
    enabled: orgId > 0,
  })

  const updateMut = useMutation({
    mutationFn: (data: { name?: string; emoji?: string }) =>
      api.organizations.update(orgId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['org-members', orgId] })
    },
  })

  const inviteMut = useMutation({
    mutationFn: (email: string) => api.organizations.invite(orgId, email),
    onSuccess: (invite) => {
      setSentInvites(prev => [invite, ...prev])
      setInviteEmail('')
    },
  })

  const removeMut = useMutation({
    mutationFn: (userId: number) => api.organizations.removeMember(orgId, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['org-members', orgId] })
    },
  })

  if (!activeOrg) return null

  const members: OrgMemberResponse[] = membersQuery.data ?? []

  const handleSave = () => {
    updateMut.mutate({ name: orgName || undefined, emoji: orgEmoji || undefined })
  }

  const handleInvite = () => {
    const email = inviteEmail.trim()
    if (!email) return
    inviteMut.mutate(email)
  }

  return (
    <div>
      <PageHeader title={t('org.settings.title')} />
      <div className="px-4 py-4 space-y-6 max-w-2xl">

        {/* Org name and emoji — owners only */}
        {isOwner && (
          <div className="card space-y-4">
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1">
                  {t('org.settings.nameLabel')}
                </label>
                <input
                  type="text"
                  value={orgName}
                  onChange={e => setOrgName(e.target.value)}
                  className="input w-full"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1">
                  {t('org.settings.emojiLabel')}
                </label>
                <input
                  type="text"
                  value={orgEmoji}
                  onChange={e => setOrgEmoji(e.target.value)}
                  className="input w-20"
                  maxLength={4}
                />
              </div>
            </div>
            <button
              onClick={handleSave}
              disabled={updateMut.isPending}
              className="btn-primary"
            >
              {updateMut.isPending ? t('common.saving') : t('org.settings.save')}
            </button>
          </div>
        )}

        {/* Members list */}
        <div className="card">
          <h2 className="text-sm font-semibold text-text-primary mb-3">{t('org.settings.members')}</h2>
          {membersQuery.isLoading ? (
            <div className="flex justify-center py-4">
              <div className="animate-spin h-5 w-5 border-2 border-accent border-t-transparent rounded-full" />
            </div>
          ) : (
            <div className="divide-y divide-divider/50">
              {members.map(member => (
                <div key={member.id} className="flex items-center gap-3 py-3">
                  {member.avatarUrl ? (
                    <img src={member.avatarUrl} alt={member.displayName} className="w-8 h-8 rounded-full flex-shrink-0" />
                  ) : (
                    <div className="w-8 h-8 rounded-full bg-accent/15 flex items-center justify-center text-sm font-semibold text-accent flex-shrink-0">
                      {member.displayName.charAt(0)}
                    </div>
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-text-primary truncate">{member.displayName}</p>
                    <p className="text-xs text-text-secondary truncate">{member.email}</p>
                  </div>
                  <div className="flex items-center gap-3 flex-shrink-0">
                    <span className="text-xs text-text-muted">
                      {t(`org.settings.role.${member.role}`, member.role)}
                    </span>
                    {isOwner && member.role !== 'OWNER' && (
                      <button
                        onClick={() => removeMut.mutate(member.userId)}
                        disabled={removeMut.isPending}
                        className="text-xs text-error hover:underline disabled:opacity-50"
                      >
                        {t('org.settings.removeMember')}
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Invite section — owners only */}
        {isOwner && (
          <div className="card space-y-4">
            <h2 className="text-sm font-semibold text-text-primary">{t('org.settings.inviteTitle')}</h2>
            <div className="flex gap-2">
              <input
                type="email"
                value={inviteEmail}
                onChange={e => setInviteEmail(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleInvite()}
                placeholder={t('org.settings.inviteLabel')}
                className="input flex-1"
              />
              <button
                onClick={handleInvite}
                disabled={inviteMut.isPending || !inviteEmail.trim()}
                className="btn-primary flex-shrink-0"
              >
                {inviteMut.isPending ? t('common.saving') : t('org.settings.inviteButton')}
              </button>
            </div>

            {sentInvites.length > 0 && (
              <div>
                <p className="text-xs font-medium text-text-secondary mb-2">{t('org.settings.pendingInvites')}</p>
                <div className="divide-y divide-divider/50">
                  {sentInvites.map(invite => (
                    <div key={invite.id} className="py-2 flex items-center justify-between">
                      <span className="text-sm text-text-primary">{invite.email}</span>
                      <span className="text-xs text-text-muted capitalize">{invite.status.toLowerCase()}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
