import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type BedPhotoReason, type GardenAreaPhotoResponse } from '../../api/client'
import { Dialog } from '../Dialog'
import { PhotoPicker } from '../PhotoPicker'
import { BedSectionHeader } from '../bed/BedSectionHeader'

const REASONS: BedPhotoReason[] = ['PROGRESS', 'ISSUE', 'HARVEST', 'PLANTING', 'OTHER']

function reasonLabelSv(reason: string): string {
  switch (reason) {
    case 'PROGRESS': return 'Tillväxt'
    case 'ISSUE': return 'Problem'
    case 'HARVEST': return 'Skörd'
    case 'PLANTING': return 'Plantering'
    case 'OTHER': return 'Övrigt'
    default: return reason
  }
}

export function AreaPhotosSection({
  areaId,
  onError,
  onSuccess,
}: {
  areaId: number
  onError: (message: string) => void
  onSuccess: (message: string) => void
}) {
  const { t } = useTranslation()
  const qc = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const [lightbox, setLightbox] = useState<GardenAreaPhotoResponse | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null)

  const { data: photos = [] } = useQuery({
    queryKey: ['area-photos', areaId],
    queryFn: () => api.areas.photos(areaId),
  })

  const deleteMut = useMutation({
    mutationFn: (photoId: number) => api.areas.deletePhoto(areaId, photoId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['area-photos', areaId] })
      setConfirmDelete(null)
      onSuccess('Bild borttagen')
    },
    onError: () => onError('Kunde inte ta bort bilden'),
  })

  return (
    <>
      <BedSectionHeader
        title="Bilder"
        meta={`${photos.length} ${photos.length === 1 ? 'bild' : 'bilder'}`}
        actions={
          <button onClick={() => setShowAdd(true)} className="btn-secondary" style={{ whiteSpace: 'nowrap' }}>
            + Lägg till bild
          </button>
        }
      />

      {photos.length === 0 ? (
        <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', margin: '8px 0' }}>
          Inga bilder ännu.
        </p>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(180px, 1fr))',
            gap: 14,
            marginBottom: 8,
          }}
        >
          {photos.map((photo) => (
            <figure
              key={photo.id}
              style={{
                margin: 0,
                border: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                background: 'var(--color-cream)',
                display: 'flex',
                flexDirection: 'column',
              }}
            >
              <button
                type="button"
                onClick={() => setLightbox(photo)}
                style={{
                  background: 'transparent',
                  border: 'none',
                  padding: 0,
                  cursor: 'zoom-in',
                  display: 'block',
                }}
              >
                <img
                  src={photo.photoUrl}
                  alt={photo.description ?? reasonLabelSv(photo.reason)}
                  style={{
                    width: '100%',
                    aspectRatio: '4 / 3',
                    objectFit: 'cover',
                    display: 'block',
                  }}
                  loading="lazy"
                />
              </button>
              <figcaption style={{ padding: '8px 10px 10px' }}>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'baseline',
                    gap: 8,
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-forest)',
                  }}
                >
                  <span>{reasonLabelSv(photo.reason)}</span>
                  <span>{photo.capturedAt.slice(0, 10)}</span>
                </div>
                {photo.description && (
                  <div
                    style={{
                      marginTop: 4,
                      fontFamily: 'var(--font-display)',
                      fontStyle: 'italic',
                      fontSize: 14,
                      lineHeight: 1.3,
                    }}
                  >
                    {photo.description}
                  </div>
                )}
                <button
                  type="button"
                  onClick={() => setConfirmDelete(photo.id)}
                  style={{
                    marginTop: 6,
                    background: 'transparent',
                    border: 'none',
                    padding: 0,
                    fontFamily: 'var(--font-mono)',
                    fontSize: 9,
                    letterSpacing: 1.4,
                    textTransform: 'uppercase',
                    color: 'var(--color-accent)',
                    cursor: 'pointer',
                  }}
                >
                  {t('common.delete')}
                </button>
              </figcaption>
            </figure>
          ))}
        </div>
      )}

      {showAdd && (
        <AddAreaPhotoDialog
          areaId={areaId}
          onClose={() => setShowAdd(false)}
          onSaved={() => {
            setShowAdd(false)
            qc.invalidateQueries({ queryKey: ['area-photos', areaId] })
            onSuccess('Bild sparad')
          }}
          onError={onError}
        />
      )}

      {lightbox && (
        <Dialog open={true} title={reasonLabelSv(lightbox.reason)} onClose={() => setLightbox(null)}>
          <img
            src={lightbox.photoUrl}
            alt={lightbox.description ?? ''}
            style={{ maxWidth: '100%', maxHeight: '70vh', display: 'block', margin: '0 auto' }}
          />
          {lightbox.description && (
            <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', marginTop: 12 }}>
              {lightbox.description}
            </p>
          )}
        </Dialog>
      )}

      {confirmDelete != null && (
        <Dialog
          open={true}
          title="Ta bort bild"
          onClose={() => setConfirmDelete(null)}
          actions={
            <>
              <button className="btn-secondary" onClick={() => setConfirmDelete(null)}>{t('common.cancel')}</button>
              <button
                className="btn-primary"
                style={{ background: 'var(--color-accent)', borderColor: 'var(--color-accent)' }}
                onClick={() => deleteMut.mutate(confirmDelete)}
                disabled={deleteMut.isPending}
              >
                {deleteMut.isPending ? '…' : t('common.delete')}
              </button>
            </>
          }
        >
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic' }}>
            Vill du ta bort den här bilden?
          </p>
        </Dialog>
      )}
    </>
  )
}

function AddAreaPhotoDialog({
  areaId,
  onClose,
  onSaved,
  onError,
}: {
  areaId: number
  onClose: () => void
  onSaved: () => void
  onError: (message: string) => void
}) {
  const { t } = useTranslation()
  const [photoDataUrl, setPhotoDataUrl] = useState<string | null>(null)
  const [reason, setReason] = useState<BedPhotoReason | ''>('')
  const [description, setDescription] = useState('')

  const isDirty = photoDataUrl != null || reason !== '' || description !== ''

  const addMut = useMutation({
    mutationFn: () => {
      if (!photoDataUrl || !reason) throw new Error('Missing image or reason')
      return api.areas.addPhoto(areaId, {
        imageBase64: photoDataUrl.replace(/^data:image\/\w+;base64,/, ''),
        reason,
        description: description.trim() || undefined,
      })
    },
    onSuccess: () => onSaved(),
    onError: (e) => onError(e instanceof Error ? e.message : 'Kunde inte spara bilden'),
  })

  const canSave = photoDataUrl != null && reason !== '' && !addMut.isPending

  return (
    <Dialog
      open={true}
      title="Ny bild"
      onClose={onClose}
      isDirty={isDirty}
      actions={
        <>
          <button className="btn-secondary" onClick={onClose}>{t('common.cancel')}</button>
          <button
            className="btn-primary"
            disabled={!canSave}
            onClick={() => addMut.mutate()}
          >
            {addMut.isPending ? '…' : t('common.save')}
          </button>
        </>
      }
    >
      <div className="space-y-4">
        <PhotoPicker value={photoDataUrl} onChange={setPhotoDataUrl} />
        <div>
          <label className="field-label">Anledning *</label>
          <select
            value={reason}
            onChange={(e) => setReason(e.target.value as BedPhotoReason | '')}
            className="input w-full mt-1"
          >
            <option value="">—</option>
            {REASONS.map((r) => (
              <option key={r} value={r}>{reasonLabelSv(r)}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="field-label">Beskrivning (valfri)</label>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="input w-full mt-1"
          />
        </div>
      </div>
    </Dialog>
  )
}
