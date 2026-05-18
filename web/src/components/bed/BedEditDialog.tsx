import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type BedResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import { SunDirectionPicker } from '../SunDirectionPicker'
import {
  SOIL_TYPES,
  SUN_EXPOSURES,
  DRAINAGES,
  IRRIGATION_TYPES,
  PROTECTIONS,
} from '../../lib/bed'

export function BedEditDialog({
  open,
  bed,
  onClose,
}: {
  open: boolean
  bed: BedResponse
  onClose: () => void
}) {
  const { t } = useTranslation()
  const qc = useQueryClient()

  // Edit dialog state — populated lazily from the current bed at mount.
  // The parent only mounts this component when the dialog opens, so these
  // initializers run once with the bed snapshot at open time.
  const [editName, setEditName] = useState(bed.name)
  const [editDescription, setEditDescription] = useState(bed.description ?? '')
  const [editLength, setEditLength] = useState(bed.lengthMeters != null ? String(bed.lengthMeters) : '')
  const [editWidth, setEditWidth] = useState(bed.widthMeters != null ? String(bed.widthMeters) : '')
  const [editConditionsOpen, setEditConditionsOpen] = useState(false)
  const [editSoilType, setEditSoilType] = useState(bed.soilType ?? '')
  const [editSoilPh, setEditSoilPh] = useState(bed.soilPh != null ? String(bed.soilPh) : '')
  const [editSunExposure, setEditSunExposure] = useState(bed.sunExposure ?? '')
  const [editSunDirections, setEditSunDirections] = useState<string[]>(bed.sunDirections ?? [])
  const [editDrainage, setEditDrainage] = useState(bed.drainage ?? '')
  const [editIrrigationType, setEditIrrigationType] = useState(bed.irrigationType ?? '')
  const [editProtection, setEditProtection] = useState(bed.protection ?? '')
  const [editRaisedBed, setEditRaisedBed] = useState(bed.raisedBed ?? false)

  const editPhNum = editSoilPh !== '' ? parseFloat(editSoilPh) : undefined
  const editPhOutOfRange = editPhNum !== undefined && (editPhNum < 3.0 || editPhNum > 9.0)

  const updateMut = useMutation({
    mutationFn: () => api.beds.update(bed.id, {
      name: editName,
      description: editDescription || undefined,
      lengthMeters: editLength !== '' ? parseFloat(editLength) : undefined,
      widthMeters: editWidth !== '' ? parseFloat(editWidth) : undefined,
      soilType: editSoilType || undefined,
      soilPh: editPhNum,
      sunExposure: editSunExposure || undefined,
      drainage: editDrainage || undefined,
      sunDirections: editSunDirections.length > 0 ? editSunDirections : undefined,
      irrigationType: editIrrigationType || undefined,
      protection: editProtection || undefined,
      raisedBed: editRaisedBed,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bed', bed.id] })
      qc.invalidateQueries({ queryKey: ['garden-beds', bed.gardenId] })
      onClose()
    },
  })

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('bed.editBedTitle') ?? 'Edit bed'}
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!editName.trim() || updateMut.isPending}
            className="btn-primary text-sm"
          >
            {updateMut.isPending ? t('common.saving') : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <div>
          <label className="field-label">{t('common.nameLabel')}</label>
          <input value={editName} onChange={e => setEditName(e.target.value)} className="input w-full" />
        </div>
        <div>
          <label className="field-label">{t('common.descriptionLabel')}</label>
          <textarea value={editDescription} onChange={e => setEditDescription(e.target.value)} rows={2} className="input w-full" placeholder={t('common.optional')} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="field-label">{t('bed.conditions.lengthMeters')}</label>
            <input type="number" step="0.1" min="0" value={editLength} onChange={e => setEditLength(e.target.value)} placeholder="—" className="input w-full" />
          </div>
          <div>
            <label className="field-label">{t('bed.conditions.widthMeters')}</label>
            <input type="number" step="0.1" min="0" value={editWidth} onChange={e => setEditWidth(e.target.value)} placeholder="—" className="input w-full" />
          </div>
        </div>

        <div className="border border-divider rounded-lg overflow-hidden">
          <button
            type="button"
            onClick={() => setEditConditionsOpen(o => !o)}
            className="w-full flex items-center justify-between px-4 py-3 text-left bg-surface hover:bg-divider transition-colors"
          >
            <span className="text-sm font-medium">{t('bed.conditions.sectionTitle')}</span>
            <span className="text-text-secondary text-sm">{editConditionsOpen ? '▲' : '▼'}</span>
          </button>
          {editConditionsOpen && (
            <div className="px-4 py-3 space-y-3 border-t border-divider">
              <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                <div>
                  <label className="field-label">{t('bed.conditions.soilType')}</label>
                  <select value={editSoilType} onChange={e => setEditSoilType(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {SOIL_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.soilTypes.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.soilPh')}</label>
                  <input type="number" step="0.1" min="0" max="14" value={editSoilPh} onChange={e => setEditSoilPh(e.target.value)} placeholder="—" className="input" />
                  {editPhOutOfRange && <p className="text-error text-xs mt-1">{t('bed.conditions.phHint')}</p>}
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.sunExposure')}</label>
                  <select value={editSunExposure} onChange={e => setEditSunExposure(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {SUN_EXPOSURES.map(v => <option key={v} value={v}>{t(`bed.conditions.sunExposures.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.sunDirections')}</label>
                  <SunDirectionPicker value={editSunDirections} onChange={setEditSunDirections} />
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.drainage')}</label>
                  <select value={editDrainage} onChange={e => setEditDrainage(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {DRAINAGES.map(v => <option key={v} value={v}>{t(`bed.conditions.drainages.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.irrigationType')}</label>
                  <select value={editIrrigationType} onChange={e => setEditIrrigationType(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {IRRIGATION_TYPES.map(v => <option key={v} value={v}>{t(`bed.conditions.irrigationTypes.${v}`)}</option>)}
                  </select>
                </div>
                <div>
                  <label className="field-label">{t('bed.conditions.protection')}</label>
                  <select value={editProtection} onChange={e => setEditProtection(e.target.value)} className="input">
                    <option value="">{t('common.select')}</option>
                    {PROTECTIONS.map(v => <option key={v} value={v}>{t(`bed.conditions.protections.${v}`)}</option>)}
                  </select>
                </div>
                <div className="flex items-center gap-2 pt-1">
                  <input
                    id="raisedBed-edit"
                    type="checkbox"
                    checked={editRaisedBed}
                    onChange={e => setEditRaisedBed(e.target.checked)}
                    className="h-4 w-4 rounded border-divider accent-accent"
                  />
                  <label htmlFor="raisedBed-edit" className="text-sm">{t('bed.conditions.raisedBed')}</label>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </Dialog>
  )
}
