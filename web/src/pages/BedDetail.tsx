import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useParams, Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Chip, Stat, PhotoPlaceholder, SectionHeader, MetaCell } from '../components/faltet'
import { Snackbar, useSnackbar } from '../components/Snackbar'
import { SpeciesEditModal } from '../components/faltet/SpeciesEditModal'
import { BedEditDialog } from '../components/bed/BedEditDialog'
import { BedPlantGroups } from '../components/bed/BedPlantGroups'
import { BedPhotosSection } from '../components/bed/BedPhotosSection'
import { MaintenanceRules } from '../components/maintenance/MaintenanceRules'
import { bedEventLabelSv, sortBedsByNaturalName } from '../lib/bed'

export function BedDetail() {
  const { id } = useParams<{ id: string }>()
  const bedId = Number(id)
  const { t } = useTranslation()
  const qc = useQueryClient()

  const { data: bed } = useQuery({ queryKey: ['bed', bedId], queryFn: () => api.beds.get(bedId) })
  const { data: plants } = useQuery({ queryKey: ['bed-plants', bedId], queryFn: () => api.beds.plants(bedId) })
  const { data: bedEvents = [] } = useQuery({
    queryKey: ['bed-events', bedId],
    queryFn: () => api.beds.events(bedId, 20),
  })
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
  const { data: seasons } = useQuery({ queryKey: ['seasons'], queryFn: () => api.seasons.list() })
  const activeSeason = seasons?.find((s) => s.isActive) ?? seasons?.[0]
  const { data: bedHarvest } = useQuery({
    queryKey: ['bed-harvest-stats', bedId, activeSeason?.id ?? null],
    queryFn: () => api.beds.harvestStats(bedId, activeSeason?.id),
    enabled: !Number.isNaN(bedId),
  })
  const harvestYear = activeSeason?.year ?? new Date().getFullYear()

  // Natural prev/next navigation within the same garden (Bed #10 after Bed #9).
  const sortedSiblings = sortBedsByNaturalName(siblings)
  const currentIdx = sortedSiblings.findIndex((s) => s.id === bedId)
  const prevBed = currentIdx > 0 ? sortedSiblings[currentIdx - 1] : null
  const nextBed =
    currentIdx >= 0 && currentIdx < sortedSiblings.length - 1
      ? sortedSiblings[currentIdx + 1]
      : null

  const [modalSpecies, setModalSpecies] = useState<number | null>(null)
  const [editing, setEditing] = useState(false)

  const { message: toast, show: showToast } = useSnackbar()

  const weedMut = useMutation({
    mutationFn: () => api.beds.weed(bedId),
    onSuccess: (r) => {
      showToast(`Rensade ogräs · ${r.plantsAffected} plantor`)
      qc.invalidateQueries({ queryKey: ['bed-events', bedId] })
      qc.invalidateQueries({ queryKey: ['maintenance-rules', 'BED', bedId] })
    },
    onError: () => showToast('Kunde inte rensa ogräs'),
  })

  const waterBedMut = useMutation({
    mutationFn: () => api.beds.water(bedId),
    onSuccess: (r) => {
      showToast(`Vattnade · ${r.plantsAffected} plantor`)
      qc.invalidateQueries({ queryKey: ['bed-events', bedId] })
      qc.invalidateQueries({ queryKey: ['maintenance-rules', 'BED', bedId] })
    },
    onError: () => showToast('Kunde inte vattna'),
  })

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
        right={
          <button onClick={() => setEditing(true)} className="btn-secondary">
            {t('common.edit')}
          </button>
        }
      />

      <div className="page-body">
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
          className="stats-band"
          style={{
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
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <button
                onClick={() => waterBedMut.mutate()}
                disabled={waterBedMut.isPending}
                className="btn-secondary"
                style={{ whiteSpace: 'nowrap' }}
              >
                {waterBedMut.isPending ? '…' : 'Vattna'}
              </button>
              <button
                onClick={() => weedMut.mutate()}
                disabled={weedMut.isPending}
                className="btn-secondary"
                style={{ whiteSpace: 'nowrap' }}
              >
                {weedMut.isPending ? '…' : 'Rensa ogräs'}
              </button>
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
            </div>
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

        {/* Plant groups — collapsed by (species, status, planted date) */}
        <BedPlantGroups plants={plants ?? []} onSpeciesClick={setModalSpecies} />

        {/* Skötsel — bed-level maintenance log */}
        <SectionHeader title="Skötsel" meta={`${bedEvents.length} händelser`} />
        {bedEvents.length === 0 ? (
          <p style={{ fontFamily: 'var(--font-mono)', fontSize: 11, color: 'var(--color-forest)', margin: '8px 0' }}>
            Inga skötselhändelser ännu.
          </p>
        ) : (
          <div style={{ marginBottom: 8 }}>
            {bedEvents.map((ev) => (
              <div
                key={ev.id}
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 80px',
                  gap: 12,
                  padding: '10px 0',
                  borderBottom: '1px solid color-mix(in srgb, var(--color-ink) 20%, transparent)',
                  alignItems: 'baseline',
                }}
              >
                <div>
                  <div style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 16 }}>
                    {bedEventLabelSv(ev.eventType)}
                  </div>
                  <div style={{ fontFamily: 'var(--font-mono)', fontSize: 10, letterSpacing: 1.2, color: 'var(--color-forest)' }}>
                    {ev.plantsAffected != null && ev.plantsAffected > 0
                      ? `${ev.plantsAffected} plantor${ev.notes ? ` · ${ev.notes}` : ''}`
                      : (ev.notes ?? '—')}
                  </div>
                </div>
                <span
                  style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: 10,
                    letterSpacing: 1.2,
                    textAlign: 'right',
                    color: 'var(--color-forest)',
                  }}
                >
                  {ev.eventDate}
                </span>
              </div>
            ))}
          </div>
        )}

        <BedPhotosSection
          bedId={bedId}
          onError={(msg) => showToast(msg)}
          onSuccess={(msg) => showToast(msg)}
        />

        {/* Harvest card — season-scoped stems for this bed */}
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
            {t('bed.harvest.headline', { stems: bedHarvest?.totalStems ?? 0 })}{' '}
            <span style={{ color: 'var(--color-blush)' }}>{t('bed.harvest.season', { year: harvestYear })}</span>.
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
            <MetaCell label={t('bed.meta.orient')} value={bed.sunDirections && bed.sunDirections.length > 0 ? bed.sunDirections.join(' · ') : '—'} />
            <MetaCell label={t('bed.meta.area')} value={`${area} m²`} />
          </div>
        </div>

        {/* Underhåll — shared rule component, generic over bed/area */}
        <MaintenanceRules target={{ kind: 'BED', id: bedId }} />

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

      {editing && (
        <BedEditDialog
          open={editing}
          bed={bed}
          onClose={() => setEditing(false)}
        />
      )}

      <Snackbar message={toast} />
    </div>
  )
}
