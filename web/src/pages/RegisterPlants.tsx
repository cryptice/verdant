import { useEffect, useId, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { sortBedsWithGardenByNaturalName } from '../lib/bed'
import { Masthead } from '../components/faltet'
import { Dialog } from '../components/Dialog'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { DATE_INPUT_MAX } from '../lib/dateInput'

const labelStyle: React.CSSProperties = {
  display: 'block',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  marginBottom: 4,
}

const fieldStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  backgroundColor: 'transparent',
  border: 'none',
  borderBottom: '1px solid var(--color-ink)',
  borderRadius: 0,
  padding: '4px 0',
  fontFamily: 'var(--font-display)',
  fontSize: 20,
  fontWeight: 300,
  color: 'var(--color-ink)',
  outline: 'none',
}

/** Same presets as Android: how long ago the plants were started. */
const AGE_PRESETS: { key: string; count?: number; days: number }[] = [
  { key: 'today', days: 0 },
  { key: 'weeks', count: 1, days: 7 },
  { key: 'weeks', count: 2, days: 14 },
  { key: 'weeks', count: 4, days: 28 },
  { key: 'weeks', count: 8, days: 56 },
  { key: 'months', count: 3, days: 90 },
  { key: 'months', count: 6, days: 180 },
  { key: 'year', days: 365 },
]

/** A local calendar date `days` back, as `YYYY-MM-DD` (sv-SE formats it that way). */
export function daysAgoIsoLocal(days: number, now = new Date()): string {
  return new Date(now.getFullYear(), now.getMonth(), now.getDate() - days).toLocaleDateString('sv-SE')
}

type Placement = 'BED' | 'TRAY'

/**
 * Registers plants the grower already has — the web counterpart of Android's
 * "Registrera plantor". Unlike sowing it draws nothing from seed stock and
 * the start date is usually in the past.
 */
export function RegisterPlants() {
  const { t } = useTranslation()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const id = useId()

  const { data: beds = [] } = useQuery({ queryKey: ['beds'], queryFn: api.beds.list })
  const { data: trayLocations = [] } = useQuery({ queryKey: ['tray-locations'], queryFn: () => api.trayLocations.list() })

  const [species, setSpecies] = useState<SpeciesResponse | null>(null)
  const [placement, setPlacement] = useState<Placement>('TRAY')
  const [bedId, setBedId] = useState('')
  const [trayLocationId, setTrayLocationId] = useState('')
  const [count, setCount] = useState('1')
  const [seedDate, setSeedDate] = useState(() => daysAgoIsoLocal(0))
  const [notes, setNotes] = useState('')
  const [showAddLocation, setShowAddLocation] = useState(false)
  const [newLocationName, setNewLocationName] = useState('')
  const [done, setDone] = useState(false)

  // Mirrors Android: with beds but no tray locations, a bed is the only place.
  useEffect(() => {
    if (beds.length > 0 && trayLocations.length === 0) setPlacement('BED')
  }, [beds.length, trayLocations.length])
  useEffect(() => {
    if (!trayLocationId && trayLocations.length === 1) setTrayLocationId(String(trayLocations[0].id))
  }, [trayLocations, trayLocationId])
  useEffect(() => {
    if (!bedId && beds.length === 1) setBedId(String(beds[0].id))
  }, [beds, bedId])

  const createLocationMut = useMutation({
    mutationFn: (name: string) => api.trayLocations.create(name),
    onSuccess: (location) => {
      qc.invalidateQueries({ queryKey: ['tray-locations'] })
      setTrayLocationId(String(location.id))
      setShowAddLocation(false)
      setNewLocationName('')
    },
  })

  const registerMut = useMutation({
    mutationFn: () => api.plants.batchSow({
      speciesId: species!.id,
      name: species!.variantNameSv ?? species!.variantName ?? species!.commonNameSv ?? species!.commonName,
      seedCount: Number(count),
      bedId: placement === 'BED' ? Number(bedId) : undefined,
      trayLocationId: placement === 'TRAY' && trayLocationId ? Number(trayLocationId) : undefined,
      plantedDate: seedDate,
      notes: notes.trim() || undefined,
      origin: 'REGISTERED',
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bed-plants'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      qc.invalidateQueries({ queryKey: ['tray-summary'] })
      setDone(true)
    },
  })

  const placementValid = placement === 'BED' ? bedId !== '' : trayLocations.length < 2 || trayLocationId !== ''
  const valid = species != null && Number(count) > 0 && seedDate !== '' && placementValid

  const toggleStyle = (active: boolean): React.CSSProperties => ({
    flex: 1,
    padding: '10px 0',
    fontFamily: 'var(--font-display)',
    fontSize: 16,
    fontWeight: 300,
    background: active ? 'var(--color-paper)' : 'transparent',
    border: `1px solid ${active ? 'var(--color-ink)' : 'color-mix(in srgb, var(--color-ink) 30%, transparent)'}`,
    cursor: 'pointer',
    color: 'var(--color-ink)',
  })

  return (
    <div>
      <Masthead left={<span style={{ color: 'var(--color-accent)' }}>{t('registerPlants.title')}</span>} center="" />

      <div className="page-body-tight">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-x-7 gap-y-5">
          <div className="md:col-span-2">
            <span style={labelStyle}>{t('common.speciesLabel')}</span>
            <SpeciesAutocomplete value={species} onChange={setSpecies} />
          </div>

          {beds.length > 0 && trayLocations.length > 0 && (
            <div className="md:col-span-2">
              <span style={labelStyle}>{t('registerPlants.placement')}</span>
              <div style={{ display: 'flex', gap: 16 }}>
                <button type="button" aria-pressed={placement === 'BED'} onClick={() => setPlacement('BED')} style={toggleStyle(placement === 'BED')}>
                  {t('registerPlants.bed')}
                </button>
                <button type="button" aria-pressed={placement === 'TRAY'} onClick={() => setPlacement('TRAY')} style={toggleStyle(placement === 'TRAY')}>
                  {t('registerPlants.tray')}
                </button>
              </div>
            </div>
          )}

          {placement === 'BED' && beds.length > 0 && (
            <div>
              <label htmlFor={`${id}-bed`} style={labelStyle}>{t('registerPlants.bedLabel')}</label>
              <select id={`${id}-bed`} value={bedId} onChange={e => setBedId(e.target.value)} style={fieldStyle}>
                <option value="">{t('common.select')}</option>
                {sortBedsWithGardenByNaturalName(beds).map(b => (
                  <option key={b.id} value={b.id}>{b.gardenName ? `${b.gardenName} · ${b.name}` : b.name}</option>
                ))}
              </select>
            </div>
          )}

          {placement === 'TRAY' && trayLocations.length >= 2 && (
            <div>
              <label htmlFor={`${id}-tray`} style={labelStyle}>{t('registerPlants.trayLocation')}</label>
              <select id={`${id}-tray`} value={trayLocationId} onChange={e => setTrayLocationId(e.target.value)} style={fieldStyle}>
                <option value="">{t('common.select')}</option>
                {trayLocations.map(l => <option key={l.id} value={l.id}>{l.name}</option>)}
              </select>
              <button
                type="button"
                onClick={() => setShowAddLocation(true)}
                style={{ marginTop: 6, background: 'transparent', border: 'none', padding: 0, cursor: 'pointer', fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, textTransform: 'uppercase', color: 'var(--color-accent)' }}
              >
                {t('registerPlants.newTrayLocation')}
              </button>
            </div>
          )}

          <div>
            <label htmlFor={`${id}-count`} style={labelStyle}>{t('registerPlants.count')}</label>
            <input id={`${id}-count`} type="number" min="1" value={count} onChange={e => setCount(e.target.value)} style={{ ...fieldStyle, fontFamily: 'var(--font-mono)', fontSize: 16 }} />
          </div>

          <div>
            <label htmlFor={`${id}-date`} style={labelStyle}>{t('registerPlants.seedDate')}</label>
            <input id={`${id}-date`} type="date" max={DATE_INPUT_MAX} value={seedDate} onChange={e => setSeedDate(e.target.value)} style={{ ...fieldStyle, fontFamily: 'var(--font-mono)', fontSize: 16 }} />
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 10 }}>
              {AGE_PRESETS.map(preset => {
                const date = daysAgoIsoLocal(preset.days)
                const active = seedDate === date
                return (
                  <button
                    key={preset.days}
                    type="button"
                    aria-pressed={active}
                    onClick={() => setSeedDate(date)}
                    style={{
                      padding: '4px 10px',
                      borderRadius: 999,
                      border: '1px solid var(--color-ink)',
                      background: active ? 'var(--color-ink)' : 'transparent',
                      color: active ? 'var(--color-cream)' : 'var(--color-ink)',
                      fontFamily: 'var(--font-mono)',
                      fontSize: 11,
                      cursor: 'pointer',
                    }}
                  >
                    {t(`registerPlants.age.${preset.key}`, { count: preset.count })}
                  </button>
                )
              })}
            </div>
          </div>

          <div className="md:col-span-2">
            <label htmlFor={`${id}-notes`} style={labelStyle}>{t('registerPlants.notes')}</label>
            <input id={`${id}-notes`} value={notes} onChange={e => setNotes(e.target.value)} style={{ ...fieldStyle, fontSize: 16 }} />
          </div>
        </div>

        {registerMut.error && (
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-error)', marginTop: 12 }}>
            {registerMut.error instanceof Error ? registerMut.error.message : String(registerMut.error)}
          </p>
        )}
      </div>

      <div className="sticky-footer">
        <button className="btn-secondary" onClick={() => navigate(-1)}>{t('common.cancel')}</button>
        <button className="btn-primary" onClick={() => registerMut.mutate()} disabled={!valid || registerMut.isPending}>
          {registerMut.isPending ? t('common.saving') : t('registerPlants.submit')}
        </button>
      </div>

      <Dialog
        open={showAddLocation}
        onClose={() => setShowAddLocation(false)}
        title={t('registerPlants.newTrayLocationTitle')}
        actions={
          <>
            <button className="btn-secondary" onClick={() => setShowAddLocation(false)}>{t('common.cancel')}</button>
            <button
              className="btn-primary"
              disabled={!newLocationName.trim() || createLocationMut.isPending}
              onClick={() => createLocationMut.mutate(newLocationName.trim())}
            >
              {t('common.save')}
            </button>
          </>
        }
      >
        <label htmlFor={`${id}-location-name`} style={labelStyle}>{t('registerPlants.locationName')}</label>
        <input id={`${id}-location-name`} value={newLocationName} onChange={e => setNewLocationName(e.target.value)} className="input w-full" />
      </Dialog>

      <Dialog
        open={done}
        onClose={() => navigate(-1)}
        title={t('registerPlants.doneTitle')}
        actions={<button className="btn-primary" onClick={() => navigate(-1)}>OK</button>}
      >
        <p className="text-text-secondary">{t('registerPlants.doneBody', { date: seedDate })}</p>
      </Dialog>
    </div>
  )
}
