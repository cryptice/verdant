import { useState, useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api, type SpeciesResponse } from '../api/client'
import { Masthead, Rule } from '../components/faltet'
import { SpeciesAutocomplete } from '../components/SpeciesAutocomplete'
import { OnboardingHint } from '../onboarding/OnboardingHint'
import { useOnboarding } from '../onboarding/OnboardingContext'

const selectLabelStyle: React.CSSProperties = {
  display: 'block',
  fontFamily: 'var(--font-mono)',
  fontSize: 9,
  letterSpacing: 1.4,
  textTransform: 'uppercase',
  color: 'var(--color-forest)',
  opacity: 0.7,
  marginBottom: 4,
}

const selectStyle: React.CSSProperties = {
  display: 'block',
  width: '100%',
  background: 'transparent',
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

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
      {children}
    </div>
  )
}

export function SowActivity() {
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t, i18n } = useTranslation()
  const { completeStep } = useOnboarding()
  const [params] = useSearchParams()
  const presetBedId = params.get('bedId') ? Number(params.get('bedId')) : null
  const presetSpeciesId = params.get('speciesId') ? Number(params.get('speciesId')) : null
  const presetSeedBatchId = params.get('seedBatchId') ?? ''
  const taskId = params.get('taskId') ? Number(params.get('taskId')) : null

  const { data: beds } = useQuery({ queryKey: ['beds'], queryFn: api.beds.list })
  const { data: task } = useQuery({
    queryKey: ['task', taskId],
    queryFn: () => api.tasks.get(taskId!),
    enabled: !!taskId,
  })

  const isGroupTask = task ? task.acceptableSpecies.length > 1 : false

  const { data: sowBed } = useQuery({
    queryKey: ['bed', presetBedId],
    queryFn: () => api.beds.get(presetBedId!),
    enabled: !!presetBedId,
  })

  const { data: sowGarden } = useQuery({
    queryKey: ['garden', sowBed?.gardenId],
    queryFn: () => api.gardens.get(sowBed!.gardenId),
    enabled: !!sowBed,
  })

  // Fetch species list to resolve names
  const presetId = presetSpeciesId ?? (task?.speciesId ?? null)
  const { data: allSpecies } = useQuery({
    queryKey: ['species'],
    queryFn: api.species.list,
  })
  const presetSpecies = presetId ? allSpecies?.find(s => s.id === presetId) ?? null : null

  // Fetch all seed stock for the recent list
  const { data: allSeedStock } = useQuery({
    queryKey: ['seed-inventory'],
    queryFn: () => api.inventory.list(),
  })

  const [selectedSpecies, setSelectedSpecies] = useState<SpeciesResponse | null>(null)
  const [bedId, setBedId] = useState(presetBedId ? String(presetBedId) : '')
  const [sowInTray, setSowInTray] = useState(false)
  const [seedCount, setSeedCount] = useState('')
  const [notes, setNotes] = useState('')

  useEffect(() => {
    if (presetSpecies && !selectedSpecies) setSelectedSpecies(presetSpecies)
  }, [presetSpecies, selectedSpecies])

  useEffect(() => {
    if (task && !seedCount) setSeedCount(String(task.remainingCount))
  }, [task, seedCount])

  const speciesId = selectedSpecies?.id ? String(selectedSpecies.id) : ''

  const { data: seedBatches } = useQuery({
    queryKey: ['seed-batches', speciesId],
    queryFn: () => api.inventory.list(Number(speciesId)),
    enabled: !!speciesId,
    select: (items) => items.filter(i => i.quantity > 0),
  })
  const [seedBatchId, setSeedBatchId] = useState(presetSeedBatchId)

  // Auto-select first seed batch when species has stock
  useEffect(() => {
    if (!presetSeedBatchId && seedBatches && seedBatches.length > 0 && !seedBatchId) {
      setSeedBatchId(String(seedBatches[0].id))
    }
  }, [seedBatches, presetSeedBatchId, seedBatchId])

  const sowMut = useMutation({
    mutationFn: async () => {
      const lang = i18n.language
      const name = selectedSpecies
        ? ((lang === 'sv' ? selectedSpecies.commonNameSv ?? selectedSpecies.commonName : selectedSpecies.commonName) +
           (selectedSpecies.variantName ? ` — ${lang === 'sv' ? selectedSpecies.variantNameSv ?? selectedSpecies.variantName : selectedSpecies.variantName}` : ''))
        : ''
      const count = Number(seedCount)
      await api.plants.batchSow({
        bedId: sowInTray ? undefined : Number(bedId),
        speciesId: Number(speciesId),
        name,
        seedCount: count,
        notes: notes || undefined,
      })
      if (seedBatchId && count > 0) {
        await api.inventory.decrement(Number(seedBatchId), count)
      }
      if (notes) {
        await api.comments.record(notes)
      }
      if (taskId && count > 0) {
        await api.tasks.complete(taskId, Number(speciesId), count)
      }
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['bed-plants'] })
      qc.invalidateQueries({ queryKey: ['dashboard'] })
      qc.invalidateQueries({ queryKey: ['tasks'] })
      qc.invalidateQueries({ queryKey: ['seed-inventory'] })
      completeStep('sow_seeds')
      navigate(-1)
    },
  })

  const valid = speciesId && (sowInTray || bedId) && Number(seedCount) > 0

  // Build masthead breadcrumb context
  const mastheadLeft = taskId
    ? <span>{t('nav.tasks')} / <span style={{ color: 'var(--color-clay)' }}>{t('sow.title')}</span></span>
    : sowGarden && sowBed
      ? <span>{sowGarden.name} / {sowBed.name} / <span style={{ color: 'var(--color-clay)' }}>{t('sow.title')}</span></span>
      : <span style={{ color: 'var(--color-clay)' }}>{t('sow.title')}</span>

  return (
    <div>
      <Masthead
        left={mastheadLeft}
        center={t('sowing.masthead.center')}
      />

      <div style={{ padding: '28px 40px', paddingBottom: 120 }}>
        <OnboardingHint />

        {/* § 1. Art */}
        <SectionLabel>§ 1. {t('sowing.section.species')}</SectionLabel>
        <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>

        <div style={{ marginTop: 14 }} data-onboarding="sow-species">
          {isGroupTask && task ? (
            <div>
              <span style={selectLabelStyle}>{t('common.speciesLabel')}</span>
              <select
                value={speciesId}
                onChange={e => {
                  const sp = allSpecies?.find(s => s.id === Number(e.target.value)) ?? null
                  setSelectedSpecies(sp)
                }}
                style={selectStyle}
              >
                <option value="">{t('sow.selectSpecies')}</option>
                {task.acceptableSpecies.map(entry => (
                  <option key={entry.speciesId} value={entry.speciesId}>{entry.speciesName}</option>
                ))}
              </select>
            </div>
          ) : (
            <SpeciesAutocomplete
              value={selectedSpecies}
              onChange={s => { setSelectedSpecies(s); setSeedBatchId('') }}
            />
          )}
        </div>

        {/* Seed batch — shown once a species is picked */}
        {speciesId && (
          <div style={{ marginTop: 20 }}>
            <label style={{ display: 'block' }}>
              <span style={selectLabelStyle}>{t('sow.seedBatch')}</span>
              <select value={seedBatchId} onChange={e => setSeedBatchId(e.target.value)} style={selectStyle}>
                <option value="">{t('sow.seedBatchNone')}</option>
                {seedBatches?.map(b => {
                  const unitLabel = b.unitType ? t(`unitTypes.${b.unitType}`) : ''
                  const parts = [
                    unitLabel ? `${b.quantity} ${unitLabel.toLowerCase()}` : String(b.quantity),
                    b.expirationDate ? `${t('seeds.expires')} ${b.expirationDate}` : null,
                  ]
                  return (
                    <option key={b.id} value={b.id}>
                      {parts.filter(Boolean).join(' · ')}
                    </option>
                  )
                })}
              </select>
              {seedBatches && seedBatches.length === 0 && (
                <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 13, color: 'var(--color-forest)', marginTop: 6 }}>
                  {t('sow.noSeedStock')}
                </p>
              )}
            </label>
          </div>
        )}

        {/* § 2. Bädd */}
        <div style={{ marginTop: 28 }}>
          <SectionLabel>§ 2. {t('sowing.section.bed')}</SectionLabel>
          <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>
        </div>

        <div style={{ marginTop: 14 }} data-onboarding="sow-location">
          {/* Destination toggle — only shown when no preset bed and a species is selected */}
          {speciesId && !presetBedId && (
            <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
              <button
                type="button"
                onClick={() => setSowInTray(false)}
                style={{
                  flex: 1,
                  padding: '10px 0',
                  fontFamily: 'var(--font-display)',
                  fontSize: 16,
                  fontWeight: 300,
                  background: !sowInTray ? 'var(--color-paper)' : 'transparent',
                  border: `1px solid ${!sowInTray ? 'var(--color-ink)' : 'color-mix(in srgb, var(--color-ink) 30%, transparent)'}`,
                  cursor: 'pointer',
                  color: 'var(--color-ink)',
                }}
              >
                {t('sow.bed')}
              </button>
              <button
                type="button"
                onClick={() => { setSowInTray(true); setBedId('') }}
                style={{
                  flex: 1,
                  padding: '10px 0',
                  fontFamily: 'var(--font-display)',
                  fontSize: 16,
                  fontWeight: 300,
                  background: sowInTray ? 'var(--color-paper)' : 'transparent',
                  border: `1px solid ${sowInTray ? 'var(--color-ink)' : 'color-mix(in srgb, var(--color-ink) 30%, transparent)'}`,
                  cursor: 'pointer',
                  color: 'var(--color-ink)',
                }}
              >
                {t('sow.tray')}
              </button>
            </div>
          )}

          {speciesId && !sowInTray && (
            <label style={{ display: 'block' }}>
              <span style={selectLabelStyle}>{t('sow.bedLabel')}</span>
              <select value={bedId} onChange={e => setBedId(e.target.value)} style={selectStyle}>
                <option value="">{t('sow.selectBed')}</option>
                {beds?.map(b => <option key={b.id} value={b.id}>{b.gardenName} — {b.name}</option>)}
              </select>
            </label>
          )}

          {!speciesId && (
            <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-forest)', opacity: 0.6 }}>
              {t('sow.selectSpecies')}
            </p>
          )}
        </div>

        {/* § 3. Detaljer */}
        <div style={{ marginTop: 28 }}>
          <SectionLabel>§ 3. {t('sowing.section.details')}</SectionLabel>
          <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>
        </div>

        <div style={{ marginTop: 14, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
          {/* Seed count */}
          <div>
            <span style={selectLabelStyle}>{t('sow.seedCount')}</span>
            <input
              type="number"
              value={seedCount}
              onChange={e => setSeedCount(e.target.value)}
              style={{ ...selectStyle, fontFamily: 'var(--font-mono)', fontSize: 20 }}
            />
          </div>
        </div>

        {/* Notes */}
        <div style={{ marginTop: 28, border: '1px solid var(--color-ink)', background: 'var(--color-paper)', padding: '14px 16px' }}>
          <div style={selectLabelStyle}>{t('common.notesLabel')}</div>
          <textarea
            value={notes}
            onChange={e => setNotes(e.target.value)}
            placeholder={t('common.optional')}
            rows={3}
            style={{ width: '100%', minHeight: 80, background: 'transparent', border: 'none', outline: 'none', fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16, resize: 'vertical', boxSizing: 'border-box' }}
          />
        </div>

        {sowMut.error && (
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-clay)', marginTop: 12 }}>
            {sowMut.error instanceof Error ? sowMut.error.message : String(sowMut.error)}
          </p>
        )}

        {/* Recent seed stock quick-pick */}
        {allSeedStock && allSeedStock.length > 0 && (
          <div style={{ marginTop: 40 }}>
            <div style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6, marginBottom: 12 }}>
              {t('sow.recentSeedStock')}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
              {allSeedStock
                .filter(s => s.quantity > 0)
                .slice(0, 10)
                .reverse()
                .map(item => {
                  const sp = allSpecies?.find(s => s.id === item.speciesId)
                  const name = i18n.language === 'sv'
                    ? (sp?.commonNameSv ?? sp?.commonName ?? item.speciesName)
                    : (sp?.commonName ?? item.speciesName)
                  const variant = i18n.language === 'sv'
                    ? (sp?.variantNameSv ?? sp?.variantName)
                    : sp?.variantName
                  const unitLabel = item.unitType ? t(`unitTypes.${item.unitType}`).toLowerCase() : ''
                  return (
                    <button
                      key={item.id}
                      onClick={() => {
                        if (sp) setSelectedSpecies(sp)
                        setSeedBatchId(String(item.id))
                        setSeedCount(String(item.quantity))
                      }}
                      style={{
                        display: 'grid',
                        gridTemplateColumns: '1fr auto',
                        gap: 16,
                        padding: '10px 14px',
                        background: 'transparent',
                        border: '1px solid color-mix(in srgb, var(--color-ink) 25%, transparent)',
                        cursor: 'pointer',
                        textAlign: 'left',
                      }}
                    >
                      <div>
                        <p style={{ fontFamily: 'var(--font-display)', fontSize: 16, margin: 0 }}>
                          {name}{variant ? <span style={{ color: 'var(--color-forest)', opacity: 0.7 }}> — {variant}</span> : ''}
                        </p>
                        {item.providerName && (
                          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1, color: 'var(--color-forest)', opacity: 0.6, margin: '2px 0 0' }}>
                            {item.providerName}
                          </p>
                        )}
                      </div>
                      <span style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', alignSelf: 'center' }}>
                        {item.quantity} {unitLabel}
                      </span>
                    </button>
                  )
                })}
            </div>
          </div>
        )}
      </div>

      {/* Sticky footer */}
      <div style={{ position: 'sticky', bottom: 0, background: 'var(--color-cream)', borderTop: '1px solid var(--color-ink)', padding: '14px 40px', display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
        <button className="btn-secondary" onClick={() => navigate(-1)}>
          {t('common.cancel')}
        </button>
        <button
          data-onboarding="sow-submit"
          className="btn-primary"
          onClick={() => sowMut.mutate()}
          disabled={!valid || sowMut.isPending}
        >
          {sowMut.isPending ? t('sow.sowing') : t('sow.sow')}
        </button>
      </div>
    </div>
  )
}
