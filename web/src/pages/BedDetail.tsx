import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Chip, Stat, PhotoPlaceholder } from '../components/faltet'
import { SpeciesEditModal } from '../components/faltet/SpeciesEditModal'

export function BedDetail() {
  const { id } = useParams<{ id: string }>()
  const bedId = Number(id)
  const { t } = useTranslation()

  const { data: bed } = useQuery({ queryKey: ['bed', bedId], queryFn: () => api.beds.get(bedId) })
  const { data: plants } = useQuery({ queryKey: ['bed-plants', bedId], queryFn: () => api.beds.plants(bedId) })
  const { data: garden } = useQuery({
    queryKey: ['garden', bed?.gardenId],
    queryFn: () => api.gardens.get(bed!.gardenId),
    enabled: !!bed,
  })
  const { data: siblings = [] } = useQuery({
    queryKey: ['garden-beds', bed?.gardenId],
    queryFn: () => api.gardens.beds(bed!.gardenId),
    enabled: !!bed,
  })

  // Alphabetical prev/next navigation within the same garden.
  const sortedSiblings = [...siblings].sort((a, b) => a.name.localeCompare(b.name))
  const currentIdx = sortedSiblings.findIndex((s) => s.id === bedId)
  const prevBed = currentIdx > 0 ? sortedSiblings[currentIdx - 1] : null
  const nextBed =
    currentIdx >= 0 && currentIdx < sortedSiblings.length - 1
      ? sortedSiblings[currentIdx + 1]
      : null

  const [modalSpecies, setModalSpecies] = useState<number | null>(null)

  if (!bed) return null

  const area =
    bed.lengthMeters && bed.widthMeters
      ? (bed.lengthMeters * bed.widthMeters).toFixed(1)
      : '—'

  const activePlants = plants?.filter((p) => p.status === 'PLANTED_OUT') ?? []
  const uniqueSpeciesCount = new Set(plants?.map((p) => p.speciesId).filter(Boolean)).size

  const sunKey = bed.sunExposure ? bed.sunExposure.toLowerCase() : null
  const irrigationKey = bed.irrigationType ? bed.irrigationType.toLowerCase() : null

  return (
    <div>
      <Masthead
        left={
          <span>
            <Link to="/gardens" style={{ color: 'inherit', textDecoration: 'none' }}>
              {t('nav.gardens')}
            </Link>
            {' / '}
            {garden ? (
              <Link to={`/garden/${garden.id}`} style={{ color: 'inherit', textDecoration: 'none' }}>
                {garden.name}
              </Link>
            ) : '…'}
            {' / '}
            <span style={{ color: 'var(--color-accent)' }}>{bed.name}</span>
          </span>
        }
        center={t('bed.masthead.center')}
      />

      <div style={{ padding: '28px 40px' }}>
        {/* Alphabetical prev/next nav within the same garden */}
        {(prevBed || nextBed) && (
          <div
            style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              marginBottom: 20,
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
            }}
          >
            {prevBed ? (
              <Link to={`/bed/${prevBed.id}`} style={{ color: 'var(--color-accent)', textDecoration: 'none' }}>
                ← {prevBed.name}
              </Link>
            ) : <span />}
            {nextBed ? (
              <Link to={`/bed/${nextBed.id}`} style={{ color: 'var(--color-accent)', textDecoration: 'none' }}>
                {nextBed.name} →
              </Link>
            ) : <span />}
          </div>
        )}

        {/* Hero row */}
        <div>
          <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginBottom: 18 }}>
            {sunKey && <Chip tone="sage">{t(`bed.sun.${sunKey}`)}</Chip>}
            {irrigationKey && <Chip tone="sky">{t(`bed.irrigation.${irrigationKey}`)}</Chip>}
            {bed.raisedBed && <Chip tone="berry">{t('bed.raised')}</Chip>}
          </div>
          <h1
            style={{
              fontFamily: 'var(--font-display)',
              fontSize: 80,
              fontWeight: 300,
              lineHeight: 1,
              letterSpacing: -1.5,
              margin: 0,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            <span style={{ fontStyle: 'italic', color: 'var(--color-accent)' }}>{bed.name}.</span>
          </h1>
          {bed.description && (
            <p
              style={{
                marginTop: 16,
                fontFamily: 'Georgia, var(--font-display)',
                fontSize: 15,
                lineHeight: 1.6,
                color: 'var(--color-forest)',
              }}
            >
              {bed.description}
            </p>
          )}
        </div>

        {/* Stats band */}
        <div
          style={{
            margin: '40px 0',
            padding: '20px 0',
            borderTop: '1px solid var(--color-ink)',
            borderBottom: '1px solid var(--color-ink)',
            display: 'grid',
            gridTemplateColumns: 'repeat(5, 1fr)',
            gap: 18,
          }}
        >
          <Stat size="medium" value={activePlants.length} label={t('bed.stats.active')} hue="sage" />
          {/* Harvest totals, days-to-harvest and utilization aren't wired yet;
             show a dash instead of mocked numbers. */}
          <Stat size="medium" value="—" label={t('bed.stats.harvested')} hue="clay" />
          <Stat size="medium" value="—" label={t('bed.stats.daysToHarvest')} hue="mustard" />
          <Stat size="medium" value={plants?.length ?? 0} label={t('bed.stats.plants')} hue="sky" />
          <Stat size="medium" value="—" label={t('bed.stats.utilization')} hue="berry" />
        </div>

        {/* Plantor section */}
        <SectionHeader
          title={t('bed.plants.title')}
          meta={`${uniqueSpeciesCount} ${t('bed.plants.metaSuffix')}`}
          actions={
            <Link
              to={`/sow?bedId=${bedId}`}
              style={{
                display: 'inline-block',
                background: 'var(--color-accent)',
                color: 'var(--color-cream)',
                border: '1px solid var(--color-accent)',
                fontFamily: 'var(--font-mono)',
                fontSize: 11,
                letterSpacing: 1.6,
                textTransform: 'uppercase',
                textDecoration: 'none',
                padding: '7px 16px',
                whiteSpace: 'nowrap',
              }}
            >
              + {t('bed.plants.sow')}
            </Link>
          }
        />

        {/* Plant table header */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: '50px 1.5fr 70px 1fr 120px',
            gap: 18,
            padding: '10px 0',
            borderBottom: '1px solid var(--color-ink)',
            fontFamily: 'var(--font-mono)',
            fontSize: 9,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
            color: 'var(--color-forest)',
            opacity: 0.7,
          }}
        >
          <span>№</span>
          <span>{t('bed.plants.col.species')}</span>
          <span>{t('bed.plants.col.count')}</span>
          <span>{t('bed.plants.col.status')}</span>
          <span>{t('bed.plants.col.timeline')}</span>
        </div>

        {/* Plant rows */}
        {plants?.map((p, i) => (
          <button
            key={p.id}
            onClick={() => p.speciesId != null && setModalSpecies(p.speciesId)}
            style={{
              display: 'grid',
              gridTemplateColumns: '50px 1.5fr 70px 1fr 120px',
              gap: 18,
              padding: '12px 0',
              borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
              background: 'transparent',
              border: 'none',
              borderBottomWidth: 1,
              borderBottomStyle: 'solid',
              borderBottomColor: 'color-mix(in srgb, var(--color-ink) 20%, transparent)',
              width: '100%',
              textAlign: 'left',
              cursor: p.speciesId != null ? 'pointer' : 'default',
              alignItems: 'center',
            }}
          >
            <span
              style={{
                fontFamily: 'var(--font-display)',
                fontStyle: 'italic',
                fontSize: 22,
                color: 'var(--color-accent)',
              }}
            >
              {String(i + 1).padStart(2, '0')}
            </span>
            <div style={{ fontFamily: 'var(--font-display)', fontSize: 20 }}>
              {p.speciesName ?? p.name}
            </div>
            <span
              style={{
                fontFamily: 'var(--font-display)',
                fontSize: 20,
                fontVariantNumeric: 'tabular-nums',
              }}
            >
              {p.seedCount ?? 1}
            </span>
            <span
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: 10,
                letterSpacing: 1.4,
                textTransform: 'uppercase',
              }}
            >
              {p.status}
            </span>
            <span style={{ fontFamily: 'var(--font-mono)', fontSize: 10 }}>
              {p.plantedDate ?? '—'}
            </span>
          </button>
        ))}

        {/* Harvest card — TODO: wire to real harvest stats when available */}
        <div
          style={{
            background: 'var(--color-ink)',
            color: 'var(--color-cream)',
            padding: '22px 28px',
            position: 'relative',
            overflow: 'hidden',
            marginTop: 40,
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: -40,
              right: -40,
              width: 140,
              height: 140,
              borderRadius: '50%',
              background: 'var(--color-butter)',
              opacity: 0.2,
            }}
          />
          <div
            style={{
              fontFamily: 'var(--font-display)',
              fontStyle: 'italic',
              fontSize: 26,
              fontVariationSettings: '"SOFT" 100, "opsz" 144',
            }}
          >
            {t('bed.harvest.headline', { stems: 0 })}{' '}
            <span style={{ color: 'var(--color-blush)' }}>{t('bed.harvest.season', { year: new Date().getFullYear() })}</span>.
          </div>
        </div>

        {/* Bed layout — photo + dimensions side-by-side */}
        <div style={{ marginTop: 48, display: 'grid', gridTemplateColumns: '260px 1fr', gap: 22, alignItems: 'start' }}>
          <PhotoPlaceholder tone="sage" aspect="tall" label={bed.name.toUpperCase()} />
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: 0,
              border: '1px solid var(--color-ink)',
            }}
          >
            <MetaCell label={t('bed.meta.length')} value={bed.lengthMeters ? `${bed.lengthMeters} m` : '—'} />
            <MetaCell label={t('bed.meta.width')} value={bed.widthMeters ? `${bed.widthMeters} m` : '—'} />
            <MetaCell label={t('bed.meta.orient')} value={bed.aspect ?? '—'} />
            <MetaCell label={t('bed.meta.area')} value={`${area} m²`} />
          </div>
        </div>

        {/* Danger callout */}
        <div
          style={{
            marginTop: 48,
            border: '1px solid color-mix(in srgb, var(--color-accent) 40%, transparent)',
            padding: '22px 28px',
          }}
        >
          <div
            style={{
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              marginBottom: 10,
            }}
          >
            {t('bed.danger.title')}
          </div>
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 15, margin: 0 }}>
            {t('bed.danger.warning')}
          </p>
          <button
            onClick={() => {
              if (window.confirm(t('bed.danger.confirm') ?? '')) {
                api.beds.delete(bedId)
              }
            }}
            style={{
              marginTop: 10,
              background: 'transparent',
              border: 'none',
              fontFamily: 'var(--font-mono)',
              fontSize: 10,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-accent)',
              cursor: 'pointer',
              padding: 0,
            }}
          >
            → {t('bed.danger.delete', { id: String(bedId).padStart(2, '0') })}
          </button>
        </div>
      </div>

      <SpeciesEditModal speciesId={modalSpecies} onClose={() => setModalSpecies(null)} />
    </div>
  )
}

function MetaCell({ label, value }: { label: string; value: string }) {
  return (
    <div
      style={{
        padding: '10px 14px',
        borderTop: '1px solid var(--color-ink)',
        borderLeft: '1px solid var(--color-ink)',
      }}
    >
      <div
        style={{
          fontFamily: 'var(--font-mono)',
          fontSize: 9,
          letterSpacing: 1.4,
          textTransform: 'uppercase',
          color: 'var(--color-forest)',
          opacity: 0.7,
        }}
      >
        {label}
      </div>
      <div style={{ fontFamily: 'var(--font-display)', fontSize: 22 }}>{value}</div>
    </div>
  )
}

function SectionHeader({
  title,
  meta,
  actions,
}: {
  title: string
  meta?: string
  actions?: React.ReactNode
}) {
  return (
    <div style={{ display: 'flex', alignItems: 'baseline', gap: 12, margin: '40px 0 12px' }}>
      <h2
        style={{
          fontFamily: 'var(--font-display)',
          fontStyle: 'italic',
          fontSize: 30,
          fontWeight: 300,
          margin: 0,
          fontVariationSettings: '"SOFT" 100, "opsz" 144',
        }}
      >
        {title}
        <span style={{ color: 'var(--color-accent)' }}>.</span>
      </h2>
      {meta && (
        <span
          style={{
            fontFamily: 'var(--font-mono)',
            fontSize: 10,
            letterSpacing: 1.4,
            textTransform: 'uppercase',
          }}
        >
          {meta}
        </span>
      )}
      {actions && <div style={{ marginLeft: 'auto' }}>{actions}</div>}
    </div>
  )
}
