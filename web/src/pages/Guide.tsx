import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useAuth } from '../auth/AuthContext'
import { Masthead, Rule } from '../components/faltet'

const basicSections = [
  'getting-started',
  'concepts',
  'dashboard',
  'seasons',
  'gardens',
  'species',
  'sowing',
  'plants',
  'tasks',
  'seeds',
  'customers',
  'tutorial',
]

const advancedSections = [
  'successions',
  'targets',
  'calendar',
  'bouquets',
  'trials',
  'pestDisease',
  'analytics',
]

export function Guide() {
  const { t } = useTranslation()
  const { user } = useAuth()
  const [open, setOpen] = useState<string | null>(null)

  function renderSection(id: string) {
    const isOpen = open === id
    return (
      <section key={id} style={{ marginTop: 32 }}>
        <button
          aria-expanded={isOpen}
          onClick={() => setOpen(isOpen ? null : id)}
          style={{
            display: 'flex',
            alignItems: 'baseline',
            gap: 10,
            background: 'transparent',
            border: 'none',
            padding: 0,
            cursor: 'pointer',
            width: '100%',
            textAlign: 'left',
          }}
        >
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
            § {t(`guide.sections.${id}.title`)}
          </div>
          <span
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              color: 'var(--color-accent)',
              marginLeft: 'auto',
            }}
          >
            {isOpen ? '−' : '+'}
          </span>
        </button>
        <div style={{ marginTop: 6 }}>
          <Rule variant="soft" />
        </div>
        {isOpen && (
          <div style={{ marginTop: 14 }}>
            {(t(`guide.sections.${id}.body`, { returnObjects: true }) as string[]).map((p, i) => {
              if (p.startsWith('##'))
                return (
                  <h3
                    key={i}
                    style={{
                      fontFamily: 'var(--font-display)',
                      fontStyle: 'italic',
                      fontSize: 18,
                      fontWeight: 300,
                      marginTop: 16,
                      marginBottom: 4,
                      color: 'var(--color-ink)',
                    }}
                  >
                    {p.replace(/^##\s*/, '')}
                  </h3>
                )
              if (p.startsWith('- '))
                return (
                  <p
                    key={i}
                    style={{
                      fontFamily: 'Georgia, var(--font-display)',
                      fontSize: 16,
                      lineHeight: 1.7,
                      color: 'var(--color-forest)',
                      marginTop: 4,
                      paddingLeft: 16,
                    }}
                  >
                    {p}
                  </p>
                )
              return (
                <p
                  key={i}
                  style={{
                    fontFamily: 'Georgia, var(--font-display)',
                    fontSize: 16,
                    lineHeight: 1.7,
                    color: 'var(--color-forest)',
                    marginTop: 6,
                  }}
                >
                  {p}
                </p>
              )
            })}
          </div>
        )}
      </section>
    )
  }

  return (
    <div>
      <Masthead left={t('nav.guide')} center={t('guide.masthead.center')} />

      <div style={{ maxWidth: 860, margin: '40px auto', padding: '0 40px 80px' }}>
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
          {t('guide.hero.headline')}
        </h1>

        <div style={{ marginTop: 40 }}>
          {basicSections.map(renderSection)}
        </div>

        {user?.advancedMode && (
          <div style={{ marginTop: 48 }}>
            <div
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 9,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
                color: 'var(--color-forest)',
                opacity: 0.5,
                marginBottom: 8,
              }}
            >
              {t('guide.advanced')}
            </div>
            <Rule variant="soft" />
            <div style={{ marginTop: 8 }}>
              {advancedSections.map(renderSection)}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
