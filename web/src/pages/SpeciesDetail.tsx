import { useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Masthead } from '../components/faltet'
import { SpeciesEditForm } from '../components/faltet/SpeciesEditForm'

export function SpeciesDetail() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  return (
    <div>
      <Masthead left={t('species.masthead.left')} center={t('species.masthead.center')} />
      <SpeciesEditForm speciesId={Number(id)} />
    </div>
  )
}
