import { useId, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SeedInventoryResponse, type SpeciesResponse } from '../api/client'
import { Dialog } from './Dialog'
import { SpeciesAutocomplete } from './SpeciesAutocomplete'
import { useOnboarding } from '../onboarding/OnboardingContext'

const UNIT_TYPES = ['SEED', 'PLUG', 'BULB', 'TUBER', 'PLANT']

const roundCents = (value: number) => Math.round(value * 100) / 100

interface Props {
  /** The batch to edit; leave out to add a new one. */
  batch?: SeedInventoryResponse
  /** Preselects the species when adding, e.g. from a deep link. */
  initialSpecies?: SpeciesResponse | null
  species?: SpeciesResponse[]
  onClose: () => void
  onDelete?: (batch: SeedInventoryResponse) => void
}

/**
 * Adding and editing a seed batch share one form. A new batch is entered as
 * packages × size; an existing one shows what is left, since sowing draws it
 * down below whole packages. The package size is what prices a package.
 */
export function SeedBatchDialog({ batch, initialSpecies = null, species, onClose, onDelete }: Props) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const { completeStep } = useOnboarding()
  const id = useId()
  const isEdit = batch != null
  const batchSpecies = isEdit ? species?.find(s => s.id === batch.speciesId) ?? null : null

  const [selectedSpecies, setSelectedSpecies] = useState<SpeciesResponse | null>(initialSpecies)
  const providerSpecies = isEdit ? batchSpecies : selectedSpecies
  const [providerId, setProviderId] = useState<number | ''>(
    isEdit ? batch.speciesProviderId ?? '' : initialSpecies?.providers[0]?.id ?? '',
  )
  const [unitType, setUnitType] = useState(isEdit ? batch.unitType ?? '' : initialSpecies?.defaultUnitType ?? 'SEED')
  const [perPackage, setPerPackage] = useState(batch?.seedsPerPackage != null ? String(batch.seedsPerPackage) : '')
  const [packages, setPackages] = useState('')
  const [remaining, setRemaining] = useState(isEdit ? String(batch.quantity) : '')
  const [collection, setCollection] = useState(batch?.collectionDate ?? '')
  const [expiration, setExpiration] = useState(batch?.expirationDate ?? '')
  const [costUnit, setCostUnit] = useState(batch?.costPerUnitCents != null ? String(batch.costPerUnitCents / 100) : '')
  const [costPackage, setCostPackage] = useState(
    batch?.costPerUnitCents != null && batch.seedsPerPackage
      ? String(roundCents(batch.costPerUnitCents * batch.seedsPerPackage / 100))
      : '',
  )

  const per = Number(perPackage) || 0
  const quantity = isEdit ? Number(remaining) : per * (Number(packages) || 0)
  const unitLabel = t(`unitTypes.${unitType || 'SEED'}`).toLowerCase()

  const changePerPackage = (value: string) => {
    setPerPackage(value)
    const size = Number(value) || 0
    if (!size) setCostPackage('')
    else if (costPackage) setCostUnit(String(roundCents(Number(costPackage) / size)))
  }
  const changeCostUnit = (value: string) => {
    setCostUnit(value)
    setCostPackage(value && per ? String(roundCents(Number(value) * per)) : '')
  }
  const changeCostPackage = (value: string) => {
    setCostPackage(value)
    setCostUnit(value && per ? String(roundCents(Number(value) / per)) : '')
  }

  const saveMut = useMutation({
    mutationFn: () => {
      const fields = {
        quantity,
        collectionDate: collection || undefined,
        expirationDate: expiration || undefined,
        costPerUnitCents: costUnit ? Math.round(Number(costUnit) * 100) : undefined,
        unitType: unitType || undefined,
        speciesProviderId: providerId !== '' ? providerId : undefined,
        seedsPerPackage: per || undefined,
      }
      return isEdit
        ? api.inventory.update(batch.id, fields)
        : api.inventory.create({ speciesId: selectedSpecies!.id, ...fields })
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      if (!isEdit) completeStep('add_seeds')
      onClose()
    },
  })

  const valid = isEdit
    ? remaining !== '' && !!unitType
    : !!selectedSpecies && per > 0 && Number(packages) > 0 && !!unitType
  const packagesLeft = isEdit && per ? quantity / per : null

  return (
    <Dialog
      open
      onClose={onClose}
      title={t(isEdit ? 'seeds.editSeedsTitle' : 'seeds.addSeedsTitle')}
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button onClick={() => saveMut.mutate()} disabled={!valid || saveMut.isPending} className="btn-primary text-sm">
            {isEdit
              ? (saveMut.isPending ? t('common.saving') : t('common.save'))
              : (saveMut.isPending ? t('species.adding') : t('common.add'))}
          </button>
        </>
      }
    >
      <div data-onboarding={isEdit ? undefined : 'seed-form'} className="space-y-4">
        <div>
          <label className="field-label">{t('common.speciesLabel')}</label>
          {isEdit ? (
            <p className="text-sm text-text-primary py-2">{batch.speciesName}</p>
          ) : (
            <SpeciesAutocomplete value={selectedSpecies} onChange={s => {
              setSelectedSpecies(s)
              setProviderId(s?.providers[0]?.id ?? '')
              setUnitType(s?.defaultUnitType ?? 'SEED')
            }} />
          )}
        </div>
        <div>
          <label htmlFor={`${id}-provider`} className="field-label">{t('seeds.provider')}</label>
          <select
            id={`${id}-provider`}
            value={providerId}
            onChange={e => setProviderId(e.target.value ? Number(e.target.value) : '')}
            disabled={!providerSpecies}
            className="input w-full"
          >
            <option value="">{providerSpecies ? t('common.none') : t('common.select')}</option>
            {providerSpecies?.providers.map(p => (
              <option key={p.id} value={p.id}>{p.providerName}</option>
            ))}
          </select>
        </div>
        <div className="grid grid-cols-3 gap-3">
          <div>
            <label htmlFor={`${id}-per`} className="field-label">
              {t(isEdit ? 'seeds.perPackageEditLabel' : 'seeds.perPackageLabel')}
            </label>
            <input id={`${id}-per`} type="number" min="1" value={perPackage} onChange={e => changePerPackage(e.target.value)} placeholder="e.g. 50" className="input w-full" />
          </div>
          {isEdit ? (
            <div>
              <label htmlFor={`${id}-remaining`} className="field-label">{t('seeds.remainingLabel')}</label>
              <input id={`${id}-remaining`} type="number" min="0" value={remaining} onChange={e => setRemaining(e.target.value)} className="input w-full" />
            </div>
          ) : (
            <div>
              <label htmlFor={`${id}-packages`} className="field-label">{t('seeds.packagesLabel')}</label>
              <input id={`${id}-packages`} type="number" min="0" value={packages} onChange={e => setPackages(e.target.value)} placeholder="e.g. 2" className="input w-full" />
            </div>
          )}
          <div>
            <label htmlFor={`${id}-unit`} className="field-label">{t('seeds.unitType')} *</label>
            <select id={`${id}-unit`} value={unitType} onChange={e => setUnitType(e.target.value)} className="input w-full">
              <option value="">{t('common.select')}</option>
              {UNIT_TYPES.map(ut => (
                <option key={ut} value={ut}>{t(`unitTypes.${ut}`)}</option>
              ))}
            </select>
          </div>
        </div>
        {isEdit ? (
          packagesLeft != null && (
            <p className="text-xs text-text-secondary -mt-2">
              {t('seeds.packagesLeft', { count: roundCents(packagesLeft) })}
            </p>
          )
        ) : (
          <p className="text-xs text-text-secondary -mt-2">
            {t('seeds.totalLabel')}: {quantity} {unitLabel}
          </p>
        )}
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor={`${id}-collection`} className="field-label">{t('seeds.collectionDate')}</label>
            <input id={`${id}-collection`} type="date" value={collection} onChange={e => setCollection(e.target.value)} className="input w-full" />
          </div>
          <div>
            <label htmlFor={`${id}-expiration`} className="field-label">{t('seeds.expirationDate')}</label>
            <input id={`${id}-expiration`} type="date" value={expiration} onChange={e => setExpiration(e.target.value)} className="input w-full" />
          </div>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label htmlFor={`${id}-cost-unit`} className="field-label">{t('seeds.costPerUnit')}</label>
            <input id={`${id}-cost-unit`} type="number" value={costUnit} onChange={e => changeCostUnit(e.target.value)} placeholder="e.g. 2" className="input w-full" />
          </div>
          <div>
            <label htmlFor={`${id}-cost-package`} className="field-label">{t('seeds.costPerPackage')}</label>
            <input id={`${id}-cost-package`} type="number" value={costPackage} onChange={e => changeCostPackage(e.target.value)} disabled={!per} placeholder="e.g. 100" className="input w-full" />
          </div>
        </div>
        <p className="text-xs text-text-secondary -mt-2">{t('seeds.costHint')}</p>
        {isEdit && onDelete && (
          <button onClick={() => onDelete(batch)} className="text-sm text-error hover:underline">
            {t('seeds.deleteBatch')}
          </button>
        )}
        {saveMut.error && (
          <p className="text-error text-sm mt-2">
            {saveMut.error instanceof Error ? saveMut.error.message : String(saveMut.error)}
          </p>
        )}
      </div>
    </Dialog>
  )
}
