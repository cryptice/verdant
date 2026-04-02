import { useTranslation } from 'react-i18next'

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
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-text-primary mb-1">{t('privacy.title')}</h1>
      <p className="text-sm text-text-muted mb-8">{t('privacy.lastUpdated')}</p>

      <div className="space-y-8">
        {sections.map(id => (
          <div key={id}>
            <h2 className="text-base font-semibold text-text-primary mb-2">
              {t(`privacy.sections.${id}.title`)}
            </h2>
            <p className="text-sm text-text-secondary leading-relaxed">
              {t(`privacy.sections.${id}.body`)}
            </p>
          </div>
        ))}
      </div>
    </div>
  )
}
