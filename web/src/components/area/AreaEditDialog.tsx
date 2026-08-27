import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type GardenAreaResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import { AREA_CATEGORIES, areaCategoryLabelSv } from '../../lib/area'

export function AreaEditDialog({
  open,
  area,
  onClose,
}: {
  open: boolean
  area: GardenAreaResponse
  onClose: () => void
}) {
  const { t } = useTranslation()
  const qc = useQueryClient()

  // Edit dialog state — populated lazily from the current area at mount.
  // The parent only mounts this component when the dialog opens, so these
  // initializers run once with the area snapshot at open time.
  const [editName, setEditName] = useState(area.name)
  const [editCategory, setEditCategory] = useState(area.category)
  const [editDescription, setEditDescription] = useState(area.description ?? '')
  const [editSizeSqm, setEditSizeSqm] = useState(area.sizeSqm != null ? String(area.sizeSqm) : '')

  const editSizeNum = editSizeSqm !== '' ? parseFloat(editSizeSqm) : undefined
  const editSizeInvalid = editSizeNum !== undefined && (Number.isNaN(editSizeNum) || editSizeNum <= 0)

  // An omitted field reads as "keep the current value" server-side, so
  // emptying one has to be an explicit flag — and the flag may not travel
  // with a replacement value for the same field.
  const clearDescription = editDescription === '' && area.description != null
  const clearSizeSqm = editSizeSqm === '' && area.sizeSqm != null

  const updateMut = useMutation({
    mutationFn: () => api.areas.update(area.id, {
      name: editName,
      category: editCategory,
      description: editDescription || undefined,
      sizeSqm: editSizeNum,
      clearDescription,
      clearSizeSqm,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['area', area.id] })
      qc.invalidateQueries({ queryKey: ['garden-areas', area.gardenId] })
      onClose()
    },
  })

  const canSave = editName.trim() !== '' && editCategory !== '' && !editSizeInvalid

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={t('area.editAreaTitle')}
      actions={
        <>
          <button onClick={onClose} className="px-4 py-2 text-sm text-text-secondary">{t('common.cancel')}</button>
          <button
            onClick={() => updateMut.mutate()}
            disabled={!canSave || updateMut.isPending}
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
          <label className="field-label">{t('area.meta.category')}</label>
          <select value={editCategory} onChange={e => setEditCategory(e.target.value)} className="input w-full">
            <option value="">{t('common.select')}</option>
            {AREA_CATEGORIES.map((c) => (
              <option key={c} value={c}>{areaCategoryLabelSv(c)}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="field-label">{t('common.descriptionLabel')}</label>
          <textarea value={editDescription} onChange={e => setEditDescription(e.target.value)} rows={2} className="input w-full" placeholder={t('common.optional')} />
        </div>
        <div>
          <label className="field-label">{t('area.sizeSqmLabel')}</label>
          <input type="number" step="0.1" min="0" value={editSizeSqm} onChange={e => setEditSizeSqm(e.target.value)} placeholder="—" className="input w-full" />
          {editSizeInvalid && <p className="text-error text-xs mt-1">{t('area.sizeSqmHint')}</p>}
        </div>
      </div>
    </Dialog>
  )
}
