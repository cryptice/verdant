import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { GardenSummary } from '../api/client'

export function GardenBedsPrompt({ gardens }: { gardens: GardenSummary[] | undefined }) {
  const { t } = useTranslation()
  const garden = gardens?.length === 1 ? gardens[0] : undefined
  if (!garden || garden.bedCount !== 0) return null

  return (
    <section className="mx-4 mt-4 border border-accent bg-surface p-5 sm:mx-10" aria-label={t('garden.addBed')}>
      <h2 className="text-xl">{t('dashboard.addBedsTitle', { name: garden.name })}</h2>
      <p className="mt-2 text-text-secondary">{t('dashboard.addBedsHint')}</p>
      <Link className="btn-primary mt-4 inline-block" to={`/garden/${garden.id}/bed/new`}>
        {t('garden.addBed')}
      </Link>
    </section>
  )
}
