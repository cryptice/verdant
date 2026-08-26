import { useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient, useQuery } from '@tanstack/react-query'
import { useTranslation } from 'react-i18next'
import { api } from '../api/client'
import { Masthead, Field } from '../components/faltet'
import { AREA_CATEGORIES, areaCategoryLabelSv } from '../lib/area'

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

export function AreaForm() {
  const { gardenId } = useParams<{ gardenId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const { t } = useTranslation()
  const [name, setName] = useState('')
  const [category, setCategory] = useState('')
  const [description, setDescription] = useState('')
  const [sizeSqm, setSizeSqm] = useState('')

  const sizeNum = sizeSqm !== '' ? parseFloat(sizeSqm) : undefined
  const sizeInvalid = sizeNum !== undefined && (Number.isNaN(sizeNum) || sizeNum <= 0)

  const { data: garden } = useQuery({
    queryKey: ['garden', Number(gardenId)],
    queryFn: () => api.gardens.get(Number(gardenId)),
  })

  const mutation = useMutation({
    mutationFn: () => api.areas.create(Number(gardenId), {
      name,
      category,
      description: description || undefined,
      sizeSqm: sizeNum,
    }),
    onSuccess: (area) => {
      qc.invalidateQueries({ queryKey: ['garden-areas', Number(gardenId)] })
      navigate(`/area/${area.id}`, { replace: true })
    },
  })

  const canSave = name.trim() !== '' && category !== '' && !sizeInvalid

  return (
    <div>
      <Masthead
        left={
          <span>
            {t('nav.gardens')}
            {garden?.name ? ` / ${garden.name}` : ''} /{' '}
            <span style={{ color: 'var(--color-accent)' }}>{t('area.newAreaTitle')}</span>
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
            placeholder={t('area.areaNamePlaceholder')}
          />
          <label style={{ display: 'block' }}>
            <span style={selectLabelStyle}>{t('area.meta.category')}</span>
            <select value={category} onChange={(e) => setCategory(e.target.value)} style={selectStyle}>
              <option value="">{t('common.select')}</option>
              {AREA_CATEGORIES.map((c) => (
                <option key={c} value={c}>{areaCategoryLabelSv(c)}</option>
              ))}
            </select>
          </label>
          <div>
            <Field
              label={t('area.sizeSqmLabel')}
              editable
              value={sizeSqm}
              onChange={setSizeSqm}
              accent={sizeInvalid ? 'clay' : 'mustard'}
            />
            {sizeInvalid && (
              <p style={{ fontFamily: 'var(--font-mono)', fontSize: 9, color: 'var(--color-accent)', marginTop: 4 }}>
                {t('area.sizeSqmHint')}
              </p>
            )}
          </div>
          <div style={{ gridColumn: '1 / -1' }}>
            <Field
              label={t('common.descriptionLabel')}
              editable
              value={description}
              onChange={setDescription}
            />
          </div>
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
          disabled={!canSave || mutation.isPending}
        >
          {mutation.isPending ? t('area.creatingArea') : t('area.createArea')}
        </button>
      </div>
    </div>
  )
}
