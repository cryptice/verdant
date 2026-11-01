import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Field } from '../faltet'

const toNumber = (v: string) => parseFloat(v.replace(',', '.'))
const money = (v: number) => String(Math.round(v * 100) / 100)

/**
 * Price per seed and per package, linked through the package size like the seed batch dialog: changing
 * one price fills in the other. The price per seed is what the variety stores.
 */
export function PackagePriceFields({ seedsPerPackage, costPerSeedSek, onSeedsPerPackage, onCostPerSeed }: {
  seedsPerPackage: string
  costPerSeedSek: string
  onSeedsPerPackage: (v: string) => void
  onCostPerSeed: (v: string) => void
}) {
  const { t } = useTranslation()
  const size = parseInt(seedsPerPackage, 10) || 0
  const perSeed = toNumber(costPerSeedSek)
  const [packagePrice, setPackagePrice] = useState(size && !Number.isNaN(perSeed) ? money(perSeed * size) : '')

  const changeSize = (value: string) => {
    const digits = value.replace(/\D/g, '')
    onSeedsPerPackage(digits)
    const next = parseInt(digits, 10) || 0
    if (!next) setPackagePrice('')
    else if (packagePrice) onCostPerSeed(money(toNumber(packagePrice) / next))
  }
  const changePerSeed = (value: string) => {
    const clean = value.replace(/[^\d.,]/g, '')
    onCostPerSeed(clean)
    setPackagePrice(clean && size ? money(toNumber(clean) * size) : '')
  }
  const changePackagePrice = (value: string) => {
    const clean = value.replace(/[^\d.,]/g, '')
    setPackagePrice(clean)
    if (clean && size) onCostPerSeed(money(toNumber(clean) / size))
  }

  return (
    <>
      <Field label={t('speciesForm.seedsPerPackage')} editable value={seedsPerPackage} onChange={changeSize} accent="mustard" />
      <Field label={t('speciesForm.costPerSeedSek')} editable value={costPerSeedSek} onChange={changePerSeed} accent="mustard" />
      <Field label={t('speciesForm.costPerPackageSek')} editable value={size ? packagePrice : ''} onChange={changePackagePrice} accent="mustard" />
    </>
  )
}
