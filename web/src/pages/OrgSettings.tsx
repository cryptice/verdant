import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import type { OrgMemberResponse, OrgInviteResponse } from '../api/client'
import { useOrg } from '../auth/OrgContext'
import { Masthead, Field, Rule } from '../components/faltet'

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
      <Masthead
        left={t('nav.orgSettings')}
        center={t('orgSettings.masthead.center')}
      />

      <div className="page-body" style={{ paddingBottom: 80 }}>

        {/* Org info fields — owners only */}
        {isOwner && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
            <Field
              label={t('org.settings.nameLabel')}
              editable
              value={orgName}
              onChange={setOrgName}
            />
            <Field
              label={t('org.settings.emojiLabel')}
              editable
              value={orgEmoji}
              onChange={setOrgEmoji}
            />
          </div>
        )}

        {isOwner && (
          <div style={{ marginTop: 20, display: 'flex', justifyContent: 'flex-end' }}>
            <button
              onClick={handleSave}
              disabled={updateMut.isPending}
              className="btn-primary"
            >
              {updateMut.isPending ? t('common.saving') : t('org.settings.save')}
            </button>
          </div>
        )}

        {/* Members section */}
        <div style={{ marginTop: 40 }}>
          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              opacity: 0.6,
            }}
          >
            § {t('org.settings.members')}
          </div>
          <div style={{ marginTop: 8 }}>
            <Rule variant="soft" />
          </div>

          {membersQuery.isLoading ? (
            <div style={{ padding: '20px 0', fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, color: 'var(--color-forest)' }}>
              …
            </div>
          ) : (
            <div style={{ marginTop: 14 }}>
              {members.map((member) => (
                <div
                  key={member.id}
                  style={{
                    display: 'grid',
                    gridTemplateColumns: '1.5fr 120px auto',
                    gap: 18,
                    padding: '12px 0',
                    borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    alignItems: 'center',
                  }}
                >
                  <div style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>
                    {member.displayName}
                    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1, color: 'var(--color-forest)', opacity: 0.6 }}>
                      {member.email}
                    </div>
                  </div>
                  <div
                    style={{
                      fontFamily: 'var(--font-mono)',
                      fontSize: 9,
                      letterSpacing: 1.4,
                      textTransform: 'uppercase',
                      color: 'var(--color-forest)',
                      opacity: 0.7,
                    }}
                  >
                    {t(`org.settings.role.${member.role}`, member.role)}
                  </div>
                  <div>
                    {isOwner && member.role !== 'OWNER' && (
                      <button
                        onClick={() => removeMut.mutate(member.userId)}
                        disabled={removeMut.isPending}
                        style={{
                          background: 'transparent',
                          border: 'none',
                          fontFamily: 'var(--font-mono)',
                          fontSize: 9,
                          letterSpacing: 1.4,
                          textTransform: 'uppercase',
                          color: 'var(--color-accent)',
                          cursor: 'pointer',
                          padding: 0,
                          opacity: removeMut.isPending ? 0.5 : 1,
                        }}
                      >
                        ↵ {t('org.settings.removeMember')}
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
          <div style={{ marginTop: 40 }}>
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 9,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-forest)',
                opacity: 0.6,
              }}
            >
              § {t('org.settings.inviteTitle')}
            </div>
            <div style={{ marginTop: 8 }}>
              <Rule variant="soft" />
            </div>
            <div style={{ marginTop: 14, display: 'flex', gap: 12 }}>
              <input
                type="email"
                value={inviteEmail}
                onChange={e => setInviteEmail(e.target.value)}
                onKeyDown={e => e.key === 'Enter' && handleInvite()}
                placeholder={t('org.settings.inviteLabel')}
                className="input"
                style={{ flex: 1 }}
              />
              <button
                onClick={handleInvite}
                disabled={inviteMut.isPending || !inviteEmail.trim()}
                className="btn-primary"
              >
                {inviteMut.isPending ? t('common.saving') : t('org.settings.inviteButton')}
              </button>
            </div>

            {sentInvites.length > 0 && (
              <div style={{ marginTop: 14 }}>
                <div
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-forest)',
                    opacity: 0.6,
                    marginBottom: 8,
                  }}
                >
                  {t('org.settings.pendingInvites')}
                </div>
                {sentInvites.map(invite => (
                  <div
                    key={invite.id}
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      padding: '10px 0',
                      borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                    }}
                  >
                    <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{invite.email}</span>
                    <span
                      style={{
                        fontFamily: 'var(--font-mono)',
                        fontSize: 9,
                        letterSpacing: 1.4,
                        textTransform: 'uppercase',
                        color: 'var(--color-forest)',
                        opacity: 0.6,
                      }}
                    >
                      {invite.status.toLowerCase()}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}
