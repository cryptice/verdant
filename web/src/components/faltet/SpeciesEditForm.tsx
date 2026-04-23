import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../../api/client'
import { Field, Rule, Chip, PhotoPlaceholder } from './index'

export function SpeciesEditForm({ speciesId, onSaved }: { speciesId: number; onSaved?: () => void }) {
  const { t, i18n } = useTranslation()
  const qc = useQueryClient()
  const { data: species } = useQuery({
    queryKey: ['species', speciesId],
    queryFn: () => api.species.get(speciesId),
  })

  const [draft, setDraft] = useState<Record<string, unknown>>({})
  const value = (k: string): string => {
    if (k in draft) return String(draft[k] ?? '')
    const sp = species as Record<string, unknown> | undefined
    const v = sp?.[k]
    return v != null ? String(v) : ''
  }
  const set = (k: string, v: string) => setDraft((d) => ({ ...d, [k]: v }))

  const saveMut = useMutation({
    mutationFn: () => api.species.update(speciesId, draft),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['species', speciesId] })
      onSaved?.()
    },
  })

  if (!species) return null

  const lang = i18n.language
  const displayName = lang === 'sv' ? (species.commonNameSv ?? species.commonName) : species.commonName
  const displayVariant = lang === 'sv' ? (species.variantNameSv ?? species.variantName) : species.variantName

  return (
    <div style={{ padding: '22px 28px' }}>
      {/* Hero */}
      <div style={{ display: 'grid', gridTemplateColumns: '96px 1fr auto', gap: 20, alignItems: 'start' }}>
        <div style={{ width: 96, height: 96 }}>
          <PhotoPlaceholder tone="blush" aspect="square" label="ART · Foto" />
        </div>
        <div>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 10 }}>
            {species.plantType && <Chip tone="berry">{species.plantType}</Chip>}
            {species.groups.map((g) => (
              <Chip key={g.id} tone="sage">{g.name}</Chip>
            ))}
          </div>
          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 44,
              fontWeight: 300,
              letterSpacing: -1,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
              lineHeight: 1.1,
            }}
          >
            {displayName}
            {displayVariant && (
              <span style={{ fontStyle: 'italic', color: 'var(--color-accent)' }}> '{displayVariant}'</span>
            )}
          </div>
          {species.scientificName && (
            <div
              style={{
                fontFamily: 'var(--font-display)',
                fontStyle: 'italic',
                fontSize: 14,
                color: 'var(--color-sage)',
                marginTop: 4,
              }}
            >
              {species.scientificName}
            </div>
          )}
        </div>
        <div
          style={{
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 22,
            color: 'var(--color-mustard)',
          }}
        >
          № {String(species.id).padStart(3, '0')}
        </div>
      </div>

      <div style={{ margin: '22px 0' }}><Rule variant="ink" /></div>

      {/* Identity fields */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
        <Field label={t('species.fields.sortSv')} editable value={value('variantNameSv')} onChange={(v) => set('variantNameSv', v)} accent="clay" />
        <Field label={t('species.fields.sortEn')} editable value={value('variantName')} onChange={(v) => set('variantName', v)} accent="clay" />
        <Field label={t('speciesDetail.commonNameSv')} editable value={value('commonNameSv')} onChange={(v) => set('commonNameSv', v)} accent="clay" />
        <Field label={t('species.commonName')} editable value={value('commonName')} onChange={(v) => set('commonName', v)} accent="clay" />
        <Field label={t('species.scientificName')} editable value={value('scientificName')} onChange={(v) => set('scientificName', v)} accent="sage" />
        <Field label={t('speciesDetail.plantType')} editable value={value('plantType')} onChange={(v) => set('plantType', v)} accent="berry" />
      </div>

      <div style={{ margin: '28px 0 12px', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
        {t('species.section.cultivation')}
      </div>

      {/* Cultivation window fields */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
        <Field label={t('species.fields.sowStart')} editable value={value('sowingMonths')} onChange={(v) => set('sowingMonths', v)} accent="mustard" />
        <Field label={t('speciesDetail.bloomMonths')} editable value={value('bloomMonths')} onChange={(v) => set('bloomMonths', v)} accent="mustard" />
        <Field label={t('speciesDetail.germinationTime')} editable value={value('germinationTimeDaysMin')} onChange={(v) => set('germinationTimeDaysMin', v)} accent="sage" />
        <Field label={t('speciesDetail.daysToHarvest')} editable value={value('daysToHarvestMin')} onChange={(v) => set('daysToHarvestMin', v)} accent="clay" />
        <Field label={t('speciesDetail.sowingDepth')} editable value={value('sowingDepthMm')} onChange={(v) => set('sowingDepthMm', v)} accent="mustard" />
        <Field label={t('speciesDetail.height')} editable value={value('heightCmMin')} onChange={(v) => set('heightCmMin', v)} accent="sage" />
      </div>

      <div style={{ margin: '28px 0 12px', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
        {t('speciesDetail.commercial')}
      </div>

      {/* Commercial fields */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
        <Field label={t('speciesDetail.stemsPerPlant')} editable value={value('expectedStemsPerPlant')} onChange={(v) => set('expectedStemsPerPlant', v)} accent="sage" />
        <Field label={t('speciesDetail.vaseLife')} editable value={value('expectedVaseLifeDays')} onChange={(v) => set('expectedVaseLifeDays', v)} accent="sky" />
        <Field label={t('speciesDetail.costPerSeed')} editable value={value('costPerSeedSek')} onChange={(v) => set('costPerSeedSek', v)} accent="mustard" />
        <Field label={t('speciesDetail.germinationRate')} editable value={value('germinationRate')} onChange={(v) => set('germinationRate', v)} accent="clay" />
      </div>

      <div style={{ margin: '28px 0 16px', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
        {t('species.notes.label')}
      </div>

      {/* Notes block */}
      <div
        style={{
          border: '1px solid var(--color-ink)',
          background: 'var(--color-paper)',
          padding: '14px 16px',
          minHeight: 120,
        }}
      >
        <textarea
          value={value('cultivationNotes')}
          onChange={(e) => set('cultivationNotes', e.target.value)}
          placeholder={t('common.notesLabel')}
          style={{
            width: '100%',
            minHeight: 80,
            background: 'transparent',
            border: 'none',
            outline: 'none',
            resize: 'vertical',
            fontFamily: 'var(--font-display)',
            fontStyle: 'italic',
            fontSize: 16,
            color: 'var(--color-ink)',
            boxSizing: 'border-box',
          }}
        />
      </div>

      {/* Providers ledger */}
      {species.providers && species.providers.length > 0 && (
        <>
          <div style={{ margin: '28px 0 12px', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
            {t('speciesDetail.providers')}
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            {species.providers.map((p) => (
              <div key={p.id} style={{ display: 'flex', alignItems: 'center', padding: '8px 0', borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 15%, transparent)' }}>
                <span style={{ fontFamily: 'var(--font-display)', fontSize: 16 }}>{p.providerName}</span>
              </div>
            ))}
          </div>
        </>
      )}

      {/* Tags */}
      {species.tags && species.tags.length > 0 && (
        <>
          <div style={{ margin: '28px 0 12px', fontFamily: 'var(--font-mono)', fontSize: 9, letterSpacing: 1.4, textTransform: 'uppercase', color: 'var(--color-forest)', opacity: 0.6 }}>
            {t('speciesDetail.tags')}
          </div>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {species.tags.map((tag) => (
              <Chip key={tag.id} tone="sage">{tag.name}</Chip>
            ))}
          </div>
        </>
      )}

      {/* Save button */}
      <div style={{ marginTop: 28, display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
        <button
          onClick={() => saveMut.mutate()}
          disabled={saveMut.isPending || Object.keys(draft).length === 0}
          className="btn-primary"
        >
          {saveMut.isPending ? t('species.edit.saving') : t('species.edit.save')}
        </button>
      </div>
    </div>
  )
}
