import { useTranslation } from 'react-i18next'
import { Rule } from '../components/faltet'

const sections = [
  'dataCollected',
  'dataStorage',
  'thirdParties',
  'retention',
  'yourRights',
  'exercisingRights',
  'cookies',
  'contact',
] as const

export function PrivacyPolicy() {
  const { t } = useTranslation()

  return (
    <div style={{ minHeight: '100vh', background: 'var(--color-cream)' }}>
      {/* Top strip */}
      <div style={{ padding: '22px 40px', borderBottom: '1px solid var(--color-ink)' }}>
        <span
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 32,
            fontWeight: 300,
            color: 'var(--color-ink)',
          }}
        >
          Verdant<span style={{ color: 'var(--color-accent)' }}>.</span>
        </span>
      </div>

      <div style={{ maxWidth: 860, margin: '60px auto', padding: '0 40px 80px' }}>
        <h1
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: 56,
            fontWeight: 300,
            letterSpacing: -1,
            margin: 0,
            fontVariationSettings: '"SOFT" 100, "opsz" 144',
          }}
        >
          {t('privacy.title')}
        </h1>
        <p
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 9,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
            opacity: 0.6,
            marginTop: 12,
          }}
        >
          {t('privacy.lastUpdated')}
        </p>

        {sections.map((id) => (
          <section key={id} style={{ marginTop: 40 }}>
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 9,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-forest)',
                opacity: 0.7,
                marginBottom: 8,
              }}
            >
              § {t(`privacy.sections.${id}.title`)}
            </div>
            <Rule variant="soft" />
            <p
              style={{
                fontFamily: 'Georgia, var(--font-display)',
                fontSize: 16,
                lineHeight: 1.7,
                color: 'var(--color-forest)',
                marginTop: 14,
              }}
            >
              {t(`privacy.sections.${id}.body`)}
            </p>
          </section>
        ))}
      </div>
    </div>
  )
}
