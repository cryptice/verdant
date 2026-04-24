import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, downloadDataExport } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { Masthead } from '../components/faltet'
import { Dialog } from '../components/Dialog'
import { useOnboarding } from '../onboarding/OnboardingContext'

export function Account() {
  const { user, logout, refreshUser } = useAuth()
  const { t, i18n } = useTranslation()
  const { enabled: onboardingEnabled, setEnabled: setOnboardingEnabled, setDrawerOpen: setOnboardingDrawerOpen } = useOnboarding()
  const [showDelete, setShowDelete] = useState(false)

  const advancedModeMut = useMutation({
    mutationFn: (enabled: boolean) => api.user.update({ advancedMode: enabled }),
    onSuccess: () => refreshUser(),
  })

  const deleteMut = useMutation({
    mutationFn: () => api.user.delete(),
    onSuccess: logout,
  })

  const exportMut = useMutation({
    mutationFn: () => downloadDataExport(),
  })

  if (!user) return null

  return (
    <div>
      <Masthead
        left={t('nav.account')}
        center={t('account.masthead.center')}
      />

      <div className="page-body" style={{ paddingBottom: 80 }}>
        {/* User info row */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 16, marginBottom: 40 }}>
          {user.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt={user.displayName}
              style={{ width: 56, height: 56, borderRadius: '50%', border: '1px solid var(--color-ink)' }}
            />
          ) : (
            <div
              style={{
                width: 56,
                height: 56,
                borderRadius: '50%',
                background: 'var(--color-paper)',
                border: '1px solid var(--color-ink)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontFamily: 'var(--font-display)',
                fontSize: 22,
              }}
            >
              {user.displayName.charAt(0)}
            </div>
          )}
          <div>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 22, fontWeight: 300 }}>{user.displayName}</div>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.7, marginTop: 2 }}>{user.email}</div>
          </div>
        </div>

        {/* Settings grid */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
          <div>
            <span
              style={{
                display: 'block',
                fontFamily: 'var(--font-mono)',
                fontSize: 9,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-forest)',
                opacity: 0.7,
                marginBottom: 8,
              }}
            >
              {t('account.advancedMode')}
            </span>
            <label style={{ display: 'flex', gap: 10, alignItems: 'center', cursor: 'pointer' }}>
              <input
                type="checkbox"
                checked={user.advancedMode}
                onChange={() => advancedModeMut.mutate(!user.advancedMode)}
                disabled={advancedModeMut.isPending}
              />
              <span style={{ fontFamily: 'var(--font-display)', fontSize: 16, fontStyle: 'italic' }}>
                {t('account.advancedModeDescription')}
              </span>
            </label>
          </div>
          <div>
            <span
              style={{
                display: 'block',
                fontFamily: 'var(--font-mono)',
                fontSize: 9,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-forest)',
                opacity: 0.7,
                marginBottom: 8,
              }}
            >
              {t('language.label')}
            </span>
            <select
              value={i18n.language}
              onChange={(e) => { i18n.changeLanguage(e.target.value); localStorage.setItem('verdant-lang', e.target.value) }}
              className="input"
            >
              <option value="sv">Svenska</option>
              <option value="en">English</option>
            </select>
          </div>
        </div>

        {/* Onboarding toggle */}
        <div style={{ marginTop: 28 }}>
          <span
            style={{
              display: 'block',
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              opacity: 0.7,
              marginBottom: 8,
            }}
          >
            {t('account.onboarding.label')}
          </span>
          {onboardingEnabled ? (
            <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap' }}>
              <span style={{ fontFamily: 'var(--font-display)', fontSize: 16, fontStyle: 'italic' }}>
                {t('account.onboarding.enabled')}
              </span>
              <button
                onClick={() => setOnboardingDrawerOpen(true)}
                className="btn-secondary"
              >
                {t('account.onboarding.open')}
              </button>
              <button
                onClick={() => setOnboardingEnabled(false)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  fontFamily: 'var(--font-mono)',
                  fontSize: 10,
                  letterSpacing: 1.4,
                  textTransform: 'uppercase',
                  color: 'var(--color-forest)',
                  opacity: 0.7,
                  cursor: 'pointer',
                  padding: 0,
                }}
              >
                {t('account.onboarding.disable')}
              </button>
            </div>
          ) : (
            <button onClick={() => setOnboardingEnabled(true)} className="btn-secondary">
              {t('account.onboarding.enable')}
            </button>
          )}
        </div>

        {/* Actions row */}
        <div style={{ marginTop: 40, display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <button onClick={() => logout()} className="btn-secondary">
            {t('account.signOut')}
          </button>
          <button
            onClick={() => exportMut.mutate()}
            disabled={exportMut.isPending}
            className="btn-secondary"
          >
            {exportMut.isPending ? t('account.exportingData') : t('account.downloadMyData')}
          </button>
          <Link
            to="/privacy"
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              textDecoration: 'none',
            }}
          >
            {t('privacy.title')}
          </Link>
        </div>

        {/* Farozon callout */}
        <div
          style={{
            marginTop: 60,
            padding: '22px 28px',
            border: '1px solid color-mix(in srgb, var(--color-accent) 40%, transparent)',
          }}
        >
          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              marginBottom: 10,
            }}
          >
            {t('common.dangerZone')}
          </div>
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, margin: 0 }}>
            {t('account.deleteAccountConfirm')}
          </p>
          <button
            onClick={() => setShowDelete(true)}
            style={{
              marginTop: 10,
              background: 'transparent',
              border: 'none',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              padding: 0,
            }}
          >
            → {t('account.deleteAccount')}
          </button>
        </div>
      </div>

      <Dialog
        open={showDelete}
        onClose={() => setShowDelete(false)}
        title={t('account.deleteAccountTitle')}
        actions={
          <>
            <button onClick={() => setShowDelete(false)} className="px-4 py-2 text-sm text-text-secondary">
              {t('common.cancel')}
            </button>
            <button onClick={() => deleteMut.mutate()} className="px-4 py-2 text-sm text-error font-semibold">
              {deleteMut.isPending ? t('common.deleting') : t('common.delete')}
            </button>
          </>
        }
      >
        <p className="text-text-secondary">{t('account.deleteAccountConfirm')}</p>
      </Dialog>
    </div>
  )
}
