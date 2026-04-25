import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Field, Rule } from '../components/faltet'
import { SunDirectionPicker } from '../components/SunDirectionPicker'
import { useOnboarding } from '../onboarding/OnboardingContext'

const SOIL_TYPES = ['SANDY', 'LOAMY', 'CLAY', 'SILTY', 'PEATY', 'CHALKY'] as const
const SUN_EXPOSURES = ['FULL_SUN', 'PARTIAL_SUN', 'PARTIAL_SHADE', 'FULL_SHADE'] as const
const DRAINAGES = ['POOR', 'MODERATE', 'GOOD', 'SHARP'] as const
const IRRIGATION_TYPES = ['DRIP', 'SPRINKLER', 'SOAKER_HOSE', 'MANUAL', 'NONE'] as const
const PROTECTIONS = ['OPEN_FIELD', 'ROW_COVER', 'LOW_TUNNEL', 'HIGH_TUNNEL', 'GREENHOUSE', 'COLDFRAME'] as const

const selectStyle: React.CSSProperties = {
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

export function BedForm() {
  const { gardenId } = useParams<{ gardenId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const { completeStep } = useOnboarding()
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [lengthMeters, setLengthMeters] = useState('')
  const [widthMeters, setWidthMeters] = useState('')

  // Conditions — collapsed by default on create
  const [conditionsOpen, setConditionsOpen] = useState(false)
  const [soilType, setSoilType] = useState('')
  const [soilPh, setSoilPh] = useState('')
  const [sunExposure, setSunExposure] = useState('')
  const [sunDirections, setSunDirections] = useState<string[]>([])
  const [drainage, setDrainage] = useState('')
  const [irrigationType, setIrrigationType] = useState('')
  const [protection, setProtection] = useState('')
  const [raisedBed, setRaisedBed] = useState(false)

  const phNum = soilPh !== '' ? parseFloat(soilPh) : undefined
  const phOutOfRange = phNum !== undefined && (phNum < 3.0 || phNum > 9.0)

  const { data: garden } = useQuery({
    queryKey: ['garden', Number(gardenId)],
    queryFn: () => api.gardens.get(Number(gardenId)),
  })

  const mutation = useMutation({
    mutationFn: () => api.beds.create(Number(gardenId), {
      name,
      description: description || undefined,
      lengthMeters: lengthMeters !== '' ? parseFloat(lengthMeters) : undefined,
      widthMeters: widthMeters !== '' ? parseFloat(widthMeters) : undefined,
      soilType: soilType || undefined,
      soilPh: phNum,
      sunExposure: sunExposure || undefined,
      drainage: drainage || undefined,
      sunDirections: sunDirections.length > 0 ? sunDirections : undefined,
      irrigationType: irrigationType || undefined,
      protection: protection || undefined,
      raisedBed,
    }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['garden-beds', Number(gardenId)] })
      completeStep('create_bed')
      navigate(`/garden/${gardenId}`, { replace: true })
    },
  })

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.gardens')}
            {garden?.name ? ` / ${garden.name}` : ''} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>{t('bed.newBedTitle')}</span>
          </span>
        }
        center={t('form.masthead.center')}
      />

      <div className="page-body-tight">
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
          <Field
            label={t('common.nameLabel')}
            editable
            value={name}
            onChange={setName}
            placeholder={t('bed.bedNamePlaceholder')}
          />
          <Field
            label={t('bed.conditions.lengthMeters')}
            editable
            value={lengthMeters}
            onChange={setLengthMeters}
            accent="mustard"
          />
          <Field
            label={t('bed.conditions.widthMeters')}
            editable
            value={widthMeters}
            onChange={setWidthMeters}
            accent="mustard"
          />
          <div style={{ gridColumn: '1 / -1' }}>
            <Field
              label={t('common.descriptionLabel')}
              editable
              value={description}
              onChange={setDescription}
            />
          </div>
        </div>

        {/* § Förhållanden — collapsible conditions section */}
        <div style={{ marginTop: 28 }}>
          <button
            type="button"
            onClick={() => setConditionsOpen((o) => !o)}
            style={{
              background: 'transparent',
              border: 'none',
              fontFamily: 'var(--font-mono)',
              fontSize: 9,
              letterSpacing: 1.4,
              textTransform: 'uppercase',
              color: 'var(--color-forest)',
              opacity: 0.6,
              cursor: 'pointer',
              padding: 0,
            }}
          >
            {conditionsOpen ? '▼' : '▶'} § {t('bed.conditions.sectionTitle')}
          </button>
          <div style={{ marginTop: 8 }}><Rule variant="soft" /></div>

          {conditionsOpen && (
            <div style={{ marginTop: 20, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px 28px' }}>
              {/* Soil type */}
              <label style={{ display: 'block' }}>
                <span style={selectLabelStyle}>{t('bed.conditions.soilType')}</span>
                <select value={soilType} onChange={(e) => setSoilType(e.target.value)} style={selectStyle}>
                  <option value="">{t('common.select')}</option>
                  {SOIL_TYPES.map((v) => (
                    <option key={v} value={v}>{t(`bed.conditions.soilTypes.${v}`)}</option>
                  ))}
                </select>
              </label>

              {/* Soil pH */}
              <div>
                <Field
                  label={t('bed.conditions.soilPh')}
                  editable
                  value={soilPh}
                  onChange={setSoilPh}
                  accent={phOutOfRange ? 'clay' : undefined}
                />
                {phOutOfRange && (
                  <p style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--color-accent)', marginTop: 4 }}>
                    {t('bed.conditions.phHint')}
                  </p>
                )}
              </div>

              {/* Sun exposure */}
              <label style={{ display: 'block' }}>
                <span style={selectLabelStyle}>{t('bed.conditions.sunExposure')}</span>
                <select value={sunExposure} onChange={(e) => setSunExposure(e.target.value)} style={selectStyle}>
                  <option value="">{t('common.select')}</option>
                  {SUN_EXPOSURES.map((v) => (
                    <option key={v} value={v}>{t(`bed.conditions.sunExposures.${v}`)}</option>
                  ))}
                </select>
              </label>

              {/* Sun directions — multi-select compass */}
              <div>
                <span style={selectLabelStyle}>{t('bed.conditions.sunDirections')}</span>
                <SunDirectionPicker value={sunDirections} onChange={setSunDirections} />
              </div>

              {/* Drainage */}
              <label style={{ display: 'block' }}>
                <span style={selectLabelStyle}>{t('bed.conditions.drainage')}</span>
                <select value={drainage} onChange={(e) => setDrainage(e.target.value)} style={selectStyle}>
                  <option value="">{t('common.select')}</option>
                  {DRAINAGES.map((v) => (
                    <option key={v} value={v}>{t(`bed.conditions.drainages.${v}`)}</option>
                  ))}
                </select>
              </label>

              {/* Irrigation type */}
              <label style={{ display: 'block' }}>
                <span style={selectLabelStyle}>{t('bed.conditions.irrigationType')}</span>
                <select value={irrigationType} onChange={(e) => setIrrigationType(e.target.value)} style={selectStyle}>
                  <option value="">{t('common.select')}</option>
                  {IRRIGATION_TYPES.map((v) => (
                    <option key={v} value={v}>{t(`bed.conditions.irrigationTypes.${v}`)}</option>
                  ))}
                </select>
              </label>

              {/* Protection */}
              <label style={{ display: 'block' }}>
                <span style={selectLabelStyle}>{t('bed.conditions.protection')}</span>
                <select value={protection} onChange={(e) => setProtection(e.target.value)} style={selectStyle}>
                  <option value="">{t('common.select')}</option>
                  {PROTECTIONS.map((v) => (
                    <option key={v} value={v}>{t(`bed.conditions.protections.${v}`)}</option>
                  ))}
                </select>
              </label>

              {/* Raised bed checkbox */}
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, paddingTop: 4 }}>
                <input
                  id="raisedBed-create"
                  type="checkbox"
                  checked={raisedBed}
                  onChange={(e) => setRaisedBed(e.target.checked)}
                  style={{ width: 16, height: 16 }}
                />
                <label
                  htmlFor="raisedBed-create"
                  style={{ fontFamily: 'var(--font-display)', fontSize: 16, fontWeight: 300, cursor: 'pointer' }}
                >
                  {t('bed.conditions.raisedBed')}
                </label>
              </div>
            </div>
          )}
        </div>
      </div>

      {mutation.error && (
        <div style={{ padding: '0 40px 16px' }}>
          <p style={{ fontFamily: 'var(--font-display)', fontStyle: 'italic', fontSize: 14, color: 'var(--color-accent)' }}>
            {mutation.error instanceof Error ? mutation.error.message : String(mutation.error)}
          </p>
        </div>
      )}

      {/* Sticky footer */}
      <div className="sticky-footer">
        <button className="btn-secondary" onClick={() => navigate(`/garden/${gardenId}`)}>
          {t('common.cancel')}
        </button>
        <button
          className="btn-primary"
          onClick={() => mutation.mutate()}
          disabled={!name.trim() || mutation.isPending}
        >
          {mutation.isPending ? t('bed.creatingBed') : t('bed.createBed')}
        </button>
      </div>
    </div>
  )
}
